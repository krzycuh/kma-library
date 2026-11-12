package pl.kmazurek.library.infrastructure.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
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
 * Biblioteka Narodowa (data.bn.org.pl) adapter: GET /api/institutions/bibs.json?isbnIssn={isbn}.
 * Best coverage of Polish editions; no covers.
 *
 * The flat `title`/`author` fields are unusable (they mix the original title in, and glue every person –
 * translators and the publisher too – without a separator), so title and authors come from the MARC record:
 * 245 $a/$n/$p/$b = title, 100 $a = main author, 700 $a with relator $e "Autor" = co-authors.
 * ISBNs are stored without hyphens, older books with the ISBN-10 only – so ISBN-13 first, then ISBN-10.
 * Pages come from 300 $a ("459, [4] strony ;", "48 s. :"): the first number of the physical description.
 * Title/author search: the same endpoint with title= and author= (author in catalogue order, "Lem, Stanisław",
 * so the surname alone is sent).
 */
class NationalLibraryBookMetadataProvider(
    private val restClient: RestClient,
) : BookMetadataProvider {
    private val logger = LoggerFactory.getLogger(NationalLibraryBookMetadataProvider::class.java)

    override fun lookupByIsbn(isbn: Isbn): BookMetadata? {
        val candidates = listOfNotNull(isbn.value, isbn.toIsbn10())
        for (query in candidates) {
            val bib = selectRecord(fetch(isbn, query), isbn) ?: continue
            return toMetadata(bib, isbn)
        }
        return null
    }

    override fun searchByTitleAndAuthor(
        title: String,
        author: String?,
    ): List<BookCandidate> {
        val params = linkedMapOf("title" to title, "limit" to SEARCH_LIMIT.toString())
        // The catalogue indexes "Surname, Given names"; the surname is what both orders share
        author?.trim()?.substringAfterLast(' ')?.takeIf { it.isNotEmpty() }?.let { params["author"] = it }
        return fetch(params, "'$title' / '$author'")
            .filter { it.deleted != true && it.marc != null }
            .mapNotNull { bib ->
                val marc = MarcRecord(bib.marc!!)
                val hitTitle = marc.title() ?: return@mapNotNull null
                BookCandidate(hitTitle, marc.authors(), marc.pageCount(), BookSource.NATIONAL_LIBRARY)
            }
    }

    private fun fetch(
        isbn: Isbn,
        query: String,
    ): List<BnBib> = fetch(mapOf("isbnIssn" to query), isbn.value)

    private fun fetch(
        params: Map<String, String>,
        what: String,
    ): List<BnBib> =
        try {
            retryOnceOnServerError {
                restClient.get()
                    .uri {
                        it.path("/api/institutions/bibs.json")
                        params.forEach { (name, value) -> it.queryParam(name, value) }
                        it.build()
                    }
                    .retrieve()
                    .body(BnBibs::class.java)
            }?.bibs.orEmpty()
        } catch (ex: HttpClientErrorException.NotFound) {
            emptyList()
        } catch (ex: RestClientException) {
            logger.warn("Biblioteka Narodowa lookup failed for {}: {}", what, ex.message)
            throw BookMetadataUnavailableException("Biblioteka Narodowa is unavailable", ex)
        }

    private fun selectRecord(
        bibs: List<BnBib>,
        isbn: Isbn,
    ): BnBib? {
        val live = bibs.filter { it.deleted != true && it.marc != null }
        return live.firstOrNull { bib -> bib.isbnIssn.orEmpty().split(' ').any { Isbn.parseOrNull(it) == isbn } }
            ?: live.firstOrNull()
    }

    private fun toMetadata(
        bib: BnBib,
        isbn: Isbn,
    ): BookMetadata? {
        val marc = MarcRecord(bib.marc!!)
        val title = marc.title() ?: return null
        return BookMetadata(
            isbn = isbn.value,
            title = title,
            authors = marc.authors(),
            publishedYear = PublishedYearParser.parse(bib.publicationYear),
            coverUrl = null,
            source = BookSource.NATIONAL_LIBRARY,
            externalId = bib.id?.toString(),
            pageCount = marc.pageCount(),
        )
    }

    /**
     * MARC-in-JSON as served by data.bn.org.pl: {"fields": [{"001": "..."}, {"245": {"subfields": [{"a": "..."}]}}]}
     */
    internal class MarcRecord(private val marc: JsonNode) {
        private fun dataFields(tag: String): List<List<Pair<String, String>>> =
            marc.path("fields")
                .filter { it.has(tag) }
                .map { field ->
                    field.path(tag).path("subfields").flatMap { subfield ->
                        subfield.fields().asSequence().map { (code, value) -> code to value.asText() }.toList()
                    }
                }

        fun title(): String? {
            val subfields = dataFields("245").firstOrNull() ?: return null
            val title = StringBuilder()
            for ((code, value) in subfields) {
                val part = clean(value).takeIf { it.isNotEmpty() } ?: continue
                when (code) {
                    "a" -> title.append(part)
                    "n", "p" -> title.append(if (title.isEmpty()) part else ". $part")
                    "b" -> title.append(if (title.isEmpty()) part else ": $part")
                }
            }
            return title.toString().takeIf { it.isNotEmpty() }
        }

        fun authors(): List<String> {
            val main = dataFields("100").mapNotNull { it.firstValue("a") }
            val coAuthors =
                dataFields("700")
                    .filter { field -> field.any { (code, value) -> code == "e" && clean(value).equals("Autor", ignoreCase = true) } }
                    .mapNotNull { it.firstValue("a") }
            val names = (main + coAuthors).map { personName(it) }.filter { it.isNotEmpty() }.distinct()
            return names.ifEmpty { statementOfResponsibility() }
        }

        /**
         * 300 $a, e.g. "459, [4] strony ;" → 459, "48 s. :" → 48, "[8], 312, [3] s." → 312:
         * the largest number outside brackets (bracketed ones are unnumbered leaves).
         */
        fun pageCount(): Int? =
            dataFields("300").firstOrNull()?.firstValue("a")
                ?.replace(BRACKETS, " ")
                ?.let { NUMBER.findAll(it).mapNotNull { m -> m.value.toIntOrNull() }.maxOrNull() }
                ?.takeIf { it > 0 }

        /**
         * 245 $c without a 100 field, e.g. "Lush Gjergji ; [tł. ...]" → ["Lush Gjergji"].
         */
        private fun statementOfResponsibility(): List<String> =
            dataFields("245").firstOrNull()?.firstValue("c")
                ?.substringBefore(";")
                ?.replace(BRACKETS, "")
                ?.split(", ", " i ")
                ?.map { clean(it) }
                ?.filter { it.isNotEmpty() }
                .orEmpty()

        private fun List<Pair<String, String>>.firstValue(code: String) = firstOrNull { it.first == code }?.second
    }

    companion object {
        private val LIFE_DATES = Regex("""\s*\([^)]*\)""")
        private val BRACKETS = Regex("""\[[^]]*]""")
        private val TRAILING_PUNCTUATION = Regex("""[\s/:;,.=]+$""")
        private val NUMBER = Regex("""\d+""")
        private const val SEARCH_LIMIT = 5

        internal fun clean(raw: String): String = raw.trim().replace(TRAILING_PUNCTUATION, "").trim()

        /**
         * "Woolf, Virginia" → "Virginia Woolf" (catalogue order to the order the other catalogs use).
         */
        internal fun personName(raw: String): String {
            val name = clean(raw.replace(LIFE_DATES, ""))
            val parts = name.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            return if (parts.size == 2) "${parts[1]} ${parts[0]}" else name
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class BnBibs(val bibs: List<BnBib>? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class BnBib(
        val id: Long? = null,
        val deleted: Boolean? = null,
        val isbnIssn: String? = null,
        val publicationYear: String? = null,
        val marc: JsonNode? = null,
    )
}
