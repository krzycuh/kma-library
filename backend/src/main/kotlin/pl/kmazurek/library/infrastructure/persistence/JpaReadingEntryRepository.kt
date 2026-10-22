package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pl.kmazurek.library.infrastructure.persistence.entity.ReadingEntryJpaEntity
import java.util.UUID

@Repository
interface JpaReadingEntryRepository : JpaRepository<ReadingEntryJpaEntity, UUID> {
    @Query(
        """
        SELECT r FROM ReadingEntryJpaEntity r
        JOIN BookJpaEntity b ON b.id = r.bookId
        WHERE r.readerId = :readerId
          AND (:year IS NULL OR r.finishedYear = :year)
          AND (:rating IS NULL OR r.rating = :rating)
          AND (:bookId IS NULL OR r.bookId = :bookId)
          AND (:pattern IS NULL OR LOWER(b.title) LIKE :pattern OR LOWER(b.authors) LIKE :pattern)
        ORDER BY r.finishedYear DESC NULLS LAST, r.finishedMonth DESC NULLS LAST, r.createdAt DESC
        """,
    )
    fun findByReader(
        @Param("readerId") readerId: UUID,
        @Param("year") year: Short?,
        @Param("rating") rating: Short?,
        @Param("bookId") bookId: UUID?,
        @Param("pattern") pattern: String?,
        pageable: Pageable,
    ): Page<ReadingEntryJpaEntity>

    fun findAllByReaderIdAndFinishedYear(
        readerId: UUID,
        finishedYear: Short,
    ): List<ReadingEntryJpaEntity>

    fun countByReaderId(readerId: UUID): Long

    @Query(
        """
        SELECT DISTINCT r.finishedYear FROM ReadingEntryJpaEntity r
        WHERE r.readerId = :readerId AND r.finishedYear IS NOT NULL
        ORDER BY r.finishedYear DESC
        """,
    )
    fun findDistinctYears(
        @Param("readerId") readerId: UUID,
    ): List<Short>
}
