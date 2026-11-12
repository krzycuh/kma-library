package pl.kmazurek.library.domain.model.reading

/**
 * Value Object: meteorological season of a month, in calendar order.
 * December belongs to the winter of its own year (statistics are grouped by calendar year).
 */
enum class Season(val months: Set<Int>) {
    SPRING(setOf(3, 4, 5)),
    SUMMER(setOf(6, 7, 8)),
    AUTUMN(setOf(9, 10, 11)),
    WINTER(setOf(12, 1, 2)),
    ;

    companion object {
        fun ofMonth(month: Int): Season {
            require(month in 1..12) { "Month must be between 1 and 12" }
            return entries.first { month in it.months }
        }
    }
}
