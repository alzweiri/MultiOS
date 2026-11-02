package com.galaxy.m21.desktop.ui.recents

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.hardware.AppEntry
import com.galaxy.m21.desktop.hardware.HardwareViewModel
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.components.MultiOsPullTabCapsule
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * MultiOS Spatial Liquid Glass Recents Deck & Native OS App Switcher Bridge:
 *
 * - Real OS task snapshot images rendered directly from /data/system_ce/0/snapshots JPG files!
 * - Per-app privacy blur toggleable on each card and in settings.
 * - Tap any card to switch to that app immediately.
 * - Swipe UP on any card to flick-dismiss it from Recents!
 * - Includes 1-tap "System Recents" button in top header.
 */
@Composable
fun RecentsDeckOverlay(
    hardwareViewModel: HardwareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val recentApps by hardwareViewModel.recentApps.collectAsState()
    val recentTaskSnapshots by hardwareViewModel.recentTaskSnapshots.collectAsState()
    val blurredPackages by hardwareViewModel.blurredRecentPackages.collectAsState()
    val accent = telemetry.resolveAccentColor()

    val scrim = if (telemetry.liquidGlassEnabled) {
        themeColors.overlayScrimGlass
    } else {
        themeColors.overlayScrimMatte
    }

    val listState = rememberLazyListState()
    val snapFling = rememberSnapFlingBehavior(lazyListState = listState)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim)
            .clickable(interactionSource = null, indication = null) { onDismiss() }
            .pointerInput(Unit) {
                var totalDy = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDy = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        totalDy += dragAmount
                        if (totalDy > 48f) {
                            change.consume()
                            onDismiss()
                            totalDy = -999f
                        }
                    }
                )
            }
            .padding(horizontal = 26.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Title + Native System Recents Pill + Clear All + Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null) {},
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MultiOsIcons.RecentsCards(Modifier.size(19.dp), themeColors.iconTint)
                    Text(
                        text = "Recents",
                        color = themeColors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1-Tap Native Android OS Recents Button
                    LiquidGlassBox(
                        modifier = Modifier
                            .height(34.dp)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                hardwareViewModel.triggerNativeSystemRecents(context)
                            },
                        shape = RoundedCornerShape(50),
                        borderColor = accent.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 13.dp)
                                .height(34.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MultiOsIcons.RecentsCards(Modifier.size(13.dp), accent)
                            Text(
                                text = if (telemetry.isAccessibilityRecentsReady) "System Recents" else "Enable OS Recents",
                                color = themeColors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    if (recentApps.isNotEmpty()) {
                        LiquidGlassBox(
                            modifier = Modifier
                                .height(34.dp)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    hardwareViewModel.clearRecentApps()
                                },
                            shape = RoundedCornerShape(50),
                            borderColor = themeColors.subtleBorder
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .height(34.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear All",
                                    color = themeColors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(themeColors.subtleFill)
                            .border(0.75.dp, themeColors.subtleBorder, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        MultiOsIcons.Close(Modifier.size(14.dp), themeColors.iconTint)
                    }
                }
            }

            // Center Carousel of Recent App Liquid Glass Cards
            if (recentApps.isEmpty()) {
                LiquidGlassBox(
                    modifier = Modifier
                        .width(340.dp)
                        .height(150.dp)
                        .clickable(interactionSource = null, indication = null) {},
                    shape = RoundedCornerShape(26.dp),
                    borderColor = themeColors.subtleBorder
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MultiOsIcons.RecentsCards(Modifier.size(28.dp), themeColors.textSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Recent Apps",
                            color = themeColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Launch apps or tap System Recents above",
                            color = themeColors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            } else {
                LazyRow(
                    state = listState,
                    flingBehavior = snapFling,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(interactionSource = null, indication = null) {}
                ) {
                    items(
                        items = recentApps,
                        key = { it.packageName }
                    ) { app ->
                        RecentAppGlassCard(
                            app = app,
                            snapshot = recentTaskSnapshots[app.packageName],
                            isBlurred = app.packageName in blurredPackages,
                            accent = accent,
                            onToggleBlur = { hardwareViewModel.toggleRecentPackageBlur(app.packageName) },
                            onLaunch = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                hardwareViewModel.launchApp(context, app)
                            },
                            onSwipeDismiss = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                hardwareViewModel.removeRecentApp(app.packageName)
                            },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(180),
                                placementSpec = spring(dampingRatio = 0.80f, stiffness = 380f),
                                fadeOutSpec = tween(150)
                            )
                        )
                    }
                }
            }

            // Bottom Pull Tab Capsule
            MultiOsPullTabCapsule(
                pullUpward = true,
                onTrigger = onDismiss
            )
        }
    }
}

