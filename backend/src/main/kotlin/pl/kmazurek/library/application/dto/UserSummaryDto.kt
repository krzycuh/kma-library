package pl.kmazurek.library.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserRole
import java.time.LocalDateTime

/**
 * DTOs used for admin-only user management endpoints.
 */
data class UserSummaryDto(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(user: User): UserSummaryDto =
            UserSummaryDto(
                id = user.id.toString(),
                email = user.email.value,
                name = user.name,
                role = user.role,
                isActive = user.isActive,
                createdAt = user.createdAt,
            )
    }
}

data class SetUserActiveRequest(
    val active: Boolean,
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String,
)
