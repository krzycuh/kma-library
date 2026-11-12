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

/**
 * Page counts: read from the ISBN record of each catalog, and found by title/author search.
 */
class PageCountSearchProviderTest {
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

    private fun baseUrl() = server.url("/").toString().removeSuffix("/")

    @Test
    fun `Open Library ISBN record carries number_of_pages`() {
        server.enqueue(jsonResponse(fixture("openlibrary/isbn-9780140328721.json")))

        val metadata = OpenLibraryBookMetadataProvider(testRestClient(baseUrl())).lookupByIsbn(Isbn.parse("9780140328721"))!!

        assertEquals(96, metadata.pageCount)
    }

    @Test
    fun `Open Library search returns the median page count per work`() {
        server.enqueue(jsonResponse(fixture("openlibrary/search-solaris.json")))

        val hits = OpenLibraryBookMetadataProvider(testRestClient(baseUrl())).searchByTitleAndAuthor("Solaris", "Stanisław Lem")

        assertEquals(2, hits.size)
        assertEquals("Solaris", hits[0].title)
        assertEquals(listOf("Stanisław Lem"), hits[0].authors)
        assertEquals(204, hits[0].pageCount)
        assertEquals(BookSource.OPEN_LIBRARY, hits[0].source)
        assertNull(hits[1].pageCount)

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/search.json", request.requestUrl!!.encodedPath)
        assertEquals("Solaris", request.requestUrl!!.queryParameter("title"))
        assertEquals("Stanisław Lem", request.requestUrl!!.queryParameter("author"))
        assertEquals("key,title,author_name,number_of_pages_median", request.requestUrl!!.queryParameter("fields"))
    }

    @Test
    fun `Google Books ISBN record carries pageCount`() {
        server.enqueue(jsonResponse(fixture("google/isbn-9780140328721.json")))

        val metadata = GoogleBooksBookMetadataProvider(testRestClient(baseUrl()), null).lookupByIsbn(Isbn.parse("9780140328721"))!!

        assertEquals(96, metadata.pageCount)
    }

    @Test
    fun `Google Books search asks intitle and inauthor and maps every volume`() {
        server.enqueue(jsonResponse(fixture("google/search-solaris.json")))

        val hits = GoogleBooksBookMetadataProvider(testRestClient(baseUrl()), "k3y").searchByTitleAndAuthor("Solaris", "Stanisław Lem")

        assertEquals(listOf("Solaris" to 224, "Lem: a biography" to 440), hits.map { it.title to it.pageCount })
        assertEquals(BookSource.GOOGLE_BOOKS, hits[0].source)

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("intitle:\"Solaris\" inauthor:\"Stanisław Lem\"", request.requestUrl!!.queryParameter("q"))
        assertEquals("5", request.requestUrl!!.queryParameter("maxResults"))
        assertEquals("k3y", request.requestUrl!!.queryParameter("key"))
    }

    @Test
    fun `Biblioteka Narodowa reads the page count from MARC 300`() {
        server.enqueue(jsonResponse(fixture("bn/isbn-9788308049402.json")))
        val provider = NationalLibraryBookMetadataProvider(testRestClient(baseUrl()))

        assertEquals(459, provider.lookupByIsbn(Isbn.parse("9788308049402"))!!.pageCount)

        server.enqueue(jsonResponse(fixture("bn/isbn10-8871351460.json")))
        assertEquals(48, provider.lookupByIsbn(Isbn.parse("8871351460"))!!.pageCount)
    }

    @Test
    fun `Biblioteka Narodowa search sends the surname, skips deleted records and ignores bracketed leaves`() {
        server.enqueue(jsonResponse(fixture("bn/search-solaris.json")))

        val hits = NationalLibraryBookMetadataProvider(testRestClient(baseUrl())).searchByTitleAndAuthor("Solaris", "Stanisław Lem")

        assertEquals(1, hits.size)
        assertEquals("Solaris", hits[0].title)
        assertEquals(listOf("Stanisław Lem"), hits[0].authors)
        assertEquals(312, hits[0].pageCount)
        assertEquals(BookSource.NATIONAL_LIBRARY, hits[0].source)

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("Solaris", request.requestUrl!!.queryParameter("title"))
        assertEquals("Lem", request.requestUrl!!.queryParameter("author"))
        assertEquals("5", request.requestUrl!!.queryParameter("limit"))
    }

    @Test
    fun `search reports the catalog as unavailable on a server error`() {
        server.enqueue(jsonResponse("{}", 503))
        server.enqueue(jsonResponse("{}", 503))

        assertThrows<BookMetadataUnavailableException> {
            OpenLibraryBookMetadataProvider(testRestClient(baseUrl())).searchByTitleAndAuthor("Solaris", null)
        }
    }

    @Test
    fun `search treats 404 as no hits`() {
        server.enqueue(jsonResponse("{}", 404))

        assertEquals(
            emptyList<Any>(),
            GoogleBooksBookMetadataProvider(testRestClient(baseUrl()), null).searchByTitleAndAuthor("Solaris", null),
        )
    }
}
