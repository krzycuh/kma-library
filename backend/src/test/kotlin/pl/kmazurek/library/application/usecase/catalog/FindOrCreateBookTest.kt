package pl.kmazurek.library.application.usecase.catalog

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.application.dto.BookReference
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.catalog.Isbn
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository

class FindOrCreateBookTest {
    private lateinit var bookRepository: BookRepository
    private lateinit var useCase: FindOrCreateBook
    private val user = UserId.generate()

    @BeforeEach
    fun setup() {
        bookRepository = mockk()
        useCase = FindOrCreateBook(bookRepository)
        every { bookRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `should return existing book by ISBN without saving`() {
        val existing =
            Book.create(
                BookTitle.of("Solaris"),
                AuthorName.parseList("Lem"),
                Isbn.parse("9788308055144"),
                createdByUserId = user,
            )
        every { bookRepository.findByIsbn(Isbn.parse("9788308055144")) } returns existing

        val result = useCase.execute(FindOrCreateBookCommand("Solaris (inne wydanie)", "S. Lem", "978-83-08-05514-4", null, user))

        assertSame(existing, result)
        verify(exactly = 0) { bookRepository.save(any()) }
    }

    @Test
    fun `should return existing book by normalized title and author`() {
        val existing = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Stanisław Lem"), createdByUserId = user)
        every { bookRepository.findByNormalizedKey(existing.normalizedKey) } returns existing

        val result = useCase.execute(FindOrCreateBookCommand("solaris", "stanislaw lem", null, null, user))

        assertSame(existing, result)
        verify(exactly = 0) { bookRepository.save(any()) }
    }

    @Test
    fun `should enrich existing manual book with ISBN`() {
        val existing = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Lem"), createdByUserId = user)
        every { bookRepository.findByIsbn(any()) } returns null
        every { bookRepository.findByNormalizedKey(existing.normalizedKey) } returns existing
        val saved = slot<Book>()
        every { bookRepository.save(capture(saved)) } answers { firstArg() }

        val result = useCase.execute(FindOrCreateBookCommand("Solaris", "Lem", "9788308055144", 1961, user))

        assertEquals(existing.id, result.id)
        assertEquals("9788308055144", saved.captured.isbn?.value)
        assertEquals(1961, saved.captured.publishedYear)
    }

    @Test
    fun `should create new book when nothing matches`() {
        every { bookRepository.findByIsbn(any()) } returns null
        every { bookRepository.findByNormalizedKey(any()) } returns null

        val result = useCase.execute(FindOrCreateBookCommand("Lalka", "Bolesław Prus", null, 1890, user))

        assertEquals("Lalka", result.title.value)
        assertEquals(listOf("Bolesław Prus"), result.authors.map { it.value })
        assertEquals(user, result.createdByUserId)
        verify(exactly = 1) { bookRepository.save(any()) }
    }

    @Test
    fun `should reject invalid ISBN`() {
        assertThrows<IllegalArgumentException> {
            useCase.execute(FindOrCreateBookCommand("Lalka", "Prus", "123", null, user))
        }
    }

    @Test
    fun `resolve should load by id or fail with not found`() {
        val existing = Book.create(BookTitle.of("Lalka"), AuthorName.parseList("Prus"), createdByUserId = user)
        every { bookRepository.findById(any()) } returns null
        every { bookRepository.findById(existing.id) } returns existing

        assertSame(existing, useCase.resolve(BookReference(bookId = existing.id.toString()), user))
        assertThrows<BookNotFoundException> {
            useCase.resolve(BookReference(bookId = BookId.generate().toString()), user)
        }
    }

    @Test
    fun `resolve should require title and author for new books`() {
        assertThrows<IllegalArgumentException> { useCase.resolve(BookReference(title = "X"), user) }
        assertThrows<IllegalArgumentException> { useCase.resolve(BookReference(authors = "Y"), user) }
    }

    @Test
    fun `should create a manual book with a pasted cover`() {
        every { bookRepository.findByIsbn(any()) } returns null
        every { bookRepository.findByNormalizedKey(any()) } returns null

        val result =
            useCase.execute(
                FindOrCreateBookCommand("Zawisza. Czarne krzyże", "Jacek Komuda", null, null, user, " https://covers.example/zawisza.jpg "),
            )

        assertEquals("https://covers.example/zawisza.jpg", result.coverUrl)
    }

    @Test
    fun `should add a cover to an existing book that has none, without touching its other data`() {
        val existing =
            Book.create(
                BookTitle.of("Solaris"),
                AuthorName.parseList("Lem"),
                Isbn.parse("9788308055144"),
                createdByUserId = user,
            )
        every { bookRepository.findByNormalizedKey(existing.normalizedKey) } returns existing
        val saved = slot<Book>()
        every { bookRepository.save(capture(saved)) } answers { firstArg() }

        useCase.execute(FindOrCreateBookCommand("Solaris", "Lem", null, null, user, "https://covers.example/solaris.jpg"))

        assertEquals("https://covers.example/solaris.jpg", saved.captured.coverUrl)
        assertEquals("9788308055144", saved.captured.isbn?.value)
    }

    @Test
    fun `should reject a cover that is not an https address`() {
        assertThrows<IllegalArgumentException> {
            useCase.execute(FindOrCreateBookCommand("Lalka", "Prus", null, null, user, "http://covers.example/lalka.jpg"))
        }
    }
}
