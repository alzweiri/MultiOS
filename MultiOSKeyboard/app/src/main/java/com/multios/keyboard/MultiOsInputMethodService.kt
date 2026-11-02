package com.multios.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.multios.keyboard.data.KeyboardLanguage
import com.multios.keyboard.data.KeyboardPrefsRepository
import com.multios.keyboard.engine.AutocorrectEngine
import com.multios.keyboard.engine.SuggestionResult
import com.multios.keyboard.layout.KeyboardLayer
import com.multios.keyboard.layout.ShiftState
import com.multios.keyboard.ui.MultiOsKeyboardView
import com.multios.keyboard.ui.TextEditCommand
import java.text.BreakIterator

/**
 * System-Wide MultiOS InputMethodService (`BIND_INPUT_METHOD`).
 *
 * Bridges Android's `InputConnection` & `ClipboardManager` to `MultiOsKeyboardView` with:
 * - Full ViewTree Lifecycle / ViewModelStore / SavedStateRegistry wiring for Compose in IME windows
 * - Real-time English & Arabic Autocorrection (protecting all valid dictionary words) + Undo-on-Backspace
 * - Glide / Swipe Typing word commitment with automatic leading space
 * - Backspace swipe-left word-by-word deletion
 * - Full Gboard Text Editing D-pad (cursor arrows, selection mode, select all, cut, copy, paste, home/end)
 * - Built-in Clipboard listener + primary clip sync
 * - Edge-to-edge full-width base with keys-only side resizing & bottom lift
 */
