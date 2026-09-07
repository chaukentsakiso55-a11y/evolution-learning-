package com.cyberpulse.evolutionlearning

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {
    private var lastRedirectAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        if (!FocusSessionStore.isActive(this)) return

        val openedPackage = event.packageName?.toString().orEmpty()
        if (openedPackage.isBlank()) return
        if (openedPackage == packageName) return
        if (openedPackage in SAFETY_ALLOWLIST) return

        val now = System.currentTimeMillis()
        if (now - lastRedirectAt < 450L) return
        lastRedirectAt = now

        val intent = Intent(this, FocusLockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit

    companion object {
        private val SAFETY_ALLOWLIST = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.samsung.android.permissioncontroller",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer"
        )
    }
}
