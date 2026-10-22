package pl.kmazurek.library.application.usecase.reading

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.library.application.dto.BookReference
import pl.kmazurek.library.application.dto.PagedResponse
import pl.kmazurek.library.application.dto.ReadingEntryDto
import pl.kmazurek.library.application.dto.ReadingSummaryDto
import pl.kmazurek.library.application.usecase.catalog.FindOrCreateBook
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.reading.FinishedPeriod
import pl.kmazurek.library.domain.model.reading.Rating
import pl.kmazurek.library.domain.model.reading.ReadingEntry
import pl.kmazurek.library.domain.model.reading.ReadingEntryId
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.ReadingEntryFilter
import pl.kmazurek.library.domain.repository.ReadingEntryRepository
import java.time.Year

data class AddReadingEntryCommand(
    val readerId: UserId,
    val book: BookReference,
    val finishedYear: Int?,
    val finishedMonth: Int?,
    val rating: Int?,
    val review: String?,
)

data class UpdateReadingEntryCommand(
    val entryId: ReadingEntryId,
    val readerId: UserId,
    val finishedYear: Int?,
    val finishedMonth: Int?,
    val rating: Int?,
    val review: String?,
)

data class ListReadingEntriesQuery(
    val readerId: UserId,
    val year: Int? = null,
    val rating: Int? = null,
    val searchTerm: String? = null,
    val bookId: BookId? = null,
    val page: Int = 0,
    val pageSize: Int = 20,
)

internal fun toFinishedPeriod(
    year: Int?,
    month: Int?,
): FinishedPeriod? {
    if (year == null) {
        require(month == null) { "Month requires a year" }
        return null
    }
    return FinishedPeriod(year, month)
}

/**
 * Use Case: add a finished book to my reading log
 */
@Service
class AddReadingEntry(
    private val readingEntryRepository: ReadingEntryRepository,
    private val findOrCreateBook: FindOrCreateBook,
) {
    @Transactional
    fun execute(command: AddReadingEntryCommand): Pair<ReadingEntry, Book> {
        val book = findOrCreateBook.resolve(command.book, command.readerId)
        val entry =
            ReadingEntry.create(
                readerId = command.readerId,
                bookId = book.id,
                finishedOn = toFinishedPeriod(command.finishedYear, command.finishedMonth),
                rating = command.rating?.let { Rating(it) },
                review = command.review,
            )
        return readingEntryRepository.save(entry) to book
    }
}

/**
 * Use Case: edit my reading entry
 */
@Service
class UpdateReadingEntry(
    private val readingEntryRepository: ReadingEntryRepository,
    private val bookRepository: BookRepository,
) {
    fun execute(command: UpdateReadingEntryCommand): Pair<ReadingEntry, Book> {
        val entry = loadOwned(readingEntryRepository, command.entryId, command.readerId)
        val updated =
            entry.update(
                finishedOn = toFinishedPeriod(command.finishedYear, command.finishedMonth),
                rating = command.rating?.let { Rating(it) },
                review = command.review,
            )
        val saved = readingEntryRepository.save(updated)
        val book = bookRepository.findById(saved.bookId) ?: throw ReadingEntryNotFoundException("Book of entry missing")
        return saved to book
    }
}

/**
 * Use Case: delete my reading entry
 */
@Service
class DeleteReadingEntry(
    private val readingEntryRepository: ReadingEntryRepository,
) {
    fun execute(
        entryId: ReadingEntryId,
        readerId: UserId,
    ) {
        loadOwned(readingEntryRepository, entryId, readerId)
        readingEntryRepository.deleteById(entryId)
    }
}

/**
 * Use Case: get one of my reading entries
 */
@Service
class GetReadingEntry(
    private val readingEntryRepository: ReadingEntryRepository,
    private val bookRepository: BookRepository,
) {
    fun execute(
        entryId: ReadingEntryId,
        readerId: UserId,
    ): ReadingEntryDto {
        val entry = loadOwned(readingEntryRepository, entryId, readerId)
        val book = bookRepository.findById(entry.bookId) ?: throw ReadingEntryNotFoundException("Book of entry missing")
        return ReadingEntryDto.fromDomain(entry, book)
    }
}

/**
 * Use Case: list my reading entries with filters
 */
@Service
class ListReadingEntries(
    private val readingEntryRepository: ReadingEntryRepository,
    private val bookRepository: BookRepository,
) {
    fun execute(query: ListReadingEntriesQuery): PagedResponse<ReadingEntryDto> {
        val filter =
            ReadingEntryFilter(
                year = query.year,
                rating = query.rating,
                searchTerm = query.searchTerm?.trim()?.takeIf { it.isNotEmpty() },
                bookId = query.bookId,
            )
        val pageSize = query.pageSize.coerceIn(1, 100)
        val (entries, total) = readingEntryRepository.findByReader(query.readerId, filter, query.page.coerceAtLeast(0), pageSize)
        val dtos = withBooks(bookRepository, entries)
        return PagedResponse.of(dtos, query.page, pageSize, total)
    }
}

/**
 * Use Case: yearly reading summary
 */
@Service
class GetReadingSummary(
    private val readingEntryRepository: ReadingEntryRepository,
    private val bookRepository: BookRepository,
) {
    fun execute(
        readerId: UserId,
        year: Int? = null,
    ): ReadingSummaryDto {
        val targetYear = year ?: Year.now().value
        val entries = readingEntryRepository.findByReaderAndYear(readerId, targetYear)
        val ratings = entries.mapNotNull { it.rating?.value }
        val topRated =
            entries
                .filter { it.rating != null }
                .sortedWith(compareByDescending<ReadingEntry> { it.rating!!.value }.thenByDescending { it.finishedOn })
                .take(3)
        return ReadingSummaryDto(
            year = targetYear,
            booksThisYear = entries.size,
            booksTotal = readingEntryRepository.countByReader(readerId),
            averageRating = if (ratings.isEmpty()) null else ratings.average(),
            topRated = withBooks(bookRepository, topRated),
            years = readingEntryRepository.findDistinctYears(readerId),
        )
    }
}

internal fun loadOwned(
    repository: ReadingEntryRepository,
    entryId: ReadingEntryId,
    readerId: UserId,
): ReadingEntry {
    val entry = repository.findById(entryId) ?: throw ReadingEntryNotFoundException("Reading entry not found")
    // Entries are strictly personal: another user's entry is reported as not found
    if (!entry.belongsTo(readerId)) throw ReadingEntryNotFoundException("Reading entry not found")
    return entry
}

internal fun withBooks(
    bookRepository: BookRepository,
    entries: List<ReadingEntry>,
): List<ReadingEntryDto> {
    val books = bookRepository.findByIds(entries.map { it.bookId }.toSet())
    return entries.mapNotNull { entry -> books[entry.bookId]?.let { ReadingEntryDto.fromDomain(entry, it) } }
}
