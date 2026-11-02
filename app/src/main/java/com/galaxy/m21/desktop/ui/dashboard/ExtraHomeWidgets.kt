package com.galaxy.m21.desktop.ui.dashboard

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.galaxy.m21.desktop.service.LiveNotificationItem
import com.galaxy.m21.desktop.service.MultiOsNotificationListener
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live Message / WhatsApp Notification Monitor Window Widget for MultiOS.
 *
 * Real-time liquid glass window displaying incoming WhatsApp, Telegram, SMS, and app notifications.
 * Tap any card to open the conversation directly!
 */
@Composable
fun LiveMessagesMonitorWidget(
    accentColor: Color,
    onAccentColor: Color,
    onDismissWidget: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    val notifications by MultiOsNotificationListener.notifications.collectAsState()
    val isListenerConnected by MultiOsNotificationListener.isConnected.collectAsState()

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }

    LiquidGlassBox(
        modifier = modifier
            .width(260.dp)
            .height(180.dp),
        shape = RoundedCornerShape(22.dp),
        borderColor = themeColors.subtleBorder,
        glassAlpha = if (themeColors.isLight) 0.82f else 0.64f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isListenerConnected) Color(0xFF22C55E) else Color(0xFFEAB308))
                    )
                    Text(
                        text = "Live Messages",
                        color = themeColors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (notifications.isNotEmpty()) {
                        Text(
                            text = "${notifications.size}",
                            color = themeColors.textSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    if (onDismissWidget != null) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(themeColors.subtleFill)
                                .clickable { onDismissWidget() },
                            contentAlignment = Alignment.Center
                        ) {
                            MultiOsIcons.Close(Modifier.size(9.dp), themeColors.iconTint)
                        }
                    }
                }
            }

            // Notifications List / Empty State
            if (!isListenerConnected) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Enable Notification Access",
                        color = themeColors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accentColor)
                            .clickable {
                                runCatching {
                                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Grant Access",
                            color = onAccentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            } else if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No Messages",
                        color = themeColors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Waiting for WhatsApp / SMS...",
                        color = themeColors.textSecondary.copy(alpha = 0.65f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(notifications.take(6), key = { it.key }) { notif ->
                        NotificationRowCard(
                            item = notif,
                            timeFormatted = timeFormat.format(Date(notif.postTimeMs)),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                runCatching {
                                    val launch = context.packageManager.getLaunchIntentForPackage(notif.packageName)
                                    if (launch != null) {
                                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launch)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRowCard(
    item: LiveNotificationItem,
    timeFormatted: String,
    onClick: () -> Unit
) {
    val themeColors = LocalMultiOsColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(themeColors.subtleFill)
            .border(0.5.dp, themeColors.subtleBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (item.smallIcon != null) {
            Image(
                bitmap = item.smallIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(themeColors.subtleBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.appName.take(1).uppercase(),
                    color = themeColors.textPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    color = themeColors.textPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = timeFormatted,
                    color = themeColors.textSecondary,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
            if (item.text.isNotEmpty()) {
                Text(
                    text = item.text,
                    color = themeColors.textSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Battery & Wattage Radial Ring Widget.
 */
@Composable
fun BatteryAndWattageWidget(
    batteryPercent: Int,
    wattage: Float,
    accentColor: Color,
    onDismissWidget: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalMultiOsColors.current

    LiquidGlassBox(
        modifier = modifier
            .width(138.dp)
            .height(138.dp),
        shape = RoundedCornerShape(26.dp),
        borderColor = themeColors.subtleBorder,
        glassAlpha = if (themeColors.isLight) 0.82f else 0.64f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Power",
                    color = themeColors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
                if (onDismissWidget != null) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(themeColors.subtleFill)
                            .clickable { onDismissWidget() },
                        contentAlignment = Alignment.Center
                    ) {
                        MultiOsIcons.Close(Modifier.size(8.dp), themeColors.iconTint)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$batteryPercent%",
                    color = themeColors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "${wattage}W",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Text(
                text = if (wattage > 1.0f) "Charging" else "Discharging",
                color = themeColors.textSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

/**
 * RAM & Storage Gauge Widget.
 */
@Composable
fun RamAndStorageWidget(
    accentColor: Color,
    onDismissWidget: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalMultiOsColors.current
    val memoryUsage = remember {
        val rt = Runtime.getRuntime()
        val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        val maxMb = rt.maxMemory() / (1024 * 1024)
        Pair(usedMb, maxMb)
    }

    val storageUsage = remember {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalGb = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
        val freeGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
        val usedGb = totalGb - freeGb
        Pair(usedGb, totalGb)
    }

    LiquidGlassBox(
        modifier = modifier
            .width(148.dp)
            .height(138.dp),
        shape = RoundedCornerShape(26.dp),
        borderColor = themeColors.subtleBorder,
        glassAlpha = if (themeColors.isLight) 0.82f else 0.64f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Memory",
                    color = themeColors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
                if (onDismissWidget != null) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(themeColors.subtleFill)
                            .clickable { onDismissWidget() },
                        contentAlignment = Alignment.Center
                    ) {
                        MultiOsIcons.Close(Modifier.size(8.dp), themeColors.iconTint)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RAM", color = themeColors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text("${memoryUsage.first}M / ${memoryUsage.second}M", color = themeColors.textSecondary, fontSize = 9.sp)
                    }
                    val ramRatio = (memoryUsage.first.toFloat() / memoryUsage.second.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(themeColors.subtleBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ramRatio)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Disk", color = themeColors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text("${storageUsage.first}G / ${storageUsage.second}G", color = themeColors.textSecondary, fontSize = 9.sp)
                    }
                    val diskRatio = (storageUsage.first.toFloat() / storageUsage.second.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(themeColors.subtleBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(diskRatio)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                    }
                }
            }

            Text(
                text = "crDroid 16 Linux",
                color = themeColors.textSecondary,
                fontSize = 8.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

/**
 * Native Android AppWidget Container: embeds any third-party app's real widget inside MultiOS!
 *
 * - Crash-proof safe view creation with auto root BIND_APPWIDGET grant.
 * - Frameless widget toggle support (removes glass border for flush, sleek third-party widgets).
 */
@Composable
fun CustomAppWidgetHostContainer(
    appWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    editMode: Boolean,
    showGlassBorder: Boolean = true,
    onRemoveWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalMultiOsColors.current
    val widgetInfo = remember(appWidgetId) {
        runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) }.getOrNull()
    }

    val widgetContent: @Composable () -> Unit = {
        if (widgetInfo != null) {
            AndroidView(
                factory = { ctx ->
                    try {
                        appWidgetHost.createView(ctx, appWidgetId, widgetInfo).apply {
                            setAppWidget(appWidgetId, widgetInfo)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("MultiOsWidget", "AppWidgetHostView createView failed: ${e.message}")
                        // Heal root binding in background
                        com.galaxy.m21.desktop.hardware.SysFsBridge.grantAppWidgetBindRoot(ctx.packageName)
                        android.widget.TextView(ctx).apply {
                            text = "Widget Loading..."
                            setTextColor(android.graphics.Color.WHITE)
                            textSize = 12f
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (showGlassBorder) 8.dp else 2.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Widget #$appWidgetId",
                    color = themeColors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }

    Box(
        modifier = modifier
            .width(220.dp)
            .height(140.dp)
    ) {
        if (showGlassBorder) {
            LiquidGlassBox(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(22.dp),
                borderColor = if (editMode) themeColors.accent.copy(alpha = 0.75f) else themeColors.subtleBorder,
                glassAlpha = if (themeColors.isLight) 0.82f else 0.60f
            ) {
                widgetContent()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .then(
                        if (editMode) Modifier.border(1.dp, themeColors.accent.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                        else Modifier
                    )
            ) {
                widgetContent()
            }
        }

        if (editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .clickable { onRemoveWidget() },
                contentAlignment = Alignment.Center
            ) {
                MultiOsIcons.Close(Modifier.size(10.dp), Color.White)
            }
        }
    }
}
