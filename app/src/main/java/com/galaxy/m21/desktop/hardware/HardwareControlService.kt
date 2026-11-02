package com.galaxy.m21.desktop.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.Application
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxy.m21.desktop.service.MultiOsAccessibilityService
import com.galaxy.m21.desktop.ui.navigation.SurfaceDestination
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassConfig
import com.galaxy.m21.desktop.ui.theme.AppAppearanceMode
import com.galaxy.m21.desktop.ui.theme.AppOpeningAnimStyle
import com.galaxy.m21.desktop.ui.theme.ChargingAnimStyle
import com.galaxy.m21.desktop.ui.theme.ClockStylePreset
import com.galaxy.m21.desktop.ui.theme.ColorPalettePreset
import com.galaxy.m21.desktop.ui.theme.DiscFinishStyle
import com.galaxy.m21.desktop.ui.theme.DrawerAppAlignment
import com.galaxy.m21.desktop.ui.theme.DrawerKeyboardMode
import com.galaxy.m21.desktop.ui.theme.GeminiEdgeStyle
import com.galaxy.m21.desktop.ui.theme.GlassTintPreset
import com.galaxy.m21.desktop.ui.theme.HomeKeyboardMode
import com.galaxy.m21.desktop.ui.theme.KnobStylePreset
import com.galaxy.m21.desktop.ui.theme.OrientationMode
import com.galaxy.m21.desktop.ui.theme.SliderStylePreset
import com.galaxy.m21.desktop.ui.theme.SwipeActionTarget
import com.galaxy.m21.desktop.ui.theme.VolumeHudStyle
import com.galaxy.m21.desktop.ui.theme.WallpaperStyle
import com.galaxy.m21.desktop.ui.theme.WelcomeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 6 Minimalist Icon-Communicated Rotary Knob Parameters.
 */
enum class KnobParameter(val displayTitle: String) {
    DISPLAY_BRIGHTNESS("Brightness"),
    SYSTEM_AUDIO("System Volume"),
    MUSIC_VOLUME("Music Volume"),
    CPU_SPEED("CPU Speed"),
    DJ_VINYL_SCRATCH("Vinyl Scrub"),
    DJ_BASS_FILTER("Bass Filter")
}

enum class ScreenSizeMode {
    FIXED_DOCK_2140x1080,
    UNIVERSAL_FULLSCREEN
}

data class CdAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val shortCode: String,
    val bpm: Int,
    val durationSeconds: Int,
    val scaleNotes: FloatArray = floatArrayOf(130.81f, 164.81f, 196f, 246.94f, 261.63f, 329.63f),
    val coverArt: ImageBitmap? = null,
    val localAudioUri: Uri? = null
)

data class AppEntry(
    val name: String,
    val packageName: String,
    val category: String,
    val icon: ImageBitmap? = null
)

data class SystemTelemetryData(
    val cpuBigFreqGhz: Float = 2.31f,
    val cpuLittleFreqGhz: Float = 1.74f,
    val cpuLoad: Float = 0.22f,
    val cpuSpeedProgress: Float = 0.62f,
    val isRootGranted: Boolean = false,
    val temperatureCpu: Int = 40,
    val thermalThreshold: Int = 70,
    val batteryPercent: Int = 100,
    val batteryWattage: Float = 3.0f,
    val governorLabel: String = "SCHEDUTIL",
    val brightnessLevel: Float = 0.78f,
    val canWriteSystemSettings: Boolean = false,
    val systemVolume: Int = 11,
    val maxSystemVolume: Int = 15,
    val audioProgress: Float = 0.73f,
    val musicVolumeProgress: Float = 0.80f,
    val uptimeFormatted: String = "00:00:00",
    val currentTimeFormatted: String = "12:00",
    val wifiEnabled: Boolean = true,
    val btEnabled: Boolean = false,
    val adbWifiEnabled: Boolean = false,
    val oledBurnInProtection: Boolean = true,
    val burnInOffsetX: Int = 0,
    val burnInOffsetY: Int = 0,
    val localIpAddress: String = "192.168.1.100",
    // Appearance, HSV Color & Liquid Glass State
    val appearanceMode: AppAppearanceMode = AppAppearanceMode.DARK,
    val useCustomAccentColor: Boolean = false,
    val customAccentHue: Float = 210f,
    val customAccentSat: Float = 0.70f,
    val customAccentVal: Float = 0.98f,
    val liquidGlassEnabled: Boolean = true,
    val vinylLiquidGlassEnabled: Boolean = true,
    val glassTintPreset: GlassTintPreset = GlassTintPreset.PURE_CLEAR,
    val customGlassHue: Float = 210f,
    val customGlassSat: Float = 0.58f,
    val glassTintIntensity: Float = 0.14f,
    val clockStyle: ClockStylePreset = ClockStylePreset.SWISS_ANALOG,
    val colorPalette: ColorPalettePreset = ColorPalettePreset.APPLE_LIQUID_GLASS,
    val wallpaperStyle: WallpaperStyle = WallpaperStyle.LIQUID_AURORA,
    val syncSystemWallpaper: Boolean = true,
    val hasCustomWallpaper: Boolean = false,
    val knobStyle: KnobStylePreset = KnobStylePreset.LIQUID_GLASS,
    val sliderStyle: SliderStylePreset = SliderStylePreset.LIQUID_CAPSULE,
    val discFinishStyle: DiscFinishStyle = DiscFinishStyle.DARK_VINYL,
    // Gesture, Draggable Side Tab, Recents & Keyboard Customization
    val verticalSwipeFromTabsOnly: Boolean = true,
    val sideTabOnRight: Boolean = true,
    val sideTabVerticalFraction: Float = 0.36f,
    val showRecentsModal: Boolean = false,
    val isAccessibilityRecentsReady: Boolean = false,
    val geminiEdgeStyle: GeminiEdgeStyle = GeminiEdgeStyle.BEZEL_PEEL,
    val geminiAutoRedirect: Boolean = false,
    val homeKeyboardMode: HomeKeyboardMode = HomeKeyboardMode.FLOATING,
    val drawerAppAlignment: DrawerAppAlignment = DrawerAppAlignment.LEFT,
    val drawerKeyboardMode: DrawerKeyboardMode = DrawerKeyboardMode.FLOATING_FREEFORM,
    // 4-Side Resizable Full Screen Keyboard Geometry
    val fullKbHeightScale: Float = 0.92f,
    val fullKbBottomDp: Float = 4f,
    val fullKbLeftDp: Float = 20f,
    val fullKbRightDp: Float = 20f,
    // Viewport & Modal State
    val screenSizeMode: ScreenSizeMode = ScreenSizeMode.UNIVERSAL_FULLSCREEN,
    val customCanvasWidthPx: Int = 2140,
    val customCanvasHeightPx: Int = 1080,
    val bezelMaskHorizontalPx: Int = 0,
    val bezelMaskVerticalPx: Int = 0,
    val customDpi: Int = 280,
    val showPullTab: Boolean = true,
    val freeformDragEnabled: Boolean = false,
    val showSettingsModal: Boolean = false,
    // V4 Features: Wallpaper Blur, Dynamic Widgets, Custom Swipes, Gemini DPI, AOD & Charging
    val homeWallpaperBlurDp: Float = 0f,
    val showHomeKeyboard: Boolean = true,
    val showClockWidget: Boolean = true,
    val showThermalWidget: Boolean = true,
    val showBottomSliders: Boolean = true,
    val showLiveMessagesWidget: Boolean = false,
    val showBatteryWidget: Boolean = false,
    val showRamWidget: Boolean = false,
    val swipeRightTarget: SwipeActionTarget = SwipeActionTarget.GEMINI_WEB,
    val swipeLeftTarget: SwipeActionTarget = SwipeActionTarget.APP_DRAWER,
    val swipeUpTarget: SwipeActionTarget = SwipeActionTarget.VINYL_STUDIO,
    val geminiZoomPercent: Int = 100,
    val geminiWindowW: Float = 880f,
    val geminiWindowH: Float = 640f,
    val aodEnabled: Boolean = false,
    val disableSystemLockScreenRoot: Boolean = false,
    val chargingAnimStyle: ChargingAnimStyle = ChargingAnimStyle.MAGSAFE_LIQUID_RING,
    val isDeviceCharging: Boolean = false,
    val isScreenAodActive: Boolean = false,
    // Ecosystem Upgrades: Orientation, Welcome Gate, Mobile Layout, Hardware Privacy & Bypass Charging
    val orientationMode: OrientationMode = OrientationMode.DESK_ONLY,
    val showWelcomeScreen: Boolean = true,
    val welcomeStyle: WelcomeStyle = WelcomeStyle.IOS_17_CURSIVE,
    val bezelCornerRadiusDp: Float = 28f,
    val enableGeminiSwipePortrait: Boolean = true,
    val geminiDpiScale: Float = 1.0f,
    val mobileGridCols: Int = 5,
    val mobileGridRows: Int = 10,
    val mobileScatterMode: Boolean = false,
    val mobileSearchBarAtTop: Boolean = false,
    val mobileUseSystemIme: Boolean = false,
    val showStockStatusBar: Boolean = false,
    val appOpeningAnimStyle: AppOpeningAnimStyle = AppOpeningAnimStyle.IOS_PERSPECTIVE_BALLOON,
    val volumeHudStyle: VolumeHudStyle = VolumeHudStyle.IOS_SIDE_PILL,
    val showWidgetGlassBorder: Boolean = true,
    val lowEndGpuTurbo: Boolean = true,
    val bypassChargingRoot: Boolean = false,
    val micKillswitch: Boolean = false,
    val cameraKillswitch: Boolean = false,
    val locationEnabled: Boolean = true,
    val airplaneModeEnabled: Boolean = false,
    val sidePanelOnLeft: Boolean = false,
    val isWelcomeGateDismissed: Boolean = false,
    val isOrientationSwitching: Boolean = false,
    val orientationSwitchLabel: String = "",
    // Vinyl & CD Studio State (Empty until user adds files)
    val selectedCdIndex: Int = 0,
    val currentTrackTitle: String = "",
    val currentTrackArtist: String = "",
    val isPlayingAudio: Boolean = false,
    val isScratching: Boolean = false,
    val cdTrackProgress: Float = 0.0f,
    val djPitchSpeed: Float = 1.0f,
    val djBpm: Int = 120,
    val djFilterCutoff: Float = 0.82f,
    val activeRemixFx: Int = 0
) {
    fun resolveAccentColor(): Color {
        return if (useCustomAccentColor) {
            Color.hsv(
                hue = customAccentHue.coerceIn(0f, 360f),
                saturation = customAccentSat.coerceIn(0f, 1f),
                value = customAccentVal.coerceIn(0.18f, 1f)
            )
        } else {
            colorPalette.primary
        }
    }

    fun resolveLiquidGlassConfig(): LiquidGlassConfig {
        val tintColor = if (glassTintPreset == GlassTintPreset.CUSTOM_HUE) {
            Color.hsv(
                hue = customGlassHue.coerceIn(0f, 360f),
                saturation = customGlassSat.coerceIn(0f, 1f),
                value = 1.0f
            )
        } else {
            glassTintPreset.color
        }
        val effectiveStrength = if (glassTintPreset == GlassTintPreset.PURE_CLEAR) {
            glassTintPreset.strength
        } else {
            glassTintIntensity.coerceIn(0.05f, 0.55f)
        }
        return LiquidGlassConfig(
            enabled = liquidGlassEnabled,
            isLightMode = appearanceMode == AppAppearanceMode.LIGHT,
            tintColor = tintColor,
            tintStrength = effectiveStrength,
            lowEndGpuTurbo = lowEndGpuTurbo
        )
    }
}

class HardwareViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("multios_launcher_prefs", Context.MODE_PRIVATE)
    private val wallpaperFile = File(application.filesDir, "multios_custom_wallpaper.jpg")

    // User-only music library (NO prebuilt fake songs!)
    private val _cdCatalog = MutableStateFlow<List<CdAlbum>>(emptyList())
    val cdCatalogState: StateFlow<List<CdAlbum>> = _cdCatalog.asStateFlow()

    private val _customWallpaperBitmap = MutableStateFlow<ImageBitmap?>(null)
    val customWallpaperBitmap: StateFlow<ImageBitmap?> = _customWallpaperBitmap.asStateFlow()

    private val _telemetryState = MutableStateFlow(loadInitialState(application))
    val telemetryState: StateFlow<SystemTelemetryData> = _telemetryState.asStateFlow()

    private val _activeManipulatedKnob = MutableStateFlow<KnobParameter?>(null)
    val activeManipulatedKnob: StateFlow<KnobParameter?> = _activeManipulatedKnob.asStateFlow()

    private val _currentDestination = MutableStateFlow(SurfaceDestination.DASHBOARD)
    val currentDestination: StateFlow<SurfaceDestination> = _currentDestination.asStateFlow()

    private val _overlayBlurProgress = MutableStateFlow(0f)
    val overlayBlurProgress: StateFlow<Float> = _overlayBlurProgress.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppEntry>>(emptyList())
    val installedApps: StateFlow<List<AppEntry>> = _installedApps.asStateFlow()

    private val _recentPackageNames = MutableStateFlow(
        prefs.getString("recent_app_packages", null)
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    )
    private val _recentApps = MutableStateFlow<List<AppEntry>>(emptyList())
    val recentApps: StateFlow<List<AppEntry>> = _recentApps.asStateFlow()

    private val _knobSlotAssignments = MutableStateFlow(loadInitialKnobSlots())
    val knobSlotAssignments: StateFlow<List<KnobParameter>> = _knobSlotAssignments.asStateFlow()

    private val _boundAppWidgetIds = MutableStateFlow(
        prefs.getString("bound_app_widget_ids", null)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: emptyList()
    )
    val boundAppWidgetIds: StateFlow<List<Int>> = _boundAppWidgetIds.asStateFlow()

    private val _blurredRecentPackages = MutableStateFlow(
        prefs.getString("blurred_recent_packages", null)
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()
    )
    val blurredRecentPackages: StateFlow<Set<String>> = _blurredRecentPackages.asStateFlow()

    private val _recentTaskSnapshots = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val recentTaskSnapshots: StateFlow<Map<String, Bitmap>> = _recentTaskSnapshots.asStateFlow()

    // Persisted Home Screen & App Drawer Freeform Layouts
    private val _widgetOffsets = MutableStateFlow(decodeOffsetMap(prefs.getString("home_widget_offsets", null)))
    val widgetOffsets: StateFlow<Map<String, Pair<Float, Float>>> = _widgetOffsets.asStateFlow()

    private val _widgetScales = MutableStateFlow(decodeFloatMap(prefs.getString("home_widget_scales", null)))
    val widgetScales: StateFlow<Map<String, Float>> = _widgetScales.asStateFlow()

    private val _miniDeckSize = MutableStateFlow(
        Pair(
            prefs.getFloat("home_mini_deck_w", 336f).coerceIn(220f, 580f),
            prefs.getFloat("home_mini_deck_h", 156f).coerceIn(120f, 290f)
        )
    )
    val miniDeckSize: StateFlow<Pair<Float, Float>> = _miniDeckSize.asStateFlow()

    private val _miniDeckText = MutableStateFlow("")
    val miniDeckText: StateFlow<String> = _miniDeckText.asStateFlow()

    private val _miniDeckSubtitle = MutableStateFlow("")
    val miniDeckSubtitle: StateFlow<String> = _miniDeckSubtitle.asStateFlow()

    private val _appIconOffsets = MutableStateFlow(decodeOffsetMap(prefs.getString("drawer_app_offsets", null)))
    val appIconOffsets: StateFlow<Map<String, Pair<Float, Float>>> = _appIconOffsets.asStateFlow()

    // Persisted App Drawer Repositionable & Real-Time Resizable Keyboard
    private val _drawerKeyboardOffset = MutableStateFlow(
        Pair(
            prefs.getFloat("drawer_kb_offset_x", 0f),
            prefs.getFloat("drawer_kb_offset_y", 0f)
        )
    )
    val drawerKeyboardOffset: StateFlow<Pair<Float, Float>> = _drawerKeyboardOffset.asStateFlow()

    // Pair(widthDp, heightScaleFactor)
    private val _drawerKeyboardSize = MutableStateFlow(
        Pair(
            prefs.getFloat("drawer_kb_width_dp", 396f).coerceIn(260f, 680f),
            prefs.getFloat("drawer_kb_height_scale", 1.0f).coerceIn(0.72f, 1.45f)
        )
    )
    val drawerKeyboardSize: StateFlow<Pair<Float, Float>> = _drawerKeyboardSize.asStateFlow()

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val djSynthEngine = DjSynthEngine(viewModelScope)
    private var localMediaPlayer: MediaPlayer? = null

    private var telemetryJob: Job? = null
    private var knobResetJob: Job? = null
    private var fxResetJob: Job? = null
    private var cpuScaleJob: Job? = null
    private var thermalWriteJob: Job? = null

    private var internalCpuSpeedProgress = _telemetryState.value.cpuSpeedProgress
    private var internalBrightnessProgress = _telemetryState.value.brightnessLevel
    private var internalAudioProgress = 0.73f
    private var internalMusicVolProgress = 0.80f

    init {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        internalAudioProgress = (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
        internalMusicVolProgress = prefs.getFloat("saved_music_vol", internalAudioProgress).coerceIn(0f, 1f)
        djSynthEngine.musicVolumeGain = internalMusicVolProgress

        val sysBright = runCatching {
            Settings.System.getInt(application.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        }.getOrNull()
        val savedBright = prefs.getFloat("saved_brightness", sysBright ?: 0.78f).coerceIn(0.03f, 1f)
        internalBrightnessProgress = savedBright

        val liveWifi = readRealWifiState()
        val liveBt = readRealBluetoothState(application)
        val canWriteSys = Settings.System.canWrite(application)

        _telemetryState.update {
            it.copy(
                systemVolume = curVol,
                maxSystemVolume = maxVol,
                audioProgress = internalAudioProgress,
                musicVolumeProgress = internalMusicVolProgress,
                brightnessLevel = internalBrightnessProgress,
                wifiEnabled = liveWifi,
                btEnabled = liveBt,
                canWriteSystemSettings = canWriteSys
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (wallpaperFile.exists()) {
                runCatching {
                    BitmapFactory.decodeFile(wallpaperFile.absolutePath)?.asImageBitmap()
                }.getOrNull()?.let { bmp ->
                    _customWallpaperBitmap.value = bmp
                    _telemetryState.update { it.copy(hasCustomWallpaper = true) }
                }
            }
            // Reload user-added audio files persisted across sessions
            loadPersistedUserSongs()
            // Probe Magisk root quietly in background
            val rooted = SysFsBridge.requestMagiskRoot()
            if (rooted) {
                SysFsBridge.enforceImmersiveStatusBarRoot(application.packageName)
                SysFsBridge.grantAppWidgetBindRoot(application.packageName)
                SysFsBridge.enableNotificationListenerRoot(application)
                if (_telemetryState.value.disableSystemLockScreenRoot) {
                    SysFsBridge.setSystemLockScreenDisabledRoot(true)
                }
            }
            _telemetryState.update {
                it.copy(
                    isRootGranted = rooted,
                    isAccessibilityRecentsReady = MultiOsAccessibilityService.isConnected || rooted
                )
            }
        }

        djSynthEngine.startEngine { _ ->
            // Synth FX engine stays idle unless FX pad is pressed while user track plays
        }
    }

    private fun loadInitialState(app: Application): SystemTelemetryData {
        val appearanceOrd = prefs.getInt("appearance_mode_ordinal", 0).coerceIn(0, AppAppearanceMode.entries.lastIndex)
        val useCustomAccent = prefs.getBoolean("use_custom_accent", false)
        val accentHue = prefs.getFloat("custom_accent_hue", 210f)
        val accentSat = prefs.getFloat("custom_accent_sat", 0.70f)
        val accentVal = prefs.getFloat("custom_accent_val", 0.98f)
        val glassOn = prefs.getBoolean("liquid_glass_enabled", true)
        val vinylGlassOn = prefs.getBoolean("vinyl_liquid_glass_enabled", true)
        val tintOrd = prefs.getInt("glass_tint_ordinal", 0).coerceIn(0, GlassTintPreset.entries.lastIndex)
        val customHue = prefs.getFloat("custom_glass_hue", 210f)
        val customSat = prefs.getFloat("custom_glass_sat", 0.58f)
        val tintIntensity = prefs.getFloat("glass_tint_intensity", 0.22f)
        val clockOrd = prefs.getInt("clock_style_ordinal", 0).coerceIn(0, ClockStylePreset.entries.lastIndex)
        val paletteOrd = prefs.getInt("palette_ordinal", 0).coerceIn(0, ColorPalettePreset.entries.lastIndex)
        val wallOrd = prefs.getInt("wallpaper_style_ordinal", 0).coerceIn(0, WallpaperStyle.entries.lastIndex)
        val syncSysWall = prefs.getBoolean("sync_system_wallpaper", true)
        val knobOrd = prefs.getInt("knob_style_ordinal", 0).coerceIn(0, KnobStylePreset.entries.lastIndex)
        val sliderOrd = prefs.getInt("slider_style_ordinal", 0).coerceIn(0, SliderStylePreset.entries.lastIndex)
        val discOrd = prefs.getInt("disc_finish_ordinal", 0).coerceIn(0, DiscFinishStyle.entries.lastIndex)
        val tabsOnly = prefs.getBoolean("vertical_swipe_tabs_only", true)
        val sideOnRight = prefs.getBoolean("side_tab_on_right", true)
        val sideYFrac = prefs.getFloat("side_tab_y_fraction", 0.36f).coerceIn(0.10f, 0.88f)
        val geminiEdgeOrd = prefs.getInt("gemini_edge_ordinal", 0).coerceIn(0, GeminiEdgeStyle.entries.lastIndex)
        val geminiRedirect = prefs.getBoolean("gemini_auto_redirect", false)
        val homeKbOrd = prefs.getInt("home_kb_mode_ordinal", 0).coerceIn(0, HomeKeyboardMode.entries.lastIndex)
        val drawerAlignOrd = prefs.getInt("drawer_align_ordinal", 0).coerceIn(0, DrawerAppAlignment.entries.lastIndex)
        val drawerKbOrd = prefs.getInt("drawer_kb_mode_ordinal", 0).coerceIn(0, DrawerKeyboardMode.entries.lastIndex)
        val fullKbH = prefs.getFloat("full_kb_height_scale", 0.92f).coerceIn(0.65f, 1.55f)
        val fullKbBottom = prefs.getFloat("full_kb_bottom_dp", 4f).coerceIn(0f, 140f)
        val fullKbLeft = prefs.getFloat("full_kb_left_dp", 20f).coerceIn(0f, 260f)
        val fullKbRight = prefs.getFloat("full_kb_right_dp", 20f).coerceIn(0f, 260f)
        val dpi = prefs.getInt("custom_dpi", 280).coerceIn(200, 420)
        val thermal = prefs.getInt("thermal_threshold_c", 70).coerceIn(45, 85)

        val wallBlur = prefs.getFloat("home_wallpaper_blur_dp", 0f).coerceIn(0f, 35f)
        val showHomeKb = prefs.getBoolean("show_home_keyboard", true)
        val showClock = prefs.getBoolean("show_clock_widget", true)
        val showThermal = prefs.getBoolean("show_thermal_widget", true)
        val showBottomSl = prefs.getBoolean("show_bottom_sliders", true)
        val showLiveMsg = prefs.getBoolean("show_live_messages_widget", false)
        val showBatt = prefs.getBoolean("show_battery_widget", false)
        val showRam = prefs.getBoolean("show_ram_widget", false)

        val swipeROrd = prefs.getInt("swipe_right_ordinal", SwipeActionTarget.GEMINI_WEB.ordinal).coerceIn(0, SwipeActionTarget.entries.lastIndex)
        val swipeLOrd = prefs.getInt("swipe_left_ordinal", SwipeActionTarget.APP_DRAWER.ordinal).coerceIn(0, SwipeActionTarget.entries.lastIndex)
        val swipeUOrd = prefs.getInt("swipe_up_ordinal", SwipeActionTarget.VINYL_STUDIO.ordinal).coerceIn(0, SwipeActionTarget.entries.lastIndex)

        val geminiZoom = prefs.getInt("gemini_zoom_percent", 100).coerceIn(50, 180)
        val geminiW = prefs.getFloat("gemini_window_w", 880f).coerceIn(400f, 1600f)
        val geminiH = prefs.getFloat("gemini_window_h", 640f).coerceIn(300f, 1000f)

        val aodOn = prefs.getBoolean("aod_enabled", false)
        val lockOff = prefs.getBoolean("disable_sys_lock_screen_root", false)
        val chargeAnimOrd = prefs.getInt("charging_anim_ordinal", ChargingAnimStyle.MAGSAFE_LIQUID_RING.ordinal).coerceIn(0, ChargingAnimStyle.entries.lastIndex)

        val orientOrd = prefs.getInt("orientation_mode_ordinal", OrientationMode.DESK_ONLY.ordinal).coerceIn(0, OrientationMode.entries.lastIndex)
        val welcomeOn = prefs.getBoolean("show_welcome_screen", true)
        val welcomeOrd = prefs.getInt("welcome_style_ordinal", WelcomeStyle.IOS_17_CURSIVE.ordinal).coerceIn(0, WelcomeStyle.entries.lastIndex)
        val bezelRadius = prefs.getFloat("bezel_corner_radius_dp", 28f).coerceIn(0f, 48f)
        val geminiPortraitOn = prefs.getBoolean("enable_gemini_swipe_portrait", true)
        val mCols = prefs.getInt("mobile_grid_cols", 5).coerceIn(2, 12)
        val mRows = prefs.getInt("mobile_grid_rows", 10).coerceIn(2, 20)
        val mScatter = prefs.getBoolean("mobile_scatter_mode", false)
        val mSearchTop = prefs.getBoolean("mobile_search_top", false)
        val mSysIme = prefs.getBoolean("mobile_use_system_ime", false)
        val mStockStatus = prefs.getBoolean("show_stock_status_bar", false)
        val animOrd = prefs.getInt("app_anim_ordinal", AppOpeningAnimStyle.IOS_PERSPECTIVE_BALLOON.ordinal).coerceIn(0, AppOpeningAnimStyle.entries.lastIndex)
        val volHudOrd = prefs.getInt("vol_hud_ordinal", VolumeHudStyle.IOS_SIDE_PILL.ordinal).coerceIn(0, VolumeHudStyle.entries.lastIndex)
        val widgetBorderOn = prefs.getBoolean("show_widget_glass_border", true)
        val gpuTurboOn = prefs.getBoolean("low_end_gpu_turbo", true)
        val bypassChargeOn = prefs.getBoolean("bypass_charging_root", false)
        val sideLeft = prefs.getBoolean("side_panel_on_left", false)

        return SystemTelemetryData(
            appearanceMode = AppAppearanceMode.entries[appearanceOrd],
            useCustomAccentColor = useCustomAccent,
            customAccentHue = accentHue,
            customAccentSat = accentSat,
            customAccentVal = accentVal,
            liquidGlassEnabled = glassOn,
            vinylLiquidGlassEnabled = vinylGlassOn,
            glassTintPreset = GlassTintPreset.entries[tintOrd],
            customGlassHue = customHue,
            customGlassSat = customSat,
            glassTintIntensity = tintIntensity,
            clockStyle = ClockStylePreset.entries[clockOrd],
            colorPalette = ColorPalettePreset.entries[paletteOrd],
            wallpaperStyle = WallpaperStyle.entries[wallOrd],
            syncSystemWallpaper = syncSysWall,
            knobStyle = KnobStylePreset.entries[knobOrd],
            sliderStyle = SliderStylePreset.entries[sliderOrd],
            discFinishStyle = DiscFinishStyle.entries[discOrd],
            verticalSwipeFromTabsOnly = tabsOnly,
            sideTabOnRight = sideOnRight,
            sideTabVerticalFraction = sideYFrac,
            isAccessibilityRecentsReady = MultiOsAccessibilityService.isConnected,
            geminiEdgeStyle = GeminiEdgeStyle.entries[geminiEdgeOrd],
            geminiAutoRedirect = geminiRedirect,
            homeKeyboardMode = HomeKeyboardMode.entries[homeKbOrd],
            drawerAppAlignment = DrawerAppAlignment.entries[drawerAlignOrd],
            drawerKeyboardMode = DrawerKeyboardMode.entries[drawerKbOrd],
            fullKbHeightScale = fullKbH,
            fullKbBottomDp = fullKbBottom,
            fullKbLeftDp = fullKbLeft,
            fullKbRightDp = fullKbRight,
            customDpi = dpi,
            thermalThreshold = thermal,
            canWriteSystemSettings = Settings.System.canWrite(app),
            homeWallpaperBlurDp = wallBlur,
            showHomeKeyboard = showHomeKb,
            showClockWidget = showClock,
            showThermalWidget = showThermal,
            showBottomSliders = showBottomSl,
            showLiveMessagesWidget = showLiveMsg,
            showBatteryWidget = showBatt,
            showRamWidget = showRam,
            swipeRightTarget = SwipeActionTarget.entries[swipeROrd],
            swipeLeftTarget = SwipeActionTarget.entries[swipeLOrd],
            swipeUpTarget = SwipeActionTarget.entries[swipeUOrd],
            geminiZoomPercent = geminiZoom,
            geminiWindowW = geminiW,
            geminiWindowH = geminiH,
            aodEnabled = aodOn,
            disableSystemLockScreenRoot = lockOff,
            chargingAnimStyle = ChargingAnimStyle.entries[chargeAnimOrd],
            orientationMode = OrientationMode.entries[orientOrd],
            showWelcomeScreen = welcomeOn,
            welcomeStyle = WelcomeStyle.entries[welcomeOrd],
            bezelCornerRadiusDp = bezelRadius,
            enableGeminiSwipePortrait = geminiPortraitOn,
            mobileGridCols = mCols,
            mobileGridRows = mRows,
            mobileScatterMode = mScatter,
            mobileSearchBarAtTop = mSearchTop,
            mobileUseSystemIme = mSysIme,
            showStockStatusBar = mStockStatus,
            appOpeningAnimStyle = AppOpeningAnimStyle.entries[animOrd],
            volumeHudStyle = VolumeHudStyle.entries[volHudOrd],
            showWidgetGlassBorder = widgetBorderOn,
            lowEndGpuTurbo = gpuTurboOn,
            bypassChargingRoot = bypassChargeOn,
            sidePanelOnLeft = sideLeft
        )
    }

    private fun loadInitialKnobSlots(): List<KnobParameter> {
        val csv = prefs.getString("knob_slots_csv", null)
        if (!csv.isNullOrBlank()) {
            val parsed = csv.split(",").mapNotNull { s ->
                s.trim().toIntOrNull()?.let { ord ->
                    KnobParameter.entries.getOrNull(ord)
                }
            }
            if (parsed.isNotEmpty()) return parsed.take(6)
        }
        val defaults = listOf(
            KnobParameter.DISPLAY_BRIGHTNESS,
            KnobParameter.SYSTEM_AUDIO,
            KnobParameter.CPU_SPEED,
            KnobParameter.MUSIC_VOLUME
        )
        return (0..3).map { idx ->
            val ord = prefs.getInt("knob_slot_$idx", defaults[idx].ordinal)
            KnobParameter.entries.getOrElse(ord) { defaults[idx] }
        }
    }

    override fun onCleared() {
        super.onCleared()
        djSynthEngine.stopEngine()
        runCatching { localMediaPlayer?.release() }
        localMediaPlayer = null
    }

    // =========================================================================
    // PERSISTENCE ENCODERS / DECODERS FOR LAYOUTS & OFFSETS
    // =========================================================================

    private fun encodeOffsetMap(map: Map<String, Pair<Float, Float>>): String {
        return map.entries.joinToString("|") { (k, v) -> "$k:${v.first},${v.second}" }
    }

    private fun decodeOffsetMap(raw: String?): Map<String, Pair<Float, Float>> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = mutableMapOf<String, Pair<Float, Float>>()
        raw.split("|").forEach { entry ->
            val kv = entry.split(":")
            if (kv.size == 2) {
                val xy = kv[1].split(",")
                if (xy.size == 2) {
                    val x = xy[0].toFloatOrNull()
                    val y = xy[1].toFloatOrNull()
                    if (x != null && y != null) out[kv[0]] = Pair(x, y)
                }
            }
        }
        return out
    }

    private fun encodeFloatMap(map: Map<String, Float>): String {
        return map.entries.joinToString("|") { (k, v) -> "$k:$v" }
    }

    private fun decodeFloatMap(raw: String?): Map<String, Float> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = mutableMapOf<String, Float>()
        raw.split("|").forEach { entry ->
            val kv = entry.split(":")
            if (kv.size == 2) {
                kv[1].toFloatOrNull()?.let { out[kv[0]] = it }
            }
        }
        return out
    }

    // =========================================================================
    // NAVIGATION, SETTINGS MODAL & PERSISTED LAYOUT PLACEMENTS
    // =========================================================================

    fun requestNavigate(destination: SurfaceDestination) {
        _currentDestination.value = destination
        if (destination != SurfaceDestination.QUICK_SETTINGS && destination != SurfaceDestination.DASHBOARD) {
            _telemetryState.update { it.copy(showSettingsModal = false, showRecentsModal = false) }
        } else if (destination != SurfaceDestination.DASHBOARD) {
            _telemetryState.update { it.copy(showRecentsModal = false) }
        }
    }

    fun updateOverlayBlurProgress(progress: Float) {
        _overlayBlurProgress.value = progress.coerceIn(0f, 1f)
    }

    fun openSettingsModal() {
        val canWrite = Settings.System.canWrite(getApplication())
        _telemetryState.update {
            it.copy(
                showSettingsModal = true,
                showRecentsModal = false,
                canWriteSystemSettings = canWrite,
                isAccessibilityRecentsReady = MultiOsAccessibilityService.isConnected || SysFsBridge.hasMagiskRoot
            )
        }
    }

    fun closeSettingsModal() {
        _telemetryState.update { it.copy(showSettingsModal = false) }
    }

    fun openRecentsDeck() {
        refreshRecentAppsFromInstalled()
        refreshRecentTaskSnapshots()
        _telemetryState.update {
            it.copy(
                showRecentsModal = true,
                showSettingsModal = false,
                isAccessibilityRecentsReady = MultiOsAccessibilityService.isConnected || SysFsBridge.hasMagiskRoot
            )
        }
    }

    fun closeRecentsDeck() {
        _telemetryState.update { it.copy(showRecentsModal = false) }
    }

    fun toggleRecentsDeck() {
        if (_telemetryState.value.showRecentsModal) closeRecentsDeck() else openRecentsDeck()
    }

    fun triggerNativeSystemRecents(context: Context) {
        closeRecentsDeck()
        if (MultiOsAccessibilityService.triggerSystemRecents()) return
        if (SysFsBridge.hasMagiskRoot) {
            viewModelScope.launch(Dispatchers.IO) {
                SysFsBridge.triggerSystemRecentsRoot()
            }
            return
        }
        runCatching {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun removeRecentApp(packageName: String) {
        _recentPackageNames.update { cur ->
            val updated = cur.filter { it != packageName }
            prefs.edit().putString("recent_app_packages", updated.joinToString("|")).apply()
            updated
        }
        refreshRecentAppsFromInstalled()
    }

    fun clearRecentApps() {
        _recentPackageNames.value = emptyList()
        _recentApps.value = emptyList()
        prefs.edit().putString("recent_app_packages", "").apply()
    }

    private fun recordAppLaunchInRecents(packageName: String) {
        _recentPackageNames.update { cur ->
            val next = (listOf(packageName) + cur.filter { it != packageName }).take(14)
            prefs.edit().putString("recent_app_packages", next.joinToString("|")).apply()
            next
        }
        refreshRecentAppsFromInstalled()
    }

    private fun refreshRecentAppsFromInstalled() {
        val all = _installedApps.value
        if (all.isEmpty()) return
        val byPkg = all.associateBy { it.packageName }
        val saved = _recentPackageNames.value
        val resolved = saved.mapNotNull { byPkg[it] }
        _recentApps.value = if (resolved.isNotEmpty() || prefs.contains("recent_app_packages")) {
            resolved
        } else {
            all.take(6)
        }
    }

    /**
     * Direct normal ("3adi") Side Pull Tab repositioning — NO Edit Mode needed!
     */
    fun updateSideTabPosition(onRight: Boolean, verticalFraction: Float) {
        val clampedY = verticalFraction.coerceIn(0.10f, 0.88f)
        prefs.edit()
            .putBoolean("side_tab_on_right", onRight)
            .putFloat("side_tab_y_fraction", clampedY)
            .apply()
        _telemetryState.update {
            it.copy(
                sideTabOnRight = onRight,
                sideTabVerticalFraction = clampedY
            )
        }
    }

    /**
     * 4-Side Resizable Full Screen Keyboard (Top height scale, Bottom lift clearance, Left & Right insets).
     */
    fun updateFullKeyboardBounds(
        dHeightScale: Float = 0f,
        dBottomDp: Float = 0f,
        dLeftDp: Float = 0f,
        dRightDp: Float = 0f
    ) {
        _telemetryState.update { cur ->
            val nextH = (cur.fullKbHeightScale + dHeightScale).coerceIn(0.65f, 1.55f)
            val nextBottom = (cur.fullKbBottomDp + dBottomDp).coerceIn(0f, 140f)
            val nextLeft = (cur.fullKbLeftDp + dLeftDp).coerceIn(0f, 260f)
            val nextRight = (cur.fullKbRightDp + dRightDp).coerceIn(0f, 260f)
            prefs.edit()
                .putFloat("full_kb_height_scale", nextH)
                .putFloat("full_kb_bottom_dp", nextBottom)
                .putFloat("full_kb_left_dp", nextLeft)
                .putFloat("full_kb_right_dp", nextRight)
                .apply()
            cur.copy(
                fullKbHeightScale = nextH,
                fullKbBottomDp = nextBottom,
                fullKbLeftDp = nextLeft,
                fullKbRightDp = nextRight
            )
        }
    }

    fun resetFullKeyboardBounds() {
        prefs.edit()
            .putFloat("full_kb_height_scale", 0.92f)
            .putFloat("full_kb_bottom_dp", 4f)
            .putFloat("full_kb_left_dp", 20f)
            .putFloat("full_kb_right_dp", 20f)
            .apply()
        _telemetryState.update {
            it.copy(
                fullKbHeightScale = 0.92f,
                fullKbBottomDp = 4f,
                fullKbLeftDp = 20f,
                fullKbRightDp = 20f
            )
        }
    }

    fun toggleFreeformDragMode() {
        _telemetryState.update {
            it.copy(
                freeformDragEnabled = !it.freeformDragEnabled,
                showSettingsModal = false,
                showRecentsModal = false
            )
        }
        if (_currentDestination.value != SurfaceDestination.DASHBOARD) {
            _currentDestination.value = SurfaceDestination.DASHBOARD
        }
    }

    fun moveWidget(key: String, dx: Float, dy: Float) {
        _widgetOffsets.update { map ->
            val cur = map[key] ?: Pair(0f, 0f)
            val updated = map + (key to Pair(cur.first + dx, cur.second + dy))
            prefs.edit().putString("home_widget_offsets", encodeOffsetMap(updated)).apply()
            updated
        }
    }

    fun scaleWidget(key: String, scaleDelta: Float) {
        _widgetScales.update { map ->
            val cur = map[key] ?: 1f
            val updated = map + (key to (cur + scaleDelta).coerceIn(0.68f, 1.55f))
            prefs.edit().putString("home_widget_scales", encodeFloatMap(updated)).apply()
            updated
        }
    }

    fun resizeMiniDeck(dwDp: Float, dhDp: Float) {
        _miniDeckSize.update { cur ->
            val updated = Pair(
                (cur.first + dwDp).coerceIn(220f, 580f),
                (cur.second + dhDp).coerceIn(120f, 290f)
            )
            prefs.edit()
                .putFloat("home_mini_deck_w", updated.first)
                .putFloat("home_mini_deck_h", updated.second)
                .apply()
            updated
        }
    }

    fun resetWidgetOffsets() {
        _widgetOffsets.value = emptyMap()
        _widgetScales.value = emptyMap()
        _miniDeckSize.value = Pair(336f, 156f)
        prefs.edit()
            .remove("home_widget_offsets")
            .remove("home_widget_scales")
            .putFloat("home_mini_deck_w", 336f)
            .putFloat("home_mini_deck_h", 156f)
            .apply()
    }

    fun moveAppIcon(packageName: String, dx: Float, dy: Float) {
        _appIconOffsets.update { map ->
            val cur = map[packageName] ?: Pair(0f, 0f)
            val updated = map + (packageName to Pair(cur.first + dx, cur.second + dy))
            prefs.edit().putString("drawer_app_offsets", encodeOffsetMap(updated)).apply()
            updated
        }
    }

    fun resetAppIconOffsets() {
        _appIconOffsets.value = emptyMap()
        prefs.edit().remove("drawer_app_offsets").apply()
    }

    fun moveDrawerKeyboard(dx: Float, dy: Float) {
        _drawerKeyboardOffset.update { cur ->
            val next = Pair(
                (cur.first + dx).coerceIn(-680f, 680f),
                (cur.second + dy).coerceIn(-340f, 340f)
            )
            prefs.edit()
                .putFloat("drawer_kb_offset_x", next.first)
                .putFloat("drawer_kb_offset_y", next.second)
                .apply()
            next
        }
    }

    fun resizeDrawerKeyboard(dwDp: Float, dHeightScale: Float) {
        _drawerKeyboardSize.update { cur ->
            val next = Pair(
                (cur.first + dwDp).coerceIn(260f, 680f),
                (cur.second + dHeightScale).coerceIn(0.72f, 1.45f)
            )
            prefs.edit()
                .putFloat("drawer_kb_width_dp", next.first)
                .putFloat("drawer_kb_height_scale", next.second)
                .apply()
            next
        }
    }

    fun resetDrawerKeyboardLayout() {
        _drawerKeyboardOffset.value = Pair(0f, 0f)
        _drawerKeyboardSize.value = Pair(396f, 1.0f)
        prefs.edit()
            .putFloat("drawer_kb_offset_x", 0f)
            .putFloat("drawer_kb_offset_y", 0f)
            .putFloat("drawer_kb_width_dp", 396f)
            .putFloat("drawer_kb_height_scale", 1.0f)
            .apply()
    }

    // =========================================================================
    // GALLERY WALLPAPER & CUSTOM ALBUM COVER PICKER
    // =========================================================================

    fun setSyncSystemWallpaper(enabled: Boolean) {
        prefs.edit().putBoolean("sync_system_wallpaper", enabled).apply()
        _telemetryState.update { it.copy(syncSystemWallpaper = enabled) }
    }

    fun applyGalleryWallpaper(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val decoded = decodeSampledBitmapFromUri(app, uri, maxDim = 2400) ?: return@launch
                FileOutputStream(wallpaperFile).use { out ->
                    decoded.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                _customWallpaperBitmap.value = decoded.asImageBitmap()
                _telemetryState.update { it.copy(hasCustomWallpaper = true) }

                // Sync picked wallpaper directly to Android Home Screen AND Lock Screen!
                if (_telemetryState.value.syncSystemWallpaper) {
                    runCatching {
                        WallpaperManager.getInstance(app).setBitmap(
                            decoded,
                            null,
                            true,
                            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                        )
                    }
                }
            }
        }
    }

    fun syncCurrentWallpaperToSystem() {
        val app = getApplication<Application>()
        setSyncSystemWallpaper(true)
        viewModelScope.launch(Dispatchers.IO) {
            if (wallpaperFile.exists()) {
                runCatching {
                    val bmp = BitmapFactory.decodeFile(wallpaperFile.absolutePath) ?: return@runCatching
                    WallpaperManager.getInstance(app).setBitmap(
                        bmp,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    )
                }
            }
        }
    }

    fun useLivePhoneSystemWallpaper() {
        setSyncSystemWallpaper(true)
        clearCustomWallpaper()
    }

    fun clearCustomWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { if (wallpaperFile.exists()) wallpaperFile.delete() }
            _customWallpaperBitmap.value = null
            _telemetryState.update { it.copy(hasCustomWallpaper = false) }
        }
    }

    fun applyCustomAlbumCoverForSelectedDisc(uri: Uri) {
        val app = getApplication<Application>()
        val targetIdx = _telemetryState.value.selectedCdIndex
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = decodeSampledBitmapFromUri(app, uri, maxDim = 600) ?: return@launch
            val imgBmp = bmp.asImageBitmap()
            _cdCatalog.update { list ->
                if (targetIdx !in list.indices) list
                else list.toMutableList().apply {
                    set(targetIdx, list[targetIdx].copy(coverArt = imgBmp))
                }
            }
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxDim || boundsOpts.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }

    // =========================================================================
    // LIGHT/DARK MODE, HSV COLOR SELECTOR, LIQUID GLASS & CUSTOMIZATION
    // =========================================================================

    fun setAppearanceMode(mode: AppAppearanceMode) {
        prefs.edit().putInt("appearance_mode_ordinal", mode.ordinal).apply()
        _telemetryState.update { it.copy(appearanceMode = mode) }
    }

    fun toggleAppearanceMode() {
        val next = if (_telemetryState.value.appearanceMode == AppAppearanceMode.DARK) {
            AppAppearanceMode.LIGHT
        } else {
            AppAppearanceMode.DARK
        }
        setAppearanceMode(next)
    }

    fun setCustomAccentHsv(
        hue: Float = _telemetryState.value.customAccentHue,
        sat: Float = _telemetryState.value.customAccentSat,
        value: Float = _telemetryState.value.customAccentVal
    ) {
        val h = hue.coerceIn(0f, 360f)
        val s = sat.coerceIn(0f, 1f)
        val v = value.coerceIn(0.18f, 1f)
        prefs.edit()
            .putBoolean("use_custom_accent", true)
            .putFloat("custom_accent_hue", h)
            .putFloat("custom_accent_sat", s)
            .putFloat("custom_accent_val", v)
            .apply()
        _telemetryState.update {
            it.copy(
                useCustomAccentColor = true,
                customAccentHue = h,
                customAccentSat = s,
                customAccentVal = v
            )
        }
    }

    fun selectPalettePreset(preset: ColorPalettePreset) {
        prefs.edit()
            .putBoolean("use_custom_accent", false)
            .putInt("palette_ordinal", preset.ordinal)
            .apply()
        _telemetryState.update {
            it.copy(
                useCustomAccentColor = false,
                colorPalette = preset
            )
        }
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("liquid_glass_enabled", enabled).apply()
        _telemetryState.update { it.copy(liquidGlassEnabled = enabled) }
    }

    fun setVinylLiquidGlassEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vinyl_liquid_glass_enabled", enabled).apply()
        _telemetryState.update { it.copy(vinylLiquidGlassEnabled = enabled) }
    }

    fun toggleVinylLiquidGlass() {
        setVinylLiquidGlassEnabled(!_telemetryState.value.vinylLiquidGlassEnabled)
    }

    fun selectGlassTintPreset(preset: GlassTintPreset) {
        prefs.edit().putInt("glass_tint_ordinal", preset.ordinal).apply()
        _telemetryState.update {
            it.copy(
                glassTintPreset = preset,
                glassTintIntensity = if (preset == GlassTintPreset.PURE_CLEAR) 0.08f else it.glassTintIntensity.coerceAtLeast(0.20f)
            )
        }
    }

    fun setCustomGlassHue(hueDegrees: Float) {
        setCustomGlassHsv(hueDegrees, _telemetryState.value.customGlassSat)
    }

    fun setCustomGlassHsv(hueDegrees: Float, saturation: Float) {
        val h = hueDegrees.coerceIn(0f, 360f)
        val s = saturation.coerceIn(0f, 1f)
        prefs.edit()
            .putInt("glass_tint_ordinal", GlassTintPreset.CUSTOM_HUE.ordinal)
            .putFloat("custom_glass_hue", h)
            .putFloat("custom_glass_sat", s)
            .apply()
        _telemetryState.update {
            it.copy(
                glassTintPreset = GlassTintPreset.CUSTOM_HUE,
                customGlassHue = h,
                customGlassSat = s
            )
        }
    }

    fun setGlassTintIntensity(strength: Float) {
        val clamped = strength.coerceIn(0.05f, 0.55f)
        prefs.edit().putFloat("glass_tint_intensity", clamped).apply()
        _telemetryState.update { it.copy(glassTintIntensity = clamped) }
    }

    fun setVerticalSwipeFromTabsOnly(tabsOnly: Boolean) {
        prefs.edit().putBoolean("vertical_swipe_tabs_only", tabsOnly).apply()
        _telemetryState.update { it.copy(verticalSwipeFromTabsOnly = tabsOnly) }
    }

    fun setGeminiEdgeStyle(style: GeminiEdgeStyle) {
        prefs.edit().putInt("gemini_edge_ordinal", style.ordinal).apply()
        _telemetryState.update { it.copy(geminiEdgeStyle = style) }
    }

    fun setGeminiAutoRedirect(enabled: Boolean) {
        prefs.edit().putBoolean("gemini_auto_redirect", enabled).apply()
        _telemetryState.update { it.copy(geminiAutoRedirect = enabled) }
    }

    fun setHomeKeyboardMode(mode: HomeKeyboardMode) {
        prefs.edit().putInt("home_kb_mode_ordinal", mode.ordinal).apply()
        _telemetryState.update { it.copy(homeKeyboardMode = mode) }
    }

    fun toggleHomeKeyboardMode() {
        val next = if (_telemetryState.value.homeKeyboardMode == HomeKeyboardMode.FLOATING) {
            HomeKeyboardMode.FULL_POPUP
        } else {
            HomeKeyboardMode.FLOATING
        }
        setHomeKeyboardMode(next)
    }

    fun setDrawerAppAlignment(alignment: DrawerAppAlignment) {
        prefs.edit().putInt("drawer_align_ordinal", alignment.ordinal).apply()
        _telemetryState.update { it.copy(drawerAppAlignment = alignment) }
    }

    fun setDrawerKeyboardMode(mode: DrawerKeyboardMode) {
        prefs.edit().putInt("drawer_kb_mode_ordinal", mode.ordinal).apply()
        _telemetryState.update { it.copy(drawerKeyboardMode = mode) }
    }

    fun toggleDrawerKeyboardMode() {
        val next = if (_telemetryState.value.drawerKeyboardMode == DrawerKeyboardMode.FLOATING_FREEFORM) {
            DrawerKeyboardMode.FULL_LANDSCAPE_GBOARD
        } else {
            DrawerKeyboardMode.FLOATING_FREEFORM
        }
        setDrawerKeyboardMode(next)
    }

    fun selectClockStyle(style: ClockStylePreset) {
        prefs.edit().putInt("clock_style_ordinal", style.ordinal).apply()
        _telemetryState.update { it.copy(clockStyle = style) }
    }

    fun cycleClockStyle() {
        val entries = ClockStylePreset.entries
        selectClockStyle(entries[(_telemetryState.value.clockStyle.ordinal + 1) % entries.size])
    }

    fun selectKnobStyle(style: KnobStylePreset) {
        prefs.edit().putInt("knob_style_ordinal", style.ordinal).apply()
        _telemetryState.update { it.copy(knobStyle = style) }
    }

    fun cycleKnobStyle() {
        val entries = KnobStylePreset.entries
        selectKnobStyle(entries[(_telemetryState.value.knobStyle.ordinal + 1) % entries.size])
    }

    fun toggleDiscFinishStyle() {
        val next = if (_telemetryState.value.discFinishStyle == DiscFinishStyle.DARK_VINYL) {
            DiscFinishStyle.SILVER_OPTICAL
        } else {
            DiscFinishStyle.DARK_VINYL
        }
        prefs.edit().putInt("disc_finish_ordinal", next.ordinal).apply()
        _telemetryState.update { it.copy(discFinishStyle = next) }
    }

    fun cycleWallpaperStyle() {
        val entries = WallpaperStyle.entries
        val next = entries[(_telemetryState.value.wallpaperStyle.ordinal + 1) % entries.size]
        prefs.edit()
            .putInt("wallpaper_style_ordinal", next.ordinal)
            .putBoolean("sync_system_wallpaper", false)
            .apply()
        _telemetryState.update {
            it.copy(
                wallpaperStyle = next,
                syncSystemWallpaper = false,
                hasCustomWallpaper = false
            )
        }
    }

    fun selectSliderStyle(style: SliderStylePreset) {
        prefs.edit().putInt("slider_style_ordinal", style.ordinal).apply()
        _telemetryState.update { it.copy(sliderStyle = style) }
    }

    fun setCustomDpi(dpi: Int) {
        val clamped = dpi.coerceIn(200, 400)
        prefs.edit().putInt("custom_dpi", clamped).apply()
        _telemetryState.update { it.copy(customDpi = clamped) }
    }

    fun requestMagiskRootInteractive() {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val granted = SysFsBridge.requestMagiskRoot()
            if (granted) {
                SysFsBridge.enforceImmersiveStatusBarRoot(app.packageName)
            }
            _telemetryState.update {
                it.copy(
                    isRootGranted = granted,
                    isAccessibilityRecentsReady = MultiOsAccessibilityService.isConnected || granted
                )
            }
        }
    }

    fun openWriteSettingsPermission(context: Context) {
        runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun promptSetDefaultHomeLauncher(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context is Activity) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                        context.startActivityForResult(intent, 9041)
                        return
                    }
                }
            }
            val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(homeSettings)
        }.onFailure {
            openNativeAndroidSettings(context)
        }
    }

    fun launchExternalGeminiWithFade(context: Context): Boolean {
        val pm = context.packageManager
        val fadeOpts = ActivityOptions.makeCustomAnimation(
            context,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        ).toBundle()

        val geminiPackages = listOf(
            "com.google.android.apps.bard",
            "com.google.android.googlequicksearchbox"
        )
        for (pkg in geminiPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null && pkg == "com.google.android.apps.bard") {
                return runCatching {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent, fadeOpts)
                    true
                }.getOrDefault(false)
            }
        }

        val assistIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (assistIntent.resolveActivity(pm) != null) {
            return runCatching {
                context.startActivity(assistIntent, fadeOpts)
                true
            }.getOrDefault(false)
        }
        return false
    }

    // =========================================================================
    // MINI KEYBOARD-WITH-SCREEN DECK & 1x4 APP SEARCH LOGIC
    // =========================================================================

    fun appendMiniDeckKey(char: String) {
        val next = (_miniDeckText.value + char).take(48)
        _miniDeckText.value = next
        updateMiniDeckPreviewHint(next)
    }

    fun backspaceMiniDeck() {
        if (_miniDeckText.value.isNotEmpty()) {
            val next = _miniDeckText.value.dropLast(1)
            _miniDeckText.value = next
            updateMiniDeckPreviewHint(next)
        }
    }

    fun clearMiniDeck() {
        _miniDeckText.value = ""
        _miniDeckSubtitle.value = ""
    }

    private fun updateMiniDeckPreviewHint(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            _miniDeckSubtitle.value = ""
            return
        }
        val mathResult = evaluateSimpleMath(trimmed)
        if (mathResult != null) {
            _miniDeckSubtitle.value = "= $mathResult"
            return
        }
        val matchedApp = _installedApps.value.firstOrNull {
            it.name.contains(trimmed, ignoreCase = true)
        }
        _miniDeckSubtitle.value = matchedApp?.name ?: ""
    }

    fun submitMiniDeck(context: Context) {
        val query = _miniDeckText.value.trim()
        if (query.isEmpty()) return

        val mathResult = evaluateSimpleMath(query)
        if (mathResult != null) {
            _miniDeckText.value = mathResult
            _miniDeckSubtitle.value = ""
            return
        }

        val matchedApp = _installedApps.value.firstOrNull {
            it.name.contains(query, ignoreCase = true)
        }
        if (matchedApp != null) {
            launchApp(context, matchedApp)
            _miniDeckText.value = ""
            _miniDeckSubtitle.value = ""
        } else {
            _miniDeckSubtitle.value = query
        }
    }

    private fun evaluateSimpleMath(expr: String): String? {
        val clean = expr.replace(" ", "")
        val match = Regex("^(-?\\d+(?:\\.\\d+)?)([+\\-*/])(-?\\d+(?:\\.\\d+)?)$").matchEntire(clean) ?: return null
        val a = match.groupValues[1].toDoubleOrNull() ?: return null
        val op = match.groupValues[2]
        val b = match.groupValues[3].toDoubleOrNull() ?: return null
        val res = when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0.0) a / b else return null
            else -> return null
        }
        return if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.2f", res)
    }

    fun addKnobSlot(param: KnobParameter = KnobParameter.SYSTEM_AUDIO) {
        _knobSlotAssignments.update { cur ->
            if (cur.size >= 6) return@update cur
            val next = cur + param
            saveKnobSlots(next)
            next
        }
    }

    fun removeKnobSlotAt(index: Int) {
        _knobSlotAssignments.update { cur ->
            if (index !in cur.indices) return@update cur
            val next = cur.toMutableList().apply { removeAt(index) }
            saveKnobSlots(next)
            next
        }
    }

    private fun saveKnobSlots(slots: List<KnobParameter>) {
        prefs.edit().putString("knob_slots_csv", slots.joinToString(",") { it.ordinal.toString() }).apply()
    }

    fun cycleKnobSlotAssignment(slotIndex: Int) {
        val allParams = KnobParameter.entries
        _knobSlotAssignments.update { currentList ->
            if (slotIndex !in currentList.indices) return@update currentList
            val curParam = currentList[slotIndex]
            val nextParam = allParams[(curParam.ordinal + 1) % allParams.size]
            val updated = currentList.toMutableList().apply { set(slotIndex, nextParam) }
            saveKnobSlots(updated)
            updated
        }
    }

    fun addBoundAppWidget(appWidgetId: Int) {
        _boundAppWidgetIds.update { cur ->
            if (appWidgetId !in cur) {
                val next = cur + appWidgetId
                prefs.edit().putString("bound_app_widget_ids", next.joinToString(",")).apply()
                next
            } else cur
        }
    }

    fun removeBoundAppWidget(appWidgetId: Int) {
        _boundAppWidgetIds.update { cur ->
            val next = cur.filter { it != appWidgetId }
            prefs.edit().putString("bound_app_widget_ids", next.joinToString(",")).apply()
            next
        }
    }

    fun toggleRecentPackageBlur(packageName: String) {
        _blurredRecentPackages.update { cur ->
            val next = if (packageName in cur) cur - packageName else cur + packageName
            prefs.edit().putString("blurred_recent_packages", next.joinToString("|")).apply()
            next
        }
    }

    fun isPackageBlurredInRecents(packageName: String): Boolean = packageName in _blurredRecentPackages.value

    fun refreshRecentTaskSnapshots() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val map = SysFsBridge.fetchRecentTaskSnapshotsRoot(app)
            _recentTaskSnapshots.value = map
        }
    }

    fun setOrientationMode(mode: OrientationMode) {
        prefs.edit().putInt("orientation_mode_ordinal", mode.ordinal).apply()
        _telemetryState.update { it.copy(orientationMode = mode) }
    }

    fun setShowWelcomeScreen(show: Boolean) {
        prefs.edit().putBoolean("show_welcome_screen", show).apply()
        _telemetryState.update { it.copy(showWelcomeScreen = show) }
    }

    fun setWelcomeStyle(style: WelcomeStyle) {
        prefs.edit().putInt("welcome_style_ordinal", style.ordinal).apply()
        _telemetryState.update { it.copy(welcomeStyle = style) }
    }

    fun setBezelCornerRadiusDp(radius: Float) {
        val clamped = radius.coerceIn(0f, 48f)
        prefs.edit().putFloat("bezel_corner_radius_dp", clamped).apply()
        _telemetryState.update { it.copy(bezelCornerRadiusDp = clamped) }
    }

    fun setEnableGeminiSwipePortrait(enabled: Boolean) {
        prefs.edit().putBoolean("enable_gemini_swipe_portrait", enabled).apply()
        _telemetryState.update { it.copy(enableGeminiSwipePortrait = enabled) }
    }

    fun setMobileGrid(cols: Int, rows: Int) {
        val c = cols.coerceIn(2, 12)
        val r = rows.coerceIn(2, 20)
        prefs.edit().putInt("mobile_grid_cols", c).putInt("mobile_grid_rows", r).apply()
        _telemetryState.update { it.copy(mobileGridCols = c, mobileGridRows = r) }
    }

    fun setMobileScatterMode(enabled: Boolean) {
        prefs.edit().putBoolean("mobile_scatter_mode", enabled).apply()
        _telemetryState.update { it.copy(mobileScatterMode = enabled) }
    }

    fun setMobileSearchBarAtTop(atTop: Boolean) {
        prefs.edit().putBoolean("mobile_search_top", atTop).apply()
        _telemetryState.update { it.copy(mobileSearchBarAtTop = atTop) }
    }

    fun setMobileUseSystemIme(useSystem: Boolean) {
        prefs.edit().putBoolean("mobile_use_system_ime", useSystem).apply()
        _telemetryState.update { it.copy(mobileUseSystemIme = useSystem) }
    }

    fun setShowStockStatusBar(show: Boolean) {
        prefs.edit().putBoolean("show_stock_status_bar", show).apply()
        _telemetryState.update { it.copy(showStockStatusBar = show) }
    }

    fun setAppOpeningAnimStyle(style: AppOpeningAnimStyle) {
        prefs.edit().putInt("app_anim_ordinal", style.ordinal).apply()
        _telemetryState.update { it.copy(appOpeningAnimStyle = style) }
    }

    fun setVolumeHudStyle(style: VolumeHudStyle) {
        prefs.edit().putInt("vol_hud_ordinal", style.ordinal).apply()
        _telemetryState.update { it.copy(volumeHudStyle = style) }
    }

    fun setShowWidgetGlassBorder(show: Boolean) {
        prefs.edit().putBoolean("show_widget_glass_border", show).apply()
        _telemetryState.update { it.copy(showWidgetGlassBorder = show) }
    }

    fun setLowEndGpuTurbo(enabled: Boolean) {
        prefs.edit().putBoolean("low_end_gpu_turbo", enabled).apply()
        _telemetryState.update { it.copy(lowEndGpuTurbo = enabled) }
    }

    fun setBypassChargingRoot(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            SysFsBridge.setBypassChargingRoot(enabled)
            prefs.edit().putBoolean("bypass_charging_root", enabled).apply()
            _telemetryState.update { it.copy(bypassChargingRoot = enabled) }
        }
    }

    fun toggleMicKillswitch() {
        val next = !_telemetryState.value.micKillswitch
        viewModelScope.launch(Dispatchers.IO) {
            SysFsBridge.setSensorPrivacyRoot(1, next)
            _telemetryState.update { it.copy(micKillswitch = next) }
        }
    }

    fun toggleCameraKillswitch() {
        val next = !_telemetryState.value.cameraKillswitch
        viewModelScope.launch(Dispatchers.IO) {
            SysFsBridge.setSensorPrivacyRoot(2, next)
            _telemetryState.update { it.copy(cameraKillswitch = next) }
        }
    }

    fun toggleLocation() {
        val next = !_telemetryState.value.locationEnabled
        viewModelScope.launch(Dispatchers.IO) {
            SysFsBridge.setLocationEnabledRoot(next)
            _telemetryState.update { it.copy(locationEnabled = next) }
        }
    }

    fun toggleAirplaneMode() {
        val next = !_telemetryState.value.airplaneModeEnabled
        viewModelScope.launch(Dispatchers.IO) {
            SysFsBridge.setAirplaneModeRoot(next)
            _telemetryState.update { it.copy(airplaneModeEnabled = next) }
        }
    }

    fun openVpnSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun launchScreenRecorder(context: Context) {
        runCatching {
            val intent = Intent("com.android.systemui.screenrecord.START").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.sendBroadcast(intent)
        }.onFailure {
            viewModelScope.launch(Dispatchers.IO) {
                SysFsBridge.executeRootCommand("cmd statusbar collapse ; input keyevent 120 2>/dev/null")
            }
        }
    }

    fun cycleGovernor() {
        val current = _telemetryState.value.governorLabel.uppercase()
        val nextGov = when (current) {
            "SCHEDUTIL" -> "PERFORMANCE"
            "PERFORMANCE" -> "POWERSAVE"
            else -> "SCHEDUTIL"
        }
        val targetProgress = when (nextGov) {
            "PERFORMANCE" -> 0.95f
            "POWERSAVE" -> 0.15f
            else -> 0.60f
        }
        setCpuSpeedNormalized(targetProgress)
    }

    fun setSidePanelOnLeft(onLeft: Boolean) {
        prefs.edit().putBoolean("side_panel_on_left", onLeft).apply()
        _telemetryState.update { it.copy(sidePanelOnLeft = onLeft) }
    }

    fun dismissWelcomeGate() {
        _telemetryState.update { it.copy(isWelcomeGateDismissed = true) }
        viewModelScope.launch(Dispatchers.IO) {
            SysFsBridge.dismissKeyguardRoot()
        }
    }

    fun triggerOrientationSwitchBanner(targetLabel: String) {
        viewModelScope.launch {
            _telemetryState.update { it.copy(isOrientationSwitching = true, orientationSwitchLabel = targetLabel) }
            delay(1200L)
            _telemetryState.update { it.copy(isOrientationSwitching = false) }
        }
    }

    fun setHomeWallpaperBlurDp(blurDp: Float) {
        val clamped = blurDp.coerceIn(0f, 35f)
        prefs.edit().putFloat("home_wallpaper_blur_dp", clamped).apply()
        _telemetryState.update { it.copy(homeWallpaperBlurDp = clamped) }
    }

    fun openSystemLiveWallpaperChooser(context: Context) {
        runCatching {
            val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun selectWallpaperStyle(style: WallpaperStyle) {
        prefs.edit()
            .putInt("wallpaper_style_ordinal", style.ordinal)
            .putBoolean("sync_system_wallpaper", false)
            .apply()
        _telemetryState.update {
            it.copy(wallpaperStyle = style, syncSystemWallpaper = false, hasCustomWallpaper = false)
        }
    }

    /** Grants live-messages notification access silently via root, falling back to the system screen. */
    fun enableLiveMessagesAccess(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = SysFsBridge.enableNotificationListenerRoot(context.applicationContext)
            if (!ok) {
                launch(Dispatchers.Main) {
                    runCatching {
                        context.startActivity(
                            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        }
    }

    fun setShowHomeKeyboard(show: Boolean) {
        prefs.edit().putBoolean("show_home_keyboard", show).apply()
        _telemetryState.update { it.copy(showHomeKeyboard = show) }
    }

    fun setShowClockWidget(show: Boolean) {
        prefs.edit().putBoolean("show_clock_widget", show).apply()
        _telemetryState.update { it.copy(showClockWidget = show) }
    }

    fun setShowThermalWidget(show: Boolean) {
        prefs.edit().putBoolean("show_thermal_widget", show).apply()
        _telemetryState.update { it.copy(showThermalWidget = show) }
    }

    fun setShowBottomSliders(show: Boolean) {
        prefs.edit().putBoolean("show_bottom_sliders", show).apply()
        _telemetryState.update { it.copy(showBottomSliders = show) }
    }

    fun setShowLiveMessagesWidget(show: Boolean) {
        prefs.edit().putBoolean("show_live_messages_widget", show).apply()
        _telemetryState.update { it.copy(showLiveMessagesWidget = show) }
    }

    fun setShowBatteryWidget(show: Boolean) {
        prefs.edit().putBoolean("show_battery_widget", show).apply()
        _telemetryState.update { it.copy(showBatteryWidget = show) }
    }

    fun setShowRamWidget(show: Boolean) {
        prefs.edit().putBoolean("show_ram_widget", show).apply()
        _telemetryState.update { it.copy(showRamWidget = show) }
    }

    fun setSwipeRightTarget(target: SwipeActionTarget) {
        prefs.edit().putInt("swipe_right_ordinal", target.ordinal).apply()
        _telemetryState.update { it.copy(swipeRightTarget = target) }
    }

    fun setSwipeLeftTarget(target: SwipeActionTarget) {
        prefs.edit().putInt("swipe_left_ordinal", target.ordinal).apply()
        _telemetryState.update { it.copy(swipeLeftTarget = target) }
    }

    fun setSwipeUpTarget(target: SwipeActionTarget) {
        prefs.edit().putInt("swipe_up_ordinal", target.ordinal).apply()
        _telemetryState.update { it.copy(swipeUpTarget = target) }
    }

    fun setGeminiZoomPercent(zoom: Int) {
        val clamped = zoom.coerceIn(50, 180)
        prefs.edit().putInt("gemini_zoom_percent", clamped).apply()
        _telemetryState.update { it.copy(geminiZoomPercent = clamped) }
    }

    fun setGeminiWindowSize(w: Float, h: Float) {
        val clampedW = w.coerceIn(400f, 1600f)
        val clampedH = h.coerceIn(300f, 1000f)
        prefs.edit().putFloat("gemini_window_w", clampedW).putFloat("gemini_window_h", clampedH).apply()
        _telemetryState.update { it.copy(geminiWindowW = clampedW, geminiWindowH = clampedH) }
    }

    fun setAodEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("aod_enabled", enabled).apply()
        _telemetryState.update { it.copy(aodEnabled = enabled) }
    }

    fun setChargingAnimStyle(style: ChargingAnimStyle) {
        prefs.edit().putInt("charging_anim_ordinal", style.ordinal).apply()
        _telemetryState.update { it.copy(chargingAnimStyle = style) }
    }

    fun setDisableSystemLockScreenRoot(disable: Boolean) {
        prefs.edit().putBoolean("disable_sys_lock_screen_root", disable).apply()
        _telemetryState.update { it.copy(disableSystemLockScreenRoot = disable) }
        viewModelScope.launch(Dispatchers.IO) {
            SysFsBridge.setSystemLockScreenDisabledRoot(disable)
        }
    }

    fun updateChargingState(isCharging: Boolean) {
        _telemetryState.update { it.copy(isDeviceCharging = isCharging) }
    }

    fun setScreenAodActive(active: Boolean) {
        _telemetryState.update { it.copy(isScreenAodActive = active) }
    }

    fun getKnobProgress(param: KnobParameter, t: SystemTelemetryData): Float {
        return when (param) {
            KnobParameter.CPU_SPEED -> t.cpuSpeedProgress
            KnobParameter.DISPLAY_BRIGHTNESS -> t.brightnessLevel
            KnobParameter.SYSTEM_AUDIO -> t.audioProgress
            KnobParameter.MUSIC_VOLUME -> t.musicVolumeProgress
            KnobParameter.DJ_VINYL_SCRATCH -> t.cdTrackProgress
            KnobParameter.DJ_BASS_FILTER -> t.djFilterCutoff
        }.coerceIn(0f, 1f)
    }

    // =========================================================================
    // USER-ONLY VINYL / CD AUDIO LIBRARY (`+` FROM FILES) & TONEARM ENGINE
    // =========================================================================

    fun addSongsFromFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val savedStrings = loadSavedSongUriStrings().toMutableList()
            val addedAlbums = mutableListOf<CdAlbum>()

            for (uri in uris) {
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val uriStr = uri.toString()
                if (uriStr !in savedStrings) {
                    savedStrings.add(uriStr)
                }
                parseAudioUriToAlbum(app, uri, _cdCatalog.value.size + addedAlbums.size)?.let { album ->
                    if (_cdCatalog.value.none { it.localAudioUri == uri }) {
                        addedAlbums.add(album)
                    }
                }
            }

            saveSongUriStrings(savedStrings)

            if (addedAlbums.isNotEmpty()) {
                val wasEmpty = _cdCatalog.value.isEmpty()
                val updatedList = _cdCatalog.value + addedAlbums
                _cdCatalog.value = updatedList
                if (wasEmpty) {
                    val first = updatedList.first()
                    _telemetryState.update {
                        it.copy(
                            selectedCdIndex = 0,
                            currentTrackTitle = first.title,
                            currentTrackArtist = first.artist,
                            cdTrackProgress = 0f
                        )
                    }
                } else {
                    selectCdFromRack(updatedList.lastIndex)
                }
            }
        }
    }

    fun removeSongAt(index: Int) {
        val current = _cdCatalog.value
        if (index !in current.indices) return
        val removed = current[index]
        val updated = current.toMutableList().apply { removeAt(index) }
        _cdCatalog.value = updated

        removed.localAudioUri?.toString()?.let { remStr ->
            val saved = loadSavedSongUriStrings().filter { it != remStr }
            saveSongUriStrings(saved)
        }

        if (updated.isEmpty()) {
            stopLocalMediaPlayer()
            djSynthEngine.isPlaying = false
            _telemetryState.update {
                it.copy(
                    selectedCdIndex = 0,
                    currentTrackTitle = "",
                    currentTrackArtist = "",
                    isPlayingAudio = false,
                    cdTrackProgress = 0f
                )
            }
        } else {
            val nextIdx = index.coerceAtMost(updated.lastIndex)
            val nextCd = updated[nextIdx]
            stopLocalMediaPlayer()
            _telemetryState.update {
                it.copy(
                    selectedCdIndex = nextIdx,
                    currentTrackTitle = nextCd.title,
                    currentTrackArtist = nextCd.artist,
                    isPlayingAudio = false,
                    cdTrackProgress = 0f
                )
            }
        }
    }

    private fun loadPersistedUserSongs() {
        val app = getApplication<Application>()
        val uriStrings = loadSavedSongUriStrings()
        if (uriStrings.isEmpty()) return
        val loaded = uriStrings.mapIndexedNotNull { idx, str ->
            runCatching { parseAudioUriToAlbum(app, Uri.parse(str), idx) }.getOrNull()
        }
        _cdCatalog.value = loaded
        loaded.firstOrNull()?.let { first ->
            _telemetryState.update {
                it.copy(
                    selectedCdIndex = 0,
                    currentTrackTitle = first.title,
                    currentTrackArtist = first.artist
                )
            }
        }
    }

    private fun loadSavedSongUriStrings(): List<String> {
        val raw = prefs.getString("user_vinyl_song_uris", null) ?: return emptyList()
        return raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun saveSongUriStrings(uris: List<String>) {
        prefs.edit().putString("user_vinyl_song_uris", uris.joinToString("\n")).apply()
    }

    private fun parseAudioUriToAlbum(context: Context, uri: Uri, idx: Int): CdAlbum? {
        var metaTitle: String? = null
        var metaArtist: String? = null
        var durationSec = 180
        var coverBitmap: ImageBitmap? = null

        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            metaTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            metaArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { ms ->
                durationSec = (ms / 1000L).toInt().coerceAtLeast(1)
            }
            mmr.embeddedPicture?.let { bytes ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.let { bmp ->
                    coverBitmap = bmp.asImageBitmap()
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { mmr.release() }
        }

        if (metaTitle.isNullOrBlank()) {
            metaTitle = queryDisplayName(context, uri)
                ?.substringBeforeLast('.')
                ?.takeIf { it.isNotBlank() }
                ?: "Track ${idx + 1}"
        }
        val finalArtist = metaArtist ?: "Local Audio"

        return CdAlbum(
            id = "user_${uri.hashCode()}_$idx",
            title = metaTitle!!,
            artist = finalArtist,
            shortCode = String.format(Locale.US, "%02d", idx + 1),
            bpm = 120,
            durationSeconds = durationSec,
            coverArt = coverBitmap,
            localAudioUri = uri
        )
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
    }

    fun selectCdFromRack(index: Int, autoPlay: Boolean = true) {
        val catalog = _cdCatalog.value
        val cd = catalog.getOrElse(index) { return }

        stopLocalMediaPlayer()
        djSynthEngine.isPlaying = false

        if (autoPlay && cd.localAudioUri != null) {
            startLocalMediaTrack(cd.localAudioUri, startProgress = 0f, autoStart = true)
        }

        _telemetryState.update {
            it.copy(
                selectedCdIndex = index,
                currentTrackTitle = cd.title,
                currentTrackArtist = cd.artist,
                isPlayingAudio = autoPlay && cd.localAudioUri != null,
                cdTrackProgress = 0f,
                djBpm = (cd.bpm * it.djPitchSpeed).roundToInt()
            )
        }
    }

    fun skipTrack(delta: Int) {
        val catalog = _cdCatalog.value
        if (catalog.isEmpty()) return
        val nextIdx = (_telemetryState.value.selectedCdIndex + delta).mod(catalog.size)
        selectCdFromRack(nextIdx, autoPlay = true)
    }

    fun togglePlayPauseCd() {
        setCdPlayingDirect(!_telemetryState.value.isPlayingAudio)
    }

    /**
     * Explicitly sets playback ON or OFF (driven by Play/Pause button OR lifting/dropping the Tonearm!).
     */
    fun setCdPlayingDirect(shouldPlay: Boolean) {
        val catalog = _cdCatalog.value
        if (catalog.isEmpty()) {
            _telemetryState.update { it.copy(isPlayingAudio = false) }
            return
        }
        val cd = catalog.getOrElse(_telemetryState.value.selectedCdIndex) { catalog.first() }
        val uri = cd.localAudioUri ?: return

        djSynthEngine.isPlaying = false
        if (shouldPlay) {
            if (localMediaPlayer == null) {
                startLocalMediaTrack(uri, startProgress = _telemetryState.value.cdTrackProgress, autoStart = true)
            } else {
                runCatching { localMediaPlayer?.start() }
            }
        } else {
            runCatching { localMediaPlayer?.pause() }
        }
        _telemetryState.update {
            it.copy(
                currentTrackTitle = cd.title,
                currentTrackArtist = cd.artist,
                isPlayingAudio = shouldPlay
            )
        }
    }

    /**
     * Seeks accurately to radial vinyl groove position (`0.0f` outer edge .. `1.0f` inner label edge)
     * when the user places or drags the Tonearm needle onto the record!
     */
    fun seekCdToProgress(progress: Float, autoPlay: Boolean) {
        val catalog = _cdCatalog.value
        if (catalog.isEmpty()) return
        val clamped = progress.coerceIn(0f, 0.99f)
        val cd = catalog.getOrElse(_telemetryState.value.selectedCdIndex) { catalog.first() }
        val uri = cd.localAudioUri ?: return

        if (localMediaPlayer == null) {
            startLocalMediaTrack(uri, startProgress = clamped, autoStart = autoPlay)
        } else {
            localMediaPlayer?.let { mp ->
                runCatching {
                    if (mp.duration > 0) {
                        mp.seekTo((clamped * mp.duration).roundToInt().coerceIn(0, mp.duration))
                    }
                    if (autoPlay && !mp.isPlaying) {
                        mp.start()
                    }
                }
            }
        }

        _telemetryState.update {
            it.copy(
                cdTrackProgress = clamped,
                isPlayingAudio = if (autoPlay) true else it.isPlayingAudio
            )
        }
    }

    private fun startLocalMediaTrack(uri: Uri, startProgress: Float = 0f, autoStart: Boolean = true) {
        val app = getApplication<Application>()
        runCatching {
            val mp = MediaPlayer().apply {
                setDataSource(app, uri)
                isLooping = true
                val vol = internalMusicVolProgress.coerceIn(0f, 1f)
                setVolume(vol, vol)
                prepare()
                if (startProgress > 0.005f && duration > 0) {
                    seekTo((startProgress * duration).roundToInt().coerceIn(0, duration))
                }
                if (autoStart) start()
            }
            localMediaPlayer = mp
        }
    }

    private fun stopLocalMediaPlayer() {
        runCatching {
            localMediaPlayer?.stop()
            localMediaPlayer?.release()
        }
        localMediaPlayer = null
    }

    fun setScratchState(scratching: Boolean) {
        _telemetryState.update { it.copy(isScratching = scratching) }
    }

    fun scratchCdByAngle(deltaDegrees: Float) {
        if (_cdCatalog.value.isEmpty()) return
        val progressDelta = deltaDegrees / 1440f
        _telemetryState.update { cur ->
            var nextProg = (cur.cdTrackProgress + progressDelta) % 1.0f
            if (nextProg < 0f) nextProg += 1.0f
            localMediaPlayer?.let { mp ->
                runCatching {
                    if (mp.duration > 0) {
                        mp.seekTo((nextProg * mp.duration).roundToInt().coerceIn(0, mp.duration))
                    }
                }
            }
            cur.copy(cdTrackProgress = nextProg)
        }
    }

    fun setDjFilterCutoff(cutoff: Float) {
        val clamped = cutoff.coerceIn(0.05f, 1.0f)
        djSynthEngine.filterCutoff = clamped
        _telemetryState.update { it.copy(djFilterCutoff = clamped) }
    }

    fun triggerDjRemixFx(fxId: Int) {
        djSynthEngine.activeRemixFx = fxId
        djSynthEngine.isPlaying = true
        _telemetryState.update { it.copy(activeRemixFx = fxId) }
        fxResetJob?.cancel()
        fxResetJob = viewModelScope.launch {
            delay(900)
            djSynthEngine.activeRemixFx = 0
            djSynthEngine.isPlaying = false
            _telemetryState.update { it.copy(activeRemixFx = 0) }
        }
    }

    // =========================================================================
    // HARDWARE, BRIGHTNESS, CONNECTIVITY & REAL-TIME LINKED AUDIO ENGINE
    // =========================================================================

    fun adjustParameter(param: KnobParameter, delta: Float) {
        markKnobActive(param)

        when (param) {
            KnobParameter.CPU_SPEED -> {
                internalCpuSpeedProgress = (internalCpuSpeedProgress + delta).coerceIn(0f, 1f)
                setCpuSpeedNormalized(internalCpuSpeedProgress)
            }

            KnobParameter.DISPLAY_BRIGHTNESS -> {
                internalBrightnessProgress = (internalBrightnessProgress + delta).coerceIn(0.03f, 1f)
                setBrightnessDirect(internalBrightnessProgress)
            }

            KnobParameter.SYSTEM_AUDIO -> {
                internalAudioProgress = (internalAudioProgress + delta).coerceIn(0f, 1f)
                setSystemWideVolumeDirect(internalAudioProgress, triggerKnobHighlight = false)
            }

            KnobParameter.MUSIC_VOLUME -> {
                internalMusicVolProgress = (internalMusicVolProgress + delta).coerceIn(0f, 1f)
                setMusicVolumeDirect(internalMusicVolProgress)
            }

            KnobParameter.DJ_VINYL_SCRATCH -> {
                scratchCdByAngle(delta * 360f)
            }

            KnobParameter.DJ_BASS_FILTER -> {
                val nextFilter = (_telemetryState.value.djFilterCutoff + delta).coerceIn(0.05f, 1.0f)
                setDjFilterCutoff(nextFilter)
            }
        }
    }

    private fun markKnobActive(param: KnobParameter) {
        _activeManipulatedKnob.value = param
        knobResetJob?.cancel()
        knobResetJob = viewModelScope.launch {
            delay(1600)
            _activeManipulatedKnob.value = null
        }
    }

    fun setCpuSpeedNormalized(normalized: Float) {
        val clamped = normalized.coerceIn(0f, 1f)
        internalCpuSpeedProgress = clamped
        val previewGhz = ((0.95f + clamped * 1.45f) * 100f).roundToInt() / 100f
        val previewGov = when {
            clamped < 0.25f -> "POWERSAVE"
            clamped > 0.82f -> "PERFORMANCE"
            else -> "SCHEDUTIL"
        }
        _telemetryState.update {
            it.copy(
                cpuSpeedProgress = clamped,
                cpuBigFreqGhz = previewGhz,
                governorLabel = previewGov
            )
        }

        cpuScaleJob?.cancel()
        cpuScaleJob = viewModelScope.launch(Dispatchers.IO) {
            delay(80)
            val (appliedGhz, appliedGov) = SysFsBridge.applyCpuSpeedNormalized(clamped)
            _telemetryState.update {
                it.copy(
                    cpuBigFreqGhz = appliedGhz,
                    governorLabel = appliedGov.uppercase(),
                    isRootGranted = SysFsBridge.hasMagiskRoot
                )
            }
        }
    }

    fun setSystemWideVolumeDirect(normalized: Float, triggerKnobHighlight: Boolean = true) {
        val clamped = normalized.coerceIn(0f, 1f)
        internalAudioProgress = clamped
        internalMusicVolProgress = clamped
        prefs.edit().putFloat("saved_music_vol", clamped).apply()
        djSynthEngine.musicVolumeGain = clamped
        runCatching { localMediaPlayer?.setVolume(clamped, clamped) }

        val streams = intArrayOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_SYSTEM
        )
        var musicVol = 0
        var musicMax = 15
        for (stream in streams) {
            runCatching {
                val maxS = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
                val targetS = (clamped * maxS).roundToInt().coerceIn(0, maxS)
                audioManager.setStreamVolume(stream, targetS, 0)
                if (stream == AudioManager.STREAM_MUSIC) {
                    musicVol = targetS
                    musicMax = maxS
                }
            }
        }

        _telemetryState.update {
            it.copy(
                systemVolume = musicVol,
                maxSystemVolume = musicMax,
                audioProgress = clamped,
                musicVolumeProgress = clamped
            )
        }

        if (triggerKnobHighlight) {
            markKnobActive(KnobParameter.SYSTEM_AUDIO)
        }
    }

    fun setMusicVolumeDirect(normalized: Float) {
        val clamped = normalized.coerceIn(0f, 1f)
        internalMusicVolProgress = clamped
        prefs.edit().putFloat("saved_music_vol", clamped).apply()
        djSynthEngine.musicVolumeGain = clamped
        runCatching { localMediaPlayer?.setVolume(clamped, clamped) }

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val newVol = (clamped * maxVol).roundToInt().coerceIn(0, maxVol)
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        }
        _telemetryState.update {
            it.copy(
                systemVolume = newVol,
                maxSystemVolume = maxVol,
                musicVolumeProgress = clamped
            )
        }
    }

    fun stepVolumeSilent(stepDelta: Int) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val newVol = (_telemetryState.value.systemVolume + stepDelta).coerceIn(0, maxVol)
        setSystemWideVolumeDirect(newVol.toFloat() / maxVol.toFloat(), triggerKnobHighlight = true)
    }

    /**
     * Real Hardware Brightness Control:
     * 1. Updates `telemetryState.brightnessLevel` -> observed immediately in `MainActivity` to drive
     *    hardware `window.attributes.screenBrightness` with zero permission lag!
     * 2. Persists `saved_brightness` in `SharedPreferences`.
     * 3. Writes system-wide `SCREEN_BRIGHTNESS_MODE_MANUAL` + `SCREEN_BRIGHTNESS` if `WRITE_SETTINGS`
     *    is granted, or scales sysfs backlight via Magisk root.
     */
    fun setBrightnessDirect(normalized: Float) {
        val clamped = normalized.coerceIn(0.03f, 1f)
        internalBrightnessProgress = clamped
        prefs.edit().putFloat("saved_brightness", clamped).apply()
        _telemetryState.update { it.copy(brightnessLevel = clamped) }
        val appContext = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            SysFsBridge.writeDisplayBrightness(appContext, clamped)
        }
    }

    fun setThermalThresholdDirect(celsius: Int) {
        val clamped = celsius.coerceIn(45, 85)
        prefs.edit().putInt("thermal_threshold_c", clamped).apply()
        _telemetryState.update { it.copy(thermalThreshold = clamped) }
        thermalWriteJob?.cancel()
        thermalWriteJob = viewModelScope.launch(Dispatchers.IO) {
            delay(100)
            SysFsBridge.writeThermalLimit(clamped)
            _telemetryState.update { it.copy(isRootGranted = SysFsBridge.hasMagiskRoot) }
        }
    }

    private fun readRealWifiState(): Boolean {
        return runCatching { wifiManager?.isWifiEnabled ?: true }.getOrDefault(true)
    }

    @SuppressLint("MissingPermission")
    private fun readRealBluetoothState(context: Context): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPerm) return@runCatching bluetoothManager?.adapter?.state == BluetoothAdapter.STATE_ON
            }
            bluetoothManager?.adapter?.isEnabled == true
        }.getOrDefault(false)
    }

    /**
     * Genuine Wi-Fi Toggle:
     * - If Magisk root is available, toggles Wi-Fi directly via `cmd wifi` / `svc wifi`.
     * - On unrooted Android 10+ devices (where third-party `setWifiEnabled` is blocked by OS),
     *   opens Android's compact inline `Settings.Panel.ACTION_WIFI` bottom-sheet right over MultiOS!
     */
    @Suppress("DEPRECATION")
    fun toggleWifi(context: Context) {
        val target = !readRealWifiState()
        if (SysFsBridge.hasMagiskRoot) {
            _telemetryState.update { it.copy(wifiEnabled = target) }
            viewModelScope.launch(Dispatchers.IO) {
                SysFsBridge.setWifiEnabledRoot(target)
                delay(500)
                _telemetryState.update { it.copy(wifiEnabled = readRealWifiState()) }
            }
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val changed = runCatching { wifiManager?.setWifiEnabled(target) == true }.getOrDefault(false)
            if (changed) {
                _telemetryState.update { it.copy(wifiEnabled = target) }
                return
            }
        }

        // Launch Android's native inline Wi-Fi Panel slice right over the launcher
        runCatching {
            val panelIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_WIFI)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(panelIntent)
        }.onFailure {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    /**
     * Genuine Bluetooth Toggle:
     * - Uses Magisk root (`cmd bluetooth_manager`) when rooted.
     * - Uses `BluetoothAdapter` or system `ACTION_REQUEST_ENABLE` / Bluetooth Settings sheet on unrooted Android 13+.
     */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun toggleBluetooth(context: Context) {
        val currentlyOn = readRealBluetoothState(context)
        val target = !currentlyOn

        if (SysFsBridge.hasMagiskRoot) {
            _telemetryState.update { it.copy(btEnabled = target) }
            viewModelScope.launch(Dispatchers.IO) {
                SysFsBridge.setBluetoothEnabledRoot(target)
                delay(600)
                _telemetryState.update { it.copy(btEnabled = readRealBluetoothState(context)) }
            }
            return
        }

        val hasBtPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

        if (hasBtPerm) {
            val adapter = bluetoothManager?.adapter
            if (adapter != null) {
                val directOk = runCatching {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        if (target) adapter.enable() else adapter.disable()
                    } else false
                }.getOrDefault(false)

                if (directOk) {
                    _telemetryState.update { it.copy(btEnabled = target) }
                    return
                }

                if (target) {
                    val reqOk = runCatching {
                        val reqIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(reqIntent)
                        true
                    }.getOrDefault(false)
                    if (reqOk) return
                }
            }
        }

        runCatching {
            val btSettings = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(btSettings)
        }
    }

    fun toggleWirelessAdb() {
        val next = !_telemetryState.value.adbWifiEnabled
        _telemetryState.update { it.copy(adbWifiEnabled = next) }
        viewModelScope.launch(Dispatchers.IO) { SysFsBridge.setWirelessAdbEnabled(next) }
    }

    fun startTelemetrySampling() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch(Dispatchers.IO) {
            var tickCount = 0
            val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
            val app = getApplication<Application>()

            while (isActive) {
                val realTemp = SysFsBridge.readCpuTemperature()
                val bigFreq = SysFsBridge.readBigClusterFreqGhz()
                val littleFreq = SysFsBridge.readLittleClusterFreqGhz()
                val load = SysFsBridge.readCpuLoadRatio()
                val batteryInfo = readRealBatteryTelemetry()
                val uptimeStr = formatElapsedUptime(SystemClock.elapsedRealtime())
                val clockStr = timeFormat.format(Date())
                val ipAddr = queryLocalIpAddress()
                val liveWifi = readRealWifiState()
                val liveBt = readRealBluetoothState(app)
                val canWriteSys = Settings.System.canWrite(app)

                val shiftX = if (tickCount % 60 == 0) (-1..1).random() else _telemetryState.value.burnInOffsetX
                val shiftY = if (tickCount % 60 == 0) (-1..1).random() else _telemetryState.value.burnInOffsetY
                tickCount++

                val mpProgress = localMediaPlayer?.let { mp ->
                    runCatching {
                        if (mp.isPlaying && mp.duration > 0) mp.currentPosition.toFloat() / mp.duration.toFloat() else null
                    }.getOrNull()
                }

                _telemetryState.update { current ->
                    current.copy(
                        temperatureCpu = if (realTemp > 0) realTemp else current.temperatureCpu,
                        cpuBigFreqGhz = if (bigFreq > 0f) bigFreq else current.cpuBigFreqGhz,
                        cpuLittleFreqGhz = if (littleFreq > 0f) littleFreq else current.cpuLittleFreqGhz,
                        cpuLoad = if (load >= 0f) load else 0.20f,
                        isRootGranted = SysFsBridge.hasMagiskRoot,
                        isAccessibilityRecentsReady = MultiOsAccessibilityService.isConnected || SysFsBridge.hasMagiskRoot,
                        canWriteSystemSettings = canWriteSys,
                        wifiEnabled = liveWifi,
                        btEnabled = liveBt,
                        batteryPercent = batteryInfo.first,
                        batteryWattage = batteryInfo.second,
                        uptimeFormatted = uptimeStr,
                        currentTimeFormatted = clockStr,
                        localIpAddress = ipAddr,
                        burnInOffsetX = shiftX,
                        burnInOffsetY = shiftY,
                        cdTrackProgress = mpProgress ?: current.cdTrackProgress
                    )
                }
                delay(900)
            }
        }
    }

    fun stopTelemetrySampling() {
        telemetryJob?.cancel()
    }

    // =========================================================================
    // INSTALLED APPS & SYSTEM SETTINGS
    // =========================================================================

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(mainIntent, 0)
                }
            }.getOrDefault(emptyList())

            val myPkg = getApplication<Application>().packageName
            val discovered = resolveInfos
                .mapNotNull { ri ->
                    val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg == myPkg) return@mapNotNull null
                    val label = ri.loadLabel(pm)?.toString() ?: pkg
                    val iconBitmap = runCatching {
                        drawableToImageBitmap(ri.loadIcon(pm))
                    }.getOrNull()
                    AppEntry(name = label, packageName = pkg, category = "App", icon = iconBitmap)
                }
                .distinctBy { it.packageName }
                .sortedBy { it.name.lowercase() }

            if (discovered.isNotEmpty()) {
                _installedApps.value = discovered
            } else {
                _installedApps.value = listOf(
                    AppEntry("Settings", "com.android.settings", "System"),
                    AppEntry("Files", "com.android.documentsui", "Utilities"),
                    AppEntry("Browser", "com.android.chrome", "Web"),
                    AppEntry("Camera", "com.android.camera2", "Media"),
                    AppEntry("Clock", "com.google.android.deskclock", "Utilities"),
                    AppEntry("Calculator", "com.android.calculator2", "Utilities")
                )
            }
            refreshRecentAppsFromInstalled()
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable, sizePx: Int = 128): ImageBitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bmp.asImageBitmap()
    }

    fun launchApp(context: Context, app: AppEntry) {
        recordAppLaunchInRecents(app.packageName)
        closeRecentsDeck()
        viewModelScope.launch(Dispatchers.IO) {
            SysFsBridge.dismissKeyguardRoot()
        }
        runCatching {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                val animStyle = _telemetryState.value.appOpeningAnimStyle
                val opts = when (animStyle) {
                    AppOpeningAnimStyle.IOS_PERSPECTIVE_BALLOON -> {
                        ActivityOptions.makeCustomAnimation(
                            context,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        ).toBundle()
                    }
                    AppOpeningAnimStyle.HYPEROS_BALLOON -> {
                        ActivityOptions.makeCustomAnimation(
                            context,
                            android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right
                        ).toBundle()
                    }
                    AppOpeningAnimStyle.ONEUI_PORTAL -> {
                        ActivityOptions.makeCustomAnimation(
                            context,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        ).toBundle()
                    }
                }
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent, opts)
            }
        }
    }

    fun openNativeAndroidSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun readRealBatteryTelemetry(): Pair<Int, Float> {
        val app = getApplication<Application>()
        val bm = app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        val rawCurrent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

        val batteryStatus = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3950) ?: 3950
        val voltageV = (voltageMv / 1000f).coerceIn(3.2f, 4.45f)

        val currentAmps = if (abs(rawCurrent) > 10_000) {
            abs(rawCurrent) / 1_000_000f
        } else {
            abs(rawCurrent) / 1_000f
        }

        val watts = (currentAmps * voltageV).coerceIn(0.4f, 18.0f)
        return Pair(level, (watts * 10f).roundToInt() / 10f)
    }

    private fun formatElapsedUptime(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun queryLocalIpAddress(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    val host = addr.hostAddress ?: continue
                    if (!addr.isLoopbackAddress && !host.contains(":")) {
                        return host
                    }
                }
            }
            "Connected"
        }.getOrDefault("Connected")
    }
}
