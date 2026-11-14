package pl.kmazurek.library.infrastructure.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.HashedPassword
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.model.user.UserRole
import pl.kmazurek.library.domain.repository.UserRepository

class JwtAuthenticationFilterTest {
    private val jwtService = JwtService("test-secret-that-is-long-enough-for-hmac-sha-256", 1, 1)
    private val userRepository: UserRepository = mockk()
    private val filter = JwtAuthenticationFilter(jwtService, ViewAsUserResolver(userRepository), jacksonObjectMapper())

    private val adminId = UserId.generate()
    private val adminEmail = Email("admin@example.com")
    private val viewed = User.create(Email("anna@example.com"), HashedPassword("h"), "Anna")

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    @Test
    fun `without the header the token holder is the principal`() {
        val response = run(tokenFor(adminId, adminEmail, UserRole.ADMIN), method = "POST")

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals(adminId.toString(), auth.principal)
        assertEquals(adminEmail.value, auth.credentials)
        assertEquals(setOf("ROLE_ADMIN"), auth.authorities.map { it.authority }.toSet())
        assertFalse(auth.isViewingAsUser())
        assertEquals(200, response.status)
    }

    @Test
    fun `admin with the header becomes the viewed user, keeping their name in the credentials`() {
        every { userRepository.findById(viewed.id) } returns viewed

        val response = run(tokenFor(adminId, adminEmail, UserRole.ADMIN), viewAs = viewed.id.toString())

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals(viewed.id.toString(), auth.principal)
        assertEquals("admin@example.com (as anna@example.com)", auth.credentials)
        assertEquals(setOf("ROLE_USER", "VIEW_AS"), auth.authorities.map { it.authority }.toSet())
        assertTrue(auth.isViewingAsUser())
        assertEquals(200, response.status)
    }

    @Test
    fun `admin with the header may not change anything`() {
        val response = run(tokenFor(adminId, adminEmail, UserRole.ADMIN), viewAs = viewed.id.toString(), method = "POST")

        assertEquals(403, response.status)
        assertTrue(response.contentAsString.contains("\"code\":\"VIEW_AS_READ_ONLY\""), response.contentAsString)
        assertNull(SecurityContextHolder.getContext().authentication)
        assertFalse(chainReached)
    }

    @Test
    fun `regular user with the header is refused`() {
        val response = run(tokenFor(viewed.id, viewed.email, UserRole.USER), viewAs = adminId.toString())

        assertEquals(403, response.status)
        assertTrue(response.contentAsString.contains("\"code\":\"VIEW_AS_FORBIDDEN\""), response.contentAsString)
        assertFalse(chainReached)
    }

    @Test
    fun `header naming a missing account is not found`() {
        every { userRepository.findById(any()) } returns null

        val response = run(tokenFor(adminId, adminEmail, UserRole.ADMIN), viewAs = UserId.generate().toString())

        assertEquals(404, response.status)
        assertTrue(response.contentAsString.contains("\"code\":\"VIEW_AS_USER_NOT_FOUND\""), response.contentAsString)
        assertFalse(chainReached)
    }

    private var chainReached = false

    private fun run(
        token: String,
        viewAs: String? = null,
        method: String = "GET",
    ): MockHttpServletResponse {
        val request = MockHttpServletRequest(method, "/api/dashboard")
        request.addHeader("Authorization", "Bearer $token")
        viewAs?.let { request.addHeader(ViewAsUserResolver.HEADER, it) }
        val response = MockHttpServletResponse()
        chainReached = false
        filter.doFilter(request, response) { _, _ -> chainReached = true }
        return response
    }

    private fun tokenFor(
        id: UserId,
        email: Email,
        role: UserRole,
    ) = jwtService.generateAccessToken(id, email, role)
}
