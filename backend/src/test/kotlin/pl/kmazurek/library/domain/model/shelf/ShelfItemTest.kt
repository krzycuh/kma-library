package pl.kmazurek.library.domain.model.shelf

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.domain.model.catalog.BookId
import java.time.LocalDate

class ShelfItemTest {
    private val library = LibraryId.generate()
    private val book = BookId.generate()
    private val borrower = BorrowerName.of(" Jan ", "Kowalski")

    private fun item() = ShelfItem.create(library, book, acquiredOn = LocalDate.of(2020, 1, 1), notes = " first edition ")

    @Test
    fun `new item is on shelf without loans`() {
        val item = item()
        assertEquals(ShelfItemStatus.ON_SHELF, item.status)
        assertTrue(item.loans.isEmpty())
        assertNull(item.activeLoan)
        assertEquals("first edition", item.notes)
    }

    @Test
    fun `lending creates active loan and sets status LENT`() {
        val lent = item().lendTo(borrower, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 4, 1))

        assertEquals(ShelfItemStatus.LENT, lent.status)
        val loan = lent.activeLoan
        assertNotNull(loan)
        loan!!
        assertEquals("Jan Kowalski", loan.borrower.fullName)
        assertTrue(loan.isActive)
        assertTrue(loan.isOverdue(LocalDate.of(2024, 4, 2)))
        assertFalse(loan.isOverdue(LocalDate.of(2024, 3, 15)))
    }

    @Test
    fun `cannot lend an item that is already lent`() {
        val lent = item().lendTo(borrower, LocalDate.of(2024, 3, 1))
        assertThrows<IllegalStateException> { lent.lendTo(borrower, LocalDate.of(2024, 3, 2)) }
    }

    @Test
    fun `cannot lend a lost item`() {
        val lost = item().changeStatus(ShelfItemStatus.LOST)
        assertThrows<IllegalStateException> { lost.lendTo(borrower, LocalDate.of(2024, 3, 1)) }
    }

    @Test
    fun `returning closes the loan, keeps history and puts item back on shelf`() {
        val lent = item().lendTo(borrower, LocalDate.of(2024, 3, 1))
        val returned = lent.markReturned(LocalDate.of(2024, 3, 20))

        assertEquals(ShelfItemStatus.ON_SHELF, returned.status)
        assertNull(returned.activeLoan)
        assertEquals(1, returned.loans.size)
        assertEquals(LocalDate.of(2024, 3, 20), returned.loans.single().returnedOn)
    }

    @Test
    fun `return date cannot be before lent date`() {
        val lent = item().lendTo(borrower, LocalDate.of(2024, 3, 10))
        assertThrows<IllegalArgumentException> { lent.markReturned(LocalDate.of(2024, 3, 1)) }
    }

    @Test
    fun `cannot return when there is no active loan`() {
        assertThrows<IllegalStateException> { item().markReturned(LocalDate.of(2024, 3, 1)) }
    }

    @Test
    fun `item can be lent again after return`() {
        val again =
            item()
                .lendTo(borrower, LocalDate.of(2024, 1, 1))
                .markReturned(LocalDate.of(2024, 2, 1))
                .lendTo(BorrowerName.of("Anna", "Nowak"), LocalDate.of(2024, 5, 1))

        assertEquals(2, again.loans.size)
        assertEquals("Anna Nowak", again.activeLoan?.borrower?.fullName)
    }

    @Test
    fun `cannot change status while lent and LENT cannot be set directly`() {
        val lent = item().lendTo(borrower, LocalDate.of(2024, 3, 1))
        assertThrows<IllegalStateException> { lent.changeStatus(ShelfItemStatus.LOST) }
        assertThrows<IllegalArgumentException> { item().changeStatus(ShelfItemStatus.LENT) }
    }

    @Test
    fun `borrower name requires first and last name`() {
        assertThrows<IllegalArgumentException> { BorrowerName.of("", "Kowalski") }
        assertThrows<IllegalArgumentException> { BorrowerName.of("Jan", " ") }
    }
}
