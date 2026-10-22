package pl.kmazurek.library.domain.model.reading

import java.util.UUID

@JvmInline
value class ReadingEntryId(val value: UUID) {
    companion object {
        fun generate() = ReadingEntryId(UUID.randomUUID())

        fun fromString(value: String) = ReadingEntryId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}
