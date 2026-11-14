package pl.kmazurek.library.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.model.user.UserRole
import pl.kmazurek.library.infrastructure.api.rest.controller.ErrorResponse

/**
 * JWT Authentication Filter
 * Intercepts requests and validates JWT tokens.
 *
 * An administrator may add the [ViewAsUserResolver.HEADER]: the request is then authenticated as the viewed user
 * (read-only, see [ViewAsUserResolver]), while the log and audit entries still name the administrator.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val viewAsUserResolver: ViewAsUserResolver,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val token = authHeader.substring(7)

            if (jwtService.isTokenValid(token) && !jwtService.isRefreshToken(token)) {
                val userId = jwtService.extractUserId(token)
                val email = jwtService.extractEmail(token)
                val role = jwtService.extractRole(token)

                val viewAsHeader = request.getHeader(ViewAsUserResolver.HEADER)
                val authentication =
                    if (viewAsHeader.isNullOrBlank()) {
                        ownAuthentication(userId, email, role)
                    } else {
                        when (val result = viewAsUserResolver.resolve(role, request.method, viewAsHeader)) {
                            is ViewAsUserResolver.Result.ViewAs -> viewAsAuthentication(result.target, email)
                            is ViewAsUserResolver.Result.Refused -> {
                                refuse(response, result)
                                return
                            }
                        }
                    }

                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: Exception) {
            logger.error("JWT authentication failed: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun ownAuthentication(
        userId: UserId,
        email: Email,
        role: UserRole,
    ): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken(
            userId.toString(),
            email.value,
            listOf(SimpleGrantedAuthority("ROLE_${role.name}")),
        )

    /**
     * Principal and role are the viewed user's, so endpoints answer exactly as they would for them.
     * The credentials (used by request and audit logging as the "user") keep naming the administrator.
     */
    private fun viewAsAuthentication(
        target: User,
        adminEmail: Email,
    ): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken(
            target.id.toString(),
            "${adminEmail.value} (as ${target.email.value})",
            listOf(
                SimpleGrantedAuthority("ROLE_${target.role.name}"),
                SimpleGrantedAuthority(ViewAsUserResolver.AUTHORITY),
            ),
        )

    private fun refuse(
        response: HttpServletResponse,
        refused: ViewAsUserResolver.Result.Refused,
    ) {
        logger.warn("View-as request refused: ${refused.code}")
        response.status = refused.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.writer, ErrorResponse(refused.message, refused.code))
    }
}

/** True when the request is an administrator's read-only view of another user's account. */
fun Authentication.isViewingAsUser(): Boolean = authorities.any { it.authority == ViewAsUserResolver.AUTHORITY }
