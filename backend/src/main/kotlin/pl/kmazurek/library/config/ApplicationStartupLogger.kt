package pl.kmazurek.library.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class ApplicationStartupLogger(
    private val environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(ApplicationStartupLogger::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        val version =
            sequenceOf(
                environment.getProperty("app.version"),
                environment.getProperty("APP_VERSION"),
                environment.getProperty("VITE_APP_VERSION"),
                environment.getProperty("GIT_COMMIT"),
                environment.getProperty("COMMIT_SHA"),
                environment.getProperty("SOURCE_VERSION"),
            ).firstOrNull { !it.isNullOrBlank() }
                ?.trim()
                ?: "unknown"

        logger.info("KMA Library backend started (version: {})", version)
    }
}
