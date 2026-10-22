package pl.kmazurek.library.domain.model.reading

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.user.UserId
import java.time.YearMonth

class ReadingEntryTest {
    private val reader = UserId.generate()
    private val book = BookId.generate()

    @Test
    fun `should create entry with year only`() {
        val entry = ReadingEntry.create(reader, book, FinishedPeriod(2023), Rating(4), "  great  ")

        assertEquals(2023, entry.finishedOn?.year)
        assertNull(entry.finishedOn?.month)
        assertEquals(4, entry.rating?.value)
        assertEquals("great", entry.review)
        assertEquals(ReadingStatus.FINISHED, entry.status)
    }

    @Test
    fun `should allow unknown finish date and no rating`() {
        val entry = ReadingEntry.create(reader, book)
        assertNull(entry.finishedOn)
        assertNull(entry.rating)
    }

    @Test
    fun `should reject future period`() {
        val next = YearMonth.now().plusMonths(1)
        assertThrows<IllegalArgumentException> {
            ReadingEntry.create(reader, book, FinishedPeriod(next.year, next.monthValue))
        }
    }

    @Test
    fun `should reject rating outside 1-5`() {
        assertThrows<IllegalArgumentException> { Rating(0) }
        assertThrows<IllegalArgumentException> { Rating(6) }
    }

    @Test
    fun `should reject month outside 1-12`() {
        assertThrows<IllegalArgumentException> { FinishedPeriod(2020, 13) }
    }

    @Test
    fun `periods should compare by year then month`() {
        assertTrue(FinishedPeriod(2020, 5) < FinishedPeriod(2021))
        assertTrue(FinishedPeriod(2020) < FinishedPeriod(2020, 1))
    }

    @Test
    fun `update should replace fields and blank review becomes null`() {
        val entry = ReadingEntry.create(reader, book, FinishedPeriod(2020), Rating(2), "meh")
        val updated = entry.update(FinishedPeriod(2021, 3), Rating(5), "   ")

        assertEquals(FinishedPeriod(2021, 3), updated.finishedOn)
        assertEquals(5, updated.rating?.value)
        assertNull(updated.review)
        assertEquals(entry.id, updated.id)
    }
}
