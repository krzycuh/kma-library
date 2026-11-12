package pl.kmazurek.library.infrastructure.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import pl.kmazurek.library.application.service.BookCandidate
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Google Books adapter: GET /books/v1/volumes?q=isbn:{isbn} (API key optional, raises the quota).
 * Title/author search: the same endpoint with q=intitle:"..." inauthor:"...".
 */
class GoogleBooksBookMetadataProvider(
    private val restClient: RestClient,
    private val apiKey: String?,
) : BookMetadataProvider {
    private val logger = LoggerFactory.getLogger(GoogleBooksBookMetadataProvider::class.java)

    override fun lookupByIsbn(isbn: Isbn): BookMetadata? {
        val response = volumes("isbn:${isbn.value}", maxResults = null, what = isbn.value) ?: return null
        val volume = response.items?.firstOrNull() ?: return null
        val info = volume.volumeInfo ?: return null
        val fullTitle = listOfNotNull(info.title?.trim(), info.subtitle?.trim()).filter { it.isNotEmpty() }.joinToString(": ")
        if (fullTitle.isEmpty()) return null
        return BookMetadata(
            isbn = isbn.value,
            title = fullTitle,
            authors = info.authors.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
            publishedYear = PublishedYearParser.parse(info.publishedDate),
            coverUrl = (info.imageLinks?.thumbnail ?: info.imageLinks?.smallThumbnail)?.let(::toHttps),
            source = BookSource.GOOGLE_BOOKS,
            externalId = volume.id,
            pageCount = info.pageCount?.takeIf { it > 0 },
        )
    }

    override fun searchByTitleAndAuthor(
        title: String,
        author: String?,
    ): List<BookCandidate> {
        val query = listOfNotNull("intitle:\"$title\"", author?.let { "inauthor:\"$it\"" }).joinToString(" ")
        val response = volumes(query, maxResults = SEARCH_LIMIT, what = "'$title' / '$author'") ?: return emptyList()
        return response.items.orEmpty().mapNotNull { volume ->
            val info = volume.volumeInfo ?: return@mapNotNull null
            val hitTitle = listOfNotNull(info.title?.trim(), info.subtitle?.trim()).filter { it.isNotEmpty() }.joinToString(": ")
            if (hitTitle.isEmpty()) return@mapNotNull null
            BookCandidate(
                title = hitTitle,
                authors = info.authors.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
                pageCount = info.pageCount?.takeIf { it > 0 },
                source = BookSource.GOOGLE_BOOKS,
            )
        }
    }

    /** GET /books/v1/volumes?q=...; null on 404. [what] names the lookup in the log. */
    private fun volumes(
        query: String,
        maxResults: Int?,
        what: String,
    ): GoogleVolumes? =
        try {
            retryOnceOnServerError {
                restClient.get()
                    .uri {
                        it.path("/books/v1/volumes").queryParam("q", query)
                        maxResults?.let { n -> it.queryParam("maxResults", n) }
                        apiKey?.takeIf { key -> key.isNotBlank() }?.let { key -> it.queryParam("key", key) }
                        it.build()
                    }
                    .retrieve()
                    .body(GoogleVolumes::class.java)
            }
        } catch (ex: HttpClientErrorException.NotFound) {
            null
        } catch (ex: RestClientException) {
            logger.warn("Google Books lookup failed for {}: {}", what, ex.message)
            throw BookMetadataUnavailableException("Google Books is unavailable", ex)
        }

    companion object {
        private const val SEARCH_LIMIT = 5
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GoogleVolumes(val items: List<GoogleVolume>? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GoogleVolume(
        val id: String? = null,
        val volumeInfo: GoogleVolumeInfo? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GoogleVolumeInfo(
        val title: String? = null,
        val subtitle: String? = null,
        val authors: List<String>? = null,
        val publishedDate: String? = null,
        val pageCount: Int? = null,
        val imageLinks: GoogleImageLinks? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GoogleImageLinks(
        val smallThumbnail: String? = null,
        val thumbnail: String? = null,
    )
}
