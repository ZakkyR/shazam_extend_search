package com.example.shazamextendsearch

import java.net.URLEncoder

// 検索クエリ・検索URLの組み立てを一か所にまとめる（Shazam通知・検索シートで共通）
object SearchUrls {

    // 空でない要素だけを半角スペースでつなぐ（エンコード前の生クエリ）
    fun joinTerms(vararg terms: String): String =
        terms.map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")

    fun encode(query: String): String = URLEncoder.encode(query.trim(), "UTF-8")

    fun google(query: String): String =
        "https://www.google.com/search?q=${encode(query)}"

    fun youtube(query: String): String =
        "https://www.youtube.com/results?search_query=${encode(query)}"

    fun wikipedia(query: String): String =
        "https://ja.wikipedia.org/wiki/Special:Search?search=${encode(query)}"

    // Google で「クエリ 歌詞」を検索する
    fun lyrics(query: String): String = google(joinTerms(query, "歌詞"))
}
