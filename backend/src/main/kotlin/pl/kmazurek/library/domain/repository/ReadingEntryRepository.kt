package pl.kmazurek.library.domain.repository

import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.reading.ReadingEntry
import pl.kmazurek.library.domain.model.reading.ReadingEntryId
import pl.kmazurek.library.domain.model.user.UserId

data class ReadingEntryFilter(
    val year: Int? = null,
    val rating: Int? = null,
    val searchTerm: String? = null,
    val bookId: BookId? = null,
)

/**
 * Repository interface (port) for the ReadingEntry aggregate
 */
interface ReadingEntryRepository {
    fun findById(id: ReadingEntryId): ReadingEntry?

    /**
     * Entries of a reader, newest finished first (unknown dates last). Returns (entries, totalCount).
     */
    fun findByReader(
        readerId: UserId,
        filter: ReadingEntryFilter,
        page: Int,
        pageSize: Int,
    ): Pair<List<ReadingEntry>, Long>

    fun findByReaderAndYear(
        readerId: UserId,
        year: Int,
    ): List<ReadingEntry>

    fun findDistinctYears(readerId: UserId): List<Int>

    fun countByReader(readerId: UserId): Long

    fun save(entry: ReadingEntry): ReadingEntry

    fun deleteById(id: ReadingEntryId)
}
