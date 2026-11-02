package com.galaxy.m21.desktop.ui.gate

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.hardware.HardwareViewModel
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import com.galaxy.m21.desktop.ui.theme.WelcomeStyle
import kotlin.math.sin

/**
 * MultiOS Startup & Wake Welcome Gate Screen.
 *
 * - Solves the startup crash sequence on portrait/landscape reboot.
 * - Arrow points STRICTLY LEFT (`<`) per user specification!
 * - Style A: iOS 17/18 animated cursive vector stroke drawing.
 * - Style B: iOS 26/27 luminous liquid glass cursive with specular light sweep.
 * - Root-assisted Keyguard bypass (`dismissKeyguardRoot`) so crDroid lock screen never traps the user!
 */
@Composable
fun HelloGateScreen(
    hardwareViewModel: HardwareViewModel,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val themeColors = LocalMultiOsColors.current
    val accent = telemetry.resolveAccentColor()

    val isVisible = telemetry.showWelcomeScreen && !telemetry.isWelcomeGateDismissed

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(350)),
        exit = fadeOut(tween(450)),
        modifier = modifier
    ) {
        val bezelCornerRadius = telemetry.bezelCornerRadiusDp.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bezelCornerRadius))
                .background(Color(0xFF06080F))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    hardwareViewModel.dismissWelcomeGate()
                },
            contentAlignment = Alignment.Center
        ) {
            // Subtle ambient backdrop radiance
            val infiniteTransition = rememberInfiniteTransition(label = "GateAmbience")
            val ambientPulse by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "AmbientPulse"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.5f, size.height * 0.45f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f * ambientPulse),
                            accent.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.40f * ambientPulse
                    ),
                    center = center,
                    radius = size.width * 0.40f * ambientPulse
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                when (telemetry.welcomeStyle) {
                    WelcomeStyle.IOS_17_CURSIVE -> {
                        CursiveVectorDrawHello(
                            strokeColor = accent,
                            secondaryColor = Color.White
                        )
                    }
                    WelcomeStyle.IOS_26_LIQUID_GLASS -> {
                        LuminousGlassCursiveHello(
                            accentColor = accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Left-pointing arrow pill button strictly: `<`
                LiquidGlassBox(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            hardwareViewModel.dismissWelcomeGate()
                        }
                        .padding(horizontal = 22.dp),
                    shape = RoundedCornerShape(24.dp),
                    glassAlpha = 0.72f
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Arrow pointing LEFT (<)
                        Text(
                            text = "<",
                            color = accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Tap or swipe to start",
                            color = themeColors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

/**
 * Style A: iOS 17/18 animated cursive vector stroke drawing.
 */
@Composable
private fun CursiveVectorDrawHello(
    strokeColor: Color,
    secondaryColor: Color
) {
    val drawProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        drawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .size(width = 320.dp, height = 110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Cursive "hello" bezier path
            val path = Path().apply {
                // 'h'
                moveTo(w * 0.12f, h * 0.75f)
                cubicTo(w * 0.12f, h * 0.20f, w * 0.15f, h * 0.15f, w * 0.20f, h * 0.15f)
                cubicTo(w * 0.24f, h * 0.15f, w * 0.18f, h * 0.75f, w * 0.18f, h * 0.75f)
                cubicTo(w * 0.20f, h * 0.45f, w * 0.28f, h * 0.42f, w * 0.30f, h * 0.58f)
                cubicTo(w * 0.31f, h * 0.70f, w * 0.32f, h * 0.75f, w * 0.35f, h * 0.75f)

                // 'e'
                cubicTo(w * 0.38f, h * 0.75f, w * 0.42f, h * 0.50f, w * 0.45f, h * 0.50f)
                cubicTo(w * 0.48f, h * 0.50f, w * 0.44f, h * 0.75f, w * 0.48f, h * 0.75f)

                // 'l' 1
                cubicTo(w * 0.50f, h * 0.75f, w * 0.50f, h * 0.18f, w * 0.54f, h * 0.18f)
                cubicTo(w * 0.57f, h * 0.18f, w * 0.53f, h * 0.75f, w * 0.56f, h * 0.75f)

                // 'l' 2
                cubicTo(w * 0.58f, h * 0.75f, w * 0.59f, h * 0.18f, w * 0.63f, h * 0.18f)
                cubicTo(w * 0.66f, h * 0.18f, w * 0.62f, h * 0.75f, w * 0.65f, h * 0.75f)

                // 'o'
                cubicTo(w * 0.68f, h * 0.75f, w * 0.70f, h * 0.48f, w * 0.76f, h * 0.48f)
                cubicTo(w * 0.82f, h * 0.48f, w * 0.82f, h * 0.75f, w * 0.76f, h * 0.75f)
                cubicTo(w * 0.71f, h * 0.75f, w * 0.72f, h * 0.52f, w * 0.78f, h * 0.52f)
                cubicTo(w * 0.84f, h * 0.52f, w * 0.88f, h * 0.60f, w * 0.92f, h * 0.58f)
            }

            val pathMeasure = PathMeasure().apply { setPath(path, false) }
            val totalLength = pathMeasure.length
            val curLength = totalLength * drawProgress.value

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(
                    width = 4.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(curLength, totalLength),
                        0f
                    )
                )
            )

            // Leading luminous pen dot
            if (drawProgress.value in 0.01f..0.99f) {
                val pos = pathMeasure.getPosition(curLength)
                drawCircle(
                    color = secondaryColor,
                    radius = 5.5f,
                    center = pos
                )
            }
        }
    }
}

/**
 * Style B: iOS 26/27 luminous liquid glass cursive with light sweep.
 */
@Composable
private fun LuminousGlassCursiveHello(
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GlassSweep")
    val sweepX by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Sweep"
    )

    Box(
        modifier = Modifier.size(width = 320.dp, height = 110.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing base
        Text(
            text = "hello",
            fontSize = 72.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Cursive,
            color = accentColor.copy(alpha = 0.45f),
            modifier = Modifier.graphicsLayer {
                val s = 1f + 0.02f * sin(sweepX * 3.1415f)
                scaleX = s
                scaleY = s
            }
        )

        // Shimmering specular liquid sweep text
        Text(
            text = "hello",
            fontSize = 72.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Cursive,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.35f),
                        Color.White,
                        accentColor.copy(alpha = 0.35f)
                    ),
                    startX = 320f * (sweepX - 0.25f),
                    endX = 320f * (sweepX + 0.25f)
                )
            )
        )
    }
}
