package com.example.shazamextendsearch

// 共有メニューで受け取ったテキストから曲名・アーティストを取り出す
object ShareTextParser {

    data class Result(
        val title: String,
        val artist: String,
        // 曲名・アーティストに分解できたか
        val recognized: Boolean,
        // クエリ欄の初期値
        val query: String
    )

    private val URL_REGEX = Regex("""https?://\S+""")
    private val DASH_REGEX = Regex("""\s+[-–—]\s+""")
    // Spotify のページタイトル形式: "曲名 - song and lyrics by アーティスト | Spotify"
    private val SPOTIFY_BY_REGEX = Regex("""^(.+?)\s+[-–—]\s+song(?: and lyrics)? by\s+(.+)$""", RegexOption.IGNORE_CASE)
    // Shazam 英語版: "... discover 曲名 by アーティスト."
    private val SHAZAM_EN_REGEX = Regex("""discover\s+(.+)\s+by\s+(.+?)\.?$""", RegexOption.IGNORE_CASE)
    private val TRAILING_SERVICE_REGEX = Regex("""\s*[|｜]\s*(Spotify|YouTube Music|Apple Music)\s*$""", RegexOption.IGNORE_CASE)

    fun parse(text: String): Result {
        val withoutUrl = text.replace(URL_REGEX, " ")
            .replace(Regex("""\s+"""), " ")
            .replace(TRAILING_SERVICE_REGEX, "")
            .trim()

        // URL だけ等で中身が残らない場合は、共有テキストをそのままクエリに使う
        if (withoutUrl.isBlank()) {
            return Result("", "", recognized = false, query = text.trim())
        }

        SPOTIFY_BY_REGEX.find(withoutUrl)?.let {
            return recognized(it.groupValues[1], it.groupValues[2])
        }
        SHAZAM_EN_REGEX.find(withoutUrl)?.let {
            return recognized(it.groupValues[1], it.groupValues[2])
        }

        // 音楽アプリの「曲名 - アーティスト」形式
        val dashParts = withoutUrl.split(DASH_REGEX, limit = 2)
        if (dashParts.size == 2 && dashParts.all { it.isNotBlank() }) {
            return recognized(dashParts[0], dashParts[1])
        }

        // Shazam の共有文（従来の解釈）
        // "宇多田ヒカルのOne Last Kiss" → 曲名 "One Last Kiss" / アーティスト "宇多田ヒカル"
        // "水曜日のカンパネラのKOKYU" → の が2つ以上は分解しない
        if (withoutUrl.count { it == 'の' } == 1) {
            val parts = withoutUrl.split("の", limit = 2)
            if (parts[0].isNotBlank() && parts[1].isNotBlank()) {
                return recognized(parts[1], parts[0])
            }
        }

        // 解釈できない場合は URL を除いた文字列を曲名・クエリとして扱う
        return Result(withoutUrl, "", recognized = false, query = withoutUrl)
    }

    private fun recognized(title: String, artist: String): Result {
        val t = title.trim()
        val a = artist.trim()
        return Result(t, a, recognized = true, query = SearchUrls.joinTerms(t, a))
    }
}
