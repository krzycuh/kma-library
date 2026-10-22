package pl.kmazurek.library.application.dto

data class DashboardDto(
    val year: Int,
    val booksReadThisYear: Int,
    val booksReadTotal: Long,
    val averageRatingThisYear: Double?,
    val lentCount: Long,
    val overdueCount: Int,
    val onShelfCount: Long,
    val recentReadingEntries: List<ReadingEntryDto>,
    val activeLoans: List<ShelfItemDto>,
    val defaultLibraryId: String?,
)
