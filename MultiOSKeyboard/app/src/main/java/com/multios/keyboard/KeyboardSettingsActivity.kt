package com.multios.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.multios.keyboard.data.KeyboardLanguage
import com.multios.keyboard.data.KeyboardMaterialMode
import com.multios.keyboard.data.KeyboardPrefsRepository
import com.multios.keyboard.data.KeyboardTintPreset
import com.multios.keyboard.engine.AutocorrectEngine
import com.multios.keyboard.layout.KeyboardLayer
import com.multios.keyboard.layout.ShiftState
import com.multios.keyboard.ui.MultiOsKeyboardView
import kotlin.math.roundToInt

/**
 * Companion Setup & Real-Time Design Customizer Activity for `MultiOS Keyboard`.
 *
 * Lets the user:
 * 1. Enable MultiOS Keyboard in Android System Settings & switch to it in 2 taps.
 * 2. Choose between `Liquid Glass Keys & Base`, `Blurred Keys`, or `All Pastel`.
 * 3. Change the Tint Color (curated presets + full continuous Custom Hue & Tint Intensity sliders).
 * 4. Toggle the Number Row, resize Keyboard Height (`78%..135%`), switch English / العربية, and toggle Autocorrect.
 * 5. Preview the live keyboard right on screen AND test typing in a sandbox text box!
 */
class KeyboardSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeyboardPrefsRepository.init(this)
        enableEdgeToEdge()
        setContent {
            KeyboardSettingsScreen()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyboardSettingsScreen() {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val settings by KeyboardPrefsRepository.state.collectAsState()

    var isImeEnabled by remember { mutableStateOf(checkImeEnabled(context)) }
    var testInput by remember { mutableStateOf("") }
    var previewLayer by remember { mutableStateOf(KeyboardLayer.LETTERS) }
    var previewShift by remember { mutableStateOf(ShiftState.ONCE) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isImeEnabled = checkImeEnabled(context)
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    val accent = if (settings.tintPreset == KeyboardTintPreset.PURE_FROST) {
        Color(0xFF7DD3FC)
    } else {
        settings.resolvedTintColor
    }

    val previewSuggestions = remember(testInput, settings.language, settings.autocorrectEnabled) {
        val lastWord = testInput.takeLastWhile { it.isLetterOrDigit() || it == '\'' }
        AutocorrectEngine.computeSuggestions(
            currentWord = lastWord,
            previousWord = "",
            language = settings.language,
            autocorrectEnabled = settings.autocorrectEnabled
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080B12), Color(0xFF0E131F), Color(0xFF07090F))
                )
            )
            .statusBarsPadding()
            .imePadding()
    ) {
        // Scrollable Customizer Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "MultiOS Keyboard",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "English & العربية · Gboard Layout · Liquid Glass",
                    color = Color.White.copy(alpha = 0.56f),
                    fontSize = 13.sp
                )
            }

            // 1. System Activation Card
            GlassSettingCard {
                Text(
                    text = "System Setup",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionPillButton(
                        label = if (isImeEnabled) "1. Enabled ✓" else "1. Enable Keyboard",
                        highlighted = !isImeEnabled,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    )
                    ActionPillButton(
                        label = "2. Select Keyboard",
                        highlighted = isImeEnabled,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        }
                    )
                }
            }

            // 2. Live Test Input Field
            GlassSettingCard {
                OutlinedTextField(
                    value = testInput,
                    onValueChange = { testInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Tap here to test system keyboard (English / العربية)...",
                            color = Color.White.copy(alpha = 0.42f),
                            fontSize = 13.5.sp
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        cursorColor = accent
                    )
                )
            }

            // 3. Material Mode Selector (Liquid Glass Keys & Base | Blurred Keys | All Pastel)
            GlassSettingCard {
                Text(
                    text = "Material Design",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyboardMaterialMode.entries.forEach { mode ->
                        val selected = settings.materialMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) accent.copy(alpha = 0.20f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (selected) 1.2.dp else 0.7.dp,
                                    color = if (selected) accent else Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { KeyboardPrefsRepository.setMaterialMode(mode) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = mode.title,
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = mode.subtitle,
                                    color = Color.White.copy(alpha = 0.56f),
                                    fontSize = 12.sp
                                )
                            }
                            if (selected) {
                                Text("✓", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Tint Color Presets + Custom Hue & Tint Intensity
            GlassSettingCard {
                Text(
                    text = "Glass & Pastel Tint",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KeyboardTintPreset.entries.forEach { preset ->
                        val selected = settings.tintPreset == preset
                        val swatchColor = if (preset == KeyboardTintPreset.CUSTOM) {
                            Color.hsv(settings.customHue, 0.65f, 0.98f)
                        } else {
                            preset.color
                        }
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) swatchColor.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    width = if (selected) 1.2.dp else 0.7.dp,
                                    color = if (selected) swatchColor else Color.White.copy(alpha = 0.16f),
                                    shape = CircleShape
                                )
                                .clickable { KeyboardPrefsRepository.setTintPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                            )
                            Text(
                                text = preset.label,
                                color = Color.White,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Custom Tint Hue (${settings.resolvedHue.roundToInt()}°)",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.5.sp
                )
                Slider(
                    value = settings.resolvedHue,
                    onValueChange = { KeyboardPrefsRepository.setCustomHue(it) },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = settings.resolvedTintColor,
                        activeTrackColor = settings.resolvedTintColor
                    )
                )

                Text(
                    text = "Tint Intensity (${(settings.tintStrength * 100).roundToInt()}%)",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.5.sp
                )
                Slider(
                    value = settings.tintStrength,
                    onValueChange = { KeyboardPrefsRepository.setTintStrength(it) },
                    valueRange = 0f..0.85f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent
                    )
                )
            }

            // 5. Layout, Number Row, Resizable Height & Languages
            GlassSettingCard {
                Text(
                    text = "Layout & Typing",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                ToggleSettingRow(
                    title = "Number Row",
                    subtitle = "Dedicated 1–0 / ١–٠ row above letters",
                    checked = settings.showNumberRow,
                    accent = accent,
                    onCheckedChange = { KeyboardPrefsRepository.setShowNumberRow(it) }
                )

                ToggleSettingRow(
                    title = "Autocorrection & Smart Lexicon",
                    subtitle = "Protects real English words (made, hell, etc.) while fixing typos",
                    checked = settings.autocorrectEnabled,
                    accent = accent,
                    onCheckedChange = { KeyboardPrefsRepository.setAutocorrectEnabled(it) }
                )

                ToggleSettingRow(
                    title = "Glide / Swipe Typing",
                    subtitle = "Slide across letters to form words fluidly",
                    checked = settings.glideTypingEnabled,
                    accent = accent,
                    onCheckedChange = { KeyboardPrefsRepository.setGlideTypingEnabled(it) }
                )

                ToggleSettingRow(
                    title = "Swipe Left on Backspace Deletes Words",
                    subtitle = "Slide left from ⌫ to erase whole words fast",
                    checked = settings.swipeBackspaceWords,
                    accent = accent,
                    onCheckedChange = { KeyboardPrefsRepository.setSwipeBackspaceWords(it) }
                )

                ToggleSettingRow(
                    title = "Instant Zero-Latency Key Haptics",
                    subtitle = "Hardware impulse fired immediately on touch-down",
                    checked = settings.hapticsEnabled,
                    accent = accent,
                    onCheckedChange = { KeyboardPrefsRepository.setHapticsEnabled(it) }
                )

                if (settings.hapticsEnabled) {
                    Text(
                        text = if (settings.hapticStrengthMs == 0) {
                            "Haptic Impulse: Native Gboard Crisp Tick (0ms)"
                        } else {
                            "Haptic Pulse Strength: ${settings.hapticStrengthMs} ms"
                        },
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 12.5.sp
                    )
                    Slider(
                        value = settings.hapticStrengthMs.toFloat(),
                        onValueChange = { KeyboardPrefsRepository.setHapticStrengthMs(it.roundToInt()) },
                        valueRange = 0f..25f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }

                ToggleSettingRow(
                    title = "Visible Key Borders",
                    subtitle = "Off by default for seamless Gboard borderless feel",
                    checked = settings.showKeyBorders,
                    accent = accent,
                    onCheckedChange = { KeyboardPrefsRepository.setShowKeyBorders(it) }
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Keyboard Height (${(settings.heightScale * 100).roundToInt()}%)",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 13.sp
                )
                Slider(
                    value = settings.heightScale,
                    onValueChange = { KeyboardPrefsRepository.setHeightScale(it) },
                    valueRange = 0.68f..1.45f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent
                    )
                )

                Text(
                    text = "Bottom Lift Clearance (${settings.bottomOffsetDp.roundToInt()}dp — clears Circle to Search & Globe)",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 13.sp
                )
                Slider(
                    value = settings.bottomOffsetDp,
                    onValueChange = { KeyboardPrefsRepository.setBottomOffsetDp(it) },
                    valueRange = 0f..160f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Left Keys Inset (${settings.leftInsetDp.roundToInt()}dp)",
                            color = Color.White.copy(alpha = 0.80f),
                            fontSize = 12.sp
                        )
                        Slider(
                            value = settings.leftInsetDp,
                            onValueChange = {
                                KeyboardPrefsRepository.setSideInsetsDp(it, settings.rightInsetDp)
                            },
                            valueRange = 0f..140f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Right Keys Inset (${settings.rightInsetDp.roundToInt()}dp)",
                            color = Color.White.copy(alpha = 0.80f),
                            fontSize = 12.sp
                        )
                        Slider(
                            value = settings.rightInsetDp,
                            onValueChange = {
                                KeyboardPrefsRepository.setSideInsetsDp(settings.leftInsetDp, it)
                            },
                            valueRange = 0f..140f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KeyboardLanguage.entries.forEach { lang ->
                        ActionPillButton(
                            label = lang.displayName,
                            highlighted = settings.language == lang,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = { KeyboardPrefsRepository.setLanguage(lang) }
                        )
                    }
                }
            }
        }

        // Bottom Live Interactive Preview of MultiOS Keyboard
        MultiOsKeyboardView(
            settings = settings,
            suggestions = previewSuggestions,
            shiftState = previewShift,
            layer = previewLayer,
            onCommitChar = { ch ->
                testInput += ch
                if (previewShift == ShiftState.ONCE) previewShift = ShiftState.OFF
            },
            onCommitGlideWord = { word ->
                val space = if (testInput.isNotEmpty() && !testInput.endsWith(" ")) " " else ""
                testInput += "$space$word"
                if (previewShift == ShiftState.ONCE) previewShift = ShiftState.OFF
            },
            onSelectSuggestion = { word, wasLiteral ->
                if (wasLiteral) KeyboardPrefsRepository.learnUserWord(word)
                val prefix = testInput.dropLastWhile { it.isLetterOrDigit() || it == '\'' }
                testInput = "$prefix$word "
            },
            onBackspace = {
                if (testInput.isNotEmpty()) testInput = testInput.dropLast(1)
            },
            onDeleteWordBack = {
                testInput = testInput.trimEnd().dropLastWhile { !it.isWhitespace() }
            },
            onSpacePress = { testInput += " " },
            onMoveCursorBy = {},
            onEnterAction = { testInput += "\n" },
            onShiftTap = {
                previewShift = when (previewShift) {
                    ShiftState.OFF -> ShiftState.ONCE
                    ShiftState.ONCE -> ShiftState.CAPS_LOCK
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
            },
            onLayerChange = { previewLayer = it },
            onToggleLanguage = { KeyboardPrefsRepository.toggleLanguage() },
            onShowSystemImePicker = {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            },
            onToggleNumberRow = { KeyboardPrefsRepository.setShowNumberRow(!settings.showNumberRow) },
            onUpdateGeometry = { h, b, l, r ->
                KeyboardPrefsRepository.updateKeyboardGeometry(h, b, l, r)
            },
            onResetGeometry = { KeyboardPrefsRepository.resetKeyboardGeometry() },
            onPickEmoji = { emoji ->
                KeyboardPrefsRepository.recordEmojiUsed(emoji)
                testInput += emoji
            },
            onPasteClip = { clip -> testInput += clip },
            onTogglePinClip = { clip -> KeyboardPrefsRepository.togglePinClip(clip) },
            onRemoveClip = { clip -> KeyboardPrefsRepository.removeClip(clip) },
            onClearUnpinnedClips = { KeyboardPrefsRepository.clearUnpinnedClips() },
            onOpenSettings = {}
        )
    }
}

@Composable
private fun GlassSettingCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun ActionPillButton(
    label: String,
    highlighted: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (highlighted) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.07f))
            .border(
                width = 0.9.dp,
                color = if (highlighted) accent else Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(13.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Color.White.copy(alpha = 0.52f), fontSize = 11.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent.copy(alpha = 0.72f)
            )
        )
    }
}

private fun checkImeEnabled(context: Context): Boolean {
    return runCatching {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.enabledInputMethodList.any { it.packageName == context.packageName }
    }.getOrDefault(false)
}
