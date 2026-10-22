package pl.kmazurek.library.infrastructure.api.rest.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(2)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    companion object {
        private val QUIET_PATHS = setOf("/api/health", "/actuator/health")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val start = System.currentTimeMillis()

        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - start
            val method = request.method
            val path = request.requestURI
            val query = request.queryString
            val fullPath = if (query != null) "$path?$query" else path
            val status = response.status
            val userEmail = resolveUserEmail()
            val ip = resolveClientIp(request)

            if (QUIET_PATHS.contains(path)) {
                log.debug("HTTP {} {} | user={} | ip={} | status={} | {}ms", method, fullPath, userEmail, ip, status, duration)
            } else {
                log.info("HTTP {} {} | user={} | ip={} | status={} | {}ms", method, fullPath, userEmail, ip, status, duration)
            }
        }
    }

    private fun resolveUserEmail(): String {
        val auth = SecurityContextHolder.getContext().authentication ?: return "anonymous"
        if (!auth.isAuthenticated || auth.principal == "anonymousUser") return "anonymous"
        return auth.credentials?.toString() ?: "anonymous"
    }

    private fun resolveClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (xForwardedFor.isNullOrBlank()) {
            request.remoteAddr
        } else {
            xForwardedFor.split(",")[0].trim()
        }
    }
}
