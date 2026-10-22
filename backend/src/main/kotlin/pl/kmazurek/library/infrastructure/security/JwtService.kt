package pl.kmazurek.library.infrastructure.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.model.user.UserRole
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

/**
 * Service for JWT token generation and validation
 */
@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-hours:24}") private val expirationHours: Long,
    @Value("\${jwt.refresh-expiration-days:7}") private val refreshExpirationDays: Long,
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    /**
     * Generate access token
     */
    fun generateAccessToken(
        userId: UserId,
        email: Email,
        role: UserRole,
    ): String {
        val now = Instant.now()
        val expiration = now.plus(expirationHours, ChronoUnit.HOURS)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email.value)
            .claim("role", role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact()
    }

    /**
     * Generate refresh token
     */
    fun generateRefreshToken(userId: UserId): String {
        val now = Instant.now()
        val expiration = now.plus(refreshExpirationDays, ChronoUnit.DAYS)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact()
    }

    /**
     * Extract user ID from token
     */
    fun extractUserId(token: String): UserId {
        val claims = extractAllClaims(token)
        return UserId.fromString(claims.subject)
    }

    /**
     * Extract email from token
     */
    fun extractEmail(token: String): Email {
        val claims = extractAllClaims(token)
        return Email(claims["email"] as String)
    }

    /**
     * Extract role from token
     */
    fun extractRole(token: String): UserRole {
        val claims = extractAllClaims(token)
        return UserRole.valueOf(claims["role"] as String)
    }

    /**
     * Validate token
     */
    fun isTokenValid(token: String): Boolean {
        return try {
            val claims = extractAllClaims(token)
            !isTokenExpired(claims)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if token is a refresh token
     */
    fun isRefreshToken(token: String): Boolean {
        return try {
            val claims = extractAllClaims(token)
            claims["type"] == "refresh"
        } catch (e: Exception) {
            false
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun isTokenExpired(claims: Claims): Boolean {
        return claims.expiration.before(Date())
    }
}
