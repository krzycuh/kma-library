package pl.kmazurek.library.infrastructure.api.rest.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.application.dto.PagedResponse
import pl.kmazurek.library.application.dto.ResetPasswordRequest
import pl.kmazurek.library.application.dto.SetUserActiveRequest
import pl.kmazurek.library.application.dto.UserSummaryDto
import pl.kmazurek.library.application.usecase.user.GetUser
import pl.kmazurek.library.application.usecase.user.ListUsers
import pl.kmazurek.library.application.usecase.user.ListUsersQuery
import pl.kmazurek.library.application.usecase.user.ResetUserPassword
import pl.kmazurek.library.application.usecase.user.SetUserActive
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.infrastructure.logging.AuditLogger
import pl.kmazurek.library.infrastructure.security.ViewAsUserResolver

/**
 * Admin-only endpoints for managing user accounts.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
class AdminUserController(
    private val listUsers: ListUsers,
    private val getUser: GetUser,
    private val setUserActive: SetUserActive,
    private val resetUserPassword: ResetUserPassword,
    private val auditLogger: AuditLogger,
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") pageSize: Int,
    ): ResponseEntity<PagedResponse<UserSummaryDto>> {
        return ResponseEntity.ok(listUsers.execute(ListUsersQuery(searchTerm, page, pageSize)))
    }

    /**
     * Starts viewing the app as [id]: confirms the account exists and leaves an audit trail.
     * The viewing itself is the client sending [ViewAsUserResolver.HEADER] with its further (read-only) requests.
     */
    @PostMapping("/{id}/view-as")
    fun viewAs(
        @PathVariable id: String,
    ): ResponseEntity<UserSummaryDto> {
        val user = getUser.execute(UserId.fromString(id))
        auditLogger.log("USER_VIEW_AS_STARTED", details = mapOf("userId" to id, "email" to user.email.value))
        return ResponseEntity.ok(UserSummaryDto.fromDomain(user))
    }

    @PatchMapping("/{id}/active")
    fun setActive(
        @AuthenticationPrincipal adminIdString: String,
        @PathVariable id: String,
        @RequestBody request: SetUserActiveRequest,
    ): ResponseEntity<UserSummaryDto> {
        val user = setUserActive.execute(UserId.fromString(id), request.active, UserId.fromString(adminIdString))
        auditLogger.log(if (request.active) "USER_ACTIVATED" else "USER_DEACTIVATED", details = mapOf("userId" to id))
        return ResponseEntity.ok(UserSummaryDto.fromDomain(user))
    }

    @PatchMapping("/{id}/password")
    fun resetPassword(
        @PathVariable id: String,
        @Valid @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<Map<String, String>> {
        resetUserPassword.execute(UserId.fromString(id), request.newPassword)
        auditLogger.log("USER_PASSWORD_RESET", details = mapOf("userId" to id))
        return ResponseEntity.ok(mapOf("message" to "Password reset"))
    }
}
