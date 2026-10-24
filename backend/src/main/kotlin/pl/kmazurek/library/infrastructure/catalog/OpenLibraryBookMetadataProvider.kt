package pl.kmazurek.library.infrastructure.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Open Library adapter: GET /api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data
 * (one call returns title, author names, publish date and cover links; unknown ISBN = empty object).
 */
class OpenLibraryBookMetadataProvider(
    private val restClient: RestClient,
) : BookMetadataProvider {
    private val logger = LoggerFactory.getLogger(OpenLibraryBookMetadataProvider::class.java)

    override fun lookupByIsbn(isbn: Isbn): BookMetadata? {
        val key = "ISBN:${isbn.value}"
        val response =
            try {
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
            } catch (ex: HttpClientErrorException.NotFound) {
                return null
            } catch (ex: RestClientException) {
                logger.warn("Open Library lookup failed for {}: {}", isbn, ex.message)
                throw BookMetadataUnavailableException("Open Library is unavailable", ex)
            }
        return response?.get(key)?.toMetadata(isbn)
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
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenLibraryBook(
        val key: String? = null,
        val title: String? = null,
        val subtitle: String? = null,
        val authors: List<OpenLibraryAuthor>? = null,
        @JsonProperty("publish_date") val publishDate: String? = null,
        val cover: OpenLibraryCover? = null,
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
