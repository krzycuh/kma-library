package pl.kmazurek.library.domain.model.catalog

/**
 * Cover image address typed or pasted by a user (e.g. copied from a bookstore or Storytel page).
 * Only https: the app's CSP and every browser block plain-http images on an https page.
 */
object CoverUrl {
    const val MAX_LENGTH = 500

    /** Trimmed address, or null for a blank one; IllegalArgumentException (HTTP 400) for an unusable one. */
    fun parseOrNull(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        require(value.length <= MAX_LENGTH) { "Cover URL cannot exceed $MAX_LENGTH characters" }
        require(value.startsWith("https://", ignoreCase = true) && value.length > "https://".length && value.none { it.isWhitespace() }) {
            "Cover URL must be an https:// address"
        }
        return value
    }
}
