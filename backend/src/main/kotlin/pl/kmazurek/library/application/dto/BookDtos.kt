package pl.kmazurek.library.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.usecase.catalog.BookLookupResult
import pl.kmazurek.library.application.usecase.catalog.MergeBooksResult
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookSource
import java.time.LocalDateTime

data class BookDto(
    val id: String,
    val title: String,
    val authors: List<String>,
    val isbn: String?,
    val publishedYear: Int?,
    val coverUrl: String?,
    val source: BookSource,
    val createdByUserId: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(book: Book): BookDto =
            BookDto(
                id = book.id.toString(),
                title = book.title.value,
                authors = book.authors.map { it.value },
                isbn = book.isbn?.value,
                publishedYear = book.publishedYear,
                coverUrl = book.coverUrl,
                source = book.source,
                createdByUserId = book.createdByUserId?.toString(),
                createdAt = book.createdAt,
            )
    }
}

/**
 * Either an existing catalog book (bookId) or data to create one (title + authors).
 */
data class BookReference(
    val bookId: String? = null,
    @field:Size(max = 500, message = "Title cannot exceed 500 characters")
    val title: String? = null,
    @field:Size(max = 1000, message = "Authors cannot exceed 1000 characters")
    val authors: String? = null,
    @field:Size(max = 20, message = "ISBN cannot exceed 20 characters")
    val isbn: String? = null,
    val publishedYear: Int? = null,
)

data class CreateBookRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 500, message = "Title cannot exceed 500 characters")
    val title: String,
    @field:NotBlank(message = "Author is required")
    @field:Size(max = 1000, message = "Authors cannot exceed 1000 characters")
    val authors: String,
    @field:Size(max = 20, message = "ISBN cannot exceed 20 characters")
    val isbn: String? = null,
    val publishedYear: Int? = null,
)

data class UpdateBookRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 500, message = "Title cannot exceed 500 characters")
    val title: String,
    @field:NotBlank(message = "Author is required")
    @field:Size(max = 1000, message = "Authors cannot exceed 1000 characters")
    val authors: String,
    @field:Size(max = 20, message = "ISBN cannot exceed 20 characters")
    val isbn: String? = null,
    val publishedYear: Int? = null,
    @field:Size(max = 500, message = "Cover URL cannot exceed 500 characters")
    val coverUrl: String? = null,
)

data class BookMetadataDto(
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val publishedYear: Int?,
    val coverUrl: String?,
    val source: BookSource,
) {
    companion object {
        fun fromMetadata(metadata: BookMetadata): BookMetadataDto =
            BookMetadataDto(
                isbn = metadata.isbn,
                title = metadata.title,
                authors = metadata.authors,
                publishedYear = metadata.publishedYear,
                coverUrl = metadata.coverUrl,
                source = metadata.source,
            )
    }
}

/**
 * ISBN lookup: `book` when the ISBN is already in our catalog, otherwise `metadata` from the external catalog.
 */
data class BookLookupDto(
    val isbn: String,
    val book: BookDto?,
    val metadata: BookMetadataDto?,
) {
    companion object {
        fun fromResult(result: BookLookupResult): BookLookupDto =
            BookLookupDto(
                isbn = result.isbn.value,
                book = result.existingBook?.let { BookDto.fromDomain(it) },
                metadata = result.metadata?.let { BookMetadataDto.fromMetadata(it) },
            )
    }
}

data class ImportBookRequest(
    @field:NotBlank(message = "ISBN is required")
    @field:Size(max = 20, message = "ISBN cannot exceed 20 characters")
    val isbn: String,
)

data class MergeBooksResultDto(
    val target: BookDto,
    val movedReadingEntries: Int,
    val movedShelfItems: Int,
) {
    companion object {
        fun fromResult(result: MergeBooksResult): MergeBooksResultDto =
            MergeBooksResultDto(BookDto.fromDomain(result.target), result.movedReadingEntries, result.movedShelfItems)
    }
}
