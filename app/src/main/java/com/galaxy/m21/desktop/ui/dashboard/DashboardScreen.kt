package com.galaxy.m21.desktop.ui.dashboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.LocalAppWidgetHost
import com.galaxy.m21.desktop.LocalAppWidgetManager
import com.galaxy.m21.desktop.LocalLaunchWidgetPicker
import com.galaxy.m21.desktop.hardware.AppEntry
import com.galaxy.m21.desktop.hardware.HardwareViewModel
import com.galaxy.m21.desktop.hardware.KnobParameter
import com.galaxy.m21.desktop.hardware.SystemTelemetryData
import com.galaxy.m21.desktop.ui.components.CustomLiquidSlider
import com.galaxy.m21.desktop.ui.components.CustomStatusBar
import com.galaxy.m21.desktop.ui.components.DynamicWallpaperCanvas
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.components.MultiOsPullTabCapsule
import com.galaxy.m21.desktop.ui.components.SliderGlyphType
import com.galaxy.m21.desktop.ui.navigation.SurfaceDestination
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.theme.HomeKeyboardMode
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import com.galaxy.m21.desktop.ui.theme.MultiOsTheme
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * MultiOS Pure Landscape Home Launcher Surface (`sensorLandscape` — Either Side of Landscape).
 *
 * - Long press anywhere on empty space enters Edit Mode!
 * - Dynamic 0..6 knobs split symmetrically across Left & Right wings with remove '×' badges and '+ Knob' dock action!
 * - Modular show/hide toggles for Keyboard, Clock, Thermal box, Sliders, and Extra Widgets!
 * - Embeds native third-party Android AppWidgets via AppWidgetHost!
 * - Uses symmetrical MultiOsPullTabCapsule at bottom center for Vinyl Studio!
 */
