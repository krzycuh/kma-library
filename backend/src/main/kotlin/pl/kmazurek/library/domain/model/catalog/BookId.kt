package pl.kmazurek.library.domain.model.catalog

import java.util.UUID

@JvmInline
value class BookId(val value: UUID) {
    companion object {
        fun generate() = BookId(UUID.randomUUID())

        fun fromString(value: String) = BookId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}
