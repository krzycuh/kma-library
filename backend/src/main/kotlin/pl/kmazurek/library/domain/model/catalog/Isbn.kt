package pl.kmazurek.library.domain.model.catalog

/**
 * Value Object: ISBN normalized to ISBN-13.
 * Accepts ISBN-10 or ISBN-13 with optional hyphens/spaces; validates the checksum.
 */
@JvmInline
value class Isbn private constructor(val value: String) {
    override fun toString() = value

    companion object {
        fun parse(raw: String): Isbn {
            val cleaned = raw.uppercase().replace("-", "").replace(" ", "")
            return when (cleaned.length) {
                10 -> {
                    require(isValidIsbn10(cleaned)) { "Invalid ISBN-10: $raw" }
                    Isbn(toIsbn13(cleaned))
                }
                13 -> {
                    require(isValidIsbn13(cleaned)) { "Invalid ISBN-13: $raw" }
                    Isbn(cleaned)
                }
                else -> throw IllegalArgumentException("ISBN must have 10 or 13 characters: $raw")
            }
        }

        fun parseOrNull(raw: String?): Isbn? {
            if (raw.isNullOrBlank()) return null
            return runCatching { parse(raw) }.getOrNull()
        }

        /**
         * Re-creates an Isbn from a value already stored in the database (assumed valid ISBN-13).
         */
        fun fromStored(value: String): Isbn = Isbn(value)

        fun isValidIsbn10(s: String): Boolean {
            if (s.length != 10) return false
            var sum = 0
            for (i in 0 until 9) {
                val c = s[i]
                if (!c.isDigit()) return false
                sum += (c - '0') * (10 - i)
            }
            val last = s[9]
            val checkValue =
                when {
                    last == 'X' -> 10
                    last.isDigit() -> last - '0'
                    else -> return false
                }
            return (sum + checkValue) % 11 == 0
        }

        fun isValidIsbn13(s: String): Boolean {
            if (s.length != 13 || !s.all { it.isDigit() }) return false
            val sum =
                s.take(12).withIndex().sumOf { (i, c) ->
                    (c - '0') * if (i % 2 == 0) 1 else 3
                }
            val check = (10 - sum % 10) % 10
            return check == s[12] - '0'
        }

        private fun toIsbn13(isbn10: String): String {
            val base = "978" + isbn10.take(9)
            val sum =
                base.withIndex().sumOf { (i, c) ->
                    (c - '0') * if (i % 2 == 0) 1 else 3
                }
            val check = (10 - sum % 10) % 10
            return base + check
        }
    }
}
