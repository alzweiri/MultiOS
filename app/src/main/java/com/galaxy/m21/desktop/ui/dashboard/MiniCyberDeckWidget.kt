package com.galaxy.m21.desktop.ui.dashboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.hardware.AppEntry
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.drawer.MinimalistTouchKeyboard
import com.galaxy.m21.desktop.ui.drawer.ResizableFourSideKeyboardContainer
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.shaders.LocalLiquidGlassConfig
import com.galaxy.m21.desktop.ui.theme.HomeKeyboardMode
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlinx.coroutines.delay

/**
 * MultiOS Home Screen Keyboard System:
 *
 * 1. Supports BOTH **Floating Deck** (`HomeKeyboardMode.FLOATING`) AND **Full Screen Keyboard**
 *    (`HomeKeyboardMode.FULL_POPUP`) — resizable from ALL 4 SIDES!
 * 2. Zero-space contiguous touch hitboxes (`0.dp`) and invisible key borders for effortless fast typing.
 * 3. Seamless **Extending Base 1x4 Swipeable App Search Tray** with Two-Stage Disappear Choreography.
 */
@Composable
fun MiniCyberDeckWidget(
    screenText: String,
    screenSubtitle: String,
    installedApps: List<AppEntry>,
    keyboardMode: HomeKeyboardMode,
    isPopupOpen: Boolean,
    widthDp: Float,
    heightDp: Float,
    editMode: Boolean,
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onToggleKeyboardMode: () -> Unit,
    onOpenFullPopup: () -> Unit,
    onResizeDp: (dwDp: Float, dhDp: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val glassConfig = LocalLiquidGlassConfig.current
    val themeColors = LocalMultiOsColors.current
    val isLight = themeColors.isLight
    val currentOnResize by rememberUpdatedState(onResizeDp)

    // When Full Screen Keyboard mode is active on Home Screen, display a sleek search launcher pill
    if (keyboardMode == HomeKeyboardMode.FULL_POPUP && !editMode) {
        LiquidGlassBox(
            modifier = modifier
                .width(widthDp.coerceIn(240f, 380f).dp)
                .height(48.dp)
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onOpenFullPopup()
                },
            shape = RoundedCornerShape(24.dp),
            borderColor = if (isPopupOpen) themeColors.accent.copy(alpha = 0.65f) else themeColors.subtleBorder,
            glassAlpha = if (isLight) 0.76f else 0.56f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    MultiOsIcons.KeyboardGlyph(Modifier.size(17.dp), themeColors.iconTint)
                    Text(
                        text = if (screenText.isEmpty()) "Full Screen Keyboard" else "$screenText▏",
                        color = if (screenText.isEmpty()) themeColors.textSecondary else themeColors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(themeColors.subtleFill)
                        .border(0.6.dp, themeColors.subtleBorder, RoundedCornerShape(50))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onToggleKeyboardMode()
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Floater",
                        color = themeColors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
        return
    }

    val matchedApps = remember(screenText, installedApps) {
        val q = screenText.trim()
        if (q.isEmpty()) emptyList()
        else installedApps.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.packageName.substringAfterLast('.').contains(q, ignoreCase = true)
        }
    }

    val hasActiveQueryApps = screenText.isNotBlank() && matchedApps.isNotEmpty() && !editMode
    var retainedApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var appsVisible by remember { mutableStateOf(false) }
    var trayExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(hasActiveQueryApps, matchedApps) {
        if (hasActiveQueryApps) {
            retainedApps = matchedApps
            trayExpanded = true
            appsVisible = true
        } else {
            appsVisible = false
            delay(160L)
            trayExpanded = false
            delay(220L)
            if (!appsVisible) retainedApps = emptyList()
        }
    }

    val extensionHeightDp by animateDpAsState(
        targetValue = if (trayExpanded) 78.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "FloatingKeyboardBaseExtension"
    )
    val appsAlpha by animateFloatAsState(
        targetValue = if (appsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "FloatingAppsFade"
    )
    val appsScale by animateFloatAsState(
        targetValue = if (appsVisible) 1f else 0.90f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "FloatingAppsScale"
    )

    val row1 = remember { listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P") }
    val row2 = remember { listOf("A", "S", "D", "F", "G", "H", "J", "K", "L") }
    val row3 = remember { listOf("Z", "X", "C", "V", "B", "N", "M") }

    val seedKey = if (isLight) Color(0xFFFFFFFF) else Color.White
    val tintedKey = lerp(seedKey, glassConfig.tintColor, glassConfig.tintStrength)

    val keyFillBrush = remember(tintedKey, glassConfig.enabled, isLight) {
        if (!glassConfig.enabled) {
            if (isLight) Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)))
            else Brush.verticalGradient(listOf(Color(0xFF232835), Color(0xFF1B1F2A)))
        } else if (isLight) {
            Brush.verticalGradient(
                0.0f to lerp(Color.White, tintedKey, 0.3f).copy(alpha = 0.85f),
                1.0f to Color(0xFFE2E8F0).copy(alpha = 0.68f)
            )
        } else {
            Brush.verticalGradient(
                0.0f to tintedKey.copy(alpha = 0.20f),
                1.0f to tintedKey.copy(alpha = 0.08f)
            )
        }
    }

    Column(
        modifier = modifier.width(widthDp.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // UPWARD-POPPING 1x4 APP STRIP ISLAND (Stationary base beneath user fingers!)
        if (extensionHeightDp > 1.dp) {
            LiquidGlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .height(extensionHeightDp),
                shape = RoundedCornerShape(18.dp),
                borderColor = themeColors.subtleBorder,
                glassAlpha = if (isLight) 0.82f else 0.65f
            ) {
                ExtendingOneByFourAppStrip(
                    apps = if (hasActiveQueryApps) matchedApps else retainedApps,
                    contentAlpha = appsAlpha,
                    contentScale = appsScale,
                    stripHeight = (extensionHeightDp - 4.dp).coerceAtLeast(0.dp),
                    onAppClick = { app ->
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onLaunchApp(app)
                    }
                )
            }
        }

        // 100% STATIONARY KEYBOARD BASE (Never shifts or expands downwards while typing!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        ) {
            LiquidGlassBox(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(22.dp),
                borderColor = if (editMode) themeColors.accent.copy(alpha = 0.72f) else themeColors.subtleBorder,
                glassAlpha = if (isLight) 0.76f else 0.58f
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // TOP RECESSED SCREEN + FULL SCREEN KEYBOARD MODE SWITCHER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(themeColors.recessedScreenBg)
                        .border(
                            width = 0.7.dp,
                            color = themeColors.subtleBorder,
                            shape = RoundedCornerShape(11.dp)
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        themeColors.accent.copy(
                                            alpha = if (screenText.isEmpty()) 0.38f else 0.95f
                                        )
                                    )
                            )
                            Text(
                                text = if (screenText.isEmpty()) "" else "$screenText▏",
                                color = themeColors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (screenSubtitle.isNotEmpty() && matchedApps.isEmpty()) {
                                Text(
                                    text = screenSubtitle,
                                    color = themeColors.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1
                                )
                            } else if (matchedApps.size > 4) {
                                Text(
                                    text = "${matchedApps.size}",
                                    color = themeColors.textSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            if (screenText.isNotEmpty() && !editMode) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(themeColors.subtleFill)
                                        .clickable { onClear() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    MultiOsIcons.Close(Modifier.size(9.dp), themeColors.iconTint)
                                }
                            }

                            if (!editMode) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(themeColors.subtleFill)
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            onToggleKeyboardMode()
                                            onOpenFullPopup()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    MultiOsIcons.KeyboardGlyph(Modifier.size(11.dp), themeColors.iconTint)
                                }
                            }
                        }
                    }
                }

                // INTEGRATED ZERO-SPACE BORDERLESS MICRO LIQUID GLASS KEYS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        row1.forEach { key ->
                            MicroGlassKey(
                                label = key,
                                fillBrush = keyFillBrush,
                                textColor = themeColors.textPrimary,
                                enabled = !editMode,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onKeyPress(key)
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        row2.forEach { key ->
                            MicroGlassKey(
                                label = key,
                                fillBrush = keyFillBrush,
                                textColor = themeColors.textPrimary,
                                enabled = !editMode,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onKeyPress(key)
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        MicroGlassKey(
                            label = "␣",
                            fillBrush = keyFillBrush,
                            textColor = themeColors.textPrimary,
                            enabled = !editMode,
                            modifier = Modifier.weight(1.3f).fillMaxHeight(),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onKeyPress(" ")
                            }
                        )
                        row3.forEach { key ->
                            MicroGlassKey(
                                label = key,
                                fillBrush = keyFillBrush,
                                textColor = themeColors.textPrimary,
                                enabled = !editMode,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onKeyPress(key)
                                }
                            )
                        }
                        MicroGlassKey(
                            label = "⌫",
                            fillBrush = keyFillBrush,
                            textColor = themeColors.textPrimary,
                            enabled = !editMode,
                            modifier = Modifier.weight(1.25f).fillMaxHeight(),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onBackspace()
                            }
                        )
                        MicroGlassKey(
                            label = "↵",
                            fillBrush = Brush.verticalGradient(
                                listOf(
                                    themeColors.accent.copy(alpha = 0.92f),
                                    themeColors.accent.copy(alpha = 0.76f)
                                )
                            ),
                            textColor = themeColors.onAccent,
                            enabled = !editMode,
                            modifier = Modifier.weight(1.3f).fillMaxHeight(),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                onSubmit()
                            }
                        )
                    }
                }
            }
        }

        // All-Sides Resize Handles when in Layout Edit Mode!
        if (editMode) {
            // Left handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(12.dp)
                    .height(42.dp)
                    .clip(CircleShape)
                    .background(themeColors.accent.copy(alpha = 0.85f))
                    .pointerInput("mini_left") {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dwDp = with(density) { -dragAmount.x.toDp().value }
                            currentOnResize(dwDp, 0f)
                        }
                    }
            )
            // Right handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(12.dp)
                    .height(42.dp)
                    .clip(CircleShape)
                    .background(themeColors.accent.copy(alpha = 0.85f))
                    .pointerInput("mini_right") {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dwDp = with(density) { dragAmount.x.toDp().value }
                            currentOnResize(dwDp, 0f)
                        }
                    }
            )
            // Top handle
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(42.dp)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(themeColors.accent.copy(alpha = 0.85f))
                    .pointerInput("mini_top") {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dhDp = with(density) { -dragAmount.y.toDp().value }
                            currentOnResize(0f, dhDp)
                        }
                    }
            )
            // Bottom-Right corner handle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(themeColors.accent)
                    .pointerInput("mini_corner") {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dwDp = with(density) { dragAmount.x.toDp().value }
                            val dhDp = with(density) { dragAmount.y.toDp().value }
                            currentOnResize(dwDp, dhDp)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(11.dp)) {
                    val c = themeColors.onAccent
                    drawLine(c, Offset(size.width * 0.2f, size.height * 0.8f), Offset(size.width * 0.8f, size.height * 0.2f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.5f, size.height * 0.85f), Offset(size.width * 0.85f, size.height * 0.5f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
}
}

/**
 * 4-Side Resizable Full Screen Keyboard Overlay for the Home Screen!
 * Resizable from Top, Bottom, Left, and Right edges + Extending Base 1x4 Swipeable App Strip!
 */
@Composable
fun HomeFullScreenKeyboardOverlay(
    screenText: String,
    installedApps: List<AppEntry>,
    heightScale: Float,
    bottomInsetDp: Float,
    leftInsetDp: Float,
    rightInsetDp: Float,
    onUpdateBounds: (dHeightScale: Float, dBottomDp: Float, dLeftDp: Float, dRightDp: Float) -> Unit,
    onResetBounds: () -> Unit,
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onSwitchToFloater: () -> Unit,
    onDismissPopup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    val isLight = themeColors.isLight
    var resizeActive by remember { mutableStateOf(false) }

    val matchedApps = remember(screenText, installedApps) {
        val q = screenText.trim()
        if (q.isEmpty()) emptyList()
        else installedApps.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.packageName.substringAfterLast('.').contains(q, ignoreCase = true)
        }
    }

    val hasActiveQueryApps = screenText.isNotBlank() && matchedApps.isNotEmpty()
    var retainedApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var appsVisible by remember { mutableStateOf(false) }
    var trayExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(hasActiveQueryApps, matchedApps) {
        if (hasActiveQueryApps) {
            retainedApps = matchedApps
            trayExpanded = true
            appsVisible = true
        } else {
            appsVisible = false
            delay(160L)
            if (!hasActiveQueryApps) {
                trayExpanded = false
                delay(220L)
                if (!appsVisible) retainedApps = emptyList()
            }
        }
    }

    val extensionHeightDp by animateDpAsState(
        targetValue = if (trayExpanded) 86.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "FullScreenKbBaseExtension"
    )
    val appsAlpha by animateFloatAsState(
        targetValue = if (appsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "FullScreenKbAppsFade"
    )
    val appsScale by animateFloatAsState(
        targetValue = if (appsVisible) 1f else 0.90f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "FullScreenKbAppsScale"
    )

    ResizableFourSideKeyboardContainer(
        bottomInsetDp = bottomInsetDp,
        leftInsetDp = leftInsetDp,
        rightInsetDp = rightInsetDp,
        resizeActive = resizeActive,
        onUpdateBounds = onUpdateBounds,
        modifier = modifier
    ) {
        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            borderColor = themeColors.subtleBorder,
            glassAlpha = if (isLight) 0.84f else 0.68f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (extensionHeightDp > 1.dp) {
                    ExtendingOneByFourAppStrip(
                        apps = if (hasActiveQueryApps) matchedApps else retainedApps,
                        contentAlpha = appsAlpha,
                        contentScale = appsScale,
                        stripHeight = (extensionHeightDp - 4.dp).coerceAtLeast(0.dp),
                        iconSize = 42.dp,
                        onAppClick = { app ->
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onLaunchApp(app)
                        }
                    )
                }

                // Search Bar + 4-Side Resize Toggle + Reset + Mode Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(themeColors.recessedScreenBg)
                        .border(0.75.dp, themeColors.subtleBorder, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (screenText.isEmpty()) "Full Screen Keyboard — Search apps..." else "$screenText▏",
                        color = if (screenText.isEmpty()) themeColors.textSecondary else themeColors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        if (screenText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(themeColors.subtleFill)
                                    .clickable { onClear() },
                                contentAlignment = Alignment.Center
                            ) {
                                MultiOsIcons.Close(Modifier.size(10.dp), themeColors.iconTint)
                            }
                        }

                        // 4-Side Resize Mode Toggle
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (resizeActive) themeColors.accent else themeColors.subtleFill)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    resizeActive = !resizeActive
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.ResizeFourSides(
                                modifier = Modifier.size(13.dp),
                                tint = if (resizeActive) themeColors.onAccent else themeColors.iconTint
                            )
                        }

                        if (resizeActive) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(themeColors.subtleFill)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        onResetBounds()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                MultiOsIcons.ResetArrow(Modifier.size(12.dp), themeColors.iconTint)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(themeColors.subtleFill)
                                .clickable { onSwitchToFloater() }
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Floating Deck",
                                color = themeColors.textPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(themeColors.subtleFill)
                                .clickable { onDismissPopup() },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.Close(Modifier.size(11.dp), themeColors.iconTint)
                        }
                    }
                }

                MinimalistTouchKeyboard(
                    onKeyPress = onKeyPress,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onEnter = onSubmit,
                    heightScale = heightScale,
                    isFullLandscapeGboard = true,
                    showOuterGlassFrame = false
                )
            }
        }
    }
}

