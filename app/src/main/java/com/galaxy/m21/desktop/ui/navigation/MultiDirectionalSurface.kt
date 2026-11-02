package com.galaxy.m21.desktop.ui.navigation

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.galaxy.m21.desktop.hardware.HardwareViewModel
import com.galaxy.m21.desktop.hardware.SystemTelemetryData
import com.galaxy.m21.desktop.ui.aod.AlwaysOnDisplayOverlay
import com.galaxy.m21.desktop.ui.aod.ChargingAnimationOverlay
import com.galaxy.m21.desktop.ui.components.CustomStatusBar
import com.galaxy.m21.desktop.ui.components.DynamicWallpaperCanvas
import com.galaxy.m21.desktop.ui.dashboard.DashboardScreen
import com.galaxy.m21.desktop.ui.drawer.AppDrawerScreen
import com.galaxy.m21.desktop.ui.gemini.GeminiAiScreen
import com.galaxy.m21.desktop.ui.music.CdTurntableScreen
import com.galaxy.m21.desktop.ui.recents.RecentsDeckOverlay
import com.galaxy.m21.desktop.ui.settings.SystemSettingsPanel
import com.galaxy.m21.desktop.ui.shade.QuickSettingsShade
import com.galaxy.m21.desktop.ui.theme.GeminiEdgeStyle
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

enum class SurfaceDestination {
    DASHBOARD,
    APP_DRAWER,
    GEMINI_AI,
    MUSIC_STUDIO,
    QUICK_SETTINGS
}

/**
 * MultiOS Fluid 4-Way Gesture, Draggable Side Pull Tab, Live Blurred Home Backdrop & Recents Engine.
 *
 * - Keeps `DashboardScreen` (WITH ALL KNOBS, CLOCK, & WIDGETS) mounted and live-blurred behind
 *   App Drawer, Control Center, Vinyl Studio, Recents Deck, and Settings!
 * - In-App Control Center (`QUICK_SETTINGS`) pulls down strictly from the freely draggable Side Pull Tab
 *   (`DraggableSidePullTab`) so it NEVER conflicts with the phone's upper status bar!
 * - **Whole-Screen Swipe-Away When Open**:
 *   - On `DASHBOARD` (Home Screen): vertical swipes NEVER open Control Center from random screen areas.
 *   - When `QUICK_SETTINGS` IS OPEN: swiping UP anywhere on the whole screen swipes the entire Control Center away!
 *   - When `MUSIC_STUDIO` IS OPEN: swiping DOWN anywhere on the whole screen swipes the entire Music Studio away!
 */
