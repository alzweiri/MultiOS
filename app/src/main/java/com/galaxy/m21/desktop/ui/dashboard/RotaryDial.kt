package com.galaxy.m21.desktop.ui.dashboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.hardware.KnobParameter
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.shaders.LocalLiquidGlassConfig
import com.galaxy.m21.desktop.ui.theme.KnobStylePreset
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * MultiOS Sculptural Hardware Rotary Knob (Light Mode, Dark Mode & Custom HSV Accent Aware).
 */
@Composable
fun RotaryDial(
    parameter: KnobParameter,
    valuePercent: Float,
    accentColor: Color,
    knobStyle: KnobStylePreset = KnobStylePreset.LIQUID_GLASS,
    editMode: Boolean = false,
    dialSize: Dp = 132.dp,
    onValueChange: (delta: Float) -> Unit,
    onCycleAssignment: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val glassConfig = LocalLiquidGlassConfig.current
    val themeColors = LocalMultiOsColors.current
    val isLight = themeColors.isLight
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val clampedPercent = valuePercent.coerceIn(0f, 1f)

    val animatedRotation = remember { Animatable((clampedPercent * 270f) - 135f) }
    var lastAngle by remember { mutableFloatStateOf(0f) }
    var lastDetentStep by remember { mutableIntStateOf((clampedPercent * 24).roundToInt()) }

    var isTouching by remember { mutableStateOf(false) }
    var touchTiltX by remember { mutableFloatStateOf(0f) }
    var touchTiltY by remember { mutableFloatStateOf(0f) }

    val swellScale by animateFloatAsState(
        targetValue = if (isTouching) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 420f),
        label = "KnobSwell"
    )
    val animTiltX by animateFloatAsState(
        targetValue = if (isTouching) touchTiltX else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 360f),
        label = "KnobTiltX"
    )
    val animTiltY by animateFloatAsState(
        targetValue = if (isTouching) touchTiltY else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 360f),
        label = "KnobTiltY"
    )

    LaunchedEffect(clampedPercent) {
        val currentStep = (clampedPercent * 24).roundToInt()
        if (currentStep != lastDetentStep) {
            lastDetentStep = currentStep
            if (isTouching) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
        animatedRotation.animateTo(
            targetValue = (clampedPercent * 270f) - 135f,
            animationSpec = spring(dampingRatio = 0.74f, stiffness = 420f)
        )
    }

    val tintedBase = lerp(if (isLight) Color(0xFF0F172A) else Color.White, glassConfig.tintColor, glassConfig.tintStrength)
    val tintedAccent = lerp(accentColor, glassConfig.tintColor, (glassConfig.tintStrength * 0.6f).coerceIn(0f, 0.45f))

    val outerRimBrush = remember(tintedBase, glassConfig.enabled, isLight) {
        if (isLight) {
            Brush.linearGradient(
                0.0f to Color.White.copy(alpha = 0.90f),
                0.5f to Color(0xFF64748B).copy(alpha = 0.22f),
                1.0f to Color.White.copy(alpha = 0.65f)
            )
        } else {
            Brush.linearGradient(
                0.0f to tintedBase.copy(alpha = if (glassConfig.enabled) 0.44f else 0.16f),
                0.40f to tintedBase.copy(alpha = 0.08f),
                1.0f to tintedBase.copy(alpha = if (glassConfig.enabled) 0.24f else 0.10f)
            )
        }
    }

    val innerCapRimBrush = remember(tintedBase, glassConfig.enabled, isLight) {
        if (isLight) {
            Brush.linearGradient(
                0.0f to Color.White,
                0.45f to Color(0xFF94A3B8).copy(alpha = 0.32f),
                1.0f to Color.White.copy(alpha = 0.75f)
            )
        } else {
            Brush.linearGradient(
                0.0f to tintedBase.copy(alpha = if (glassConfig.enabled) 0.72f else 0.22f),
                0.35f to tintedBase.copy(alpha = 0.16f),
                1.0f to tintedBase.copy(alpha = if (glassConfig.enabled) 0.38f else 0.12f)
            )
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = editMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(themeColors.subtleFill)
                        .border(0.75.dp, themeColors.subtleBorder, RoundedCornerShape(50))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onCycleAssignment()
                        }
                        .padding(horizontal = 11.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = parameter.displayTitle,
                        color = themeColors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
            }
        }

        val innerPuckSize = dialSize * 0.72f

        Box(
            modifier = Modifier
                .size(dialSize)
                .graphicsLayer {
                    scaleX = swellScale
                    scaleY = swellScale
                    rotationX = animTiltX
                    rotationY = animTiltY
                    cameraDistance = 14f * density.density
                }
                .then(
                    if (glassConfig.enabled && knobStyle != KnobStylePreset.MINIMAL_HALO) {
                        Modifier.shadow(
                            elevation = if (isLight) 12.dp else 18.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = if (isLight) 0.12f else 0.50f),
                            spotColor = Color.Black.copy(alpha = if (isLight) 0.18f else 0.62f)
                        )
                    } else Modifier
                )
                .clip(CircleShape)
                .background(
                    when {
                        knobStyle == KnobStylePreset.MINIMAL_HALO -> Color.Transparent
                        knobStyle == KnobStylePreset.RECESSED_WELL -> if (isLight) Color(0xFFDFE6F0) else Color(0xFF06080D).copy(alpha = 0.85f)
                        glassConfig.enabled -> if (isLight) Color.White.copy(alpha = 0.45f) else tintedBase.copy(alpha = 0.05f)
                        else -> if (isLight) Color(0xFFF1F5F9) else Color(0xFF12161F)
                    }
                )
                .then(
                    if (knobStyle != KnobStylePreset.MINIMAL_HALO && knobStyle != KnobStylePreset.MONOLITH_DOT) {
                        Modifier.border(0.85.dp, outerRimBrush, CircleShape)
                    } else Modifier
                )
                .pointerInput(editMode) {
                    if (!editMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isTouching = true
                                val center = Offset(size.width / 2f, size.height / 2f)
                                lastAngle = computePolarAngle(offset, center)
                                touchTiltY = ((offset.x - center.x) / center.x) * 9f
                                touchTiltX = -((offset.y - center.y) / center.y) * 9f
                            },
                            onDragEnd = { isTouching = false },
                            onDragCancel = { isTouching = false },
                            onDrag = { change, _ ->
                                change.consume()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                touchTiltY = ((change.position.x - center.x) / center.x).coerceIn(-1f, 1f) * 10f
                                touchTiltX = -((change.position.y - center.y) / center.y).coerceIn(-1f, 1f) * 10f

                                val currentAngle = computePolarAngle(change.position, center)
                                var angleDiff = currentAngle - lastAngle
                                if (angleDiff > 180f) angleDiff -= 360f
                                if (angleDiff < -180f) angleDiff += 360f

                                val deltaNormalized = (angleDiff / 270f).coerceIn(-0.25f, 0.25f)
                                if (abs(deltaNormalized) > 0.001f) {
                                    currentOnValueChange(deltaNormalized)
                                    lastAngle = currentAngle
                                }
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val inactiveTrack = if (isLight) Color(0xFF0F172A).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.10f)

            Canvas(modifier = Modifier.size(dialSize * 0.90f)) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)

                when (knobStyle) {
                    KnobStylePreset.DIETER_RAMS -> {
                        for (i in 0..20) {
                            val deg = 135.0 + (i / 20.0) * 270.0
                            val rad = deg * (PI / 180.0)
                            val lit = (i / 20f) <= clampedPercent
                            drawLine(
                                color = if (lit) tintedAccent else inactiveTrack,
                                start = Offset(c.x + (r * 0.88f * cos(rad)).toFloat(), c.y + (r * 0.88f * sin(rad)).toFloat()),
                                end = Offset(c.x + (r * 0.98f * cos(rad)).toFloat(), c.y + (r * 0.98f * sin(rad)).toFloat()),
                                strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    KnobStylePreset.KNURLED_TITANIUM -> {
                        for (i in 0 until 36) {
                            val rad = (i * 10.0) * (PI / 180.0)
                            drawLine(
                                color = inactiveTrack,
                                start = Offset(c.x + (r * 0.84f * cos(rad)).toFloat(), c.y + (r * 0.84f * sin(rad)).toFloat()),
                                end = Offset(c.x + (r * 0.95f * cos(rad)).toFloat(), c.y + (r * 0.95f * sin(rad)).toFloat()),
                                strokeWidth = 1.4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        if (clampedPercent > 0.01f) {
                            drawArc(
                                color = tintedAccent,
                                startAngle = 135f,
                                sweepAngle = clampedPercent * 270f,
                                useCenter = false,
                                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    KnobStylePreset.CONCENTRIC_ORBIT -> {
                        drawCircle(inactiveTrack, radius = r * 0.94f, style = Stroke(width = 1.5.dp.toPx()))
                        drawCircle(inactiveTrack, radius = r * 0.78f, style = Stroke(width = 1.dp.toPx()))
                        if (clampedPercent > 0.01f) {
                            drawArc(
                                color = tintedAccent,
                                startAngle = 135f,
                                sweepAngle = clampedPercent * 270f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    KnobStylePreset.MONOLITH_DOT -> Unit

                    else -> {
                        drawArc(
                            color = inactiveTrack,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        if (clampedPercent > 0.01f) {
                            drawArc(
                                color = tintedAccent,
                                startAngle = 135f,
                                sweepAngle = clampedPercent * 270f,
                                useCenter = false,
                                style = Stroke(
                                    width = if (knobStyle == KnobStylePreset.MINIMAL_HALO) 2.4.dp.toPx() else 3.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                        if (knobStyle == KnobStylePreset.MINIMAL_HALO) {
                            val orbAngle = (135.0 + clampedPercent * 270.0) * (PI / 180.0)
                            drawCircle(
                                color = tintedAccent,
                                radius = 5.dp.toPx(),
                                center = Offset(c.x + (r * cos(orbAngle)).toFloat(), c.y + (r * sin(orbAngle)).toFloat())
                            )
                        }
                    }
                }
            }

            if (knobStyle != KnobStylePreset.MINIMAL_HALO) {
                val capBackground = when {
                    isLight && glassConfig.enabled -> lerp(Color.White, glassConfig.tintColor, glassConfig.tintStrength * 0.35f).copy(alpha = 0.78f)
                    isLight -> Color(0xFFFFFFFF)
                    knobStyle == KnobStylePreset.DIETER_RAMS -> Color(0xFF1A1D24)
                    knobStyle == KnobStylePreset.FROSTED_MATTE -> lerp(Color(0xFF1E2430), glassConfig.tintColor, glassConfig.tintStrength * 0.4f).copy(alpha = 0.86f)
                    knobStyle == KnobStylePreset.MONOLITH_DOT -> Color(0xFF0C0F16).copy(alpha = 0.92f)
                    glassConfig.enabled -> lerp(Color(0xFF121620), glassConfig.tintColor, glassConfig.tintStrength * 0.4f).copy(alpha = 0.44f)
                    else -> Color(0xFF181C26)
                }

                Box(
                    modifier = Modifier
                        .size(innerPuckSize)
                        .shadow(
                            elevation = if (knobStyle == KnobStylePreset.RECESSED_WELL) 4.dp else 10.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = if (isLight) 0.14f else 0.55f),
                            spotColor = Color.Black.copy(alpha = if (isLight) 0.20f else 0.68f)
                        )
                        .clip(CircleShape)
                        .background(capBackground)
                        .border(
                            width = if (knobStyle == KnobStylePreset.MONOLITH_DOT) 0.5.dp else 1.1.dp,
                            brush = innerCapRimBrush,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (glassConfig.enabled && (knobStyle == KnobStylePreset.LIQUID_GLASS || knobStyle == KnobStylePreset.RECESSED_WELL || knobStyle == KnobStylePreset.CONCENTRIC_ORBIT)) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isLight) 0.75f else 0.34f),
                                        Color.White.copy(alpha = if (isLight) 0.25f else 0.09f),
                                        Color.Transparent
                                    ),
                                    center = Offset(w * 0.30f, h * 0.24f),
                                    radius = w * 0.58f
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .rotate(animatedRotation.value),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (knobStyle) {
                            KnobStylePreset.FROSTED_MATTE,
                            KnobStylePreset.MONOLITH_DOT -> {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 9.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(tintedAccent)
                                )
                            }
                            KnobStylePreset.DIETER_RAMS -> {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(width = 2.5.dp, height = 18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF6B4A))
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp)
                                        .size(width = 3.8.dp, height = 12.dp)
                                        .clip(CircleShape)
                                        .background(if (isLight) tintedAccent else Color.White.copy(alpha = 0.94f))
                                )
                            }
                        }
                    }

                    KnobParameterIcon(parameter = parameter, progress = clampedPercent, tint = themeColors.iconTint)
                }
            } else {
                KnobParameterIcon(parameter = parameter, progress = clampedPercent, tint = themeColors.iconTint)
            }
        }
    }
}

@Composable
private fun KnobParameterIcon(
    parameter: KnobParameter,
    progress: Float,
    tint: Color
) {
    when (parameter) {
        KnobParameter.DISPLAY_BRIGHTNESS -> MultiOsIcons.BrightnessSun(Modifier.size(20.dp), tint)
        KnobParameter.SYSTEM_AUDIO -> MultiOsIcons.Speaker(Modifier.size(20.dp), tint, level = progress)
        KnobParameter.MUSIC_VOLUME -> MultiOsIcons.MusicVolume(iconSize = 15.dp, tint = tint, level = progress)
        KnobParameter.CPU_SPEED -> MultiOsIcons.CpuSpeed(Modifier.size(20.dp), tint)
        KnobParameter.DJ_VINYL_SCRATCH -> MultiOsIcons.VinylDisc(Modifier.size(20.dp), tint)
        KnobParameter.DJ_BASS_FILTER -> MultiOsIcons.BassWave(Modifier.size(20.dp), tint)
    }
}

private fun computePolarAngle(touch: Offset, center: Offset): Float {
    val dx = touch.x - center.x
    val dy = touch.y - center.y
    val rad = atan2(dy, dx)
    var deg = (rad * (180f / PI)).toFloat()
    if (deg < 0f) deg += 360f
    return deg
}
