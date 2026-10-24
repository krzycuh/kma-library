package pl.kmazurek.library.application.usecase.catalog

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.catalog.Isbn
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository

class IsbnLookupTest {
    private lateinit var bookRepository: BookRepository
    private lateinit var provider: BookMetadataProvider
    private lateinit var lookup: LookupBookByIsbn
    private lateinit var import: ImportBookByIsbn
    private val user = UserId.generate()
    private val isbn = Isbn.parse("9780140328721")

    private val metadata =
        BookMetadata(
            isbn = isbn.value,
            title = "Fantastic Mr. Fox",
            authors = listOf("Roald Dahl"),
            publishedYear = 1988,
            coverUrl = "https://covers.openlibrary.org/b/id/8739161-M.jpg",
            source = BookSource.OPEN_LIBRARY,
            externalId = "OL7353617M",
        )

    @BeforeEach
    fun setup() {
        bookRepository = mockk()
        provider = mockk()
        lookup = LookupBookByIsbn(bookRepository, provider)
        import = ImportBookByIsbn(lookup, FindOrCreateBook(bookRepository))
        every { bookRepository.save(any()) } answers { firstArg() }
        every { bookRepository.findByNormalizedKey(any()) } returns null
    }

    @Test
    fun `lookup should prefer the local catalog and not ask the provider`() {
        val existing = Book.create(BookTitle.of("Fantastic Mr. Fox"), AuthorName.parseList("Roald Dahl"), isbn, createdByUserId = user)
        every { bookRepository.findByIsbn(isbn) } returns existing

        val result = lookup.execute("0-14-032872-6")

        assertSame(existing, result.existingBook)
        assertNull(result.metadata)
        verify(exactly = 0) { provider.lookupByIsbn(any()) }
    }

    @Test
    fun `lookup should return provider metadata when the book is not in the catalog`() {
        every { bookRepository.findByIsbn(isbn) } returns null
        every { provider.lookupByIsbn(isbn) } returns metadata

        val result = lookup.execute("978-0-14-032872-1")

        assertNull(result.existingBook)
        assertEquals(metadata, result.metadata)
    }

    @Test
    fun `lookup should fail with not found when nobody knows the ISBN and reject invalid ISBN`() {
        every { bookRepository.findByIsbn(isbn) } returns null
        every { provider.lookupByIsbn(isbn) } returns null

        assertThrows<BookNotFoundException> { lookup.execute(isbn.value) }
        assertThrows<IllegalArgumentException> { lookup.execute("9780140328722") }
    }

    @Test
    fun `import should create a catalog entry from metadata with cover, source and external id`() {
        every { bookRepository.findByIsbn(isbn) } returns null
        every { provider.lookupByIsbn(isbn) } returns metadata

        val book = import.execute(isbn.value, user)

        assertEquals("Fantastic Mr. Fox", book.title.value)
        assertEquals(listOf("Roald Dahl"), book.authors.map { it.value })
        assertEquals(isbn, book.isbn)
        assertEquals(1988, book.publishedYear)
        assertEquals(metadata.coverUrl, book.coverUrl)
        assertEquals(BookSource.OPEN_LIBRARY, book.source)
        assertEquals("OL7353617M", book.externalId)
        assertEquals(user, book.createdByUserId)
    }

    @Test
    fun `import should keep commas inside author names and drop separators that would break storage`() {
        every { bookRepository.findByIsbn(isbn) } returns null
        every { provider.lookupByIsbn(isbn) } returns metadata.copy(authors = listOf("Tolkien, J. R. R.", "A; B", " ", "Tolkien, J. R. R."))

        val book = import.execute(isbn.value, user)

        assertEquals(listOf("Tolkien, J. R. R.", "A, B"), book.authors.map { it.value })
    }

    @Test
    fun `import should return the existing book without saving`() {
        val existing = Book.create(BookTitle.of("Fantastic Mr. Fox"), AuthorName.parseList("Roald Dahl"), isbn, createdByUserId = user)
        every { bookRepository.findByIsbn(isbn) } returns existing

        assertSame(existing, import.execute(isbn.value, user))
        verify(exactly = 0) { bookRepository.save(any()) }
    }

    @Test
    fun `import should refuse metadata without authors`() {
        every { bookRepository.findByIsbn(isbn) } returns null
        every { provider.lookupByIsbn(isbn) } returns metadata.copy(authors = emptyList())

        assertThrows<IncompleteBookMetadataException> { import.execute(isbn.value, user) }
    }
}
