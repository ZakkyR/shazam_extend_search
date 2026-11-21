package com.example.shazamextendsearch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

// 通知アクセス（NotificationListenerService）の権限まわり
object NotificationAccess {

    fun listenerComponent(context: Context): ComponentName =
        ComponentName(context, ShazamListenerService::class.java)

    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val cn = listenerComponent(context).flattenToString()
        return flat.split(":").any { it == cn }
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