class MultiOsInputMethodService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var shiftState by mutableStateOf(ShiftState.ONCE)
    private var activeLayer by mutableStateOf(KeyboardLayer.LETTERS)
    private var currentImeAction by mutableIntStateOf(EditorInfo.IME_ACTION_NONE)
    private var suppressAutocorrectForField by mutableStateOf(false)
    private var currentSuggestions by mutableStateOf(
        SuggestionResult(typedWord = "", primaryCorrection = null, candidates = listOf("I", "The", "Hey"))
    )

    private var lastShiftTapMs = 0L
    private var lastSpaceTapMs = 0L

    private var pendingUndoOriginalWord: String? = null
    private var pendingUndoCommittedWord: String? = null

    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        syncPrimaryClipIntoHistory()
    }

    override fun onCreate() {
        super.onCreate()
        KeyboardPrefsRepository.init(this)
        AutocorrectEngine.ensureInitialized(this)
        com.multios.keyboard.emoji.EmojiCatalog.init(this)
        com.multios.keyboard.ui.KeyboardAudioPlayer.init(this)
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        runCatching {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardManager = cm
            cm?.addPrimaryClipChangedListener(clipListener)
            syncPrimaryClipIntoHistory()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        KeyboardPrefsRepository.setOrientation(isLandscape)
    }

    private fun syncPrimaryClipIntoHistory() {
        runCatching {
            val cm = clipboardManager ?: return
            if (!cm.hasPrimaryClip()) return
            val clipData = cm.primaryClip ?: return
            if (clipData.itemCount <= 0) return
            val text = clipData.getItemAt(0)?.coerceToText(this)?.toString() ?: return
            if (text.isNotBlank() && text.length <= 4000) {
                KeyboardPrefsRepository.addClipboardItem(text)
            }
        }
    }

    private fun attachViewTreeOwners(target: View?) {
        target?.let { v ->
            v.setViewTreeLifecycleOwner(this)
            v.setViewTreeViewModelStoreOwner(this)
            v.setViewTreeSavedStateRegistryOwner(this)
        }
    }

    override fun onCreateInputView(): View {
        attachViewTreeOwners(window?.window?.decorView)

        return ComposeView(this).also { composeView ->
            attachViewTreeOwners(composeView)
            composeView.setContent {
                val settings by KeyboardPrefsRepository.state.collectAsState()

                MultiOsKeyboardView(
                    settings = settings,
                    suggestions = currentSuggestions,
                    shiftState = shiftState,
                    layer = activeLayer,
                    imeAction = currentImeAction,
                    onCommitChar = { text -> handleCommitChar(text) },
                    onCommitGlideWord = { glideWord -> handleCommitGlideWord(glideWord) },
                    onSelectSuggestion = { chosen, wasQuotedLiteral ->
                        handleSuggestionSelected(chosen, wasQuotedLiteral)
                    },
                    onBackspace = { handleBackspace() },
                    onDeleteWordBack = { handleDeleteWordBack() },
                    onSpacePress = { handleSpacePress() },
                    onMoveCursorBy = { delta -> handleMoveCursor(delta) },
                    onTextEditCommand = { cmd, selecting -> handleTextEditCommand(cmd, selecting) },
                    onEnterAction = { handleEnterAction() },
                    onShiftTap = { handleShiftTap() },
                    onLayerChange = { nextLayer ->
                        if (nextLayer == KeyboardLayer.CLIPBOARD) {
                            syncPrimaryClipIntoHistory()
                        }
                        activeLayer = nextLayer
                    },
                    onToggleLanguage = {
                        KeyboardPrefsRepository.toggleLanguage()
                        pendingUndoOriginalWord = null
                        pendingUndoCommittedWord = null
                        refreshSuggestionsAndAutoCap()
                    },
                    onShowSystemImePicker = {
                        runCatching {
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        }
                    },
                    onToggleNumberRow = {
                        KeyboardPrefsRepository.setShowNumberRow(!settings.showNumberRow)
                    },
                    onUpdateGeometry = { hScale, bottomDp, leftDp, rightDp ->
                        KeyboardPrefsRepository.updateKeyboardGeometry(
                            heightScale = hScale,
                            bottomOffsetDp = bottomDp,
                            leftInsetDp = leftDp,
                            rightInsetDp = rightDp
                        )
                    },
                    onResetGeometry = {
                        KeyboardPrefsRepository.resetKeyboardGeometry()
                    },
                    onPickEmoji = { emoji ->
                        KeyboardPrefsRepository.recordEmojiUsed(emoji)
                        currentInputConnection?.commitText(emoji, 1)
                        pendingUndoOriginalWord = null
                        pendingUndoCommittedWord = null
                        refreshSuggestionsAndAutoCap()
                    },
                    onPasteClip = { clip ->
                        currentInputConnection?.commitText(clip, 1)
                        pendingUndoOriginalWord = null
                        pendingUndoCommittedWord = null
                        if (activeLayer == KeyboardLayer.CLIPBOARD) {
                            activeLayer = KeyboardLayer.LETTERS
                        }
                        refreshSuggestionsAndAutoCap()
                    },
                    onTogglePinClip = { clip ->
                        KeyboardPrefsRepository.togglePinClip(clip)
                    },
                    onRemoveClip = { clip ->
                        KeyboardPrefsRepository.removeClip(clip)
                    },
                    onClearUnpinnedClips = {
                        KeyboardPrefsRepository.clearUnpinnedClips()
                    },
                    onOpenSettings = {
                        val intent = Intent(this@MultiOsInputMethodService, KeyboardSettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        attachViewTreeOwners(window?.window?.decorView)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        syncPrimaryClipIntoHistory()
        refreshSuggestionsAndAutoCap()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        attachViewTreeOwners(window?.window?.decorView)
        activeLayer = KeyboardLayer.LETTERS
        pendingUndoOriginalWord = null
        pendingUndoCommittedWord = null

        syncPrimaryClipIntoHistory()

        val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noEnterAction = ((info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        currentImeAction = if (noEnterAction) EditorInfo.IME_ACTION_NONE else action

        val inputType = info?.inputType ?: 0
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val cls = inputType and InputType.TYPE_MASK_CLASS

        suppressAutocorrectForField = cls != InputType.TYPE_CLASS_TEXT ||
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_URI ||
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
            (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0

        if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE) {
            activeLayer = KeyboardLayer.SYMBOLS_1
        }

        refreshSuggestionsAndAutoCap()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        refreshSuggestionsAndAutoCap()
    }

    private fun handleCommitChar(text: String) {
        val ic = currentInputConnection ?: return
        pendingUndoOriginalWord = null
        pendingUndoCommittedWord = null

        val isPunctuation = text.length == 1 && text[0] in listOf('.', ',', '!', '?', '،', '؟', '؛', ':', ';')
        if (isPunctuation) {
            applyPrimaryAutocorrectIfApplicable()
        }

        ic.commitText(text, 1)

        if (shiftState == ShiftState.ONCE) {
            shiftState = ShiftState.OFF
        }
        refreshSuggestionsAndAutoCap()
    }

    private fun handleCommitGlideWord(word: String) {
        val ic = currentInputConnection ?: return
        if (word.isBlank()) return
        val before = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
        val needsLeadingSpace = before.isNotEmpty() &&
            !before.last().isWhitespace() &&
            before.last() !in listOf('(', '[', '{', '"', '\'', '/')
        val formatted = when (shiftState) {
            ShiftState.CAPS_LOCK -> word.uppercase()
            ShiftState.ONCE -> word.replaceFirstChar { it.uppercaseChar() }
            ShiftState.OFF -> word
        }
        ic.beginBatchEdit()
        if (needsLeadingSpace) ic.commitText(" ", 1)
        ic.commitText(formatted, 1)
        ic.endBatchEdit()

        if (shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
        pendingUndoOriginalWord = null
        pendingUndoCommittedWord = null
        refreshSuggestionsAndAutoCap()
    }

    private fun handleSpacePress() {
        val ic = currentInputConnection ?: return
        val settings = KeyboardPrefsRepository.state.value
        val now = System.currentTimeMillis()

        if (settings.doubleSpacePeriod && now - lastSpaceTapMs < 310L) {
            val before = ic.getTextBeforeCursor(3, 0)?.toString() ?: ""
            if (before.length >= 2 && before.endsWith(" ") && before[before.length - 2].isLetterOrDigit()) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                lastSpaceTapMs = 0L
                pendingUndoOriginalWord = null
                pendingUndoCommittedWord = null
                if (activeLayer != KeyboardLayer.LETTERS) {
                    activeLayer = KeyboardLayer.LETTERS
                }
                refreshSuggestionsAndAutoCap()
                return
            }
        }
        lastSpaceTapMs = now

        val (currentWord, _) = extractWordsBeforeCursor()
        if (currentWord.isNotBlank()) {
            KeyboardPrefsRepository.recordTypedWord(currentWord)
        }

        val correctedPair = applyPrimaryAutocorrectIfApplicable()
        ic.commitText(" ", 1)

        if (correctedPair != null) {
            pendingUndoOriginalWord = correctedPair.first
            pendingUndoCommittedWord = correctedPair.second + " "
            KeyboardPrefsRepository.recordTypedWord(correctedPair.second)
        } else {
            pendingUndoOriginalWord = null
            pendingUndoCommittedWord = null
        }

        if (activeLayer == KeyboardLayer.SYMBOLS_1 || activeLayer == KeyboardLayer.SYMBOLS_2) {
            activeLayer = KeyboardLayer.LETTERS
        }
        refreshSuggestionsAndAutoCap()
    }

    private fun applyPrimaryAutocorrectIfApplicable(): Pair<String, String>? {
        val settings = KeyboardPrefsRepository.state.value
        if (!settings.autocorrectEnabled || suppressAutocorrectForField) return null
        val ic = currentInputConnection ?: return null

        val (currentWord, _) = extractWordsBeforeCursor()
        if (currentWord.isBlank()) return null

        val sug = AutocorrectEngine.computeSuggestions(
            currentWord = currentWord,
            previousWord = "",
            language = settings.language,
            autocorrectEnabled = true
        )
        val replacement = sug.primaryCorrection ?: return null
        if (replacement == currentWord) return null

        ic.beginBatchEdit()
        ic.deleteSurroundingText(currentWord.length, 0)
        ic.commitText(replacement, 1)
        ic.endBatchEdit()
        return Pair(currentWord, replacement)
    }

    private fun handleSuggestionSelected(chosen: String, wasQuotedLiteral: Boolean) {
        val ic = currentInputConnection ?: return
        val (currentWord, _) = extractWordsBeforeCursor()

        if (wasQuotedLiteral && chosen.length >= 2) {
            KeyboardPrefsRepository.learnUserWord(chosen)
        }

        ic.beginBatchEdit()
        if (currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }
        ic.commitText("$chosen ", 1)
        ic.endBatchEdit()

        pendingUndoOriginalWord = null
        pendingUndoCommittedWord = null
        if (shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
        refreshSuggestionsAndAutoCap()
    }

    private fun handleDeleteWordBack() {
        val ic = currentInputConnection ?: return
        pendingUndoOriginalWord = null
        pendingUndoCommittedWord = null

        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
            refreshSuggestionsAndAutoCap()
            return
        }

        val before = ic.getTextBeforeCursor(48, 0)?.toString() ?: ""
        if (before.isEmpty()) return

        var idx = before.length - 1
        // Skip trailing spaces first, then delete the whole preceding word
        while (idx >= 0 && before[idx].isWhitespace()) idx--
        if (idx >= 0 && isWordChar(before[idx])) {
            while (idx >= 0 && isWordChar(before[idx])) idx--
        } else if (idx >= 0) {
            idx--
        }
        val deleteCount = (before.length - 1 - idx).coerceAtLeast(1)
        ic.deleteSurroundingText(deleteCount, 0)
        refreshSuggestionsAndAutoCap()
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return

        val undoOrig = pendingUndoOriginalWord
        val undoCommitted = pendingUndoCommittedWord
        if (undoOrig != null && undoCommitted != null) {
            val tail = ic.getTextBeforeCursor(undoCommitted.length, 0)?.toString() ?: ""
            if (tail == undoCommitted) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(undoCommitted.length, 0)
                ic.commitText(undoOrig, 1)
                ic.endBatchEdit()
                KeyboardPrefsRepository.learnUserWord(undoOrig)
                pendingUndoOriginalWord = null
                pendingUndoCommittedWord = null
                refreshSuggestionsAndAutoCap()
                return
            }
            pendingUndoOriginalWord = null
            pendingUndoCommittedWord = null
        }

        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            val before = ic.getTextBeforeCursor(8, 0)?.toString() ?: ""
            if (before.isNotEmpty()) {
                val bi = BreakIterator.getCharacterInstance()
                bi.setText(before)
                val end = bi.last()
                val start = bi.previous()
                val deleteLen = if (start != BreakIterator.DONE && end > start) end - start else 1
                ic.deleteSurroundingText(deleteLen, 0)
            } else {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
        }
        refreshSuggestionsAndAutoCap()
    }

    private fun handleMoveCursor(delta: Int) {
        if (delta == 0) return
        val ic = currentInputConnection
        if (ic != null) {
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
            if (extracted != null && extracted.selectionStart == extracted.selectionEnd && extracted.text != null) {
                val target = (extracted.selectionStart + delta).coerceIn(0, extracted.text.length)
                ic.setSelection(target, target)
                refreshSuggestionsAndAutoCap()
                return
            }
        }
        val keyCode = if (delta > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(kotlin.math.abs(delta)) {
            sendDownUpKeyEvents(keyCode)
        }
        refreshSuggestionsAndAutoCap()
    }

    private fun sendKeyWithOptionalShift(keyCode: Int, shiftHeld: Boolean) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        val meta = if (shiftHeld) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }

    private fun handleTextEditCommand(cmd: TextEditCommand, selecting: Boolean) {
        val ic = currentInputConnection ?: return
        when (cmd) {
            TextEditCommand.LEFT -> sendKeyWithOptionalShift(KeyEvent.KEYCODE_DPAD_LEFT, selecting)
            TextEditCommand.RIGHT -> sendKeyWithOptionalShift(KeyEvent.KEYCODE_DPAD_RIGHT, selecting)
            TextEditCommand.UP -> sendKeyWithOptionalShift(KeyEvent.KEYCODE_DPAD_UP, selecting)
            TextEditCommand.DOWN -> sendKeyWithOptionalShift(KeyEvent.KEYCODE_DPAD_DOWN, selecting)
            TextEditCommand.LINE_START -> sendKeyWithOptionalShift(KeyEvent.KEYCODE_MOVE_HOME, selecting)
            TextEditCommand.LINE_END -> sendKeyWithOptionalShift(KeyEvent.KEYCODE_MOVE_END, selecting)
            TextEditCommand.SELECT_ALL -> ic.performContextMenuAction(android.R.id.selectAll)
            TextEditCommand.CUT -> {
                ic.performContextMenuAction(android.R.id.cut)
                syncPrimaryClipIntoHistory()
            }
            TextEditCommand.COPY -> {
                ic.performContextMenuAction(android.R.id.copy)
                syncPrimaryClipIntoHistory()
            }
            TextEditCommand.PASTE -> ic.performContextMenuAction(android.R.id.paste)
            TextEditCommand.BACKSPACE -> handleBackspace()
        }
        refreshSuggestionsAndAutoCap()
    }

    private fun handleEnterAction() {
        val ic = currentInputConnection ?: return
        pendingUndoOriginalWord = null
        pendingUndoCommittedWord = null

        when (currentImeAction) {
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_DONE -> {
                val handled = ic.performEditorAction(currentImeAction)
                if (!handled) {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
            }
            else -> {
                ic.commitText("\n", 1)
            }
        }
        refreshSuggestionsAndAutoCap()
    }

    private fun handleShiftTap() {
        val now = System.currentTimeMillis()
        shiftState = when (shiftState) {
            ShiftState.OFF -> ShiftState.ONCE
            ShiftState.ONCE -> {
                if (now - lastShiftTapMs < 360L) ShiftState.CAPS_LOCK else ShiftState.OFF
            }
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        lastShiftTapMs = now
    }

    private fun extractWordsBeforeCursor(): Pair<String, String> {
        val ic = currentInputConnection ?: return Pair("", "")
        val raw = ic.getTextBeforeCursor(40, 0)?.toString() ?: return Pair("", "")
        if (raw.isEmpty()) return Pair("", "")

        var idx = raw.length - 1
        while (idx >= 0 && isWordChar(raw[idx])) {
            idx--
        }
        val currentWord = raw.substring(idx + 1)

        var pEnd = idx
        while (pEnd >= 0 && raw[pEnd].isWhitespace()) pEnd--
        var pStart = pEnd
        while (pStart >= 0 && isWordChar(raw[pStart])) pStart--
        val prevWord = if (pEnd >= 0 && pEnd > pStart) raw.substring(pStart + 1, pEnd + 1) else ""

        return Pair(currentWord, prevWord)
    }

    private fun isWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '\'' || c in '\u064B'..'\u0652' || c == 'ـ'
    }

    private fun refreshSuggestionsAndAutoCap() {
        val settings = KeyboardPrefsRepository.state.value
        val ic = currentInputConnection

        if (settings.language == KeyboardLanguage.ENGLISH && shiftState != ShiftState.CAPS_LOCK && ic != null) {
            val info = currentInputEditorInfo
            val capFlags = (info?.inputType ?: 0) and (
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS or
                    InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                )
            if (capFlags != 0) {
                val cursorCaps = ic.getCursorCapsMode(capFlags)
                shiftState = if (cursorCaps != 0) ShiftState.ONCE else ShiftState.OFF
            } else {
                val before = ic.getTextBeforeCursor(4, 0)?.toString() ?: ""
                val atSentenceStart = before.isEmpty() ||
                    before.endsWith("\n") ||
                    before.endsWith(". ") ||
                    before.endsWith("! ") ||
                    before.endsWith("? ")
                if (atSentenceStart) {
                    shiftState = ShiftState.ONCE
                }
            }
        }

        val (currentWord, prevWord) = extractWordsBeforeCursor()
        currentSuggestions = AutocorrectEngine.computeSuggestions(
            currentWord = currentWord,
            previousWord = prevWord,
            language = settings.language,
            autocorrectEnabled = settings.autocorrectEnabled && !suppressAutocorrectForField
        )
    }

    override fun onDestroy() {
        runCatching { clipboardManager?.removePrimaryClipChangedListener(clipListener) }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}
