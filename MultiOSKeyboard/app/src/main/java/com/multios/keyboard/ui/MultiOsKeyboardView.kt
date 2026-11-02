package com.multios.keyboard.ui

import android.view.inputmethod.EditorInfo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.multios.keyboard.data.KeyboardLanguage
import com.multios.keyboard.data.KeyboardSettingsState
import com.multios.keyboard.emoji.EmojiCatalog
import com.multios.keyboard.engine.AutocorrectEngine
import com.multios.keyboard.engine.SuggestionResult
import com.multios.keyboard.layout.GboardLayouts
import com.multios.keyboard.layout.KeyRole
import com.multios.keyboard.layout.KeySpec
import com.multios.keyboard.layout.KeyboardLayer
import com.multios.keyboard.layout.ShiftState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class TextEditCommand {
    LEFT, RIGHT, UP, DOWN, LINE_START, LINE_END, SELECT_ALL, CUT, COPY, PASTE, BACKSPACE
}

/**
 * Coordinates multi-thumb rollover typing just like Gboard:
 * If Thumb 1 presses 'T' and Thumb 2 presses 'H' before Thumb 1 lifts,
 * 'T' is flushed immediately on 'H' touch-down so fast 2-thumb typing
 * never transposes letters ("th" never becomes "ht").
 */
@Stable
private class RolloverKeyCoordinator {
    private var activeToken: Any? = null
    private var flushCallback: (() -> Unit)? = null

    fun onKeyDown(token: Any, onEarlyFlush: (() -> Unit)?) {
        if (activeToken != null && activeToken !== token) {
            val prev = flushCallback
            activeToken = null
            flushCallback = null
            prev?.invoke()
        }
        if (onEarlyFlush != null) {
            activeToken = token
            flushCallback = onEarlyFlush
        }
    }

    fun clearIfOwner(token: Any) {
        if (activeToken === token) {
            activeToken = null
            flushCallback = null
        }
    }
}

private data class KeyHitRect(
    val id: String,
    val charText: String,
    val rectInGrid: Rect
)

/**
 * Full System-Wide MultiOS Keyboard Composable (V4 Gboard-Parity Edition).
 *
 * Key V4 Upgrades:
 * 1. Zero-latency hardware haptics fired instantaneously on `ACTION_DOWN` (0ms Gboard feel).
 * 2. Outer Keyboard Base/Borders ALWAYS touch both left & right edges of the screen (`100%` width),
 *    while `leftInsetDp` & `rightInsetDp` strictly resize/inset the inner KEYS grid!
 * 3. Bulletproof `try/finally` + disposal press tracking (key popups can NEVER stick!) +
 *    fast pop-out darker rounded key box (exact `7.5.dp` corner radius) on press & fast snap-down on release +
 *    horizontal expansion strip on hold with left/right slide selection!
 * 4. Full Gboard Suite (minus voice/mic): Glide/Swipe typing trail, Backspace swipe-left word delete,
 *    Spacebar cursor trackpad, `<I>` Text-Editing cursor/selection D-pad, Emoji keyword search + Kaomoji,
 *    Clipboard manager, Number row, and 1-handed/4-side key resizing!
 */
