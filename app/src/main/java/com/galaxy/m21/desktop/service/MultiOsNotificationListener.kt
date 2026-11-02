package com.galaxy.m21.desktop.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LiveNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTimeMs: Long,
    val smallIcon: ImageBitmap? = null,
    val isMessaging: Boolean = false
)

/**
 * MultiOS Live Notification & WhatsApp / Messages Stream Service.
 *
 * Streams active notifications directly into MultiOS for the Live Message Monitor Widget.
 * Can be auto-granted on rooted crDroid 16 via:
 * `cmd notification allow_listener com.galaxy.m21.desktop/com.galaxy.m21.desktop.service.MultiOsNotificationListener`
 */
class MultiOsNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val item = parseNotification(sbn) ?: return
        _notifications.update { current ->
            (listOf(item) + current.filter { it.key != item.key }).take(24)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        _notifications.update { current ->
            current.filter { it.key != sbn.key }
        }
    }

    private fun refreshActiveNotifications() {
        runCatching {
            val active = activeNotifications ?: return
            val items = active.mapNotNull { parseNotification(it) }.sortedByDescending { it.postTimeMs }
            _notifications.value = items.take(24)
        }
    }

    private fun parseNotification(sbn: StatusBarNotification): LiveNotificationItem? {
        val n = sbn.notification ?: return null
        val extras = n.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()

        if (title.isNullOrBlank() && text.isNullOrBlank()) return null

        val pkg = sbn.packageName
        val pm = packageManager
        val appName = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)

        val iconBmp = runCatching {
            n.smallIcon?.loadDrawable(this)?.toBitmap(48, 48)?.asImageBitmap()
        }.getOrNull()

        val isMsg = pkg.contains("whatsapp", ignoreCase = true) ||
            pkg.contains("telegram", ignoreCase = true) ||
            pkg.contains("messaging", ignoreCase = true) ||
            pkg.contains("mms", ignoreCase = true) ||
            pkg.contains("signal", ignoreCase = true) ||
            pkg.contains("discord", ignoreCase = true)

        return LiveNotificationItem(
            key = sbn.key,
            packageName = pkg,
            appName = appName,
            title = title ?: appName,
            text = text ?: "",
            postTimeMs = sbn.postTime,
            smallIcon = iconBmp,
            isMessaging = isMsg
        )
    }

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _notifications = MutableStateFlow<List<LiveNotificationItem>>(emptyList())
        val notifications: StateFlow<List<LiveNotificationItem>> = _notifications.asStateFlow()

        fun clearItem(key: String) {
            _notifications.update { current -> current.filter { it.key != key } }
        }
    }
}
