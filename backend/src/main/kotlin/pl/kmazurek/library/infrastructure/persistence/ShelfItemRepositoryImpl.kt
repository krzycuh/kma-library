package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.ShelfItem
import pl.kmazurek.library.domain.model.shelf.ShelfItemId
import pl.kmazurek.library.domain.model.shelf.ShelfItemStatus
import pl.kmazurek.library.domain.repository.ShelfItemFilter
import pl.kmazurek.library.domain.repository.ShelfItemRepository
import pl.kmazurek.library.infrastructure.persistence.mapper.ShelfItemMapper

@Component
class ShelfItemRepositoryImpl(
    private val jpaRepository: JpaShelfItemRepository,
) : ShelfItemRepository {
    override fun findById(id: ShelfItemId): ShelfItem? = jpaRepository.findById(id.value).map { ShelfItemMapper.toDomain(it) }.orElse(null)

    override fun findByLibrary(
        libraryId: LibraryId,
        filter: ShelfItemFilter,
        page: Int,
        pageSize: Int,
    ): Pair<List<ShelfItem>, Long> {
        val result =
            jpaRepository.findByLibrary(
                libraryId = libraryId.value,
                status = filter.status?.name,
                bookId = filter.bookId?.value,
                pattern = filter.searchTerm?.lowercase()?.let { "%$it%" },
                pageable = PageRequest.of(page, pageSize),
            )
        return result.content.map { ShelfItemMapper.toDomain(it) } to result.totalElements
    }

    override fun findLentByLibrary(libraryId: LibraryId): List<ShelfItem> =
        jpaRepository.findAllByLibraryIdAndStatus(libraryId.value, ShelfItemStatus.LENT.name).map { ShelfItemMapper.toDomain(it) }

    override fun countByLibraryAndStatus(
        libraryId: LibraryId,
        status: ShelfItemStatus,
    ): Long = jpaRepository.countByLibraryIdAndStatus(libraryId.value, status.name)

    @Transactional
    override fun save(item: ShelfItem): ShelfItem {
        val existing = jpaRepository.findById(item.id.value).orElse(null)
        val entity = ShelfItemMapper.toJpa(item, existing)
        return ShelfItemMapper.toDomain(jpaRepository.save(entity))
    }

    override fun deleteById(id: ShelfItemId) = jpaRepository.deleteById(id.value)

    @Transactional
    override fun reassignBook(
        from: BookId,
        to: BookId,
    ): Int = jpaRepository.reassignBook(from.value, to.value)
}