@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun MultiDirectionalSurface(hardwareViewModel: HardwareViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val customWallpaper by hardwareViewModel.customWallpaperBitmap.collectAsState()
    val currentDest by hardwareViewModel.currentDestination.collectAsState()
    val density = LocalDensity.current
    val themeColors = LocalMultiOsColors.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val canvasWidth = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val canvasHeight = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val tabZonePx = with(density) { 56.dp.toPx() }

        fun offsetFor(dest: SurfaceDestination): Offset = when (dest) {
            SurfaceDestination.DASHBOARD -> Offset.Zero
            SurfaceDestination.APP_DRAWER -> Offset(-canvasWidth, 0f)
            SurfaceDestination.GEMINI_AI -> Offset(canvasWidth, 0f)
            SurfaceDestination.MUSIC_STUDIO -> Offset(0f, -canvasHeight)
            SurfaceDestination.QUICK_SETTINGS -> Offset(0f, canvasHeight)
        }

        val surfaceOffset = remember {
            Animatable(
                initialValue = Offset.Zero,
                typeConverter = Offset.VectorConverter
            )
        }

        var rawVelocityX by remember { mutableFloatStateOf(0f) }
        var rawVelocityY by remember { mutableFloatStateOf(0f) }
        var gestureTouchY by remember { mutableFloatStateOf(canvasHeight * 0.5f) }

        val bendX by animateFloatAsState(
            targetValue = (rawVelocityX / 15000f).coerceIn(-0.04f, 0.04f),
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 280f),
            label = "iOSBendX"
        )
        val bendY by animateFloatAsState(
            targetValue = (rawVelocityY / 15000f).coerceIn(-0.045f, 0.045f),
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 280f),
            label = "iOSBendY"
        )

        LaunchedEffect(currentDest, canvasWidth, canvasHeight) {
            rawVelocityX = 0f
            rawVelocityY = 0f
            surfaceOffset.animateTo(
                targetValue = offsetFor(currentDest),
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 320f
                )
            )
        }

        val velocityTracker = remember { VelocityTracker() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    currentDest,
                    canvasWidth,
                    canvasHeight,
                    telemetry.showSettingsModal,
                    telemetry.showRecentsModal,
                    telemetry.freeformDragEnabled,
                    telemetry.verticalSwipeFromTabsOnly
                ) {
                    if (telemetry.showSettingsModal || telemetry.showRecentsModal || telemetry.freeformDragEnabled) {
                        return@pointerInput
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        gestureTouchY = down.position.y.coerceIn(canvasHeight * 0.14f, canvasHeight * 0.86f)

                        val startedInTopTab = down.position.y <= tabZonePx
                        val startedInBottomTab = down.position.y >= (canvasHeight - tabZonePx)

                        var isDraggingSurface = false
                        var dragDirection: DragAxis? = null
                        var accumulatedPan = Offset.Zero
                        val startOffset = surfaceOffset.value

                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val change: PointerInputChange? = event.changes.firstOrNull { it.id == down.id }

                            if (change != null && change.pressed) {
                                if (change.isConsumed && !isDraggingSurface) {
                                    break
                                }

                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val panChange = change.position - change.previousPosition
                                accumulatedPan += panChange
                                gestureTouchY = change.position.y.coerceIn(canvasHeight * 0.14f, canvasHeight * 0.86f)

                                if (!isDraggingSurface) {
                                    val dist = hypot(accumulatedPan.x, accumulatedPan.y)
                                    if (dist > 24f) {
                                        val absX = abs(accumulatedPan.x)
                                        val absY = abs(accumulatedPan.y)

                                        // Strict 1.38x cone axis locking so diagonal swipes never misfire!
                                        if (absX > absY * 1.38f && startOffset.y == 0f) {
                                            isDraggingSurface = true
                                            dragDirection = DragAxis.HORIZONTAL
                                        } else if (absY > absX * 1.38f && startOffset.x == 0f) {
                                            val allowVertical = when (currentDest) {
                                                SurfaceDestination.DASHBOARD -> {
                                                    if (accumulatedPan.y > 0f) {
                                                        // Pulling down opens Control Center
                                                        if (telemetry.verticalSwipeFromTabsOnly) startedInTopTab else true
                                                    } else {
                                                        // Pulling up opens Vinyl/Music Studio
                                                        if (telemetry.verticalSwipeFromTabsOnly) startedInBottomTab else true
                                                    }
                                                }

                                                SurfaceDestination.QUICK_SETTINGS -> {
                                                    // Whole-screen swipe-away ONLY while Control Center is open!
                                                    accumulatedPan.y < 0f
                                                }

                                                SurfaceDestination.MUSIC_STUDIO -> {
                                                    // Whole-screen swipe-away ONLY while Vinyl/Music Studio is open!
                                                    accumulatedPan.y > 0f
                                                }

                                                else -> false
                                            }

                                            if (allowVertical) {
                                                isDraggingSurface = true
                                                dragDirection = DragAxis.VERTICAL
                                            }
                                        }
                                    }
                                }

                                if (isDraggingSurface && dragDirection != null) {
                                    change.consume()
                                    val liveVel = velocityTracker.calculateVelocity()
                                    rawVelocityX = liveVel.x
                                    rawVelocityY = liveVel.y

                                    coroutineScope.launch {
                                        val currentX = surfaceOffset.value.x
                                        val currentY = surfaceOffset.value.y

                                        val newOffset = if (dragDirection == DragAxis.HORIZONTAL) {
                                            Offset(
                                                x = applyRubberBandClamp(currentX + panChange.x, -canvasWidth, canvasWidth, canvasWidth * 0.22f),
                                                y = 0f
                                            )
                                        } else {
                                            val minVertical = if (currentDest == SurfaceDestination.QUICK_SETTINGS) 0f else -canvasHeight
                                            val maxVertical = if (currentDest == SurfaceDestination.MUSIC_STUDIO) 0f else canvasHeight
                                            Offset(
                                                x = 0f,
                                                y = applyRubberBandClamp(currentY + panChange.y, minVertical, maxVertical, canvasHeight * 0.18f)
                                            )
                                        }
                                        surfaceOffset.snapTo(newOffset)
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        rawVelocityX = 0f
                        rawVelocityY = 0f

                        if (isDraggingSurface && dragDirection != null) {
                            val velocity = velocityTracker.calculateVelocity()
                            val lockedAxis = dragDirection!!
                            coroutineScope.launch {
                                val nextDest = resolveDestinationFromGesture(
                                    startDest = currentDest,
                                    lockedAxis = lockedAxis,
                                    startOffset = offsetFor(currentDest),
                                    currentOffset = surfaceOffset.value,
                                    velocityX = velocity.x,
                                    velocityY = velocity.y,
                                    canvasWidth = canvasWidth,
                                    canvasHeight = canvasHeight,
                                    telemetry = telemetry
                                )
                                hardwareViewModel.requestNavigate(nextDest)
                                surfaceOffset.animateTo(
                                    targetValue = offsetFor(nextDest),
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = 330f
                                    )
                                )
                            }
                        }
                    }
                }
        ) {
            val curX = surfaceOffset.value.x
            val curY = surfaceOffset.value.y

            val shadeReveal = (curY / canvasHeight).coerceIn(0f, 1f)
            val musicReveal = (-curY / canvasHeight).coerceIn(0f, 1f)
            val drawerReveal = (-curX / canvasWidth).coerceIn(0f, 1f)
            val geminiProgress = (curX / canvasWidth).coerceIn(0f, 1f)

            val settingsAnim by animateFloatAsState(
                targetValue = if (telemetry.showSettingsModal) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 340f),
                label = "SettingsBlurProgress"
            )
            val recentsAnim by animateFloatAsState(
                targetValue = if (telemetry.showRecentsModal) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 340f),
                label = "RecentsBlurProgress"
            )

            val panelOverlayProgress = max(
                max(max(shadeReveal, musicReveal), recentsAnim),
                max(drawerReveal, geminiProgress)
            )
            val totalBlurProgress = max(panelOverlayProgress, settingsAnim)
            hardwareViewModel.updateOverlayBlurProgress(totalBlurProgress)

            val homeBlurRadiusPx = with(density) { (panelOverlayProgress * 26f).dp.toPx() }
            val settingsBlurRadiusPx = with(density) { (settingsAnim * 28f).dp.toPx() }

            // =========================================================================
            // ROCK-STEADY STATIONARY WALLPAPER BACKGROUND (ZERO SHAKE / WOBBLE!)
            // =========================================================================
            DynamicWallpaperCanvas(
                style = telemetry.wallpaperStyle,
                palette = telemetry.colorPalette,
                customWallpaper = customWallpaper,
                syncSystemWallpaper = telemetry.syncSystemWallpaper,
                blurRadiusDp = telemetry.homeWallpaperBlurDp.dp,
                modifier = Modifier.fillMaxSize()
            )

            // =========================================================================
            // OUTER CONTAINER BLURRED WHEN GEAR ICON SETTINGS SHEET IS OPEN!
            // Blurs BOTH the Home Screen AND Control Center cleanly behind Settings!
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (settingsBlurRadiusPx > 1.0f) {
                            renderEffect = RenderEffect.createBlurEffect(
                                settingsBlurRadiusPx,
                                settingsBlurRadiusPx,
                                Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    }
            ) {
                // =====================================================================
                // BASE LAYER: HOME SCREEN (WITH KNOBS!)
                // NEVER unmounted! Knobs, Clock, Thermal Box & Sliders remain visibly
                // blurred behind App Drawer, Control Center, Recents, and Vinyl Studio!
                // =====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val depthScale = 1f - (0.045f * panelOverlayProgress)
                            scaleX = depthScale * (1f + abs(bendX) * 0.45f - abs(bendY) * 0.2f)
                            scaleY = depthScale * (1f + abs(bendY) * 0.45f - abs(bendX) * 0.2f)
                            rotationY = -bendX * 32f
                            rotationX = bendY * 32f
                            cameraDistance = 16f * density.density

                            if (homeBlurRadiusPx > 1.2f) {
                                renderEffect = RenderEffect.createBlurEffect(
                                    homeBlurRadiusPx,
                                    homeBlurRadiusPx,
                                    Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CustomStatusBar(
                            telemetry = telemetry,
                            onPullDownControlCenter = {
                                hardwareViewModel.requestNavigate(SurfaceDestination.QUICK_SETTINGS)
                            },
                            onFinishEditMode = {
                                hardwareViewModel.toggleFreeformDragMode()
                            }
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            DashboardScreen(
                                hardwareViewModel = hardwareViewModel,
                                onNavigate = { dest -> hardwareViewModel.requestNavigate(dest) }
                            )
                        }
                    }

                    // Block accidental knob touches when an overlay is covering the blurred Home Screen
                    if (panelOverlayProgress > 0.06f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                    }
                                }
                        )
                    }
                }

                // =====================================================================
                // SWIPE LEFT: APP DRAWER + REPOSITIONABLE/RESIZABLE KEYBOARD
                // =====================================================================
                if (drawerReveal > 0.005f) {
                    val xOffset = ((1f - drawerReveal) * canvasWidth).roundToInt()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(xOffset, 0) }
                            .graphicsLayer {
                                alpha = (drawerReveal * 1.2f).coerceIn(0f, 1f)
                                val elastic = 0.95f + 0.05f * drawerReveal
                                scaleX = elastic + abs(bendX) * 0.5f
                                scaleY = elastic - abs(bendX) * 0.2f
                                transformOrigin = TransformOrigin(1f, 0.5f)
                            }
                    ) {
                        AppDrawerScreen(
                            hardwareViewModel = hardwareViewModel,
                            onClose = { hardwareViewModel.requestNavigate(SurfaceDestination.DASHBOARD) }
                        )
                    }
                }

                // =====================================================================
                // SWIPE RIGHT: GEMINI IN-APP UI WITH SPATIAL FADE / BEZEL-PEEL
                // Guarded so returning from App Drawer never triggers ghost Gemini animation!
                // =====================================================================
                val isGeminiActive = currentDest == SurfaceDestination.GEMINI_AI ||
                    (currentDest == SurfaceDestination.DASHBOARD && curX > 20f)
                if (isGeminiActive && geminiProgress > 0.015f) {
                    val edgeStyle = telemetry.geminiEdgeStyle
                    val maxRadius = hypot(canvasWidth, canvasHeight) * 1.05f

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (edgeStyle) {
                            GeminiEdgeStyle.CROSSFADE -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = geminiProgress
                                            val zoom = 0.95f + 0.05f * geminiProgress
                                            scaleX = zoom
                                            scaleY = zoom
                                        }
                                ) {
                                    GeminiAiScreen(
                                        hardwareViewModel = hardwareViewModel,
                                        isFullySettled = currentDest == SurfaceDestination.GEMINI_AI && geminiProgress > 0.97f,
                                        onClose = { hardwareViewModel.requestNavigate(SurfaceDestination.DASHBOARD) }
                                    )
                                }
                            }

                            GeminiEdgeStyle.PORTAL_EXPAND,
                            GeminiEdgeStyle.BEZEL_PEEL,
                            GeminiEdgeStyle.LIQUID_BUBBLE -> {
                                val revealStart = if (edgeStyle == GeminiEdgeStyle.PORTAL_EXPAND) 0.02f else 0.20f
                                val portalPhase = ((geminiProgress - revealStart) / (1f - revealStart)).coerceIn(0f, 1f)
                                val portalRadius = portalPhase * portalPhase * maxRadius
                                val anchorX = if (edgeStyle == GeminiEdgeStyle.PORTAL_EXPAND) 0f else (geminiProgress * canvasWidth * 0.24f).coerceAtMost(canvasWidth * 0.22f)

                                if (portalPhase > 0.005f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .drawWithContent {
                                                val clipOval = Path().apply {
                                                    addOval(
                                                        Rect(
                                                            center = Offset(anchorX, gestureTouchY),
                                                            radius = portalRadius
                                                        )
                                                    )
                                                }
                                                clipPath(clipOval) {
                                                    this@drawWithContent.drawContent()
                                                }
                                            }
                                    ) {
                                        GeminiAiScreen(
                                            hardwareViewModel = hardwareViewModel,
                                            isFullySettled = currentDest == SurfaceDestination.GEMINI_AI && geminiProgress > 0.97f,
                                            onClose = { hardwareViewModel.requestNavigate(SurfaceDestination.DASHBOARD) }
                                        )
                                    }
                                }

                                val membraneAlpha = (1f - ((geminiProgress - 0.52f) / 0.36f).coerceIn(0f, 1f))
                                if (edgeStyle != GeminiEdgeStyle.PORTAL_EXPAND && membraneAlpha > 0.01f) {
                                    BezelPeelAndBubbleCanvas(
                                        style = edgeStyle,
                                        progress = geminiProgress,
                                        touchY = gestureTouchY,
                                        accentColor = themeColors.accent,
                                        onAccentColor = themeColors.onAccent,
                                        alphaMultiplier = membraneAlpha,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // =====================================================================
                // SIDE PULL TAB REVEAL: WHOLE-SCREEN LIQUID GLASS CONTROL CENTER
                // (Swipes away from ANYWHERE on the whole screen when open!)
                // =====================================================================
                if (shadeReveal > 0.005f) {
                    val yOffset = ((-1f + shadeReveal) * canvasHeight).roundToInt()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(0, yOffset) }
                            .graphicsLayer {
                                alpha = (shadeReveal * 1.45f).coerceIn(0f, 1f)
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                scaleY = (0.94f + 0.06f * shadeReveal) + max(0f, bendY) * 0.6f
                                scaleX = (0.97f + 0.03f * shadeReveal) - abs(bendY) * 0.15f
                            }
                    ) {
                        QuickSettingsShade(
                            hardwareViewModel = hardwareViewModel,
                            onOpenSettings = { hardwareViewModel.openSettingsModal() },
                            onOpenRecents = { hardwareViewModel.openRecentsDeck() },
                            onTriggerEditLayout = { hardwareViewModel.toggleFreeformDragMode() },
                            onDismiss = { hardwareViewModel.requestNavigate(SurfaceDestination.DASHBOARD) }
                        )
                    }
                }

                // =====================================================================
                // SWIPE UP: MINIMALIST VINYL & CD STUDIO WITH INTERACTIVE TONEARM
                // (Swipes away from ANYWHERE on the whole screen when open!)
                // =====================================================================
                if (musicReveal > 0.005f) {
                    val yOffset = ((1f - musicReveal) * canvasHeight).roundToInt()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(0, yOffset) }
                            .graphicsLayer {
                                alpha = (musicReveal * 1.4f).coerceIn(0f, 1f)
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                scaleY = (0.94f + 0.06f * musicReveal) + abs(bendY) * 0.55f
                                scaleX = (0.97f + 0.03f * musicReveal) - abs(bendY) * 0.15f
                            }
                    ) {
                        CdTurntableScreen(
                            hardwareViewModel = hardwareViewModel,
                            onBackToCockpit = { hardwareViewModel.requestNavigate(SurfaceDestination.DASHBOARD) }
                        )
                    }
                }

                // =====================================================================
                // CREATIVE RECENTS DECK OVERLAY (Over Live Blurred Home Screen!)
                // =====================================================================
                AnimatedVisibility(
                    visible = telemetry.showRecentsModal,
                    enter = fadeIn(spring(stiffness = 380f)) + scaleIn(
                        initialScale = 0.93f,
                        animationSpec = spring(dampingRatio = 0.80f, stiffness = 350f)
                    ),
                    exit = fadeOut(spring(stiffness = 420f)) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = spring(stiffness = 420f)
                    )
                ) {
                    RecentsDeckOverlay(
                        hardwareViewModel = hardwareViewModel,
                        onDismiss = { hardwareViewModel.closeRecentsDeck() }
                    )
                }
            }

            // =========================================================================
            // GEAR ICON MODAL: MULTIOS SETTINGS & CUSTOMIZATION SHEET
            // =========================================================================
            AnimatedVisibility(
                visible = telemetry.showSettingsModal,
                enter = fadeIn(spring(stiffness = 380f)) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f)
                ),
                exit = fadeOut(spring(stiffness = 420f)) + scaleOut(
                    targetScale = 0.94f,
                    animationSpec = spring(stiffness = 420f)
                )
            ) {
                SystemSettingsPanel(
                    hardwareViewModel = hardwareViewModel,
                    onDismiss = { hardwareViewModel.closeSettingsModal() }
                )
            }
        }

        // Always-On Display & Rich Charging Overlays
        AlwaysOnDisplayOverlay(
            telemetry = telemetry,
            onDismiss = { hardwareViewModel.setScreenAodActive(false) }
        )
        ChargingAnimationOverlay(
            telemetry = telemetry
        )
    }
}

