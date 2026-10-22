package pl.kmazurek.library.infrastructure.api.rest.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.Filter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiting configuration using Bucket4j
 * Implements token bucket algorithm for API rate limiting
 */
@Configuration
class RateLimitingConfig {
    private val cache = ConcurrentHashMap<String, Bucket>()

    /**
     * Rate limiting filter
     * Limits API requests per IP address
     */
    @Bean
    @Order(1)
    fun rateLimitingFilter(): Filter =
        Filter { request, response, chain ->
            val httpRequest = request as HttpServletRequest
            val httpResponse = response as HttpServletResponse

            val key = getClientIP(httpRequest)
            val bucket = resolveBucket(key)

            if (bucket.tryConsume(1)) {
                // Add rate limit headers
                httpResponse.addHeader("X-Rate-Limit-Remaining", bucket.availableTokens.toString())
                chain.doFilter(request, response)
            } else {
                httpResponse.status = HttpStatus.TOO_MANY_REQUESTS.value()
                httpResponse.contentType = "application/json"
                httpResponse.writer.write(
                    """{"error": "Too many requests. Please try again later."}""",
                )
            }
        }

    private fun resolveBucket(key: String): Bucket =
        cache.computeIfAbsent(key) {
            Bucket
                .builder()
                .addLimit(
                    Bandwidth.builder()
                        .capacity(100)
                        .refillIntervally(100, Duration.ofMinutes(1))
                        .build(),
                ).build()
        }

    private fun getClientIP(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (xForwardedFor.isNullOrBlank()) {
            request.remoteAddr
        } else {
            xForwardedFor.split(",")[0].trim()
        }
    }
}
