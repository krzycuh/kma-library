package pl.kmazurek.library.infrastructure.api.rest.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.application.usecase.auth.AccountDisabledException
import pl.kmazurek.library.application.usecase.auth.EmailAlreadyExistsException
import pl.kmazurek.library.application.usecase.auth.InvalidCredentialsException
import pl.kmazurek.library.application.usecase.auth.InvalidRefreshTokenException
import pl.kmazurek.library.application.usecase.auth.RegistrationDisabledException
import pl.kmazurek.library.application.usecase.catalog.IncompleteBookMetadataException
import pl.kmazurek.library.application.usecase.common.ConflictException
import pl.kmazurek.library.application.usecase.common.ForbiddenException
import pl.kmazurek.library.application.usecase.common.NotFoundException
import pl.kmazurek.library.application.usecase.user.CannotModifySelfException
import pl.kmazurek.library.application.usecase.user.InvalidPasswordException
import pl.kmazurek.library.application.usecase.user.UserNotFoundException
import pl.kmazurek.library.infrastructure.logging.AuditLogger

/**
 * Global exception handler for REST controllers
 */
@RestControllerAdvice
class GlobalExceptionHandler(
    private val auditLogger: AuditLogger,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExists(ex: EmailAlreadyExistsException): ResponseEntity<ErrorResponse> =
        buildErrorResponse(HttpStatus.CONFLICT, "Email already exists", ex)

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ErrorResponse> {
        auditLogger.log("USER_LOGIN_FAILED", null, mapOf("reason" to (ex.message ?: "invalid credentials")))
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex)
    }

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshToken(ex: InvalidRefreshTokenException): ResponseEntity<ErrorResponse> =
        buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid refresh token", ex)

    @ExceptionHandler(AccountDisabledException::class, RegistrationDisabledException::class, ForbiddenException::class)
    fun handleForbidden(ex: Exception): ResponseEntity<ErrorResponse> = buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", ex)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleSpringAccessDenied(ex: AccessDeniedException): ResponseEntity<ErrorResponse> =
        buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied", ex)

    @ExceptionHandler(UserNotFoundException::class, NotFoundException::class)
    fun handleNotFound(ex: Exception): ResponseEntity<ErrorResponse> = buildErrorResponse(HttpStatus.NOT_FOUND, "Not found", ex)

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ResponseEntity<ErrorResponse> = buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex)

    @ExceptionHandler(BookMetadataUnavailableException::class)
    fun handleCatalogUnavailable(ex: BookMetadataUnavailableException): ResponseEntity<ErrorResponse> {
        // An outage of a third-party service is not our error: no stack trace
        logger.warn("HTTP 503: {}", ex.message)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse("External book catalog is unavailable"))
    }

    @ExceptionHandler(InvalidPasswordException::class, CannotModifySelfException::class, IncompleteBookMetadataException::class)
    fun handleBadRequest(ex: Exception): ResponseEntity<ErrorResponse> = buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request", ex)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val errors =
            ex.bindingResult.fieldErrors.associate {
                it.field to (it.defaultMessage ?: "Invalid value")
            }
        logException(HttpStatus.BAD_REQUEST, ex)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ValidationErrorResponse("Validation failed", errors))
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        IllegalStateException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleIllegalArgument(ex: Exception): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request", ex)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> =
        buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex)

    private fun buildErrorResponse(
        status: HttpStatus,
        fallbackMessage: String,
        ex: Exception,
    ): ResponseEntity<ErrorResponse> {
        logException(status, ex)
        // Never expose internal messages for 5xx
        val clientMessage = if (status.is5xxServerError) fallbackMessage else ex.message ?: fallbackMessage
        return ResponseEntity.status(status).body(ErrorResponse(clientMessage))
    }

    private fun logException(
        status: HttpStatus,
        ex: Exception,
    ) {
        val message = ex.message ?: status.reasonPhrase
        val exceptionName = ex::class.simpleName ?: "UnknownException"
        if (status.is5xxServerError) {
            logger.error("HTTP {} ({}): {} - {}", status.value(), status.reasonPhrase, exceptionName, message, ex)
        } else {
            logger.warn("HTTP {} ({}): {} - {}", status.value(), status.reasonPhrase, exceptionName, message)
        }
    }
}

data class ErrorResponse(val message: String)

data class ValidationErrorResponse(val message: String, val errors: Map<String, String>)
