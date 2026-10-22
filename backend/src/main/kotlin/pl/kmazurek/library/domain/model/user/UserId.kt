package pl.kmazurek.library.domain.model.user

import java.util.UUID

/**
 * Value Object representing a User's unique identifier
 */
@JvmInline
value class UserId(val value: UUID) {
    companion object {
        fun generate() = UserId(UUID.randomUUID())

        fun fromString(value: String) = UserId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}
