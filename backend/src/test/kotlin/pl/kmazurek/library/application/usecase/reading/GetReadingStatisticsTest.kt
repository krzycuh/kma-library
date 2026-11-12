package pl.kmazurek.library.application.usecase.reading

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.reading.FinishedPeriod
import pl.kmazurek.library.domain.model.reading.Rating
import pl.kmazurek.library.domain.model.reading.ReadingEntry
import pl.kmazurek.library.domain.model.reading.Season
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.ReadingEntryRepository
import java.time.Year

class GetReadingStatisticsTest {
    private val reader = UserId.generate()
    private val solaris = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Lem"), createdByUserId = reader)
    private val cyberiada = Book.create(BookTitle.of("Cyberiada"), AuthorName.parseList("Lem"), createdByUserId = reader)
    private val dune = Book.create(BookTitle.of("Dune"), AuthorName.parseList("Herbert"), createdByUserId = reader)
    private val entryRepository: ReadingEntryRepository = mockk()
    private val bookRepository: BookRepository = mockk()

    private fun useCase(entries: List<ReadingEntry>): GetReadingStatistics {
        every { entryRepository.findAllByReader(reader) } returns entries
        every { bookRepository.findByIds(any()) } returns listOf(solaris, cyberiada, dune).associateBy { it.id }
        return GetReadingStatistics(entryRepository, bookRepository)
    }

    @Test
    fun `groups entries by year, newest year first, and keeps undated ones in the total only`() {
        val stats =
            useCase(
                listOf(
                    ReadingEntry.create(reader, solaris.id, FinishedPeriod(2023, 1), Rating(5)),
                    ReadingEntry.create(reader, dune.id, FinishedPeriod(2024, 7), Rating(3)),
                    ReadingEntry.create(reader, cyberiada.id, FinishedPeriod(2024), null),
                    ReadingEntry.create(reader, solaris.id, null, Rating(4)),
                ),
            ).execute(reader)

        assertEquals(Year.now().value, stats.currentYear)
        assertEquals(listOf(2024, 2023), stats.years.map { it.year })
        assertEquals(listOf(2, 1), stats.years.map { it.books })
        assertEquals(1, stats.withoutYear)
        assertNull(stats.total.year)
        assertEquals(4, stats.total.books)
        assertEquals(3, stats.total.uniqueBooks)
    }

    @Test
    fun `counts months, seasons and entries without a month`() {
        val stats =
            useCase(
                listOf(
                    ReadingEntry.create(reader, solaris.id, FinishedPeriod(2024, 12)),
                    ReadingEntry.create(reader, dune.id, FinishedPeriod(2024, 1)),
                    ReadingEntry.create(reader, cyberiada.id, FinishedPeriod(2024, 7)),
                    ReadingEntry.create(reader, cyberiada.id, FinishedPeriod(2024, 7)),
                    ReadingEntry.create(reader, dune.id, FinishedPeriod(2024)),
                ),
            ).execute(reader)

        val year = stats.years.single()
        assertEquals(12, year.months.size)
        assertEquals(2, year.months.single { it.month == 7 }.count)
        assertEquals(1, year.months.single { it.month == 12 }.count)
        assertEquals(0, year.months.single { it.month == 3 }.count)
        assertEquals(1, year.withoutMonth)
        assertEquals(Season.entries, year.seasons.map { it.season })
        assertEquals(2, year.seasons.single { it.season == Season.WINTER }.count)
        assertEquals(2, year.seasons.single { it.season == Season.SUMMER }.count)
        assertEquals(0, year.seasons.single { it.season == Season.SPRING }.count)
    }

    @Test
    fun `rating distribution covers every star and average ignores unrated entries`() {
        val stats =
            useCase(
                listOf(
                    ReadingEntry.create(reader, solaris.id, FinishedPeriod(2024, 1), Rating(5)),
                    ReadingEntry.create(reader, dune.id, FinishedPeriod(2024, 2), Rating(5)),
                    ReadingEntry.create(reader, cyberiada.id, FinishedPeriod(2024, 3), Rating(2)),
                    ReadingEntry.create(reader, cyberiada.id, FinishedPeriod(2024, 4), null),
                ),
            ).execute(reader)

        assertEquals(listOf(1, 2, 3, 4, 5), stats.total.ratings.map { it.rating })
        assertEquals(listOf(0, 1, 0, 0, 2), stats.total.ratings.map { it.count })
        assertEquals(3, stats.total.ratedBooks)
        assertEquals(4.0, stats.total.averageRating)
    }

    @Test
    fun `top authors are counted per reading, most read first, ties by name`() {
        val stats =
            useCase(
                listOf(
                    ReadingEntry.create(reader, solaris.id, FinishedPeriod(2024, 1)),
                    ReadingEntry.create(reader, cyberiada.id, FinishedPeriod(2024, 2)),
                    ReadingEntry.create(reader, dune.id, FinishedPeriod(2023, 2)),
                ),
            ).execute(reader)

        assertEquals(listOf("Lem" to 2, "Herbert" to 1), stats.total.topAuthors.map { it.author to it.count })
        assertEquals(listOf("Herbert" to 1), stats.years.single { it.year == 2023 }.topAuthors.map { it.author to it.count })
    }

    @Test
    fun `empty log gives empty statistics`() {
        val stats = useCase(emptyList()).execute(reader)

        assertEquals(0, stats.total.books)
        assertNull(stats.total.averageRating)
        assertEquals(emptyList<Any>(), stats.years)
        assertEquals(0, stats.withoutYear)
        assertEquals(12, stats.total.months.size)
    }
}
