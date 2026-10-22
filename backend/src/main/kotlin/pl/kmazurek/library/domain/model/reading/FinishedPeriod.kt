package pl.kmazurek.library.domain.model.reading

import java.time.YearMonth

/**
 * Value Object: when a book was finished, with year precision and optional month.
 * Day precision is intentionally not supported.
 */
data class FinishedPeriod(
    val year: Int,
    val month: Int? = null,
) : Comparable<FinishedPeriod> {
    init {
        require(year in MIN_YEAR..MAX_YEAR) { "Year must be between $MIN_YEAR and $MAX_YEAR" }
        month?.let { require(it in 1..12) { "Month must be between 1 and 12" } }
    }

    fun isAfter(other: YearMonth): Boolean {
        val m = month ?: 1
        return YearMonth.of(year, m).isAfter(other)
    }

    override fun compareTo(other: FinishedPeriod): Int = compareValuesBy(this, other, { it.year }, { it.month ?: 0 })

    companion object {
        const val MIN_YEAR = 1900
        const val MAX_YEAR = 2200
    }
}
