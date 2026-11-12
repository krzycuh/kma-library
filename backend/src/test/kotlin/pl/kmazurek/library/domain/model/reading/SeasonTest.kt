package pl.kmazurek.library.domain.model.reading

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SeasonTest {
    @Test
    fun `every month belongs to exactly one season`() {
        val monthsOf = { season: Season -> (1..12).filter { Season.ofMonth(it) == season }.toSet() }

        assertEquals(setOf(3, 4, 5), monthsOf(Season.SPRING))
        assertEquals(setOf(6, 7, 8), monthsOf(Season.SUMMER))
        assertEquals(setOf(9, 10, 11), monthsOf(Season.AUTUMN))
        assertEquals(setOf(12, 1, 2), monthsOf(Season.WINTER))
    }

    @Test
    fun `month out of range is rejected`() {
        assertThrows<IllegalArgumentException> { Season.ofMonth(0) }
        assertThrows<IllegalArgumentException> { Season.ofMonth(13) }
    }
}
