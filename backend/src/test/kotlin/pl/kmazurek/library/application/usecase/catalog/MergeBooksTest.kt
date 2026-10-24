package pl.kmazurek.library.application.usecase.catalog

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.catalog.Isbn
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.ReadingEntryRepository
import pl.kmazurek.library.domain.repository.ShelfItemRepository

class MergeBooksTest {
    private lateinit var bookRepository: BookRepository
    private lateinit var readingEntryRepository: ReadingEntryRepository
    private lateinit var shelfItemRepository: ShelfItemRepository
    private lateinit var useCase: MergeBooks
    private val user = UserId.generate()

    private val target = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Stanisław Lem"), createdByUserId = user)
    private val duplicate =
        Book.create(BookTitle.of("Solaris (wyd. 2)"), AuthorName.parseList("S. Lem"), Isbn.parse("9788308055144"), createdByUserId = user)

    @BeforeEach
    fun setup() {
        bookRepository = mockk(relaxUnitFun = true)
        readingEntryRepository = mockk()
        shelfItemRepository = mockk()
        useCase = MergeBooks(bookRepository, readingEntryRepository, shelfItemRepository)
        every { bookRepository.findById(target.id) } returns target
        every { bookRepository.findById(duplicate.id) } returns duplicate
        every { bookRepository.save(any()) } answers { firstArg() }
        every { readingEntryRepository.reassignBook(duplicate.id, target.id) } returns 2
        every { shelfItemRepository.reassignBook(duplicate.id, target.id) } returns 1
    }

    @Test
    fun `should move entries and copies, delete the duplicate, then save the enriched target`() {
        val result = useCase.execute(duplicate.id, target.id)

        assertEquals(2, result.movedReadingEntries)
        assertEquals(1, result.movedShelfItems)
        assertEquals(target.id, result.target.id)
        assertEquals("Solaris", result.target.title.value)
        assertEquals("9788308055144", result.target.isbn?.value)
        verifyOrder {
            readingEntryRepository.reassignBook(duplicate.id, target.id)
            shelfItemRepository.reassignBook(duplicate.id, target.id)
            bookRepository.deleteById(duplicate.id)
            bookRepository.save(match { it.id == target.id })
        }
    }

    @Test
    fun `should not save the target when the duplicate brings nothing new`() {
        val plainDuplicate = Book.create(BookTitle.of("solaris"), AuthorName.parseList("Lem"), createdByUserId = user)
        every { bookRepository.findById(plainDuplicate.id) } returns plainDuplicate
        every { readingEntryRepository.reassignBook(plainDuplicate.id, target.id) } returns 0
        every { shelfItemRepository.reassignBook(plainDuplicate.id, target.id) } returns 0

        val result = useCase.execute(plainDuplicate.id, target.id)

        assertSame(target, result.target)
        verify(exactly = 0) { bookRepository.save(any()) }
        verify { bookRepository.deleteById(plainDuplicate.id) }
    }

    @Test
    fun `should reject merging a book into itself or into a missing book`() {
        assertThrows<IllegalArgumentException> { useCase.execute(target.id, target.id) }

        val missing = BookId.generate()
        every { bookRepository.findById(missing) } returns null
        assertThrows<BookNotFoundException> { useCase.execute(duplicate.id, missing) }
        verify(exactly = 0) { bookRepository.deleteById(any()) }
    }
}
