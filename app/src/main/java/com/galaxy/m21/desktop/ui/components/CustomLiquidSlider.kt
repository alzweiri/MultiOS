package com.galaxy.m21.desktop.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.ui.components.MultiOsIcons.drawMusicNoteGlyph
import com.galaxy.m21.desktop.ui.components.MultiOsIcons.drawSpeakerGlyph
import com.galaxy.m21.desktop.ui.components.MultiOsIcons.drawSunGlyph
import com.galaxy.m21.desktop.ui.shaders.LocalLiquidGlassConfig
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import com.galaxy.m21.desktop.ui.theme.SliderStylePreset
import kotlin.math.roundToInt

enum class SliderGlyphType {
    SPEAKER,
    MUSIC_SPEAKER,
    BRIGHTNESS_SUN,
    THERMAL_LIMIT,
    BASS_WAVE,
    NONE
}

/**
 * MultiOS iOS-Style Elastic Liquid Glass Capsule Slider (Light Mode, Dark Mode & HSV Accent Aware).
 */
@Composable
fun CustomLiquidSlider(
    title: String = "",
    valueText: String = "",
    progress: Float,
    accentColor: Color,
    style: SliderStylePreset,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false,
    isBrightnessGlyph: Boolean = false,
    glyphType: SliderGlyphType? = null,
    barHeight: Dp = 36.dp
) {
    val view = LocalView.current
    val glassConfig = LocalLiquidGlassConfig.current
    val themeColors = LocalMultiOsColors.current
    val isLight = themeColors.isLight
    val currentOnChange by rememberUpdatedState(onProgressChange)
    val clamped = progress.coerceIn(0f, 1f)
    var lastHapticTick by remember { mutableIntStateOf((clamped * 20).roundToInt()) }

    var isPressed by remember { mutableStateOf(false) }
    var overscrollStretch by remember { mutableFloatStateOf(0f) }

    val scaleYAnim by animateFloatAsState(
        targetValue = if (isPressed) 1.09f else 1.0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
        label = "iOSSliderSwellY"
    )
    val scaleXAnim by animateFloatAsState(
        targetValue = (if (isPressed) 1.02f else 1.0f) + kotlin.math.abs(overscrollStretch) * 0.045f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "iOSSliderStretchX"
    )
    val translateXAnim by animateFloatAsState(
        targetValue = overscrollStretch * 10f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 380f),
        label = "iOSSliderRubberX"
    )

    val effectiveGlyph = glyphType ?: if (isBrightnessGlyph) SliderGlyphType.BRIGHTNESS_SUN else SliderGlyphType.SPEAKER
    val tintedFill = lerp(accentColor, glassConfig.tintColor, (glassConfig.tintStrength * 0.55f).coerceIn(0f, 0.45f))

    val rimBrush = remember(glassConfig, isLight) {
        if (isLight) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0F172A).copy(alpha = 0.16f),
                    Color(0xFF0F172A).copy(alpha = 0.08f)
                )
            )
        } else if (!glassConfig.enabled) {
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.10f)))
        } else {
            val rimTint = lerp(Color.White, glassConfig.tintColor, glassConfig.tintStrength)
            Brush.verticalGradient(
                0.0f to rimTint.copy(alpha = 0.44f),
                1.0f to rimTint.copy(alpha = 0.12f)
            )
        }
    }

    fun dispatchNewProgress(rawRatio: Float) {
        if (rawRatio < 0f) {
            overscrollStretch = (rawRatio * 0.35f).coerceAtLeast(-0.6f)
        } else if (rawRatio > 1f) {
            overscrollStretch = ((rawRatio - 1f) * 0.35f).coerceAtMost(0.6f)
        } else {
            overscrollStretch = 0f
        }
        val next = rawRatio.coerceIn(0f, 1f)
        val step = (next * 24).roundToInt()
        if (step != lastHapticTick) {
            lastHapticTick = step
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        currentOnChange(next)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showLabels && title.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = themeColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
                if (valueText.isNotEmpty()) {
                    Text(
                        text = valueText,
                        color = themeColors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        val cornerDp = barHeight / 2
        val bgColor = when {
            isLight && glassConfig.enabled -> Color(0xFF0F172A).copy(alpha = 0.08f)
            isLight -> Color(0xFFE2E8F0)
            glassConfig.enabled -> lerp(Color.White, glassConfig.tintColor, glassConfig.tintStrength).copy(alpha = 0.085f)
            else -> Color(0xFF171B24)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .graphicsLayer {
                    scaleX = scaleXAnim
                    scaleY = scaleYAnim
                    translationX = translateXAnim
                }
                .clip(RoundedCornerShape(cornerDp))
                .background(bgColor)
                .border(0.8.dp, rimBrush, RoundedCornerShape(cornerDp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                            overscrollStretch = 0f
                        },
                        onTap = { offset ->
                            if (size.width > 0) {
                                dispatchNewProgress(offset.x / size.width.toFloat())
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { isPressed = true },
                        onDragEnd = {
                            isPressed = false
                            overscrollStretch = 0f
                        },
                        onDragCancel = {
                            isPressed = false
                            overscrollStretch = 0f
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (size.width > 0) {
                                dispatchNewProgress(change.position.x / size.width.toFloat())
                            }
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val inactiveTrack = if (isLight) Color(0xFF0F172A).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.14f)
            val contrastOnFill = if (tintedFill.luminance() > 0.50f) Color(0xFF0A0D14) else Color.White

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
            ) {
                val w = size.width
                val h = size.height
                val activeW = (w * clamped).coerceAtLeast(if (clamped > 0.01f) h * 0.42f else 0f)

                when (style) {
                    SliderStylePreset.MINIMAL_HAIRLINE -> {
                        val midY = h / 2f
                        drawLine(
                            color = inactiveTrack,
                            start = Offset(16.dp.toPx(), midY),
                            end = Offset(w - 16.dp.toPx(), midY),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = tintedFill,
                            start = Offset(16.dp.toPx(), midY),
                            end = Offset(activeW.coerceIn(16.dp.toPx(), w - 16.dp.toPx()), midY),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = tintedFill,
                            radius = 5.5.dp.toPx(),
                            center = Offset(activeW.coerceIn(16.dp.toPx(), w - 16.dp.toPx()), midY)
                        )
                    }

                    SliderStylePreset.STUDIO_TICKS -> {
                        val ticks = 22
                        for (i in 0 until ticks) {
                            val ratio = i.toFloat() / (ticks - 1)
                            val x = 14.dp.toPx() + ratio * (w - 28.dp.toPx())
                            val lit = ratio <= clamped
                            drawLine(
                                color = if (lit) tintedFill else inactiveTrack,
                                start = Offset(x, h * 0.28f),
                                end = Offset(x, h * 0.72f),
                                strokeWidth = 2.4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    else -> {
                        if (activeW > 1f) {
                            val endHighlight = if (isLight) tintedFill else lerp(tintedFill, Color.White, 0.35f)
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        tintedFill.copy(alpha = 0.86f),
                                        endHighlight.copy(alpha = 0.96f)
                                    ),
                                    endX = activeW
                                ),
                                size = Size(activeW, h),
                                cornerRadius = CornerRadius(h / 2f, h / 2f)
                            )
                        }

                        val iconBox = 14.dp.toPx()
                        val leftZoneThreshold = if (effectiveGlyph == SliderGlyphType.MUSIC_SPEAKER) 40.dp.toPx() else 26.dp.toPx()
                        val glyphTint = if (activeW > leftZoneThreshold) {
                            contrastOnFill.copy(alpha = 0.86f)
                        } else {
                            themeColors.iconTint.copy(alpha = 0.78f)
                        }

                        when (effectiveGlyph) {
                            SliderGlyphType.BRIGHTNESS_SUN -> {
                                drawSunGlyph(
                                    tint = glyphTint,
                                    centerOverride = Offset(19.dp.toPx(), h / 2f),
                                    boxSizePx = iconBox
                                )
                            }
                            SliderGlyphType.SPEAKER -> {
                                drawSpeakerGlyph(
                                    tint = glyphTint,
                                    level = clamped,
                                    centerOverride = Offset(19.dp.toPx(), h / 2f),
                                    boxSizePx = iconBox
                                )
                            }
                            SliderGlyphType.MUSIC_SPEAKER -> {
                                drawMusicNoteGlyph(
                                    tint = glyphTint,
                                    centerOverride = Offset(16.dp.toPx(), h / 2f),
                                    boxSizePx = 12.5.dp.toPx()
                                )
                                drawSpeakerGlyph(
                                    tint = glyphTint,
                                    level = clamped,
                                    centerOverride = Offset(31.dp.toPx(), h / 2f),
                                    boxSizePx = 13.dp.toPx()
                                )
                            }
                            SliderGlyphType.THERMAL_LIMIT -> {
                                val c = Offset(18.dp.toPx(), h / 2f)
                                drawCircle(glyphTint, radius = 3.2.dp.toPx(), center = Offset(c.x, c.y + 3.dp.toPx()))
                                drawLine(
                                    color = glyphTint,
                                    start = Offset(c.x, c.y - 5.5.dp.toPx()),
                                    end = Offset(c.x, c.y + 3.dp.toPx()),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            SliderGlyphType.BASS_WAVE -> {
                                val baseX = 13.dp.toPx()
                                val heights = floatArrayOf(0.25f, 0.52f, 0.38f)
                                heights.forEachIndexed { idx, frac ->
                                    val x = baseX + idx * 4.5.dp.toPx()
                                    val bh = h * frac
                                    drawLine(
                                        color = glyphTint,
                                        start = Offset(x, (h - bh) / 2f),
                                        end = Offset(x, (h + bh) / 2f),
                                        strokeWidth = 1.8.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                            SliderGlyphType.NONE -> Unit
                        }
                    }
                }
            }
        }
    }
}
