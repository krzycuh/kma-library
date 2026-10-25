package pl.kmazurek.library.infrastructure.api.rest.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.config.AppVersion

/**
 * Public health check; also tells the frontend which backend build is running.
 */
@RestController
@RequestMapping("/api/health")
class HealthController(
    private val appVersion: AppVersion,
) {
    @GetMapping
    fun health(): ResponseEntity<Map<String, String?>> =
        ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "message" to "KMA Library API is running",
                "version" to appVersion.gitSha,
                "releaseTag" to appVersion.releaseTag,
                "buildTime" to appVersion.buildTime?.toString(),
            ),
        )
}
