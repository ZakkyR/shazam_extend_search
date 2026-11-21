package com.example.shazamextendsearch

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchUrlsTest {

    @Test
    fun joinTerms_skipsBlank() {
        assertEquals("A C", SearchUrls.joinTerms(" A ", "", "  ", "C"))
    }

    @Test
    fun google() {
        assertEquals("https://www.google.com/search?q=Shape+of+You", SearchUrls.google("Shape of You"))
    }

    @Test
    fun youtube() {
        assertEquals(
            "https://www.youtube.com/results?search_query=rock+%26+roll",
            SearchUrls.youtube("rock & roll")
        )
    }

    @Test
    fun wikipedia() {
        assertEquals(
            "https://ja.wikipedia.org/wiki/Special:Search?search=AC%2FDC",
            SearchUrls.wikipedia("AC/DC")
        )
    }

    @Test
    fun lyrics_appendsKashi() {
        assertEquals(
            "https://www.google.com/search?q=Lemon+%E7%B1%B3%E6%B4%A5%E7%8E%84%E5%B8%AB+%E6%AD%8C%E8%A9%9E",
            SearchUrls.lyrics("Lemon 米津玄師")
        )
    }
}
