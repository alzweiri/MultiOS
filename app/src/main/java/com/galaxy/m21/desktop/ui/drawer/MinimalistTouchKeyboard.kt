package com.galaxy.m21.desktop.ui.drawer

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.shaders.LocalLiquidGlassConfig
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlin.math.roundToInt

/**
 * MultiOS Adaptive Liquid Glass Keyboard:
 *
 * - **Zero-Gap Contiguous Touch Hitboxes (`0.dp` spacing between touch cells)**:
 *   Every key's touch target extends flush to its neighbors so there is ZERO dead space between keys,
 *   while an inner visual pad renders clean separation!
 * - **Invisible Key Borders**:
 *   Borderless soft-glass key surfaces matching modern flagship keyboards for ultra-clean reliability.
 * - Supports 4-Side Resizable Full Screen Keyboard & Freeform Floating Keyboard modes!
 */
@Composable
fun MinimalistTouchKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEnter: () -> Unit = {},
    heightScale: Float = 1.0f,
    isFullLandscapeGboard: Boolean = false,
    showOuterGlassFrame: Boolean = true,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val glassConfig = LocalLiquidGlassConfig.current
    val themeColors = LocalMultiOsColors.current
    val isLight = themeColors.isLight
    val clampedScale = heightScale.coerceIn(0.65f, 1.55f)

    val rowNums = remember { listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0") }
    val row1 = remember { listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P") }
    val row2 = remember { listOf("A", "S", "D", "F", "G", "H", "J", "K", "L") }
    val row3 = remember { listOf("Z", "X", "C", "V", "B", "N", "M") }

    val seedWhite = if (isLight) Color(0xFFFFFFFF) else Color.White
    val tintedTop = lerp(seedWhite, glassConfig.tintColor, (glassConfig.tintStrength * 2.0f).coerceIn(0f, 0.70f))

    val keyFillBrush = remember(glassConfig, isLight) {
        if (glassConfig.enabled) {
            if (isLight) {
                Brush.verticalGradient(
                    0.0f to lerp(Color.White, tintedTop, 0.30f).copy(alpha = 0.84f),
                    1.0f to lerp(Color(0xFFF1F5F9), tintedTop, 0.22f).copy(alpha = 0.68f)
                )
            } else {
                Brush.verticalGradient(
                    0.0f to tintedTop.copy(alpha = 0.20f),
                    0.5f to tintedTop.copy(alpha = 0.13f),
                    1.0f to tintedTop.copy(alpha = 0.09f)
                )
            }
        } else {
            if (isLight) {
                Brush.verticalGradient(
                    0.0f to Color(0xFFFFFFFF),
                    1.0f to Color(0xFFE6ECF4)
                )
            } else {
                Brush.verticalGradient(
                    0.0f to Color(0xFF252934),
                    1.0f to Color(0xFF1D212B)
                )
            }
        }
    }

    val modifierKeyFillBrush = remember(glassConfig, isLight) {
        if (glassConfig.enabled) {
            if (isLight) {
                Brush.verticalGradient(
                    0.0f to Color(0xFFE2E8F0).copy(alpha = 0.78f),
                    1.0f to Color(0xFFCBD5E1).copy(alpha = 0.60f)
                )
            } else {
                Brush.verticalGradient(
                    0.0f to tintedTop.copy(alpha = 0.13f),
                    1.0f to tintedTop.copy(alpha = 0.06f)
                )
            }
        } else {
            if (isLight) {
                Brush.verticalGradient(
                    0.0f to Color(0xFFE2E8F0),
                    1.0f to Color(0xFFCBD5E1)
                )
            } else {
                Brush.verticalGradient(
                    0.0f to Color(0xFF1B1F29),
                    1.0f to Color(0xFF151821)
                )
            }
        }
    }

    val returnKeyFillBrush = remember(themeColors.accent) {
        Brush.verticalGradient(
            0.0f to themeColors.accent.copy(alpha = 0.94f),
            1.0f to themeColors.accent.copy(alpha = 0.80f)
        )
    }

    // Cell heights INCLUDE inner visual padding so contiguous touch hitboxes have 0.dp dead zones!
    val numCellH = ((if (isFullLandscapeGboard) 36 else 37) * clampedScale).roundToInt().coerceAtLeast(26)
    val alphaCellH = ((if (isFullLandscapeGboard) 44 else 45) * clampedScale).roundToInt().coerceAtLeast(30)
    val spaceCellH = ((if (isFullLandscapeGboard) 42 else 43) * clampedScale).roundToInt().coerceAtLeast(29)

    val keyTextColor = themeColors.textPrimary
    val returnTextColor = themeColors.onAccent

    val keyboardBody: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isFullLandscapeGboard) 10.dp else 8.dp,
                    vertical = (6f * clampedScale).dp
                ),
            // ZERO gap between rows -> 100% contiguous vertical touch hitboxes!
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            ContiguousLiquidKeyRow(
                keys = rowNums,
                cellHeightDp = numCellH,
                fillBrush = modifierKeyFillBrush,
                textColor = keyTextColor.copy(alpha = 0.88f),
                fontSizeSp = (13f * clampedScale.coerceIn(0.85f, 1.20f)).roundToInt(),
                onKey = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onKeyPress(it)
                }
            )

            ContiguousLiquidKeyRow(
                keys = row1,
                cellHeightDp = alphaCellH,
                fillBrush = keyFillBrush,
                textColor = keyTextColor,
                fontSizeSp = (15f * clampedScale.coerceIn(0.85f, 1.22f)).roundToInt(),
                onKey = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onKeyPress(it)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isFullLandscapeGboard) 22.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                row2.forEach { char ->
                    BorderlessContiguousKeyCap(
                        label = char,
                        cellHeightDp = alphaCellH,
                        fillBrush = keyFillBrush,
                        textColor = keyTextColor,
                        fontSizeSp = (15f * clampedScale.coerceIn(0.85f, 1.22f)).roundToInt(),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onKeyPress(char)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                BorderlessContiguousKeyCap(
                    label = "clear",
                    cellHeightDp = alphaCellH,
                    fontSizeSp = (11f * clampedScale.coerceIn(0.85f, 1.15f)).roundToInt(),
                    fillBrush = modifierKeyFillBrush,
                    textColor = keyTextColor.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1.45f),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onClear()
                    }
                )

                row3.forEach { char ->
                    BorderlessContiguousKeyCap(
                        label = char,
                        cellHeightDp = alphaCellH,
                        fillBrush = keyFillBrush,
                        textColor = keyTextColor,
                        fontSizeSp = (15f * clampedScale.coerceIn(0.85f, 1.22f)).roundToInt(),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onKeyPress(char)
                        }
                    )
                }

                BorderlessContiguousKeyCap(
                    label = "⌫",
                    cellHeightDp = alphaCellH,
                    fontSizeSp = (15f * clampedScale.coerceIn(0.85f, 1.22f)).roundToInt(),
                    fillBrush = modifierKeyFillBrush,
                    textColor = keyTextColor,
                    modifier = Modifier.weight(1.45f),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onBackspace()
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (isFullLandscapeGboard) {
                    BorderlessContiguousKeyCap(
                        label = ",",
                        cellHeightDp = spaceCellH,
                        fontSizeSp = 14,
                        fillBrush = modifierKeyFillBrush,
                        textColor = keyTextColor,
                        modifier = Modifier.weight(0.75f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onKeyPress(",")
                        }
                    )
                }

                BorderlessContiguousKeyCap(
                    label = "space",
                    cellHeightDp = spaceCellH,
                    fontSizeSp = 13,
                    fillBrush = keyFillBrush,
                    textColor = keyTextColor.copy(alpha = 0.90f),
                    modifier = Modifier.weight(if (isFullLandscapeGboard) 4.2f else 3.2f),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onKeyPress(" ")
                    }
                )

                if (isFullLandscapeGboard) {
                    BorderlessContiguousKeyCap(
                        label = ".",
                        cellHeightDp = spaceCellH,
                        fontSizeSp = 14,
                        fillBrush = modifierKeyFillBrush,
                        textColor = keyTextColor,
                        modifier = Modifier.weight(0.75f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onKeyPress(".")
                        }
                    )
                }

                BorderlessContiguousKeyCap(
                    label = "return",
                    cellHeightDp = spaceCellH,
                    fontSizeSp = 13,
                    fillBrush = returnKeyFillBrush,
                    textColor = returnTextColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1.35f),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onEnter()
                    }
                )
            }
        }
    }

    if (showOuterGlassFrame) {
        LiquidGlassBox(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(if (isFullLandscapeGboard) 22.dp else 26.dp),
            borderColor = themeColors.subtleBorder,
            glassAlpha = if (isLight) 0.74f else 0.54f
        ) {
            keyboardBody()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            keyboardBody()
        }
    }
}

