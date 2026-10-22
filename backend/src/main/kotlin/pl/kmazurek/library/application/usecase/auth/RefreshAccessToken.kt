package pl.kmazurek.library.application.usecase.auth

import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.infrastructure.security.JwtService

/**
 * Use Case: Refresh access token using refresh token
 */
@Service
class RefreshAccessToken(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
) {
    fun execute(refreshToken: String): String {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw InvalidRefreshTokenException("Invalid or expired refresh token")
        }

        val userId = jwtService.extractUserId(refreshToken)
        val user =
            userRepository.findById(userId)
                ?: throw InvalidRefreshTokenException("User not found")

        if (!user.isActive) {
            throw InvalidRefreshTokenException("Account is disabled")
        }

        return jwtService.generateAccessToken(user.id, user.email, user.role)
    }
}

class InvalidRefreshTokenException(message: String) : RuntimeException(message)
