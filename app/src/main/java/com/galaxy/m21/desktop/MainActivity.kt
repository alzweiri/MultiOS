package com.galaxy.m21.desktop

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.galaxy.m21.desktop.hardware.HardwareViewModel
import com.galaxy.m21.desktop.hardware.ScreenSizeMode
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.gate.HelloGateScreen
import com.galaxy.m21.desktop.ui.mobile.MobileHomeScreen
import com.galaxy.m21.desktop.ui.navigation.MultiDirectionalSurface
import com.galaxy.m21.desktop.ui.navigation.SurfaceDestination
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.shaders.LocalLiquidGlassConfig
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import com.galaxy.m21.desktop.ui.theme.MultiOsTheme
import com.galaxy.m21.desktop.ui.theme.OrientationMode
import kotlin.math.abs
import kotlin.math.roundToInt

val LocalAppWidgetHost = compositionLocalOf<AppWidgetHost?> { null }
val LocalAppWidgetManager = compositionLocalOf<AppWidgetManager?> { null }
val LocalLaunchWidgetPicker = compositionLocalOf<(() -> Unit)?> { null }

/**
 * MultiOS Home Launcher Root Activity:
 *
 * - Dynamically synchronizes OrientationMode:
 *     DESK_ONLY: Strictly sensor landscape (either side of landscape).
 *     MOBILE_ONLY: User portrait for daily vertical mobile operation.
 *     MIX_AUTO: Full sensor orientation with Apple-style mode transition banners.
 * - Suppresses Android system bars immersively.
 * - Hosts HelloGateScreen with Root Keyguard bypass upon swipe.
 * - Supports custom Bezel corner radius masking.
 */
class MainActivity : ComponentActivity() {

    companion object {
        const val APPWIDGET_HOST_ID = 2048
        const val REQUEST_PICK_APPWIDGET = 5001
        const val REQUEST_CREATE_APPWIDGET = 5002
    }

