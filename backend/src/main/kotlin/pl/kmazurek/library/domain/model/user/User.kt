package pl.kmazurek.library.domain.model.user

import java.time.LocalDateTime

/**
 * User Aggregate Root
 * Represents a user in the Identity & Access bounded context
 * Domain entity - no framework dependencies
 */
data class User(
    val id: UserId,
    val email: Email,
    val passwordHash: HashedPassword,
    val name: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        require(name.isNotBlank()) { "User name cannot be blank" }
        require(name.length <= 255) { "User name cannot exceed 255 characters" }
    }

    fun updateProfile(
        name: String,
        avatarUrl: String?,
    ): User {
        require(name.isNotBlank()) { "User name cannot be blank" }
        require(name.length <= 255) { "User name cannot exceed 255 characters" }

        return copy(
            name = name,
            avatarUrl = avatarUrl,
            updatedAt = LocalDateTime.now(),
        )
    }

    fun updatePassword(newPasswordHash: HashedPassword): User {
        return copy(
            passwordHash = newPasswordHash,
            updatedAt = LocalDateTime.now(),
        )
    }

    fun deactivate(): User {
        require(isActive) { "User is already deactivated" }
        return copy(isActive = false, updatedAt = LocalDateTime.now())
    }

    fun activate(): User {
        require(!isActive) { "User is already active" }
        return copy(isActive = true, updatedAt = LocalDateTime.now())
    }

    fun isAdmin() = role.hasAdminPrivileges()

    companion object {
        fun create(
            email: Email,
            passwordHash: HashedPassword,
            name: String,
            role: UserRole = UserRole.USER,
            avatarUrl: String? = null,
        ): User {
            val now = LocalDateTime.now()
            return User(
                id = UserId.generate(),
                email = email,
                passwordHash = passwordHash,
                name = name,
                role = role,
                avatarUrl = avatarUrl,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
