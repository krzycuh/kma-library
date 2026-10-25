package pl.kmazurek.library.infrastructure.catalog

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.Isbn
import java.util.concurrent.TimeUnit

class NationalLibraryBookMetadataProviderTest {
    private lateinit var server: MockWebServer
    private lateinit var provider: NationalLibraryBookMetadataProvider

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        provider = NationalLibraryBookMetadataProvider(testRestClient(server.url("/").toString().removeSuffix("/")))
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `should read title and main author from MARC, not from the glued flat fields`() {
        server.enqueue(jsonResponse(fixture("bn/isbn-9788308049402.json")))

        val metadata = provider.lookupByIsbn(Isbn.parse("978-83-08-04940-2"))!!

        assertEquals("9788308049402", metadata.isbn)
        assertEquals("Nawiedzony dom: opowiadania zebrane", metadata.title)
        assertEquals(listOf("Virginia Woolf"), metadata.authors)
        assertEquals(2012, metadata.publishedYear)
        assertNull(metadata.coverUrl)
        assertEquals(BookSource.NATIONAL_LIBRARY, metadata.source)
        assertEquals("2742627", metadata.externalId)

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/api/institutions/bibs.json", request.requestUrl!!.encodedPath)
        assertEquals("9788308049402", request.requestUrl!!.queryParameter("isbnIssn"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `should ask again with the ISBN-10 when the book is catalogued with it`() {
        server.enqueue(jsonResponse(fixture("bn/not-found.json")))
        server.enqueue(jsonResponse(fixture("bn/isbn10-8871351460.json")))

        val metadata = provider.lookupByIsbn(Isbn.parse("8871351460"))!!

        assertEquals("9788871351469", metadata.isbn)
        assertEquals("Matka Teresa: miłość w czynach", metadata.title)
        assertEquals(listOf("Lush Gjergji"), metadata.authors)
        assertEquals(2002, metadata.publishedYear)
        assertEquals("9788871351469", server.takeRequest(1, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("isbnIssn"))
        assertEquals("8871351460", server.takeRequest(1, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("isbnIssn"))
    }

    @Test
    fun `should return null when neither ISBN form has a record`() {
        server.enqueue(jsonResponse(fixture("bn/not-found.json")))
        server.enqueue(jsonResponse(fixture("bn/not-found.json")))

        assertNull(provider.lookupByIsbn(Isbn.parse("9788383226347")))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `should not ask for an ISBN-10 that does not exist`() {
        server.enqueue(jsonResponse(fixture("bn/not-found.json")))

        assertNull(provider.lookupByIsbn(Isbn.parse("9791032305690")))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `should retry a 5xx once and then report the catalog as unavailable`() {
        server.enqueue(jsonResponse("{}", status = 502))
        server.enqueue(jsonResponse("{}", status = 502))

        assertThrows<BookMetadataUnavailableException> { provider.lookupByIsbn(Isbn.parse("9788383226347")) }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `should take co-authors marked as authors, skip translators, join volume parts`() {
        // Synthetic record: the real samples have no 700 fields within the captured part
        val marc =
            ObjectMapper().readTree(
                """
                {"fields": [
                  {"100": {"subfields": [{"a": "Pratchett, Terry"}, {"d": "(1948-2015)"}, {"e": "Autor"}]}},
                  {"245": {"subfields": [{"a": "Dobry omen."}, {"n": "T. 1 ,"}, {"b": "prawdziwa przepowiednia /"}, {"c": "x"}]}},
                  {"700": {"subfields": [{"a": "Gaiman, Neil"}, {"d": "(1960- )"}, {"e": "Autor"}]}},
                  {"700": {"subfields": [{"a": "Gałązka, Jacek"}, {"e": "Tłumaczenie"}]}}
                ]}
                """,
            )
        val record = NationalLibraryBookMetadataProvider.MarcRecord(marc)

        assertEquals("Dobry omen. T. 1: prawdziwa przepowiednia", record.title())
        assertEquals(listOf("Terry Pratchett", "Neil Gaiman"), record.authors())
    }

    @Test
    fun `should fall back to the statement of responsibility without a main entry`() {
        val marc =
            ObjectMapper().readTree(
                """{"fields": [{"245": {"subfields": [{"a": "Antologia /"}, {"c": "Jan Nowak i Anna Kowalska ; [wybór X]."}]}}]}""",
            )

        assertEquals(listOf("Jan Nowak", "Anna Kowalska"), NationalLibraryBookMetadataProvider.MarcRecord(marc).authors())
    }
}
