package pl.kmazurek.library.application.service

import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Bibliographic data found in an external catalog (Open Library, Google Books).
 */
data class BookMetadata(
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val publishedYear: Int?,
    val coverUrl: String?,
    val source: BookSource,
    val externalId: String?,
    val pageCount: Int? = null,
)

/**
 * One hit of a title/author search in an external catalog: enough to recognise the book and read its page count.
 * Open Library reports the median over editions; the others the pages of the edition they matched.
 */
data class BookCandidate(
    val title: String,
    val authors: List<String>,
    val pageCount: Int?,
    val source: BookSource,
)

/**
 * Port: looks up a book in an external catalog.
 * Returns null when the catalog does not know the ISBN; throws BookMetadataUnavailableException
 * when the catalog cannot be asked (timeout, 5xx, network).
 */
interface BookMetadataProvider {
    fun lookupByIsbn(isbn: Isbn): BookMetadata?

    /**
     * Candidates for a book known only by title (and author), best match first. Used to find page counts
     * for catalog entries without an ISBN; a catalog that cannot search answers with an empty list.
     * Throws BookMetadataUnavailableException when the catalog cannot be asked.
     */
    fun searchByTitleAndAuthor(
        title: String,
        author: String?,
    ): List<BookCandidate> = emptyList()
}

class BookMetadataUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
