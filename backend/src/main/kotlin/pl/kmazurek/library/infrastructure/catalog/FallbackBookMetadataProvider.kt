package pl.kmazurek.library.infrastructure.catalog

import org.slf4j.LoggerFactory
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Asks external catalogs in order (name → provider) and returns the first hit. The next catalog is asked when
 * the previous one does not know the ISBN or is unavailable. "Not found" only when every catalog answered
 * "not found"; if none found it and at least one was unavailable, the lookup is reported as unavailable
 * (so it is not cached and a retry asks again).
 */
class FallbackBookMetadataProvider(
    private val providers: Map<String, BookMetadataProvider>,
) : BookMetadataProvider {
    private val logger = LoggerFactory.getLogger(FallbackBookMetadataProvider::class.java)

    init {
        require(providers.isNotEmpty()) { "At least one metadata provider is required" }
    }

    override fun lookupByIsbn(isbn: Isbn): BookMetadata? {
        val notFound = mutableListOf<String>()
        val unavailable = mutableListOf<String>()
        var lastFailure: BookMetadataUnavailableException? = null
        for ((name, provider) in providers) {
            try {
                val metadata = provider.lookupByIsbn(isbn)
                if (metadata != null) {
                    logger.info(
                        "ISBN {}: found in {} (not found: {}, unavailable: {})",
                        isbn,
                        name,
                        notFound.orNone(),
                        unavailable.orNone(),
                    )
                    return metadata
                }
                notFound += name
            } catch (ex: BookMetadataUnavailableException) {
                unavailable += name
                lastFailure = ex
            }
        }
        logger.info("ISBN {}: not found in {}, unavailable: {}", isbn, notFound.orNone(), unavailable.orNone())
        if (lastFailure != null) {
            throw BookMetadataUnavailableException(
                "ISBN ${isbn.value} not found in ${notFound.orNone()}; unavailable: ${unavailable.joinToString()}",
                lastFailure,
            )
        }
        return null
    }

    private fun List<String>.orNone() = if (isEmpty()) "-" else joinToString()
}
