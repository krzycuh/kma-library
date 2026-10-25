package pl.kmazurek.library.application.usecase.catalog

import org.springframework.stereotype.Service
import pl.kmazurek.library.application.dto.BookReference
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.catalog.CoverUrl
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
    val coverUrl: String? = null,
)

/**
 * Parsed data for a catalog entry that may not exist yet.
 */
data class BookDraft(
    val title: BookTitle,
    val authors: List<AuthorName>,
    val isbn: Isbn? = null,
    val publishedYear: Int? = null,
    val coverUrl: String? = null,
    val source: BookSource = BookSource.MANUAL,
    val externalId: String? = null,
)

/**
 * Use Case: resolve a catalog entry. Deduplicates by ISBN, then by normalized title + authors.
 */
@Service
class FindOrCreateBook(
    private val bookRepository: BookRepository,
) {
    fun execute(command: FindOrCreateBookCommand): Book {
        val authors = AuthorName.parseList(command.authors)
        require(authors.isNotEmpty()) { "At least one author is required" }
        return findOrCreate(
            BookDraft(
                title = BookTitle.of(command.title),
                authors = authors,
                isbn = command.isbn?.takeIf { it.isNotBlank() }?.let { Isbn.parse(it) },
                publishedYear = command.publishedYear,
                coverUrl = CoverUrl.parseOrNull(command.coverUrl),
            ),
            createdBy = command.createdBy,
        )
    }

    /**
     * Same deduplication for an already-parsed draft (e.g. built from external catalog metadata,
     * where author names may contain commas and must not be re-split).
     */
    fun findOrCreate(
        draft: BookDraft,
        createdBy: UserId,
    ): Book {
        draft.isbn?.let { bookRepository.findByIsbn(it) }?.let { return it }

        val key = BookDeduplicationKey.of(draft.title, draft.authors)
        bookRepository.findByNormalizedKey(key)?.let { existing ->
            // Enrich an existing entry with data it lacks (ISBN, year, cover); nothing it has is overwritten
            val enriched = existing.fillGapsFrom(draft.isbn, draft.publishedYear, draft.coverUrl, draft.source, draft.externalId)
            return if (enriched === existing) existing else bookRepository.save(enriched)
        }

        return bookRepository.save(
            Book.create(
                title = draft.title,
                authors = draft.authors,
                isbn = draft.isbn,
                publishedYear = draft.publishedYear,
                coverUrl = draft.coverUrl,
                source = draft.source,
                externalId = draft.externalId,
                createdByUserId = createdBy,
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
                coverUrl = reference.coverUrl,
                createdBy = requestedBy,
            ),
        )
    }
}
