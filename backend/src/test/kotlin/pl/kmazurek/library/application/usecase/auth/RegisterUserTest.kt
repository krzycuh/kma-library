package pl.kmazurek.library.application.usecase.auth

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.application.usecase.shelf.CreateDefaultLibrary
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.HashedPassword
import pl.kmazurek.library.domain.model.user.UserRole
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.infrastructure.security.PasswordEncoderService

/**
 * Unit tests for RegisterUser use case
 */
class RegisterUserTest {
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoderService
    private lateinit var createDefaultLibrary: CreateDefaultLibrary
    private lateinit var registerUser: RegisterUser

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        passwordEncoder = mockk()
        createDefaultLibrary = mockk(relaxed = true)
        registerUser = RegisterUser(userRepository, passwordEncoder, createDefaultLibrary, registrationEnabled = true)
    }

    @Test
    fun `should register new user with USER role and create default library`() {
        val email = "test@example.com"
        every { userRepository.existsByEmail(Email(email)) } returns false
        every { passwordEncoder.hash("password123") } returns HashedPassword("hashed_password")
        every { userRepository.save(any()) } answers { firstArg() }

        val user = registerUser.execute(email, "password123", "Test User")

        assertEquals(email, user.email.value)
        assertEquals("Test User", user.name)
        assertEquals(UserRole.USER, user.role)
        verify { userRepository.save(any()) }
        verify { createDefaultLibrary.execute(user) }
    }

    @Test
    fun `should throw exception when email already exists`() {
        val email = "existing@example.com"
        every { userRepository.existsByEmail(Email(email)) } returns true

        assertThrows<EmailAlreadyExistsException> {
            registerUser.execute(email, "password123", "Test User")
        }
    }

    @Test
    fun `should refuse registration when disabled`() {
        val disabled = RegisterUser(userRepository, passwordEncoder, createDefaultLibrary, registrationEnabled = false)

        assertThrows<RegistrationDisabledException> {
            disabled.execute("new@example.com", "password123", "New User")
        }
        verify(exactly = 0) { userRepository.save(any()) }
    }
}
