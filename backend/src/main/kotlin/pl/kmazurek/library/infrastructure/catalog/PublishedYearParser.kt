package pl.kmazurek.library.infrastructure.catalog

/**
 * Extracts a year from free-form publication dates ("1961", "March 2002", "2002-03-01", "c1999").
 */
internal object PublishedYearParser {
    private val YEAR = Regex("""(?<!\d)(\d{4})(?!\d)""")

    fun parse(raw: String?): Int? = raw?.let { YEAR.find(it) }?.groupValues?.get(1)?.toInt()
}
