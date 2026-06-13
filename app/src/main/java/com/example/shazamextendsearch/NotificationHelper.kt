package com.example.shazamextendsearch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import java.net.URLEncoder

object NotificationHelper {

    const val CHANNEL_DEBUG = "shazam_debug"
    const val CHANNEL_SHORTCUT = "shazam_shortcut"
    private const val DEBUG_NOTIFICATION_ID = 1001
    const val SHORTCUT_NOTIFICATION_ID = 1002

    private const val PI_GOOGLE    = 100
    private const val PI_YOUTUBE   = 101
    private const val PI_WIKIPEDIA = 102

    private val dismissHandler by lazy { Handler(Looper.getMainLooper()) }
    private var dismissRunnable: Runnable? = null

    fun createChannels(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return

        val debugChannel = NotificationChannel(
            CHANNEL_DEBUG,
            "Shazam デバッグ",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shazam通知内容のダンプ（開発用）"
            setShowBadge(false)
        }

        val shortcutChannel = NotificationChannel(
            CHANNEL_SHORTCUT,
            "Shazam 検索ショートカット",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "認識した曲の検索ショートカット"
            setShowBadge(false)
        }

        nm.createNotificationChannel(debugChannel)
        nm.createNotificationChannel(shortcutChannel)
    }

    fun postDebugNotification(context: Context, title: String, text: String, subText: String) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_DEBUG)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Shazam通知受信")
            .setContentText("title=$title, text=$text")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("title=$title\ntext=$text\nsub=$subText")
            )
            .build()
        nm.notify(DEBUG_NOTIFICATION_ID, notification)
    }

    fun postShortcutNotification(
        context: Context,
        songName: String,
        artistName: String,
        showGoogle: Boolean,
        showYoutube: Boolean,
        showWikipedia: Boolean,
        autoDismissSeconds: Int
    ) {
        if (!showGoogle && !showYoutube && !showWikipedia) return
        if (songName.isBlank() && artistName.isBlank()) return

        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val query = buildQuery(songName, artistName)
        val builder = NotificationCompat.Builder(context, CHANNEL_SHORTCUT)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setSubText("Shazam検索ショートカット")
            .setContentTitle(songName)
            .setContentText(artistName)
            .setLocalOnly(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(true)

        if (showGoogle) {
            builder.addAction(
                0, "Google検索",
                buildBrowserIntent(context, "https://www.google.com/search?q=$query", PI_GOOGLE)
            )
        }
        if (showYoutube) {
            builder.addAction(
                0, "YouTube",
                buildBrowserIntent(context, "https://www.youtube.com/results?search_query=$query", PI_YOUTUBE)
            )
        }
        if (showWikipedia) {
            builder.addAction(
                0, "Wikipedia",
                buildBrowserIntent(context, "https://ja.wikipedia.org/wiki/Special:Search?search=$query", PI_WIKIPEDIA)
            )
        }

        nm.notify(SHORTCUT_NOTIFICATION_ID, builder.build())

        // 常に前回のタイマーをキャンセル
        dismissRunnable?.let { dismissHandler.removeCallbacks(it) }
        dismissRunnable = null

        if (autoDismissSeconds > 0) {
            val runnable = Runnable { nm.cancel(SHORTCUT_NOTIFICATION_ID) }
            dismissRunnable = runnable
            dismissHandler.postDelayed(runnable, autoDismissSeconds * 1000L)
        }
    }

    fun buildQuery(songName: String, artistName: String): String {
        val combined = listOf(songName, artistName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return URLEncoder.encode(combined, "UTF-8")
    }

    private fun buildBrowserIntent(context: Context, url: String, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return PendingIntent.getActivity(
            context.applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
