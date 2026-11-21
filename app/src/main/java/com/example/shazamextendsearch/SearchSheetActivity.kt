package com.example.shazamextendsearch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

// 曲名・アーティストを表示して各種検索を行う共通シート
// ・クイック設定タイル / アプリ本体から開く → 再生中の曲を表示
// ・共有メニューから開く（ShareReceiverActivity エイリアス）→ 共有テキストと再生中の曲を切り替え可能
class SearchSheetActivity : AppCompatActivity() {

    companion object {
        private const val STATE_SOURCE = "state_source"
        private const val STATE_QUERY = "state_query"
    }

    private enum class Source { SHARED, NOW_PLAYING }

    // シートに表示する曲情報
    private data class SongInfo(
        val title: String,
        val artist: String,
        val album: String = "",
        val appName: String = "",
        // 曲名・アーティストに分解できなかったときのクエリ初期値
        val fallbackQuery: String = ""
    ) {
        fun buildQuery(includeAlbum: Boolean): String {
            val base = SearchUrls.joinTerms(title, artist).ifBlank { fallbackQuery }
            return if (includeAlbum) SearchUrls.joinTerms(base, album) else base
        }
    }

    private lateinit var rgSource: RadioGroup
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var tvAlbum: TextView
    private lateinit var tvApp: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnPermission: Button
    private lateinit var cbIncludeAlbum: CheckBox
    private lateinit var etQuery: TextInputEditText
    private lateinit var searchButtons: List<Button>
    private lateinit var btnCopyTitle: Button

    private var sharedInfo: SongInfo? = null
    private var nowPlayingResult: NowPlayingResult = NowPlayingResult.NotFound
    private var currentSource = Source.NOW_PLAYING
    private var currentInfo: SongInfo? = null

