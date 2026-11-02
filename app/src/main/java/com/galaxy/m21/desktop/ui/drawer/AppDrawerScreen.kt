package com.galaxy.m21.desktop.ui.drawer

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.theme.DrawerAppAlignment
import com.galaxy.m21.desktop.ui.theme.DrawerKeyboardMode
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * MultiOS Landscape App Library & 4-Side Resizable Liquid Glass Keyboard:
 *
 * - Blurred Home Screen WITH KNOBS stays visibly underneath (`overlayScrimGlass`).
 * - App Area Alignment Picker: Choose **Left**, **Center**, or **Right** for the App Grid!
 * - Keyboard Mode Picker: Choose **Freeform** OR **Full Screen Keyboard** (resizable from ALL 4 SIDES!).
 */
@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun AppDrawerScreen(
    hardwareViewModel: HardwareViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val themeColors = LocalMultiOsColors.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val installedApps by hardwareViewModel.installedApps.collectAsState()
    val appFreeformOffsets by hardwareViewModel.appIconOffsets.collectAsState()
    val kbOffset by hardwareViewModel.drawerKeyboardOffset.collectAsState()
    val kbSize by hardwareViewModel.drawerKeyboardSize.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var fullKbResizeActive by remember { mutableStateOf(false) }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                it.packageName.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    val scrim = if (telemetry.liquidGlassEnabled) {
        themeColors.overlayScrimGlass
    } else {
        themeColors.overlayScrimMatte
    }

    val isFullScreenKeyboard = telemetry.drawerKeyboardMode == DrawerKeyboardMode.FULL_LANDSCAPE_GBOARD
    val appAlignment = telemetry.drawerAppAlignment

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim)
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
        val maxW = maxWidth
        val appAreaAlign = when (appAlignment) {
            DrawerAppAlignment.LEFT -> Alignment.TopStart
            DrawerAppAlignment.CENTER -> Alignment.TopCenter
            DrawerAppAlignment.RIGHT -> Alignment.TopEnd
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpotlightSearchAndLayoutBar(
                searchQuery = searchQuery,
                appAlignment = appAlignment,
                keyboardMode = telemetry.drawerKeyboardMode,
                isResizeActive = fullKbResizeActive,
                onSelectAlignment = { align ->
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    hardwareViewModel.setDrawerAppAlignment(align)
                },
                onToggleKeyboardMode = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    hardwareViewModel.toggleDrawerKeyboardMode()
                },
                onToggleResizeMode = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    fullKbResizeActive = !fullKbResizeActive
                },
                onResetKeyboardArea = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    hardwareViewModel.resetDrawerKeyboardLayout()
                    hardwareViewModel.resetFullKeyboardBounds()
                    hardwareViewModel.resetAppIconOffsets()
                },
                onClear = { searchQuery = "" },
                onClose = onClose
            )

            if (isFullScreenKeyboard) {
                // =====================================================================
                // 4-SIDE RESIZABLE FULL SCREEN KEYBOARD MODE
                // =====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = appAreaAlign
                ) {
                    val gridWidth = if (appAlignment == DrawerAppAlignment.CENTER) maxW * 0.88f else maxW * 0.76f
                    AnimatedAppGrid(
                        apps = filteredApps,
                        columns = 6,
                        appFreeformOffsets = appFreeformOffsets,
                        onMoveApp = { pkg, dx, dy -> hardwareViewModel.moveAppIcon(pkg, dx, dy) },
                        onLaunchApp = { app -> hardwareViewModel.launchApp(context, app) },
                        modifier = Modifier
                            .width(gridWidth)
                            .fillMaxHeight()
                    )
                }

                ResizableFourSideKeyboardContainer(
                    bottomInsetDp = telemetry.fullKbBottomDp,
                    leftInsetDp = telemetry.fullKbLeftDp,
                    rightInsetDp = telemetry.fullKbRightDp,
                    resizeActive = fullKbResizeActive,
                    onUpdateBounds = { dh, db, dl, dr ->
                        hardwareViewModel.updateFullKeyboardBounds(dh, db, dl, dr)
                    }
                ) {
                    MinimalistTouchKeyboard(
                        onKeyPress = { key -> searchQuery += key },
                        onBackspace = { if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1) },
                        onClear = { searchQuery = "" },
                        onEnter = {
                            filteredApps.firstOrNull()?.let { firstApp ->
                                hardwareViewModel.launchApp(context, firstApp)
                            }
                        },
                        heightScale = telemetry.fullKbHeightScale,
                        isFullLandscapeGboard = true
                    )
                }
            } else {
                // =====================================================================
                // FREEFORM FLOATING KEYBOARD MODE:
                // App Area Placed Left / Center / Right + Real-Time Repositionable & Resizable Keyboard!
                // =====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // App Grid Area positioned according to Left / Center / Right selection
                    val appGridWidth = (maxW * 0.54f).coerceAtLeast(320.dp)
                    Box(
                        modifier = Modifier
                            .width(appGridWidth)
                            .fillMaxHeight()
                            .align(
                                when (appAlignment) {
                                    DrawerAppAlignment.LEFT -> Alignment.CenterStart
                                    DrawerAppAlignment.CENTER -> Alignment.Center
                                    DrawerAppAlignment.RIGHT -> Alignment.CenterEnd
                                }
                            )
                    ) {
                        AnimatedAppGrid(
                            apps = filteredApps,
                            columns = 5,
                            appFreeformOffsets = appFreeformOffsets,
                            onMoveApp = { pkg, dx, dy -> hardwareViewModel.moveAppIcon(pkg, dx, dy) },
                            onLaunchApp = { app -> hardwareViewModel.launchApp(context, app) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Default anchor sits opposite the app grid, then applies persisted user freeform offset!
                    val defaultKbAlign = when (appAlignment) {
                        DrawerAppAlignment.RIGHT -> Alignment.CenterStart
                        else -> Alignment.CenterEnd
                    }

                    FreeformDraggableResizableKeyboard(
                        widthDp = kbSize.first.coerceIn(260f, (maxW.value - 32f).coerceAtLeast(280f)),
                        heightScale = kbSize.second,
                        offsetPx = kbOffset,
                        onMovePx = { dx, dy -> hardwareViewModel.moveDrawerKeyboard(dx, dy) },
                        onResize = { dwDp, dScale -> hardwareViewModel.resizeDrawerKeyboard(dwDp, dScale) },
                        onReset = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            hardwareViewModel.resetDrawerKeyboardLayout()
                        },
                        onKeyPress = { key -> searchQuery += key },
                        onBackspace = { if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1) },
                        onClear = { searchQuery = "" },
                        onEnter = {
                            filteredApps.firstOrNull()?.let { firstApp ->
                                hardwareViewModel.launchApp(context, firstApp)
                            }
                        },
                        density = density,
                        modifier = Modifier.align(defaultKbAlign)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedAppGrid(
    apps: List<AppEntry>,
    columns: Int,
    appFreeformOffsets: Map<String, Pair<Float, Float>>,
    onMoveApp: (String, Float, Float) -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = apps,
            key = { it.packageName }
        ) { app ->
            val offset = appFreeformOffsets[app.packageName] ?: Pair(0f, 0f)
            Box(
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = tween(durationMillis = 175),
                        placementSpec = spring(dampingRatio = 0.80f, stiffness = 400f),
                        fadeOutSpec = tween(durationMillis = 135)
                    )
                    .offset { IntOffset(offset.first.roundToInt(), offset.second.roundToInt()) }
                    .pointerInput(app.packageName) {
                        detectDragGesturesAfterLongPress { change, dragAmount ->
                            change.consume()
                            onMoveApp(app.packageName, dragAmount.x, dragAmount.y)
                        }
                    }
            ) {
                AppleAppTile(
                    app = app,
                    onClick = { onLaunchApp(app) }
                )
            }
        }
    }
}

