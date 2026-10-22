package pl.kmazurek.library.application.usecase.user

import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.UserRepository

/**
 * Use Case: Update user profile
 */
@Service
class UpdateUserProfile(
    private val userRepository: UserRepository,
) {
    fun execute(
        userId: UserId,
        name: String,
        avatarUrl: String?,
    ): User {
        val user =
            userRepository.findById(userId)
                ?: throw UserNotFoundException("User not found")

        val updatedUser = user.updateProfile(name, avatarUrl)
        return userRepository.save(updatedUser)
    }
}