    // アルバム名の有無を切り替えたらクエリを作り直す（手入力した内容は上書きされる）
    private val albumListener = CompoundButton.OnCheckedChangeListener { _, checked ->
        currentInfo?.let { etQuery.setText(it.buildQuery(checked)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_sheet)
        bindViews()

        sharedInfo = parseSharedIntent(intent)
        nowPlayingResult = NowPlayingRepository(this).fetch()

        currentSource = savedInstanceState?.getString(STATE_SOURCE)
            ?.let { Source.valueOf(it) }
            ?: defaultSource()

        setupSourceSelector()
        showSource(currentSource, resetQuery = savedInstanceState == null)
        savedInstanceState?.getString(STATE_QUERY)?.let { etQuery.setText(it) }
        updateButtonsEnabled()
    }

    override fun onResume() {
        super.onResume()
        // 通知アクセス設定から戻ってきたときは再取得する
        if (nowPlayingResult == NowPlayingResult.NoPermission &&
            NotificationAccess.isEnabled(this)
        ) {
            nowPlayingResult = NowPlayingRepository(this).fetch()
            if (currentSource == Source.NOW_PLAYING) showSource(Source.NOW_PLAYING, resetQuery = true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SOURCE, currentSource.name)
        outState.putString(STATE_QUERY, etQuery.text?.toString().orEmpty())
    }

    private fun bindViews() {
        rgSource       = findViewById(R.id.rg_source)
        tvTitle        = findViewById(R.id.tv_title)
        tvArtist       = findViewById(R.id.tv_artist)
        tvAlbum        = findViewById(R.id.tv_album)
        tvApp          = findViewById(R.id.tv_app)
        tvMessage      = findViewById(R.id.tv_message)
        btnPermission  = findViewById(R.id.btn_permission)
        cbIncludeAlbum = findViewById(R.id.cb_include_album)
        etQuery        = findViewById(R.id.et_query)
        btnCopyTitle   = findViewById(R.id.btn_copy_title)

        val btnGoogle    = findViewById<Button>(R.id.btn_google)
        val btnYoutube   = findViewById<Button>(R.id.btn_youtube)
        val btnWikipedia = findViewById<Button>(R.id.btn_wikipedia)
        val btnLyrics    = findViewById<Button>(R.id.btn_lyrics)
        searchButtons = listOf(btnGoogle, btnYoutube, btnWikipedia, btnLyrics)

        btnGoogle.setOnClickListener    { openSearch(SearchUrls::google) }
        btnYoutube.setOnClickListener   { openSearch(SearchUrls::youtube) }
        btnWikipedia.setOnClickListener { openSearch(SearchUrls::wikipedia) }
        btnLyrics.setOnClickListener    { openSearch(SearchUrls::lyrics) }
        btnCopyTitle.setOnClickListener { copyTitle() }

        btnPermission.setOnClickListener {
            startActivity(NotificationAccess.settingsIntent())
        }

        cbIncludeAlbum.setOnCheckedChangeListener(albumListener)

        etQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = updateButtonsEnabled()
        })
        etQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH && currentQuery().isNotBlank()) {
                openSearch(SearchUrls::google)
                true
            } else {
                false
            }
        }
    }

    private fun parseSharedIntent(intent: Intent?): SongInfo? {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()

        var parsed = ShareTextParser.parse(text)
        // 本文が URL だけのアプリ向けに、件名からも解釈を試みる
        if (!parsed.recognized && subject.isNotBlank()) {
            val fromSubject = ShareTextParser.parse(subject)
            if (fromSubject.recognized || parsed.query.isBlank()) parsed = fromSubject
        }
        if (parsed.query.isBlank()) return null

        return if (parsed.recognized) {
            SongInfo(title = parsed.title, artist = parsed.artist)
        } else {
            SongInfo(title = parsed.title, artist = "", fallbackQuery = parsed.query)
        }
    }

    private fun defaultSource(): Source {
        val shared = sharedInfo ?: return Source.NOW_PLAYING
        // 共有テキストを解釈できず、再生中の曲が取れているときは再生中の曲を優先する
        val sharedUnrecognized = shared.artist.isBlank() && shared.fallbackQuery.isNotBlank() &&
            shared.title.isBlank()
        return if (sharedUnrecognized && nowPlayingResult is NowPlayingResult.Found) {
            Source.NOW_PLAYING
        } else {
            Source.SHARED
        }
    }

    private fun setupSourceSelector() {
        if (sharedInfo == null) {
            rgSource.visibility = View.GONE
            return
        }
        rgSource.visibility = View.VISIBLE
        findViewById<RadioButton>(
            if (currentSource == Source.SHARED) R.id.rb_shared else R.id.rb_now_playing
        ).isChecked = true
        rgSource.setOnCheckedChangeListener { _, checkedId ->
            val source = if (checkedId == R.id.rb_shared) Source.SHARED else Source.NOW_PLAYING
            if (source != currentSource) showSource(source, resetQuery = true)
        }
    }

    private fun showSource(source: Source, resetQuery: Boolean) {
        currentSource = source
        val info: SongInfo?
        var message: String? = null
        var needsPermission = false

        when (source) {
            Source.SHARED -> info = sharedInfo
            Source.NOW_PLAYING -> when (val r = nowPlayingResult) {
                is NowPlayingResult.Found -> {
                    val np = r.nowPlaying
                    info = SongInfo(np.title, np.artist, np.album, np.appName)
                }
                NowPlayingResult.NotFound -> {
                    info = null
                    message = "再生中の曲が見つかりません"
                }
                NowPlayingResult.NoPermission -> {
                    info = null
                    message = "再生中の曲を取得するには通知アクセスの許可が必要です"
                    needsPermission = true
                }
            }
        }
        currentInfo = info

        tvTitle.setTextOrHide(info?.title?.ifBlank { info.fallbackQuery })
        tvArtist.setTextOrHide(info?.artist)
        tvAlbum.setTextOrHide(info?.album?.takeIf { it.isNotBlank() }?.let { "アルバム: $it" })
        tvApp.setTextOrHide(info?.appName?.takeIf { it.isNotBlank() }?.let { "再生元: $it" })
        tvMessage.setTextOrHide(message)
        btnPermission.visibility = if (needsPermission) View.VISIBLE else View.GONE

        val hasAlbum = !info?.album.isNullOrBlank()
        cbIncludeAlbum.visibility = if (hasAlbum) View.VISIBLE else View.GONE
        btnCopyTitle.isEnabled = !info?.title.isNullOrBlank()

        if (resetQuery) {
            // チェック変更リスナーでクエリが作り直されるので、先にチェックを外してから設定する
            cbIncludeAlbum.setOnCheckedChangeListener(null)
            cbIncludeAlbum.isChecked = false
            cbIncludeAlbum.setOnCheckedChangeListener(albumListener)
            etQuery.setText(info?.buildQuery(includeAlbum = false).orEmpty())
        }
        updateButtonsEnabled()
    }

    private fun TextView.setTextOrHide(value: String?) {
        if (value.isNullOrBlank()) {
            visibility = View.GONE
        } else {
            text = value
            visibility = View.VISIBLE
        }
    }

    private fun currentQuery(): String = etQuery.text?.toString()?.trim().orEmpty()

    private fun updateButtonsEnabled() {
        if (!::searchButtons.isInitialized) return
        val enabled = currentQuery().isNotBlank()
        searchButtons.forEach { it.isEnabled = enabled }
    }

    private fun openSearch(buildUrl: (String) -> String) {
        val query = currentQuery()
        if (query.isBlank()) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildUrl(query))))
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "開けるアプリがありません", Toast.LENGTH_SHORT).show()
            return
        }
        finish()
    }

    private fun copyTitle() {
        val title = currentInfo?.title?.takeIf { it.isNotBlank() } ?: return
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("曲名", title))
        // Android 13 以降はシステムがコピー通知を出すため、それより前だけ Toast を出す
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "曲名をコピーしました", Toast.LENGTH_SHORT).show()
        }
    }
}
