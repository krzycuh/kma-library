package pl.kmazurek.library.infrastructure.api.rest.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.application.dto.ChangePasswordRequest
import pl.kmazurek.library.application.dto.UpdateProfileRequest
import pl.kmazurek.library.application.dto.UserDto
import pl.kmazurek.library.application.usecase.user.ChangeUserPassword
import pl.kmazurek.library.application.usecase.user.GetCurrentUser
import pl.kmazurek.library.application.usecase.user.UpdateUserProfile
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.infrastructure.logging.AuditLogger

/**
 * REST Controller for user profile endpoints
 */
@RestController
@RequestMapping("/api/users")
class UserController(
    private val getCurrentUser: GetCurrentUser,
    private val updateUserProfile: UpdateUserProfile,
    private val changeUserPassword: ChangeUserPassword,
    private val auditLogger: AuditLogger,
) {
    @GetMapping("/me")
    fun getCurrentUserProfile(
        @AuthenticationPrincipal userIdString: String,
    ): ResponseEntity<UserDto> {
        val user = getCurrentUser.execute(UserId.fromString(userIdString))
        return ResponseEntity.ok(UserDto.fromDomain(user))
    }

    @PutMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal userIdString: String,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): ResponseEntity<UserDto> {
        val user = updateUserProfile.execute(UserId.fromString(userIdString), request.name, request.avatarUrl)
        auditLogger.log("PROFILE_UPDATED")
        return ResponseEntity.ok(UserDto.fromDomain(user))
    }

    @PatchMapping("/me/password")
    fun changePassword(
        @AuthenticationPrincipal userIdString: String,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<Map<String, String>> {
        changeUserPassword.execute(UserId.fromString(userIdString), request.currentPassword, request.newPassword)
        auditLogger.log("PASSWORD_CHANGED")
        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }
}
