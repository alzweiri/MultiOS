package com.galaxy.m21.desktop.ui.mobile

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * MultiOS Mobile Vertical Daily Home Screen (Portrait Mode):
 *
 * - Custom X*Y Grid (configurable columns & rows).
 * - macOS Freeform Scatter mode (drag icons freely or snap to grid).
 * - Multi-page HorizontalPager with iOS liquid morphing page dots.
 * - Symmetrical Top & Bottom pull triggers for Quick Settings & App Drawer.
 * - Floating Liquid Glass bottom dock for primary phone apps.
 * - Configurable Spotlight search bar (Top vs Bottom position).
 */
@Composable
fun MobileHomeScreen(
    hardwareViewModel: HardwareViewModel,
    onOpenAppDrawer: () -> Unit,
    onOpenQuickSettings: () -> Unit,
    onOpenRecents: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val themeColors = LocalMultiOsColors.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val installedApps by hardwareViewModel.installedApps.collectAsState()
    val appOffsets by hardwareViewModel.appIconOffsets.collectAsState()
    val accent = telemetry.resolveAccentColor()

    val columns = telemetry.mobileGridCols.coerceIn(3, 6)
    val rows = telemetry.mobileGridRows.coerceIn(3, 8)
    val pageSize = (columns * rows).coerceAtLeast(1)

    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                it.packageName.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    val pageCount = ((filteredApps.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Dedicated dock apps: take first 4 apps or apps with dialer/messages/browser/camera
    val dockApps = remember(installedApps) {
        val candidates = installedApps.filter { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("dialer") || pkg.contains("phone") ||
                pkg.contains("messaging") || pkg.contains("mms") ||
                pkg.contains("chrome") || pkg.contains("browser") ||
                pkg.contains("camera")
        }
        if (candidates.size >= 4) candidates.take(4)
        else installedApps.take(4)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =========================================================================
            // TOP BAR: Clock, Battery Pill, Quick Settings Pull Tab, Settings Gear
            // =========================================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clock + Date
                    Column {
                        Text(
                            text = telemetry.currentTimeFormatted.take(5),
                            color = themeColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "${telemetry.batteryPercent}% • ${telemetry.temperatureCpu}°C",
                            color = themeColors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    // Top Pull Tab for Quick Settings
                    MultiOsPullTabCapsule(
                        pullUpward = false,
                        onTrigger = onOpenQuickSettings
                    )

                    // Recents and Settings Quick Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(themeColors.subtleFill)
                                .border(0.6.dp, themeColors.subtleBorder, CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onOpenRecents()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.RecentsCards(Modifier.size(16.dp), themeColors.iconTint)
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(themeColors.subtleFill)
                                .border(0.6.dp, themeColors.subtleBorder, CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onOpenSettings()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.Gear(Modifier.size(16.dp), themeColors.iconTint)
                        }
                    }
                }

                // Optional Top Search Bar
                if (telemetry.mobileSearchBarAtTop) {
                    Spacer(modifier = Modifier.height(6.dp))
                    MobileSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClear = { searchQuery = "" },
                        onOpenDrawer = onOpenAppDrawer
                    )
                }
            }

            // =========================================================================
            // CENTER: HorizontalPager Pages of Apps (or Freeform Scatter)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (telemetry.mobileScatterMode) {
                    // Freeform macOS-Style Canvas: User can drag icons anywhere freely
                    Box(modifier = Modifier.fillMaxSize()) {
                        filteredApps.take(pageSize).forEach { app ->
                            val offset = appOffsets[app.packageName] ?: Pair(0f, 0f)
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(offset.first.roundToInt(), offset.second.roundToInt()) }
                                    .pointerInput(app.packageName) {
                                        detectDragGesturesAfterLongPress { change, dragAmount ->
                                            change.consume()
                                            hardwareViewModel.moveAppIcon(
                                                app.packageName,
                                                dragAmount.x,
                                                dragAmount.y
                                            )
                                        }
                                    }
                            ) {
                                MobileAppIconTile(
                                    app = app,
                                    onClick = { hardwareViewModel.launchApp(context, app) }
                                )
                            }
                        }
                    }
                } else {
                    // Structured Multi-Page Squircle Grid
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val startIdx = pageIdx * pageSize
                        val pageApps = filteredApps.drop(startIdx).take(pageSize)

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pageApps, key = { it.packageName }) { app ->
                                MobileAppIconTile(
                                    app = app,
                                    onClick = { hardwareViewModel.launchApp(context, app) }
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // BOTTOM SECTION: Morphing Page Dots + Bottom Search Bar + Glass Dock
            // =========================================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // iOS Liquid Morphing Page Dots
                if (pageCount > 1 && !telemetry.mobileScatterMode) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        repeat(pageCount) { pageIdx ->
                            val isSelected = pagerState.currentPage == pageIdx
                            val dotColor by animateColorAsState(
                                targetValue = if (isSelected) accent else themeColors.subtleBorder,
                                animationSpec = tween(220)
                            )
                            val dotWidth by animateDpAsState(
                                targetValue = if (isSelected) 18.dp else 6.dp,
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
                            )
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(dotWidth)
                                    .clip(RoundedCornerShape(50))
                                    .background(dotColor)
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pageIdx)
                                        }
                                    }
                            )
                        }
                    }
                }

                // Optional Bottom Search Bar
                if (!telemetry.mobileSearchBarAtTop) {
                    MobileSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClear = { searchQuery = "" },
                        onOpenDrawer = onOpenAppDrawer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Floating Liquid Glass Bottom Dock
                LiquidGlassBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(28.dp),
                    borderColor = themeColors.subtleBorder,
                    glassAlpha = if (themeColors.isLight) 0.82f else 0.65f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dockApps.forEach { app ->
                            MobileDockIconTile(
                                app = app,
                                onClick = { hardwareViewModel.launchApp(context, app) }
                            )
                        }

                        // App Library Shortcut Icon in Dock
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(themeColors.subtleFill)
                                .border(0.8.dp, themeColors.subtleBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onOpenAppDrawer()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.EditGrid(Modifier.size(22.dp), themeColors.iconTint)
                        }
                    }
                }

                // Symmetrical Bottom Pull Tab Capsule for App Drawer swipe
                Spacer(modifier = Modifier.height(4.dp))
                MultiOsPullTabCapsule(
                    pullUpward = true,
                    onTrigger = onOpenAppDrawer
                )
            }
        }
    }
}

