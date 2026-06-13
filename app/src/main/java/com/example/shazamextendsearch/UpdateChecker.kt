package com.example.shazamextendsearch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/ZakkyR/shazam_extend_search/releases/latest"
    const val RELEASES_URL =
        "https://github.com/ZakkyR/shazam_extend_search/releases"

    sealed class Result {
        data class Available(val tag: String) : Result()
        object UpToDate : Result()
        object Error : Result()
    }

    fun checkAsync(currentVersion: String, onResult: (Result) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val result = try {
                val conn = URL(API_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().readText()
                    val tag = Regex(""""tag_name"\s*:\s*"([^"]+)"""")
                        .find(json)?.groupValues?.get(1)
                    val latestVersion = tag?.removePrefix("v")
                    if (latestVersion != null && isNewer(currentVersion, latestVersion)) {
                        Result.Available(tag!!)
                    } else {
                        Result.UpToDate
                    }
                } else {
                    Result.Error
                }
            } catch (e: Exception) {
                Result.Error
            }
            handler.post { onResult(result) }
        }.start()
    }

    // アップデート確認 + ダイアログ表示をまとめて行うユーティリティ
    // showUpToDate = true のとき最新版・エラー時もダイアログを表示する（手動チェック用）
    fun checkAndShowDialog(activity: Activity, currentVersion: String, showUpToDate: Boolean) {
        checkAsync(currentVersion) { result ->
            if (activity.isFinishing || activity.isDestroyed) return@checkAsync
            when (result) {
                is Result.Available -> AlertDialog.Builder(activity)
                    .setTitle("アップデートがあります")
                    .setMessage("${result.tag} が利用可能です。ダウンロードページを開きますか？")
                    .setPositiveButton("開く") { _, _ ->
                        activity.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))
                        )
                    }
                    .setNegativeButton("後で", null)
                    .show()
                Result.UpToDate -> if (showUpToDate) AlertDialog.Builder(activity)
                    .setTitle("最新版です")
                    .setMessage("$currentVersion は最新バージョンです。")
                    .setPositiveButton("OK", null)
                    .show()
                Result.Error -> if (showUpToDate) AlertDialog.Builder(activity)
                    .setTitle("確認できませんでした")
                    .setMessage("アップデートの確認に失敗しました。ネットワーク接続を確認してください。")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun isNewer(current: String, latest: String): Boolean {
        val cur = current.split(".").mapNotNull { it.toIntOrNull() }
        val lat = latest.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(cur.size, lat.size)) {
            val c = cur.getOrElse(i) { 0 }
            val l = lat.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
