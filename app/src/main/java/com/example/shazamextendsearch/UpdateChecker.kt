package com.example.shazamextendsearch

import android.os.Handler
import android.os.Looper
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
