package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pl.kmazurek.library.infrastructure.persistence.entity.UserJpaEntity
import java.util.UUID

/**
 * Spring Data JPA repository for users
 */
@Repository
interface JpaUserRepository : JpaRepository<UserJpaEntity, UUID> {
    fun findByEmail(email: String): UserJpaEntity?

    fun existsByEmail(email: String): Boolean

    @Query(
        """
        SELECT u FROM UserJpaEntity u
        WHERE (
            :searchPattern IS NULL
            OR LOWER(u.name) LIKE :searchPattern
            OR LOWER(u.email) LIKE :searchPattern
        )
        ORDER BY u.createdAt DESC
        """,
    )
    fun search(
        @Param("searchPattern") searchPattern: String?,
        pageable: Pageable,
    ): Page<UserJpaEntity>
}
