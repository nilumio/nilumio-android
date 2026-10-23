package com.github.damontecres.wholphin.services.syncplay

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPlayTimeTest {
    @Test
    fun `time sample with symmetric delay finds the exact offset`() {
        // gesendet 1000, Server (+500 ms voraus) empfängt nach 100 ms, antwortet nach 10 ms, zurück nach 100 ms
        val sample = TimeSample.of(sent = 1_000, serverReceived = 1_600, serverSent = 1_610, received = 1_210)
        assertEquals(500L, sample.offsetMs)
        assertEquals(200L, sample.roundTripMs)
    }

    @Test
    fun `time sample with asymmetric delay splits the difference`() {
        val sample = TimeSample.of(sent = 1_000, serverReceived = 1_600, serverSent = 1_610, received = 1_220)
        assertEquals(495L, sample.offsetMs)
        assertEquals(210L, sample.roundTripMs)
    }

    @Test
    fun `position runs on from the start position`() {
        // 5 s Startposition (in Ticks), seit 2,5 s am Laufen
        assertEquals(7_500L, extrapolatePositionMs(positionTicks = 5_000 * TICKS_PER_MS, startedAtMs = 10_000, nowMs = 12_500))
        assertEquals(5_000L, extrapolatePositionMs(positionTicks = 5_000 * TICKS_PER_MS, startedAtMs = 10_000, nowMs = 10_000))
    }

    @Test
    fun `server date round trip keeps milliseconds`() {
        val epochMs = 1_792_000_123_456L
        assertEquals(epochMs, toServerDate(epochMs).toEpochMs())
    }

    @Test
    fun `german genitive for lobby names`() {
        assertEquals("Schnicks", lobbyOwner("Schnick", "de"))
        assertEquals("Annas", lobbyOwner("Anna", "de"))
        assertEquals("Jonas’", lobbyOwner("Jonas", "de"))
        assertEquals("JONAS’", lobbyOwner("JONAS", "de"))
        assertEquals("Strauß’", lobbyOwner("Strauß", "de"))
        assertEquals("Max’", lobbyOwner("Max", "de"))
        assertEquals("Fritz’", lobbyOwner("Fritz", "de"))
    }

    @Test
    fun `other languages keep the name`() {
        assertEquals("Jonas", lobbyOwner("Jonas", "en"))
        assertEquals("Schnick", lobbyOwner("Schnick", "fr"))
    }
}
