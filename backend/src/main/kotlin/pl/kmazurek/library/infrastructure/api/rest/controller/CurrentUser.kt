package pl.kmazurek.library.infrastructure.api.rest.controller

import org.springframework.security.core.Authentication
import pl.kmazurek.library.domain.model.user.UserId

/**
 * Helpers to read the authenticated user from the security context
 */
internal fun Authentication.userId(): UserId = UserId.fromString(principal as String)

internal fun Authentication.isAdmin(): Boolean = authorities.any { it.authority == "ROLE_ADMIN" }
