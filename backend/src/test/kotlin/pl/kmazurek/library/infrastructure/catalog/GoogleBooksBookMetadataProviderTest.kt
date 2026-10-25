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

class GoogleBooksBookMetadataProviderTest {
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun provider(apiKey: String? = null) =
        GoogleBooksBookMetadataProvider(testRestClient(server.url("/").toString().removeSuffix("/")), apiKey)

    @Test
    fun `should map the first volume and upgrade the thumbnail to https`() {
        server.enqueue(jsonResponse(fixture("google/isbn-9780140328721.json")))

        val metadata = provider().lookupByIsbn(Isbn.parse("9780140328721"))!!

        assertEquals("Fantastic Mr Fox", metadata.title)
        assertEquals(listOf("Roald Dahl"), metadata.authors)
        assertEquals(1988, metadata.publishedYear)
        assertEquals(
            "https://books.google.com/books/content?id=wrOQLV6xB-wC&printsec=frontcover&img=1&zoom=1&source=gbs_api",
            metadata.coverUrl,
        )
        assertEquals(BookSource.GOOGLE_BOOKS, metadata.source)
        assertEquals("wrOQLV6xB-wC", metadata.externalId)

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/books/v1/volumes", request.requestUrl!!.encodedPath)
        assertEquals("isbn:9780140328721", request.requestUrl!!.queryParameter("q"))
        assertNull(request.requestUrl!!.queryParameter("key"))
    }

    @Test
    fun `should send the API key when configured`() {
        server.enqueue(jsonResponse(fixture("google/not-found.json")))

        assertNull(provider(apiKey = "secret").lookupByIsbn(Isbn.parse("9780140328721")))
        assertEquals("secret", server.takeRequest(1, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("key"))
    }

    @Test
    fun `should report quota errors as unavailable`() {
        server.enqueue(jsonResponse("""{"error":{"code":429}}""", status = 429))

        assertThrows<BookMetadataUnavailableException> { provider().lookupByIsbn(Isbn.parse("9780140328721")) }
    }

    @Test
    fun `should retry once after a 5xx such as backendFailed`() {
        server.enqueue(jsonResponse("""{"error":{"code":503,"errors":[{"reason":"backendFailed"}]}}""", status = 503))
        server.enqueue(jsonResponse(fixture("google/isbn-9780140328721.json")))

        assertEquals("Fantastic Mr Fox", provider().lookupByIsbn(Isbn.parse("9780140328721"))!!.title)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `should not retry client errors`() {
        server.enqueue(jsonResponse("""{"error":{"code":400}}""", status = 400))

        assertThrows<BookMetadataUnavailableException> { provider().lookupByIsbn(Isbn.parse("9780140328721")) }
        assertEquals(1, server.requestCount)
    }
}