@Composable
fun DashboardScreen(
    hardwareViewModel: HardwareViewModel,
    onNavigate: (SurfaceDestination) -> Unit
) {
    val context = LocalContext.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val installedApps by hardwareViewModel.installedApps.collectAsState()
    val activeKnob by hardwareViewModel.activeManipulatedKnob.collectAsState()
    val knobSlots by hardwareViewModel.knobSlotAssignments.collectAsState()
    val boundAppWidgetIds by hardwareViewModel.boundAppWidgetIds.collectAsState()
    val widgetOffsets by hardwareViewModel.widgetOffsets.collectAsState()
    val widgetScales by hardwareViewModel.widgetScales.collectAsState()
    val miniDeckSize by hardwareViewModel.miniDeckSize.collectAsState()
    val miniDeckText by hardwareViewModel.miniDeckText.collectAsState()
    val miniDeckSubtitle by hardwareViewModel.miniDeckSubtitle.collectAsState()

    DashboardContent(
        telemetry = telemetry,
        installedApps = installedApps,
        activeKnob = activeKnob,
        knobSlots = knobSlots,
        boundAppWidgetIds = boundAppWidgetIds,
        widgetOffsets = widgetOffsets,
        widgetScales = widgetScales,
        miniDeckSize = miniDeckSize,
        miniDeckText = miniDeckText,
        miniDeckSubtitle = miniDeckSubtitle,
        getKnobProgress = { param -> hardwareViewModel.getKnobProgress(param, telemetry) },
        onAdjustParameter = { param, delta -> hardwareViewModel.adjustParameter(param, delta) },
        onCycleKnobAssignment = { slot -> hardwareViewModel.cycleKnobSlotAssignment(slot) },
        onAddKnob = { hardwareViewModel.addKnobSlot() },
        onRemoveKnob = { slot -> hardwareViewModel.removeKnobSlotAt(slot) },
        onRemoveBoundWidget = { id -> hardwareViewModel.removeBoundAppWidget(id) },
        onToggleShowHomeKeyboard = { hardwareViewModel.setShowHomeKeyboard(it) },
        onToggleShowClockWidget = { hardwareViewModel.setShowClockWidget(it) },
        onToggleShowThermalWidget = { hardwareViewModel.setShowThermalWidget(it) },
        onToggleShowBottomSliders = { hardwareViewModel.setShowBottomSliders(it) },
        onToggleShowLiveMessages = { hardwareViewModel.setShowLiveMessagesWidget(it) },
        onToggleShowBattery = { hardwareViewModel.setShowBatteryWidget(it) },
        onToggleShowRam = { hardwareViewModel.setShowRamWidget(it) },
        onCycleClockStyle = { hardwareViewModel.cycleClockStyle() },
        onMoveWidget = { key, dx, dy -> hardwareViewModel.moveWidget(key, dx, dy) },
        onScaleWidget = { key, delta -> hardwareViewModel.scaleWidget(key, delta) },
        onResizeMiniDeck = { dw, dh -> hardwareViewModel.resizeMiniDeck(dw, dh) },
        onMiniDeckKey = { hardwareViewModel.appendMiniDeckKey(it) },
        onMiniDeckBackspace = { hardwareViewModel.backspaceMiniDeck() },
        onMiniDeckClear = { hardwareViewModel.clearMiniDeck() },
        onMiniDeckSubmit = { hardwareViewModel.submitMiniDeck(context) },
        onLaunchAppFromKeyboard = { app ->
            hardwareViewModel.launchApp(context, app)
            hardwareViewModel.clearMiniDeck()
        },
        onToggleHomeKeyboardMode = { hardwareViewModel.toggleHomeKeyboardMode() },
        onUpdateFullKbBounds = { dh, db, dl, dr -> hardwareViewModel.updateFullKeyboardBounds(dh, db, dl, dr) },
        onResetFullKbBounds = { hardwareViewModel.resetFullKeyboardBounds() },
        onSetSystemWideVolume = { hardwareViewModel.setSystemWideVolumeDirect(it, triggerKnobHighlight = false) },
        onSetBrightness = { hardwareViewModel.setBrightnessDirect(it) },
        onSetThermalCeiling = { hardwareViewModel.setThermalThresholdDirect(it) },
        onCycleKnobStyle = { hardwareViewModel.cycleKnobStyle() },
        onResetLayout = { hardwareViewModel.resetWidgetOffsets() },
        onToggleFramelessWidgets = { hardwareViewModel.setShowWidgetGlassBorder(!telemetry.showWidgetGlassBorder) },
        onToggleSidePanel = { hardwareViewModel.setSidePanelOnLeft(!telemetry.sidePanelOnLeft) },
        onDoneEditing = { hardwareViewModel.toggleFreeformDragMode() },
        onNavigate = onNavigate
    )
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun DashboardContent(
    telemetry: SystemTelemetryData = SystemTelemetryData(),
    installedApps: List<AppEntry> = emptyList(),
    activeKnob: KnobParameter? = null,
    knobSlots: List<KnobParameter> = listOf(
        KnobParameter.DISPLAY_BRIGHTNESS,
        KnobParameter.SYSTEM_AUDIO,
        KnobParameter.CPU_SPEED,
        KnobParameter.MUSIC_VOLUME
    ),
    boundAppWidgetIds: List<Int> = emptyList(),
    widgetOffsets: Map<String, Pair<Float, Float>> = emptyMap(),
    widgetScales: Map<String, Float> = emptyMap(),
    miniDeckSize: Pair<Float, Float> = Pair(336f, 156f),
    miniDeckText: String = "",
    miniDeckSubtitle: String = "",
    getKnobProgress: (KnobParameter) -> Float = { defaultGetKnobProgress(it, telemetry) },
    onAdjustParameter: (KnobParameter, Float) -> Unit = { _, _ -> },
    onCycleKnobAssignment: (Int) -> Unit = {},
    onAddKnob: () -> Unit = {},
    onRemoveKnob: (Int) -> Unit = {},
    onRemoveBoundWidget: (Int) -> Unit = {},
    onToggleShowHomeKeyboard: (Boolean) -> Unit = {},
    onToggleShowClockWidget: (Boolean) -> Unit = {},
    onToggleShowThermalWidget: (Boolean) -> Unit = {},
    onToggleShowBottomSliders: (Boolean) -> Unit = {},
    onToggleShowLiveMessages: (Boolean) -> Unit = {},
    onToggleShowBattery: (Boolean) -> Unit = {},
    onToggleShowRam: (Boolean) -> Unit = {},
    onCycleClockStyle: () -> Unit = {},
    onMoveWidget: (String, Float, Float) -> Unit = { _, _, _ -> },
    onScaleWidget: (String, Float) -> Unit = { _, _ -> },
    onResizeMiniDeck: (Float, Float) -> Unit = { _, _ -> },
    onMiniDeckKey: (String) -> Unit = {},
    onMiniDeckBackspace: () -> Unit = {},
    onMiniDeckClear: () -> Unit = {},
    onMiniDeckSubmit: () -> Unit = {},
    onLaunchAppFromKeyboard: (AppEntry) -> Unit = {},
    onToggleHomeKeyboardMode: () -> Unit = {},
    onUpdateFullKbBounds: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onResetFullKbBounds: () -> Unit = {},
    onSetSystemWideVolume: (Float) -> Unit = {},
    onSetBrightness: (Float) -> Unit = {},
    onSetThermalCeiling: (Int) -> Unit = {},
    onCycleKnobStyle: () -> Unit = {},
    onResetLayout: () -> Unit = {},
    onToggleFramelessWidgets: () -> Unit = {},
    onToggleSidePanel: () -> Unit = {},
    onDoneEditing: () -> Unit = {},
    onNavigate: (SurfaceDestination) -> Unit = {}
) {
    val themeColors = LocalMultiOsColors.current
    val appWidgetHost = LocalAppWidgetHost.current
    val appWidgetManager = LocalAppWidgetManager.current
    val launchWidgetPicker = LocalLaunchWidgetPicker.current

    val pixelShiftX = if (telemetry.oledBurnInProtection) telemetry.burnInOffsetX else 0
    val pixelShiftY = if (telemetry.oledBurnInProtection) telemetry.burnInOffsetY else 0

    val accent = telemetry.resolveAccentColor()
    val editMode = telemetry.freeformDragEnabled
    var fullScreenKeyboardOpen by remember { mutableStateOf(false) }

    LaunchedEffect(telemetry.homeKeyboardMode, editMode) {
        if (telemetry.homeKeyboardMode == HomeKeyboardMode.FLOATING || editMode) {
            fullScreenKeyboardOpen = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 2.dp)
            .offset { IntOffset(pixelShiftX, pixelShiftY) }
            .pointerInput(editMode) {
                detectTapGestures(
                    onLongPress = {
                        onDoneEditing()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dynamic Wings Split
            val totalKnobs = knobSlots.size
            val half = ceil(totalKnobs / 2.0).toInt()
            val leftWingSlots = (0 until half)
            val rightWingSlots = (half until totalKnobs)

            // =========================================================================
            // STRICT LANDSCAPE MULTI-OS COCKPIT
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT WING KNOBS
                Column(
                    modifier = Modifier
                        .width(168.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (slot in leftWingSlots) {
                        val key = "KNOB_$slot"
                        val param = knobSlots.getOrElse(slot) { KnobParameter.DISPLAY_BRIGHTNESS }
                        EditableGlassPlacementBox(
                            widgetKey = key,
                            editMode = editMode,
                            accentColor = accent,
                            onAccentColor = themeColors.onAccent,
                            currentOffset = widgetOffsets[key] ?: Pair(0f, 0f),
                            currentScale = widgetScales[key] ?: 1f,
                            showScaleHandle = true,
                            onUpdateOffset = { dx, dy -> onMoveWidget(key, dx, dy) },
                            onUpdateScale = { ds -> onScaleWidget(key, ds) }
                        ) {
                            Box {
                                RotaryDial(
                                    parameter = param,
                                    valuePercent = getKnobProgress(param),
                                    accentColor = accent,
                                    knobStyle = telemetry.knobStyle,
                                    editMode = editMode,
                                    dialSize = 128.dp,
                                    onValueChange = { delta -> onAdjustParameter(param, delta) },
                                    onCycleAssignment = { onCycleKnobAssignment(slot) }
                                )
                                if (editMode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-6).dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                            .clickable { onRemoveKnob(slot) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MultiOsIcons.Close(Modifier.size(10.dp), Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // CENTER STAGE: Clock + Thermal + Sliders + Extra Widgets + Native AppWidgets
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Clock & Thermal row
                    if (telemetry.showClockWidget || telemetry.showThermalWidget) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (telemetry.showClockWidget) {
                                EditableGlassPlacementBox(
                                    widgetKey = "CENTER_HUD",
                                    editMode = editMode,
                                    accentColor = accent,
                                    onAccentColor = themeColors.onAccent,
                                    currentOffset = widgetOffsets["CENTER_HUD"] ?: Pair(0f, 0f),
                                    currentScale = widgetScales["CENTER_HUD"] ?: 1f,
                                    showScaleHandle = true,
                                    onUpdateOffset = { dx, dy -> onMoveWidget("CENTER_HUD", dx, dy) },
                                    onUpdateScale = { ds -> onScaleWidget("CENTER_HUD", ds) }
                                ) {
                                    CenterTelemetryDisplay(
                                        telemetry = telemetry,
                                        activeKnob = activeKnob,
                                        editMode = editMode,
                                        onCycleClockStyle = onCycleClockStyle,
                                        modifier = Modifier.size(138.dp)
                                    )
                                }
                            }

                            if (telemetry.showThermalWidget) {
                                EditableGlassPlacementBox(
                                    widgetKey = "THERMAL_MODULE",
                                    editMode = editMode,
                                    accentColor = accent,
                                    onAccentColor = themeColors.onAccent,
                                    currentOffset = widgetOffsets["THERMAL_MODULE"] ?: Pair(0f, 0f),
                                    currentScale = widgetScales["THERMAL_MODULE"] ?: 1f,
                                    showScaleHandle = false,
                                    onUpdateOffset = { dx, dy -> onMoveWidget("THERMAL_MODULE", dx, dy) },
                                    onUpdateScale = {}
                                ) {
                                    ThermalMiniBoxAndSlider(
                                        cpuTempC = telemetry.temperatureCpu,
                                        thermalLimitC = telemetry.thermalThreshold,
                                        accent = accent,
                                        sliderStyle = telemetry.sliderStyle,
                                        onThermalChange = onSetThermalCeiling,
                                        modifier = Modifier.width(196.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Extra Modular Widgets Row (Live Messages, Battery, RAM)
                    if (telemetry.showLiveMessagesWidget || telemetry.showBatteryWidget || telemetry.showRamWidget) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (telemetry.showLiveMessagesWidget) {
                                EditableGlassPlacementBox(
                                    widgetKey = "LIVE_MESSAGES",
                                    editMode = editMode,
                                    accentColor = accent,
                                    onAccentColor = themeColors.onAccent,
                                    currentOffset = widgetOffsets["LIVE_MESSAGES"] ?: Pair(0f, 0f),
                                    currentScale = widgetScales["LIVE_MESSAGES"] ?: 1f,
                                    showScaleHandle = true,
                                    onUpdateOffset = { dx, dy -> onMoveWidget("LIVE_MESSAGES", dx, dy) },
                                    onUpdateScale = { ds -> onScaleWidget("LIVE_MESSAGES", ds) }
                                ) {
                                    LiveMessagesMonitorWidget(
                                        accentColor = accent,
                                        onAccentColor = themeColors.onAccent,
                                        onDismissWidget = if (editMode) ({ onToggleShowLiveMessages(false) }) else null
                                    )
                                }
                            }

                            if (telemetry.showBatteryWidget) {
                                EditableGlassPlacementBox(
                                    widgetKey = "BATTERY_WIDGET",
                                    editMode = editMode,
                                    accentColor = accent,
                                    onAccentColor = themeColors.onAccent,
                                    currentOffset = widgetOffsets["BATTERY_WIDGET"] ?: Pair(0f, 0f),
                                    currentScale = widgetScales["BATTERY_WIDGET"] ?: 1f,
                                    showScaleHandle = false,
                                    onUpdateOffset = { dx, dy -> onMoveWidget("BATTERY_WIDGET", dx, dy) },
                                    onUpdateScale = {}
                                ) {
                                    BatteryAndWattageWidget(
                                        batteryPercent = telemetry.batteryPercent,
                                        wattage = telemetry.batteryWattage,
                                        accentColor = accent,
                                        onDismissWidget = if (editMode) ({ onToggleShowBattery(false) }) else null
                                    )
                                }
                            }

                            if (telemetry.showRamWidget) {
                                EditableGlassPlacementBox(
                                    widgetKey = "RAM_STORAGE_WIDGET",
                                    editMode = editMode,
                                    accentColor = accent,
                                    onAccentColor = themeColors.onAccent,
                                    currentOffset = widgetOffsets["RAM_STORAGE_WIDGET"] ?: Pair(0f, 0f),
                                    currentScale = widgetScales["RAM_STORAGE_WIDGET"] ?: 1f,
                                    showScaleHandle = false,
                                    onUpdateOffset = { dx, dy -> onMoveWidget("RAM_STORAGE_WIDGET", dx, dy) },
                                    onUpdateScale = {}
                                ) {
                                    RamAndStorageWidget(
                                        accentColor = accent,
                                        onDismissWidget = if (editMode) ({ onToggleShowRam(false) }) else null
                                    )
                                }
                            }
                        }
                    }

                    // Native Bound Third-Party App Widgets
                    if (appWidgetHost != null && appWidgetManager != null && boundAppWidgetIds.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (widgetId in boundAppWidgetIds) {
                                val key = "APP_WIDGET_$widgetId"
                                EditableGlassPlacementBox(
                                    widgetKey = key,
                                    editMode = editMode,
                                    accentColor = accent,
                                    onAccentColor = themeColors.onAccent,
                                    currentOffset = widgetOffsets[key] ?: Pair(0f, 0f),
                                    currentScale = widgetScales[key] ?: 1f,
                                    showScaleHandle = true,
                                    onUpdateOffset = { dx, dy -> onMoveWidget(key, dx, dy) },
                                    onUpdateScale = { ds -> onScaleWidget(key, ds) }
                                ) {
                                    CustomAppWidgetHostContainer(
                                        appWidgetId = widgetId,
                                        appWidgetHost = appWidgetHost,
                                        appWidgetManager = appWidgetManager,
                                        editMode = editMode,
                                        showGlassBorder = telemetry.showWidgetGlassBorder,
                                        onRemoveWidget = { onRemoveBoundWidget(widgetId) }
                                    )
                                }
                            }
                        }
                    }

                    // Home Keyboard Widget
                    if (telemetry.showHomeKeyboard) {
                        EditableGlassPlacementBox(
                            widgetKey = "MINI_DECK",
                            editMode = editMode,
                            accentColor = accent,
                            onAccentColor = themeColors.onAccent,
                            currentOffset = widgetOffsets["MINI_DECK"] ?: Pair(0f, 0f),
                            currentScale = 1f,
                            showScaleHandle = false,
                            onUpdateOffset = { dx, dy -> onMoveWidget("MINI_DECK", dx, dy) },
                            onUpdateScale = {}
                        ) {
                            MiniCyberDeckWidget(
                                screenText = miniDeckText,
                                screenSubtitle = miniDeckSubtitle,
                                installedApps = installedApps,
                                keyboardMode = telemetry.homeKeyboardMode,
                                isPopupOpen = fullScreenKeyboardOpen,
                                widthDp = miniDeckSize.first,
                                heightDp = miniDeckSize.second,
                                editMode = editMode,
                                onKeyPress = onMiniDeckKey,
                                onBackspace = onMiniDeckBackspace,
                                onClear = onMiniDeckClear,
                                onSubmit = onMiniDeckSubmit,
                                onLaunchApp = onLaunchAppFromKeyboard,
                                onToggleKeyboardMode = onToggleHomeKeyboardMode,
                                onOpenFullPopup = { fullScreenKeyboardOpen = true },
                                onResizeDp = onResizeMiniDeck
                            )
                        }
                    }

                    // Bottom Dual Sliders
                    if (telemetry.showBottomSliders) {
                        EditableGlassPlacementBox(
                            widgetKey = "SLIDERS_DECK",
                            editMode = editMode,
                            accentColor = accent,
                            onAccentColor = themeColors.onAccent,
                            currentOffset = widgetOffsets["SLIDERS_DECK"] ?: Pair(0f, 0f),
                            currentScale = widgetScales["SLIDERS_DECK"] ?: 1f,
                            showScaleHandle = false,
                            onUpdateOffset = { dx, dy -> onMoveWidget("SLIDERS_DECK", dx, dy) },
                            onUpdateScale = {}
                        ) {
                            BottomDualSlidersRow(
                                telemetry = telemetry,
                                accent = accent,
                                onSetBrightness = onSetBrightness,
                                onSetSystemWideVolume = onSetSystemWideVolume,
                                modifier = Modifier.width(348.dp)
                            )
                        }
                    }
                }

                // RIGHT WING KNOBS
                Column(
                    modifier = Modifier
                        .width(168.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (slot in rightWingSlots) {
                        val key = "KNOB_$slot"
                        val param = knobSlots.getOrElse(slot) { KnobParameter.CPU_SPEED }
                        EditableGlassPlacementBox(
                            widgetKey = key,
                            editMode = editMode,
                            accentColor = accent,
                            onAccentColor = themeColors.onAccent,
                            currentOffset = widgetOffsets[key] ?: Pair(0f, 0f),
                            currentScale = widgetScales[key] ?: 1f,
                            showScaleHandle = true,
                            onUpdateOffset = { dx, dy -> onMoveWidget(key, dx, dy) },
                            onUpdateScale = { ds -> onScaleWidget(key, ds) }
                        ) {
                            Box {
                                RotaryDial(
                                    parameter = param,
                                    valuePercent = getKnobProgress(param),
                                    accentColor = accent,
                                    knobStyle = telemetry.knobStyle,
                                    editMode = editMode,
                                    dialSize = 128.dp,
                                    onValueChange = { delta -> onAdjustParameter(param, delta) },
                                    onCycleAssignment = { onCycleKnobAssignment(slot) }
                                )
                                if (editMode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-6).dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                            .clickable { onRemoveKnob(slot) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MultiOsIcons.Close(Modifier.size(10.dp), Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Symmetrical Pull Tab Capsule for Vinyl Studio
            if (!editMode && telemetry.showPullTab) {
                MultiOsPullTabCapsule(
                    pullUpward = true,
                    onTrigger = { onNavigate(SurfaceDestination.MUSIC_STUDIO) }
                )
            }
        }

        // =========================================================================
        // SCROLLABLE LIQUID GLASS SIDE PANEL WIDGET & KNOB EDITOR (WITH SWIPE PULL BAR)
        // =========================================================================
        if (editMode) {
            val sideOnLeft = telemetry.sidePanelOnLeft
            var panelExtended by remember { mutableStateOf(true) }
            val panelWidthDp = 208.dp

            val panelOffsetAnim by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (panelExtended) 0.dp else (if (sideOnLeft) (-176).dp else 176.dp),
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
                label = "SidePanelSlide"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(if (sideOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .offset(x = panelOffsetAnim)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = if (sideOnLeft) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!sideOnLeft) {
                        SidePullBarHandle(
                            isExtended = panelExtended,
                            onToggle = { panelExtended = !panelExtended },
                            onSwipeHorizontal = { deltaX ->
                                if (deltaX > 18f) panelExtended = false
                                else if (deltaX < -18f) panelExtended = true
                            }
                        )
                    }

                    LiquidGlassBox(
                        modifier = Modifier
                            .width(panelWidthDp)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(22.dp),
                        borderColor = themeColors.accent.copy(alpha = 0.65f),
                        glassAlpha = if (themeColors.isLight) 0.90f else 0.74f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(themeColors.subtleFill)
                                        .clickable { onToggleSidePanel() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (sideOnLeft) "→ Move Right" else "← Move Left",
                                        color = themeColors.textPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                        .clickable { onDoneEditing() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    MultiOsIcons.Check(Modifier.size(13.dp), themeColors.onAccent)
                                }
                            }

                            Text(
                                text = "Widget & Knob Studio",
                                color = themeColors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (knobSlots.size < 6) {
                                EditDockChip("+ Add Knob", accent, onClick = onAddKnob, modifier = Modifier.fillMaxWidth())
                            }
                            if (launchWidgetPicker != null) {
                                EditDockChip("+ Add 3rd-Party Widget", accent, onClick = { launchWidgetPicker() }, modifier = Modifier.fillMaxWidth())
                            }

                            EditDockChip(
                                label = if (telemetry.showWidgetGlassBorder) "Widgets: Framed" else "Widgets: Frameless",
                                textColor = accent,
                                onClick = onToggleFramelessWidgets,
                                modifier = Modifier.fillMaxWidth()
                            )

                            EditDockChip("Keyboard: ${if (telemetry.showHomeKeyboard) "ON" else "OFF"}", themeColors.textPrimary, onClick = { onToggleShowHomeKeyboard(!telemetry.showHomeKeyboard) }, modifier = Modifier.fillMaxWidth())
                            EditDockChip("Clock: ${if (telemetry.showClockWidget) "ON" else "OFF"}", themeColors.textPrimary, onClick = { onToggleShowClockWidget(!telemetry.showClockWidget) }, modifier = Modifier.fillMaxWidth())
                            EditDockChip("Thermal: ${if (telemetry.showThermalWidget) "ON" else "OFF"}", themeColors.textPrimary, onClick = { onToggleShowThermalWidget(!telemetry.showThermalWidget) }, modifier = Modifier.fillMaxWidth())
                            EditDockChip("Sliders: ${if (telemetry.showBottomSliders) "ON" else "OFF"}", themeColors.textPrimary, onClick = { onToggleShowBottomSliders(!telemetry.showBottomSliders) }, modifier = Modifier.fillMaxWidth())
                            EditDockChip("Messages: ${if (telemetry.showLiveMessagesWidget) "ON" else "OFF"}", themeColors.textPrimary, onClick = { onToggleShowLiveMessages(!telemetry.showLiveMessagesWidget) }, modifier = Modifier.fillMaxWidth())
                            EditDockChip("Battery: ${if (telemetry.showBatteryWidget) "ON" else "OFF"}", themeColors.textPrimary, onClick = { onToggleShowBattery(!telemetry.showBatteryWidget) }, modifier = Modifier.fillMaxWidth())
                            EditDockChip("RAM & Disk: ${if (telemetry.showRamWidget) "ON" else "OFF"}", themeColors.textPrimary, onClick = { onToggleShowRam(!telemetry.showRamWidget) }, modifier = Modifier.fillMaxWidth())
                            EditDockChip("Knob: ${telemetry.knobStyle.title}", themeColors.textPrimary, onClick = onCycleKnobStyle, modifier = Modifier.fillMaxWidth())
                            EditDockChip("Clock: ${telemetry.clockStyle.title}", themeColors.textPrimary, onClick = onCycleClockStyle, modifier = Modifier.fillMaxWidth())
                            EditDockChip("Reset Positions", themeColors.textPrimary, onClick = onResetLayout, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    if (sideOnLeft) {
                        SidePullBarHandle(
                            isExtended = panelExtended,
                            onToggle = { panelExtended = !panelExtended },
                            onSwipeHorizontal = { deltaX ->
                                if (deltaX < -18f) panelExtended = false
                                else if (deltaX > 18f) panelExtended = true
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 4-SIDE RESIZABLE FULL SCREEN KEYBOARD OVERLAY (WITH EXTENDING BASE 1x4 APP STRIP)
        // =========================================================================
        AnimatedVisibility(
            visible = fullScreenKeyboardOpen && telemetry.homeKeyboardMode == HomeKeyboardMode.FULL_POPUP && !editMode,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            HomeFullScreenKeyboardOverlay(
                screenText = miniDeckText,
                installedApps = installedApps,
                heightScale = telemetry.fullKbHeightScale,
                bottomInsetDp = telemetry.fullKbBottomDp,
                leftInsetDp = telemetry.fullKbLeftDp,
                rightInsetDp = telemetry.fullKbRightDp,
                onUpdateBounds = onUpdateFullKbBounds,
                onResetBounds = onResetFullKbBounds,
                onKeyPress = onMiniDeckKey,
                onBackspace = onMiniDeckBackspace,
                onClear = onMiniDeckClear,
                onSubmit = onMiniDeckSubmit,
                onLaunchApp = { app ->
                    onLaunchAppFromKeyboard(app)
                    fullScreenKeyboardOpen = false
                },
                onSwitchToFloater = {
                    fullScreenKeyboardOpen = false
                    onToggleHomeKeyboardMode()
                },
                onDismissPopup = { fullScreenKeyboardOpen = false }
            )
        }
    }
}

/**
 * Thermal Limiter Slider right beside a compact Recessed Mini Box Screen displaying live CPU Temp!
 */
@Composable
private fun ThermalMiniBoxAndSlider(
    cpuTempC: Int,
    thermalLimitC: Int,
    accent: Color,
    sliderStyle: com.galaxy.m21.desktop.ui.theme.SliderStylePreset,
    onThermalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalMultiOsColors.current
    var showTargetCeiling by remember { mutableStateOf(false) }

    LaunchedEffect(thermalLimitC) {
        if (showTargetCeiling) {
            delay(1100)
            showTargetCeiling = false
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LiquidGlassBox(
            modifier = Modifier
                .width(68.dp)
                .height(36.dp),
            shape = RoundedCornerShape(12.dp),
            borderColor = themeColors.subtleBorder,
            glassAlpha = if (themeColors.isLight) 0.78f else 0.65f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(themeColors.recessedScreenBg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MultiOsIcons.Thermometer(
                    modifier = Modifier.size(14.dp),
                    tint = if (showTargetCeiling) Color(0xFFF59E0B) else themeColors.iconTint
                )
                Text(
                    text = if (showTargetCeiling) "${thermalLimitC}°" else "${cpuTempC}°",
                    color = if (showTargetCeiling) Color(0xFFF59E0B) else themeColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        CustomLiquidSlider(
            progress = ((thermalLimitC - 45f) / 40f).coerceIn(0f, 1f),
            accentColor = accent,
            style = sliderStyle,
            showLabels = false,
            glyphType = SliderGlyphType.THERMAL_LIMIT,
            barHeight = 36.dp,
            onProgressChange = { ratio ->
                showTargetCeiling = true
                onThermalChange((45f + ratio * 40f).roundToInt())
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BottomDualSlidersRow(
    telemetry: SystemTelemetryData,
    accent: Color,
    onSetBrightness: (Float) -> Unit,
    onSetSystemWideVolume: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomLiquidSlider(
            progress = telemetry.brightnessLevel,
            accentColor = accent,
            style = telemetry.sliderStyle,
            showLabels = false,
            glyphType = SliderGlyphType.BRIGHTNESS_SUN,
            barHeight = 36.dp,
            onProgressChange = onSetBrightness,
            modifier = Modifier.weight(1f)
        )
        CustomLiquidSlider(
            progress = telemetry.audioProgress,
            accentColor = accent,
            style = telemetry.sliderStyle,
            showLabels = false,
            glyphType = SliderGlyphType.SPEAKER,
            barHeight = 36.dp,
            onProgressChange = onSetSystemWideVolume,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun defaultGetKnobProgress(param: KnobParameter, t: SystemTelemetryData): Float {
    return when (param) {
        KnobParameter.CPU_SPEED -> t.cpuSpeedProgress
        KnobParameter.DISPLAY_BRIGHTNESS -> t.brightnessLevel
        KnobParameter.SYSTEM_AUDIO -> t.audioProgress
        KnobParameter.MUSIC_VOLUME -> t.musicVolumeProgress
        KnobParameter.DJ_VINYL_SCRATCH -> t.cdTrackProgress
        KnobParameter.DJ_BASS_FILTER -> t.djFilterCutoff
    }.coerceIn(0f, 1f)
}

@Composable
private fun EditableGlassPlacementBox(
    widgetKey: String,
    editMode: Boolean,
    accentColor: Color,
    onAccentColor: Color,
    currentOffset: Pair<Float, Float>,
    currentScale: Float,
    showScaleHandle: Boolean,
    onUpdateOffset: (Float, Float) -> Unit,
    onUpdateScale: (Float) -> Unit,
    content: @Composable () -> Unit
) {
    val latestOnOffset by rememberUpdatedState(onUpdateOffset)
    val latestOnScale by rememberUpdatedState(onUpdateScale)

    Box(
        modifier = Modifier
            .offset { IntOffset(currentOffset.first.roundToInt(), currentOffset.second.roundToInt()) }
            .graphicsLayer {
                scaleX = currentScale
                scaleY = currentScale
            }
            .then(
                if (editMode) {
                    Modifier
                        .border(
                            width = 0.9.dp,
                            color = accentColor.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(4.dp)
                        .pointerInput(widgetKey) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                latestOnOffset(dragAmount.x, dragAmount.y)
                            }
                        }
                } else {
                    Modifier
                }
            )
    ) {
        content()

        if (editMode && showScaleHandle) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .pointerInput("${widgetKey}_scale") {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = (dragAmount.x + dragAmount.y) / 260f
                            latestOnScale(delta)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawLine(onAccentColor, Offset(size.width * 0.2f, size.height * 0.8f), Offset(size.width * 0.8f, size.height * 0.2f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun SidePullBarHandle(
    isExtended: Boolean,
    onToggle: () -> Unit,
    onSwipeHorizontal: (Float) -> Unit
) {
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(50))
            .background(themeColors.subtleFill)
            .border(0.6.dp, themeColors.subtleBorder, RoundedCornerShape(50))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onToggle()
            }
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        total += dragAmount
                        if (kotlin.math.abs(total) > 18f) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onSwipeHorizontal(total)
                            total = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(themeColors.textSecondary.copy(alpha = 0.55f))
        )
    }
}

@Composable
private fun EditDockChip(
    label: String,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.SansSerif,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Preview(
    name = "MultiOS Landscape Home Launcher",
    showBackground = true,
    backgroundColor = 0xFF07090E,
    widthDp = 960,
    heightDp = 460
)
@Composable
fun DashboardScreenPreview() {
    var telemetry by remember { mutableStateOf(SystemTelemetryData()) }
    MultiOsTheme(
        appearanceMode = telemetry.appearanceMode,
        accentColor = telemetry.resolveAccentColor(),
        palette = telemetry.colorPalette
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DynamicWallpaperCanvas(
                style = telemetry.wallpaperStyle,
                palette = telemetry.colorPalette,
                modifier = Modifier.fillMaxSize()
            )
            Column(modifier = Modifier.fillMaxSize()) {
                CustomStatusBar(
                    telemetry = telemetry,
                    onPullDownControlCenter = {},
                    onFinishEditMode = { telemetry = telemetry.copy(freeformDragEnabled = false) }
                )
                Box(modifier = Modifier.weight(1f)) {
                    DashboardContent(
                        telemetry = telemetry,
                        onSetSystemWideVolume = { p ->
                            telemetry = telemetry.copy(audioProgress = p, musicVolumeProgress = p)
                        },
                        onSetBrightness = { p ->
                            telemetry = telemetry.copy(brightnessLevel = p)
                        },
                        onSetThermalCeiling = { c ->
                            telemetry = telemetry.copy(thermalThreshold = c)
                        }
                    )
                }
            }
        }
    }
}