/**
 * HyperOS Side Bezel-Peel & Liquid Bubble Edge Geometry Renderer.
 */
@Composable
private fun BezelPeelAndBubbleCanvas(
    style: GeminiEdgeStyle,
    progress: Float,
    touchY: Float,
    accentColor: Color,
    onAccentColor: Color,
    alphaMultiplier: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cy = touchY.coerceIn(h * 0.12f, h * 0.88f)
        val pullPx = (progress * w * 0.42f).coerceIn(8f, w * 0.36f)

        if (style == GeminiEdgeStyle.BEZEL_PEEL) {
            val halfSpanY = (110.dp.toPx() + pullPx * 0.55f).coerceAtMost(h * 0.48f)
            val peelPath = Path().apply {
                moveTo(0f, (cy - halfSpanY).coerceAtLeast(0f))
                cubicTo(
                    0f, cy - halfSpanY * 0.48f,
                    pullPx, cy - halfSpanY * 0.36f,
                    pullPx, cy
                )
                cubicTo(
                    pullPx, cy + halfSpanY * 0.36f,
                    0f, cy + halfSpanY * 0.48f,
                    0f, (cy + halfSpanY).coerceAtMost(h)
                )
                close()
            }

            drawPath(
                path = peelPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.92f * alphaMultiplier),
                        accentColor.copy(alpha = 0.78f * alphaMultiplier)
                    ),
                    startX = 0f,
                    endX = pullPx
                )
            )
            drawPath(
                path = peelPath,
                color = Color.White.copy(alpha = 0.55f * alphaMultiplier),
                style = Stroke(width = 1.6.dp.toPx())
            )

            if (pullPx > 28.dp.toPx()) {
                val sparkCenter = Offset(pullPx * 0.54f, cy)
                val sr = (11.dp.toPx() * (pullPx / 90.dp.toPx()).coerceIn(0.6f, 1.25f))
                val spark = Path().apply {
                    moveTo(sparkCenter.x, sparkCenter.y - sr)
                    quadraticTo(sparkCenter.x + sr * 0.08f, sparkCenter.y - sr * 0.08f, sparkCenter.x + sr, sparkCenter.y)
                    quadraticTo(sparkCenter.x + sr * 0.08f, sparkCenter.y + sr * 0.08f, sparkCenter.x, sparkCenter.y + sr)
                    quadraticTo(sparkCenter.x - sr * 0.08f, sparkCenter.y + sr * 0.08f, sparkCenter.x - sr, sparkCenter.y)
                    quadraticTo(sparkCenter.x - sr * 0.08f, sparkCenter.y - sr * 0.08f, sparkCenter.x, sparkCenter.y - sr)
                    close()
                }
                drawPath(spark, color = onAccentColor.copy(alpha = 0.95f * alphaMultiplier))
            }
        } else {
            val bubbleR = (28.dp.toPx() + progress * 46.dp.toPx())
            val bubbleCenter = Offset((pullPx * 0.72f).coerceAtLeast(bubbleR * 0.5f), cy)
            val neckY = bubbleR * (1f - (progress * 2.2f).coerceIn(0f, 0.85f))

            if (neckY > 3f) {
                val neck = Path().apply {
                    moveTo(0f, cy - neckY * 1.35f)
                    quadraticTo(bubbleCenter.x * 0.45f, cy - neckY * 0.4f, bubbleCenter.x, cy - bubbleR * 0.7f)
                    lineTo(bubbleCenter.x, cy + bubbleR * 0.7f)
                    quadraticTo(bubbleCenter.x * 0.45f, cy + neckY * 0.4f, 0f, cy + neckY * 1.35f)
                    close()
                }
                drawPath(neck, color = accentColor.copy(alpha = 0.78f * alphaMultiplier))
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f * alphaMultiplier),
                        accentColor.copy(alpha = 0.92f * alphaMultiplier)
                    ),
                    center = Offset(bubbleCenter.x - bubbleR * 0.28f, cy - bubbleR * 0.28f),
                    radius = bubbleR * 1.25f
                ),
                radius = bubbleR,
                center = bubbleCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.65f * alphaMultiplier),
                radius = bubbleR,
                center = bubbleCenter,
                style = Stroke(width = 1.4.dp.toPx())
            )

            val sr = 11.dp.toPx()
            val spark = Path().apply {
                moveTo(bubbleCenter.x, cy - sr)
                quadraticTo(bubbleCenter.x + sr * 0.08f, cy - sr * 0.08f, bubbleCenter.x + sr, cy)
                quadraticTo(bubbleCenter.x + sr * 0.08f, cy + sr * 0.08f, bubbleCenter.x, cy + sr)
                quadraticTo(bubbleCenter.x - sr * 0.08f, cy + sr * 0.08f, bubbleCenter.x - sr, cy)
                quadraticTo(bubbleCenter.x - sr * 0.08f, cy - sr * 0.08f, bubbleCenter.x, cy - sr)
                close()
            }
            drawPath(spark, color = onAccentColor.copy(alpha = 0.95f * alphaMultiplier))
        }
    }
}

