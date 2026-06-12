package com.example.shazamextendsearch

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun buildQuery_combinesNameAndArtist() {
        val result = NotificationHelper.buildQuery("Shape of You", "Ed Sheeran")
        assertEquals("Shape+of+You+Ed+Sheeran", result)
    }

    @Test
    fun buildQuery_withEmptyArtist_returnsSongNameOnly() {
        val result = NotificationHelper.buildQuery("Shape of You", "")
        assertEquals("Shape+of+You", result)
    }

    @Test
    fun buildQuery_withEmptySong_returnsArtistOnly() {
        val result = NotificationHelper.buildQuery("", "Ed Sheeran")
        assertEquals("Ed+Sheeran", result)
    }

    @Test
    fun buildQuery_withBothEmpty_returnsEmpty() {
        val result = NotificationHelper.buildQuery("", "")
        assertEquals("", result)
    }

    @Test
    fun buildQuery_encodesSpecialCharacters() {
        val result = NotificationHelper.buildQuery("rock & roll", "AC/DC")
        assertEquals("rock+%26+roll+AC%2FDC", result)
    }
}
