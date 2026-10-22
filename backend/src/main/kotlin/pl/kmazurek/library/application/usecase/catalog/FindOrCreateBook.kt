package pl.kmazurek.library.application.usecase.catalog

import org.springframework.stereotype.Service
import pl.kmazurek.library.application.dto.BookReference
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.catalog.Isbn
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.service.BookDeduplicationKey

data class FindOrCreateBookCommand(
    val title: String,
    val authors: String,
    val isbn: String? = null,
    val publishedYear: Int? = null,
    val createdBy: UserId,
)

/**
 * Use Case: resolve a catalog entry. Deduplicates by ISBN, then by normalized title + authors.
 */
@Service
class FindOrCreateBook(
    private val bookRepository: BookRepository,
) {
    fun execute(command: FindOrCreateBookCommand): Book {
        val title = BookTitle.of(command.title)
        val authors = AuthorName.parseList(command.authors)
        require(authors.isNotEmpty()) { "At least one author is required" }
        val isbn = command.isbn?.takeIf { it.isNotBlank() }?.let { Isbn.parse(it) }

        isbn?.let { bookRepository.findByIsbn(it) }?.let { return it }

        val key = BookDeduplicationKey.of(title, authors)
        bookRepository.findByNormalizedKey(key)?.let { existing ->
            // Enrich an existing manual entry with an ISBN we did not know before
            if (isbn != null && existing.isbn == null) {
                return bookRepository.save(
                    existing.update(
                        existing.title,
                        existing.authors,
                        isbn,
                        existing.publishedYear ?: command.publishedYear,
                        existing.coverUrl,
                    ),
                )
            }
            return existing
        }

        return bookRepository.save(
            Book.create(
                title = title,
                authors = authors,
                isbn = isbn,
                publishedYear = command.publishedYear,
                createdByUserId = command.createdBy,
            ),
        )
    }

    /**
     * Resolves a BookReference coming from the API: existing id or new catalog data.
     */
    fun resolve(
        reference: BookReference,
        requestedBy: UserId,
    ): Book {
        reference.bookId?.takeIf { it.isNotBlank() }?.let { id ->
            return bookRepository.findById(BookId.fromString(id))
                ?: throw BookNotFoundException("Book $id not found")
        }
        val title = reference.title?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Title is required")
        val authors = reference.authors?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Author is required")
        return execute(
            FindOrCreateBookCommand(
                title = title,
                authors = authors,
                isbn = reference.isbn,
                publishedYear = reference.publishedYear,
                createdBy = requestedBy,
            ),
        )
    }
}
