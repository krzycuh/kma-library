package pl.kmazurek.library.domain.model.shelf

import pl.kmazurek.library.domain.model.catalog.BookId
import java.time.LocalDate
import java.time.LocalDateTime

enum class ShelfItemStatus {
    ON_SHELF,
    LENT,
    LOST,
    GIVEN_AWAY,
}

/**
 * ShelfItem Aggregate Root (Shelf context)
 * A physical copy of a book in a library, together with its loan history.
 * Invariant: at most one active loan at a time; status LENT iff there is an active loan.
 */
data class ShelfItem(
    val id: ShelfItemId,
    val libraryId: LibraryId,
    val bookId: BookId,
    val status: ShelfItemStatus = ShelfItemStatus.ON_SHELF,
    val acquiredOn: LocalDate? = null,
    val notes: String? = null,
    val loans: List<Loan> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        require(loans.count { it.isActive } <= 1) { "Shelf item can have at most one active loan" }
        require((status == ShelfItemStatus.LENT) == loans.any { it.isActive }) {
            "Status LENT must match an active loan"
        }
    }

    val activeLoan: Loan? get() = loans.firstOrNull { it.isActive }

    fun update(
        acquiredOn: LocalDate?,
        notes: String?,
    ): ShelfItem =
        copy(
            acquiredOn = acquiredOn,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = LocalDateTime.now(),
        )

    fun lendTo(
        borrower: BorrowerName,
        lentOn: LocalDate,
        dueOn: LocalDate? = null,
        notes: String? = null,
    ): ShelfItem {
        check(activeLoan == null) { "Shelf item is already lent" }
        check(status == ShelfItemStatus.ON_SHELF) { "Only items on the shelf can be lent" }
        val loan = Loan.create(id, borrower, lentOn, dueOn, notes)
        return copy(
            status = ShelfItemStatus.LENT,
            loans = loans + loan,
            updatedAt = LocalDateTime.now(),
        )
    }

    fun markReturned(returnedOn: LocalDate): ShelfItem {
        val loan = activeLoan ?: throw IllegalStateException("Shelf item has no active loan")
        val returned = loan.markReturned(returnedOn)
        return copy(
            status = ShelfItemStatus.ON_SHELF,
            loans = loans.map { if (it.id == loan.id) returned else it },
            updatedAt = LocalDateTime.now(),
        )
    }

    /**
     * Change status between ON_SHELF / LOST / GIVEN_AWAY. LENT is only reachable via lendTo.
     */
    fun changeStatus(newStatus: ShelfItemStatus): ShelfItem {
        check(activeLoan == null) { "Return the active loan before changing status" }
        require(newStatus != ShelfItemStatus.LENT) { "Use lendTo to lend an item" }
        return copy(status = newStatus, updatedAt = LocalDateTime.now())
    }

    fun belongsTo(libraryId: LibraryId) = this.libraryId == libraryId

    companion object {
        fun create(
            libraryId: LibraryId,
            bookId: BookId,
            acquiredOn: LocalDate? = null,
            notes: String? = null,
        ): ShelfItem {
            val now = LocalDateTime.now()
            return ShelfItem(
                id = ShelfItemId.generate(),
                libraryId = libraryId,
                bookId = bookId,
                status = ShelfItemStatus.ON_SHELF,
                acquiredOn = acquiredOn,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                loans = emptyList(),
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
