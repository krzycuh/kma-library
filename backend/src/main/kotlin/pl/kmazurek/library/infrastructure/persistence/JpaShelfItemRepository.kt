package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pl.kmazurek.library.infrastructure.persistence.entity.ShelfItemJpaEntity
import java.util.UUID

@Repository
interface JpaShelfItemRepository : JpaRepository<ShelfItemJpaEntity, UUID> {
    @Query(
        value = """
        SELECT s FROM ShelfItemJpaEntity s
        JOIN BookJpaEntity b ON b.id = s.bookId
        WHERE s.libraryId = :libraryId
          AND (:status IS NULL OR s.status = :status)
          AND (:bookId IS NULL OR s.bookId = :bookId)
          AND (:pattern IS NULL OR LOWER(b.title) LIKE :pattern OR LOWER(b.authors) LIKE :pattern)
        ORDER BY s.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(s) FROM ShelfItemJpaEntity s
        JOIN BookJpaEntity b ON b.id = s.bookId
        WHERE s.libraryId = :libraryId
          AND (:status IS NULL OR s.status = :status)
          AND (:bookId IS NULL OR s.bookId = :bookId)
          AND (:pattern IS NULL OR LOWER(b.title) LIKE :pattern OR LOWER(b.authors) LIKE :pattern)
        """,
    )
    fun findByLibrary(
        @Param("libraryId") libraryId: UUID,
        @Param("status") status: String?,
        @Param("bookId") bookId: UUID?,
        @Param("pattern") pattern: String?,
        pageable: Pageable,
    ): Page<ShelfItemJpaEntity>

    fun findAllByLibraryIdAndStatus(
        libraryId: UUID,
        status: String,
    ): List<ShelfItemJpaEntity>

    fun countByLibraryIdAndStatus(
        libraryId: UUID,
        status: String,
    ): Long
}
