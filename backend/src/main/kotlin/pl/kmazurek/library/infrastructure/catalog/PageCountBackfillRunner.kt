package pl.kmazurek.library.infrastructure.catalog

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.application.usecase.catalog.BackfillPageCounts
import java.time.Duration

/**
 * Runs the page-count backfill once the application is up, on a background thread so start-up is not delayed.
 * Each book is tried once (see BackfillPageCounts), so later starts only pick up new books and earlier failures.
 */
@Component
class PageCountBackfillRunner(
    private val backfillPageCounts: BackfillPageCounts,
    private val metadataProvider: BookMetadataProvider,
    @Value("\${app.catalog.page-count-backfill.enabled:true}") private val enabled: Boolean,
    @Value("\${app.catalog.page-count-backfill.pause:500ms}") private val pause: Duration,
    @Value("\${app.catalog.page-count-backfill.batch-size:500}") private val batchSize: Int,
) {
    private val logger = LoggerFactory.getLogger(PageCountBackfillRunner::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        if (!enabled) {
            logger.info("Page count backfill: disabled (app.catalog.page-count-backfill.enabled=false)")
            return
        }
        if (metadataProvider is NoOpBookMetadataProvider) {
            logger.info("Page count backfill: skipped, no external catalog configured (app.catalog.providers=none)")
            return
        }
        val thread =
            Thread({
                try {
                    backfillPageCounts.execute(batchSize, pause)
                } catch (ex: Exception) {
                    logger.error("Page count backfill failed: {}", ex.message, ex)
                }
            }, "page-count-backfill")
        thread.isDaemon = true
        thread.start()
    }
}
