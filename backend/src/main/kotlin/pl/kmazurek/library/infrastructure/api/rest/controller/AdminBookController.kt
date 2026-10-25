package pl.kmazurek.library.infrastructure.api.rest.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.application.dto.MergeBooksResultDto
import pl.kmazurek.library.application.usecase.catalog.MergeBooks
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.infrastructure.logging.AuditLogger

/**
 * Admin-only catalog maintenance.
 */
@RestController
@RequestMapping("/api/admin/books")
@PreAuthorize("hasRole('ADMIN')")
class AdminBookController(
    private val mergeBooks: MergeBooks,
    private val auditLogger: AuditLogger,
) {
    /**
     * Merges duplicate book {id} into {targetId}: moves reading entries and shelf items, deletes {id}.
     */
    @PostMapping("/{id}/merge-into/{targetId}")
    fun mergeInto(
        @PathVariable id: String,
        @PathVariable targetId: String,
    ): ResponseEntity<MergeBooksResultDto> {
        val result = mergeBooks.execute(BookId.fromString(id), BookId.fromString(targetId))
        auditLogger.log(
            "BOOKS_MERGED",
            details =
                mapOf(
                    "duplicateId" to id,
                    "targetId" to targetId,
                    "movedReadingEntries" to result.movedReadingEntries,
                    "movedShelfItems" to result.movedShelfItems,
                ),
        )
        return ResponseEntity.ok(MergeBooksResultDto.fromResult(result))
    }
}
