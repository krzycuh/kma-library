package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pl.kmazurek.library.infrastructure.persistence.entity.BookJpaEntity
import java.util.UUID

@Repository
interface JpaBookRepository : JpaRepository<BookJpaEntity, UUID> {
    fun findByIsbn13(isbn13: String): BookJpaEntity?

    fun findFirstByNormalizedKey(normalizedKey: String): BookJpaEntity?

    @Query(
        """
        SELECT b FROM BookJpaEntity b
        WHERE LOWER(b.title) LIKE :pattern OR LOWER(b.authors) LIKE :pattern OR b.isbn13 = :exact
        ORDER BY b.title ASC
        """,
    )
    fun search(
        @Param("pattern") pattern: String,
        @Param("exact") exact: String,
        pageable: Pageable,
    ): List<BookJpaEntity>

    // Bulk delete runs immediately, so a following update may take over the deleted row's unique ISBN
    @Modifying
    @Query("DELETE FROM BookJpaEntity b WHERE b.id = :id")
    fun deleteNow(
        @Param("id") id: UUID,
    ): Int
}
