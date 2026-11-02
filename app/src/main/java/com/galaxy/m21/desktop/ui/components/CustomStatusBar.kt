package com.galaxy.m21.desktop.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.galaxy.m21.desktop.hardware.SystemTelemetryData
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors

/**
 * Minimalist MultiOS Liquid Glass Pull Tab Capsule.
 * Placed ON TOP CENTER of screen for pulling down Control Center,
 * and at bottom center for pulling up Vinyl Studio!
 */
@Composable
fun MultiOsPullTabCapsule(
    pullUpward: Boolean,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current

    Box(
        modifier = modifier
            .width(108.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(50))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onTrigger()
            }
            .pointerInput(pullUpward) {
                var totalPull = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalPull = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        totalPull += dragAmount
                        val triggered = if (pullUpward) totalPull < -14f else totalPull > 14f
                        if (triggered) {
                            change.consume()
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onTrigger()
                            totalPull = if (pullUpward) 999f else -999f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val barFill = if (themeColors.isLight) {
            themeColors.textPrimary.copy(alpha = 0.28f)
        } else {
            themeColors.textPrimary.copy(alpha = 0.36f)
        }
        val barRim = if (themeColors.isLight) {
            themeColors.textPrimary.copy(alpha = 0.42f)
        } else {
            themeColors.textPrimary.copy(alpha = 0.48f)
        }
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(barFill)
                .border(0.5.dp, barRim, CircleShape)
        )
    }
}

/**
 * Minimal Top In-App Status Bar:
 * - Pull Tab ON TOP OF THE SCREEN (top center) for Control Center!
 * - Edit-Mode Finish Checkmark button at top right!
 */
@Composable
fun CustomStatusBar(
    telemetry: SystemTelemetryData,
    onPullDownControlCenter: () -> Unit,
    onFinishEditMode: () -> Unit
) {
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    val isEditing = telemetry.freeformDragEnabled

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 20.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // TOP CENTER PULL TAB FOR CONTROL CENTER (when not editing)
        if (!isEditing && telemetry.showPullTab) {
            MultiOsPullTabCapsule(
                pullUpward = false,
                onTrigger = onPullDownControlCenter,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // EDIT MODE FINISH CHECKMARK BUTTON
        AnimatedVisibility(
            visible = isEditing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(themeColors.accent)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onFinishEditMode()
                    },
                contentAlignment = Alignment.Center
            ) {
                MultiOsIcons.Check(
                    modifier = Modifier.size(13.dp),
                    tint = themeColors.onAccent
                )
            }
        }
    }
}
