package pl.kmazurek.library.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
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
