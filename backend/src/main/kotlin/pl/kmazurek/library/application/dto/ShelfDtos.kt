package pl.kmazurek.library.application.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.shelf.Library
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.shelf.Loan
import pl.kmazurek.library.domain.model.shelf.ShelfItem
import pl.kmazurek.library.domain.model.shelf.ShelfItemStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class LibraryDto(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: LocalDateTime,
    /** Role of the current user in this library */
    val myRole: LibraryRole,
    val memberCount: Int,
) {
    companion object {
        fun fromDomain(
            library: Library,
            myRole: LibraryRole,
            memberCount: Int,
        ) = LibraryDto(
            id = library.id.toString(),
            name = library.name,
            ownerId = library.ownerId.toString(),
            createdAt = library.createdAt,
            myRole = myRole,
            memberCount = memberCount,
        )
    }
}

data class LibraryMemberDto(
    val userId: String,
    val name: String,
    val email: String,
    val role: LibraryRole,
)

data class InviteLibraryMemberRequest(
    @field:NotBlank(message = "E-mail is required")
    @field:Size(max = 255, message = "E-mail cannot exceed 255 characters")
    val email: String,
)

data class RenameLibraryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name cannot exceed 255 characters")
    val name: String,
)

data class LoanDto(
    val id: String,
    val borrowerFirstName: String,
    val borrowerLastName: String,
    val lentOn: LocalDate,
    val dueOn: LocalDate?,
    val returnedOn: LocalDate?,
    val notes: String?,
    val active: Boolean,
    val overdue: Boolean,
) {
    companion object {
        fun fromDomain(
            loan: Loan,
            today: LocalDate = LocalDate.now(),
        ) = LoanDto(
            id = loan.id.toString(),
            borrowerFirstName = loan.borrower.firstName,
            borrowerLastName = loan.borrower.lastName,
            lentOn = loan.lentOn,
            dueOn = loan.dueOn,
            returnedOn = loan.returnedOn,
            notes = loan.notes,
            active = loan.isActive,
            overdue = loan.isOverdue(today),
        )
    }
}

data class ShelfItemDto(
    val id: String,
    val libraryId: String,
    val book: BookDto,
    val status: ShelfItemStatus,
    val acquiredOn: LocalDate?,
    val notes: String?,
    val activeLoan: LoanDto?,
    val loans: List<LoanDto>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(
            item: ShelfItem,
            book: Book,
        ) = ShelfItemDto(
            id = item.id.toString(),
            libraryId = item.libraryId.toString(),
            book = BookDto.fromDomain(book),
            status = item.status,
            acquiredOn = item.acquiredOn,
            notes = item.notes,
            activeLoan = item.activeLoan?.let { LoanDto.fromDomain(it) },
            loans = item.loans.sortedByDescending { it.lentOn }.map { LoanDto.fromDomain(it) },
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
        )
    }
}

data class AddShelfItemRequest(
    @field:Valid
    val book: BookReference,
    val acquiredOn: LocalDate? = null,
    @field:Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    val notes: String? = null,
)

data class UpdateShelfItemRequest(
    val acquiredOn: LocalDate? = null,
    @field:Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    val notes: String? = null,
    val status: ShelfItemStatus? = null,
)

data class LendShelfItemRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100)
    val borrowerFirstName: String,
    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100)
    val borrowerLastName: String,
    @field:NotNull(message = "Lent date is required")
    val lentOn: LocalDate,
    val dueOn: LocalDate? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class ReturnLoanRequest(
    val returnedOn: LocalDate? = null,
)
