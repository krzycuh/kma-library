package pl.kmazurek.library.domain.model.catalog

/**
 * Value Object: book title (trimmed, non-blank, max 500 chars)
 */
@JvmInline
value class BookTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "Title cannot be blank" }
        require(value == value.trim()) { "Title cannot have leading or trailing whitespace" }
        require(value.length <= MAX_LENGTH) { "Title cannot exceed $MAX_LENGTH characters" }
    }

    override fun toString() = value

    companion object {
        const val MAX_LENGTH = 500

        fun of(raw: String) = BookTitle(raw.trim())
    }
}