/**
 * Strict 1x4 Horizontal Swipeable App Strip inside the Extended Keyboard Base.
 */
@Suppress("UnusedBoxWithConstraintsScope")
@Composable
private fun ExtendingOneByFourAppStrip(
    apps: List<AppEntry>,
    contentAlpha: Float,
    contentScale: Float,
    stripHeight: Dp,
    iconSize: Dp = 36.dp,
    onAppClick: (AppEntry) -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    val listState = rememberLazyListState()
    val snapFling = rememberSnapFlingBehavior(lazyListState = listState)
    val canScrollHorizontal by remember(apps.size) { derivedStateOf { apps.size > 4 } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(stripHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(themeColors.subtleFill)
            .border(0.65.dp, themeColors.subtleBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        val spacing = 6.dp
        val cellWidth = ((maxWidth - spacing * 3) / 4f).coerceAtLeast(44.dp)

        LazyRow(
            state = listState,
            flingBehavior = snapFling,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                }
        ) {
            items(
                items = apps,
                key = { it.packageName }
            ) { app ->
                Column(
                    modifier = Modifier
                        .width(cellWidth)
                        .animateItem(
                            fadeInSpec = tween(durationMillis = 160),
                            placementSpec = spring(dampingRatio = 0.80f, stiffness = 420f),
                            fadeOutSpec = tween(durationMillis = 130)
                        )
                        .clip(RoundedCornerShape(11.dp))
                        .clickable { onAppClick(app) }
                        .padding(vertical = 2.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val squircle = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .shadow(4.dp, squircle)
                            .clip(squircle)
                            .background(themeColors.recessedScreenBg)
                            .border(0.6.dp, themeColors.subtleBorder, squircle),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            MultiOsIcons.HomeHouse(Modifier.size(16.dp), themeColors.iconTint)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = app.name,
                        color = themeColors.textPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (canScrollHorizontal && contentAlpha > 0.25f) {
            val pageCount = ((apps.size + 3) / 4).coerceAtMost(5)
            val activePage by remember {
                derivedStateOf { (listState.firstVisibleItemIndex / 4).coerceIn(0, pageCount - 1) }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (p in 0 until pageCount) {
                    Box(
                        modifier = Modifier
                            .size(if (p == activePage) 4.5.dp else 3.5.dp)
                            .clip(CircleShape)
                            .background(
                                if (p == activePage) themeColors.accent
                                else themeColors.textSecondary.copy(alpha = 0.35f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Zero-space contiguous hitbox + invisible-border Micro Liquid Glass Key!
 */
@Composable
private fun MicroGlassKey(
    label: String,
    fillBrush: Brush,
    textColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 1.4.dp, vertical = 1.6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (isPressed) 0.93f else 1f
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(6.5.dp))
                .background(fillBrush)
                .then(
                    if (isPressed) Modifier.background(Color.White.copy(alpha = 0.16f)) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1
            )
        }
    }
}
