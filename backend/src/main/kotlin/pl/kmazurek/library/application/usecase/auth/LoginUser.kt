package pl.kmazurek.library.application.usecase.auth

import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.infrastructure.security.JwtService
import pl.kmazurek.library.infrastructure.security.PasswordEncoderService

/**
 * Use Case: Login user
 */
@Service
class LoginUser(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoderService,
    private val jwtService: JwtService,
) {
    data class LoginResult(
        val user: User,
        val accessToken: String,
        val refreshToken: String,
    )

    fun execute(
        email: String,
        password: String,
    ): LoginResult {
        val user =
            userRepository.findByEmail(Email(email))
                ?: throw InvalidCredentialsException("Invalid email or password")

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid email or password")
        }

        if (!user.isActive) {
            throw AccountDisabledException("Account is disabled")
        }

        val accessToken = jwtService.generateAccessToken(user.id, user.email, user.role)
        val refreshToken = jwtService.generateRefreshToken(user.id)

        return LoginResult(user, accessToken, refreshToken)
    }
}

class InvalidCredentialsException(message: String) : RuntimeException(message)

class AccountDisabledException(message: String) : RuntimeException(message)
