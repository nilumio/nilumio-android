package com.github.damontecres.wholphin.ui.shuffle

import com.github.damontecres.wholphin.ui.shuffle.ShuffleViewModel.Companion.pickWeighted
import com.github.damontecres.wholphin.ui.shuffle.ShuffleViewModel.Companion.weight
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class ShuffleWeightTest {
    private val now = LocalDateTime.of(2026, 10, 23, 20, 0)

    private fun item(lastPlayed: LocalDateTime?): BaseItemDto {
        val id = UUID.randomUUID()
        return BaseItemDto(
            id = id,
            type = BaseItemKind.EPISODE,
            userData =
                UserItemDataDto(
                    playbackPositionTicks = 0L,
                    playCount = 0,
                    isFavorite = false,
                    lastPlayedDate = lastPlayed,
                    played = lastPlayed != null,
                    key = "",
                    itemId = id,
                ),
        )
    }

    /** Liefert immer denselben Wert für nextDouble(). */
    private fun fixedRandom(value: Double) =
        object : Random() {
            override fun nextBits(bitCount: Int) = 0

            override fun nextDouble() = value
        }

    @Test
    fun `never watched has full weight`() = assertEquals(1.0, weight(null, now), 0.0)

    @Test
    fun `watched in the last two weeks is almost blocked`() {
        assertEquals(0.01, weight(now.minusDays(3), now), 0.0)
        assertEquals(0.01, weight(now.minusDays(13), now), 0.0)
        // Zeitstempel in der Zukunft (falsche Uhr) zählen wie gerade gesehen
        assertEquals(0.01, weight(now.plusDays(2), now), 0.0)
    }

    @Test
    fun `weight returns slowly and is full after about half a year`() {
        assertEquals(0.05 + 14 / 180.0, weight(now.minusDays(14), now), 1e-9)
        assertEquals(0.05 + 90 / 180.0, weight(now.minusDays(90), now), 1e-9)
        assertEquals(1.0, weight(now.minusDays(200), now), 0.0)
        assertEquals(1.0, weight(now.minusYears(2), now), 0.0)
    }

    @Test
    fun `empty list picks nothing`() = assertNull(pickWeighted(emptyList()))

    @Test
    fun `pick walks the cumulative weights`() {
        val items = listOf(item(null), item(null))
        // Summe 2: unter 1 → erstes, darüber → zweites
        assertSame(items[0], pickWeighted(items, fixedRandom(0.25)))
        assertSame(items[1], pickWeighted(items, fixedRandom(0.75)))
        assertSame(items[1], pickWeighted(items, fixedRandom(0.999_999)))
    }

    @Test
    fun `recently watched comes up rarely with a fixed seed`() {
        val unseen = item(null)
        val recent = item(LocalDateTime.now().minusDays(1))
        val random = Random(42)
        val recentPicks = (1..10_000).count { pickWeighted(listOf(unseen, recent), random) === recent }
        // Erwartet etwa 1 % (0,01 / 1,01)
        assertTrue("recent picked $recentPicks times", recentPicks in 50..200)
    }
}