@Composable
private fun FreeformDraggableResizableKeyboard(
    widthDp: Float,
    heightScale: Float,
    offsetPx: Pair<Float, Float>,
    onMovePx: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onReset: () -> Unit,
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEnter: () -> Unit,
    density: androidx.compose.ui.unit.Density,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalMultiOsColors.current
    val latestMove by rememberUpdatedState(onMovePx)
    val latestResize by rememberUpdatedState(onResize)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetPx.first.roundToInt(), offsetPx.second.roundToInt()) }
            .width(widthDp.dp)
    ) {
        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            borderColor = themeColors.subtleBorder,
            glassAlpha = if (themeColors.isLight) 0.78f else 0.58f
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Drag Handle Bar + Quick Reset Keyboard Icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                latestMove(dragAmount.x, dragAmount.y)
                            }
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.size(18.dp))

                    // Tactile Grab Pill for Real-Time Freeform Repositioning
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(4.5.dp)
                            .clip(CircleShape)
                            .background(themeColors.textSecondary.copy(alpha = 0.45f))
                    )

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(themeColors.subtleFill)
                            .clickable { onReset() },
                        contentAlignment = Alignment.Center
                    ) {
                        MultiOsIcons.ResetArrow(Modifier.size(11.dp), themeColors.iconTint)
                    }
                }

                MinimalistTouchKeyboard(
                    onKeyPress = onKeyPress,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onEnter = onEnter,
                    heightScale = heightScale,
                    isFullLandscapeGboard = false,
                    showOuterGlassFrame = false
                )
            }
        }

        // Left Side Resize Handle (Adjusts Width + Anchors Right Edge!)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(12.dp)
                .height(44.dp)
                .pointerInput("freeform_left") {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dwDp = with(density) { -dragAmount.x.toDp().value }
                        latestResize(dwDp, 0f)
                        latestMove(dragAmount.x * 0.5f, 0f)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(themeColors.textSecondary.copy(alpha = 0.42f))
            )
        }

        // Right Side Resize Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(12.dp)
                .height(44.dp)
                .pointerInput("freeform_right") {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dwDp = with(density) { dragAmount.x.toDp().value }
                        latestResize(dwDp, 0f)
                        latestMove(dragAmount.x * 0.5f, 0f)
                    }
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(themeColors.textSecondary.copy(alpha = 0.42f))
            )
        }

        // Bottom Edge Resize Handle
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(52.dp)
                .height(12.dp)
                .pointerInput("freeform_bottom") {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dScale = with(density) { dragAmount.y.toDp().value } / 200f
                        latestResize(0f, dScale)
                        latestMove(0f, dragAmount.y * 0.5f)
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .background(themeColors.textSecondary.copy(alpha = 0.42f))
            )
        }

        // Real-Time Corner Resize Handle (Adjusts Width & Height Scale in Real Time!)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(26.dp)
                .clip(CircleShape)
                .background(themeColors.accent)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dwDp = with(density) { dragAmount.x.toDp().value }
                        val dScale = with(density) { dragAmount.y.toDp().value } / 210f
                        latestResize(dwDp, dScale)
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

