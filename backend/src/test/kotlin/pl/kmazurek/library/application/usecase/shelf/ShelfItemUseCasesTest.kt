package pl.kmazurek.library.application.usecase.shelf

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
import pl.kmazurek.library.domain.model.shelf.BorrowerName
import pl.kmazurek.library.domain.model.shelf.Library
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.shelf.ShelfItem
import pl.kmazurek.library.domain.model.shelf.ShelfItemStatus
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.LibraryRepository
import pl.kmazurek.library.domain.repository.ShelfItemRepository
import pl.kmazurek.library.domain.service.LibraryAccessPolicy
import java.time.LocalDate

class ShelfItemUseCasesTest {
    private lateinit var shelfItemRepository: ShelfItemRepository
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var bookRepository: BookRepository
    private lateinit var findOrCreateBook: FindOrCreateBook
    private lateinit var guard: LibraryAccessGuard
    private lateinit var access: ShelfItemAccess

    private val owner = UserId.generate()
    private val stranger = UserId.generate()
    private val library = Library.create("Biblioteczka", owner)
    private val book = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Lem"), createdByUserId = owner)

    @BeforeEach
    fun setup() {
        shelfItemRepository = mockk()
        libraryRepository = mockk()
        bookRepository = mockk()
        findOrCreateBook = mockk()
        every { libraryRepository.findById(library.id) } returns library
        every { libraryRepository.findMemberRole(library.id, owner) } returns LibraryRole.OWNER
        every { libraryRepository.findMemberRole(library.id, stranger) } returns null
        every { shelfItemRepository.save(any()) } answers { firstArg() }
        every { bookRepository.findById(book.id) } returns book
        every { bookRepository.findByIds(any()) } returns mapOf(book.id to book)
        guard = LibraryAccessGuard(libraryRepository, LibraryAccessPolicy(libraryRepository))
        access = ShelfItemAccess(shelfItemRepository, guard, bookRepository)
    }

    @Test
    fun `owner can add item to own library`() {
        every { findOrCreateBook.resolve(any(), owner) } returns book
        val useCase = AddShelfItem(shelfItemRepository, guard, findOrCreateBook, access)

        val dto = useCase.execute(AddShelfItemCommand(owner, library.id, BookReference(bookId = book.id.toString()), null, "notes"))

        assertEquals(ShelfItemStatus.ON_SHELF, dto.status)
        assertEquals(book.id.toString(), dto.book.id)
        verify { shelfItemRepository.save(any()) }
    }

    @Test
    fun `stranger cannot add item to someone else's library`() {
        val useCase = AddShelfItem(shelfItemRepository, guard, findOrCreateBook, access)

        assertThrows<LibraryAccessDeniedException> {
            useCase.execute(AddShelfItemCommand(stranger, library.id, BookReference(bookId = book.id.toString()), null, null))
        }
        verify(exactly = 0) { shelfItemRepository.save(any()) }
    }

    @Test
    fun `lend and return flow`() {
        val item = ShelfItem.create(library.id, book.id)
        every { shelfItemRepository.findById(item.id) } returns item

        val lent =
            LendShelfItem(shelfItemRepository, access).execute(
                LendShelfItemCommand(owner, item.id, "Jan", "Kowalski", LocalDate.of(2024, 3, 1), LocalDate.of(2024, 4, 1), null),
            )
        assertEquals(ShelfItemStatus.LENT, lent.status)
        assertEquals("Jan", lent.activeLoan?.borrowerFirstName)

        val lentDomain = item.lendTo(BorrowerName.of("Jan", "Kowalski"), LocalDate.of(2024, 3, 1))
        every { shelfItemRepository.findById(item.id) } returns lentDomain

        val returned = ReturnLoan(shelfItemRepository, access).execute(owner, item.id, LocalDate.of(2024, 3, 15))
        assertEquals(ShelfItemStatus.ON_SHELF, returned.status)
        assertNull(returned.activeLoan)
        assertEquals(1, returned.loans.size)
    }

    @Test
    fun `lending an already lent item is a conflict`() {
        val lent = ShelfItem.create(library.id, book.id).lendTo(BorrowerName.of("Jan", "Kowalski"), LocalDate.of(2024, 3, 1))
        every { shelfItemRepository.findById(lent.id) } returns lent

        assertThrows<ShelfItemAlreadyLentException> {
            LendShelfItem(shelfItemRepository, access).execute(
                LendShelfItemCommand(owner, lent.id, "Anna", "Nowak", LocalDate.of(2024, 3, 2), null, null),
            )
        }
    }

    @Test
    fun `returning an item without loan is a conflict`() {
        val item = ShelfItem.create(library.id, book.id)
        every { shelfItemRepository.findById(item.id) } returns item

        assertThrows<NoActiveLoanException> {
            ReturnLoan(shelfItemRepository, access).execute(owner, item.id, null)
        }
    }

    @Test
    fun `stranger cannot see item`() {
        val item = ShelfItem.create(library.id, book.id)
        every { shelfItemRepository.findById(item.id) } returns item

        assertThrows<LibraryAccessDeniedException> { GetShelfItem(access).execute(stranger, item.id) }
    }

    @Test
    fun `status cannot change while lent`() {
        val lent = ShelfItem.create(library.id, book.id).lendTo(BorrowerName.of("Jan", "Kowalski"), LocalDate.of(2024, 3, 1))
        every { shelfItemRepository.findById(lent.id) } returns lent

        assertThrows<ShelfItemAlreadyLentException> {
            UpdateShelfItem(shelfItemRepository, access).execute(UpdateShelfItemCommand(owner, lent.id, null, null, ShelfItemStatus.LOST))
        }
    }

    @Test
    fun `create default library adds owner as member`() {
        val user =
            pl.kmazurek.library.domain.model.user.User.create(
                pl.kmazurek.library.domain.model.user.Email("a@b.pl"),
                pl.kmazurek.library.domain.model.user.HashedPassword("x"),
                "Krzysztof Mazurek",
            )
        every { libraryRepository.save(any()) } answers { firstArg() }
        every { libraryRepository.addMember(any()) } returns Unit

        val created = CreateDefaultLibrary(libraryRepository).execute(user)

        assertEquals("Biblioteczka Krzysztof", created.name)
        assertEquals(user.id, created.ownerId)
        verify { libraryRepository.addMember(match { it.libraryId == created.id && it.userId == user.id && it.role == LibraryRole.OWNER }) }
    }
}
