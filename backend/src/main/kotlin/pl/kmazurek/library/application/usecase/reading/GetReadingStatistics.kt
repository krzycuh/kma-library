package pl.kmazurek.library.application.usecase.reading

import org.springframework.stereotype.Service
import pl.kmazurek.library.application.dto.AuthorCountDto
import pl.kmazurek.library.application.dto.MonthCountDto
import pl.kmazurek.library.application.dto.RatingCountDto
import pl.kmazurek.library.application.dto.ReadingPeriodStatsDto
import pl.kmazurek.library.application.dto.ReadingStatisticsDto
import pl.kmazurek.library.application.dto.SeasonCountDto
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.reading.Rating
import pl.kmazurek.library.domain.model.reading.ReadingEntry
import pl.kmazurek.library.domain.model.reading.Season
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.ReadingEntryRepository
import java.time.Year

/**
 * Use Case: statistics of my reading log – per year, per month and season, ratings, most read authors.
 * Everything is computed in one go from all of the reader's entries: a personal log is small,
 * and the page can then switch between years without another request.
 */
@Service
class GetReadingStatistics(
    private val readingEntryRepository: ReadingEntryRepository,
    private val bookRepository: BookRepository,
) {
    fun execute(readerId: UserId): ReadingStatisticsDto {
        val entries = readingEntryRepository.findAllByReader(readerId)
        val books = bookRepository.findByIds(entries.map { it.bookId }.toSet())
        val byYear = entries.filter { it.finishedOn != null }.groupBy { it.finishedOn!!.year }
        return ReadingStatisticsDto(
            currentYear = Year.now().value,
            total = periodStats(null, entries, books),
            years = byYear.entries.sortedByDescending { it.key }.map { (year, yearEntries) -> periodStats(year, yearEntries, books) },
            withoutYear = entries.count { it.finishedOn == null },
        )
    }

    companion object {
        const val TOP_AUTHORS = 5

        internal fun periodStats(
            year: Int?,
            entries: List<ReadingEntry>,
            books: Map<BookId, Book>,
        ): ReadingPeriodStatsDto {
            val ratings = entries.mapNotNull { it.rating?.value }
            val months = entries.mapNotNull { it.finishedOn?.month }
            val monthCounts = months.groupingBy { it }.eachCount()
            val authorCounts =
                entries
                    .flatMap { entry -> books[entry.bookId]?.authors.orEmpty() }
                    .groupingBy { it.value }
                    .eachCount()
            return ReadingPeriodStatsDto(
                year = year,
                books = entries.size,
                uniqueBooks = entries.distinctBy { it.bookId }.size,
                ratedBooks = ratings.size,
                averageRating = if (ratings.isEmpty()) null else ratings.average(),
                ratings = (Rating.MIN..Rating.MAX).map { r -> RatingCountDto(r, ratings.count { it == r }) },
                months = (1..12).map { m -> MonthCountDto(m, monthCounts[m] ?: 0) },
                withoutMonth = entries.size - months.size,
                seasons = Season.entries.map { s -> SeasonCountDto(s, months.count { Season.ofMonth(it) == s }) },
                topAuthors =
                    authorCounts.entries
                        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                        .take(TOP_AUTHORS)
                        .map { AuthorCountDto(it.key, it.value) },
            )
        }
    }
}
