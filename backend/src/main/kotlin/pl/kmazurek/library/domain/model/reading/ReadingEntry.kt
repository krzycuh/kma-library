package pl.kmazurek.library.domain.model.reading

import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.user.UserId
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * ReadingEntry Aggregate Root (Reading context)
 * One record of a reader finishing a book. Re-reads are separate entries.
 */
data class ReadingEntry(
    val id: ReadingEntryId,
    val readerId: UserId,
    val bookId: BookId,
    val status: ReadingStatus = ReadingStatus.FINISHED,
    val finishedOn: FinishedPeriod? = null,
    val rating: Rating? = null,
    val review: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        finishedOn?.let {
            require(!it.isAfter(YearMonth.now())) { "Finished period cannot be in the future" }
        }
        review?.let { require(it.length <= MAX_REVIEW_LENGTH) { "Review cannot exceed $MAX_REVIEW_LENGTH characters" } }
    }

    fun update(
        finishedOn: FinishedPeriod?,
        rating: Rating?,
        review: String?,
    ): ReadingEntry =
        copy(
            finishedOn = finishedOn,
            rating = rating,
            review = review?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = LocalDateTime.now(),
        )

    fun belongsTo(userId: UserId) = readerId == userId

    companion object {
        const val MAX_REVIEW_LENGTH = 5000

        fun create(
            readerId: UserId,
            bookId: BookId,
            finishedOn: FinishedPeriod? = null,
            rating: Rating? = null,
            review: String? = null,
        ): ReadingEntry {
            val now = LocalDateTime.now()
            return ReadingEntry(
                id = ReadingEntryId.generate(),
                readerId = readerId,
                bookId = bookId,
                status = ReadingStatus.FINISHED,
                finishedOn = finishedOn,
                rating = rating,
                review = review?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