private enum class DragAxis { HORIZONTAL, VERTICAL }

private fun applyRubberBandClamp(value: Float, minVal: Float, maxVal: Float, maxStretch: Float): Float {
    return when {
        value < minVal -> {
            val over = minVal - value
            minVal - maxStretch * (1f - (1f / ((over * 0.55f / maxStretch) + 1f)))
        }
        value > maxVal -> {
            val over = value - maxVal
            maxVal + maxStretch * (1f - (1f / ((over * 0.55f / maxStretch) + 1f)))
        }
        else -> value
    }
}

private fun mapSwipeTargetToDest(target: com.galaxy.m21.desktop.ui.theme.SwipeActionTarget, defaultDest: SurfaceDestination): SurfaceDestination {
    return when (target) {
        com.galaxy.m21.desktop.ui.theme.SwipeActionTarget.GEMINI_WEB -> SurfaceDestination.GEMINI_AI
        com.galaxy.m21.desktop.ui.theme.SwipeActionTarget.APP_DRAWER -> SurfaceDestination.APP_DRAWER
        com.galaxy.m21.desktop.ui.theme.SwipeActionTarget.VINYL_STUDIO -> SurfaceDestination.MUSIC_STUDIO
        com.galaxy.m21.desktop.ui.theme.SwipeActionTarget.CONTROL_CENTER -> SurfaceDestination.QUICK_SETTINGS
        else -> defaultDest
    }
}

