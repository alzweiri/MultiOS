package com.galaxy.m21.desktop.ui.gemini

import android.annotation.SuppressLint
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.galaxy.m21.desktop.hardware.HardwareViewModel
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Persistent Singleton Container for Gemini Web (`gemini.google.com/app`).
 *
 * - Never reloads on swipe out / swipe back!
 * - Automatically pauses JavaScript & timers when closed (battery saver / no CPU spikes on stand).
 * - Restores instant state when swiped in.
 * - Reset button allows manual chat reset.
 */
@SuppressLint("StaticFieldLeak")
object GeminiWebContainer {
    @Volatile
    private var instance: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    @Synchronized
    fun getOrCreate(context: Context): WebView {
        instance?.let { existing ->
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }
        val appCtx = context.applicationContext
        val wv = WebView(appCtx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            }
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            loadUrl("https://gemini.google.com/app")
        }
        instance = wv
        return wv
    }

    fun resume() {
        instance?.onResume()
        instance?.resumeTimers()
    }

    fun pause() {
        instance?.onPause()
        instance?.pauseTimers()
    }

    fun reset() {
        instance?.loadUrl("https://gemini.google.com/app")
    }

    fun updateZoom(percent: Int) {
        instance?.settings?.textZoom = percent
    }
}

/**
 * Live Official `gemini.google.com/app` In-App Web Workspace with Damped Fling Physics.
 */
@Composable
fun GeminiAiScreen(
    hardwareViewModel: HardwareViewModel,
    isFullySettled: Boolean = false,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val themeColors = LocalMultiOsColors.current

    val panOffsetX = remember { Animatable(0f) }
    val panOffsetY = remember { Animatable(0f) }

    // Pause WebView JS execution when window is closed / swiped away (battery saver)
    DisposableEffect(isFullySettled) {
        if (isFullySettled) {
            GeminiWebContainer.resume()
        }
        onDispose {
            GeminiWebContainer.pause()
        }
    }

    LaunchedEffect(telemetry.geminiZoomPercent) {
        GeminiWebContainer.updateZoom(telemetry.geminiZoomPercent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating window with fling velocity tracker & magnetic snap
        val velocityTracker = remember { VelocityTracker() }

        LiquidGlassBox(
            modifier = Modifier
                .offset { IntOffset(panOffsetX.value.roundToInt(), panOffsetY.value.roundToInt()) }
                .width(telemetry.geminiWindowW.dp)
                .height(telemetry.geminiWindowH.dp),
            shape = RoundedCornerShape(26.dp),
            borderColor = themeColors.subtleBorder,
            glassAlpha = if (themeColors.isLight) 0.88f else 0.72f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header Bar: Draggable Window Bar with Title, Zoom/DPI, Reset Chat, Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { velocityTracker.resetTracking() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    scope.launch {
                                        panOffsetX.snapTo(panOffsetX.value + dragAmount.x)
                                        panOffsetY.snapTo(panOffsetY.value + dragAmount.y)
                                    }
                                },
                                onDragEnd = {
                                    val vel = velocityTracker.calculateVelocity()
                                    val targetX = if (vel.x > 800f) 120f else if (vel.x < -800f) -120f else 0f
                                    val targetY = if (vel.y > 800f) 80f else if (vel.y < -800f) -80f else 0f
                                    scope.launch {
                                        panOffsetX.animateTo(
                                            targetX,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                        )
                                    }
                                    scope.launch {
                                        panOffsetY.animateTo(
                                            targetY,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                        )
                                    }
                                }
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MultiOsIcons.GeminiSpark(
                            modifier = Modifier.size(20.dp),
                            tint = themeColors.accent
                        )
                        Text(
                            text = "Gemini",
                            color = themeColors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )

                        // Dedicated Zoom / DPI Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(themeColors.subtleFill)
                                .border(0.6.dp, themeColors.subtleBorder, RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "−",
                                color = themeColors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        hardwareViewModel.setGeminiZoomPercent(telemetry.geminiZoomPercent - 10)
                                    }
                                    .padding(horizontal = 4.dp)
                            )
                            Text(
                                text = "${telemetry.geminiZoomPercent}%",
                                color = themeColors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "+",
                                color = themeColors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        hardwareViewModel.setGeminiZoomPercent(telemetry.geminiZoomPercent + 10)
                                    }
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset Chat button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(themeColors.subtleFill)
                                .border(0.6.dp, themeColors.subtleBorder, CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    GeminiWebContainer.reset()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.ResetArrow(Modifier.size(13.dp), themeColors.iconTint)
                        }

                        // Close / Back button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(themeColors.subtleFill)
                                .border(0.6.dp, themeColors.subtleBorder, CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    onClose()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.Close(Modifier.size(13.dp), themeColors.iconTint)
                        }
                    }
                }

                // Persistent Web Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(0.8.dp, themeColors.subtleBorder, RoundedCornerShape(18.dp))
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            GeminiWebContainer.getOrCreate(ctx)
                        },
                        update = { webView ->
                            webView.settings.textZoom = telemetry.geminiZoomPercent
                        }
                    )
                }
            }

            // Bottom-Right Corner Resize Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newW = telemetry.geminiWindowW + (dragAmount.x / 2.2f)
                            val newH = telemetry.geminiWindowH + (dragAmount.y / 2.2f)
                            hardwareViewModel.setGeminiWindowSize(newW, newH)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                MultiOsIcons.ResizeFourSides(
                    modifier = Modifier.size(12.dp),
                    tint = themeColors.textSecondary.copy(alpha = 0.65f)
                )
            }
        }
    }
}
