package pl.kmazurek.library.domain.repository

import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.PageCountLookup

/**
 * Repository interface (port) for the page-count backfill bookkeeping
 */
interface PageCountLookupRepository {
    /**
     * Books without a page count that were never tried, or whose last try found the catalogs unavailable;
     * oldest entries first.
     */
    fun findBooksPendingLookup(limit: Int): List<Book>

    fun countBooksPendingLookup(): Long

    /** Replaces an earlier record of the same book */
    fun save(lookup: PageCountLookup)
}
