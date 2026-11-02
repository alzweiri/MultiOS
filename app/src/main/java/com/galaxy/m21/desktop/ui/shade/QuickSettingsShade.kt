package com.galaxy.m21.desktop.ui.shade

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.galaxy.m21.desktop.hardware.HardwareViewModel
import com.galaxy.m21.desktop.ui.components.CustomLiquidSlider
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.components.MultiOsPullTabCapsule
import com.galaxy.m21.desktop.ui.components.SliderGlyphType
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.theme.AppAppearanceMode
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * MultiOS Ultra-Optimized Control Center:
 *
 * - Symmetrical Top & Bottom Liquid Glass Pull Tabs for seamless gestures.
 * - 2-Page Swipeable Quick Settings Deck (HorizontalPager with iOS-style morphing page dots):
 *     Page 1 (Primary): Wi-Fi, Bluetooth, Light/Dark Mode, Liquid Glass, Custom Wallpaper,
 *                       Precision Brightness & Volume Sliders, CPU Temp & Thermal Guard.
 *     Page 2 (Security & Kernel): Hardware Location, Camera & Mic Privacy Killswitches,
 *                                  VPN, Airplane Mode, Screen Recorder, CPU Governor,
 *                                  and Magisk Root Bypass Charging (direct motherboard power).
 */
@Composable
fun QuickSettingsShade(
    hardwareViewModel: HardwareViewModel,
    onOpenSettings: () -> Unit,
    onOpenRecents: () -> Unit = {},
    onTriggerEditLayout: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val themeColors = LocalMultiOsColors.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val accent = telemetry.resolveAccentColor()

    val pagerState = rememberPagerState(pageCount = { 2 })

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            hardwareViewModel.applyGalleryWallpaper(uri)
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        hardwareViewModel.toggleBluetooth(context)
    }

    val scrimColor = if (telemetry.liquidGlassEnabled) {
        themeColors.overlayScrimGlass
    } else {
        themeColors.overlayScrimMatte
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrimColor)
            .padding(horizontal = 26.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(760.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =========================================================================
            // TOP HEADER: Clock + Battery Pill + Pager Dots + Utility Buttons
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = telemetry.currentTimeFormatted.take(5),
                        color = themeColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(themeColors.subtleFill)
                            .border(0.6.dp, themeColors.subtleBorder, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${telemetry.batteryPercent}%",
                            color = themeColors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    if (telemetry.bypassChargingRoot) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(accent.copy(alpha = 0.25f))
                                .border(0.8.dp, accent, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "BYPASS ON",
                                color = accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Page Indicator Dots (clickable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(2) { pageIndex ->
                        val isSelected = pagerState.currentPage == pageIndex
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) accent else themeColors.subtleBorder,
                            animationSpec = tween(220)
                        )
                        val dotWidth = if (isSelected) 18.dp else 7.dp
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(50))
                                .background(dotColor)
                                .clickable {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pageIndex)
                                    }
                                }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ControlCenterCircleButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onOpenRecents()
                        }
                    ) {
                        MultiOsIcons.RecentsCards(
                            modifier = Modifier.size(17.dp),
                            tint = themeColors.iconTint
                        )
                    }

                    ControlCenterCircleButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onTriggerEditLayout()
                        }
                    ) {
                        MultiOsIcons.EditGrid(
                            modifier = Modifier.size(17.dp),
                            tint = themeColors.iconTint
                        )
                    }

                    ControlCenterCircleButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onOpenSettings()
                        }
                    ) {
                        MultiOsIcons.Gear(
                            modifier = Modifier.size(18.dp),
                            tint = themeColors.iconTint
                        )
                    }
                }
            }

            // =========================================================================
            // 2-PAGE HORIZONTAL PAGER DECK
            // =========================================================================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) { page ->
                when (page) {
                    0 -> QuickSettingsPrimaryPage(
                        hardwareViewModel = hardwareViewModel,
                        onLaunchGallery = { galleryPicker.launch("image/*") },
                        onRequestBtPermission = { btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
                    )
                    1 -> QuickSettingsKernelAndSecurityPage(
                        hardwareViewModel = hardwareViewModel
                    )
                }
            }

            // =========================================================================
            // SYMMETRICAL BOTTOM PULL TAB CAPSULE (Tap or Drag Up to Close)
            // =========================================================================
            MultiOsPullTabCapsule(
                pullUpward = true,
                onTrigger = onDismiss
            )
        }
    }
}

