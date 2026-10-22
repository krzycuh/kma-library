package pl.kmazurek.library.application.usecase.reading

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.application.dto.BookReference
import pl.kmazurek.library.application.usecase.catalog.FindOrCreateBook
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.reading.FinishedPeriod
import pl.kmazurek.library.domain.model.reading.Rating
import pl.kmazurek.library.domain.model.reading.ReadingEntry
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.ReadingEntryRepository

class ReadingEntryUseCasesTest {
    private lateinit var entryRepository: ReadingEntryRepository
    private lateinit var bookRepository: BookRepository
    private lateinit var findOrCreateBook: FindOrCreateBook
    private val reader = UserId.generate()
    private val otherUser = UserId.generate()
    private val book = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Lem"), createdByUserId = reader)

    @BeforeEach
    fun setup() {
        entryRepository = mockk()
        bookRepository = mockk()
        findOrCreateBook = mockk()
        every { entryRepository.save(any()) } answers { firstArg() }
        every { bookRepository.findById(book.id) } returns book
        every { bookRepository.findByIds(any()) } returns mapOf(book.id to book)
    }

    @Test
    fun `add entry resolves book and stores period and rating`() {
        every { findOrCreateBook.resolve(any(), reader) } returns book
        val useCase = AddReadingEntry(entryRepository, findOrCreateBook)

        val (entry, resolvedBook) =
            useCase.execute(AddReadingEntryCommand(reader, BookReference(title = "Solaris", authors = "Lem"), 2023, 5, 4, "ok"))

        assertEquals(book, resolvedBook)
        assertEquals(FinishedPeriod(2023, 5), entry.finishedOn)
        assertEquals(4, entry.rating?.value)
        verify { entryRepository.save(any()) }
    }

    @Test
    fun `month without year is rejected`() {
        every { findOrCreateBook.resolve(any(), reader) } returns book
        val useCase = AddReadingEntry(entryRepository, findOrCreateBook)

        assertThrows<IllegalArgumentException> {
            useCase.execute(AddReadingEntryCommand(reader, BookReference(bookId = book.id.toString()), null, 5, null, null))
        }
    }

    @Test
    fun `update of another user's entry is reported as not found`() {
        val entry = ReadingEntry.create(otherUser, book.id)
        every { entryRepository.findById(entry.id) } returns entry
        val useCase = UpdateReadingEntry(entryRepository, bookRepository)

        assertThrows<ReadingEntryNotFoundException> {
            useCase.execute(UpdateReadingEntryCommand(entry.id, reader, 2020, null, 3, null))
        }
        verify(exactly = 0) { entryRepository.save(any()) }
    }

    @Test
    fun `delete own entry`() {
        val entry = ReadingEntry.create(reader, book.id)
        every { entryRepository.findById(entry.id) } returns entry
        every { entryRepository.deleteById(entry.id) } returns Unit

        DeleteReadingEntry(entryRepository).execute(entry.id, reader)

        verify { entryRepository.deleteById(entry.id) }
    }

    @Test
    fun `summary computes count, average and top rated`() {
        val entries =
            listOf(
                ReadingEntry.create(reader, book.id, FinishedPeriod(2024, 1), Rating(5)),
                ReadingEntry.create(reader, book.id, FinishedPeriod(2024, 2), Rating(3)),
                ReadingEntry.create(reader, book.id, FinishedPeriod(2024), null),
            )
        every { entryRepository.findByReaderAndYear(reader, 2024) } returns entries
        every { entryRepository.countByReader(reader) } returns 10
        every { entryRepository.findDistinctYears(reader) } returns listOf(2024, 2023)

        val summary = GetReadingSummary(entryRepository, bookRepository).execute(reader, 2024)

        assertEquals(3, summary.booksThisYear)
        assertEquals(10, summary.booksTotal)
        assertEquals(4.0, summary.averageRating)
        assertEquals(2, summary.topRated.size)
        assertEquals(5, summary.topRated.first().rating)
        assertEquals(listOf(2024, 2023), summary.years)
    }

    @Test
    fun `summary without ratings has null average`() {
        every { entryRepository.findByReaderAndYear(reader, 2022) } returns emptyList()
        every { entryRepository.countByReader(reader) } returns 0
        every { entryRepository.findDistinctYears(reader) } returns emptyList()

        val summary = GetReadingSummary(entryRepository, bookRepository).execute(reader, 2022)

        assertEquals(0, summary.booksThisYear)
        assertNull(summary.averageRating)
    }
}