@Composable
fun MultiOsKeyboardView(
    settings: KeyboardSettingsState,
    suggestions: SuggestionResult,
    shiftState: ShiftState,
    layer: KeyboardLayer,
    imeAction: Int = EditorInfo.IME_ACTION_NONE,
    onCommitChar: (String) -> Unit,
    onCommitGlideWord: (String) -> Unit = { onCommitChar("$it ") },
    onSelectSuggestion: (chosen: String, wasQuotedLiteral: Boolean) -> Unit,
    onBackspace: () -> Unit,
    onDeleteWordBack: () -> Unit = onBackspace,
    onSpacePress: () -> Unit,
    onMoveCursorBy: (Int) -> Unit,
    onTextEditCommand: (TextEditCommand, Boolean) -> Unit = { _, _ -> },
    onEnterAction: () -> Unit,
    onShiftTap: () -> Unit,
    onLayerChange: (KeyboardLayer) -> Unit,
    onToggleLanguage: () -> Unit,
    onShowSystemImePicker: () -> Unit = {},
    onToggleNumberRow: () -> Unit,
    onUpdateGeometry: (heightScale: Float, bottomOffsetDp: Float, leftInsetDp: Float, rightInsetDp: Float) -> Unit,
    onResetGeometry: () -> Unit = { onUpdateGeometry(1.0f, 18f, 0f, 0f) },
    onPickEmoji: (String) -> Unit,
    onPasteClip: (String) -> Unit = { onCommitChar(it) },
    onTogglePinClip: (String) -> Unit = {},
    onRemoveClip: (String) -> Unit = {},
    onClearUnpinnedClips: () -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val palette = rememberKeyboardPalette(settings)
    val rolloverCoordinator = remember { RolloverKeyCoordinator() }

    var resizeModeActive by remember { mutableStateOf(false) }
    var dismissedQuickClip by remember { mutableStateOf<String?>(null) }

    // Glide typing live trail state
    val keyRectsInRoot = remember { mutableStateMapOf<String, KeyHitRect>() }
    var gridBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val glidePointsInGrid = remember { mutableStateListOf<Offset>() }
    var glidePreviewWord by remember { mutableStateOf<String?>(null) }

    fun fireDownHaptic() {
        KeyboardHaptics.keyDown(view, settings.hapticsEnabled, settings.hapticStrengthMs)
    }

    fun fireTickHaptic() {
        KeyboardHaptics.cursorTick(view, settings.hapticsEnabled)
    }

    fun fireHoldHaptic() {
        KeyboardHaptics.longPressPop(view, settings.hapticsEnabled)
    }

    val rows = remember(settings.language, layer, shiftState, settings.showNumberRow) {
        GboardLayouts.buildRows(
            language = settings.language,
            layer = layer,
            shiftState = shiftState,
            showNumberRow = settings.showNumberRow
        )
    }

    val visualKeyHeightDp = (43f * settings.heightScale).coerceIn(31f, 62f)
    val visualNumKeyHeightDp = (35f * settings.heightScale).coerceIn(26f, 50f)
    val keyVerticalPadDp = (3.0f * settings.heightScale).coerceIn(2.0f, 4.5f)

    val touchKeyCellHeightDp = visualKeyHeightDp + keyVerticalPadDp * 2f
    val touchNumCellHeightDp = visualNumKeyHeightDp + keyVerticalPadDp * 2f

    // OUTER CONTAINER: 100% FULL SCREEN WIDTH — Base & borders ALWAYS touch left & right screen edges!
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        KeyboardSurfaceContainer(
            palette = palette,
            cornerRadius = 22.dp,
            roundBottomCorners = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp,
                            // Bottom clearance lifts the keys above Circle-to-Search & IME globe while base stays anchored!
                            bottom = (4f + settings.bottomOffsetDp).dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // =========================================================================
                    // 1. TOP BAR: SUGGESTIONS / GLIDE PREVIEW / QUICK CLIP + GBOARD TOOLBAR
                    // =========================================================================
                    val latestClip = settings.clipboardHistory.firstOrNull()
                    val showQuickClipChip = latestClip != null &&
                        latestClip != dismissedQuickClip &&
                        suggestions.typedWord.isEmpty() &&
                        glidePreviewWord == null &&
                        (System.currentTimeMillis() - settings.lastClipTimestampMs) < 45_000L

                    val effectiveSuggestions = if (glidePreviewWord != null) {
                        SuggestionResult(
                            typedWord = glidePreviewWord!!,
                            primaryCorrection = glidePreviewWord,
                            candidates = listOf(glidePreviewWord!!)
                        )
                    } else {
                        suggestions
                    }

                    SuggestionAndToolbarStrip(
                        suggestions = effectiveSuggestions,
                        quickClip = if (showQuickClipChip) latestClip else null,
                        settings = settings,
                        layer = layer,
                        resizeModeActive = resizeModeActive,
                        palette = palette,
                        onTouchDownFeedback = { fireDownHaptic() },
                        onSelectSuggestion = { word, wasLiteral ->
                            onSelectSuggestion(word, wasLiteral)
                        },
                        onPasteQuickClip = { clip ->
                            dismissedQuickClip = clip
                            onPasteClip(clip)
                        },
                        onToggleEmoji = {
                            onLayerChange(
                                if (layer == KeyboardLayer.EMOJI) KeyboardLayer.LETTERS else KeyboardLayer.EMOJI
                            )
                        },
                        onToggleClipboard = {
                            onLayerChange(
                                if (layer == KeyboardLayer.CLIPBOARD) KeyboardLayer.LETTERS else KeyboardLayer.CLIPBOARD
                            )
                        },
                        onToggleTextEdit = {
                            onLayerChange(
                                if (layer == KeyboardLayer.TEXT_EDIT) KeyboardLayer.LETTERS else KeyboardLayer.TEXT_EDIT
                            )
                        },
                        onToggleTranslator = {
                            onLayerChange(
                                if (layer == KeyboardLayer.TRANSLATOR) KeyboardLayer.LETTERS else KeyboardLayer.TRANSLATOR
                            )
                        },
                        onToggleNumberRow = {
                            onToggleNumberRow()
                        },
                        onToggleResize = {
                            resizeModeActive = !resizeModeActive
                        },
                        onOpenSettings = {
                            onOpenSettings()
                        }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val overlayPanelHeightDp =
                        touchKeyCellHeightDp * 4f + if (settings.showNumberRow) touchNumCellHeightDp else 0f

                    // =========================================================================
                    // 2. INNER KEYS / PANEL AREA — Side resize insets ONLY the keys, NOT the outer base!
                    // =========================================================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = (3f + settings.leftInsetDp).dp,
                                end = (3f + settings.rightInsetDp).dp
                            )
                    ) {
                        when (layer) {
                            KeyboardLayer.EMOJI -> {
                                EmojiPickerPanel(
                                    settings = settings,
                                    palette = palette,
                                    panelHeightDp = overlayPanelHeightDp,
                                    onDownHaptic = { fireDownHaptic() },
                                    onPickEmoji = { emoji -> onPickEmoji(emoji) },
                                    onBackspace = { onBackspace() },
                                    onReturnToLetters = { onLayerChange(KeyboardLayer.LETTERS) }
                                )
                            }

                            KeyboardLayer.CLIPBOARD -> {
                                ClipboardPickerPanel(
                                    settings = settings,
                                    palette = palette,
                                    panelHeightDp = overlayPanelHeightDp,
                                    onDownHaptic = { fireDownHaptic() },
                                    onPasteClip = { clip -> onPasteClip(clip) },
                                    onTogglePinClip = { clip -> onTogglePinClip(clip) },
                                    onRemoveClip = { clip -> onRemoveClip(clip) },
                                    onClearUnpinned = { onClearUnpinnedClips() },
                                    onReturnToLetters = { onLayerChange(KeyboardLayer.LETTERS) }
                                )
                            }

                            KeyboardLayer.TEXT_EDIT -> {
                                TextEditingCursorPanel(
                                    settings = settings,
                                    palette = palette,
                                    panelHeightDp = overlayPanelHeightDp,
                                    onDownHaptic = { fireDownHaptic() },
                                    onCommand = { cmd, selecting -> onTextEditCommand(cmd, selecting) },
                                    onReturnToLetters = { onLayerChange(KeyboardLayer.LETTERS) }
                                )
                            }

                            KeyboardLayer.TRANSLATOR -> {
                                TranslatorPanel(
                                    palette = palette,
                                    panelHeightDp = overlayPanelHeightDp,
                                    onDownHaptic = { fireDownHaptic() },
                                    onCommitTranslation = { translation ->
                                        onCommitChar(translation)
                                        onLayerChange(KeyboardLayer.LETTERS)
                                    },
                                    onReturnToLetters = { onLayerChange(KeyboardLayer.LETTERS) }
                                )
                            }

                            else -> {
                                // Zero-gap Column + Zero-gap Rows -> 0px dead space anywhere on the keys grid!
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coords ->
                                            gridBoundsInRoot = coords.boundsInRoot()
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        rows.forEachIndexed { rowIndex, rowKeys ->
                                            val isNumberRow = settings.showNumberRow &&
                                                layer == KeyboardLayer.LETTERS &&
                                                rowIndex == 0

                                            val isCenteredNineKeyRow = layer == KeyboardLayer.LETTERS &&
                                                settings.language == KeyboardLanguage.ENGLISH &&
                                                rowKeys.size == 9 &&
                                                rowKeys.first().role == KeyRole.CHAR

                                            val cellHeightDp =
                                                if (isNumberRow) touchNumCellHeightDp else touchKeyCellHeightDp

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .zIndex((10 - rowIndex).toFloat()),
                                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                rowKeys.forEachIndexed { keyIdx, spec ->
                                                    val extraLeftVisualInset =
                                                        if (isCenteredNineKeyRow && keyIdx == 0) 13.dp else 0.dp
                                                    val extraRightVisualInset =
                                                        if (isCenteredNineKeyRow && keyIdx == rowKeys.lastIndex) 13.dp else 0.dp
                                                    val touchWeight =
                                                        if (isCenteredNineKeyRow && (keyIdx == 0 || keyIdx == rowKeys.lastIndex)) {
                                                            spec.weight + 0.42f
                                                        } else {
                                                            spec.weight
                                                        }

                                                    val keyId = "$rowIndex-$keyIdx-${spec.label}"
                                                    val keyFractionX = if (rowKeys.size > 1) {
                                                        keyIdx.toFloat() / (rowKeys.size - 1).toFloat()
                                                    } else 0.5f

                                                    GboardKeyItem(
                                                        keyId = keyId,
                                                        spec = spec,
                                                        keyHorizontalFraction = keyFractionX,
                                                        touchCellHeightDp = cellHeightDp,
                                                        verticalPadDp = keyVerticalPadDp,
                                                        extraStartVisualPad = extraLeftVisualInset,
                                                        extraEndVisualPad = extraRightVisualInset,
                                                        showKeyBorders = settings.showKeyBorders,
                                                        glideEnabled = settings.glideTypingEnabled && layer == KeyboardLayer.LETTERS,
                                                        swipeBackspaceWords = settings.swipeBackspaceWords,
                                                        shiftState = shiftState,
                                                        imeAction = imeAction,
                                                        palette = palette,
                                                        rolloverCoordinator = rolloverCoordinator,
                                                        modifier = Modifier
                                                            .weight(touchWeight)
                                                            .onGloballyPositioned { coords ->
                                                                if (spec.role == KeyRole.CHAR && spec.commitText.length == 1 && spec.commitText[0].isLetter()) {
                                                                    keyRectsInRoot[keyId] = KeyHitRect(
                                                                        id = keyId,
                                                                        charText = spec.commitText,
                                                                        rectInGrid = coords.boundsInRoot()
                                                                    )
                                                                }
                                                            },
                                                        onInstantKeyDownHaptic = {
                                                            fireDownHaptic()
                                                            KeyboardAudioPlayer.playKeySound(settings.soundProfile, settings.soundVolume)
                                                        },
                                                        onTickHaptic = { fireTickHaptic() },
                                                        onHoldHaptic = { fireHoldHaptic() },
                                                        onTapPrimary = {
                                                            when (spec.role) {
                                                                KeyRole.CHAR, KeyRole.COMMA, KeyRole.PERIOD -> {
                                                                    onCommitChar(spec.commitText)
                                                                }
                                                                KeyRole.SHIFT -> onShiftTap()
                                                                KeyRole.BACKSPACE -> onBackspace()
                                                                KeyRole.SYMBOLS_TOGGLE -> {
                                                                    val next = if (layer == KeyboardLayer.LETTERS) {
                                                                        KeyboardLayer.SYMBOLS_1
                                                                    } else {
                                                                        KeyboardLayer.LETTERS
                                                                    }
                                                                    onLayerChange(next)
                                                                }
                                                                KeyRole.SECONDARY_SYMBOLS -> {
                                                                    val next = if (layer == KeyboardLayer.SYMBOLS_1) {
                                                                        KeyboardLayer.SYMBOLS_2
                                                                    } else {
                                                                        KeyboardLayer.SYMBOLS_1
                                                                    }
                                                                    onLayerChange(next)
                                                                }
                                                                KeyRole.LANGUAGE_GLOBE -> onToggleLanguage()
                                                                KeyRole.EMOJI -> onLayerChange(KeyboardLayer.EMOJI)
                                                                KeyRole.SPACE -> onSpacePress()
                                                                KeyRole.ENTER_ACTION -> onEnterAction()
                                                            }
                                                        },
                                                        onLongPressLanguageGlobe = { onShowSystemImePicker() },
                                                        onCommitAlternate = { alt -> onCommitChar(alt) },
                                                        onRepeatBackspace = { onBackspace() },
                                                        onSwipeDeleteWord = { onDeleteWordBack() },
                                                        onSpaceCursorStep = { dir -> onMoveCursorBy(dir) },
                                                        onGlideMoveRoot = { rootPos ->
                                                            val localPt = Offset(
                                                                rootPos.x - gridBoundsInRoot.left,
                                                                rootPos.y - gridBoundsInRoot.top
                                                            )
                                                            glidePointsInGrid.add(localPt)
                                                            if (glidePointsInGrid.size > 64) {
                                                                glidePointsInGrid.removeAt(0)
                                                            }
                                                            val hit = keyRectsInRoot.values.firstOrNull {
                                                                it.rectInGrid.contains(rootPos)
                                                            }
                                                            hit?.charText
                                                        },
                                                        onGlideVisitedUpdate = { visited ->
                                                            val cands = AutocorrectEngine.decodeGlideSequence(
                                                                visited,
                                                                settings.language
                                                            )
                                                            glidePreviewWord = cands.firstOrNull()
                                                        },
                                                        onGlideFinish = { visited ->
                                                            val best = AutocorrectEngine.decodeGlideSequence(
                                                                visited,
                                                                settings.language
                                                            ).firstOrNull()
                                                            glidePointsInGrid.clear()
                                                            glidePreviewWord = null
                                                            if (!best.isNullOrBlank()) {
                                                                onCommitGlideWord(best)
                                                            }
                                                        },
                                                        onGlideCancel = {
                                                            glidePointsInGrid.clear()
                                                            glidePreviewWord = null
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Luminous Glide Typing Trail Overlay
                                    if (glidePointsInGrid.size >= 2) {
                                        val trailColor = settings.resolvedTintColor
                                        Canvas(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .zIndex(25f)
                                        ) {
                                            val pts = glidePointsInGrid.toList()
                                            if (pts.size >= 2) {
                                                val path = Path().apply {
                                                    moveTo(pts.first().x, pts.first().y)
                                                    for (i in 1 until pts.size) {
                                                        lineTo(pts[i].x, pts[i].y)
                                                    }
                                                }
                                                drawPath(
                                                    path = path,
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            trailColor.copy(alpha = 0.08f),
                                                            trailColor.copy(alpha = 0.82f)
                                                        ),
                                                        start = pts.first(),
                                                        end = pts.last()
                                                    ),
                                                    style = Stroke(
                                                        width = 7.dp.toPx(),
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 3. SAMSUNG-STYLE KEYS-ONLY 4-SIDE RESIZE & ONE-HANDED DOCK OVERLAY
                // =========================================================================
                if (resizeModeActive) {
                    FourSideResizeOverlay(
                        settings = settings,
                        modifier = Modifier.matchParentSize(),
                        onUpdateGeometry = onUpdateGeometry,
                        onHaptic = { fireDownHaptic() },
                        onReset = {
                            fireDownHaptic()
                            onResetGeometry()
                        },
                        onDone = {
                            fireDownHaptic()
                            resizeModeActive = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionAndToolbarStrip(
    suggestions: SuggestionResult,
    quickClip: String?,
    settings: KeyboardSettingsState,
    layer: KeyboardLayer,
    resizeModeActive: Boolean,
    palette: KeyboardVisualPalette,
    onTouchDownFeedback: () -> Unit,
    onSelectSuggestion: (word: String, wasLiteral: Boolean) -> Unit,
    onPasteQuickClip: (String) -> Unit,
    onToggleEmoji: () -> Unit,
    onToggleClipboard: () -> Unit,
    onToggleTextEdit: () -> Unit,
    onToggleTranslator: () -> Unit,
    onToggleNumberRow: () -> Unit,
    onToggleResize: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Emoji Toggle
        ToolbarIconButton(
            active = layer == KeyboardLayer.EMOJI,
            palette = palette,
            onClick = {
                onTouchDownFeedback()
                onToggleEmoji()
            }
        ) {
            if (layer == KeyboardLayer.EMOJI) {
                KeyboardIcons.KeyboardIconGlyph(Modifier.size(17.dp), palette.primaryText)
            } else {
                KeyboardIcons.EmojiSmiley(Modifier.size(17.dp), palette.primaryText)
            }
        }

        // Clipboard Toggle
        ToolbarIconButton(
            active = layer == KeyboardLayer.CLIPBOARD,
            palette = palette,
            onClick = {
                onTouchDownFeedback()
                onToggleClipboard()
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (layer == KeyboardLayer.CLIPBOARD) {
                    KeyboardIcons.KeyboardIconGlyph(Modifier.size(17.dp), palette.primaryText)
                } else {
                    KeyboardIcons.Clipboard(Modifier.size(17.dp), palette.primaryText)
                    if (settings.clipboardHistory.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(palette.popupHighlightFill)
                        )
                    }
                }
            }
        }

        // Gboard Text-Editing Cursor Pad Toggle (`<I>`)
        ToolbarIconButton(
            active = layer == KeyboardLayer.TEXT_EDIT,
            palette = palette,
            onClick = {
                onTouchDownFeedback()
                onToggleTextEdit()
            }
        ) {
            KeyboardIcons.TextCursorIcon(Modifier.size(16.dp), palette.primaryText)
        }

        // Live Translator Toggle (`文A`)
        if (settings.translatorEnabled) {
            ToolbarIconButton(
                active = layer == KeyboardLayer.TRANSLATOR,
                palette = palette,
                onClick = {
                    onTouchDownFeedback()
                    onToggleTranslator()
                }
            ) {
                Text(
                    text = "ع/E",
                    color = if (layer == KeyboardLayer.TRANSLATOR) palette.popupHighlightText else palette.primaryText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center Area: 1-Tap Quick Clipboard Chip OR 3-Slot Gboard Suggestion Bar
        if (quickClip != null && layer == KeyboardLayer.LETTERS) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.popupHighlightFill.copy(alpha = 0.28f))
                        .clickable {
                            onTouchDownFeedback()
                            onPasteQuickClip(quickClip)
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeyboardIcons.Clipboard(Modifier.size(13.dp), palette.primaryText)
                    Text(
                        text = quickClip.replace('\n', ' '),
                        color = palette.primaryText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cands = suggestions.candidates
                for (idx in 0 until 3) {
                    val raw = cands.getOrNull(idx) ?: ""
                    val isCenterPrimary = idx == 1 && suggestions.primaryCorrection != null
                    val wasQuoted = raw.startsWith("\"") && raw.endsWith("\"") && raw.length >= 2
                    val displayLabel = if (wasQuoted) raw.removeSurrounding("\"") else raw

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isCenterPrimary) palette.popupHighlightFill.copy(alpha = 0.26f)
                                else Color.Transparent
                            )
                            .then(
                                if (raw.isNotBlank()) {
                                    Modifier.clickable {
                                        onTouchDownFeedback()
                                        onSelectSuggestion(displayLabel, wasQuoted)
                                    }
                                } else Modifier
                            )
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (raw.isNotBlank()) {
                            Text(
                                text = displayLabel,
                                color = if (isCenterPrimary) palette.primaryText else palette.primaryText.copy(alpha = 0.82f),
                                fontSize = 13.5.sp,
                                fontWeight = if (isCenterPrimary || idx == 1) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                    if (idx < 2) {
                        Box(
                            modifier = Modifier
                                .width(0.75.dp)
                                .height(15.dp)
                                .background(palette.secondaryText.copy(alpha = 0.20f))
                        )
                    }
                }
            }
        }

        // Quick Number Row Toggle (`123`)
        ToolbarIconButton(
            active = settings.showNumberRow,
            palette = palette,
            onClick = {
                onTouchDownFeedback()
                onToggleNumberRow()
            }
        ) {
            Text(
                text = "123",
                color = palette.primaryText.copy(alpha = if (settings.showNumberRow) 0.96f else 0.52f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Keys-Only 4-Side Resize & One-Handed Snap Toggle
        ToolbarIconButton(
            active = resizeModeActive,
            palette = palette,
            onClick = {
                onTouchDownFeedback()
                onToggleResize()
            }
        ) {
            KeyboardIcons.MoveFourWay(Modifier.size(16.dp), palette.primaryText)
        }

        // Settings Gear Icon
        ToolbarIconButton(
            active = false,
            palette = palette,
            onClick = {
                onTouchDownFeedback()
                onOpenSettings()
            }
        ) {
            KeyboardIcons.Gear(Modifier.size(16.dp), palette.primaryText.copy(alpha = 0.82f))
        }
    }
}

@Composable
private fun ToolbarIconButton(
    active: Boolean,
    palette: KeyboardVisualPalette,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
                if (active) palette.popupHighlightFill.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Keys-Only 4-Side Resize & One-Handed Quick Snap Frame:
 * - Outer keyboard glass base stays flush edge-to-edge touching both sides of the screen!
 * - Inner cyan frame hugs the KEYS grid (`start = leftInsetDp`, `end = rightInsetDp`, `bottom = bottomOffsetDp`)
 *   so pulling the left/right handles squeezes ONLY the keys inward!
 */
@Composable
private fun FourSideResizeOverlay(
    settings: KeyboardSettingsState,
    modifier: Modifier = Modifier,
    onUpdateGeometry: (heightScale: Float, bottomOffsetDp: Float, leftInsetDp: Float, rightInsetDp: Float) -> Unit,
    onHaptic: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit
) {
    val density = LocalDensity.current
    val currentSettings by rememberUpdatedState(settings)
    val updatedGeometryCallback by rememberUpdatedState(onUpdateGeometry)
    val accent = Color(0xFF38BDF8)

    Box(
        modifier = modifier
            .background(Color(0x99060911))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    down.consume()
                }
            }
    ) {
        // Active Keys Bounding Box showing exact keys-only inset inside the full-width keyboard base!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 38.dp,
                    start = settings.leftInsetDp.dp,
                    end = settings.rightInsetDp.dp,
                    bottom = settings.bottomOffsetDp.dp
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xAA0B1220))
                .border(2.dp, accent.copy(alpha = 0.90f), RoundedCornerShape(16.dp))
        ) {
            // 1. TOP EDGE RESIZE HANDLE (Key Height Scale)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.56f)
                    .height(32.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val s = currentSettings
                            val deltaScale = -dragAmount / with(density) { 210.dp.toPx() }
                            updatedGeometryCallback(
                                (s.heightScale + deltaScale).coerceIn(0.68f, 1.45f),
                                s.bottomOffsetDp,
                                s.leftInsetDp,
                                s.rightInsetDp
                            )
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .width(54.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            // 2. BOTTOM EDGE RESIZE HANDLE (Lifts keys above Circle-to-Search & IME Globe)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.56f)
                    .height(34.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val s = currentSettings
                            val deltaDp = -with(density) { dragAmount.toDp().value }
                            updatedGeometryCallback(
                                s.heightScale,
                                (s.bottomOffsetDp + deltaDp).coerceIn(0f, 160f),
                                s.leftInsetDp,
                                s.rightInsetDp
                            )
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 5.dp)
                        .width(54.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            // 3. LEFT EDGE KEYS-ONLY INSET HANDLE
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(32.dp)
                    .height(90.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val s = currentSettings
                            val deltaDp = with(density) { dragAmount.toDp().value }
                            updatedGeometryCallback(
                                s.heightScale,
                                s.bottomOffsetDp,
                                (s.leftInsetDp + deltaDp).coerceIn(0f, 140f),
                                s.rightInsetDp
                            )
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .width(6.dp)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            // 4. RIGHT EDGE KEYS-ONLY INSET HANDLE
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(32.dp)
                    .height(90.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val s = currentSettings
                            val deltaDp = -with(density) { dragAmount.toDp().value }
                            updatedGeometryCallback(
                                s.heightScale,
                                s.bottomOffsetDp,
                                s.leftInsetDp,
                                (s.rightInsetDp + deltaDp).coerceIn(0f, 140f)
                            )
                        }
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .width(6.dp)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            // 5. CENTER 4-WAY KEYS PAD + ONE-HANDED PRESETS + RESET / DONE
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.copy(alpha = 0.22f))
                        .border(1.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val s = currentSettings
                                val dxDp = with(density) { dragAmount.x.toDp().value }
                                val dyDp = -with(density) { dragAmount.y.toDp().value }
                                val totalHoriz = s.leftInsetDp + s.rightInsetDp
                                val newLeft: Float
                                val newRight: Float
                                if (totalHoriz > 1f) {
                                    newLeft = (s.leftInsetDp + dxDp).coerceIn(0f, totalHoriz)
                                    newRight = (totalHoriz - newLeft).coerceIn(0f, 140f)
                                } else if (dxDp > 0f) {
                                    newLeft = (s.leftInsetDp + dxDp).coerceIn(0f, 140f)
                                    newRight = s.rightInsetDp
                                } else {
                                    newLeft = s.leftInsetDp
                                    newRight = (s.rightInsetDp - dxDp).coerceIn(0f, 140f)
                                }
                                updatedGeometryCallback(
                                    s.heightScale,
                                    (s.bottomOffsetDp + dyDp).coerceIn(0f, 160f),
                                    newLeft,
                                    newRight
                                )
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeyboardIcons.MoveFourWay(Modifier.size(18.dp), Color.White)
                        Column {
                            Text(
                                text = "Resize Keys Only (Base Stays Full-Width)",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "H ${(settings.heightScale * 100).roundToInt()}% · ↑${settings.bottomOffsetDp.roundToInt()}dp · L${settings.leftInsetDp.roundToInt()} / R${settings.rightInsetDp.roundToInt()}",
                                color = Color.White.copy(alpha = 0.74f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // One-Handed Quick Snap Row: Left | Full | Right | Reset | Done
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallSnapPill("← Left") {
                        onHaptic()
                        onUpdateGeometry(settings.heightScale, settings.bottomOffsetDp, 0f, 72f)
                    }
                    SmallSnapPill("Full") {
                        onHaptic()
                        onUpdateGeometry(settings.heightScale, settings.bottomOffsetDp, 0f, 0f)
                    }
                    SmallSnapPill("Right →") {
                        onHaptic()
                        onUpdateGeometry(settings.heightScale, settings.bottomOffsetDp, 72f, 0f)
                    }
                    SmallSnapPill("Reset") { onReset() }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accent)
                            .clickable { onDone() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Done ✓",
                            color = Color(0xFF04111D),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallSnapPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Individual Gboard-parity Key (V4):
 * - Instant `onInstantKeyDownHaptic()` fired synchronously on `awaitFirstDown` (0ms Gboard feel!)
 * - Guaranteed `try { ... } finally { ... }` + `DisposableEffect` cleanup so popups NEVER stick!
 * - Fast Pop-Out Darker Rounded Box (same `7.5.dp` corner radius) on key press that pops up fast (`45ms`)
 *   and snaps back down fast (`55ms`) on release — even on super-fast taps!
 * - Horizontal Expansion Strip on hold (`195ms`) where sliding left or right smoothly selects alternates!
 * - Backspace swipe-left word deletion + Spacebar cursor trackpad + Glide-typing handoff!
 */
@Composable
private fun GboardKeyItem(
    keyId: String,
    spec: KeySpec,
    keyHorizontalFraction: Float,
    touchCellHeightDp: Float,
    verticalPadDp: Float,
    extraStartVisualPad: Dp,
    extraEndVisualPad: Dp,
    showKeyBorders: Boolean,
    glideEnabled: Boolean,
    swipeBackspaceWords: Boolean,
    shiftState: ShiftState,
    imeAction: Int,
    palette: KeyboardVisualPalette,
    rolloverCoordinator: RolloverKeyCoordinator,
    modifier: Modifier = Modifier,
    onInstantKeyDownHaptic: () -> Unit,
    onTickHaptic: () -> Unit,
    onHoldHaptic: () -> Unit,
    onTapPrimary: () -> Unit,
    onLongPressLanguageGlobe: () -> Unit = {},
    onCommitAlternate: (String) -> Unit,
    onRepeatBackspace: () -> Unit,
    onSwipeDeleteWord: () -> Unit,
    onSpaceCursorStep: (Int) -> Unit,
    onGlideMoveRoot: (Offset) -> String?,
    onGlideVisitedUpdate: (List<String>) -> Unit,
    onGlideFinish: (List<String>) -> Unit,
    onGlideCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val keyToken = remember(keyId) { Any() }

    var isPressed by remember(keyId) { mutableStateOf(false) }
    var popHoldLatch by remember(keyId) { mutableStateOf(false) }
    var showAlternatesPopup by remember(keyId) { mutableStateOf(false) }
    var selectedAltIndex by remember(keyId) { mutableIntStateOf(0) }
    var visualWidthPx by remember { mutableFloatStateOf(0f) }
    var keyRootOrigin by remember { mutableStateOf(Offset.Zero) }

    // Safety cleanup if key leaves composition mid-touch
    DisposableEffect(keyId) {
        onDispose {
            isPressed = false
            popHoldLatch = false
            showAlternatesPopup = false
            rolloverCoordinator.clearIfOwner(keyToken)
        }
    }

    val updatedOnInstantDown by rememberUpdatedState(onInstantKeyDownHaptic)
    val updatedOnTick by rememberUpdatedState(onTickHaptic)
    val updatedOnHoldHaptic by rememberUpdatedState(onHoldHaptic)
    val updatedOnTap by rememberUpdatedState(onTapPrimary)
    val updatedOnLongGlobe by rememberUpdatedState(onLongPressLanguageGlobe)
    val updatedOnAlt by rememberUpdatedState(onCommitAlternate)
    val updatedOnRepeatBs by rememberUpdatedState(onRepeatBackspace)
    val updatedOnSwipeWordBs by rememberUpdatedState(onSwipeDeleteWord)
    val updatedOnSpaceStep by rememberUpdatedState(onSpaceCursorStep)
    val updatedOnGlideMove by rememberUpdatedState(onGlideMoveRoot)
    val updatedOnGlideUpdate by rememberUpdatedState(onGlideVisitedUpdate)
    val updatedOnGlideFinish by rememberUpdatedState(onGlideFinish)
    val updatedOnGlideCancel by rememberUpdatedState(onGlideCancel)
    val updatedSpec by rememberUpdatedState(spec)
    val updatedGlideEnabled by rememberUpdatedState(glideEnabled)
    val updatedSwipeBsWords by rememberUpdatedState(swipeBackspaceWords)

    // Fast upward pop animation (`0f` = resting inside key, `1f` = popped out fast above key)
    val showPopBox = (isPressed || popHoldLatch) &&
        (spec.role == KeyRole.CHAR || spec.role == KeyRole.COMMA || spec.role == KeyRole.PERIOD || showAlternatesPopup)

    val popProgress by animateFloatAsState(
        targetValue = if (showPopBox) 1f else 0f,
        animationSpec = if (showPopBox) {
            tween(durationMillis = 48, easing = LinearOutSlowInEasing)
        } else {
            tween(durationMillis = 62, easing = FastOutLinearInEasing)
        },
        label = "KeyFastPopOut"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "KeyPressScale"
    )

    // Exact same corner radius for both the keycap AND the fast pop-out darker rounded box!
    val keyCornerRadius = 7.5.dp
    val keyShape = RoundedCornerShape(keyCornerRadius)

    val fillBrush = when {
        isPressed -> palette.letterKeyPressedFill
        spec.role == KeyRole.ENTER_ACTION -> palette.actionKeyFill
        spec.role == KeyRole.SHIFT && shiftState != ShiftState.OFF -> palette.letterKeyPressedFill
        spec.isModifierStyle -> palette.modifierKeyFill
        else -> palette.letterKeyFill
    }

    val cellStepPx = with(density) { 33.dp.toPx() }
    val cursorStepPx = with(density) { 14.dp.toPx() }
    val wordDeleteStepPx = with(density) { 38.dp.toPx() }
    val glideThresholdPx = with(density) { 26.dp.toPx() }

    // Determine horizontal strip anchor direction so edge keys expand inward cleanly without clipping
    val expandRightward = keyHorizontalFraction <= 0.62f

    Box(
        modifier = modifier
            .height(touchCellHeightDp.dp)
            .zIndex(if (popProgress > 0.01f || showAlternatesPopup) 40f else 1f)
            .onGloballyPositioned { coords ->
                keyRootOrigin = coords.boundsInRoot().topLeft
            }
            .pointerInput(keyId, spec.role, spec.label) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    // 1. ZERO-LATENCY HARDWARE HAPTIC RIGHT ON FINGER DOWN!
                    updatedOnInstantDown()

                    val pressStartMs = System.currentTimeMillis()
                    isPressed = true
                    popHoldLatch = true
                    showAlternatesPopup = false
                    selectedAltIndex = 0

                    var holdJob: Job? = null
                    var repeatJob: Job? = null
                    var latchReleaseJob: Job? = null

                    try {
                        val startPos = down.position
                        var spaceDragAccum = 0f
                        var spaceMovedCursor = false
                        var bsSwipeAccum = 0f
                        var bsSwipedWord = false
                        var globeLongHandled = false
                        var committedEarlyByRollover = false
                        var isGliding = false
                        val glideVisited = mutableListOf<String>()
                        if (updatedSpec.role == KeyRole.CHAR && updatedSpec.commitText.length == 1 && updatedSpec.commitText[0].isLetter()) {
                            glideVisited.add(updatedSpec.commitText)
                        }

                        val canRolloverFlush = updatedSpec.role == KeyRole.CHAR ||
                            updatedSpec.role == KeyRole.COMMA ||
                            updatedSpec.role == KeyRole.PERIOD ||
                            updatedSpec.role == KeyRole.SPACE

                        rolloverCoordinator.onKeyDown(
                            token = keyToken,
                            onEarlyFlush = if (canRolloverFlush) {
                                {
                                    if (!committedEarlyByRollover && !showAlternatesPopup && !spaceMovedCursor && !isGliding) {
                                        committedEarlyByRollover = true
                                        holdJob?.cancel()
                                        updatedOnTap()
                                    }
                                }
                            } else null
                        )

                        if (updatedSpec.role == KeyRole.BACKSPACE) {
                            updatedOnRepeatBs()
                            repeatJob = scope.launch {
                                delay(340L)
                                var delayMs = 58L
                                while (true) {
                                    if (!bsSwipedWord) {
                                        updatedOnTick()
                                        updatedOnRepeatBs()
                                    }
                                    delay(delayMs)
                                    if (delayMs > 26L) delayMs -= 4L
                                }
                            }
                        } else if (updatedSpec.role == KeyRole.LANGUAGE_GLOBE) {
                            holdJob = scope.launch {
                                delay(300L)
                                globeLongHandled = true
                                updatedOnHoldHaptic()
                                updatedOnLongGlobe()
                            }
                        } else if (updatedSpec.alternates.isNotEmpty()) {
                            holdJob = scope.launch {
                                delay(195L)
                                if (!committedEarlyByRollover && !isGliding) {
                                    rolloverCoordinator.clearIfOwner(keyToken)
                                    showAlternatesPopup = true
                                    selectedAltIndex = 0
                                    updatedOnHoldHaptic()
                                }
                            }
                        }

                        var pointerUp = false
                        var cancelled = false

                        while (!pointerUp && !cancelled) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                cancelled = true
                            } else if (!change.pressed) {
                                change.consume()
                                pointerUp = true
                            } else {
                                val dx = change.position.x - startPos.x
                                val dy = change.position.y - startPos.y
                                val dist = hypot(dx, dy)

                                when {
                                    updatedSpec.role == KeyRole.SPACE && !committedEarlyByRollover -> {
                                        val diff = change.position.x - change.previousPosition.x
                                        spaceDragAccum += diff
                                        while (abs(spaceDragAccum) >= cursorStepPx) {
                                            val dir = if (spaceDragAccum > 0f) 1 else -1
                                            spaceDragAccum -= dir * cursorStepPx
                                            spaceMovedCursor = true
                                            rolloverCoordinator.clearIfOwner(keyToken)
                                            updatedOnTick()
                                            updatedOnSpaceStep(dir)
                                        }
                                    }

                                    updatedSpec.role == KeyRole.BACKSPACE && updatedSwipeBsWords -> {
                                        val diff = change.position.x - change.previousPosition.x
                                        if (dx < -wordDeleteStepPx * 0.7f) {
                                            repeatJob?.cancel()
                                            bsSwipeAccum += -diff
                                            while (bsSwipeAccum >= wordDeleteStepPx) {
                                                bsSwipeAccum -= wordDeleteStepPx
                                                bsSwipedWord = true
                                                updatedOnTick()
                                                updatedOnSwipeWordBs()
                                            }
                                        }
                                    }

                                    showAlternatesPopup && updatedSpec.alternates.isNotEmpty() -> {
                                        // Slide left or right across the horizontal strip!
                                        val count = updatedSpec.alternates.size
                                        val signedSteps = if (expandRightward) {
                                            (dx / cellStepPx).roundToInt()
                                        } else {
                                            (-dx / cellStepPx).roundToInt()
                                        }
                                        val nextIdx = signedSteps.coerceIn(0, count - 1)
                                        if (nextIdx != selectedAltIndex) {
                                            selectedAltIndex = nextIdx
                                            updatedOnTick()
                                        }
                                    }

                                    updatedGlideEnabled &&
                                        !committedEarlyByRollover &&
                                        !showAlternatesPopup &&
                                        updatedSpec.role == KeyRole.CHAR &&
                                        updatedSpec.commitText.length == 1 &&
                                        updatedSpec.commitText[0].isLetter() &&
                                        dist >= glideThresholdPx -> {
                                        if (!isGliding) {
                                            isGliding = true
                                            holdJob?.cancel()
                                            popHoldLatch = false
                                            rolloverCoordinator.clearIfOwner(keyToken)
                                        }
                                        val rootPt = keyRootOrigin + change.position
                                        val hitChar = updatedOnGlideMove(rootPt)
                                        if (hitChar != null && glideVisited.lastOrNull() != hitChar) {
                                            glideVisited.add(hitChar)
                                            updatedOnGlideUpdate(glideVisited)
                                        }
                                    }
                                }
                                change.consume()
                            }
                        }

                        holdJob?.cancel()
                        repeatJob?.cancel()
                        rolloverCoordinator.clearIfOwner(keyToken)

                        val wasPopupOpen = showAlternatesPopup
                        val chosenAlt = updatedSpec.alternates.getOrNull(selectedAltIndex)

                        isPressed = false
                        showAlternatesPopup = false

                        // Ensure fast 20ms taps still render the crisp upward pop & fast return!
                        val elapsed = System.currentTimeMillis() - pressStartMs
                        if (!isGliding && !wasPopupOpen && elapsed < 52L) {
                            latchReleaseJob = scope.launch {
                                delay(52L - elapsed)
                                popHoldLatch = false
                            }
                        } else {
                            popHoldLatch = false
                        }

                        if (cancelled) {
                            if (isGliding) updatedOnGlideCancel()
                        } else if (pointerUp && !committedEarlyByRollover) {
                            when {
                                isGliding -> updatedOnGlideFinish(glideVisited)
                                updatedSpec.role == KeyRole.BACKSPACE -> Unit
                                updatedSpec.role == KeyRole.LANGUAGE_GLOBE && globeLongHandled -> Unit
                                updatedSpec.role == KeyRole.SPACE && spaceMovedCursor -> Unit
                                wasPopupOpen && chosenAlt != null -> updatedOnAlt(chosenAlt)
                                else -> updatedOnTap()
                            }
                        }
                    } finally {
                        holdJob?.cancel()
                        repeatJob?.cancel()
                        rolloverCoordinator.clearIfOwner(keyToken)
                        isPressed = false
                        showAlternatesPopup = false
                        if (latchReleaseJob == null) {
                            popHoldLatch = false
                        }
                    }
                }
            }
            .padding(
                start = 2.dp + extraStartVisualPad,
                end = 2.dp + extraEndVisualPad,
                top = verticalPadDp.dp,
                bottom = verticalPadDp.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        // =========================================================================
        // 1. FAST POP-OUT DARKER ROUNDED BOX / HORIZONTAL ALTERNATES STRIP (Behind/Above Key!)
        // =========================================================================
        if (popProgress > 0.01f) {
            val visualKeyHeightDpVal = (touchCellHeightDp - verticalPadDp * 2f).coerceAtLeast(26f)
            val popRisePx = with(density) { (visualKeyHeightDpVal + 8f).dp.toPx() } * popProgress
            val keyWidthDp = with(density) { visualWidthPx.toDp() }.coerceAtLeast(30.dp)

            if (showAlternatesPopup && spec.alternates.isNotEmpty()) {
                // Horizontal expansion strip anchored smoothly from the key!
                val orderedAlts = if (expandRightward) spec.alternates else spec.alternates.reversed()
                val visualHighlightIdx = if (expandRightward) {
                    selectedAltIndex
                } else {
                    (spec.alternates.lastIndex - selectedAltIndex).coerceIn(0, spec.alternates.lastIndex)
                }

                Box(
                    modifier = Modifier
                        .align(if (expandRightward) Alignment.TopStart else Alignment.TopEnd)
                        .offset { IntOffset(0, -popRisePx.roundToInt()) }
                        .wrapContentWidth(
                            align = if (expandRightward) Alignment.Start else Alignment.End,
                            unbounded = true
                        )
                        .zIndex(50f)
                ) {
                    Row(
                        modifier = Modifier
                            .height(visualKeyHeightDpVal.dp + 4.dp)
                            .shadow(10.dp, keyShape)
                            .clip(keyShape)
                            .background(palette.keyPopDarkerFill)
                            .border(0.85.dp, palette.popupBorder, keyShape)
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        orderedAlts.forEachIndexed { idx, alt ->
                            val highlighted = idx == visualHighlightIdx
                            Box(
                                modifier = Modifier
                                    .width(31.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (highlighted) palette.popupHighlightFill
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = alt,
                                    color = if (highlighted) palette.popupHighlightText else palette.primaryText,
                                    fontSize = 17.sp,
                                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            } else {
                // Single-key fast pop-out slightly darker rounded box with EXACT SAME corner radius (`7.5.dp`)!
                Box(
                    modifier = Modifier
                        .requiredWidth(keyWidthDp + 4.dp)
                        .height(visualKeyHeightDpVal.dp + 4.dp)
                        .graphicsLayer {
                            translationY = -popRisePx
                            val s = 0.92f + 0.12f * popProgress
                            scaleX = s
                            scaleY = s
                            alpha = popProgress.coerceIn(0f, 1f)
                        }
                        .shadow(8.dp, keyShape)
                        .clip(keyShape)
                        .background(palette.keyPopDarkerFill)
                        .border(0.75.dp, palette.popupBorder, keyShape)
                        .zIndex(45f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spec.label,
                        color = palette.primaryText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        // =========================================================================
        // 2. INNER VISUAL KEYCAP — invisible border by default just like Gboard!
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    visualWidthPx = coords.boundsInParent().width
                }
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(keyShape)
                .background(fillBrush)
                .then(
                    if (showKeyBorders) {
                        Modifier.border(0.65.dp, palette.keyBorderBrush, keyShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (spec.topHint != null) {
                Text(
                    text = spec.topHint,
                    color = palette.secondaryText,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 1.5.dp, end = 3.dp)
                )
            }

            when (spec.role) {
                KeyRole.SHIFT -> {
                    KeyboardIcons.ShiftArrow(
                        modifier = Modifier.size(20.dp),
                        color = palette.primaryText,
                        filled = shiftState != ShiftState.OFF,
                        capsLock = shiftState == ShiftState.CAPS_LOCK
                    )
                }
                KeyRole.BACKSPACE -> {
                    KeyboardIcons.Backspace(
                        modifier = Modifier.size(21.dp),
                        color = palette.primaryText
                    )
                }
                KeyRole.LANGUAGE_GLOBE -> {
                    KeyboardIcons.Globe(
                        modifier = Modifier.size(19.dp),
                        color = palette.primaryText
                    )
                }
                KeyRole.EMOJI -> {
                    KeyboardIcons.EmojiSmiley(
                        modifier = Modifier.size(19.dp),
                        color = palette.primaryText
                    )
                }
                KeyRole.ENTER_ACTION -> {
                    KeyboardIcons.ActionGlyph(
                        modifier = Modifier.size(20.dp),
                        color = palette.actionText,
                        imeAction = imeAction
                    )
                }
                KeyRole.SPACE -> {
                    Text(
                        text = spec.label,
                        color = palette.primaryText.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                else -> {
                    val fontSp = when {
                        spec.label.length >= 3 -> 11.5f
                        spec.label.length == 2 -> 14.5f
                        else -> 19f
                    }
                    Text(
                        text = spec.label,
                        color = palette.primaryText,
                        fontSize = fontSp.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

/**
 * Full Gboard-Style Text Editing & Selection D-Pad Panel (`<I>` Toolbar Action).
 */
@Composable
private fun TextEditingCursorPanel(
    settings: KeyboardSettingsState,
    palette: KeyboardVisualPalette,
    panelHeightDp: Float,
    onDownHaptic: () -> Unit,
    onCommand: (TextEditCommand, Boolean) -> Unit,
    onReturnToLetters: () -> Unit
) {
    var selecting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left D-Pad Cluster
            Column(
                modifier = Modifier
                    .weight(1.55f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    EditActionButton("⇤ Start", palette, Modifier.weight(1f)) {
                        onDownHaptic()
                        onCommand(TextEditCommand.LINE_START, selecting)
                    }
                    EditActionButton("↑", palette, Modifier.weight(1f)) {
                        onDownHaptic()
                        onCommand(TextEditCommand.UP, selecting)
                    }
                    EditActionButton("End ⇥", palette, Modifier.weight(1f)) {
                        onDownHaptic()
                        onCommand(TextEditCommand.LINE_END, selecting)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    EditActionButton("←", palette, Modifier.weight(1f)) {
                        onDownHaptic()
                        onCommand(TextEditCommand.LEFT, selecting)
                    }
                    EditActionButton(
                        label = if (selecting) "Select ●" else "Select",
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        highlighted = selecting
                    ) {
                        onDownHaptic()
                        selecting = !selecting
                    }
                    EditActionButton("→", palette, Modifier.weight(1f)) {
                        onDownHaptic()
                        onCommand(TextEditCommand.RIGHT, selecting)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    EditActionButton(
                        label = if (settings.language == KeyboardLanguage.ARABIC) "أ ب ج" else "ABC",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    ) {
                        onDownHaptic()
                        onReturnToLetters()
                    }
                    EditActionButton("↓", palette, Modifier.weight(1f)) {
                        onDownHaptic()
                        onCommand(TextEditCommand.DOWN, selecting)
                    }
                    EditActionButton("⌫", palette, Modifier.weight(1f)) {
                        onDownHaptic()
                        onCommand(TextEditCommand.BACKSPACE, false)
                    }
                }
            }

            // Right Clipboard & Selection Action Column
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                EditActionButton("Select All", palette, Modifier.fillMaxWidth().weight(1f)) {
                    onDownHaptic()
                    onCommand(TextEditCommand.SELECT_ALL, false)
                }
                EditActionButton("Copy", palette, Modifier.fillMaxWidth().weight(1f)) {
                    onDownHaptic()
                    onCommand(TextEditCommand.COPY, false)
                }
                EditActionButton("Cut", palette, Modifier.fillMaxWidth().weight(1f)) {
                    onDownHaptic()
                    onCommand(TextEditCommand.CUT, false)
                }
                EditActionButton("Paste", palette, Modifier.fillMaxWidth().weight(1f), highlighted = true) {
                    onDownHaptic()
                    onCommand(TextEditCommand.PASTE, false)
                }
            }
        }
    }
}

@Composable
private fun EditActionButton(
    label: String,
    palette: KeyboardVisualPalette,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (highlighted) SolidColor(palette.popupHighlightFill)
                else palette.letterKeyFill
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (highlighted) palette.popupHighlightText else palette.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Built-In Gboard-style Clipboard Manager Panel.
 */
@Composable
private fun ClipboardPickerPanel(
    settings: KeyboardSettingsState,
    palette: KeyboardVisualPalette,
    panelHeightDp: Float,
    onDownHaptic: () -> Unit,
    onPasteClip: (String) -> Unit,
    onTogglePinClip: (String) -> Unit,
    onRemoveClip: (String) -> Unit,
    onClearUnpinned: () -> Unit,
    onReturnToLetters: () -> Unit
) {
    val orderedClips = remember(settings.clipboardHistory, settings.pinnedClips) {
        val pinnedList = settings.clipboardHistory.filter { it in settings.pinnedClips } +
            settings.pinnedClips.filterNot { it in settings.clipboardHistory }
        val unpinnedList = settings.clipboardHistory.filterNot { it in settings.pinnedClips }
        pinnedList.distinct() + unpinnedList
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeyboardIcons.Clipboard(Modifier.size(16.dp), palette.primaryText)
                Text(
                    text = "Clipboard (${orderedClips.size})",
                    color = palette.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (orderedClips.any { it !in settings.pinnedClips }) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.modifierKeyFill)
                        .clickable {
                            onDownHaptic()
                            onClearUnpinned()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Clear recent",
                        color = palette.secondaryText,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (orderedClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Copied text appears here automatically",
                    color = palette.secondaryText,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(orderedClips, key = { it.hashCode() }) { clip ->
                    val isPinned = clip in settings.pinnedClips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (isPinned) SolidColor(palette.popupHighlightFill.copy(alpha = 0.22f))
                                else palette.letterKeyFill
                            )
                            .clickable {
                                onDownHaptic()
                                onPasteClip(clip)
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = clip,
                            color = palette.primaryText,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onDownHaptic()
                                    onTogglePinClip(clip)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            KeyboardIcons.Pin(
                                modifier = Modifier.size(14.dp),
                                color = if (isPinned) palette.primaryText else palette.secondaryText,
                                filled = isPinned
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onDownHaptic()
                                    onRemoveClip(clip)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "×",
                                color = palette.secondaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(palette.modifierKeyFill)
                    .clickable {
                        onDownHaptic()
                        onReturnToLetters()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (settings.language == KeyboardLanguage.ARABIC) "أ ب ج" else "ABC",
                    color = palette.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Full Emoji + Kaomoji Picker Panel with Quick Keyword Filter Pills, Recents, Categories, and Backspace.
 */
@Composable
private fun EmojiPickerPanel(
    settings: KeyboardSettingsState,
    palette: KeyboardVisualPalette,
    panelHeightDp: Float,
    onDownHaptic: () -> Unit,
    onPickEmoji: (String) -> Unit,
    onBackspace: () -> Unit,
    onReturnToLetters: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) }
    var filterTag by remember { mutableStateOf("") }

    val quickTags = remember {
        listOf("fire", "laugh", "love", "skull", "cool", "pray", "party", "sad")
    }

    val isKaomojiTab = selectedTab > 0 &&
        EmojiCatalog.categories.getOrNull(selectedTab - 1)?.id == "kaomoji"

    val activeEmojis = remember(selectedTab, filterTag, settings.recentEmojis) {
        if (filterTag.isNotBlank()) {
            EmojiCatalog.search(filterTag)
        } else if (selectedTab == 0) {
            settings.recentEmojis
        } else {
            EmojiCatalog.categories.getOrElse(selectedTab - 1) { EmojiCatalog.categories.first() }.emojis
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiTabChip(
                icon = "🕒",
                selected = filterTag.isEmpty() && selectedTab == 0,
                palette = palette,
                onClick = {
                    onDownHaptic()
                    filterTag = ""
                    selectedTab = 0
                }
            )
            EmojiCatalog.categories.forEachIndexed { idx, cat ->
                EmojiTabChip(
                    icon = cat.icon,
                    selected = filterTag.isEmpty() && selectedTab == idx + 1,
                    palette = palette,
                    onClick = {
                        onDownHaptic()
                        filterTag = ""
                        selectedTab = idx + 1
                    }
                )
            }
        }

        // Quick Mood Search Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickTags.forEach { tag ->
                val active = filterTag == tag
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (active) SolidColor(palette.popupHighlightFill)
                            else palette.modifierKeyFill
                        )
                        .clickable {
                            onDownHaptic()
                            filterTag = if (active) "" else tag
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$tag",
                        color = if (active) palette.popupHighlightText else palette.secondaryText,
                        fontSize = 10.sp
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isKaomojiTab && filterTag.isEmpty()) 3 else 8),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(activeEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isKaomojiTab && filterTag.isEmpty()) palette.letterKeyFill
                            else SolidColor(Color.Transparent)
                        )
                        .clickable {
                            onDownHaptic()
                            onPickEmoji(emoji)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        color = palette.primaryText,
                        fontSize = if (emoji.length > 3) 12.sp else 21.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(palette.modifierKeyFill)
                    .clickable {
                        onDownHaptic()
                        onReturnToLetters()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (settings.language == KeyboardLanguage.ARABIC) "أ ب ج" else "ABC",
                    color = palette.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(palette.modifierKeyFill)
                    .clickable {
                        onDownHaptic()
                        onBackspace()
                    },
                contentAlignment = Alignment.Center
            ) {
                KeyboardIcons.Backspace(Modifier.size(20.dp), palette.primaryText)
            }
        }
    }
}

@Composable
private fun EmojiTabChip(
    icon: String,
    selected: Boolean,
    palette: KeyboardVisualPalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
                if (selected) palette.popupHighlightFill.copy(alpha = 0.36f)
                else Color.Transparent
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, color = palette.primaryText, fontSize = 15.sp)
    }
}

/**
 * Slide-out Glass Live Translator Panel (English <-> Arabic).
 */
@Composable
fun TranslatorPanel(
    palette: KeyboardVisualPalette,
    panelHeightDp: Float,
    onDownHaptic: () -> Unit,
    onCommitTranslation: (String) -> Unit,
    onReturnToLetters: () -> Unit
) {
    var fromEnglish by remember { mutableStateOf(true) }
    var inputText by remember { mutableStateOf("") }
    val translatedText = remember(inputText, fromEnglish) {
        if (inputText.isBlank()) ""
        else com.multios.keyboard.engine.LiveTranslatorEngine.translate(inputText, fromEnglish)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (fromEnglish) "English" else "العربية",
                    color = palette.popupHighlightText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(palette.keyPopDarkerFill)
                        .clickable {
                            onDownHaptic()
                            fromEnglish = !fromEnglish
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⇄", color = palette.primaryText, fontSize = 14.sp)
                }
                Text(
                    text = if (fromEnglish) "العربية" else "English",
                    color = palette.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.keyPopDarkerFill)
                    .clickable {
                        onDownHaptic()
                        onReturnToLetters()
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Close",
                    color = palette.secondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Translation Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.keyPopDarkerFill)
                .border(0.75.dp, palette.popupBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (translatedText.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Translation:",
                        color = palette.secondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = translatedText,
                        color = palette.primaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = if (fromEnglish) "Type in English to translate to Arabic..." else "اكتب بالعربية للترجمة إلى الإنجليزية...",
                    color = palette.secondaryText,
                    fontSize = 12.sp
                )
            }
        }

        // Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (translatedText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.popupHighlightFill)
                        .clickable {
                            onDownHaptic()
                            onCommitTranslation(translatedText)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Insert Translation",
                        color = palette.popupHighlightText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick Presets Chips
            val quickPhrases = if (fromEnglish) {
                listOf("Hello", "Thank you", "How are you", "Yes", "No")
            } else {
                listOf("مرحبا", "شكرا", "كيف حالك", "نعم", "تمام")
            }
            quickPhrases.forEach { phrase ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.keyPopDarkerFill)
                        .clickable {
                            onDownHaptic()
                            inputText = phrase
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = phrase,
                        color = palette.primaryText,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

