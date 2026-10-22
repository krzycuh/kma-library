package pl.kmazurek.library.domain.model.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for Email value object
 */
class EmailTest {
    @Test
    fun `should create valid email`() {
        // Given/When
        val email = Email("test@example.com")

        // Then
        assertEquals("test@example.com", email.value)
    }

    @Test
    fun `should reject blank email`() {
        assertThrows<IllegalArgumentException> {
            Email("")
        }
    }

    @Test
    fun `should reject invalid email format`() {
        assertThrows<IllegalArgumentException> {
            Email("notanemail")
        }
    }

    @Test
    fun `should accept valid email formats`() {
        // These should all succeed
        Email("user@example.com")
        Email("user.name@example.com")
        Email("user+tag@example.co.uk")
        Email("123@test.org")
    }
}
