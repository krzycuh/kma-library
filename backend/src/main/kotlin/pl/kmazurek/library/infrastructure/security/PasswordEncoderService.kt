package pl.kmazurek.library.infrastructure.security

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.model.user.HashedPassword

/**
 * Service for password hashing and verification
 * Uses BCrypt for secure password hashing
 */
@Service
class PasswordEncoderService(
    private val passwordEncoder: PasswordEncoder,
) {
    /**
     * Hash a plain text password
     */
    fun hash(plainPassword: String): HashedPassword {
        require(plainPassword.isNotBlank()) { "Password cannot be blank" }
        require(plainPassword.length >= 8) { "Password must be at least 8 characters" }

        val hashed = passwordEncoder.encode(plainPassword)
        return HashedPassword(hashed)
    }

    /**
     * Verify if plain password matches the hashed password
     */
    fun matches(
        plainPassword: String,
        hashedPassword: HashedPassword,
    ): Boolean {
        return passwordEncoder.matches(plainPassword, hashedPassword.value)
    }
}
