package com.example.shazamextendsearch

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager

class ShazamListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "ShazamListener"
        val SHAZAM_PACKAGES = listOf(
            "com.shazam.android",
            "com.shazam.encore.android"
        )
        const val BROADCAST_ACTION = "com.example.shazamextendsearch.SHAZAM_DEBUG"
        const val EXTRA_DEBUG_TEXT = "debug_text"
    }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, ShazamListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in SHAZAM_PACKAGES) return
        // 「聞き取り中」などの常駐通知（FLAG_ONGOING_EVENT）は無視する
        if (sbn.isOngoing) return

        val extras = sbn.notification.extras
        val title   = extras.getString(Notification.EXTRA_TITLE)    ?: ""
        val text    = extras.getString(Notification.EXTRA_TEXT)     ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""

        if (title.isBlank() && text.isBlank()) return

        Log.d(TAG, "pkg=$pkg title=$title text=$text sub=$subText")

        val debugText = "pkg=$pkg\ntitle=$title\ntext=$text\nsub=$subText"
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(BROADCAST_ACTION).putExtra(EXTRA_DEBUG_TEXT, debugText)
        )

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("pref_show_debug_notification", false)) {
            NotificationHelper.postDebugNotification(this, title, text, subText)
        }

        val songName   = title
        val artistName = text

        NotificationHelper.postShortcutNotification(
            context           = this,
            songName          = songName,
            artistName        = artistName,
            showGoogle        = prefs.getBoolean("pref_show_google",    true),
            showYoutube       = prefs.getBoolean("pref_show_youtube",   true),
            showWikipedia     = prefs.getBoolean("pref_show_wikipedia", true),
            autoDismissSeconds = prefs.getInt("pref_auto_dismiss_seconds", 30)
        )
    }
}
