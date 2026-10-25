package pl.kmazurek.library.infrastructure.catalog

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.Isbn

class FallbackBookMetadataProviderTest {
    private lateinit var openLibrary: BookMetadataProvider
    private lateinit var bn: BookMetadataProvider
    private lateinit var google: BookMetadataProvider
    private lateinit var provider: FallbackBookMetadataProvider
    private val isbn = Isbn.parse("9788383182070")

    private val complete =
        BookMetadata(isbn.value, "Solaris", listOf("Stanisław Lem"), 1961, "https://covers.example/ol.jpg", BookSource.OPEN_LIBRARY, "OL1M")

    // Open Library knows the edition and its cover, but not the author
    private val olWithoutAuthor = complete.copy(authors = emptyList(), publishedYear = null)

    // Biblioteka Narodowa has the author and year, never a cover
    private val bnRecord =
        BookMetadata(isbn.value, "Solaris", listOf("Stanisław Lem"), 2022, null, BookSource.NATIONAL_LIBRARY, "123")

    @BeforeEach
    fun setup() {
        openLibrary = mockk()
        bn = mockk()
        google = mockk()
        provider = FallbackBookMetadataProvider(linkedMapOf("openlibrary" to openLibrary, "bn" to bn, "google" to google))
    }

    @Test
    fun `should stop at the first complete record`() {
        every { openLibrary.lookupByIsbn(isbn) } returns complete

        assertEquals(complete, provider.lookupByIsbn(isbn))
        verify(exactly = 0) { bn.lookupByIsbn(any()) }
        verify(exactly = 0) { google.lookupByIsbn(any()) }
    }

    @Test
    fun `should take the record with authors as the main one and keep the cover from the other catalog`() {
        every { openLibrary.lookupByIsbn(isbn) } returns olWithoutAuthor
        every { bn.lookupByIsbn(isbn) } returns bnRecord

        val merged = provider.lookupByIsbn(isbn)!!

        assertEquals(listOf("Stanisław Lem"), merged.authors)
        assertEquals(2022, merged.publishedYear)
        assertEquals("https://covers.example/ol.jpg", merged.coverUrl)
        assertEquals(BookSource.NATIONAL_LIBRARY, merged.source)
        assertEquals("123", merged.externalId)
        verify(exactly = 0) { google.lookupByIsbn(any()) }
    }

    @Test
    fun `should keep asking for a cover and return what it has when the rest have none`() {
        every { openLibrary.lookupByIsbn(isbn) } returns null
        every { bn.lookupByIsbn(isbn) } returns bnRecord
        every { google.lookupByIsbn(isbn) } returns null

        assertEquals(bnRecord, provider.lookupByIsbn(isbn))
        verify { google.lookupByIsbn(isbn) }
    }

    @Test
    fun `should skip an unavailable catalog when the others give a usable record`() {
        every { openLibrary.lookupByIsbn(isbn) } returns olWithoutAuthor
        every { bn.lookupByIsbn(isbn) } returns bnRecord
        every { google.lookupByIsbn(isbn) } throws BookMetadataUnavailableException("503")
        provider = FallbackBookMetadataProvider(linkedMapOf("google" to google, "openlibrary" to openLibrary, "bn" to bn))

        assertEquals(listOf("Stanisław Lem"), provider.lookupByIsbn(isbn)!!.authors)
    }

    @Test
    fun `should return a record without authors when every catalog answered`() {
        every { openLibrary.lookupByIsbn(isbn) } returns olWithoutAuthor
        every { bn.lookupByIsbn(isbn) } returns null
        every { google.lookupByIsbn(isbn) } returns null

        assertEquals(olWithoutAuthor, provider.lookupByIsbn(isbn))
    }

    @Test
    fun `should report unavailable when the authors may be in a catalog that could not be asked`() {
        every { openLibrary.lookupByIsbn(isbn) } returns olWithoutAuthor
        every { bn.lookupByIsbn(isbn) } throws BookMetadataUnavailableException("down")
        every { google.lookupByIsbn(isbn) } returns null

        assertThrows<BookMetadataUnavailableException> { provider.lookupByIsbn(isbn) }
    }

    @Test
    fun `should return null only when every catalog answered not found`() {
        every { openLibrary.lookupByIsbn(isbn) } returns null
        every { bn.lookupByIsbn(isbn) } returns null
        every { google.lookupByIsbn(isbn) } returns null

        assertNull(provider.lookupByIsbn(isbn))
    }

    @Test
    fun `should report unavailable when nothing was found and some catalog could not be asked`() {
        every { openLibrary.lookupByIsbn(isbn) } returns null
        every { bn.lookupByIsbn(isbn) } returns null
        every { google.lookupByIsbn(isbn) } throws BookMetadataUnavailableException("quota")

        val ex = assertThrows<BookMetadataUnavailableException> { provider.lookupByIsbn(isbn) }
        assertEquals("ISBN ${isbn.value}: no complete record in openlibrary, bn; unavailable: google", ex.message)
    }
}
