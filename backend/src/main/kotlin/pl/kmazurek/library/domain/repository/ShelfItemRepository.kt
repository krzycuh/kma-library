package pl.kmazurek.library.domain.repository

import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.ShelfItem
import pl.kmazurek.library.domain.model.shelf.ShelfItemId
import pl.kmazurek.library.domain.model.shelf.ShelfItemStatus

data class ShelfItemFilter(
    val status: ShelfItemStatus? = null,
    val searchTerm: String? = null,
    val bookId: BookId? = null,
)

/**
 * Repository interface (port) for the ShelfItem aggregate (loads loans with the item)
 */
interface ShelfItemRepository {
    fun findById(id: ShelfItemId): ShelfItem?

    /**
     * Items of a library, newest first. Returns (items, totalCount).
     */
    fun findByLibrary(
        libraryId: LibraryId,
        filter: ShelfItemFilter,
        page: Int,
        pageSize: Int,
    ): Pair<List<ShelfItem>, Long>

    fun findLentByLibrary(libraryId: LibraryId): List<ShelfItem>

    fun countByLibraryAndStatus(
        libraryId: LibraryId,
        status: ShelfItemStatus,
    ): Long

    fun save(item: ShelfItem): ShelfItem

    fun deleteById(id: ShelfItemId)
}
