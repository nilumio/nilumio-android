package com.github.damontecres.wholphin.ui.playback.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class LichtkanteTimecodeTest {
    @Test
    fun `minutes and seconds below an hour`() {
        assertEquals("0:00", formatTimecode(0))
        assertEquals("0:09", formatTimecode(9))
        assertEquals("1:05", formatTimecode(65))
        assertEquals("59:59", formatTimecode(3_599))
    }

    @Test
    fun `hours with padded minutes and seconds`() {
        assertEquals("1:00:00", formatTimecode(3_600))
        assertEquals("1:02:05", formatTimecode(3_725))
        assertEquals("12:00:01", formatTimecode(43_201))
    }
}
