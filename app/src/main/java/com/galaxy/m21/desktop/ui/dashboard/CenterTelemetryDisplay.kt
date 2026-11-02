package com.galaxy.m21.desktop.ui.dashboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.hardware.KnobParameter
import com.galaxy.m21.desktop.hardware.SystemTelemetryData
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.shaders.LocalLiquidGlassConfig
import com.galaxy.m21.desktop.ui.theme.ClockStylePreset
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * MultiOS Customizable Clock & Icon Telemetry Core (Light Mode, Dark Mode & HSV Accent Aware).
 */
@Composable
fun CenterTelemetryDisplay(
    telemetry: SystemTelemetryData,
    activeKnob: KnobParameter?,
    editMode: Boolean = false,
    onCycleClockStyle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val glassConfig = LocalLiquidGlassConfig.current
    val themeColors = LocalMultiOsColors.current
    val isLight = themeColors.isLight

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(60L)
        }
    }

    val tintedRim = lerp(if (isLight) Color.White else Color.White, glassConfig.tintColor, glassConfig.tintStrength)
    val rimBrush = remember(tintedRim, glassConfig.enabled, isLight) {
        if (isLight) {
            Brush.linearGradient(
                0.0f to Color.White,
                0.5f to Color(0xFF94A3B8).copy(alpha = 0.28f),
                1.0f to Color.White.copy(alpha = 0.80f)
            )
        } else if (!glassConfig.enabled) {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.08f)))
        } else {
            Brush.linearGradient(
                0.0f to tintedRim.copy(alpha = 0.62f),
                0.38f to tintedRim.copy(alpha = 0.15f),
                0.75f to tintedRim.copy(alpha = 0.06f),
                1.0f to tintedRim.copy(alpha = 0.34f)
            )
        }
    }

    val containerShape = when (telemetry.clockStyle) {
        ClockStylePreset.APPLE_ANALOG,
        ClockStylePreset.XIAOMI_MAGAZINE,
        ClockStylePreset.APPLE_IOS_BOLD -> RoundedCornerShape(38.dp)
        else -> CircleShape
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = editMode, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(themeColors.subtleFill)
                        .border(0.75.dp, themeColors.subtleBorder, RoundedCornerShape(50))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onCycleClockStyle()
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = telemetry.clockStyle.title,
                        color = themeColors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        val substrate = when {
            isLight && glassConfig.enabled -> lerp(Color.White, glassConfig.tintColor, glassConfig.tintStrength * 0.3f).copy(alpha = 0.72f)
            isLight -> Color.White.copy(alpha = 0.96f)
            glassConfig.enabled -> lerp(Color(0xFF0E121B), glassConfig.tintColor, glassConfig.tintStrength * 0.35f).copy(alpha = 0.42f)
            else -> Color(0xFF141821).copy(alpha = 0.94f)
        }

        Box(
            modifier = modifier
                .then(
                    if (glassConfig.enabled) {
                        Modifier.shadow(
                            elevation = if (isLight) 14.dp else 22.dp,
                            shape = containerShape,
                            ambientColor = Color.Black.copy(alpha = if (isLight) 0.12f else 0.50f),
                            spotColor = Color.Black.copy(alpha = if (isLight) 0.18f else 0.62f)
                        )
                    } else Modifier
                )
                .clip(containerShape)
                .background(substrate)
                .border(1.dp, rimBrush, containerShape)
                .then(
                    if (editMode) {
                        Modifier.clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onCycleClockStyle()
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (glassConfig.enabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isLight) 0.65f else 0.22f),
                                Color.White.copy(alpha = if (isLight) 0.18f else 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.28f, h * 0.22f),
                            radius = w * 0.64f
                        )
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = activeKnob,
                    transitionSpec = {
                        (fadeIn(spring(stiffness = 420f)) + scaleIn(initialScale = 0.90f)) togetherWith
                            (fadeOut(spring(stiffness = 420f)) + scaleOut(targetScale = 0.92f))
                    },
                    label = "ClockKnobCrossfade"
                ) { knobState ->
                    if (knobState != null) {
                        ActiveIconKnobReadout(knobState, telemetry, themeColors.textPrimary, themeColors.iconTint)
                    } else {
                        MultiOsClockRenderer(
                            style = telemetry.clockStyle,
                            nowMillis = nowMillis,
                            primaryInk = themeColors.textPrimary,
                            accent = themeColors.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiOsClockRenderer(
    style: ClockStylePreset,
    nowMillis: Long,
    primaryInk: Color,
    accent: Color
) {
    val cal = remember(nowMillis / 60L) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val hour12 = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val millis = cal.get(Calendar.MILLISECOND)

    val smoothSeconds = second + (millis / 1000f)
    val smoothMinutes = minute + (smoothSeconds / 60f)
    val smoothHours = (hour12 % 12) + (smoothMinutes / 60f)

    when (style) {
        ClockStylePreset.SWISS_ANALOG -> {
            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f

                for (i in 0 until 60) {
                    val angleRad = (i * 6.0 - 90.0) * (PI / 180.0)
                    val isMajor = i % 5 == 0
                    val innerR = if (isMajor) r * 0.80f else r * 0.90f
                    val outerR = r * 0.96f
                    drawLine(
                        color = if (isMajor) primaryInk.copy(alpha = 0.86f) else primaryInk.copy(alpha = 0.24f),
                        start = Offset(c.x + (innerR * cos(angleRad)).toFloat(), c.y + (innerR * sin(angleRad)).toFloat()),
                        end = Offset(c.x + (outerR * cos(angleRad)).toFloat(), c.y + (outerR * sin(angleRad)).toFloat()),
                        strokeWidth = if (isMajor) 2.2.dp.toPx() else 0.9.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                val hourRad = (smoothHours * 30.0 - 90.0) * (PI / 180.0)
                drawLine(
                    color = primaryInk,
                    start = c,
                    end = Offset(c.x + (r * 0.50f * cos(hourRad)).toFloat(), c.y + (r * 0.50f * sin(hourRad)).toFloat()),
                    strokeWidth = 3.8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val minRad = (smoothMinutes * 6.0 - 90.0) * (PI / 180.0)
                drawLine(
                    color = primaryInk.copy(alpha = 0.92f),
                    start = c,
                    end = Offset(c.x + (r * 0.74f * cos(minRad)).toFloat(), c.y + (r * 0.74f * sin(minRad)).toFloat()),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val secRad = (smoothSeconds * 6.0 - 90.0) * (PI / 180.0)
                drawLine(
                    color = Color(0xFFFF6B4A),
                    start = Offset(c.x - (r * 0.16f * cos(secRad)).toFloat(), c.y - (r * 0.16f * sin(secRad)).toFloat()),
                    end = Offset(c.x + (r * 0.82f * cos(secRad)).toFloat(), c.y + (r * 0.82f * sin(secRad)).toFloat()),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(color = primaryInk, radius = 4.dp.toPx(), center = c)
            }
        }

        ClockStylePreset.APPLE_ANALOG -> {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f

                for (i in 0 until 12) {
                    val angleRad = (i * 30.0 - 90.0) * (PI / 180.0)
                    val isCardinal = i % 3 == 0
                    val dotPos = Offset(
                        c.x + (r * 0.86f * cos(angleRad)).toFloat(),
                        c.y + (r * 0.86f * sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = if (isCardinal) primaryInk.copy(alpha = 0.85f) else primaryInk.copy(alpha = 0.25f),
                        radius = if (isCardinal) 2.5.dp.toPx() else 1.3.dp.toPx(),
                        center = dotPos
                    )
                }

                val hRad = (smoothHours * 30.0 - 90.0) * (PI / 180.0)
                drawLine(
                    color = primaryInk.copy(alpha = 0.55f),
                    start = c,
                    end = Offset(c.x + (r * 0.20f * cos(hRad)).toFloat(), c.y + (r * 0.20f * sin(hRad)).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryInk,
                    start = Offset(c.x + (r * 0.18f * cos(hRad)).toFloat(), c.y + (r * 0.18f * sin(hRad)).toFloat()),
                    end = Offset(c.x + (r * 0.50f * cos(hRad)).toFloat(), c.y + (r * 0.50f * sin(hRad)).toFloat()),
                    strokeWidth = 5.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val mRad = (smoothMinutes * 6.0 - 90.0) * (PI / 180.0)
                drawLine(
                    color = primaryInk.copy(alpha = 0.55f),
                    start = c,
                    end = Offset(c.x + (r * 0.22f * cos(mRad)).toFloat(), c.y + (r * 0.22f * sin(mRad)).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(c.x + (r * 0.20f * cos(mRad)).toFloat(), c.y + (r * 0.20f * sin(mRad)).toFloat()),
                    end = Offset(c.x + (r * 0.74f * cos(mRad)).toFloat(), c.y + (r * 0.74f * sin(mRad)).toFloat()),
                    strokeWidth = 4.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val sRad = (smoothSeconds * 6.0 - 90.0) * (PI / 180.0)
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = Offset(c.x - (r * 0.14f * cos(sRad)).toFloat(), c.y - (r * 0.14f * sin(sRad)).toFloat()),
                    end = Offset(c.x + (r * 0.80f * cos(sRad)).toFloat(), c.y + (r * 0.80f * sin(sRad)).toFloat()),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = primaryInk, radius = 3.8.dp.toPx(), center = c)
            }
        }

        ClockStylePreset.APPLE_IOS_BOLD -> {
            val hh = String.format(Locale.US, "%02d", if (hour12 == 0) 12 else hour12)
            val mm = String.format(Locale.US, "%02d", minute)
            val digitBrush = Brush.verticalGradient(
                colors = listOf(primaryInk, accent)
            )
            Text(
                text = "$hh:$mm",
                style = TextStyle(
                    brush = digitBrush,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-2).sp
                )
            )
        }

        ClockStylePreset.XIAOMI_MAGAZINE -> {
            val hh = String.format(Locale.US, "%02d", hour24)
            val mm = String.format(Locale.US, "%02d", minute)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = hh,
                    color = primaryInk,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp,
                    lineHeight = 40.sp
                )
                Box(
                    modifier = Modifier
                        .padding(vertical = 3.dp)
                        .width(26.dp)
                        .height(1.5.dp)
                        .background(accent.copy(alpha = 0.65f))
                )
                Text(
                    text = mm,
                    color = accent,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp,
                    lineHeight = 40.sp
                )
            }
        }

        ClockStylePreset.XIAOMI_MINIMAL -> {
            val hh = String.format(Locale.US, "%02d", hour24)
            val mm = String.format(Locale.US, "%02d", minute)
            Text(
                text = "$hh·$mm",
                color = primaryInk,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraLight,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp
            )
        }

        ClockStylePreset.ABSTRACT_ORBIT -> {
            Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val outerR = size.minDimension * 0.44f
                val innerR = size.minDimension * 0.28f

                drawCircle(primaryInk.copy(alpha = 0.12f), radius = outerR, center = c, style = Stroke(width = 3.dp.toPx()))
                drawCircle(primaryInk.copy(alpha = 0.12f), radius = innerR, center = c, style = Stroke(width = 4.dp.toPx()))

                val minSweep = (smoothMinutes / 60f) * 360f
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = minSweep,
                    useCenter = false,
                    topLeft = Offset(c.x - outerR, c.y - outerR),
                    size = Size(outerR * 2f, outerR * 2f),
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )
                val minRad = (minSweep - 90.0) * (PI / 180.0)
                drawCircle(
                    color = primaryInk,
                    radius = 4.5.dp.toPx(),
                    center = Offset(c.x + (outerR * cos(minRad)).toFloat(), c.y + (outerR * sin(minRad)).toFloat())
                )

                val hrSweep = (smoothHours / 12f) * 360f
                drawArc(
                    color = primaryInk,
                    startAngle = -90f,
                    sweepAngle = hrSweep,
                    useCenter = false,
                    topLeft = Offset(c.x - innerR, c.y - innerR),
                    size = Size(innerR * 2f, innerR * 2f),
                    style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun ActiveIconKnobReadout(
    knob: KnobParameter,
    telemetry: SystemTelemetryData,
    textColor: Color,
    iconTint: Color
) {
    val displayVal = when (knob) {
        KnobParameter.CPU_SPEED -> String.format(Locale.US, "%.2f", telemetry.cpuBigFreqGhz)
        KnobParameter.DISPLAY_BRIGHTNESS -> "${(telemetry.brightnessLevel * 100).roundToInt()}"
        KnobParameter.SYSTEM_AUDIO -> "${(telemetry.audioProgress * 100).roundToInt()}"
        KnobParameter.MUSIC_VOLUME -> "${(telemetry.musicVolumeProgress * 100).roundToInt()}"
        KnobParameter.DJ_VINYL_SCRATCH -> "${(telemetry.cdTrackProgress * 100).roundToInt()}"
        KnobParameter.DJ_BASS_FILTER -> "${(telemetry.djFilterCutoff * 100).roundToInt()}"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (knob) {
            KnobParameter.DISPLAY_BRIGHTNESS -> MultiOsIcons.BrightnessSun(Modifier.size(22.dp), iconTint)
            KnobParameter.SYSTEM_AUDIO -> MultiOsIcons.Speaker(Modifier.size(22.dp), iconTint, telemetry.audioProgress)
            KnobParameter.MUSIC_VOLUME -> MultiOsIcons.MusicVolume(iconSize = 18.dp, tint = iconTint, level = telemetry.musicVolumeProgress)
            KnobParameter.CPU_SPEED -> MultiOsIcons.CpuSpeed(Modifier.size(22.dp), iconTint)
            KnobParameter.DJ_VINYL_SCRATCH -> MultiOsIcons.VinylDisc(Modifier.size(22.dp), iconTint)
            KnobParameter.DJ_BASS_FILTER -> MultiOsIcons.BassWave(Modifier.size(22.dp), iconTint)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = displayVal,
            color = textColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.SansSerif
        )
    }
}
