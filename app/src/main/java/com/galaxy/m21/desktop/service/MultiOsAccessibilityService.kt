package com.galaxy.m21.desktop.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Lightweight Accessibility bridge strictly used to invoke native Android System Actions
 * such as `GLOBAL_ACTION_RECENTS` (App Switcher) on unrooted devices.
 * Does not inspect window content (`canRetrieveWindowContent="false"`).
 */
class MultiOsAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (activeInstance?.get() === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var activeInstance: WeakReference<MultiOsAccessibilityService>? = null

        val isConnected: Boolean
            get() = activeInstance?.get() != null

        fun triggerSystemRecents(): Boolean {
            val svc = activeInstance?.get() ?: return false
            return svc.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }
}