    private val hardwareViewModel: HardwareViewModel by viewModels()
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> hardwareViewModel.updateChargingState(true)
                Intent.ACTION_POWER_DISCONNECTED -> hardwareViewModel.updateChargingState(false)
                Intent.ACTION_SCREEN_OFF -> {
                    if (hardwareViewModel.telemetryState.value.aodEnabled) {
                        hardwareViewModel.setScreenAodActive(true)
                        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                        @Suppress("DEPRECATION")
                        val wakeLock = pm?.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            "MultiOS:AodWakeLock"
                        )
                        wakeLock?.acquire(1500L)
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val overrideConfig = Configuration(newBase.resources.configuration)
        overrideConfig.fontScale = 1.0f
        val context = newBase.createConfigurationContext(overrideConfig)
        super.attachBaseContext(context)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set initial orientation based on saved telemetry preference
        val initialMode = hardwareViewModel.telemetryState.value.orientationMode
        requestedOrientation = when (initialMode) {
            OrientationMode.DESK_ONLY -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.MOBILE_ONLY -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            OrientationMode.MIX_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, APPWIDGET_HOST_ID)
        appWidgetHost.startListening()

        val pFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(powerReceiver, pFilter)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            // Immediately re-hide the real OS status bar if any edge gesture tries to reveal it
            window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                if (insets.isVisible(WindowInsets.Type.statusBars()) ||
                    insets.isVisible(WindowInsets.Type.navigationBars())
                ) {
                    v.postDelayed({ hideSystemBarsImmersive() }, 180L)
                }
                v.onApplyWindowInsets(insets)
            }
        } else {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = hardwareViewModel.telemetryState.value
                when {
                    state.showSettingsModal -> hardwareViewModel.closeSettingsModal()
                    state.showRecentsModal -> hardwareViewModel.closeRecentsDeck()
                    state.freeformDragEnabled -> hardwareViewModel.toggleFreeformDragMode()
                    else -> hardwareViewModel.requestNavigate(SurfaceDestination.DASHBOARD)
                }
            }
        })

        setContent {
            val telemetry by hardwareViewModel.telemetryState.collectAsState()
            val overlayBlur by hardwareViewModel.overlayBlurProgress.collectAsState()
            val glassConfig = telemetry.resolveLiquidGlassConfig()
            val accentColor = telemetry.resolveAccentColor()
            val dynamicDensity = Density(
                density = telemetry.customDpi / 160f,
                fontScale = 1.0f
            )

            // Dynamic orientation synchronization with banner notifications
            LaunchedEffect(telemetry.orientationMode) {
                val target = when (telemetry.orientationMode) {
                    OrientationMode.DESK_ONLY -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    OrientationMode.MOBILE_ONLY -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    OrientationMode.MIX_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
                if (requestedOrientation != target) {
                    requestedOrientation = target
                    val label = when (telemetry.orientationMode) {
                        OrientationMode.DESK_ONLY -> "Switching to Desk Mode"
                        OrientationMode.MOBILE_ONLY -> "Switching to Mobile Mode"
                        OrientationMode.MIX_AUTO -> "Auto Sensor Orientation"
                    }
                    hardwareViewModel.triggerOrientationSwitchBanner(label)
                }
            }

            // Instant hardware display brightness synchronization on the window!
            LaunchedEffect(telemetry.brightnessLevel) {
                val lp = window.attributes
                val target = telemetry.brightnessLevel.coerceIn(0.02f, 1.0f)
                if (abs(lp.screenBrightness - target) > 0.004f) {
                    lp.screenBrightness = target
                    window.attributes = lp
                }
            }

            // Live blur of the phone's real OS Home Screen wallpaper behind overlays on Android 12+
            LaunchedEffect(overlayBlur, telemetry.syncSystemWallpaper, telemetry.hasCustomWallpaper) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val radius = if (telemetry.syncSystemWallpaper && !telemetry.hasCustomWallpaper) {
                        (overlayBlur.coerceIn(0f, 1f) * 46f).roundToInt()
                    } else {
                        0
                    }
                    runCatching { window.setBackgroundBlurRadius(radius) }
                }
            }

            CompositionLocalProvider(
                LocalDensity provides dynamicDensity,
                LocalLiquidGlassConfig provides glassConfig,
                LocalAppWidgetHost provides appWidgetHost,
                LocalAppWidgetManager provides appWidgetManager,
                LocalLaunchWidgetPicker provides { launchAppWidgetPicker() }
            ) {
                MultiOsTheme(
                    appearanceMode = telemetry.appearanceMode,
                    accentColor = accentColor,
                    palette = telemetry.colorPalette
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MultiOsViewport(hardwareViewModel = hardwareViewModel)

                        // Welcome Gate Screen (with root Keyguard bypass and left-arrow swipe)
                        if (telemetry.showWelcomeScreen && !telemetry.isWelcomeGateDismissed) {
                            HelloGateScreen(hardwareViewModel = hardwareViewModel)
                        }

                        // Orientation Mode Switch Banner (Apple-style Liquid Glass pill)
                        AnimatedVisibility(
                            visible = telemetry.isOrientationSwitching,
                            enter = fadeIn() + slideInVertically { -it },
                            exit = fadeOut() + slideOutVertically { -it },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp)
                        ) {
                            LiquidGlassBox(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50)),
                                shape = RoundedCornerShape(50),
                                borderColor = accentColor.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MultiOsIcons.RootBolt(Modifier.size(16.dp), accentColor)
                                    Text(
                                        text = telemetry.orientationSwitchLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        hideSystemBarsImmersive()
    }

    fun launchAppWidgetPicker() {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_APPWIDGET -> {
                    val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                    if (appWidgetId != -1) {
                        configureAppWidget(appWidgetId)
                    }
                }
                REQUEST_CREATE_APPWIDGET -> {
                    val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                    if (appWidgetId != -1) {
                        hardwareViewModel.addBoundAppWidget(appWidgetId)
                    }
                }
            }
        } else if (resultCode == RESULT_CANCELED && requestCode == REQUEST_PICK_APPWIDGET) {
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    private fun configureAppWidget(appWidgetId: Int) {
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (appWidgetInfo?.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = appWidgetInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET)
        } else {
            hardwareViewModel.addBoundAppWidget(appWidgetId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(powerReceiver) }
        runCatching { appWidgetHost.stopListening() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBarsImmersive()
        }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBarsImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.post {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                hardwareViewModel.stepVolumeSilent(1)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                hardwareViewModel.stepVolumeSilent(-1)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()
        val currentMode = hardwareViewModel.telemetryState.value.orientationMode
        requestedOrientation = when (currentMode) {
            OrientationMode.DESK_ONLY -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.MOBILE_ONLY -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            OrientationMode.MIX_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
        hideSystemBarsImmersive()
        hardwareViewModel.startTelemetrySampling()
        hardwareViewModel.loadInstalledApps()
    }

    override fun onPause() {
        super.onPause()
        hardwareViewModel.stopTelemetrySampling()
    }
}

@Composable
fun MultiOsViewport(hardwareViewModel: HardwareViewModel) {
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT ||
        telemetry.orientationMode == OrientationMode.MOBILE_ONLY

    val cornerRadius = telemetry.bezelCornerRadiusDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        if (isPortrait) {
            // Mobile Portrait Daily Launcher
            MobileHomeScreen(
                hardwareViewModel = hardwareViewModel,
                onOpenAppDrawer = { hardwareViewModel.requestNavigate(SurfaceDestination.APP_DRAWER) },
                onOpenQuickSettings = { hardwareViewModel.requestNavigate(SurfaceDestination.QUICK_SETTINGS) },
                onOpenRecents = { hardwareViewModel.openRecentsDeck() },
                onOpenSettings = { hardwareViewModel.openSettingsModal() }
            )
        } else {
            // Desk Landscape Multi-Surface Launcher
            when (telemetry.screenSizeMode) {
                ScreenSizeMode.UNIVERSAL_FULLSCREEN -> {
                    MultiDirectionalSurface(hardwareViewModel = hardwareViewModel)
                }
                ScreenSizeMode.FIXED_DOCK_2140x1080 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        val targetW = with(density) { telemetry.customCanvasWidthPx.toDp() }
                        val targetH = with(density) { telemetry.customCanvasHeightPx.toDp() }
                        val hMask = with(density) { telemetry.bezelMaskHorizontalPx.toDp() }
                        val vMask = with(density) { telemetry.bezelMaskVerticalPx.toDp() }

                        Box(
                            modifier = Modifier
                                .requiredSize(targetW, targetH)
                                .padding(horizontal = hMask, vertical = vMask)
                        ) {
                            MultiDirectionalSurface(hardwareViewModel = hardwareViewModel)
                        }
                    }
                }
            }
        }
    }
}
