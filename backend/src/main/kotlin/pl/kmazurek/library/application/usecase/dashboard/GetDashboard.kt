package pl.kmazurek.library.application.usecase.dashboard

import org.springframework.stereotype.Service
import pl.kmazurek.library.application.dto.DashboardDto
import pl.kmazurek.library.application.usecase.reading.GetReadingSummary
import pl.kmazurek.library.application.usecase.reading.ListReadingEntries
import pl.kmazurek.library.application.usecase.reading.ListReadingEntriesQuery
import pl.kmazurek.library.application.usecase.shelf.ListActiveLoans
import pl.kmazurek.library.application.usecase.shelf.ListMyLibraries
import pl.kmazurek.library.domain.model.shelf.ShelfItemStatus
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.ShelfItemRepository
import java.time.Year

/**
 * Use Case: data for the home dashboard
 */
@Service
class GetDashboard(
    private val getReadingSummary: GetReadingSummary,
    private val listReadingEntries: ListReadingEntries,
    private val listMyLibraries: ListMyLibraries,
    private val listActiveLoans: ListActiveLoans,
    private val shelfItemRepository: ShelfItemRepository,
) {
    fun execute(userId: UserId): DashboardDto {
        val year = Year.now().value
        val summary = getReadingSummary.execute(userId, year)
        val recent = listReadingEntries.execute(ListReadingEntriesQuery(readerId = userId, page = 0, pageSize = 5)).items
        val library = listMyLibraries.execute(userId).firstOrNull()
        val loans = library?.let { listActiveLoans.execute(userId, it.id) } ?: emptyList()
        val onShelf = library?.let { shelfItemRepository.countByLibraryAndStatus(it.id, ShelfItemStatus.ON_SHELF) } ?: 0L

        return DashboardDto(
            year = year,
            booksReadThisYear = summary.booksThisYear,
            booksReadTotal = summary.booksTotal,
            averageRatingThisYear = summary.averageRating,
            lentCount = loans.size.toLong(),
            overdueCount = loans.count { it.activeLoan?.overdue == true },
            onShelfCount = onShelf,
            recentReadingEntries = recent,
            activeLoans = loans.take(5),
            defaultLibraryId = library?.id?.toString(),
        )
    }
}
