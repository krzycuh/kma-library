package pl.kmazurek.library.application.usecase.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.HashedPassword
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.UserRepository

class AdminUserUseCasesTest {
    private val userRepository: UserRepository = mockk()
    private val admin = UserId.generate()
    private val user = User.create(Email("u@example.com"), HashedPassword("h"), "User")

    @Test
    fun `admin can deactivate and reactivate a user`() {
        every { userRepository.findById(user.id) } returns user
        every { userRepository.save(any()) } answers { firstArg() }
        val useCase = SetUserActive(userRepository)

        val blocked = useCase.execute(user.id, false, admin)
        assertFalse(blocked.isActive)

        every { userRepository.findById(user.id) } returns blocked
        val unblocked = useCase.execute(user.id, true, admin)
        assertTrue(unblocked.isActive)
    }

    @Test
    fun `admin cannot deactivate self`() {
        assertThrows<CannotModifySelfException> { SetUserActive(userRepository).execute(admin, false, admin) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `admin reads a single user before viewing as them`() {
        every { userRepository.findById(user.id) } returns user
        assertEquals(user, GetUser(userRepository).execute(user.id))

        every { userRepository.findById(any()) } returns null
        assertThrows<UserNotFoundException> { GetUser(userRepository).execute(UserId.generate()) }
    }

    @Test
    fun `unknown user is not found`() {
        every { userRepository.findById(any()) } returns null
        assertThrows<UserNotFoundException> { SetUserActive(userRepository).execute(UserId.generate(), false, admin) }
    }
}
