package pl.kmazurek.library.domain.model.catalog

import java.time.LocalDateTime

enum class PageCountLookupOutcome {
    FOUND,
    NOT_FOUND,

    /** The catalogs could not be asked; the book is tried again at the next run */
    UNAVAILABLE,
}

enum class PageCountQueryKind {
    ISBN,
    TITLE_AUTHOR,
}

/**
 * Record of the page-count backfill having tried a catalog entry, so every book is looked up once.
 */
data class PageCountLookup(
    val bookId: BookId,
    val attemptedAt: LocalDateTime,
    val outcome: PageCountLookupOutcome,
    val queryKind: PageCountQueryKind?,
    val provider: String?,
    val pageCount: Int?,
) {
    companion object {
        fun found(
            bookId: BookId,
            queryKind: PageCountQueryKind,
            provider: String,
            pageCount: Int,
        ) = PageCountLookup(bookId, LocalDateTime.now(), PageCountLookupOutcome.FOUND, queryKind, provider, pageCount)

        fun notFound(bookId: BookId) = PageCountLookup(bookId, LocalDateTime.now(), PageCountLookupOutcome.NOT_FOUND, null, null, null)

        fun unavailable(bookId: BookId) = PageCountLookup(bookId, LocalDateTime.now(), PageCountLookupOutcome.UNAVAILABLE, null, null, null)
    }
}
