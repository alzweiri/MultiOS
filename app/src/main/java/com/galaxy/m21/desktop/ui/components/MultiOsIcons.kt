package com.galaxy.m21.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure Minimalist Geometric Vector Iconography for MultiOS.
 * Zero emojis, zero clutter — crisp SF-Symbols-grade strokes and shapes.
 */
object MultiOsIcons {

    @Composable
    fun Speaker(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White,
        level: Float = 0.8f
    ) {
        Canvas(modifier = modifier) {
            drawSpeakerGlyph(tint = tint, level = level)
        }
    }

    @Composable
    fun MusicVolume(
        iconSize: Dp = 16.dp,
        tint: Color = Color.White,
        level: Float = 0.8f,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            MusicNote(modifier = Modifier.size(iconSize), tint = tint)
            Speaker(modifier = Modifier.size(iconSize), tint = tint, level = level)
        }
    }

    @Composable
    fun MusicNote(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            drawMusicNoteGlyph(tint = tint)
        }
    }

    @Composable
    fun BrightnessSun(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            drawSunGlyph(tint = tint)
        }
    }

    @Composable
    fun Moon(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.10f).coerceAtLeast(1.4f)
            drawArc(
                color = tint,
                startAngle = 35f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = Offset(w * 0.16f, h * 0.16f),
                size = Size(w * 0.68f, h * 0.68f),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            drawArc(
                color = tint,
                startAngle = 95f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(w * 0.34f, h * 0.12f),
                size = Size(w * 0.54f, h * 0.54f),
                style = Stroke(width = sw * 0.85f, cap = StrokeCap.Round)
            )
        }
    }

    @Composable
    fun Plus(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.11f).coerceAtLeast(1.5f)
            drawLine(tint, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.50f, h * 0.82f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.18f, h * 0.50f), Offset(w * 0.82f, h * 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun ResetArrow(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.10f).coerceAtLeast(1.4f)
            drawArc(
                color = tint,
                startAngle = -55f,
                sweepAngle = 285f,
                useCenter = false,
                topLeft = Offset(w * 0.18f, h * 0.18f),
                size = Size(w * 0.64f, h * 0.64f),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            val head = Path().apply {
                moveTo(w * 0.62f, h * 0.12f)
                lineTo(w * 0.72f, h * 0.32f)
                lineTo(w * 0.50f, h * 0.30f)
            }
            drawPath(head, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    @Composable
    fun KeyboardGlyph(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.085f).coerceAtLeast(1.3f)
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.10f, h * 0.22f),
                size = Size(w * 0.80f, h * 0.56f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                style = Stroke(width = sw)
            )
            for (col in 0..3) {
                val cx = w * (0.26f + col * 0.16f)
                drawCircle(tint, radius = w * 0.038f, center = Offset(cx, h * 0.40f))
            }
            drawLine(tint, Offset(w * 0.30f, h * 0.62f), Offset(w * 0.70f, h * 0.62f), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun Trash(
        modifier: Modifier = Modifier.size(16.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.095f).coerceAtLeast(1.3f)
            drawLine(tint, Offset(w * 0.20f, h * 0.26f), Offset(w * 0.80f, h * 0.26f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.38f, h * 0.16f), Offset(w * 0.62f, h * 0.16f), strokeWidth = sw, cap = StrokeCap.Round)
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.28f, h * 0.26f),
                size = Size(w * 0.44f, h * 0.58f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                style = Stroke(width = sw)
            )
        }
    }

    @Composable
    fun CpuSpeed(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val st = Stroke(width = (w * 0.095f).coerceAtLeast(1.4f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawArc(
                color = tint,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(w * 0.12f, h * 0.14f),
                size = Size(w * 0.76f, h * 0.76f),
                style = st
            )
            val c = Offset(w * 0.50f, h * 0.56f)
            drawCircle(color = tint, radius = w * 0.08f, center = c)
            drawLine(
                color = tint,
                start = c,
                end = Offset(w * 0.73f, h * 0.31f),
                strokeWidth = (w * 0.095f).coerceAtLeast(1.4f),
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Thermometer(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.09f).coerceAtLeast(1.3f)
            val st = Stroke(width = sw, cap = StrokeCap.Round)
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.39f, h * 0.12f),
                size = Size(w * 0.22f, h * 0.56f),
                cornerRadius = CornerRadius(w * 0.11f, w * 0.11f),
                style = st
            )
            drawCircle(
                color = tint,
                radius = w * 0.18f,
                center = Offset(w * 0.50f, h * 0.74f),
                style = st
            )
            drawCircle(
                color = tint,
                radius = w * 0.08f,
                center = Offset(w * 0.50f, h * 0.74f)
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.50f, h * 0.34f),
                end = Offset(w * 0.50f, h * 0.68f),
                strokeWidth = sw * 0.85f,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun VinylDisc(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val c = Offset(w / 2f, size.height / 2f)
            val sw = (w * 0.085f).coerceAtLeast(1.3f)
            drawCircle(color = tint, radius = w * 0.42f, center = c, style = Stroke(width = sw))
            drawArc(
                color = tint.copy(alpha = 0.65f),
                startAngle = 200f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(w * 0.24f, size.height * 0.24f),
                size = Size(w * 0.52f, size.height * 0.52f),
                style = Stroke(width = sw * 0.8f, cap = StrokeCap.Round)
            )
            drawCircle(color = tint, radius = w * 0.10f, center = c)
        }
    }

    @Composable
    fun BassWave(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.095f).coerceAtLeast(1.4f)
            val bars = listOf(0.32f, 0.68f, 0.90f, 0.56f, 0.36f)
            bars.forEachIndexed { idx, frac ->
                val x = w * (0.18f + idx * 0.16f)
                val barH = h * frac * 0.76f
                drawLine(
                    color = tint,
                    start = Offset(x, (h - barH) / 2f),
                    end = Offset(x, (h + barH) / 2f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    @Composable
    fun Gear(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val c = Offset(w / 2f, h / 2f)
            val sw = (w * 0.095f).coerceAtLeast(1.4f)
            drawCircle(
                color = tint,
                radius = w * 0.16f,
                center = c,
                style = Stroke(width = sw)
            )
            drawCircle(
                color = tint,
                radius = w * 0.31f,
                center = c,
                style = Stroke(width = sw * 0.85f)
            )
            for (i in 0 until 8) {
                val rad = i * (PI / 4.0)
                val p1 = Offset(
                    c.x + (w * 0.31f * cos(rad)).toFloat(),
                    c.y + (h * 0.31f * sin(rad)).toFloat()
                )
                val p2 = Offset(
                    c.x + (w * 0.43f * cos(rad)).toFloat(),
                    c.y + (h * 0.43f * sin(rad)).toFloat()
                )
                drawLine(tint, p1, p2, strokeWidth = sw * 1.15f, cap = StrokeCap.Round)
            }
        }
    }

    @Composable
    fun EditGrid(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.09f).coerceAtLeast(1.3f)
            val st = Stroke(width = sw, join = StrokeJoin.Round)
            val r = CornerRadius(w * 0.08f, w * 0.08f)
            drawRoundRect(tint, Offset(w * 0.14f, h * 0.14f), Size(w * 0.30f, h * 0.30f), r, st)
            drawRoundRect(tint, Offset(w * 0.56f, h * 0.14f), Size(w * 0.30f, h * 0.30f), r, st)
            drawRoundRect(tint, Offset(w * 0.14f, h * 0.56f), Size(w * 0.30f, h * 0.30f), r, st)
            drawLine(tint, Offset(w * 0.71f, h * 0.56f), Offset(w * 0.71f, h * 0.86f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.56f, h * 0.71f), Offset(w * 0.86f, h * 0.71f), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun Play(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.28f, h * 0.18f)
                lineTo(w * 0.80f, h * 0.50f)
                lineTo(w * 0.28f, h * 0.82f)
                close()
            }
            drawPath(path, color = tint)
        }
    }

    @Composable
    fun Pause(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val r = CornerRadius(w * 0.06f, w * 0.06f)
            drawRoundRect(tint, Offset(w * 0.24f, h * 0.18f), Size(w * 0.18f, h * 0.64f), r)
            drawRoundRect(tint, Offset(w * 0.58f, h * 0.18f), Size(w * 0.18f, h * 0.64f), r)
        }
    }

    @Composable
    fun SkipBack(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.10f).coerceAtLeast(1.4f)
            drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
            val path = Path().apply {
                moveTo(w * 0.78f, h * 0.22f)
                lineTo(w * 0.34f, h * 0.50f)
                lineTo(w * 0.78f, h * 0.78f)
                close()
            }
            drawPath(path, color = tint)
        }
    }

    @Composable
    fun SkipForward(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.10f).coerceAtLeast(1.4f)
            drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
            val path = Path().apply {
                moveTo(w * 0.22f, h * 0.22f)
                lineTo(w * 0.66f, h * 0.50f)
                lineTo(w * 0.22f, h * 0.78f)
                close()
            }
            drawPath(path, color = tint)
        }
    }

    @Composable
    fun Wifi(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.095f).coerceAtLeast(1.4f)
            val st = Stroke(width = sw, cap = StrokeCap.Round)
            drawArc(tint, -135f, 90f, false, Offset(w * 0.10f, h * 0.20f), Size(w * 0.80f, h * 0.80f), style = st)
            drawArc(tint, -135f, 90f, false, Offset(w * 0.24f, h * 0.38f), Size(w * 0.52f, h * 0.52f), style = st)
            drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.76f))
        }
    }

    @Composable
    fun Bluetooth(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.09f).coerceAtLeast(1.3f)
            val path = Path().apply {
                moveTo(w * 0.30f, h * 0.28f)
                lineTo(w * 0.68f, h * 0.68f)
                lineTo(w * 0.48f, h * 0.86f)
                lineTo(w * 0.48f, h * 0.14f)
                lineTo(w * 0.68f, h * 0.32f)
                lineTo(w * 0.30f, h * 0.72f)
            }
            drawPath(path, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    @Composable
    fun GeminiSpark(
        modifier: Modifier = Modifier.size(22.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.50f, h * 0.06f)
                quadraticTo(w * 0.52f, h * 0.48f, w * 0.94f, h * 0.50f)
                quadraticTo(w * 0.52f, h * 0.52f, w * 0.50f, h * 0.94f)
                quadraticTo(w * 0.48f, h * 0.52f, w * 0.06f, h * 0.50f)
                quadraticTo(w * 0.48f, h * 0.48f, w * 0.50f, h * 0.06f)
                close()
            }
            drawPath(path, color = tint)
        }
    }

    @Composable
    fun GalleryImage(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.09f).coerceAtLeast(1.3f)
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.12f, h * 0.16f),
                size = Size(w * 0.76f, h * 0.68f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                style = Stroke(width = sw)
            )
            drawCircle(tint, radius = w * 0.075f, center = Offset(w * 0.34f, h * 0.38f))
            val mountain = Path().apply {
                moveTo(w * 0.16f, h * 0.76f)
                lineTo(w * 0.42f, h * 0.50f)
                lineTo(w * 0.58f, h * 0.66f)
                lineTo(w * 0.70f, h * 0.54f)
                lineTo(w * 0.84f, h * 0.76f)
            }
            drawPath(mountain, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    @Composable
    fun HomeHouse(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.095f).coerceAtLeast(1.4f)
            val roof = Path().apply {
                moveTo(w * 0.14f, h * 0.48f)
                lineTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.86f, h * 0.48f)
            }
            drawPath(roof, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val body = Path().apply {
                moveTo(w * 0.24f, h * 0.44f)
                lineTo(w * 0.24f, h * 0.82f)
                lineTo(w * 0.76f, h * 0.82f)
                lineTo(w * 0.76f, h * 0.44f)
            }
            drawPath(body, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    @Composable
    fun Check(
        modifier: Modifier = Modifier.size(16.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.11f).coerceAtLeast(1.5f)
            val path = Path().apply {
                moveTo(w * 0.20f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.74f)
                lineTo(w * 0.82f, h * 0.28f)
            }
            drawPath(path, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    @Composable
    fun Close(
        modifier: Modifier = Modifier.size(16.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (w * 0.10f).coerceAtLeast(1.4f)
            drawLine(tint, Offset(w * 0.24f, h * 0.24f), Offset(w * 0.76f, h * 0.76f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.76f, h * 0.24f), Offset(w * 0.24f, h * 0.76f), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun RootBolt(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.56f, h * 0.12f)
                lineTo(w * 0.24f, h * 0.54f)
                lineTo(w * 0.48f, h * 0.54f)
                lineTo(w * 0.42f, h * 0.88f)
                lineTo(w * 0.76f, h * 0.46f)
                lineTo(w * 0.52f, h * 0.46f)
                close()
            }
            drawPath(path, color = tint)
        }
    }

    @Composable
    fun LocationPin(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.9f)
                cubicTo(w * 0.2f, h * 0.55f, w * 0.2f, h * 0.35f, w * 0.5f, h * 0.15f)
                cubicTo(w * 0.8f, h * 0.35f, w * 0.8f, h * 0.55f, w * 0.5f, h * 0.9f)
                close()
            }
            drawPath(path, color = tint, style = Stroke(width = w * 0.09f))
            drawCircle(color = tint, radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.38f))
        }
    }

    @Composable
    fun CameraGlyph(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.08f
            // Camera body
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.12f, h * 0.3f),
                size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.56f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f, w * 0.12f),
                style = Stroke(width = stroke)
            )
            // Lens circle
            drawCircle(color = tint, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.58f), style = Stroke(width = stroke))
            // Flash bump
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.32f, h * 0.16f),
                size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.15f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f)
            )
        }
    }

    @Composable
    fun MicGlyph(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.08f
            // Capsule
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.36f, h * 0.15f),
                size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.44f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f),
                style = Stroke(width = stroke)
            )
            // Cradle arc
            val cradle = Path().apply {
                moveTo(w * 0.22f, h * 0.44f)
                cubicTo(w * 0.22f, h * 0.72f, w * 0.78f, h * 0.72f, w * 0.78f, h * 0.44f)
            }
            drawPath(cradle, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            // Base stand
            drawLine(tint, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.32f, h * 0.88f), Offset(w * 0.68f, h * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun AirplaneGlyph(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.12f)
                lineTo(w * 0.62f, h * 0.38f)
                lineTo(w * 0.92f, h * 0.52f)
                lineTo(w * 0.62f, h * 0.58f)
                lineTo(w * 0.60f, h * 0.82f)
                lineTo(w * 0.70f, h * 0.88f)
                lineTo(w * 0.50f, h * 0.86f)
                lineTo(w * 0.30f, h * 0.88f)
                lineTo(w * 0.40f, h * 0.82f)
                lineTo(w * 0.38f, h * 0.58f)
                lineTo(w * 0.08f, h * 0.52f)
                lineTo(w * 0.38f, h * 0.38f)
                close()
            }
            drawPath(path, color = tint)
        }
    }

    @Composable
    fun VpnShield(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.08f
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.12f)
                lineTo(w * 0.84f, h * 0.25f)
                cubicTo(w * 0.84f, h * 0.64f, w * 0.5f, h * 0.88f, w * 0.5f, h * 0.88f)
                cubicTo(w * 0.5f, h * 0.88f, w * 0.16f, h * 0.64f, w * 0.16f, h * 0.25f)
                close()
            }
            drawPath(path, color = tint, style = Stroke(width = stroke))
            // Checkmark inside shield
            val check = Path().apply {
                moveTo(w * 0.32f, h * 0.48f)
                lineTo(w * 0.44f, h * 0.60f)
                lineTo(w * 0.68f, h * 0.36f)
            }
            drawPath(check, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    @Composable
    fun ScreenRecordGlyph(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.08f
            drawCircle(color = tint, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke))
            drawCircle(color = tint, radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.5f))
        }
    }

    @Composable
    fun BypassBattery(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.08f
            // Battery outline
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.15f, h * 0.24f),
                size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.52f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f),
                style = Stroke(width = stroke)
            )
            // Anode terminal
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.80f, h * 0.38f),
                size = androidx.compose.ui.geometry.Size(w * 0.07f, h * 0.24f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
            )
            // Power plug / bypass chevron in center
            val chevron = Path().apply {
                moveTo(w * 0.34f, h * 0.38f)
                lineTo(w * 0.48f, h * 0.50f)
                lineTo(w * 0.34f, h * 0.62f)
                moveTo(w * 0.46f, h * 0.38f)
                lineTo(w * 0.60f, h * 0.50f)
                lineTo(w * 0.46f, h * 0.62f)
            }
            drawPath(chevron, color = tint, style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round))
        }
    }

    // =========================================================================
    // LOW-LEVEL DRAW SCOPE HELPERS
    // =========================================================================

    fun DrawScope.drawSpeakerGlyph(
        tint: Color,
        level: Float = 0.8f,
        centerOverride: Offset? = null,
        boxSizePx: Float = size.minDimension
    ) {
        val c = centerOverride ?: Offset(size.width / 2f, size.height / 2f)
        val r = boxSizePx / 2f
        val sw = (boxSizePx * 0.095f).coerceAtLeast(1.3f)

        val cone = Path().apply {
            moveTo(c.x - r * 0.62f, c.y - r * 0.22f)
            lineTo(c.x - r * 0.34f, c.y - r * 0.22f)
            lineTo(c.x - r * 0.02f, c.y - r * 0.52f)
            lineTo(c.x - r * 0.02f, c.y + r * 0.52f)
            lineTo(c.x - r * 0.34f, c.y + r * 0.22f)
            lineTo(c.x - r * 0.62f, c.y + r * 0.22f)
            close()
        }
        drawPath(cone, color = tint)

        if (level <= 0.02f) {
            drawLine(
                color = tint,
                start = Offset(c.x + r * 0.18f, c.y - r * 0.24f),
                end = Offset(c.x + r * 0.56f, c.y + r * 0.24f),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(c.x + r * 0.56f, c.y - r * 0.24f),
                end = Offset(c.x + r * 0.18f, c.y + r * 0.24f),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
        } else {
            drawArc(
                color = tint,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(c.x - r * 0.12f, c.y - r * 0.36f),
                size = Size(r * 0.72f, r * 0.72f),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            if (level > 0.45f) {
                drawArc(
                    color = tint,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(c.x - r * 0.22f, c.y - r * 0.62f),
                    size = Size(r * 1.24f, r * 1.24f),
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }
        }
    }

    fun DrawScope.drawMusicNoteGlyph(
        tint: Color,
        centerOverride: Offset? = null,
        boxSizePx: Float = size.minDimension
    ) {
        val c = centerOverride ?: Offset(size.width / 2f, size.height / 2f)
        val r = boxSizePx / 2f
        val sw = (boxSizePx * 0.10f).coerceAtLeast(1.3f)

        val leftHead = Offset(c.x - r * 0.34f, c.y + r * 0.40f)
        val rightHead = Offset(c.x + r * 0.28f, c.y + r * 0.28f)
        drawCircle(tint, radius = r * 0.21f, center = leftHead)
        drawCircle(tint, radius = r * 0.21f, center = rightHead)

        val stemLeftX = leftHead.x + r * 0.16f
        val stemRightX = rightHead.x + r * 0.16f
        drawLine(tint, Offset(stemLeftX, leftHead.y), Offset(stemLeftX, c.y - r * 0.44f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(stemRightX, rightHead.y), Offset(stemRightX, c.y - r * 0.56f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(stemLeftX, c.y - r * 0.44f), Offset(stemRightX, c.y - r * 0.56f), strokeWidth = sw * 1.35f, cap = StrokeCap.Round)
    }

    fun DrawScope.drawSunGlyph(
        tint: Color,
        centerOverride: Offset? = null,
        boxSizePx: Float = size.minDimension
    ) {
        val c = centerOverride ?: Offset(size.width / 2f, size.height / 2f)
        val r = boxSizePx / 2f
        val sw = (boxSizePx * 0.095f).coerceAtLeast(1.3f)
        drawCircle(color = tint, radius = r * 0.36f, center = c)
        for (ray in 0 until 8) {
            val rad = ray * (PI / 4.0)
            val p1 = Offset(
                c.x + (r * 0.56f * cos(rad)).toFloat(),
                c.y + (r * 0.56f * sin(rad)).toFloat()
            )
            val p2 = Offset(
                c.x + (r * 0.80f * cos(rad)).toFloat(),
                c.y + (r * 0.80f * sin(rad)).toFloat()
            )
            drawLine(tint, p1, p2, strokeWidth = sw, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun RecentsCards(
        modifier: Modifier = Modifier.size(18.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val sw = (size.minDimension * 0.10f).coerceAtLeast(1.3f)
            val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            // Back stacked card
            drawRoundRect(
                color = tint.copy(alpha = 0.58f),
                topLeft = Offset(w * 0.28f, h * 0.12f),
                size = Size(w * 0.58f, h * 0.62f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                style = stroke
            )
            // Front card
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.14f, h * 0.26f),
                size = Size(w * 0.58f, h * 0.62f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                style = stroke
            )
        }
    }

    @Composable
    fun ResizeFourSides(
        modifier: Modifier = Modifier.size(16.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val c = Offset(w / 2f, h / 2f)
            val sw = (size.minDimension * 0.105f).coerceAtLeast(1.3f)
            // Horizontal & vertical axes
            drawLine(tint, Offset(w * 0.14f, c.y), Offset(w * 0.86f, c.y), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(c.x, h * 0.14f), Offset(c.x, h * 0.86f), strokeWidth = sw, cap = StrokeCap.Round)
            // Left arrow head
            drawLine(tint, Offset(w * 0.14f, c.y), Offset(w * 0.28f, c.y - h * 0.14f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.14f, c.y), Offset(w * 0.28f, c.y + h * 0.14f), strokeWidth = sw, cap = StrokeCap.Round)
            // Right arrow head
            drawLine(tint, Offset(w * 0.86f, c.y), Offset(w * 0.72f, c.y - h * 0.14f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.86f, c.y), Offset(w * 0.72f, c.y + h * 0.14f), strokeWidth = sw, cap = StrokeCap.Round)
            // Top arrow head
            drawLine(tint, Offset(c.x, h * 0.14f), Offset(c.x - w * 0.14f, h * 0.28f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(c.x, h * 0.14f), Offset(c.x + w * 0.14f, h * 0.28f), strokeWidth = sw, cap = StrokeCap.Round)
            // Bottom arrow head
            drawLine(tint, Offset(c.x, h * 0.86f), Offset(c.x - w * 0.14f, h * 0.72f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(c.x, h * 0.86f), Offset(c.x + w * 0.14f, h * 0.72f), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}
