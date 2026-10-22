package pl.kmazurek.library.infrastructure.logging

import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuditLogger {
    private val logger = LoggerFactory.getLogger("audit")

    fun log(
        event: String,
        userEmail: String? = null,
        details: Map<String, Any?> = emptyMap(),
    ) {
        val resolvedEmail = userEmail ?: resolveUserEmailFromContext() ?: "anonymous"

        val detailsStr =
            if (details.isNotEmpty()) {
                details.entries.joinToString(", ") { "${it.key}=${it.value}" }
            } else {
                ""
            }

        if (detailsStr.isNotEmpty()) {
            logger.info("AUDIT | event={} | user={} | {}", event, resolvedEmail, detailsStr)
        } else {
            logger.info("AUDIT | event={} | user={}", event, resolvedEmail)
        }
    }

    private fun resolveUserEmailFromContext(): String? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        if (!auth.isAuthenticated || auth.principal == "anonymousUser") return null
        return auth.credentials?.toString()
    }
}
