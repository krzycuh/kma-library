package pl.kmazurek.library.infrastructure.catalog

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
    fun `should map a record to the conventions of the other catalogs`() {
        server.enqueue(jsonResponse(fixture("bn/isbn-9788308049402.json")))

        val metadata = provider.lookupByIsbn(Isbn.parse("978-83-08-04940-2"))!!

        assertEquals("9788308049402", metadata.isbn)
        assertEquals("Solaris", metadata.title)
        assertEquals(listOf("Stanisław Lem"), metadata.authors)
        assertEquals(2012, metadata.publishedYear)
        assertNull(metadata.coverUrl)
        assertEquals(BookSource.NATIONAL_LIBRARY, metadata.source)
        assertEquals("4416730", metadata.externalId)

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/api/institutions/bibs.json", request.requestUrl!!.encodedPath)
        assertEquals("9788308049402", request.requestUrl!!.queryParameter("isbnIssn"))
    }

    @Test
    fun `should pick the record with the exact ISBN and split several authors`() {
        server.enqueue(jsonResponse(fixture("bn/isbn-9788381882385.json")))

        val metadata = provider.lookupByIsbn(Isbn.parse("9788381882385"))!!

        assertEquals("Dobry omen: prawdziwa i rzetelna przepowiednia Agnes Nutter, czarownicy", metadata.title)
        assertEquals(listOf("Terry Pratchett", "Neil Gaiman"), metadata.authors)
        assertEquals(2019, metadata.publishedYear)
        assertEquals("7001002", metadata.externalId)
    }

    @Test
    fun `should return null when there is no record`() {
        server.enqueue(jsonResponse(fixture("bn/not-found.json")))

        assertNull(provider.lookupByIsbn(Isbn.parse("9788383226347")))
    }

    @Test
    fun `should retry a 5xx once and then report the catalog as unavailable`() {
        server.enqueue(jsonResponse("{}", status = 502))
        server.enqueue(jsonResponse("{}", status = 502))

        assertThrows<BookMetadataUnavailableException> { provider.lookupByIsbn(Isbn.parse("9788383226347")) }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `should normalize catalogue-style titles and author names`() {
        assertEquals("Lalka", NationalLibraryBookMetadataProvider.cleanTitle("Lalka."))
        assertEquals("Lalka", NationalLibraryBookMetadataProvider.cleanTitle("Lalka /"))
        assertNull(NationalLibraryBookMetadataProvider.cleanTitle(" / "))
        assertEquals(
            listOf("Bolesław Prus", "Olga Tokarczuk", "Homer"),
            NationalLibraryBookMetadataProvider.parseAuthors("Prus, Bolesław (1847-1912).|Tokarczuk, Olga (1962- ) ; Homer"),
        )
        assertEquals(emptyList<String>(), NationalLibraryBookMetadataProvider.parseAuthors(null))
    }
}