@Composable
private fun ContiguousLiquidKeyRow(
    keys: List<String>,
    cellHeightDp: Int,
    fillBrush: Brush,
    textColor: Color,
    fontSizeSp: Int,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        // ZERO gap between keys -> touching between any 2 keys always hits the nearest key!
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        keys.forEach { char ->
            BorderlessContiguousKeyCap(
                label = char,
                cellHeightDp = cellHeightDp,
                fillBrush = fillBrush,
                textColor = textColor,
                fontSizeSp = fontSizeSp,
                modifier = Modifier.weight(1f),
                onClick = { onKey(char) }
            )
        }
    }
}

@Composable
private fun BorderlessContiguousKeyCap(
    label: String,
    cellHeightDp: Int,
    fillBrush: Brush,
    textColor: Color,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 15,
    fontWeight: FontWeight = FontWeight.Normal,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val keyShape = RoundedCornerShape(9.dp)

    // Full contiguous touch cell (0.dp outer gap!)
    Box(
        modifier = modifier
            .height(cellHeightDp.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.4.dp, vertical = 2.4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Invisible-border soft visual key pill
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (isPressed) 0.94f else 1f
                    scaleX = s
                    scaleY = s
                }
                .clip(keyShape)
                .background(fillBrush)
                .then(
                    if (isPressed) {
                        Modifier.background(Color.White.copy(alpha = 0.16f))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = fontSizeSp.sp,
                fontWeight = fontWeight,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

/**
 * Samsung-Style 4-Side Resizable Full Screen Keyboard Frame!
 *
 * Allows stretching/shrinking the Full Screen Keyboard from **ALL 4 SIDES**:
 * - **Top Handle**: Drag up/down to scale keyboard height (`dHeightScale`).
 * - **Bottom Handle**: Drag up/down to lift keyboard up away from Circle-to-Search / bottom globe bar (`dBottomDp`).
 * - **Left Handle**: Drag left/right to inset from the left screen edge (`dLeftDp`).
 * - **Right Handle**: Drag left/right to inset from the right screen edge (`dRightDp`).
 */
@Composable
fun ResizableFourSideKeyboardContainer(
    bottomInsetDp: Float,
    leftInsetDp: Float,
    rightInsetDp: Float,
    resizeActive: Boolean,
    onUpdateBounds: (dHeightScale: Float, dBottomDp: Float, dLeftDp: Float, dRightDp: Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val themeColors = LocalMultiOsColors.current
    val latestUpdate by rememberUpdatedState(onUpdateBounds)

    val handleColor = if (resizeActive) {
        themeColors.accent
    } else {
        themeColors.textSecondary.copy(alpha = 0.36f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = leftInsetDp.coerceAtLeast(0f).dp,
                end = rightInsetDp.coerceAtLeast(0f).dp,
                bottom = bottomInsetDp.coerceAtLeast(0f).dp
            )
            .then(
                if (resizeActive) {
                    Modifier.border(
                        width = 1.4.dp,
                        color = themeColors.accent.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(24.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        content()

        // TOP EDGE RESIZE HANDLE (Drag up to grow taller, down to shrink!)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(if (resizeActive) 84.dp else 54.dp)
                .height(16.dp)
                .pointerInput("top_edge") {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dyDp = with(density) { dragAmount.y.toDp().value }
                        latestUpdate(-dyDp / 185f, 0f, 0f, 0f)
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(if (resizeActive) 56.dp else 36.dp)
                    .height(if (resizeActive) 5.dp else 3.5.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
        }

        // BOTTOM EDGE RESIZE / LIFT HANDLE (Drag up to lift above Circle-to-Search & language bar!)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(if (resizeActive) 84.dp else 54.dp)
                .height(16.dp)
                .pointerInput("bottom_edge") {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dyDp = with(density) { dragAmount.y.toDp().value }
                        latestUpdate(0f, -dyDp, 0f, 0f)
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .width(if (resizeActive) 56.dp else 36.dp)
                    .height(if (resizeActive) 5.dp else 3.5.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
        }

        // LEFT SIDE RESIZE HANDLE (Drag right to narrow, left to widen!)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(16.dp)
                .height(if (resizeActive) 76.dp else 46.dp)
                .pointerInput("left_edge") {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dxDp = with(density) { dragAmount.x.toDp().value }
                        latestUpdate(0f, 0f, dxDp, 0f)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 2.dp)
                    .width(if (resizeActive) 5.dp else 3.5.dp)
                    .height(if (resizeActive) 52.dp else 32.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
        }

        // RIGHT SIDE RESIZE HANDLE (Drag left to narrow, right to widen!)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(16.dp)
                .height(if (resizeActive) 76.dp else 46.dp)
                .pointerInput("right_edge") {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dxDp = with(density) { dragAmount.x.toDp().value }
                        latestUpdate(0f, 0f, 0f, -dxDp)
                    }
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .width(if (resizeActive) 5.dp else 3.5.dp)
                    .height(if (resizeActive) 52.dp else 32.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
        }
    }
}
