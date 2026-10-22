package pl.kmazurek.library.application.usecase.user

import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.infrastructure.security.PasswordEncoderService

/**
 * Use Case: Change user password
 */
@Service
class ChangeUserPassword(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoderService,
) {
    fun execute(
        userId: UserId,
        currentPassword: String,
        newPassword: String,
    ) {
        val user =
            userRepository.findById(userId)
                ?: throw UserNotFoundException("User not found")

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            throw InvalidPasswordException("Current password is incorrect")
        }

        // Hash new password and update
        val newPasswordHash = passwordEncoder.hash(newPassword)
        val updatedUser = user.updatePassword(newPasswordHash)
        userRepository.save(updatedUser)
    }
}

class InvalidPasswordException(message: String) : RuntimeException(message)
