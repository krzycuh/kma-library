package pl.kmazurek.library.domain.model.shelf

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Value Object: who borrowed a book
 */
data class BorrowerName(
    val firstName: String,
    val lastName: String,
) {
    init {
        require(firstName.isNotBlank()) { "Borrower first name cannot be blank" }
        require(lastName.isNotBlank()) { "Borrower last name cannot be blank" }
        require(firstName.length <= 100) { "Borrower first name cannot exceed 100 characters" }
        require(lastName.length <= 100) { "Borrower last name cannot exceed 100 characters" }
    }

    val fullName: String get() = "$firstName $lastName"

    companion object {
        fun of(
            firstName: String,
            lastName: String,
        ) = BorrowerName(firstName.trim(), lastName.trim())
    }
}

/**
 * Loan entity - lives inside the ShelfItem aggregate
 */
data class Loan(
    val id: LoanId,
    val shelfItemId: ShelfItemId,
    val borrower: BorrowerName,
    val lentOn: LocalDate,
    val dueOn: LocalDate? = null,
    val returnedOn: LocalDate? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        dueOn?.let { require(!it.isBefore(lentOn)) { "Due date cannot be before lent date" } }
        returnedOn?.let { require(!it.isBefore(lentOn)) { "Return date cannot be before lent date" } }
    }

    val isActive: Boolean get() = returnedOn == null

    fun isOverdue(today: LocalDate): Boolean = isActive && dueOn != null && dueOn.isBefore(today)

    fun markReturned(returnedOn: LocalDate): Loan {
        require(isActive) { "Loan is already returned" }
        return copy(returnedOn = returnedOn, updatedAt = LocalDateTime.now())
    }

    companion object {
        fun create(
            shelfItemId: ShelfItemId,
            borrower: BorrowerName,
            lentOn: LocalDate,
            dueOn: LocalDate? = null,
            notes: String? = null,
        ): Loan {
            val now = LocalDateTime.now()
            return Loan(
                id = LoanId.generate(),
                shelfItemId = shelfItemId,
                borrower = borrower,
                lentOn = lentOn,
                dueOn = dueOn,
                returnedOn = null,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
