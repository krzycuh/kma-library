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
    private lateinit var first: BookMetadataProvider
    private lateinit var second: BookMetadataProvider
    private lateinit var provider: FallbackBookMetadataProvider
    private val isbn = Isbn.parse("9788308055144")
    private val fromFirst = BookMetadata(isbn.value, "Solaris", listOf("Stanisław Lem"), 1961, null, BookSource.OPEN_LIBRARY, "OL1M")
    private val fromSecond = fromFirst.copy(source = BookSource.GOOGLE_BOOKS, externalId = "g1")

    @BeforeEach
    fun setup() {
        first = mockk()
        second = mockk()
        provider = FallbackBookMetadataProvider(linkedMapOf("first" to first, "second" to second))
    }

    @Test
    fun `should stop at the first catalog that finds the book`() {
        every { first.lookupByIsbn(isbn) } returns fromFirst

        assertEquals(fromFirst, provider.lookupByIsbn(isbn))
        verify(exactly = 0) { second.lookupByIsbn(any()) }
    }

    @Test
    fun `should ask the next catalog when the first does not know the ISBN`() {
        every { first.lookupByIsbn(isbn) } returns null
        every { second.lookupByIsbn(isbn) } returns fromSecond

        assertEquals(fromSecond, provider.lookupByIsbn(isbn))
    }

    @Test
    fun `should ask the next catalog when the first is unavailable`() {
        every { first.lookupByIsbn(isbn) } throws BookMetadataUnavailableException("down")
        every { second.lookupByIsbn(isbn) } returns fromSecond

        assertEquals(fromSecond, provider.lookupByIsbn(isbn))
    }

    @Test
    fun `should return null only when every catalog answered not found`() {
        every { first.lookupByIsbn(isbn) } returns null
        every { second.lookupByIsbn(isbn) } returns null

        assertNull(provider.lookupByIsbn(isbn))
    }

    @Test
    fun `should report unavailable when nothing was found and some catalog could not be asked`() {
        every { first.lookupByIsbn(isbn) } returns null
        every { second.lookupByIsbn(isbn) } throws BookMetadataUnavailableException("quota")

        val ex = assertThrows<BookMetadataUnavailableException> { provider.lookupByIsbn(isbn) }
        assertEquals("ISBN ${isbn.value} not found in first; unavailable: second", ex.message)
    }
}
