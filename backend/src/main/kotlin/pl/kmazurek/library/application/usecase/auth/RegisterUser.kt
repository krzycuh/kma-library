package pl.kmazurek.library.application.usecase.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.library.application.usecase.shelf.CreateDefaultLibrary
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserRole
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.infrastructure.security.PasswordEncoderService

/**
 * Use Case: Register a new user and create their default library
 */
@Service
class RegisterUser(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoderService,
    private val createDefaultLibrary: CreateDefaultLibrary,
    @Value("\${app.registration.enabled:true}") private val registrationEnabled: Boolean = true,
) {
    @Transactional
    fun execute(
        email: String,
        password: String,
        name: String,
    ): User {
        if (!registrationEnabled) {
            throw RegistrationDisabledException("Registration is disabled")
        }

        val userEmail = Email(email)

        if (userRepository.existsByEmail(userEmail)) {
            throw EmailAlreadyExistsException("Email $email is already registered")
        }

        val hashedPassword = passwordEncoder.hash(password)
        val user =
            User.create(
                email = userEmail,
                passwordHash = hashedPassword,
                name = name,
                role = UserRole.USER,
            )

        val saved = userRepository.save(user)
        createDefaultLibrary.execute(saved)
        return saved
    }
}

class EmailAlreadyExistsException(message: String) : RuntimeException(message)

class RegistrationDisabledException(message: String) : RuntimeException(message)
