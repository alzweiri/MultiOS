package com.multios.keyboard.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.multios.keyboard.data.KeyboardMaterialMode
import com.multios.keyboard.data.KeyboardSettingsState

@Immutable
data class KeyboardVisualPalette(
    val mode: KeyboardMaterialMode,
    val baseFillBrush: Brush,
    val baseBorderBrush: Brush,
    val letterKeyFill: Brush,
    val letterKeyPressedFill: Brush,
    val modifierKeyFill: Brush,
    val actionKeyFill: Brush,
    val keyBorderBrush: Brush,
    val primaryText: Color,
    val secondaryText: Color,
    val actionText: Color,
    val popupBackground: Color,
    val popupBorder: Color,
    val popupHighlightFill: Color,
    val popupHighlightText: Color,
    val keyPopDarkerFill: Color,
    val shadowAlpha: Float,
    val topGleamColor: Color
)

object KeyboardGlassShader {
    val isEmulator: Boolean by lazy {
        Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.MODEL.contains("sdk_gphone", ignoreCase = true)
    }

    private const val AGSL_LENS = """
        uniform shader composable;
        uniform float2 uResolution;
        half4 main(float2 fragCoord) {
            float2 res = max(uResolution, float2(1.0, 1.0));
            float2 uv = fragCoord / res;
            float2 centered = uv - 0.5;
            float r = length(centered);
            float lens = smoothstep(0.60, 0.0, r);
            float2 sampleUv = clamp(uv - centered * lens * 0.022, 0.0, 1.0);
            half4 base = composable.eval(sampleUv * res);
            float sheen = pow(max(1.0 - uv.y, 0.0), 3.0) * 0.09;
            return half4(base.rgb + half3(sheen), base.a);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createShader(): RuntimeShader = RuntimeShader(AGSL_LENS)
}

@Composable
fun rememberKeyboardPalette(settings: KeyboardSettingsState): KeyboardVisualPalette {
    return remember(
        settings.materialMode,
        settings.tintPreset,
        settings.customHue,
        settings.tintStrength
    ) {
        val tint = settings.resolvedTintColor
        val strength = settings.tintStrength.coerceIn(0f, 0.85f)
        val hue = settings.resolvedHue

        when (settings.materialMode) {
            KeyboardMaterialMode.LIQUID_GLASS -> {
                val specularTint = lerp(Color.White, tint, (strength * 1.35f).coerceIn(0f, 0.78f))
                val deepGlass = lerp(Color(0xFF0B0F18), tint, (strength * 0.36f).coerceIn(0f, 0.30f))
                val actionAccent = lerp(Color(0xFF38BDF8), tint, 0.65f)

                KeyboardVisualPalette(
                    mode = KeyboardMaterialMode.LIQUID_GLASS,
                    baseFillBrush = Brush.verticalGradient(
                        0.0f to lerp( deepGlass, specularTint, 0.18f ).copy(alpha = 0.86f),
                        0.5f to deepGlass.copy(alpha = 0.90f),
                        1.0f to Color(0xFF070A11).copy(alpha = 0.94f)
                    ),
                    baseBorderBrush = Brush.verticalGradient(
                        0.0f to specularTint.copy(alpha = 0.46f),
                        0.5f to specularTint.copy(alpha = 0.16f),
                        1.0f to specularTint.copy(alpha = 0.08f)
                    ),
                    letterKeyFill = Brush.verticalGradient(
                        0.0f to specularTint.copy(alpha = 0.25f + strength * 0.12f),
                        0.52f to specularTint.copy(alpha = 0.14f + strength * 0.08f),
                        1.0f to specularTint.copy(alpha = 0.09f + strength * 0.06f)
                    ),
                    letterKeyPressedFill = Brush.verticalGradient(
                        0.0f to specularTint.copy(alpha = 0.44f),
                        1.0f to specularTint.copy(alpha = 0.26f)
                    ),
                    modifierKeyFill = Brush.verticalGradient(
                        0.0f to specularTint.copy(alpha = 0.15f + strength * 0.08f),
                        1.0f to specularTint.copy(alpha = 0.06f + strength * 0.04f)
                    ),
                    actionKeyFill = Brush.verticalGradient(
                        0.0f to actionAccent.copy(alpha = 0.48f),
                        1.0f to actionAccent.copy(alpha = 0.26f)
                    ),
                    keyBorderBrush = Brush.verticalGradient(
                        0.0f to specularTint.copy(alpha = 0.54f),
                        0.55f to Color.White.copy(alpha = 0.16f),
                        1.0f to specularTint.copy(alpha = 0.10f)
                    ),
                    primaryText = Color.White.copy(alpha = 0.96f),
                    secondaryText = Color.White.copy(alpha = 0.56f),
                    actionText = Color.White,
                    popupBackground = lerp(Color(0xFF0E131E), tint, (strength * 0.25f).coerceIn(0f, 0.22f)).copy(alpha = 0.98f),
                    popupBorder = specularTint.copy(alpha = 0.44f),
                    popupHighlightFill = actionAccent.copy(alpha = 0.52f),
                    popupHighlightText = Color.White,
                    keyPopDarkerFill = lerp(Color(0xFF101622), tint, (strength * 0.22f).coerceIn(0f, 0.18f)).copy(alpha = 0.96f),
                    shadowAlpha = 0.42f,
                    topGleamColor = specularTint.copy(alpha = 0.44f)
                )
            }

            KeyboardMaterialMode.BLURRED_KEYS -> {
                val frost = lerp(Color(0xFFE2E8F0), tint, (strength * 1.3f).coerceIn(0f, 0.75f))
                val darkBase = lerp(Color(0xFF0A0D14), tint, (strength * 0.16f).coerceIn(0f, 0.14f))
                val actionFrost = lerp(Color(0xFF60A5FA), tint, 0.65f)

                KeyboardVisualPalette(
                    mode = KeyboardMaterialMode.BLURRED_KEYS,
                    baseFillBrush = Brush.verticalGradient(
                        0.0f to darkBase.copy(alpha = 0.97f),
                        1.0f to Color(0xFF06080D).copy(alpha = 0.99f)
                    ),
                    baseBorderBrush = Brush.verticalGradient(
                        0.0f to frost.copy(alpha = 0.18f),
                        1.0f to Color.Transparent
                    ),
                    letterKeyFill = Brush.radialGradient(
                        colors = listOf(
                            frost.copy(alpha = 0.22f + strength * 0.10f),
                            frost.copy(alpha = 0.15f + strength * 0.08f)
                        )
                    ),
                    letterKeyPressedFill = Brush.verticalGradient(
                        0.0f to frost.copy(alpha = 0.38f),
                        1.0f to frost.copy(alpha = 0.26f)
                    ),
                    modifierKeyFill = Brush.verticalGradient(
                        0.0f to frost.copy(alpha = 0.12f + strength * 0.05f),
                        1.0f to frost.copy(alpha = 0.08f + strength * 0.04f)
                    ),
                    actionKeyFill = Brush.verticalGradient(
                        0.0f to actionFrost.copy(alpha = 0.42f),
                        1.0f to actionFrost.copy(alpha = 0.28f)
                    ),
                    keyBorderBrush = Brush.verticalGradient(
                        0.0f to frost.copy(alpha = 0.26f),
                        1.0f to frost.copy(alpha = 0.08f)
                    ),
                    primaryText = Color(0xFFF8FAFC),
                    secondaryText = Color(0xFF94A3B8),
                    actionText = Color.White,
                    popupBackground = Color(0xFF121722),
                    popupBorder = frost.copy(alpha = 0.34f),
                    popupHighlightFill = actionFrost.copy(alpha = 0.48f),
                    popupHighlightText = Color.White,
                    keyPopDarkerFill = Color(0xFF111621).copy(alpha = 0.97f),
                    shadowAlpha = 0.24f,
                    topGleamColor = frost.copy(alpha = 0.18f)
                )
            }

            KeyboardMaterialMode.ALL_PASTEL -> {
                val satBoost = (0.14f + strength * 0.28f).coerceIn(0.12f, 0.38f)
                val pastelBaseTop = Color.hsv(hue, (satBoost * 0.72f).coerceIn(0.06f, 0.26f), 0.95f)
                val pastelBaseBot = Color.hsv(hue, (satBoost * 0.85f).coerceIn(0.08f, 0.30f), 0.91f)
                val pastelKeyTop = Color.hsv(hue, (satBoost * 0.28f).coerceIn(0.02f, 0.12f), 1.0f)
                val pastelKeyBot = Color.hsv(hue, (satBoost * 0.42f).coerceIn(0.04f, 0.16f), 0.97f)
                val pastelModTop = Color.hsv(hue, (satBoost * 1.05f).coerceIn(0.12f, 0.38f), 0.88f)
                val pastelModBot = Color.hsv(hue, (satBoost * 1.15f).coerceIn(0.14f, 0.42f), 0.84f)
                val pastelActionTop = Color.hsv(hue, (satBoost * 1.75f).coerceIn(0.32f, 0.65f), 0.86f)
                val pastelActionBot = Color.hsv(hue, (satBoost * 1.85f).coerceIn(0.36f, 0.70f), 0.78f)
                val inkDark = Color.hsv(hue, 0.38f, 0.20f)
                val inkMuted = Color.hsv(hue, 0.28f, 0.46f)

                KeyboardVisualPalette(
                    mode = KeyboardMaterialMode.ALL_PASTEL,
                    baseFillBrush = Brush.verticalGradient(listOf(pastelBaseTop, pastelBaseBot)),
                    baseBorderBrush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.85f), pastelModBot.copy(alpha = 0.45f))
                    ),
                    letterKeyFill = Brush.verticalGradient(listOf(pastelKeyTop, pastelKeyBot)),
                    letterKeyPressedFill = Brush.verticalGradient(listOf(pastelModTop, pastelModBot)),
                    modifierKeyFill = Brush.verticalGradient(listOf(pastelModTop, pastelModBot)),
                    actionKeyFill = Brush.verticalGradient(listOf(pastelActionTop, pastelActionBot)),
                    keyBorderBrush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.92f), pastelModBot.copy(alpha = 0.55f))
                    ),
                    primaryText = inkDark,
                    secondaryText = inkMuted,
                    actionText = Color.White,
                    popupBackground = Color.hsv(hue, (satBoost * 1.15f).coerceIn(0.14f, 0.40f), 0.84f),
                    popupBorder = pastelModBot,
                    popupHighlightFill = pastelActionTop,
                    popupHighlightText = Color.White,
                    keyPopDarkerFill = Color.hsv(hue, (satBoost * 1.20f).coerceIn(0.15f, 0.42f), 0.82f),
                    shadowAlpha = 0.14f,
                    topGleamColor = Color.White.copy(alpha = 0.70f)
                )
            }
        }
    }
}

/**
 * Adaptive outer keyboard container implementing Liquid Glass Base, Minimal Dark Base, or Pastel Base.
 */
@Composable
fun KeyboardSurfaceContainer(
    palette: KeyboardVisualPalette,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    roundBottomCorners: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val bottomR = if (roundBottomCorners) cornerRadius else 0.dp
    val shape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = bottomR,
        bottomEnd = bottomR
    )
    val cachedShader = remember(palette.mode) {
        if (palette.mode == KeyboardMaterialMode.LIQUID_GLASS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !KeyboardGlassShader.isEmulator
        ) {
            runCatching { KeyboardGlassShader.createShader() }.getOrNull()
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = palette.shadowAlpha),
                spotColor = Color.Black.copy(alpha = palette.shadowAlpha)
            )
            .clip(shape)
            .background(palette.baseFillBrush)
            .border(0.9.dp, palette.baseBorderBrush, shape)
    ) {
        if (palette.mode == KeyboardMaterialMode.LIQUID_GLASS) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            cachedShader != null &&
                            size.width > 0f &&
                            size.height > 0f
                        ) {
                            cachedShader.setFloatUniform("uResolution", size.width, size.height)
                            renderEffect = RenderEffect
                                .createRuntimeShaderEffect(cachedShader, "composable")
                                .asComposeRenderEffect()
                        }
                    }
            )
        }
        // Top optical edge gleam
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, palette.topGleamColor, Color.Transparent)
                    )
                )
        )
        content()
    }
}

/**
 * Minimalist geometric vector icons for MultiOS Keyboard keys & toolbar.
 */
object KeyboardIcons {

    @Composable
    fun ShiftArrow(modifier: Modifier, color: Color, filled: Boolean, capsLock: Boolean) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val arrowBottom = if (capsLock) h * 0.72f else h * 0.82f
            val path = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.84f, h * 0.50f)
                lineTo(w * 0.64f, h * 0.50f)
                lineTo(w * 0.64f, arrowBottom)
                lineTo(w * 0.36f, arrowBottom)
                lineTo(w * 0.36f, h * 0.50f)
                lineTo(w * 0.16f, h * 0.50f)
                close()
            }
            if (filled || capsLock) {
                drawPath(path, color = color)
            } else {
                drawPath(
                    path,
                    color = color,
                    style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            if (capsLock) {
                drawLine(
                    color = color,
                    start = Offset(w * 0.34f, h * 0.86f),
                    end = Offset(w * 0.66f, h * 0.86f),
                    strokeWidth = w * 0.09f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    @Composable
    fun Backspace(modifier: Modifier, color: Color) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.08f
            val body = Path().apply {
                moveTo(w * 0.36f, h * 0.24f)
                lineTo(w * 0.86f, h * 0.24f)
                lineTo(w * 0.86f, h * 0.76f)
                lineTo(w * 0.36f, h * 0.76f)
                lineTo(w * 0.14f, h * 0.50f)
                close()
            }
            drawPath(body, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round))
            drawLine(color, Offset(w * 0.47f, h * 0.39f), Offset(w * 0.69f, h * 0.61f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.69f, h * 0.39f), Offset(w * 0.47f, h * 0.61f), stroke, StrokeCap.Round)
        }
    }

    @Composable
    fun Globe(modifier: Modifier, color: Color) {
        Canvas(modifier = modifier) {
            val w = size.width
            val r = size.minDimension * 0.38f
            val c = center
            val stroke = w * 0.075f
            drawCircle(color, radius = r, center = c, style = Stroke(width = stroke))
            drawOval(
                color = color,
                topLeft = Offset(c.x - r * 0.48f, c.y - r),
                size = Size(r * 0.96f, r * 2f),
                style = Stroke(width = stroke * 0.85f)
            )
            drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), stroke * 0.85f)
        }
    }

    @Composable
    fun EmojiSmiley(modifier: Modifier, color: Color) {
        Canvas(modifier = modifier) {
            val w = size.width
            val r = size.minDimension * 0.38f
            val c = center
            val stroke = w * 0.075f
            drawCircle(color, radius = r, center = c, style = Stroke(width = stroke))
            drawCircle(color, radius = w * 0.045f, center = Offset(c.x - r * 0.34f, c.y - r * 0.20f))
            drawCircle(color, radius = w * 0.045f, center = Offset(c.x + r * 0.34f, c.y - r * 0.20f))
            drawArc(
                color = color,
                startAngle = 22f,
                sweepAngle = 136f,
                useCenter = false,
                topLeft = Offset(c.x - r * 0.52f, c.y - r * 0.38f),
                size = Size(r * 1.04f, r * 0.92f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }

    @Composable
    fun ActionGlyph(modifier: Modifier, color: Color, imeAction: Int) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.085f
            when (imeAction) {
                android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> {
                    val r = w * 0.23f
                    val cc = Offset(w * 0.44f, h * 0.44f)
                    drawCircle(color, radius = r, center = cc, style = Stroke(width = stroke))
                    drawLine(color, Offset(cc.x + r * 0.72f, cc.y + r * 0.72f), Offset(w * 0.80f, h * 0.80f), stroke, StrokeCap.Round)
                }
                android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> {
                    val p = Path().apply {
                        moveTo(w * 0.20f, h * 0.24f)
                        lineTo(w * 0.82f, h * 0.50f)
                        lineTo(w * 0.20f, h * 0.76f)
                        lineTo(w * 0.33f, h * 0.50f)
                        close()
                    }
                    drawPath(p, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round))
                }
                android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> {
                    val p = Path().apply {
                        moveTo(w * 0.22f, h * 0.52f)
                        lineTo(w * 0.42f, h * 0.72f)
                        lineTo(w * 0.78f, h * 0.30f)
                    }
                    drawPath(p, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                android.view.inputmethod.EditorInfo.IME_ACTION_GO,
                android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> {
                    drawLine(color, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.78f, h * 0.50f), stroke, StrokeCap.Round)
                    val head = Path().apply {
                        moveTo(w * 0.54f, h * 0.28f)
                        lineTo(w * 0.78f, h * 0.50f)
                        lineTo(w * 0.54f, h * 0.72f)
                    }
                    drawPath(head, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                else -> {
                    // Classic Return bent arrow
                    val p = Path().apply {
                        moveTo(w * 0.74f, h * 0.28f)
                        lineTo(w * 0.74f, h * 0.58f)
                        lineTo(w * 0.24f, h * 0.58f)
                    }
                    drawPath(p, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    val head = Path().apply {
                        moveTo(w * 0.42f, h * 0.40f)
                        lineTo(w * 0.24f, h * 0.58f)
                        lineTo(w * 0.42f, h * 0.76f)
                    }
                    drawPath(head, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }

    @Composable
    fun ResizeVertical(modifier: Modifier, color: Color) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.085f
            drawLine(color, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.50f, h * 0.82f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.32f, h * 0.34f), Offset(w * 0.50f, h * 0.16f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.68f, h * 0.34f), Offset(w * 0.50f, h * 0.16f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.32f, h * 0.66f), Offset(w * 0.50f, h * 0.84f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.68f, h * 0.66f), Offset(w * 0.50f, h * 0.84f), stroke, StrokeCap.Round)
        }
    }

    @Composable
    fun Gear(modifier: Modifier, color: Color) {
        Canvas(modifier = modifier) {
            val w = size.width
            val c = center
            val rOuter = w * 0.30f
            val stroke = w * 0.08f
            drawCircle(color, radius = rOuter, center = c, style = Stroke(width = stroke))
            drawCircle(color, radius = w * 0.10f, center = c)
            for (i in 0 until 8) {
                val rad = Math.toRadians(i * 45.0)
                val sx = c.x + kotlin.math.cos(rad).toFloat() * (rOuter + stroke * 0.2f)
                val sy = c.y + kotlin.math.sin(rad).toFloat() * (rOuter + stroke * 0.2f)
                val ex = c.x + kotlin.math.cos(rad).toFloat() * (w * 0.44f)
                val ey = c.y + kotlin.math.sin(rad).toFloat() * (w * 0.44f)
                drawLine(color, Offset(sx, sy), Offset(ex, ey), stroke, StrokeCap.Round)
            }
        }
    }

    @Composable
    fun KeyboardIconGlyph(modifier: Modifier, color: Color) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.12f, h * 0.24f),
                size = Size(w * 0.76f, h * 0.52f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                style = Stroke(width = w * 0.075f)
            )
            drawLine(color, Offset(w * 0.30f, h * 0.62f), Offset(w * 0.70f, h * 0.62f), w * 0.075f, StrokeCap.Round)
        }
    }

    @Composable
    fun Clipboard(modifier: Modifier, color: Color) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.08f
            // Board body
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.20f, h * 0.22f),
                size = Size(w * 0.60f, h * 0.64f),
                cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                style = Stroke(width = stroke)
            )
            // Top clip clasp
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.35f, h * 0.12f),
                size = Size(w * 0.30f, h * 0.14f),
                cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
            )
            // Two text lines
            drawLine(color, Offset(w * 0.33f, h * 0.46f), Offset(w * 0.67f, h * 0.46f), stroke * 0.85f, StrokeCap.Round)
            drawLine(color, Offset(w * 0.33f, h * 0.63f), Offset(w * 0.57f, h * 0.63f), stroke * 0.85f, StrokeCap.Round)
        }
    }

    @Composable
    fun MoveFourWay(modifier: Modifier, color: Color) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.08f
            // Cross axes
            drawLine(color, Offset(w * 0.50f, h * 0.14f), Offset(w * 0.50f, h * 0.86f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.14f, h * 0.50f), Offset(w * 0.86f, h * 0.50f), stroke, StrokeCap.Round)
            // Top chevron
            drawLine(color, Offset(w * 0.37f, h * 0.27f), Offset(w * 0.50f, h * 0.14f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.63f, h * 0.27f), Offset(w * 0.50f, h * 0.14f), stroke, StrokeCap.Round)
            // Bottom chevron
            drawLine(color, Offset(w * 0.37f, h * 0.73f), Offset(w * 0.50f, h * 0.86f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.63f, h * 0.73f), Offset(w * 0.50f, h * 0.86f), stroke, StrokeCap.Round)
            // Left chevron
            drawLine(color, Offset(w * 0.27f, h * 0.37f), Offset(w * 0.14f, h * 0.50f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.27f, h * 0.63f), Offset(w * 0.14f, h * 0.50f), stroke, StrokeCap.Round)
            // Right chevron
            drawLine(color, Offset(w * 0.73f, h * 0.37f), Offset(w * 0.86f, h * 0.50f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.73f, h * 0.63f), Offset(w * 0.86f, h * 0.50f), stroke, StrokeCap.Round)
        }
    }

    @Composable
    fun Pin(modifier: Modifier, color: Color, filled: Boolean) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.085f
            val head = Path().apply {
                moveTo(w * 0.32f, h * 0.20f)
                lineTo(w * 0.68f, h * 0.20f)
                lineTo(w * 0.64f, h * 0.46f)
                lineTo(w * 0.76f, h * 0.58f)
                lineTo(w * 0.24f, h * 0.58f)
                lineTo(w * 0.36f, h * 0.46f)
                close()
            }
            if (filled) {
                drawPath(head, color = color)
            } else {
                drawPath(head, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round))
            }
            drawLine(color, Offset(w * 0.50f, h * 0.58f), Offset(w * 0.50f, h * 0.86f), stroke, StrokeCap.Round)
        }
    }

    @Composable
    fun TextCursorIcon(modifier: Modifier, color: Color) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.085f
            // I-beam cursor
            drawLine(color, Offset(w * 0.34f, h * 0.18f), Offset(w * 0.66f, h * 0.18f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.50f, h * 0.82f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.34f, h * 0.82f), Offset(w * 0.66f, h * 0.82f), stroke, StrokeCap.Round)
            // Left & right micro chevrons
            drawLine(color, Offset(w * 0.24f, h * 0.40f), Offset(w * 0.12f, h * 0.50f), stroke * 0.85f, StrokeCap.Round)
            drawLine(color, Offset(w * 0.24f, h * 0.60f), Offset(w * 0.12f, h * 0.50f), stroke * 0.85f, StrokeCap.Round)
            drawLine(color, Offset(w * 0.76f, h * 0.40f), Offset(w * 0.88f, h * 0.50f), stroke * 0.85f, StrokeCap.Round)
            drawLine(color, Offset(w * 0.76f, h * 0.60f), Offset(w * 0.88f, h * 0.50f), stroke * 0.85f, StrokeCap.Round)
        }
    }
}
