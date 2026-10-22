package pl.kmazurek.library.application.usecase.auth

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.HashedPassword
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.infrastructure.security.JwtService
import pl.kmazurek.library.infrastructure.security.PasswordEncoderService

/**
 * Unit tests for LoginUser use case
 */
class LoginUserTest {
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoderService
    private lateinit var jwtService: JwtService
    private lateinit var loginUser: LoginUser

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        passwordEncoder = mockk()
        jwtService = mockk()
        loginUser = LoginUser(userRepository, passwordEncoder, jwtService)
    }

    @Test
    fun `should login successfully with valid credentials`() {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val user =
            User.create(
                Email(email),
                HashedPassword("hashed_password"),
                "Test User",
            )

        every { userRepository.findByEmail(Email(email)) } returns user
        every { passwordEncoder.matches(password, user.passwordHash) } returns true
        every { jwtService.generateAccessToken(user.id, user.email, user.role) } returns "access_token"
        every { jwtService.generateRefreshToken(user.id) } returns "refresh_token"

        // When
        val result = loginUser.execute(email, password)

        // Then
        assertEquals(user, result.user)
        assertEquals("access_token", result.accessToken)
        assertEquals("refresh_token", result.refreshToken)
    }

    @Test
    fun `should throw exception for invalid email`() {
        // Given
        val email = "nonexistent@example.com"
        every { userRepository.findByEmail(Email(email)) } returns null

        // When/Then
        assertThrows<InvalidCredentialsException> {
            loginUser.execute(email, "password123")
        }
    }

    @Test
    fun `should throw exception for invalid password`() {
        // Given
        val email = "test@example.com"
        val user =
            User.create(
                Email(email),
                HashedPassword("hashed_password"),
                "Test User",
            )

        every { userRepository.findByEmail(Email(email)) } returns user
        every { passwordEncoder.matches("wrong_password", user.passwordHash) } returns false

        // When/Then
        assertThrows<InvalidCredentialsException> {
            loginUser.execute(email, "wrong_password")
        }
    }

    @Test
    fun `should reject login for deactivated account`() {
        val email = "blocked@example.com"
        val user =
            User.create(
                Email(email),
                HashedPassword("hashed_password"),
                "Blocked User",
            ).deactivate()

        every { userRepository.findByEmail(Email(email)) } returns user
        every { passwordEncoder.matches("password123", user.passwordHash) } returns true

        assertThrows<AccountDisabledException> {
            loginUser.execute(email, "password123")
        }
    }
}
