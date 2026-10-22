package pl.kmazurek.library.application.usecase.user

import org.springframework.stereotype.Service
import pl.kmazurek.library.application.dto.PagedResponse
import pl.kmazurek.library.application.dto.UserSummaryDto
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.infrastructure.security.PasswordEncoderService

data class ListUsersQuery(
    val searchTerm: String? = null,
    val page: Int = 0,
    val pageSize: Int = 20,
)

/**
 * Use Case (admin): list users
 */
@Service
class ListUsers(
    private val userRepository: UserRepository,
) {
    fun execute(query: ListUsersQuery): PagedResponse<UserSummaryDto> {
        val (users, total) = userRepository.findAll(query.searchTerm, query.page, query.pageSize)
        return PagedResponse.of(users.map { UserSummaryDto.fromDomain(it) }, query.page, query.pageSize, total)
    }
}

/**
 * Use Case (admin): block / unblock a user account
 */
@Service
class SetUserActive(
    private val userRepository: UserRepository,
) {
    fun execute(
        userId: UserId,
        active: Boolean,
        requestedBy: UserId,
    ): User {
        if (userId == requestedBy && !active) {
            throw CannotModifySelfException("You cannot deactivate your own account")
        }
        val user = userRepository.findById(userId) ?: throw UserNotFoundException("User not found")
        if (user.isActive == active) return user
        val updated = if (active) user.activate() else user.deactivate()
        return userRepository.save(updated)
    }
}

/**
 * Use Case (admin): reset a user's password
 */
@Service
class ResetUserPassword(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoderService,
) {
    fun execute(
        userId: UserId,
        newPassword: String,
    ) {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException("User not found")
        userRepository.save(user.updatePassword(passwordEncoder.hash(newPassword)))
    }
}

class CannotModifySelfException(message: String) : RuntimeException(message)
