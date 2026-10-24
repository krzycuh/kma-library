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
)

/**
 * Port: looks up a book in an external catalog.
 * Returns null when the catalog does not know the ISBN; throws BookMetadataUnavailableException
 * when the catalog cannot be asked (timeout, 5xx, network).
 */
interface BookMetadataProvider {
    fun lookupByIsbn(isbn: Isbn): BookMetadata?
}

class BookMetadataUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
