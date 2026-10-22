package pl.kmazurek.library.domain.model.shelf

import java.util.UUID

@JvmInline
value class LibraryId(val value: UUID) {
    companion object {
        fun generate() = LibraryId(UUID.randomUUID())

        fun fromString(value: String) = LibraryId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}

@JvmInline
value class ShelfItemId(val value: UUID) {
    companion object {
        fun generate() = ShelfItemId(UUID.randomUUID())

        fun fromString(value: String) = ShelfItemId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}

@JvmInline
value class LoanId(val value: UUID) {
    companion object {
        fun generate() = LoanId(UUID.randomUUID())

        fun fromString(value: String) = LoanId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}
