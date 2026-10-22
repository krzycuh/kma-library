package pl.kmazurek.library.domain.model.user

/**
 * Value Object representing user roles in the system
 */
enum class UserRole {
    USER,
    ADMIN,
    ;

    fun hasAdminPrivileges() = this == ADMIN
}
