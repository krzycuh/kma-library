package pl.kmazurek.library.application.usecase.catalog

import org.springframework.stereotype.Service
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.catalog.Isbn
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository

/**
 * What we know about an ISBN: either a book already in our catalog, or metadata from an external catalog.
 */
data class BookLookupResult(
    val isbn: Isbn,
    val existingBook: Book?,
    val metadata: BookMetadata?,
)

/**
 * Use Case: look up an ISBN – local catalog first, then the external metadata provider. Does not save anything.
 */
@Service
class LookupBookByIsbn(
    private val bookRepository: BookRepository,
    private val metadataProvider: BookMetadataProvider,
) {
    fun execute(rawIsbn: String): BookLookupResult {
        val isbn = Isbn.parse(rawIsbn)
        bookRepository.findByIsbn(isbn)?.let { return BookLookupResult(isbn, it, null) }
        val metadata =
            metadataProvider.lookupByIsbn(isbn)
                ?: throw BookNotFoundException("No book found for ISBN ${isbn.value}")
        return BookLookupResult(isbn, null, metadata)
    }
}

/**
 * Use Case: make sure the catalog has an entry for an ISBN, creating it from external metadata if needed.
 */
@Service
class ImportBookByIsbn(
    private val lookupBookByIsbn: LookupBookByIsbn,
    private val findOrCreateBook: FindOrCreateBook,
) {
    fun execute(
        rawIsbn: String,
        requestedBy: UserId,
    ): Book {
        val result = lookupBookByIsbn.execute(rawIsbn)
        result.existingBook?.let { return it }
        return findOrCreateBook.findOrCreate(toDraft(result.isbn, result.metadata!!), requestedBy)
    }

    private fun toDraft(
        isbn: Isbn,
        metadata: BookMetadata,
    ): BookDraft {
        val title =
            metadata.title.trim().take(BookTitle.MAX_LENGTH).trim()
                .ifEmpty { throw IncompleteBookMetadataException("External catalog has no title for ISBN ${isbn.value}") }
        // Stored authors are ';'-separated, so a ';' inside a name would split it on read
        val authors =
            metadata.authors
                .map { it.replace(';', ',').trim().take(AuthorName.MAX_LENGTH).trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .map { AuthorName(it) }
        if (authors.isEmpty()) {
            throw IncompleteBookMetadataException("External catalog has no author for ISBN ${isbn.value}")
        }
        return BookDraft(
            title = BookTitle(title),
            authors = authors,
            isbn = isbn,
            publishedYear = metadata.publishedYear?.takeIf { it in Book.MIN_YEAR..Book.MAX_YEAR },
            coverUrl = metadata.coverUrl?.takeIf { it.length <= MAX_COVER_URL_LENGTH },
            source = metadata.source,
            externalId = metadata.externalId?.takeIf { it.length <= MAX_EXTERNAL_ID_LENGTH },
        )
    }

    companion object {
        private const val MAX_COVER_URL_LENGTH = 500
        private const val MAX_EXTERNAL_ID_LENGTH = 100
    }
}
