package pl.kmazurek.library.application.usecase.common

/**
 * Base exceptions mapped to HTTP statuses in GlobalExceptionHandler
 */
open class NotFoundException(message: String) : RuntimeException(message)

open class ForbiddenException(message: String) : RuntimeException(message)

open class ConflictException(message: String) : RuntimeException(message)
