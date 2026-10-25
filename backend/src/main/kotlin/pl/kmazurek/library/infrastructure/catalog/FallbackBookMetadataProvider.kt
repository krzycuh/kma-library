package pl.kmazurek.library.infrastructure.catalog

import org.slf4j.LoggerFactory
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Asks external catalogs in order (name → provider) until the combined answer is complete
 * (authors, cover and year), then merges what they returned: catalogs often know only part of a book
 * (Open Library without the author, Biblioteka Narodowa without a cover).
 *
 * The main record is the first one with authors; missing cover and year are taken from the others, in order.
 * Unavailable catalogs are skipped. The lookup is reported as unavailable (so it is not cached and a retry
 * asks again) only when no usable record was found and some catalog could not be asked.
 */
class FallbackBookMetadataProvider(
    private val providers: Map<String, BookMetadataProvider>,
) : BookMetadataProvider {
    private val logger = LoggerFactory.getLogger(FallbackBookMetadataProvider::class.java)

    init {
        require(providers.isNotEmpty()) { "At least one metadata provider is required" }
    }

    override fun lookupByIsbn(isbn: Isbn): BookMetadata? {
        val found = linkedMapOf<String, BookMetadata>()
        val notFound = mutableListOf<String>()
        val unavailable = mutableListOf<String>()
        var lastFailure: BookMetadataUnavailableException? = null
        for ((name, provider) in providers) {
            if (merge(found.values)?.isComplete() == true) break
            try {
                provider.lookupByIsbn(isbn)?.let { found[name] = it } ?: notFound.add(name)
            } catch (ex: BookMetadataUnavailableException) {
                unavailable += name
                lastFailure = ex
            }
        }

        val merged = merge(found.values)
        logger.info(
            "ISBN {}: found in {}, not found in {}, unavailable: {}{}",
            isbn,
            found.keys.toList().orNone(),
            notFound.orNone(),
            unavailable.orNone(),
            merged?.let { m -> " -> authors: ${m.authors.isNotEmpty()}, cover: ${m.coverUrl != null}, year: ${m.publishedYear != null}" }
                .orEmpty(),
        )
        if (lastFailure != null && (merged == null || merged.authors.isEmpty())) {
            throw BookMetadataUnavailableException(
                "ISBN ${isbn.value}: no complete record in ${(found.keys + notFound).toList().orNone()}; " +
                    "unavailable: ${unavailable.joinToString()}",
                lastFailure,
            )
        }
        return merged
    }

    private fun BookMetadata.isComplete() = authors.isNotEmpty() && coverUrl != null && publishedYear != null

    private fun List<String>.orNone() = if (isEmpty()) "-" else joinToString()

    companion object {
        internal fun merge(records: Collection<BookMetadata>): BookMetadata? {
            val main = records.firstOrNull { it.authors.isNotEmpty() } ?: records.firstOrNull() ?: return null
            val others = records.filter { it !== main }
            return main.copy(
                coverUrl = main.coverUrl ?: others.firstNotNullOfOrNull { it.coverUrl },
                publishedYear = main.publishedYear ?: others.firstNotNullOfOrNull { it.publishedYear },
            )
        }
    }
}
