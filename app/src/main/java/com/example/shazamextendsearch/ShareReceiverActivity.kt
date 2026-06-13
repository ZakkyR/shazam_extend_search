package com.example.shazamextendsearch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ShareReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") {
            finish()
            return
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: run { finish(); return }
        val (songName, artistName) = parseShazamShare(sharedText)

        setContentView(R.layout.activity_share_receiver)

        findViewById<TextView>(R.id.tv_song_name).text = songName
        val tvArtist = findViewById<TextView>(R.id.tv_artist_name)
        if (artistName.isNotBlank()) {
            tvArtist.text = artistName
        } else {
            tvArtist.visibility = View.GONE
        }

        val query = NotificationHelper.buildQuery(songName, artistName)

        findViewById<Button>(R.id.btn_google).setOnClickListener {
            openUrl("https://www.google.com/search?q=$query")
        }
        findViewById<Button>(R.id.btn_youtube).setOnClickListener {
            openUrl("https://www.youtube.com/results?search_query=$query")
        }
        findViewById<Button>(R.id.btn_wikipedia).setOnClickListener {
            openUrl("https://ja.wikipedia.org/wiki/Special:Search?search=$query")
        }
    }

    // "宇多田ヒカルのOne Last Kiss https://..." → Pair("One Last Kiss", "宇多田ヒカル")
    private fun parseShazamShare(text: String): Pair<String, String> {
        val withoutUrl = text.replace(Regex("""https?://\S+"""), "").trim()
        val parts = withoutUrl.split("の", limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            Pair(parts[1].trim(), parts[0].trim())
        } else {
            Pair(withoutUrl, "")
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        finish()
    }
}
