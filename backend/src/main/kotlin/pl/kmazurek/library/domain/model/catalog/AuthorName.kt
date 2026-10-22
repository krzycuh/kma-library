package pl.kmazurek.library.domain.model.catalog

/**
 * Value Object: author name (trimmed, non-blank, max 255 chars)
 */
@JvmInline
value class AuthorName(val value: String) {
    init {
        require(value.isNotBlank()) { "Author name cannot be blank" }
        require(value == value.trim()) { "Author name cannot have leading or trailing whitespace" }
        require(value.length <= MAX_LENGTH) { "Author name cannot exceed $MAX_LENGTH characters" }
    }

    override fun toString() = value

    companion object {
        const val MAX_LENGTH = 255

        /**
         * Parses a free-text authors field ("Lem; Tokarczuk" or "Lem, Tokarczuk") into a list.
         */
        fun parseList(raw: String): List<AuthorName> =
            raw.split(';', ',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { AuthorName(it) }
    }
}
