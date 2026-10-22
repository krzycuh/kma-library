package pl.kmazurek.library.application.usecase.user

import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.UserRepository

/**
 * Use Case: Get current authenticated user
 */
@Service
class GetCurrentUser(
    private val userRepository: UserRepository,
) {
    fun execute(userId: UserId): User {
        return userRepository.findById(userId)
            ?: throw UserNotFoundException("User not found")
    }
}

class UserNotFoundException(message: String) : RuntimeException(message)
