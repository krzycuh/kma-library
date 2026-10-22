package pl.kmazurek.library.application.usecase.shelf

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.library.application.dto.BookReference
import pl.kmazurek.library.application.dto.PagedResponse
import pl.kmazurek.library.application.dto.ShelfItemDto
import pl.kmazurek.library.application.usecase.catalog.FindOrCreateBook
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.shelf.BorrowerName
import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.ShelfItem
import pl.kmazurek.library.domain.model.shelf.ShelfItemId
import pl.kmazurek.library.domain.model.shelf.ShelfItemStatus
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.ShelfItemFilter
import pl.kmazurek.library.domain.repository.ShelfItemRepository
import java.time.LocalDate

data class ListShelfItemsQuery(
    val userId: UserId,
    val libraryId: LibraryId,
    val status: ShelfItemStatus? = null,
    val searchTerm: String? = null,
    val bookId: BookId? = null,
    val page: Int = 0,
    val pageSize: Int = 20,
)

data class AddShelfItemCommand(
    val userId: UserId,
    val libraryId: LibraryId,
    val book: BookReference,
    val acquiredOn: LocalDate?,
    val notes: String?,
)

data class UpdateShelfItemCommand(
    val userId: UserId,
    val itemId: ShelfItemId,
    val acquiredOn: LocalDate?,
    val notes: String?,
    val status: ShelfItemStatus?,
)

data class LendShelfItemCommand(
    val userId: UserId,
    val itemId: ShelfItemId,
    val borrowerFirstName: String,
    val borrowerLastName: String,
    val lentOn: LocalDate,
    val dueOn: LocalDate?,
    val notes: String?,
)

/**
 * Shared helper for item use cases: load item + verify library membership
 */
@Service
class ShelfItemAccess(
    private val shelfItemRepository: ShelfItemRepository,
    private val libraryAccessGuard: LibraryAccessGuard,
    private val bookRepository: BookRepository,
) {
    fun loadAccessible(
        userId: UserId,
        itemId: ShelfItemId,
    ): ShelfItem {
        val item = shelfItemRepository.findById(itemId) ?: throw ShelfItemNotFoundException("Shelf item not found")
        libraryAccessGuard.requireAccess(userId, item.libraryId)
        return item
    }

    fun toDto(item: ShelfItem): ShelfItemDto {
        val book = bookRepository.findById(item.bookId) ?: throw ShelfItemNotFoundException("Book of shelf item missing")
        return ShelfItemDto.fromDomain(item, book)
    }

    fun toDtos(items: List<ShelfItem>): List<ShelfItemDto> {
        val books = bookRepository.findByIds(items.map { it.bookId }.toSet())
        return items.mapNotNull { item -> books[item.bookId]?.let { ShelfItemDto.fromDomain(item, it) } }
    }
}

@Service
class ListShelfItems(
    private val shelfItemRepository: ShelfItemRepository,
    private val libraryAccessGuard: LibraryAccessGuard,
    private val access: ShelfItemAccess,
) {
    fun execute(query: ListShelfItemsQuery): PagedResponse<ShelfItemDto> {
        libraryAccessGuard.requireAccess(query.userId, query.libraryId)
        val pageSize = query.pageSize.coerceIn(1, 100)
        val filter =
            ShelfItemFilter(
                status = query.status,
                searchTerm = query.searchTerm?.trim()?.takeIf { it.isNotEmpty() },
                bookId = query.bookId,
            )
        val (items, total) = shelfItemRepository.findByLibrary(query.libraryId, filter, query.page.coerceAtLeast(0), pageSize)
        return PagedResponse.of(access.toDtos(items), query.page, pageSize, total)
    }
}

@Service
class GetShelfItem(
    private val access: ShelfItemAccess,
) {
    fun execute(
        userId: UserId,
        itemId: ShelfItemId,
    ): ShelfItemDto = access.toDto(access.loadAccessible(userId, itemId))
}

@Service
class AddShelfItem(
    private val shelfItemRepository: ShelfItemRepository,
    private val libraryAccessGuard: LibraryAccessGuard,
    private val findOrCreateBook: FindOrCreateBook,
    private val access: ShelfItemAccess,
) {
    @Transactional
    fun execute(command: AddShelfItemCommand): ShelfItemDto {
        libraryAccessGuard.requireAccess(command.userId, command.libraryId)
        val book = findOrCreateBook.resolve(command.book, command.userId)
        val item = ShelfItem.create(command.libraryId, book.id, command.acquiredOn, command.notes)
        return access.toDto(shelfItemRepository.save(item))
    }
}

@Service
class UpdateShelfItem(
    private val shelfItemRepository: ShelfItemRepository,
    private val access: ShelfItemAccess,
) {
    fun execute(command: UpdateShelfItemCommand): ShelfItemDto {
        var item = access.loadAccessible(command.userId, command.itemId)
        item = item.update(command.acquiredOn, command.notes)
        if (command.status != null && command.status != item.status) {
            if (item.activeLoan != null) throw ShelfItemAlreadyLentException("Return the loan before changing status")
            item = item.changeStatus(command.status)
        }
        return access.toDto(shelfItemRepository.save(item))
    }
}

@Service
class RemoveShelfItem(
    private val shelfItemRepository: ShelfItemRepository,
    private val access: ShelfItemAccess,
) {
    fun execute(
        userId: UserId,
        itemId: ShelfItemId,
    ) {
        access.loadAccessible(userId, itemId)
        shelfItemRepository.deleteById(itemId)
    }
}

@Service
class LendShelfItem(
    private val shelfItemRepository: ShelfItemRepository,
    private val access: ShelfItemAccess,
) {
    fun execute(command: LendShelfItemCommand): ShelfItemDto {
        val item = access.loadAccessible(command.userId, command.itemId)
        if (item.activeLoan != null) throw ShelfItemAlreadyLentException("Shelf item is already lent")
        if (item.status != ShelfItemStatus.ON_SHELF) {
            throw ShelfItemAlreadyLentException("Only items on the shelf can be lent (status: ${item.status})")
        }
        val lent =
            item.lendTo(
                borrower = BorrowerName.of(command.borrowerFirstName, command.borrowerLastName),
                lentOn = command.lentOn,
                dueOn = command.dueOn,
                notes = command.notes,
            )
        return access.toDto(shelfItemRepository.save(lent))
    }
}

@Service
class ReturnLoan(
    private val shelfItemRepository: ShelfItemRepository,
    private val access: ShelfItemAccess,
) {
    fun execute(
        userId: UserId,
        itemId: ShelfItemId,
        returnedOn: LocalDate?,
    ): ShelfItemDto {
        val item = access.loadAccessible(userId, itemId)
        if (item.activeLoan == null) throw NoActiveLoanException("Shelf item has no active loan")
        val returned = item.markReturned(returnedOn ?: LocalDate.now())
        return access.toDto(shelfItemRepository.save(returned))
    }
}

@Service
class ListActiveLoans(
    private val shelfItemRepository: ShelfItemRepository,
    private val libraryAccessGuard: LibraryAccessGuard,
    private val access: ShelfItemAccess,
) {
    fun execute(
        userId: UserId,
        libraryId: LibraryId,
    ): List<ShelfItemDto> {
        libraryAccessGuard.requireAccess(userId, libraryId)
        val items = shelfItemRepository.findLentByLibrary(libraryId)
        return access.toDtos(items).sortedWith(compareBy({ it.activeLoan?.lentOn }, { it.book.title }))
    }
}