/**
 * PAGE 1: Connectivity, Theme, Display Brightness, Audio & Thermal Limiter.
 */
@Composable
private fun QuickSettingsPrimaryPage(
    hardwareViewModel: HardwareViewModel,
    onLaunchGallery: () -> Unit,
    onRequestBtPermission: () -> Unit
) {
    val context = LocalContext.current
    val themeColors = LocalMultiOsColors.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val accent = telemetry.resolveAccentColor()
    val isLightMode = telemetry.appearanceMode == AppAppearanceMode.LIGHT

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Wi-Fi, Bluetooth, Light/Dark Mode, Liquid Glass, Custom Wallpaper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ControlCenterIconTile(
                isActive = telemetry.wifiEnabled,
                label = "Wi-Fi",
                accentColor = accent,
                onAccentColor = themeColors.onAccent,
                modifier = Modifier.weight(1f),
                onClick = { hardwareViewModel.toggleWifi(context) }
            ) { tint ->
                MultiOsIcons.Wifi(Modifier.size(20.dp), tint)
            }

            ControlCenterIconTile(
                isActive = telemetry.btEnabled,
                label = "Bluetooth",
                accentColor = accent,
                onAccentColor = themeColors.onAccent,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        onRequestBtPermission()
                    } else {
                        hardwareViewModel.toggleBluetooth(context)
                    }
                }
            ) { tint ->
                MultiOsIcons.Bluetooth(Modifier.size(20.dp), tint)
            }

            ControlCenterIconTile(
                isActive = isLightMode,
                label = if (isLightMode) "Light" else "Dark",
                accentColor = accent,
                onAccentColor = themeColors.onAccent,
                modifier = Modifier.weight(1f),
                onClick = { hardwareViewModel.toggleAppearanceMode() }
            ) { tint ->
                if (isLightMode) {
                    MultiOsIcons.BrightnessSun(Modifier.size(20.dp), tint)
                } else {
                    MultiOsIcons.Moon(Modifier.size(20.dp), tint)
                }
            }

            ControlCenterIconTile(
                isActive = telemetry.liquidGlassEnabled,
                label = "Glass",
                accentColor = accent,
                onAccentColor = themeColors.onAccent,
                modifier = Modifier.weight(1f),
                onClick = { hardwareViewModel.setLiquidGlassEnabled(!telemetry.liquidGlassEnabled) }
            ) { tint ->
                Text(
                    text = "Glass",
                    color = tint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            ControlCenterIconTile(
                isActive = telemetry.hasCustomWallpaper,
                label = "Wallpaper",
                accentColor = accent,
                onAccentColor = themeColors.onAccent,
                modifier = Modifier.weight(1f),
                onClick = onLaunchGallery
            ) { tint ->
                MultiOsIcons.GalleryImage(Modifier.size(20.dp), tint)
            }
        }

        // Row 2: Brightness, System Audio, Music Sliders
        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            borderColor = themeColors.subtleBorder,
            glassAlpha = if (themeColors.isLight) 0.78f else 0.68f
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomLiquidSlider(
                        progress = telemetry.brightnessLevel,
                        accentColor = accent,
                        style = telemetry.sliderStyle,
                        showLabels = false,
                        glyphType = SliderGlyphType.BRIGHTNESS_SUN,
                        barHeight = 36.dp,
                        onProgressChange = { hardwareViewModel.setBrightnessDirect(it) },
                        modifier = Modifier.weight(1f)
                    )
                    if (!telemetry.canWriteSystemSettings && !telemetry.isRootGranted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(accent)
                                .clickable { hardwareViewModel.openWriteSettingsPermission(context) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Sync OS",
                                color = themeColors.onAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }

                CustomLiquidSlider(
                    progress = telemetry.audioProgress,
                    accentColor = accent,
                    style = telemetry.sliderStyle,
                    showLabels = false,
                    glyphType = SliderGlyphType.SPEAKER,
                    barHeight = 36.dp,
                    onProgressChange = { hardwareViewModel.setSystemWideVolumeDirect(it) }
                )

                CustomLiquidSlider(
                    progress = telemetry.musicVolumeProgress,
                    accentColor = accent,
                    style = telemetry.sliderStyle,
                    showLabels = false,
                    glyphType = SliderGlyphType.MUSIC_SPEAKER,
                    barHeight = 36.dp,
                    onProgressChange = { hardwareViewModel.setMusicVolumeDirect(it) }
                )
            }
        }

        // Row 3: CPU Temp & Thermal Slider + Set Default Home Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassBox(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(20.dp),
                borderColor = themeColors.subtleBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MultiOsIcons.Thermometer(Modifier.size(17.dp), themeColors.iconTint)
                    Text(
                        text = "${telemetry.temperatureCpu}°C",
                        color = themeColors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                    CustomLiquidSlider(
                        progress = ((telemetry.thermalThreshold - 45f) / 40f).coerceIn(0f, 1f),
                        accentColor = accent,
                        style = telemetry.sliderStyle,
                        showLabels = false,
                        glyphType = SliderGlyphType.NONE,
                        barHeight = 26.dp,
                        onProgressChange = { r ->
                            hardwareViewModel.setThermalThresholdDirect((45f + r * 40f).roundToInt())
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LiquidGlassBox(
                modifier = Modifier
                    .height(52.dp)
                    .clickable { hardwareViewModel.promptSetDefaultHomeLauncher(context) },
                shape = RoundedCornerShape(20.dp),
                borderColor = themeColors.subtleBorder
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MultiOsIcons.HomeHouse(Modifier.size(18.dp), themeColors.iconTint)
                    Text(
                        text = "Default Home",
                        color = themeColors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

/**
 * PAGE 2: Hardware Privacy Killswitches, Radio, VPN, Screen Recorder, Governor & Bypass Charging.
 */
@Composable
private fun QuickSettingsKernelAndSecurityPage(
    hardwareViewModel: HardwareViewModel
) {
    val context = LocalContext.current
    val themeColors = LocalMultiOsColors.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val accent = telemetry.resolveAccentColor()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Location, Camera Killswitch, Mic Killswitch, VPN, Airplane Mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ControlCenterIconTile(
                isActive = telemetry.locationEnabled,
                label = "Location",
                accentColor = accent,
                onAccentColor = themeColors.onAccent,
                modifier = Modifier.weight(1f),
                onClick = { hardwareViewModel.toggleLocation() }
            ) { tint ->
                MultiOsIcons.LocationPin(Modifier.size(20.dp), tint)
            }

            ControlCenterIconTile(
                isActive = telemetry.cameraKillswitch,
                label = if (telemetry.cameraKillswitch) "Cam Off" else "Cam On",
                accentColor = Color(0xFFEF4444),
                onAccentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = { hardwareViewModel.toggleCameraKillswitch() }
            ) { tint ->
                MultiOsIcons.CameraGlyph(Modifier.size(20.dp), tint)
            }

            ControlCenterIconTile(
                isActive = telemetry.micKillswitch,
                label = if (telemetry.micKillswitch) "Mic Off" else "Mic On",
                accentColor = Color(0xFFEF4444),
                onAccentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = { hardwareViewModel.toggleMicKillswitch() }
            ) { tint ->
                MultiOsIcons.MicGlyph(Modifier.size(20.dp), tint)
            }

            ControlCenterIconTile(
                isActive = false,
                label = "VPN",
                accentColor = accent,
                onAccentColor = themeColors.onAccent,
                modifier = Modifier.weight(1f),
                onClick = { hardwareViewModel.openVpnSettings(context) }
            ) { tint ->
                MultiOsIcons.VpnShield(Modifier.size(20.dp), tint)
            }

            ControlCenterIconTile(
                isActive = telemetry.airplaneModeEnabled,
                label = "Airplane",
                accentColor = accent,
                onAccentColor = themeColors.onAccent,
                modifier = Modifier.weight(1f),
                onClick = { hardwareViewModel.toggleAirplaneMode() }
            ) { tint ->
                MultiOsIcons.AirplaneGlyph(Modifier.size(20.dp), tint)
            }
        }

        // Row 2: Kernel & Power Deck (Screen Record, CPU Governor Cycler, Magisk Bypass Charging)
        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            borderColor = themeColors.subtleBorder,
            glassAlpha = if (themeColors.isLight) 0.78f else 0.68f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Screen Recorder Quick Action
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(themeColors.subtleFill)
                        .border(1.dp, themeColors.subtleBorder, RoundedCornerShape(18.dp))
                        .clickable { hardwareViewModel.launchScreenRecorder(context) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MultiOsIcons.ScreenRecordGlyph(Modifier.size(22.dp), Color(0xFFEF4444))
                        Column {
                            Text(
                                text = "Recorder",
                                color = themeColors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "Screen / Audio",
                                color = themeColors.textSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }

                // CPU Governor Cycler
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .clickable { hardwareViewModel.cycleGovernor() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MultiOsIcons.CpuSpeed(Modifier.size(20.dp), accent)
                        Column {
                            Text(
                                text = "Governor",
                                color = themeColors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = telemetry.governorLabel,
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Magisk Root Bypass Charging Toggle
                val isBypass = telemetry.bypassChargingRoot
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isBypass) accent else themeColors.subtleFill)
                        .border(1.dp, if (isBypass) accent else themeColors.subtleBorder, RoundedCornerShape(18.dp))
                        .clickable { hardwareViewModel.setBypassChargingRoot(!isBypass) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MultiOsIcons.BypassBattery(
                            Modifier.size(22.dp),
                            if (isBypass) themeColors.onAccent else themeColors.iconTint
                        )
                        Column {
                            Text(
                                text = "Bypass Charge",
                                color = if (isBypass) themeColors.onAccent else themeColors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = if (isBypass) "Direct Motherboard" else "Normal Battery",
                                color = if (isBypass) themeColors.onAccent.copy(alpha = 0.85f) else themeColors.textSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
        }

        // Row 3: Live Hardware Status Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HardwarePill(
                title = "GPU Engine",
                subtitle = if (telemetry.lowEndGpuTurbo) "Mali-G72 Turbo" else "Glass Full AGSL",
                modifier = Modifier.weight(1f)
            )
            HardwarePill(
                title = "Magisk Root",
                subtitle = if (telemetry.isRootGranted) "Granted (/sbin/su)" else "Standard Enforced",
                modifier = Modifier.weight(1f)
            )
            HardwarePill(
                title = "Orientation",
                subtitle = telemetry.orientationMode.name.replace("_", " "),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HardwarePill(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalMultiOsColors.current
    LiquidGlassBox(
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        borderColor = themeColors.subtleBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = themeColors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = subtitle,
                color = themeColors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ControlCenterCircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    LiquidGlassBox(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() },
        shape = CircleShape,
        borderColor = themeColors.subtleBorder,
        glassAlpha = if (themeColors.isLight) 0.78f else 0.65f
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun ControlCenterIconTile(
    isActive: Boolean,
    label: String = "",
    accentColor: Color,
    onAccentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    iconContent: @Composable (tint: Color) -> Unit
) {
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    LiquidGlassBox(
        modifier = modifier
            .height(58.dp)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        borderColor = if (isActive) accentColor.copy(alpha = 0.75f) else themeColors.subtleBorder,
        glassAlpha = if (isActive) 0.85f else if (themeColors.isLight) 0.70f else 0.50f
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isActive) accentColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            iconContent(if (isActive) onAccentColor else themeColors.iconTint)
        }
    }
}
