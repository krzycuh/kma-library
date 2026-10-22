package pl.kmazurek.library.domain.model.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for User domain entity
 * Following TDD principles - test domain logic
 */
class UserTest {
    @Test
    fun `should create user with valid data`() {
        // Given
        val email = Email("test@example.com")
        val password = HashedPassword("hashedpassword123")
        val name = "Test User"

        // When
        val user = User.create(email, password, name)

        // Then
        assertEquals(email, user.email)
        assertEquals(password, user.passwordHash)
        assertEquals(name, user.name)
        assertEquals(UserRole.USER, user.role)
    }

    @Test
    fun `should reject blank name`() {
        // Given
        val email = Email("test@example.com")
        val password = HashedPassword("hashedpassword123")

        // When/Then
        assertThrows<IllegalArgumentException> {
            User.create(email, password, "")
        }
    }

    @Test
    fun `should reject name exceeding 255 characters`() {
        // Given
        val email = Email("test@example.com")
        val password = HashedPassword("hashedpassword123")
        val longName = "a".repeat(256)

        // When/Then
        assertThrows<IllegalArgumentException> {
            User.create(email, password, longName)
        }
    }

    @Test
    fun `should update profile successfully`() {
        // Given
        val user =
            User.create(
                Email("test@example.com"),
                HashedPassword("hashedpassword123"),
                "Old Name",
            )
        val newName = "New Name"
        val newAvatarUrl = "https://example.com/avatar.jpg"

        // When
        val updatedUser = user.updateProfile(newName, newAvatarUrl)

        // Then
        assertEquals(newName, updatedUser.name)
        assertEquals(newAvatarUrl, updatedUser.avatarUrl)
        assertTrue(updatedUser.updatedAt.isAfter(user.createdAt))
    }

    @Test
    fun `should update password successfully`() {
        // Given
        val user =
            User.create(
                Email("test@example.com"),
                HashedPassword("oldpasswordhash"),
                "Test User",
            )
        val newPasswordHash = HashedPassword("newpasswordhash")

        // When
        val updatedUser = user.updatePassword(newPasswordHash)

        // Then
        assertEquals(newPasswordHash, updatedUser.passwordHash)
    }

    @Test
    fun `admin user should have admin privileges`() {
        // Given
        val user =
            User.create(
                Email("admin@example.com"),
                HashedPassword("hashedpassword123"),
                "Admin User",
                role = UserRole.ADMIN,
            )

        // When/Then
        assertTrue(user.isAdmin())
    }
}
