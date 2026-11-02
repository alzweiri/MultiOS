package com.galaxy.m21.desktop.ui.components

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.galaxy.m21.desktop.ui.theme.ColorPalettePreset
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import com.galaxy.m21.desktop.ui.theme.WallpaperStyle
import kotlin.math.cos
import kotlin.math.sin

private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
    val bmp = Bitmap.createBitmap(width.coerceIn(100, 1920), height.coerceIn(100, 1920), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp.asImageBitmap()
}

/**
 * MultiOS Stationary Spatial Backdrop, Live 60fps Wallpapers & Gallery Wallpaper Canvas.
 *
 * Stays 100% stationary during navigation gestures (ZERO shaking or bending),
 * supporting customizable background blur and live shader animations!
 *
 * NOTE: Blurs ONLY the background wallpaper canvas, NEVER the app icons on top!
 */
@SuppressLint("MissingPermission")
@Composable
fun DynamicWallpaperCanvas(
    style: WallpaperStyle,
    palette: ColorPalettePreset,
    customWallpaper: ImageBitmap? = null,
    syncSystemWallpaper: Boolean = false,
    customColorTint: Color? = null,
    blurRadiusDp: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val themeColors = LocalMultiOsColors.current
    val isLight = themeColors.isLight
    val blurPx = with(density) { blurRadiusDp.toPx() }

    val sysWallpaperBmp = remember(syncSystemWallpaper) {
        if (syncSystemWallpaper) {
            runCatching {
                val wm = WallpaperManager.getInstance(context)
                val d = wm.drawable ?: wm.peekDrawable()
                d?.let { drawableToImageBitmap(it) }
            }.getOrNull()
        } else null
    }

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 1.0f) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect.createBlurEffect(
                blurPx,
                blurPx,
                Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        }
    } else {
        Modifier
    }

    Box(modifier = modifier) {
        // Blurred Wallpaper Core Layer
        Box(modifier = Modifier.fillMaxSize().then(blurModifier)) {
            if (customWallpaper != null) {
                Image(
                    bitmap = customWallpaper,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (syncSystemWallpaper && sysWallpaperBmp != null) {
                Image(
                    bitmap = sysWallpaperBmp,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ProceduralWallpaperContent(
                    style = style,
                    palette = palette,
                    isLight = isLight
                )
            }
        }

        // Ambient Cockpit Color Bleed & Customizable Tint Gradient Layer (crisp overlay)
        val tint = customColorTint ?: palette.primary
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isLight) {
                            listOf(
                                tint.copy(alpha = 0.08f),
                                Color.Transparent,
                                tint.copy(alpha = 0.12f)
                            )
                        } else {
                            listOf(
                                tint.copy(alpha = 0.14f),
                                Color.Black.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.28f)
                            )
                        }
                    )
                )
        )
    }
}

