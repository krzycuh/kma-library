package pl.kmazurek.library.infrastructure.catalog

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.Isbn

class CachingBookMetadataProviderTest {
    private lateinit var delegate: BookMetadataProvider
    private lateinit var provider: CachingBookMetadataProvider
    private val isbn = Isbn.parse("9780140328721")
    private val metadata = BookMetadata(isbn.value, "Fantastic Mr. Fox", listOf("Roald Dahl"), 1988, null, BookSource.OPEN_LIBRARY, null)

    @BeforeEach
    fun setup() {
        delegate = mockk()
        provider = CachingBookMetadataProvider(delegate, ConcurrentMapCacheManager())
    }

    @Test
    fun `should ask the delegate once per ISBN`() {
        every { delegate.lookupByIsbn(isbn) } returns metadata

        assertEquals(metadata, provider.lookupByIsbn(isbn))
        assertEquals(metadata, provider.lookupByIsbn(isbn))
        verify(exactly = 1) { delegate.lookupByIsbn(isbn) }
    }

    @Test
    fun `should cache not found answers`() {
        every { delegate.lookupByIsbn(isbn) } returns null

        assertNull(provider.lookupByIsbn(isbn))
        assertNull(provider.lookupByIsbn(isbn))
        verify(exactly = 1) { delegate.lookupByIsbn(isbn) }
    }

    @Test
    fun `should not cache failures`() {
        every { delegate.lookupByIsbn(isbn) } throws BookMetadataUnavailableException("down") andThen metadata

        assertThrows<BookMetadataUnavailableException> { provider.lookupByIsbn(isbn) }
        assertEquals(metadata, provider.lookupByIsbn(isbn))
    }
}
