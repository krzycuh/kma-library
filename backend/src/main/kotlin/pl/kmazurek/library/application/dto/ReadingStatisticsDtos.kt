package pl.kmazurek.library.application.dto

import pl.kmazurek.library.domain.model.reading.Season

data class RatingCountDto(
    val rating: Int,
    val count: Int,
)

data class MonthCountDto(
    val month: Int,
    val count: Int,
)

data class SeasonCountDto(
    val season: Season,
    val count: Int,
)

data class AuthorCountDto(
    val author: String,
    val count: Int,
)

/**
 * Reading statistics of one period: a single calendar year, or every year together (`year` = null).
 * Lists of ratings, months and seasons are complete (zero counts included) so charts need no gap filling.
 */
data class ReadingPeriodStatsDto(
    val year: Int?,
    val books: Int,
    val uniqueBooks: Int,
    val ratedBooks: Int,
    val averageRating: Double?,
    val ratings: List<RatingCountDto>,
    val months: List<MonthCountDto>,
    val withoutMonth: Int,
    val seasons: List<SeasonCountDto>,
    val topAuthors: List<AuthorCountDto>,
)

data class ReadingStatisticsDto(
    val currentYear: Int,
    val total: ReadingPeriodStatsDto,
    /** One block per year with at least one entry, newest first */
    val years: List<ReadingPeriodStatsDto>,
    /** Entries whose finishing year is unknown: counted in `total`, in no year */
    val withoutYear: Int,
)
