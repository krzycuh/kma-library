package pl.kmazurek.library.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ApplicationStartupLogger(
    private val appVersion: AppVersion,
) {
    private val logger = LoggerFactory.getLogger(ApplicationStartupLogger::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info(
            "KMA Library backend started (commit: {}, release: {}, built: {})",
            appVersion.gitSha,
            appVersion.releaseTag ?: "-",
            appVersion.buildTime ?: "-",
        )
    }
}
