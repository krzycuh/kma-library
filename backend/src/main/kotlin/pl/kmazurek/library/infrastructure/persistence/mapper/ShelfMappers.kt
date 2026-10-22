package pl.kmazurek.library.infrastructure.persistence.mapper

import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.shelf.BorrowerName
import pl.kmazurek.library.domain.model.shelf.Library
import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.LibraryMember
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.shelf.Loan
import pl.kmazurek.library.domain.model.shelf.LoanId
import pl.kmazurek.library.domain.model.shelf.ShelfItem
import pl.kmazurek.library.domain.model.shelf.ShelfItemId
import pl.kmazurek.library.domain.model.shelf.ShelfItemStatus
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.infrastructure.persistence.entity.LibraryJpaEntity
import pl.kmazurek.library.infrastructure.persistence.entity.LibraryMemberJpaEntity
import pl.kmazurek.library.infrastructure.persistence.entity.LibraryMemberKey
import pl.kmazurek.library.infrastructure.persistence.entity.LoanJpaEntity
import pl.kmazurek.library.infrastructure.persistence.entity.ShelfItemJpaEntity

object LibraryMapper {
    fun toDomain(jpa: LibraryJpaEntity) =
        Library(
            id = LibraryId(jpa.id),
            name = jpa.name,
            ownerId = UserId(jpa.ownerId),
            createdAt = jpa.createdAt,
            updatedAt = jpa.updatedAt,
        )

    fun toJpa(domain: Library) =
        LibraryJpaEntity(
            id = domain.id.value,
            name = domain.name,
            ownerId = domain.ownerId.value,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    fun memberToDomain(jpa: LibraryMemberJpaEntity) =
        LibraryMember(
            libraryId = LibraryId(jpa.key.libraryId),
            userId = UserId(jpa.key.userId),
            role = LibraryRole.valueOf(jpa.role),
        )

    fun memberToJpa(domain: LibraryMember) =
        LibraryMemberJpaEntity(
            key = LibraryMemberKey(domain.libraryId.value, domain.userId.value),
            role = domain.role.name,
        )
}

object ShelfItemMapper {
    fun toDomain(jpa: ShelfItemJpaEntity): ShelfItem =
        ShelfItem(
            id = ShelfItemId(jpa.id),
            libraryId = LibraryId(jpa.libraryId),
            bookId = BookId(jpa.bookId),
            status = ShelfItemStatus.valueOf(jpa.status),
            acquiredOn = jpa.acquiredOn,
            notes = jpa.notes,
            loans = jpa.loans.map { loanToDomain(it, ShelfItemId(jpa.id)) },
            createdAt = jpa.createdAt,
            updatedAt = jpa.updatedAt,
        )

    /**
     * Maps onto an existing managed entity when present so Hibernate tracks loan changes.
     */
    fun toJpa(
        domain: ShelfItem,
        existing: ShelfItemJpaEntity? = null,
    ): ShelfItemJpaEntity {
        val entity =
            existing
                ?: ShelfItemJpaEntity(
                    id = domain.id.value,
                    libraryId = domain.libraryId.value,
                    bookId = domain.bookId.value,
                    createdAt = domain.createdAt,
                )
        entity.libraryId = domain.libraryId.value
        entity.bookId = domain.bookId.value
        entity.status = domain.status.name
        entity.acquiredOn = domain.acquiredOn
        entity.notes = domain.notes
        entity.updatedAt = domain.updatedAt

        val existingLoans = entity.loans.associateBy { it.id }
        val domainIds = domain.loans.map { it.id.value }.toSet()
        entity.loans.removeIf { it.id !in domainIds }
        domain.loans.forEach { loan ->
            val loanEntity = existingLoans[loan.id.value]
            if (loanEntity == null) {
                entity.loans.add(loanToJpa(loan, entity))
            } else {
                loanEntity.borrowerFirstName = loan.borrower.firstName
                loanEntity.borrowerLastName = loan.borrower.lastName
                loanEntity.lentOn = loan.lentOn
                loanEntity.dueOn = loan.dueOn
                loanEntity.returnedOn = loan.returnedOn
                loanEntity.notes = loan.notes
                loanEntity.updatedAt = loan.updatedAt
            }
        }
        return entity
    }

    private fun loanToDomain(
        jpa: LoanJpaEntity,
        shelfItemId: ShelfItemId,
    ): Loan =
        Loan(
            id = LoanId(jpa.id),
            shelfItemId = shelfItemId,
            borrower = BorrowerName(jpa.borrowerFirstName, jpa.borrowerLastName),
            lentOn = jpa.lentOn,
            dueOn = jpa.dueOn,
            returnedOn = jpa.returnedOn,
            notes = jpa.notes,
            createdAt = jpa.createdAt,
            updatedAt = jpa.updatedAt,
        )

    private fun loanToJpa(
        domain: Loan,
        owner: ShelfItemJpaEntity,
    ): LoanJpaEntity =
        LoanJpaEntity(
            id = domain.id.value,
            shelfItem = owner,
            borrowerFirstName = domain.borrower.firstName,
            borrowerLastName = domain.borrower.lastName,
            lentOn = domain.lentOn,
            dueOn = domain.dueOn,
            returnedOn = domain.returnedOn,
            notes = domain.notes,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
}
