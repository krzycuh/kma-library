package pl.kmazurek.library.infrastructure.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Biblioteka Narodowa (data.bn.org.pl) adapter: GET /api/institutions/bibs.json?isbnIssn={isbn}.
 * Best coverage of Polish editions; no covers. Records use library cataloguing conventions
 * ("Tytuł : podtytuł / autor.", "Nazwisko, Imię (1921-2006)"), normalized here to what the other catalogs return.
 */
class NationalLibraryBookMetadataProvider(
    private val restClient: RestClient,
) : BookMetadataProvider {
    private val logger = LoggerFactory.getLogger(NationalLibraryBookMetadataProvider::class.java)

    override fun lookupByIsbn(isbn: Isbn): BookMetadata? {
        val response =
            try {
                retryOnceOnServerError {
                    restClient.get()
                        .uri { it.path("/api/institutions/bibs.json").queryParam("isbnIssn", isbn.value).build() }
                        .retrieve()
                        .body(BnBibs::class.java)
                }
            } catch (ex: HttpClientErrorException.NotFound) {
                return null
            } catch (ex: RestClientException) {
                logger.warn("Biblioteka Narodowa lookup failed for {}: {}", isbn, ex.message)
                throw BookMetadataUnavailableException("Biblioteka Narodowa is unavailable", ex)
            }
        val bibs = response?.bibs.orEmpty()
        // The search may match other ISBNs of the record (e.g. a series); prefer the exact one
        val bib = bibs.firstOrNull { Isbn.parseOrNull(it.isbnIssn)?.value == isbn.value } ?: bibs.firstOrNull() ?: return null
        val title = cleanTitle(bib.title) ?: return null
        return BookMetadata(
            isbn = isbn.value,
            title = title,
            authors = parseAuthors(bib.author),
            publishedYear = PublishedYearParser.parse(bib.publicationYear),
            coverUrl = null,
            source = BookSource.NATIONAL_LIBRARY,
            externalId = bib.id?.toString(),
        )
    }

    companion object {
        private val LIFE_DATES = Regex("""\s*\([^)]*\)""")
        private val TRAILING_PUNCTUATION = Regex("""[\s/:;,.=]+$""")

        /**
         * "Solaris / Stanisław Lem." → "Solaris"; "Dobry omen : prawdziwa…" → "Dobry omen: prawdziwa…"
         */
        internal fun cleanTitle(raw: String?): String? =
            raw?.substringBefore(" / ")
                ?.replace(" : ", ": ")
                ?.replace(TRAILING_PUNCTUATION, "")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        /**
         * "Lem, Stanisław (1921-2006)" → "Stanisław Lem"; several authors separated by '|' or ';'.
         */
        internal fun parseAuthors(raw: String?): List<String> =
            raw.orEmpty()
                .split('|', ';')
                .map { it.replace(LIFE_DATES, "").replace(TRAILING_PUNCTUATION, "").trim() }
                .filter { it.isNotEmpty() }
                .map { name ->
                    val parts = name.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (parts.size == 2) "${parts[1]} ${parts[0]}" else name
                }
                .distinct()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class BnBibs(val bibs: List<BnBib>? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class BnBib(
        val id: Long? = null,
        val title: String? = null,
        val author: String? = null,
        val publicationYear: String? = null,
        val isbnIssn: String? = null,
    )
}
