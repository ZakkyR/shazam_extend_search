package com.example.shazamextendsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextParserTest {

    @Test
    fun shazamJapanese_splitsArtistAndTitle() {
        val r = ShareTextParser.parse("宇多田ヒカルのOne Last Kiss https://www.shazam.com/track/123")
        assertTrue(r.recognized)
        assertEquals("One Last Kiss", r.title)
        assertEquals("宇多田ヒカル", r.artist)
        assertEquals("One Last Kiss 宇多田ヒカル", r.query)
    }

    @Test
    fun shazamJapanese_twoNo_keepsWholeText() {
        val r = ShareTextParser.parse("水曜日のカンパネラのKOKYU https://www.shazam.com/track/123")
        assertFalse(r.recognized)
        assertEquals("水曜日のカンパネラのKOKYU", r.title)
        assertEquals("水曜日のカンパネラのKOKYU", r.query)
    }

    @Test
    fun shazamEnglish_discoverBy() {
        val r = ShareTextParser.parse(
            "I used Shazam to discover Shape of You by Ed Sheeran. https://www.shazam.com/track/1"
        )
        assertTrue(r.recognized)
        assertEquals("Shape of You", r.title)
        assertEquals("Ed Sheeran", r.artist)
    }

    @Test
    fun musicAppDashFormat() {
        val r = ShareTextParser.parse("夜に駆ける - YOASOBI https://music.apple.com/jp/album/xxx")
        assertTrue(r.recognized)
        assertEquals("夜に駆ける", r.title)
        assertEquals("YOASOBI", r.artist)
    }

    @Test
    fun dashFormat_takesPrecedenceOverNo() {
        val r = ShareTextParser.parse("君の知らない物語 - supercell\nhttps://open.spotify.com/track/abc")
        assertEquals("君の知らない物語", r.title)
        assertEquals("supercell", r.artist)
    }

    @Test
    fun spotifySongAndLyricsBy() {
        val r = ShareTextParser.parse(
            "Shape of You - song and lyrics by Ed Sheeran | Spotify https://open.spotify.com/track/x"
        )
        assertEquals("Shape of You", r.title)
        assertEquals("Ed Sheeran", r.artist)
    }

    @Test
    fun urlOnly_usesRawTextAsQuery() {
        val url = "https://open.spotify.com/track/abc?si=123"
        val r = ShareTextParser.parse(url)
        assertFalse(r.recognized)
        assertEquals("", r.title)
        assertEquals(url, r.query)
    }

    @Test
    fun plainText_usedAsIs() {
        val r = ShareTextParser.parse("  なにかのメモ  ")
        // の が1つなので Shazam 形式として分解される（従来の挙動を維持）
        assertEquals("メモ", r.title)

        val r2 = ShareTextParser.parse("hello world")
        assertFalse(r2.recognized)
        assertEquals("hello world", r2.query)
    }
}
