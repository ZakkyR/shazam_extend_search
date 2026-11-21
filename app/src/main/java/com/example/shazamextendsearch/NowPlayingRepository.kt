package com.example.shazamextendsearch

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log

// 再生中の曲の情報
data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String,
    val packageName: String,
    val appName: String,
    val isPlaying: Boolean
)

sealed class NowPlayingResult {
    data class Found(val nowPlaying: NowPlaying) : NowPlayingResult()
    object NotFound : NowPlayingResult()
    object NoPermission : NowPlayingResult()
}

// 通知アクセス権限を使って、アクティブなメディアセッションから再生中の曲を取得する
// （タイル・共有・アプリ本体のどこからでも呼べるよう独立させている）
class NowPlayingRepository(context: Context) {

    companion object {
        private const val TAG = "NowPlayingRepository"
    }

    private val appContext = context.applicationContext

    fun fetch(): NowPlayingResult {
        if (!NotificationAccess.isEnabled(appContext)) return NowPlayingResult.NoPermission

        val manager = appContext.getSystemService(MediaSessionManager::class.java)
            ?: return NowPlayingResult.NotFound

        val controllers = try {
            manager.getActiveSessions(NotificationAccess.listenerComponent(appContext))
        } catch (e: SecurityException) {
            Log.w(TAG, "getActiveSessions が拒否されました", e)
            return NowPlayingResult.NoPermission
        }

        // getActiveSessions は優先度順（直近のものが先頭）で返る
        val candidates = controllers.mapNotNull { toNowPlaying(it) }
        val picked = candidates.firstOrNull { it.isPlaying } ?: candidates.firstOrNull()
        return picked?.let { NowPlayingResult.Found(it) } ?: NowPlayingResult.NotFound
    }

    private fun toNowPlaying(controller: MediaController): NowPlaying? {
        val metadata = controller.metadata ?: return null

        val title = metadata.text(MediaMetadata.METADATA_KEY_TITLE)
            .ifBlank { metadata.text(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) }
        val artist = metadata.text(MediaMetadata.METADATA_KEY_ARTIST)
            .ifBlank { metadata.text(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) }
        val album = metadata.text(MediaMetadata.METADATA_KEY_ALBUM)

        if (title.isBlank() && artist.isBlank()) return null

        val pkg = controller.packageName
        return NowPlaying(
            title = title,
            artist = artist,
            album = album,
            packageName = pkg,
            appName = appLabel(pkg),
            isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        )
    }

    private fun MediaMetadata.text(key: String): String =
        getText(key)?.toString()?.trim().orEmpty()

    private fun appLabel(pkg: String): String = try {
        val pm = appContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        // パッケージの可視性制限などで取れない場合はパッケージ名を表示する
        pkg
    }
}
