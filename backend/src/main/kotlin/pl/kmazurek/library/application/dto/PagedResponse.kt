package pl.kmazurek.library.application.dto

/**
 * Generic paginated response wrapper
 */
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T> of(
            items: List<T>,
            page: Int,
            pageSize: Int,
            totalItems: Long,
        ): PagedResponse<T> {
            val totalPages = (totalItems + pageSize - 1) / pageSize
            return PagedResponse(
                items = items,
                page = page,
                pageSize = pageSize,
                totalItems = totalItems,
                totalPages = totalPages.toInt(),
            )
        }
    }
}