@Composable
private fun ProceduralWallpaperContent(
    style: WallpaperStyle,
    palette: ColorPalettePreset,
    isLight: Boolean
) {
    val themeColors = LocalMultiOsColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "DynamicWallpaperMotion")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.283185f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 22000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Phase"
        )
        val fastPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.283185f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 11000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "FastPhase"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base Background Scrim
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (isLight) {
                        listOf(
                            Color(0xFFF6F8FC),
                            Color(0xFFEDF2F8),
                            Color(0xFFE4ECF5)
                        )
                    } else {
                        listOf(
                            Color(0xFF0C1018),
                            Color(0xFF07090F),
                            Color(0xFF040508)
                        )
                    }
                )
            )

            val accentOrb = themeColors.accent

            when (style) {
                WallpaperStyle.LIQUID_CHROME -> {
                    // Multi-harmonic liquid chrome reflections
                    for (i in 0..4) {
                        val subPhase = phase + i * 1.25f
                        val cx = w * (0.20f + 0.15f * i + 0.08f * cos(subPhase))
                        val cy = h * (0.35f + 0.12f * sin(subPhase * 1.4f))
                        val rad = w * (0.38f + 0.06f * sin(subPhase * 0.8f))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (i % 2 == 0) accentOrb.copy(alpha = if (isLight) 0.28f else 0.18f)
                                    else palette.secondary.copy(alpha = if (isLight) 0.22f else 0.14f),
                                    Color.White.copy(alpha = if (isLight) 0.12f else 0.06f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = rad
                            ),
                            center = Offset(cx, cy),
                            radius = rad
                        )
                    }
                }

                WallpaperStyle.DEEP_CAUSTICS -> {
                    // Organic underwater caustic light refraction grid
                    for (gx in 0..3) {
                        for (gy in 0..2) {
                            val waveX = w * (0.15f + gx * 0.25f) + 32f * cos(fastPhase + gx * 0.8f)
                            val waveY = h * (0.20f + gy * 0.32f) + 28f * sin(fastPhase + gy * 0.9f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentOrb.copy(alpha = if (isLight) 0.18f else 0.10f),
                                        palette.surfaceGlow.copy(alpha = if (isLight) 0.10f else 0.08f),
                                        Color.Transparent
                                    ),
                                    center = Offset(waveX, waveY),
                                    radius = w * 0.28f
                                ),
                                center = Offset(waveX, waveY),
                                radius = w * 0.28f
                            )
                        }
                    }
                }

                WallpaperStyle.COSMIC_NEBULA -> {
                    // Deep space particle nebula gas clouds
                    val orb1 = Offset(w * (0.32f + 0.12f * cos(phase * 0.6f)), h * (0.36f + 0.10f * sin(phase * 0.8f)))
                    val orb2 = Offset(w * (0.72f + 0.09f * sin(phase * 0.7f)), h * (0.64f + 0.11f * cos(phase * 0.9f)))
                    val orbCore = Offset(w * 0.50f, h * 0.48f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF9333EA).copy(alpha = if (isLight) 0.18f else 0.14f),
                                Color.Transparent
                            ),
                            center = orb1,
                            radius = w * 0.55f
                        ),
                        center = orb1,
                        radius = w * 0.55f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentOrb.copy(alpha = if (isLight) 0.24f else 0.16f),
                                Color(0xFF0284C7).copy(alpha = if (isLight) 0.14f else 0.10f),
                                Color.Transparent
                            ),
                            center = orb2,
                            radius = w * 0.50f
                        ),
                        center = orb2,
                        radius = w * 0.50f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isLight) 0.15f else 0.08f),
                                Color.Transparent
                            ),
                            center = orbCore,
                            radius = w * 0.32f
                        ),
                        center = orbCore,
                        radius = w * 0.32f
                    )
                }

                WallpaperStyle.AMOLED_VOID -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentOrb.copy(alpha = if (isLight) 0.08f else 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.45f),
                            radius = w * 0.45f
                        )
                    )
                }

                WallpaperStyle.LIQUID_AURORA -> {
                    val orb1 = Offset(
                        x = w * (0.28f + 0.11f * cos(phase)),
                        y = h * (0.32f + 0.10f * sin(phase))
                    )
                    val orb2 = Offset(
                        x = w * (0.74f + 0.10f * sin(phase * 0.7f)),
                        y = h * (0.68f + 0.09f * cos(phase * 0.7f))
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentOrb.copy(alpha = if (isLight) 0.22f else 0.12f),
                                palette.surfaceGlow.copy(alpha = if (isLight) 0.08f else 0.16f),
                                Color.Transparent
                            ),
                            center = orb1,
                            radius = w * 0.55f
                        ),
                        center = orb1,
                        radius = w * 0.55f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.secondary.copy(alpha = if (isLight) 0.24f else 0.09f),
                                (if (isLight) Color(0xFFCBD5E1) else Color(0xFF1E293B)).copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = orb2,
                            radius = w * 0.50f
                        ),
                        center = orb2,
                        radius = w * 0.50f
                    )
                }

                WallpaperStyle.SOFT_HALO -> {
                    val glowCenter = Offset(w * 0.5f, h * 0.22f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentOrb.copy(alpha = if (isLight) 0.20f else 0.13f),
                                (if (isLight) Color(0xFFCBD5E1) else Color(0xFF1E293B)).copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = glowCenter,
                            radius = w * 0.65f
                        ),
                        center = glowCenter,
                        radius = w * 0.65f
                    )
                }

                WallpaperStyle.MONOCHROME_MIST -> {
                    val center = Offset(w * 0.5f, h * 0.5f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isLight) Color.White else Color.White.copy(alpha = 0.06f)),
                                (if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155)).copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = w * (0.50f + 0.03f * sin(phase))
                        ),
                        center = center,
                        radius = w * 0.56f
                    )
                }
            }
        }
    }
