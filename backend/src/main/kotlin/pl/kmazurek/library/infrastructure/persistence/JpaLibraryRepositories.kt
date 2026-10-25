package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pl.kmazurek.library.infrastructure.persistence.entity.LibraryJpaEntity
import pl.kmazurek.library.infrastructure.persistence.entity.LibraryMemberJpaEntity
import pl.kmazurek.library.infrastructure.persistence.entity.LibraryMemberKey
import java.util.UUID

@Repository
interface JpaLibraryRepository : JpaRepository<LibraryJpaEntity, UUID> {
    @Query(
        """
        SELECT l FROM LibraryJpaEntity l
        WHERE l.id IN (SELECT m.key.libraryId FROM LibraryMemberJpaEntity m WHERE m.key.userId = :userId)
        ORDER BY l.createdAt ASC
        """,
    )
    fun findByMember(
        @Param("userId") userId: UUID,
    ): List<LibraryJpaEntity>
}

@Repository
interface JpaLibraryMemberRepository : JpaRepository<LibraryMemberJpaEntity, LibraryMemberKey> {
    fun findAllByKeyLibraryIdOrderByCreatedAtAsc(libraryId: UUID): List<LibraryMemberJpaEntity>
}