@Composable
private fun MobileSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    LiquidGlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable { onOpenDrawer() },
        shape = RoundedCornerShape(50),
        borderColor = if (query.isNotEmpty()) themeColors.accent.copy(alpha = 0.65f) else themeColors.subtleBorder,
        glassAlpha = if (themeColors.isLight) 0.80f else 0.60f
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MultiOsIcons.GeminiSpark(Modifier.size(16.dp), themeColors.accent)
            Text(
                text = if (query.isEmpty()) "Search apps, web or Gemini..." else query,
                color = if (query.isEmpty()) themeColors.textSecondary else themeColors.textPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            if (query.isNotEmpty()) {
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
        }
    }
}

@Composable
private fun MobileAppIconTile(
    app: AppEntry,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    val squircle = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = if (themeColors.isLight) 4.dp else 8.dp,
                    shape = squircle,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(squircle)
                .background(themeColors.subtleFill)
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            themeColors.subtleBorder
                        )
                    ),
                    shape = squircle
                ),
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
                Text(
                    text = app.name.take(1).uppercase(),
                    color = themeColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = app.name,
            color = themeColors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MobileDockIconTile(
    app: AppEntry,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    val squircle = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .size(52.dp)
            .shadow(
                elevation = if (themeColors.isLight) 6.dp else 10.dp,
                shape = squircle,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(squircle)
            .background(themeColors.subtleFill)
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        themeColors.subtleBorder
                    )
                ),
                shape = squircle
            )
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
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
            Text(
                text = app.name.take(1).uppercase(),
                color = themeColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
