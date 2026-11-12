package pl.kmazurek.library.application.usecase.catalog

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.kmazurek.library.application.service.BookCandidate
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.service.BookMetadataUnavailableException
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.PageCountLookup
import pl.kmazurek.library.domain.model.catalog.PageCountLookupOutcome
import pl.kmazurek.library.domain.model.catalog.PageCountQueryKind
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.PageCountLookupRepository
import pl.kmazurek.library.domain.service.BookDeduplicationKey
import java.time.Duration

data class PageCountBackfillSummary(
    val pendingBefore: Long,
    val checked: Int,
    val foundByIsbn: Int,
    val foundByTitle: Int,
    val notFound: Int,
    val unavailable: Int,
    val duration: Duration,
) {
    val found: Int get() = foundByIsbn + foundByTitle
}

/**
 * Use Case (maintenance): fill in the page count of catalog entries that have none, from the external catalogs.
 * By ISBN when the book has one, otherwise by title and author (an approximate value: the catalog is shared,
 * so one number per title, whatever the edition). Every book is tried once; only books the catalogs could not
 * be asked about are tried again. Progress and the outcome go to the application log.
 */
@Service
class BackfillPageCounts(
    private val bookRepository: BookRepository,
    private val lookupRepository: PageCountLookupRepository,
    private val metadataProvider: BookMetadataProvider,
) {
    private val logger = LoggerFactory.getLogger(BackfillPageCounts::class.java)

    /**
     * @param batchSize at most this many books in one run (the rest at the next start)
     * @param pause breather between two books, so the catalogs are not hammered
     */
    fun execute(
        batchSize: Int,
        pause: Duration,
    ): PageCountBackfillSummary {
        val started = System.nanoTime()
        val pending = lookupRepository.countBooksPendingLookup()
        val books = lookupRepository.findBooksPendingLookup(batchSize)
        logger.info("Page count backfill: {} books without a page count, trying {} now", pending, books.size)

        val counts = mutableMapOf<String, Int>().withDefault { 0 }
        books.forEachIndexed { index, book ->
            if (index > 0 && !pause.isZero) Thread.sleep(pause.toMillis())
            val lookup = lookUp(book)
            lookupRepository.save(lookup)
            when (lookup.outcome) {
                PageCountLookupOutcome.FOUND -> {
                    bookRepository.save(book.withPageCount(lookup.pageCount!!))
                    counts.merge(lookup.queryKind!!.name, 1, Int::plus)
                    logger.info(
                        "Page count backfill: \"{}\" ({}) -> {} pages from {} by {}",
                        book.title,
                        book.authorsLine(),
                        lookup.pageCount,
                        lookup.provider,
                        if (lookup.queryKind == PageCountQueryKind.ISBN) "ISBN ${book.isbn?.value}" else "title/author",
                    )
                }
                PageCountLookupOutcome.NOT_FOUND -> {
                    counts.merge("NOT_FOUND", 1, Int::plus)
                    logger.info(
                        "Page count backfill: \"{}\" ({}) -> not found{}",
                        book.title,
                        book.authorsLine(),
                        if (book.isbn != null) " (ISBN ${book.isbn.value}, then title/author)" else " (title/author)",
                    )
                }
                PageCountLookupOutcome.UNAVAILABLE -> {
                    counts.merge("UNAVAILABLE", 1, Int::plus)
                    logger.warn(
                        "Page count backfill: \"{}\" ({}) -> catalogs unavailable, will retry at the next start",
                        book.title,
                        book.authorsLine(),
                    )
                }
            }
        }

        val summary =
            PageCountBackfillSummary(
                pendingBefore = pending,
                checked = books.size,
                foundByIsbn = counts.getValue(PageCountQueryKind.ISBN.name),
                foundByTitle = counts.getValue(PageCountQueryKind.TITLE_AUTHOR.name),
                notFound = counts.getValue("NOT_FOUND"),
                unavailable = counts.getValue("UNAVAILABLE"),
                duration = Duration.ofNanos(System.nanoTime() - started),
            )
        logger.info(
            "Page count backfill finished: {} books checked, {} found ({} by ISBN, {} by title/author), {} not found, " +
                "{} unavailable (retried at the next start), {} still pending, took {}s",
            summary.checked,
            summary.found,
            summary.foundByIsbn,
            summary.foundByTitle,
            summary.notFound,
            summary.unavailable,
            pending - summary.checked + summary.unavailable,
            summary.duration.seconds,
        )
        return summary
    }

    private fun lookUp(book: Book): PageCountLookup {
        var unavailable = false
        book.isbn?.let { isbn ->
            try {
                val metadata = metadataProvider.lookupByIsbn(isbn)
                metadata?.pageCount?.takeIf { it in 1..Book.MAX_PAGES }?.let { pages ->
                    // The chain merges catalogs: the source names the one that gave the main record
                    return PageCountLookup.found(book.id, PageCountQueryKind.ISBN, metadata.source.name.lowercase(), pages)
                }
            } catch (ex: BookMetadataUnavailableException) {
                logger.debug("Page count backfill: ISBN lookup of \"{}\" unavailable: {}", book.title, ex.message)
                unavailable = true
            }
        }
        try {
            val candidates = metadataProvider.searchByTitleAndAuthor(book.title.value, book.authors.first().value)
            candidates.firstOrNull { it.pageCount != null && matches(book, it) }?.let { hit ->
                return PageCountLookup.found(book.id, PageCountQueryKind.TITLE_AUTHOR, hit.source.name.lowercase(), hit.pageCount!!)
            }
        } catch (ex: BookMetadataUnavailableException) {
            logger.debug("Page count backfill: search for \"{}\" unavailable: {}", book.title, ex.message)
            unavailable = true
        }
        return if (unavailable) PageCountLookup.unavailable(book.id) else PageCountLookup.notFound(book.id)
    }

    private fun Book.authorsLine() = authors.joinToString(", ") { it.value }

    companion object {
        /**
         * A search hit is the book when the titles agree (one may carry a ": subtitle" the other lacks) and,
         * if the hit names authors, one of them shares a name part with one of the book's authors
         * ("Stanisław Lem" and "Lem, Stanisław" share "lem"; initials are too short to count).
         */
        internal fun matches(
            book: Book,
            candidate: BookCandidate,
        ): Boolean {
            val wanted = BookDeduplicationKey.normalize(book.title.value)
            val got = BookDeduplicationKey.normalize(candidate.title)
            if (wanted.isEmpty() || got.isEmpty()) return false
            if (wanted != got && mainTitle(book.title.value) != mainTitle(candidate.title)) return false
            if (candidate.authors.isEmpty()) return true
            val wantedNames = book.authors.flatMap { nameParts(it.value) }.toSet()
            return candidate.authors.any { author -> nameParts(author).any { it in wantedNames } }
        }

        private fun mainTitle(title: String): String = BookDeduplicationKey.normalize(title.substringBefore(':'))

        private fun nameParts(name: String): List<String> =
            BookDeduplicationKey.normalize(name).split(' ').filter { it.length >= MIN_NAME_PART }

        private const val MIN_NAME_PART = 3
    }
}
