package pl.kmazurek.library.infrastructure.api.rest.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.application.dto.DashboardDto
import pl.kmazurek.library.application.usecase.dashboard.GetDashboard

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val getDashboard: GetDashboard,
) {
    @GetMapping
    fun dashboard(authentication: Authentication): ResponseEntity<DashboardDto> =
        ResponseEntity.ok(getDashboard.execute(authentication.userId()))
}
