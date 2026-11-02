package com.galaxy.m21.desktop.ui.settings

import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.LocalLaunchWidgetPicker
import com.galaxy.m21.desktop.hardware.HardwareViewModel
import com.galaxy.m21.desktop.ui.components.CustomLiquidSlider
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.components.SliderGlyphType
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.theme.AppAppearanceMode
import com.galaxy.m21.desktop.ui.theme.ChargingAnimStyle
import com.galaxy.m21.desktop.ui.theme.ClockStylePreset
import com.galaxy.m21.desktop.ui.theme.ColorPalettePreset
import com.galaxy.m21.desktop.ui.theme.DrawerAppAlignment
import com.galaxy.m21.desktop.ui.theme.DrawerKeyboardMode
import com.galaxy.m21.desktop.ui.theme.GeminiEdgeStyle
import com.galaxy.m21.desktop.ui.theme.GlassTintPreset
import com.galaxy.m21.desktop.ui.theme.HomeKeyboardMode
import com.galaxy.m21.desktop.ui.theme.KnobStylePreset
import com.galaxy.m21.desktop.ui.theme.OrientationMode
import com.galaxy.m21.desktop.ui.theme.WelcomeStyle
import com.galaxy.m21.desktop.ui.theme.AppOpeningAnimStyle
import com.galaxy.m21.desktop.ui.theme.VolumeHudStyle
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import com.galaxy.m21.desktop.ui.theme.SliderStylePreset
import com.galaxy.m21.desktop.ui.theme.SwipeActionTarget
import com.galaxy.m21.desktop.ui.theme.WallpaperStyle
import kotlin.math.roundToInt

/** Sidebar categories of the minimal iOS-style 2-pane Settings. */
private enum class SettingsCategory(val title: String) {
    GENERAL("General"),
    APPEARANCE("Appearance"),
    WALLPAPER("Wallpaper"),
    HOME("Home & Widgets"),
    KEYBOARD("Keyboard"),
    GESTURES("Gestures"),
    GEMINI("Gemini"),
    RECENTS("Recents & Privacy"),
    LOCK("Lock & Always-On")
}

/**
 * MultiOS Settings — minimal iOS-style 2-pane layout (sidebar left, inset-grouped detail right).
 */
