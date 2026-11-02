package com.galaxy.m21.desktop.ui.aod

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.hardware.SystemTelemetryData
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.theme.ChargingAnimStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * iOS LTPO-Style Always-On Display (AOD).
 *
 * Dimmed, highly energy-efficient OLED presentation:
 * - Wallpaper is deeply dimmed and blurred behind black overlay.
 * - Every 60 seconds: smoothly fades out (alpha -> 0), shifts to a new burn-in safe position,
 *   and fades back in (alpha -> 1).
 * - Single tap dismisses AOD immediately.
 */
@Composable
fun AlwaysOnDisplayOverlay(
    telemetry: SystemTelemetryData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!telemetry.isScreenAodActive) return

    val clockAlpha = remember { Animatable(1f) }
    var clockOffsetX by remember { mutableFloatStateOf(0f) }
    var clockOffsetY by remember { mutableFloatStateOf(0f) }

    // Every 60 seconds: animate alpha to 0, pick new OLED burn-in offset (-24dp..24dp), animate back to 1
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            clockAlpha.animateTo(0f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
            clockOffsetX = (-24..24).random().toFloat()
            clockOffsetY = (-24..24).random().toFloat()
            clockAlpha.animateTo(1f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.90f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .offset { IntOffset(clockOffsetX.roundToInt(), clockOffsetY.roundToInt()) }
                .alpha(clockAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Minimalist Time
            Text(
                text = telemetry.currentTimeFormatted,
                fontSize = 78.sp,
                fontWeight = FontWeight.ExtraLight,
                fontFamily = FontFamily.SansSerif,
                color = Color.White.copy(alpha = 0.88f),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Date formatting
            val dateStr = remember {
                SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            }
            Text(
                text = dateStr.uppercase(Locale.US),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.50f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Battery telemetry pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Canvas(modifier = Modifier.size(width = 16.dp, height = 9.dp)) {
                    val strokeW = 1.2.dp.toPx()
                    val rWidth = size.width - 2.5.dp.toPx()
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.70f),
                        size = androidx.compose.ui.geometry.Size(rWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                        style = Stroke(width = strokeW)
                    )
                    // Terminal tip
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.70f),
                        topLeft = Offset(rWidth + 0.5.dp.toPx(), size.height * 0.28f),
                        size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height * 0.44f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                    // Fill level
                    val fillFraction = (telemetry.batteryPercent / 100f).coerceIn(0f, 1f)
                    val innerPad = 2.dp.toPx()
                    val fillWidth = (rWidth - innerPad * 2) * fillFraction
                    if (fillWidth > 0f) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.85f),
                            topLeft = Offset(innerPad, innerPad),
                            size = androidx.compose.ui.geometry.Size(fillWidth, size.height - innerPad * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                    }
                }
                Text(
                    text = "${telemetry.batteryPercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.65f)
                )
                if (telemetry.isDeviceCharging) {
                    Text(
                        text = "• ${telemetry.batteryWattage} W",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF34D399).copy(alpha = 0.80f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Tap to unlock",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.28f)
            )
        }
    }
}

/**
 * Rich MagSafe / Cyber Charging Overlay.
 *
 * Automatically displays for 6 seconds when power is plugged in, or stays visible during preview.
 */
@Composable
fun ChargingAnimationOverlay(
    telemetry: SystemTelemetryData,
    modifier: Modifier = Modifier
) {
    if (!telemetry.isDeviceCharging || telemetry.chargingAnimStyle == ChargingAnimStyle.OFF) {
        return
    }

    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(telemetry.isDeviceCharging) {
        visible = true
        delay(6500L)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)),
        exit = fadeOut(tween(900)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { visible = false }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (telemetry.chargingAnimStyle) {
                ChargingAnimStyle.MAGSAFE_LIQUID_RING -> {
                    MagSafeLiquidRingAnimation(telemetry)
                }
                ChargingAnimStyle.PLASMA_SURGE_WAVE -> {
                    PlasmaWaveSurgeAnimation(telemetry)
                }
                ChargingAnimStyle.CYBER_MINIMAL -> {
                    CyberMinimalGaugeAnimation(telemetry)
                }
                ChargingAnimStyle.OFF -> {}
            }
        }
    }
}

@Composable
private fun MagSafeLiquidRingAnimation(telemetry: SystemTelemetryData) {
    val infiniteTransition = rememberInfiniteTransition(label = "magsafe")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val emerald = Color(0xFF10B981)
    val mint = Color(0xFF34D399)
    val cyan = Color(0xFF06B6D4)

    Box(contentAlignment = Alignment.Center) {
        // Glowing Background Halo
        Box(
            modifier = Modifier
                .size((260 * pulseScale).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(emerald.copy(alpha = 0.28f), Color.Transparent)
                    ),
                    CircleShape
                )
                .blur(20.dp)
        )

        // MagSafe Ring Canvas
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeW = 12.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width - strokeW) / 2f

            // Base subtle ring
            drawCircle(
                color = emerald.copy(alpha = 0.16f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeW)
            )

            // Animated sweeping liquid ring
            val sweepGradient = Brush.sweepGradient(
                colors = listOf(emerald.copy(alpha = 0.1f), mint, cyan, emerald),
                center = center
            )

            drawArc(
                brush = sweepGradient,
                startAngle = rotation,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(strokeW / 2f, strokeW / 2f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        // Inner Information Pillar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MultiOsIcons.RootBolt(
                modifier = Modifier.size(32.dp),
                tint = mint
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${telemetry.batteryPercent}%",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color.White
            )

            Text(
                text = "${telemetry.batteryWattage} W FAST CHARGING",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = mint.copy(alpha = 0.90f)
            )
        }
    }
}

@Composable
private fun PlasmaWaveSurgeAnimation(telemetry: SystemTelemetryData) {
    val infiniteTransition = rememberInfiniteTransition(label = "plasma")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 0.dp)
        ) {
            val w = size.width
            val h = size.height
            val baseY = h * 0.75f

            val path1 = Path().apply {
                moveTo(0f, h)
                lineTo(0f, baseY)
                for (x in 0..w.toInt() step 15) {
                    val angle = (x.toFloat() / w) * (4 * Math.PI).toFloat() + phase
                    val y = baseY + sin(angle) * 28f
                    lineTo(x.toFloat(), y)
                }
                lineTo(w, h)
                close()
            }

            drawPath(
                path = path1,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00F0FF).copy(alpha = 0.40f),
                        Color(0xFF0066FF).copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
        }

        Column(
            modifier = Modifier.padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${telemetry.batteryPercent}%",
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF00F0FF)
            )
            Text(
                text = "PLASMA SURGE • ${telemetry.batteryWattage} W",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun CyberMinimalGaugeAnimation(telemetry: SystemTelemetryData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeW = 6.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width - strokeW) / 2f

            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeW)
            )

            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = (telemetry.batteryPercent / 100f) * 360f,
                useCenter = false,
                topLeft = Offset(strokeW / 2f, strokeW / 2f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${telemetry.batteryPercent}%",
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp,
            color = Color.White
        )
        Text(
            text = "CHARGING (${telemetry.batteryWattage}W)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = Color.White.copy(alpha = 0.45f)
        )
    }
}
