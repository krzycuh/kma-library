package pl.kmazurek.library.application.usecase.catalog

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.kmazurek.library.application.service.BookCandidate
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.catalog.Isbn
import pl.kmazurek.library.domain.model.catalog.PageCountLookup
import pl.kmazurek.library.domain.model.catalog.PageCountLookupOutcome
import pl.kmazurek.library.domain.model.catalog.PageCountQueryKind
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.PageCountLookupRepository
import java.time.Duration

class BackfillPageCountsTest {
    private val bookRepository: BookRepository = mockk()
    private val lookupRepository: PageCountLookupRepository = mockk()
    private val provider: BookMetadataProvider = mockk()
    private val useCase = BackfillPageCounts(bookRepository, lookupRepository, provider)
    private val savedLookups = mutableListOf<PageCountLookup>()
    private val savedBooks = mutableListOf<Book>()

    private val isbn = Isbn.parse("9788308049402")
    private val solaris = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Stanisław Lem"), isbn = isbn, createdByUserId = null)
    private val cyberiada = Book.create(BookTitle.of("Cyberiada"), AuthorName.parseList("Stanisław Lem"), createdByUserId = null)

    @BeforeEach
    fun setup() {
        every { lookupRepository.save(capture(savedLookups)) } returns Unit
        every { bookRepository.save(capture(savedBooks)) } answers { firstArg() }
    }

    private fun pending(vararg books: Book) {
        every { lookupRepository.countBooksPendingLookup() } returns books.size.toLong()
        every { lookupRepository.findBooksPendingLookup(any()) } returns books.toList()
    }

    private fun metadata(pages: Int?) =
        BookMetadata(isbn.value, "Solaris", listOf("Stanisław Lem"), 2012, null, BookSource.OPEN_LIBRARY, null, pages)

    @Test
    fun `books with an ISBN get the page count of the ISBN record`() {
        pending(solaris)
        every { provider.lookupByIsbn(isbn) } returns metadata(204)

        val summary = useCase.execute(batchSize = 100, pause = Duration.ZERO)

        assertEquals(1, summary.foundByIsbn)
        assertEquals(204, savedBooks.single().pageCount)
        val lookup = savedLookups.single()
        assertEquals(PageCountLookupOutcome.FOUND, lookup.outcome)
        assertEquals(PageCountQueryKind.ISBN, lookup.queryKind)
        assertEquals("open_library", lookup.provider)
        verify(exactly = 0) { provider.searchByTitleAndAuthor(any(), any()) }
    }

    @Test
    fun `without an ISBN, or when the ISBN record has no pages, the title and author are searched`() {
        pending(solaris, cyberiada)
        every { provider.lookupByIsbn(isbn) } returns metadata(null)
        every { provider.searchByTitleAndAuthor("Solaris", "Stanisław Lem") } returns
            listOf(BookCandidate("Solaris", listOf("Stanislaw Lem"), 224, BookSource.GOOGLE_BOOKS))
        every { provider.searchByTitleAndAuthor("Cyberiada", "Stanisław Lem") } returns
            listOf(
                BookCandidate("Lem: a biography", listOf("Wojciech Orliński"), 440, BookSource.GOOGLE_BOOKS),
                BookCandidate("Cyberiada", emptyList(), null, BookSource.OPEN_LIBRARY),
                BookCandidate("Cyberiada: bajki robotów", listOf("Lem, Stanisław"), 320, BookSource.NATIONAL_LIBRARY),
            )

        val summary = useCase.execute(batchSize = 100, pause = Duration.ZERO)

        assertEquals(2, summary.foundByTitle)
        assertEquals(listOf(224, 320), savedBooks.map { it.pageCount })
        assertEquals(listOf("google_books", "national_library"), savedLookups.map { it.provider })
        assertTrue(savedLookups.all { it.queryKind == PageCountQueryKind.TITLE_AUTHOR })
    }

    @Test
    fun `a hit for a different book is not taken and the book is recorded as not found`() {
        pending(cyberiada)
        every { provider.searchByTitleAndAuthor("Cyberiada", "Stanisław Lem") } returns
            listOf(BookCandidate("Cyberiada", listOf("Jan Kowalski"), 300, BookSource.GOOGLE_BOOKS))

        val summary = useCase.execute(batchSize = 100, pause = Duration.ZERO)

        assertEquals(1, summary.notFound)
        assertEquals(PageCountLookupOutcome.NOT_FOUND, savedLookups.single().outcome)
        verify(exactly = 0) { bookRepository.save(any()) }
    }

    @Test
    fun `unavailable catalogs are recorded for a retry, not as a final answer`() {
        pending(solaris)
        every { provider.lookupByIsbn(isbn) } throws BookMetadataUnavailableException("down")
        every { provider.searchByTitleAndAuthor(any(), any()) } throws BookMetadataUnavailableException("down")

        val summary = useCase.execute(batchSize = 100, pause = Duration.ZERO)

        assertEquals(1, summary.unavailable)
        assertEquals(PageCountLookupOutcome.UNAVAILABLE, savedLookups.single().outcome)
    }

    @Test
    fun `an unavailable ISBN lookup still lets the title search answer`() {
        pending(solaris)
        every { provider.lookupByIsbn(isbn) } throws BookMetadataUnavailableException("down")
        every { provider.searchByTitleAndAuthor("Solaris", "Stanisław Lem") } returns
            listOf(BookCandidate("Solaris", listOf("Stanisław Lem"), 204, BookSource.OPEN_LIBRARY))

        val summary = useCase.execute(batchSize = 100, pause = Duration.ZERO)

        assertEquals(1, summary.foundByTitle)
        assertEquals(0, summary.unavailable)
    }

    @Test
    fun `nothing pending means nothing asked`() {
        pending()

        val summary = useCase.execute(batchSize = 100, pause = Duration.ZERO)

        assertEquals(0, summary.checked)
        verify(exactly = 0) { provider.lookupByIsbn(any()) }
    }

    @Test
    fun `batch size limits one run`() {
        val limit = slot<Int>()
        every { lookupRepository.countBooksPendingLookup() } returns 3
        every { lookupRepository.findBooksPendingLookup(capture(limit)) } returns emptyList()

        useCase.execute(batchSize = 2, pause = Duration.ZERO)

        assertEquals(2, limit.captured)
    }

    @Test
    fun `matching compares normalized titles, tolerates a subtitle and checks a surname`() {
        val book = Book.create(BookTitle.of("Władca Pierścieni"), AuthorName.parseList("J. R. R. Tolkien"), createdByUserId = null)
        val hit = { title: String, authors: List<String> -> BookCandidate(title, authors, 1000, BookSource.OPEN_LIBRARY) }

        assertTrue(BackfillPageCounts.matches(book, hit("Wladca pierscieni", listOf("John Ronald Reuel Tolkien"))))
        assertTrue(BackfillPageCounts.matches(book, hit("Władca Pierścieni: Drużyna Pierścienia", listOf("Tolkien, J. R. R."))))
        assertTrue(BackfillPageCounts.matches(book, hit("Władca Pierścieni", emptyList())))
        assertFalse(BackfillPageCounts.matches(book, hit("Władca Pierścieni", listOf("Peter Jackson"))))
        assertFalse(BackfillPageCounts.matches(book, hit("Władca", listOf("J. R. R. Tolkien"))))
        assertFalse(BackfillPageCounts.matches(book, hit("Hobbit", listOf("J. R. R. Tolkien"))))
    }
}
