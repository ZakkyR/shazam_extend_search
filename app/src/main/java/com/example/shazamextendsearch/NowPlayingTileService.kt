package com.example.shazamextendsearch

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

// クイック設定タイル：タップで再生中の曲の検索シートを開く（常駐通知は出さない）
class NowPlayingTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { openSearchSheet() }
        } else {
            openSearchSheet()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openSearchSheet() {
        val intent = Intent(this, SearchSheetActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val result = NowPlayingRepository(this).fetch()

        tile.state = if (result is NowPlayingResult.Found) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        // サブタイトルは Android 10 以降のみ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (result) {
                is NowPlayingResult.Found -> result.nowPlaying.title.ifBlank { result.nowPlaying.artist }
                NowPlayingResult.NotFound -> "再生中の曲なし"
                NowPlayingResult.NoPermission -> "通知アクセスが必要"
            }
        }
        tile.updateTile()
    }
}
