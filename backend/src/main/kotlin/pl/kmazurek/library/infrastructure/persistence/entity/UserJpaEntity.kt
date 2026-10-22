package pl.kmazurek.library.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

/**
 * JPA Entity for users table - infrastructure layer, separate from domain model
 */
@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id
    @Column(columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 255)
    var email: String,
    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,
    @Column(nullable = false, length = 255)
    var name: String,
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var role: UserRoleJpa,
    @Column(name = "avatar_url", length = 500)
    var avatarUrl: String? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

enum class UserRoleJpa {
    USER,
    ADMIN,
}
