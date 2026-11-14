package pl.kmazurek.library.infrastructure.security

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.model.user.UserRole
import pl.kmazurek.library.domain.repository.UserRepository

/**
 * Admin-only "view as user": a request carrying the [HEADER] is handled as if the named user had sent it,
 * so every page shows that user's data without any endpoint knowing about it.
 *
 * Guarded in one place, here, rather than per endpoint:
 * - only an administrator's token may carry the header;
 * - viewing is read-only – any request that could change data is refused, whatever the endpoint;
 * - the viewed account must exist.
 */
@Component
class ViewAsUserResolver(
    private val userRepository: UserRepository,
) {
    companion object {
        const val HEADER = "X-View-As-User"

        /** Marker authority so that code can tell a viewed request from the user's own. */
        const val AUTHORITY = "VIEW_AS"

        private val READ_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }

    sealed interface Result {
        data class ViewAs(val target: User) : Result

        data class Refused(val status: HttpStatus, val code: String, val message: String) : Result
    }

    fun resolve(
        requesterRole: UserRole,
        method: String,
        targetId: String,
    ): Result {
        if (!requesterRole.hasAdminPrivileges()) {
            return Result.Refused(
                HttpStatus.FORBIDDEN,
                "VIEW_AS_FORBIDDEN",
                "Only an administrator can view another user's account",
            )
        }
        if (method.uppercase() !in READ_METHODS) {
            return Result.Refused(
                HttpStatus.FORBIDDEN,
                "VIEW_AS_READ_ONLY",
                "Viewing another user's account is read-only",
            )
        }
        val target = parseUserId(targetId)?.let { userRepository.findById(it) }
        return target?.let { Result.ViewAs(it) }
            ?: Result.Refused(HttpStatus.NOT_FOUND, "VIEW_AS_USER_NOT_FOUND", "User to view not found")
    }

    private fun parseUserId(value: String): UserId? =
        try {
            UserId.fromString(value.trim())
        } catch (e: IllegalArgumentException) {
            null
        }
}