private fun resolveDestinationFromGesture(
    startDest: SurfaceDestination,
    lockedAxis: DragAxis,
    startOffset: Offset,
    currentOffset: Offset,
    velocityX: Float,
    velocityY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    telemetry: SystemTelemetryData
): SurfaceDestination {
    val deltaX = currentOffset.x - startOffset.x
    val deltaY = currentOffset.y - startOffset.y
    val hThreshold = canvasWidth * 0.19f
    val vThreshold = canvasHeight * 0.17f

    return when (startDest) {
        SurfaceDestination.DASHBOARD -> {
            if (lockedAxis == DragAxis.HORIZONTAL) {
                when {
                    deltaX < -hThreshold || velocityX < -700f -> mapSwipeTargetToDest(telemetry.swipeLeftTarget, SurfaceDestination.APP_DRAWER)
                    deltaX > hThreshold || velocityX > 700f -> mapSwipeTargetToDest(telemetry.swipeRightTarget, SurfaceDestination.GEMINI_AI)
                    else -> SurfaceDestination.DASHBOARD
                }
            } else {
                when {
                    deltaY > vThreshold || velocityY > 700f -> SurfaceDestination.QUICK_SETTINGS
                    deltaY < -vThreshold || velocityY < -700f -> mapSwipeTargetToDest(telemetry.swipeUpTarget, SurfaceDestination.MUSIC_STUDIO)
                    else -> SurfaceDestination.DASHBOARD
                }
            }
        }
        SurfaceDestination.APP_DRAWER -> {
            if (lockedAxis == DragAxis.HORIZONTAL && (deltaX > hThreshold || velocityX > 650f)) {
                SurfaceDestination.DASHBOARD
            } else SurfaceDestination.APP_DRAWER
        }
        SurfaceDestination.GEMINI_AI -> {
            if (lockedAxis == DragAxis.HORIZONTAL && (deltaX < -hThreshold || velocityX < -650f)) {
                SurfaceDestination.DASHBOARD
            } else SurfaceDestination.GEMINI_AI
        }
        SurfaceDestination.QUICK_SETTINGS -> {
            if (lockedAxis == DragAxis.VERTICAL && (deltaY < -vThreshold || velocityY < -620f)) {
                SurfaceDestination.DASHBOARD
            } else SurfaceDestination.QUICK_SETTINGS
        }
        SurfaceDestination.MUSIC_STUDIO -> {
            if (lockedAxis == DragAxis.VERTICAL && (deltaY > vThreshold || velocityY > 620f)) {
                SurfaceDestination.DASHBOARD
            } else SurfaceDestination.MUSIC_STUDIO
        }
    }
}
