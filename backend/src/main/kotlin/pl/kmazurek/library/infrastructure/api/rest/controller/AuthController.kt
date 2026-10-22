package pl.kmazurek.library.infrastructure.api.rest.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.application.dto.AuthResponse
import pl.kmazurek.library.application.dto.LoginRequest
import pl.kmazurek.library.application.dto.RefreshTokenRequest
import pl.kmazurek.library.application.dto.RegisterRequest
import pl.kmazurek.library.application.dto.UserDto
import pl.kmazurek.library.application.service.NotificationService
import pl.kmazurek.library.application.usecase.auth.LoginUser
import pl.kmazurek.library.application.usecase.auth.RefreshAccessToken
import pl.kmazurek.library.application.usecase.auth.RegisterUser
import pl.kmazurek.library.infrastructure.logging.AuditLogger

/**
 * REST Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val registerUser: RegisterUser,
    private val loginUser: LoginUser,
    private val refreshAccessToken: RefreshAccessToken,
    private val auditLogger: AuditLogger,
    private val notificationService: NotificationService,
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): ResponseEntity<AuthResponse> {
        val user = registerUser.execute(request.email, request.password, request.name)

        auditLogger.log("USER_REGISTERED", request.email)
        notificationService.notifyUserRegistered(user.name, user.email.value)

        val loginResult = loginUser.execute(request.email, request.password)

        val response =
            AuthResponse(
                accessToken = loginResult.accessToken,
                refreshToken = loginResult.refreshToken,
                user = UserDto.fromDomain(user),
            )

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): ResponseEntity<AuthResponse> {
        val result = loginUser.execute(request.email, request.password)

        auditLogger.log("USER_LOGIN_SUCCESS", request.email)

        val response =
            AuthResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                user = UserDto.fromDomain(result.user),
            )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): ResponseEntity<Map<String, String>> {
        val newAccessToken = refreshAccessToken.execute(request.refreshToken)
        auditLogger.log("TOKEN_REFRESH", null)
        return ResponseEntity.ok(mapOf("accessToken" to newAccessToken))
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Map<String, String>> {
        auditLogger.log("USER_LOGOUT")
        // With stateless JWT, logout is handled client-side by discarding tokens
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}
