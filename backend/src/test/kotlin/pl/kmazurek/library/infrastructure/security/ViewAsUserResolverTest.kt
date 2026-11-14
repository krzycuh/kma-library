package pl.kmazurek.library.infrastructure.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.HashedPassword
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.model.user.UserRole
import pl.kmazurek.library.domain.repository.UserRepository

class ViewAsUserResolverTest {
    private val userRepository: UserRepository = mockk()
    private val resolver = ViewAsUserResolver(userRepository)
    private val target = User.create(Email("anna@example.com"), HashedPassword("h"), "Anna")

    @Test
    fun `admin reads as the viewed user`() {
        every { userRepository.findById(target.id) } returns target

        val result = resolver.resolve(UserRole.ADMIN, "GET", target.id.toString())

        assertEquals(ViewAsUserResolver.Result.ViewAs(target), result)
    }

    @Test
    fun `a regular user may not view anyone`() {
        val result = resolver.resolve(UserRole.USER, "GET", target.id.toString())

        assertRefused(result, HttpStatus.FORBIDDEN, "VIEW_AS_FORBIDDEN")
        verify(exactly = 0) { userRepository.findById(any()) }
    }

    @Test
    fun `viewing is read-only whatever the endpoint`() {
        for (method in listOf("POST", "PUT", "PATCH", "DELETE", "post")) {
            val result = resolver.resolve(UserRole.ADMIN, method, target.id.toString())
            assertRefused(result, HttpStatus.FORBIDDEN, "VIEW_AS_READ_ONLY")
        }
        verify(exactly = 0) { userRepository.findById(any()) }
    }

    @Test
    fun `unknown or malformed account is not found`() {
        every { userRepository.findById(any()) } returns null

        val unknown = resolver.resolve(UserRole.ADMIN, "GET", UserId.generate().toString())
        assertRefused(unknown, HttpStatus.NOT_FOUND, "VIEW_AS_USER_NOT_FOUND")
        val malformed = resolver.resolve(UserRole.ADMIN, "GET", "not-a-uuid")
        assertRefused(malformed, HttpStatus.NOT_FOUND, "VIEW_AS_USER_NOT_FOUND")
    }

    private fun assertRefused(
        result: ViewAsUserResolver.Result,
        status: HttpStatus,
        code: String,
    ) {
        assertTrue(result is ViewAsUserResolver.Result.Refused, "expected refusal, got $result")
        result as ViewAsUserResolver.Result.Refused
        assertEquals(status, result.status)
        assertEquals(code, result.code)
    }
}
