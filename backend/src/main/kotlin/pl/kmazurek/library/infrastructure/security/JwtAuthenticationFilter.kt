package pl.kmazurek.library.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT Authentication Filter
 * Intercepts requests and validates JWT tokens
 */
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
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

                val authorities =
                    listOf(
                        SimpleGrantedAuthority("ROLE_${role.name}"),
                    )

                val authentication =
                    UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        email.value,
                        authorities,
                    )

                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: Exception) {
            logger.error("JWT authentication failed: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }
}