@Composable
private fun RecentAppGlassCard(
    app: AppEntry,
    snapshot: android.graphics.Bitmap?,
    isBlurred: Boolean,
    accent: Color,
    onToggleBlur: () -> Unit,
    onLaunch: () -> Unit,
    onSwipeDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalMultiOsColors.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
            .graphicsLayer {
                val dismissRatio = (-dragOffsetY.value / 240f).coerceIn(0f, 1f)
                alpha = 1f - dismissRatio * 0.75f
                val s = 1f - dismissRatio * 0.08f
                scaleX = s
                scaleY = s
            }
            .width(204.dp)
            .height(244.dp)
            .pointerInput(app.packageName) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val next = (dragOffsetY.value + dragAmount).coerceIn(-320f, 45f)
                            dragOffsetY.snapTo(next)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (dragOffsetY.value < -85f) {
                                dragOffsetY.animateTo(-360f, tween(150))
                                onSwipeDismiss()
                            } else {
                                dragOffsetY.animateTo(0f, spring(dampingRatio = 0.76f, stiffness = 380f))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { dragOffsetY.animateTo(0f) }
                    }
                )
            }
    ) {
        LiquidGlassBox(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onLaunch() },
            shape = RoundedCornerShape(26.dp),
            borderColor = themeColors.subtleBorder,
            glassAlpha = if (themeColors.isLight) 0.80f else 0.62f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header row with app badge & privacy eye & swipe-up dismiss button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                        Text(
                            text = app.name,
                            color = themeColors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Privacy Blur Toggle Button
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isBlurred) accent.copy(alpha = 0.28f) else themeColors.subtleFill)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onToggleBlur()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBlurred) "🔒" else "👁",
                                fontSize = 10.sp
                            )
                        }

                        // Dismiss button
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(themeColors.subtleFill)
                                .clickable { onSwipeDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.Close(Modifier.size(9.dp), themeColors.iconTint)
                        }
                    }
                }

                // Center Preview Stage with Real OS Task Snapshot or App Icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accent.copy(alpha = if (themeColors.isLight) 0.14f else 0.10f),
                                    themeColors.recessedScreenBg
                                )
                            )
                        )
                        .border(0.6.dp, themeColors.subtleBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (snapshot != null) {
                        Image(
                            bitmap = snapshot.asImageBitmap(),
                            contentDescription = app.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isBlurred) Modifier.blur(22.dp) else Modifier)
                        )
                    } else {
                        val squircle = RoundedCornerShape(18.dp)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(8.dp, squircle)
                                .clip(squircle)
                                .background(themeColors.subtleFill)
                                .then(if (isBlurred) Modifier.blur(20.dp) else Modifier),
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
                                MultiOsIcons.HomeHouse(Modifier.size(28.dp), themeColors.iconTint)
                            }
                        }
                    }
                }

                // Subtle swipe-up pill indicator
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(3.5.dp)
                        .clip(CircleShape)
                        .background(themeColors.textSecondary.copy(alpha = 0.35f))
                )
            }
        }
    }
}
