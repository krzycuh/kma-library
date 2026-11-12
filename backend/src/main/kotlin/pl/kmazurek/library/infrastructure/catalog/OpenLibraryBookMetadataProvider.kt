package pl.kmazurek.library.infrastructure.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
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
 * Open Library adapter: GET /api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data
 * (one call returns title, author names, publish date, pages and cover links; unknown ISBN = empty object).
 * Title/author search: GET /search.json?title=&author=&fields=... – one hit per work, with the median page count
 * over its editions (`number_of_pages_median`), which is just right for an approximate value.
 */
class OpenLibraryBookMetadataProvider(
    private val restClient: RestClient,
) : BookMetadataProvider {
    private val logger = LoggerFactory.getLogger(OpenLibraryBookMetadataProvider::class.java)

    override fun lookupByIsbn(isbn: Isbn): BookMetadata? {
        val key = "ISBN:${isbn.value}"
        val response =
            try {
                retryOnceOnServerError {
                    restClient.get()
                        .uri {
                            it.path("/api/books")
                                .queryParam("bibkeys", key)
                                .queryParam("format", "json")
                                .queryParam("jscmd", "data")
                                .build()
                        }
                        .retrieve()
                        .body(object : ParameterizedTypeReference<Map<String, OpenLibraryBook>>() {})
                }
            } catch (ex: HttpClientErrorException.NotFound) {
                return null
            } catch (ex: RestClientException) {
                logger.warn("Open Library lookup failed for {}: {}", isbn, ex.message)
                throw BookMetadataUnavailableException("Open Library is unavailable", ex)
            }
        return response?.get(key)?.toMetadata(isbn)
    }

    override fun searchByTitleAndAuthor(
        title: String,
        author: String?,
    ): List<BookCandidate> {
        val response =
            try {
                retryOnceOnServerError {
                    restClient.get()
                        .uri {
                            it.path("/search.json").queryParam("title", title)
                            author?.let { a -> it.queryParam("author", a) }
                            it.queryParam("fields", "key,title,author_name,number_of_pages_median")
                                .queryParam("limit", SEARCH_LIMIT)
                                .build()
                        }
                        .retrieve()
                        .body(OpenLibrarySearch::class.java)
                }
            } catch (ex: HttpClientErrorException.NotFound) {
                return emptyList()
            } catch (ex: RestClientException) {
                logger.warn("Open Library search failed for '{}' / '{}': {}", title, author, ex.message)
                throw BookMetadataUnavailableException("Open Library is unavailable", ex)
            }
        return response?.docs.orEmpty().mapNotNull { doc ->
            val docTitle = doc.title?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            BookCandidate(
                title = docTitle,
                authors = doc.authorName.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
                pageCount = doc.numberOfPagesMedian?.takeIf { it > 0 },
                source = BookSource.OPEN_LIBRARY,
            )
        }
    }

    private fun OpenLibraryBook.toMetadata(isbn: Isbn): BookMetadata? {
        val fullTitle = listOfNotNull(title?.trim(), subtitle?.trim()).filter { it.isNotEmpty() }.joinToString(": ")
        if (fullTitle.isEmpty()) return null
        return BookMetadata(
            isbn = isbn.value,
            title = fullTitle,
            authors = authors.orEmpty().mapNotNull { it.name?.trim()?.takeIf { name -> name.isNotEmpty() } },
            publishedYear = PublishedYearParser.parse(publishDate),
            coverUrl = (cover?.medium ?: cover?.large ?: cover?.small)?.let(::toHttps),
            source = BookSource.OPEN_LIBRARY,
            externalId = this.key?.removePrefix("/books/"),
            pageCount = numberOfPages?.takeIf { it > 0 },
        )
    }

    companion object {
        private const val SEARCH_LIMIT = 5
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenLibraryBook(
        val key: String? = null,
        val title: String? = null,
        val subtitle: String? = null,
        val authors: List<OpenLibraryAuthor>? = null,
        @JsonProperty("publish_date") val publishDate: String? = null,
        @JsonProperty("number_of_pages") val numberOfPages: Int? = null,
        val cover: OpenLibraryCover? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenLibrarySearch(val docs: List<OpenLibraryDoc>? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenLibraryDoc(
        val title: String? = null,
        @JsonProperty("author_name") val authorName: List<String>? = null,
        @JsonProperty("number_of_pages_median") val numberOfPagesMedian: Int? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenLibraryAuthor(val name: String? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenLibraryCover(
        val small: String? = null,
        val medium: String? = null,
        val large: String? = null,
    )
}

/**
 * Catalog APIs still return some http:// links; the app is served over HTTPS (mixed content).
 */
internal fun toHttps(url: String): String = if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
