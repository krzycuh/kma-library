package pl.kmazurek.library.infrastructure.catalog

import org.slf4j.LoggerFactory
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Asks external catalogs in order and returns the first hit. The next catalog is asked when the previous one
 * does not know the ISBN or is unavailable. "Not found" only when every catalog answered "not found";
 * if none found it and at least one was unavailable, the lookup is reported as unavailable (so it is not cached).
 */
class FallbackBookMetadataProvider(
    private val providers: List<BookMetadataProvider>,
) : BookMetadataProvider {
    private val logger = LoggerFactory.getLogger(FallbackBookMetadataProvider::class.java)

    init {
        require(providers.isNotEmpty()) { "At least one metadata provider is required" }
    }

    override fun lookupByIsbn(isbn: Isbn): BookMetadata? {
        var failure: BookMetadataUnavailableException? = null
        for (provider in providers) {
            try {
                provider.lookupByIsbn(isbn)?.let { return it }
            } catch (ex: BookMetadataUnavailableException) {
                logger.info("{} unavailable for {}, trying the next catalog", provider::class.simpleName, isbn)
                failure = ex
            }
        }
        failure?.let { throw BookMetadataUnavailableException("No external catalog could be asked about ISBN ${isbn.value}", it) }
        return null
    }
}
