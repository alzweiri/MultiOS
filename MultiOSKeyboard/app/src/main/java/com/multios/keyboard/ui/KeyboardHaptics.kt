package com.multios.keyboard.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Ultra-low-latency hardware haptic engine for MultiOS Keyboard.
 *
 * Bypasses delayed Compose click callbacks and triggers directly on raw pointer `ACTION_DOWN`
 * using pre-allocated `VibrationEffect` instances and `USAGE_TOUCH` / `SONIFICATION` attributes
 * so key taps feel instantaneous and crisp — spot-on with Gboard.
 */
object KeyboardHaptics {

    @Volatile
    private var cachedVibrator: Vibrator? = null

    private val touchAudioAttrs: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private val predefinedTick: VibrationEffect? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK) }.getOrNull()
        } else null
    }

    private val predefinedClick: VibrationEffect? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK) }.getOrNull()
        } else null
    }

    private val predefinedHeavy: VibrationEffect? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK) }.getOrNull()
        } else null
    }

    @Volatile
    private var cachedCustomMs: Int = -1

    @Volatile
    private var cachedCustomEffect: VibrationEffect? = null

    private fun resolveVibrator(context: Context): Vibrator? {
        cachedVibrator?.let { return it }
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        cachedVibrator = vib
        return vib
    }

    private fun vibrateEffect(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
            vibrator.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, touchAudioAttrs)
        }
    }

    /**
     * Fires immediately on `ACTION_DOWN` (0ms latency).
     *
     * @param strengthMs 0 = system crisp tick, 1..30 = custom millisecond pulse
     */
    fun keyDown(view: View, enabled: Boolean, strengthMs: Int = 0) {
        if (!enabled) return
        val vibrator = resolveVibrator(view.context)
        if (vibrator != null && vibrator.hasVibrator()) {
            runCatching {
                if (strengthMs <= 0) {
                    // Native hardware Q+ primitive tick (exact Gboard crisp impulse)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
                    ) {
                        val comp = VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.78f)
                            .compose()
                        vibrateEffect(vibrator, comp)
                        return
                    }
                    val tick = predefinedTick ?: predefinedClick
                    if (tick != null) {
                        vibrateEffect(vibrator, tick)
                        return
                    }
                }

                val clampedMs = strengthMs.coerceIn(3, 30)
                val effect = if (cachedCustomMs == clampedMs && cachedCustomEffect != null) {
                    cachedCustomEffect!!
                } else {
                    val amp = (90 + clampedMs * 5).coerceIn(80, 255)
                    VibrationEffect.createOneShot(clampedMs.toLong(), amp).also {
                        cachedCustomMs = clampedMs
                        cachedCustomEffect = it
                    }
                }
                vibrateEffect(vibrator, effect)
                return
            }
        }
        view.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
    }

    fun cursorTick(view: View, enabled: Boolean) {
        if (!enabled) return
        val vibrator = resolveVibrator(view.context)
        if (vibrator != null && vibrator.hasVibrator()) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
                ) {
                    val comp = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.55f)
                        .compose()
                    vibrateEffect(vibrator, comp)
                    return
                }
                predefinedTick?.let {
                    vibrateEffect(vibrator, it)
                    return
                }
            }
        }
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun longPressPop(view: View, enabled: Boolean) {
        if (!enabled) return
        val vibrator = resolveVibrator(view.context)
        if (vibrator != null && vibrator.hasVibrator()) {
            runCatching {
                val click = predefinedClick ?: predefinedHeavy
                if (click != null) {
                    vibrateEffect(vibrator, click)
                    return
                }
            }
        }
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
