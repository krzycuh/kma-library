package pl.kmazurek.library.infrastructure.catalog

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.Isbn
import java.time.Duration
import java.util.concurrent.TimeUnit

class OpenLibraryBookMetadataProviderTest {
    private lateinit var server: MockWebServer
    private lateinit var provider: OpenLibraryBookMetadataProvider

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        provider = OpenLibraryBookMetadataProvider(testRestClient(server.url("/").toString().removeSuffix("/")))
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `should map a full response and ask for the ISBN-13 bibkey`() {
        server.enqueue(jsonResponse(fixture("openlibrary/isbn-9780140328721.json")))

        val metadata = provider.lookupByIsbn(Isbn.parse("0140328726"))

        assertNotNull(metadata)
        assertEquals("9780140328721", metadata!!.isbn)
        assertEquals("Fantastic Mr. Fox", metadata.title)
        assertEquals(listOf("Roald Dahl"), metadata.authors)
        assertEquals(1988, metadata.publishedYear)
        assertEquals("https://covers.openlibrary.org/b/id/8739161-M.jpg", metadata.coverUrl)
        assertEquals(BookSource.OPEN_LIBRARY, metadata.source)
        assertEquals("OL7353617M", metadata.externalId)

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/api/books", request.requestUrl!!.encodedPath)
        assertEquals("ISBN:9780140328721", request.requestUrl!!.queryParameter("bibkeys"))
        assertEquals("json", request.requestUrl!!.queryParameter("format"))
        assertEquals("data", request.requestUrl!!.queryParameter("jscmd"))
    }

    @Test
    fun `should join subtitle, keep multiple authors and tolerate a missing cover`() {
        server.enqueue(jsonResponse(fixture("openlibrary/isbn-9780060853976.json")))

        val metadata = provider.lookupByIsbn(Isbn.parse("9780060853976"))!!

        assertEquals("Good Omens: The Nice and Accurate Prophecies of Agnes Nutter, Witch", metadata.title)
        assertEquals(listOf("Terry Pratchett", "Neil Gaiman"), metadata.authors)
        assertEquals(2006, metadata.publishedYear)
        assertNull(metadata.coverUrl)
    }

    @Test
    fun `should return null for an unknown ISBN`() {
        server.enqueue(jsonResponse(fixture("openlibrary/not-found.json")))

        assertNull(provider.lookupByIsbn(Isbn.parse("9788308055144")))
    }

    @Test
    fun `should report the catalog as unavailable on server errors`() {
        server.enqueue(jsonResponse("""{"error":"boom"}""", status = 503))

        assertThrows<BookMetadataUnavailableException> { provider.lookupByIsbn(Isbn.parse("9788308055144")) }
    }

    @Test
    fun `should give up after the read timeout`() {
        val slowProvider =
            OpenLibraryBookMetadataProvider(
                testRestClient(server.url("/").toString().removeSuffix("/"), timeout = Duration.ofMillis(200)),
            )
        server.enqueue(jsonResponse(fixture("openlibrary/not-found.json")).setHeadersDelay(2, TimeUnit.SECONDS))

        assertThrows<BookMetadataUnavailableException> { slowProvider.lookupByIsbn(Isbn.parse("9788308055144")) }
    }
}