@Composable
private fun SpotlightSearchAndLayoutBar(
    searchQuery: String,
    appAlignment: DrawerAppAlignment,
    keyboardMode: DrawerKeyboardMode,
    isResizeActive: Boolean,
    onSelectAlignment: (DrawerAppAlignment) -> Unit,
    onToggleKeyboardMode: () -> Unit,
    onToggleResizeMode: () -> Unit,
    onResetKeyboardArea: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    val themeColors = LocalMultiOsColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Spotlight Search Field
        LiquidGlassBox(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = RoundedCornerShape(50),
            borderColor = if (searchQuery.isNotEmpty()) themeColors.accent.copy(alpha = 0.65f) else themeColors.subtleBorder,
            glassAlpha = if (themeColors.isLight) 0.78f else 0.55f
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        val r = size.minDimension * 0.36f
                        val c = Offset(size.width * 0.42f, size.height * 0.42f)
                        drawCircle(
                            color = themeColors.iconTint.copy(alpha = 0.70f),
                            radius = r,
                            center = c,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        drawLine(
                            color = themeColors.iconTint.copy(alpha = 0.70f),
                            start = Offset(c.x + r * 0.72f, c.y + r * 0.72f),
                            end = Offset(size.width * 0.90f, size.height * 0.90f),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    Text(
                        text = if (searchQuery.isEmpty()) "" else "$searchQuery▏",
                        color = themeColors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
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

        // App Grid Alignment Segmented Picker (Left | Center | Right)
        LiquidGlassBox(
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(50),
            borderColor = themeColors.subtleBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                DrawerAppAlignment.entries.forEach { option ->
                    val selected = appAlignment == option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) themeColors.accent else Color.Transparent)
                            .clickable { onSelectAlignment(option) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.title,
                            color = if (selected) themeColors.onAccent else themeColors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        // Freeform vs Full Screen Keyboard Toggle Pill
        LiquidGlassBox(
            modifier = Modifier
                .height(40.dp)
                .clickable { onToggleKeyboardMode() },
            shape = RoundedCornerShape(50),
            borderColor = themeColors.subtleBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MultiOsIcons.KeyboardGlyph(Modifier.size(15.dp), themeColors.iconTint)
                Text(
                    text = keyboardMode.title,
                    color = themeColors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // 4-Side Resize Button for Full Screen Keyboard
        if (keyboardMode == DrawerKeyboardMode.FULL_LANDSCAPE_GBOARD) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isResizeActive) themeColors.accent else themeColors.subtleFill)
                    .border(0.75.dp, themeColors.subtleBorder, CircleShape)
                    .clickable { onToggleResizeMode() },
                contentAlignment = Alignment.Center
            ) {
                MultiOsIcons.ResizeFourSides(
                    modifier = Modifier.size(16.dp),
                    tint = if (isResizeActive) themeColors.onAccent else themeColors.iconTint
                )
            }
        }

        // Reset Keyboard Area & Icon Offsets Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(themeColors.subtleFill)
                .border(0.75.dp, themeColors.subtleBorder, CircleShape)
                .clickable { onResetKeyboardArea() },
            contentAlignment = Alignment.Center
        ) {
            MultiOsIcons.ResetArrow(Modifier.size(16.dp), themeColors.iconTint)
        }

        // Close App Drawer
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(themeColors.subtleFill)
                .border(0.75.dp, themeColors.subtleBorder, CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            MultiOsIcons.Close(Modifier.size(15.dp), themeColors.iconTint)
        }
    }
}

@Composable
private fun AppleAppTile(
    app: AppEntry,
    onClick: () -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    val squircle = RoundedCornerShape(15.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 3.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = if (themeColors.isLight) 5.dp else 10.dp,
                    shape = squircle,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(squircle)
                .background(themeColors.subtleFill)
                .border(
                    width = 0.75.dp,
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
                FallbackAppleVectorGlyph(appName = app.name, tint = themeColors.iconTint)
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = app.name,
            color = themeColors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FallbackAppleVectorGlyph(appName: String, tint: Color) {
    val lower = appName.lowercase()
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)

        when {
            lower.contains("setting") -> {
                drawCircle(color = tint, radius = w * 0.22f, center = c, style = stroke)
                for (i in 0 until 8) {
                    val rad = i * (Math.PI / 4.0)
                    val p1 = Offset(c.x + (w * 0.30f * cos(rad)).toFloat(), c.y + (h * 0.30f * sin(rad)).toFloat())
                    val p2 = Offset(c.x + (w * 0.44f * cos(rad)).toFloat(), c.y + (h * 0.44f * sin(rad)).toFloat())
                    drawLine(tint, p1, p2, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            lower.contains("camera") || lower.contains("photo") -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.10f, h * 0.24f),
                    size = Size(w * 0.80f, h * 0.56f),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    style = stroke
                )
                drawCircle(color = tint, radius = w * 0.16f, center = Offset(c.x, h * 0.52f), style = stroke)
            }
            lower.contains("clock") || lower.contains("alarm") -> {
                drawCircle(color = tint, radius = w * 0.42f, center = c, style = stroke)
                drawLine(tint, c, Offset(c.x, h * 0.26f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, c, Offset(w * 0.70f, c.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            else -> {
                drawRoundRect(tint, Offset(w * 0.14f, h * 0.14f), Size(w * 0.30f, h * 0.30f), CornerRadius(3.dp.toPx(), 3.dp.toPx()), stroke)
                drawRoundRect(tint, Offset(w * 0.56f, h * 0.14f), Size(w * 0.30f, h * 0.30f), CornerRadius(3.dp.toPx(), 3.dp.toPx()), stroke)
                drawRoundRect(tint, Offset(w * 0.14f, h * 0.56f), Size(w * 0.30f, h * 0.30f), CornerRadius(3.dp.toPx(), 3.dp.toPx()), stroke)
                drawRoundRect(tint, Offset(w * 0.56f, h * 0.56f), Size(w * 0.30f, h * 0.30f), CornerRadius(3.dp.toPx(), 3.dp.toPx()), stroke)
            }
        }
    }
}