@Composable
fun SystemSettingsPanel(
    hardwareViewModel: HardwareViewModel,
    onDismiss: () -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val accent = telemetry.resolveAccentColor()
    var selectedName by rememberSaveable { mutableStateOf(SettingsCategory.GENERAL.name) }
    val selected = SettingsCategory.valueOf(selectedName)

    val outerScrim = if (telemetry.liquidGlassEnabled) {
        if (themeColors.isLight) Color(0xFFE2E8F0).copy(alpha = 0.40f)
        else Color(0xFF04070D).copy(alpha = 0.46f)
    } else {
        themeColors.overlayScrimMatte
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(outerScrim)
            .clickable(interactionSource = null, indication = null) { onDismiss() }
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassBox(
            modifier = Modifier
                .width(860.dp)
                .fillMaxHeight()
                .clickable(interactionSource = null, indication = null) {},
            shape = RoundedCornerShape(30.dp),
            borderColor = themeColors.subtleBorder,
            glassAlpha = if (themeColors.isLight) 0.86f else 0.80f
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // ── Sidebar ─────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .width(214.dp)
                        .fillMaxHeight()
                        .padding(start = 14.dp, top = 16.dp, bottom = 14.dp, end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Settings",
                        color = themeColors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(SettingsCategory.entries) { cat ->
                            SidebarRow(
                                category = cat,
                                selected = cat == selected,
                                accent = accent,
                                onClick = { selectedName = cat.name }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoundIconButton(onClick = { hardwareViewModel.toggleFreeformDragMode(); onDismiss() }) {
                            MultiOsIcons.EditGrid(Modifier.size(15.dp), themeColors.iconTint)
                        }
                        RoundIconButton(onClick = onDismiss) {
                            MultiOsIcons.Close(Modifier.size(13.dp), themeColors.iconTint)
                        }
                    }
                }

                // Hairline separator
                Box(
                    modifier = Modifier
                        .width(0.75.dp)
                        .fillMaxHeight()
                        .padding(vertical = 18.dp)
                        .background(themeColors.subtleBorder)
                )

                // ── Detail pane ─────────────────────────────────────────
                key(selected) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = selected.title,
                                color = themeColors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(top = 18.dp, start = 4.dp)
                            )
                        }
                        when (selected) {
                            SettingsCategory.GENERAL -> generalSection(hardwareViewModel, accent)
                            SettingsCategory.APPEARANCE -> appearanceSection(hardwareViewModel, accent)
                            SettingsCategory.WALLPAPER -> wallpaperSection(hardwareViewModel, accent)
                            SettingsCategory.HOME -> homeSection(hardwareViewModel, accent)
                            SettingsCategory.KEYBOARD -> keyboardSection(hardwareViewModel, accent)
                            SettingsCategory.GESTURES -> gesturesSection(hardwareViewModel, accent)
                            SettingsCategory.GEMINI -> geminiSection(hardwareViewModel, accent)
                            SettingsCategory.RECENTS -> recentsSection(hardwareViewModel, accent)
                            SettingsCategory.LOCK -> lockSection(hardwareViewModel, accent)
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// SECTIONS
// ═════════════════════════════════════════════════════════════════════════

private fun LazyListScope.generalSection(vm: HardwareViewModel, accent: Color) {
    item {
        val context = LocalContext.current
        val t by vm.telemetryState.collectAsState()
        SettingsGroup {
            NavRow("Default Home App", "Use MultiOS as launcher") { vm.promptSetDefaultHomeLauncher(context) }
            GroupDivider()
            NavRow(
                "System Brightness",
                if (t.canWriteSystemSettings) "Granted" else "Grant write settings"
            ) { vm.openWriteSettingsPermission(context) }
            GroupDivider()
            NavRow("Magisk Root", if (t.isRootGranted) "Active" else "Tap to grant") { vm.requestMagiskRootInteractive() }
            GroupDivider()
            ToggleRow("Magisk Root Bypass Charging", t.bypassChargingRoot, accent) {
                vm.setBypassChargingRoot(!t.bypassChargingRoot)
            }
            GroupDivider()
            NavRow("Android Settings", null) { vm.openNativeAndroidSettings(context) }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Launcher Mode & Orientation") {
            ChipRow {
                OrientationMode.entries.forEach { mode ->
                    val title = when (mode) {
                        OrientationMode.DESK_ONLY -> "Desk (Landscape)"
                        OrientationMode.MOBILE_ONLY -> "Mobile (Portrait)"
                        OrientationMode.MIX_AUTO -> "Auto Sensor"
                    }
                    SelectableGlassChip(title, t.orientationMode == mode, accent, themeColors.onAccent) {
                        vm.setOrientationMode(mode)
                    }
                }
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "Display Scale") {
            SliderRow(
                title = "Scale",
                valueText = "${t.customDpi} DPI",
                progress = (t.customDpi - 200f) / 200f,
                accent = accent,
                onChange = { vm.setCustomDpi((200f + it * 200f).roundToInt()) }
            )
        }
    }
}

private fun LazyListScope.appearanceSection(vm: HardwareViewModel, accent: Color) {
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Mode") {
            ChipRow {
                AppAppearanceMode.entries.forEach { mode ->
                    SelectableGlassChip(mode.title, t.appearanceMode == mode, accent, themeColors.onAccent) {
                        vm.setAppearanceMode(mode)
                    }
                }
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Accent Color") {
            ChipRow {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .border(1.5.dp, themeColors.textPrimary, CircleShape)
                )
                ColorPalettePreset.entries.forEach { preset ->
                    SelectableGlassChip(
                        preset.title,
                        !t.useCustomAccentColor && t.colorPalette == preset,
                        preset.primary,
                        Color(0xFF0A0D14)
                    ) { vm.selectPalettePreset(preset) }
                }
            }
            RainbowHueSpectrumBar(hueDegrees = t.customAccentHue) { h ->
                vm.setCustomAccentHsv(
                    hue = h,
                    sat = if (t.useCustomAccentColor) t.customAccentSat else 0.72f,
                    value = if (t.useCustomAccentColor) t.customAccentVal else 0.98f
                )
            }
            SliderRow("Saturation", "${(t.customAccentSat * 100).roundToInt()}%", t.customAccentSat, accent) {
                vm.setCustomAccentHsv(t.customAccentHue, it, t.customAccentVal)
            }
            SliderRow(
                "Brightness",
                "${(t.customAccentVal * 100).roundToInt()}%",
                ((t.customAccentVal - 0.18f) / 0.82f).coerceIn(0f, 1f),
                accent
            ) { vm.setCustomAccentHsv(t.customAccentHue, t.customAccentSat, 0.18f + it * 0.82f) }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        val view = LocalView.current
        SettingsGroup(header = "Liquid Glass") {
            ToggleRow("Real-time Liquid Glass", t.liquidGlassEnabled, accent) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                vm.setLiquidGlassEnabled(!t.liquidGlassEnabled)
            }
            GroupDivider()
            ToggleRow("Glass on Vinyl Discs", t.vinylLiquidGlassEnabled, accent) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                vm.toggleVinylLiquidGlass()
            }
            GroupDivider()
            ChipRow {
                GlassTintPreset.entries.filter { it != GlassTintPreset.CUSTOM_HUE }.forEach { preset ->
                    SelectableGlassChip(preset.title, t.glassTintPreset == preset, accent, themeColors.onAccent) {
                        vm.selectGlassTintPreset(preset)
                    }
                }
            }
            RainbowHueSpectrumBar(hueDegrees = t.customGlassHue) { h -> vm.setCustomGlassHsv(h, t.customGlassSat) }
            SliderRow("Tint Saturation", "${(t.customGlassSat * 100).roundToInt()}%", t.customGlassSat, accent) {
                vm.setCustomGlassHsv(t.customGlassHue, it)
            }
            SliderRow(
                "Tint Depth",
                "${(t.glassTintIntensity * 100).roundToInt()}%",
                ((t.glassTintIntensity - 0.05f) / 0.50f).coerceIn(0f, 1f),
                accent
            ) { vm.setGlassTintIntensity(0.05f + it * 0.50f) }
            GroupDivider()
            ToggleRow("Low-End GPU Turbo (Mali-G72 Bypass)", t.lowEndGpuTurbo, accent) {
                vm.setLowEndGpuTurbo(!t.lowEndGpuTurbo)
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "Hardware Bezel Mask") {
            SliderRow(
                title = "Corner Radius",
                valueText = "${t.bezelCornerRadiusDp.roundToInt()} dp",
                progress = t.bezelCornerRadiusDp / 48f,
                accent = accent,
                onChange = { vm.setBezelCornerRadiusDp(it * 48f) }
            )
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "App Opening Animation") {
            ChipRow {
                AppOpeningAnimStyle.entries.forEach { anim ->
                    val title = when (anim) {
                        AppOpeningAnimStyle.IOS_PERSPECTIVE_BALLOON -> "iOS Balloon"
                        AppOpeningAnimStyle.HYPEROS_BALLOON -> "HyperOS"
                        AppOpeningAnimStyle.ONEUI_PORTAL -> "OneUI Portal"
                    }
                    SelectableGlassChip(title, t.appOpeningAnimStyle == anim, accent, themeColors.onAccent) {
                        vm.setAppOpeningAnimStyle(anim)
                    }
                }
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Volume HUD Style") {
            ChipRow {
                VolumeHudStyle.entries.forEach { hud ->
                    val title = when (hud) {
                        VolumeHudStyle.IOS_SIDE_PILL -> "iOS Side Pill"
                        VolumeHudStyle.COLOROS_ARC -> "ColorOS Arc"
                        VolumeHudStyle.HYPEROS_CAPSULE -> "HyperOS Capsule"
                    }
                    SelectableGlassChip(title, t.volumeHudStyle == hud, accent, themeColors.onAccent) {
                        vm.setVolumeHudStyle(hud)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.wallpaperSection(vm: HardwareViewModel, accent: Color) {
    item {
        val context = LocalContext.current
        val t by vm.telemetryState.collectAsState()
        val wallpaperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) vm.applyGalleryWallpaper(uri)
        }
        SettingsGroup(header = "Source") {
            NavRow("Choose from Gallery", if (t.hasCustomWallpaper) "Custom image applied" else null) {
                wallpaperLauncher.launch("image/*")
            }
            GroupDivider()
            NavRow("Use Phone Wallpaper", null) { vm.useLivePhoneSystemWallpaper() }
            GroupDivider()
            NavRow("System Live Wallpapers", "Opens Android live wallpaper picker") {
                vm.openSystemLiveWallpaperChooser(context)
            }
            GroupDivider()
            ToggleRow("Sync to Home & Lock Screen", t.syncSystemWallpaper, accent) {
                val next = !t.syncSystemWallpaper
                vm.setSyncSystemWallpaper(next)
                if (next && t.hasCustomWallpaper) vm.syncCurrentWallpaperToSystem()
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Live Shader Wallpapers") {
            ChipRow {
                WallpaperStyle.entries.forEach { style ->
                    SelectableGlassChip(
                        style.title,
                        !t.hasCustomWallpaper && t.wallpaperStyle == style,
                        accent,
                        themeColors.onAccent
                    ) {
                        vm.clearCustomWallpaper()
                        vm.selectWallpaperStyle(style)
                    }
                }
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "Blur") {
            SliderRow(
                title = "Wallpaper Blur",
                valueText = if (t.homeWallpaperBlurDp < 0.5f) "Off" else "${t.homeWallpaperBlurDp.roundToInt()}",
                progress = t.homeWallpaperBlurDp / 35f,
                accent = accent,
                onChange = { vm.setHomeWallpaperBlurDp(it * 35f) }
            )
        }
    }
}

private fun LazyListScope.homeSection(vm: HardwareViewModel, accent: Color) {
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "Widgets") {
            ToggleRow("Keyboard", t.showHomeKeyboard, accent) { vm.setShowHomeKeyboard(!t.showHomeKeyboard) }
            GroupDivider()
            ToggleRow("Clock", t.showClockWidget, accent) { vm.setShowClockWidget(!t.showClockWidget) }
            GroupDivider()
            ToggleRow("Thermals", t.showThermalWidget, accent) { vm.setShowThermalWidget(!t.showThermalWidget) }
            GroupDivider()
            ToggleRow("Sliders", t.showBottomSliders, accent) { vm.setShowBottomSliders(!t.showBottomSliders) }
            GroupDivider()
            ToggleRow("Battery & Wattage", t.showBatteryWidget, accent) { vm.setShowBatteryWidget(!t.showBatteryWidget) }
            GroupDivider()
            ToggleRow("RAM & Storage", t.showRamWidget, accent) { vm.setShowRamWidget(!t.showRamWidget) }
            GroupDivider()
            ToggleRow("Live Messages", t.showLiveMessagesWidget, accent) {
                vm.setShowLiveMessagesWidget(!t.showLiveMessagesWidget)
            }
            GroupDivider()
            ToggleRow("Show Glass Border on Widgets", t.showWidgetGlassBorder, accent) {
                vm.setShowWidgetGlassBorder(!t.showWidgetGlassBorder)
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "Mobile Portrait Mode Layout") {
            SliderRow(
                title = "Grid Columns",
                valueText = "${t.mobileGridCols}",
                progress = (t.mobileGridCols - 3f) / 3f,
                accent = accent,
                onChange = { vm.setMobileGrid((3f + it * 3f).roundToInt(), t.mobileGridRows) }
            )
            SliderRow(
                title = "Grid Rows",
                valueText = "${t.mobileGridRows}",
                progress = (t.mobileGridRows - 4f) / 6f,
                accent = accent,
                onChange = { vm.setMobileGrid(t.mobileGridCols, (4f + it * 6f).roundToInt()) }
            )
            GroupDivider()
            ToggleRow("macOS Freeform Scatter Mode", t.mobileScatterMode, accent) {
                vm.setMobileScatterMode(!t.mobileScatterMode)
            }
            GroupDivider()
            ToggleRow("Spotlight Search at Top", t.mobileSearchBarAtTop, accent) {
                vm.setMobileSearchBarAtTop(!t.mobileSearchBarAtTop)
            }
        }
    }
    item {
        val context = LocalContext.current
        val launchWidgetPicker = LocalLaunchWidgetPicker.current
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "More Widgets") {
            NavRow("Add App Widget", "Any installed app's widget") { launchWidgetPicker?.invoke() }
            if (t.showLiveMessagesWidget) {
                GroupDivider()
                NavRow("Allow Message Access", "Needed for Live Messages") { vm.enableLiveMessagesAccess(context) }
            }
        }
    }
    item {
        val knobSlots by vm.knobSlotAssignments.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Knobs  ·  ${knobSlots.size}/6") {
            ChipRow {
                knobSlots.forEachIndexed { index, param ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(themeColors.subtleFill)
                            .border(0.75.dp, themeColors.subtleBorder, RoundedCornerShape(50))
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = param.displayTitle,
                            color = themeColors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            modifier = Modifier.clickable { vm.cycleKnobSlotAssignment(index) }
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(themeColors.subtleFill)
                                .clickable { vm.removeKnobSlotAt(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.Close(Modifier.size(8.dp), themeColors.iconTint)
                        }
                    }
                }
                if (knobSlots.size < 6) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .clickable { vm.addKnobSlot() },
                        contentAlignment = Alignment.Center
                    ) {
                        MultiOsIcons.Plus(Modifier.size(13.dp), themeColors.onAccent)
                    }
                }
            }
            Text(
                text = "Tap a knob to change what it controls.",
                color = themeColors.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Knob Design") {
            ChipRow {
                KnobStylePreset.entries.forEach { style ->
                    SelectableGlassChip(style.title, t.knobStyle == style, accent, themeColors.onAccent) {
                        vm.selectKnobStyle(style)
                    }
                }
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Clock") {
            ChipRow {
                ClockStylePreset.entries.forEach { style ->
                    SelectableGlassChip(style.title, t.clockStyle == style, accent, themeColors.onAccent) {
                        vm.selectClockStyle(style)
                    }
                }
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Slider Design") {
            ChipRow {
                SliderStylePreset.entries.forEach { style ->
                    SelectableGlassChip(style.title, t.sliderStyle == style, accent, themeColors.onAccent) {
                        vm.selectSliderStyle(style)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.keyboardSection(vm: HardwareViewModel, accent: Color) {
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Home Keyboard") {
            ChipRow {
                HomeKeyboardMode.entries.forEach { mode ->
                    SelectableGlassChip(mode.title, t.homeKeyboardMode == mode, accent, themeColors.onAccent) {
                        vm.setHomeKeyboardMode(mode)
                    }
                }
            }
            GroupDivider()
            NavRow("Reset Keyboard Size", null) { vm.resetFullKeyboardBounds() }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "App Drawer") {
            ChipRow {
                DrawerAppAlignment.entries.forEach { align ->
                    SelectableGlassChip(align.title, t.drawerAppAlignment == align, accent, themeColors.onAccent) {
                        vm.setDrawerAppAlignment(align)
                    }
                }
            }
            ChipRow {
                DrawerKeyboardMode.entries.forEach { mode ->
                    SelectableGlassChip(mode.title, t.drawerKeyboardMode == mode, accent, themeColors.onAccent) {
                        vm.setDrawerKeyboardMode(mode)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.gesturesSection(vm: HardwareViewModel, accent: Color) {
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "Swipe Actions") {
            SwipeTargetRow("Swipe Right", t.swipeRightTarget, accent) { vm.setSwipeRightTarget(it) }
            GroupDivider()
            SwipeTargetRow("Swipe Left", t.swipeLeftTarget, accent) { vm.setSwipeLeftTarget(it) }
            GroupDivider()
            SwipeTargetRow("Swipe Up", t.swipeUpTarget, accent) { vm.setSwipeUpTarget(it) }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "Control Center") {
            ToggleRow("Open from Top Pull Tab Only", t.verticalSwipeFromTabsOnly, accent) {
                vm.setVerticalSwipeFromTabsOnly(!t.verticalSwipeFromTabsOnly)
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Gemini Reveal Animation") {
            ChipRow {
                GeminiEdgeStyle.entries.forEach { edge ->
                    SelectableGlassChip(edge.title, t.geminiEdgeStyle == edge, accent, themeColors.onAccent) {
                        vm.setGeminiEdgeStyle(edge)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.geminiSection(vm: HardwareViewModel, accent: Color) {
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "Gemini Web Window") {
            SliderRow(
                title = "Zoom",
                valueText = "${t.geminiZoomPercent}%",
                progress = (t.geminiZoomPercent - 50f) / 130f,
                accent = accent,
                onChange = { vm.setGeminiZoomPercent((50f + it * 130f).roundToInt()) }
            )
            SliderRow(
                title = "Width",
                valueText = "${t.geminiWindowW.roundToInt()}",
                progress = (t.geminiWindowW - 400f) / 1200f,
                accent = accent,
                onChange = { vm.setGeminiWindowSize(400f + it * 1200f, t.geminiWindowH) }
            )
            SliderRow(
                title = "Height",
                valueText = "${t.geminiWindowH.roundToInt()}",
                progress = (t.geminiWindowH - 300f) / 700f,
                accent = accent,
                onChange = { vm.setGeminiWindowSize(t.geminiWindowW, 300f + it * 700f) }
            )
        }
    }
}

private fun LazyListScope.recentsSection(vm: HardwareViewModel, accent: Color) {
    item {
        SettingsGroup {
            NavRow("Open Recents", null) { vm.openRecentsDeck() }
            GroupDivider()
            NavRow("Refresh Snapshots", "Reads live task snapshots via root") { vm.refreshRecentTaskSnapshots() }
        }
    }
    item {
        val themeColors = LocalMultiOsColors.current
        Text(
            text = "BLUR IN RECENTS",
            color = themeColors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(start = 14.dp, bottom = 0.dp)
        )
    }
    item(key = "recents_blur_list") {
        val installedApps by vm.installedApps.collectAsState()
        val blurred by vm.blurredRecentPackages.collectAsState()
        val themeColors = LocalMultiOsColors.current
        LaunchedEffect(Unit) { vm.refreshRecentTaskSnapshots() }
        SettingsGroup {
            if (installedApps.isEmpty()) {
                Text("No apps found", color = themeColors.textSecondary, fontSize = 11.sp)
            }
            installedApps.sortedBy { it.name.lowercase() }.forEachIndexed { i, app ->
                if (i > 0) GroupDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.toggleRecentPackageBlur(app.packageName) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(themeColors.subtleFill)
                    ) {
                        app.icon?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Text(
                        text = app.name,
                        color = themeColors.textPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalToggleSwitch(
                        checked = app.packageName in blurred,
                        accent = accent,
                        onAccent = themeColors.onAccent,
                        onToggle = { vm.toggleRecentPackageBlur(app.packageName) }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.lockSection(vm: HardwareViewModel, accent: Color) {
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(header = "Always-On Display", footer = "Dimmed, blurred wallpaper with a clock that slowly fades to a new spot every minute.") {
            ToggleRow("Always-On Display", t.aodEnabled, accent) { vm.setAodEnabled(!t.aodEnabled) }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(header = "Charging Animation") {
            ChipRow {
                ChargingAnimStyle.entries.forEach { style ->
                    SelectableGlassChip(style.title, t.chargingAnimStyle == style, accent, themeColors.onAccent) {
                        vm.setChargingAnimStyle(style)
                    }
                }
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        val themeColors = LocalMultiOsColors.current
        SettingsGroup(
            header = "Hello Gate Screen",
            footer = "Swipe the left-pointing arrow (<) to unlock and dismiss root keyguard."
        ) {
            ToggleRow("Show Hello Gate on Wake", t.showWelcomeScreen, accent) {
                vm.setShowWelcomeScreen(!t.showWelcomeScreen)
            }
            if (t.showWelcomeScreen) {
                GroupDivider()
                ChipRow {
                    WelcomeStyle.entries.forEach { style ->
                        val title = when (style) {
                            WelcomeStyle.IOS_17_CURSIVE -> "iOS 17 Cursive"
                            WelcomeStyle.IOS_26_LIQUID_GLASS -> "iOS 26 Liquid Glass"
                        }
                        SelectableGlassChip(title, t.welcomeStyle == style, accent, themeColors.onAccent) {
                            vm.setWelcomeStyle(style)
                        }
                    }
                }
            }
        }
    }
    item {
        val t by vm.telemetryState.collectAsState()
        SettingsGroup(
            header = "Lock Screen",
            footer = "Uses root to disable the stock crDroid keyguard so MultiOS takes over. Removes your PIN/pattern protection."
        ) {
            ToggleRow("Replace System Lock Screen", t.disableSystemLockScreenRoot, accent) {
                vm.setDisableSystemLockScreenRoot(!t.disableSystemLockScreenRoot)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// BUILDING BLOCKS
// ═════════════════════════════════════════════════════════════════════════

@Composable
private fun SidebarRow(category: SettingsCategory, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val themeColors = LocalMultiOsColors.current
    val tint = if (selected) themeColors.onAccent else themeColors.iconTint
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val m = Modifier.size(16.dp)
        when (category) {
            SettingsCategory.GENERAL -> MultiOsIcons.Gear(m, tint)
            SettingsCategory.APPEARANCE -> MultiOsIcons.Moon(m, tint)
            SettingsCategory.WALLPAPER -> MultiOsIcons.GalleryImage(m, tint)
            SettingsCategory.HOME -> MultiOsIcons.HomeHouse(m, tint)
            SettingsCategory.KEYBOARD -> MultiOsIcons.KeyboardGlyph(m, tint)
            SettingsCategory.GESTURES -> MultiOsIcons.EditGrid(m, tint)
            SettingsCategory.GEMINI -> MultiOsIcons.GeminiSpark(m, tint)
            SettingsCategory.RECENTS -> MultiOsIcons.RecentsCards(m, tint)
            SettingsCategory.LOCK -> MultiOsIcons.BrightnessSun(m, tint)
        }
        Text(
            text = category.title,
            color = if (selected) themeColors.onAccent else themeColors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1
        )
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val themeColors = LocalMultiOsColors.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(themeColors.subtleFill)
            .border(0.75.dp, themeColors.subtleBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun SettingsGroup(
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (header != null) {
            Text(
                text = header.uppercase(),
                color = themeColors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(themeColors.subtleFill)
                .border(0.6.dp, themeColors.subtleBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
        if (footer != null) {
            Text(
                text = footer,
                color = themeColors.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

@Composable
private fun GroupDivider() {
    val themeColors = LocalMultiOsColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(themeColors.subtleBorder)
    )
}

@Composable
private fun NavRow(title: String, subtitle: String?, onClick: () -> Unit) {
    val themeColors = LocalMultiOsColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = themeColors.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.SansSerif)
            if (subtitle != null) {
                Text(subtitle, color = themeColors.textSecondary, fontSize = 10.sp, fontFamily = FontFamily.SansSerif)
            }
        }
        Text("›", color = themeColors.textSecondary, fontSize = 18.sp, fontFamily = FontFamily.SansSerif)
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, accent: Color, onToggle: () -> Unit) {
    val themeColors = LocalMultiOsColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = themeColors.textPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )
        MinimalToggleSwitch(checked = checked, accent = accent, onAccent = themeColors.onAccent, onToggle = onToggle)
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueText: String,
    progress: Float,
    accent: Color,
    onChange: (Float) -> Unit
) {
    CustomLiquidSlider(
        title = title,
        valueText = valueText,
        progress = progress.coerceIn(0f, 1f),
        accentColor = accent,
        style = SliderStylePreset.LIQUID_CAPSULE,
        showLabels = true,
        glyphType = SliderGlyphType.NONE,
        barHeight = 28.dp,
        onProgressChange = onChange,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun SwipeTargetRow(
    title: String,
    current: SwipeActionTarget,
    accent: Color,
    onSelect: (SwipeActionTarget) -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = themeColors.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.SansSerif)
        ChipRow {
            SwipeActionTarget.entries.forEach { target ->
                SelectableGlassChip(target.title, current == target, accent, themeColors.onAccent) { onSelect(target) }
            }
        }
    }
}

/**
 * Interactive 360° Rainbow Hue Spectrum Bar.
 */
@Composable
private fun RainbowHueSpectrumBar(
    hueDegrees: Float,
    onHueChange: (Float) -> Unit
) {
    val latestOnHue by rememberUpdatedState(onHueChange)
    val rainbowColors = remember {
        listOf(
            Color.hsv(0f, 0.88f, 1f),
            Color.hsv(60f, 0.88f, 1f),
            Color.hsv(120f, 0.88f, 1f),
            Color.hsv(180f, 0.88f, 1f),
            Color.hsv(240f, 0.88f, 1f),
            Color.hsv(300f, 0.88f, 1f),
            Color.hsv(360f, 0.88f, 1f)
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val ratio = (pos.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    latestOnHue(ratio * 360f)
                }
            }
            .pointerInput("drag_hue") {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val ratio = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    latestOnHue(ratio * 360f)
                }
            }
    ) {
        val r = size.height / 2f
        drawRoundRect(brush = Brush.horizontalGradient(rainbowColors), cornerRadius = CornerRadius(r, r))
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = 1.dp.toPx())
        )
        val thumbX = ((hueDegrees.coerceIn(0f, 360f) / 360f) * (size.width - r * 2f)) + r
        val thumbCenter = Offset(thumbX, size.height / 2f)
        drawCircle(color = Color.White, radius = r * 0.78f, center = thumbCenter)
        drawCircle(color = Color.hsv(hueDegrees.coerceIn(0f, 360f), 0.90f, 1f), radius = r * 0.52f, center = thumbCenter)
    }
}

@Composable
private fun SelectableGlassChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else themeColors.subtleFill)
            .border(0.75.dp, if (selected) accent else themeColors.subtleBorder, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) onAccent else themeColors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1
        )
    }
}

@Composable
private fun MinimalToggleSwitch(
    checked: Boolean,
    accent: Color,
    onAccent: Color,
    onToggle: () -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(if (checked) accent else themeColors.subtleFill)
            .border(0.6.dp, themeColors.subtleBorder, CircleShape)
            .clickable { onToggle() }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) onAccent else themeColors.textPrimary)
        )
    }
}
