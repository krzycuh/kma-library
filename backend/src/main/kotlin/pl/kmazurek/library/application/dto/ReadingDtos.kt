package pl.kmazurek.library.application.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.reading.ReadingEntry
import pl.kmazurek.library.domain.model.reading.ReadingStatus
import java.time.LocalDateTime

data class ReadingEntryDto(
    val id: String,
    val book: BookDto,
    val status: ReadingStatus,
    val finishedYear: Int?,
    val finishedMonth: Int?,
    val rating: Int?,
    val review: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(
            entry: ReadingEntry,
            book: Book,
        ): ReadingEntryDto =
            ReadingEntryDto(
                id = entry.id.toString(),
                book = BookDto.fromDomain(book),
                status = entry.status,
                finishedYear = entry.finishedOn?.year,
                finishedMonth = entry.finishedOn?.month,
                rating = entry.rating?.value,
                review = entry.review,
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt,
            )
    }
}

data class CreateReadingEntryRequest(
    @field:Valid
    val book: BookReference,
    @field:Min(1900) @field:Max(2200)
    val finishedYear: Int? = null,
    @field:Min(1) @field:Max(12)
    val finishedMonth: Int? = null,
    @field:Min(1) @field:Max(5)
    val rating: Int? = null,
    @field:Size(max = 5000, message = "Review cannot exceed 5000 characters")
    val review: String? = null,
)

data class UpdateReadingEntryRequest(
    @field:Min(1900) @field:Max(2200)
    val finishedYear: Int? = null,
    @field:Min(1) @field:Max(12)
    val finishedMonth: Int? = null,
    @field:Min(1) @field:Max(5)
    val rating: Int? = null,
    @field:Size(max = 5000, message = "Review cannot exceed 5000 characters")
    val review: String? = null,
)

data class ReadingSummaryDto(
    val year: Int,
    val booksThisYear: Int,
    val booksTotal: Long,
    val averageRating: Double?,
    val topRated: List<ReadingEntryDto>,
    val years: List<Int>,
)
