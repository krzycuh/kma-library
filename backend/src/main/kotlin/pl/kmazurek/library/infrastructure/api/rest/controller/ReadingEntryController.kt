package pl.kmazurek.library.infrastructure.api.rest.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.application.dto.CreateReadingEntryRequest
import pl.kmazurek.library.application.dto.PagedResponse
import pl.kmazurek.library.application.dto.ReadingEntryDto
import pl.kmazurek.library.application.dto.ReadingSummaryDto
import pl.kmazurek.library.application.dto.UpdateReadingEntryRequest
import pl.kmazurek.library.application.usecase.reading.AddReadingEntry
import pl.kmazurek.library.application.usecase.reading.AddReadingEntryCommand
import pl.kmazurek.library.application.usecase.reading.DeleteReadingEntry
import pl.kmazurek.library.application.usecase.reading.GetReadingEntry
import pl.kmazurek.library.application.usecase.reading.GetReadingSummary
import pl.kmazurek.library.application.usecase.reading.ListReadingEntries
import pl.kmazurek.library.application.usecase.reading.ListReadingEntriesQuery
import pl.kmazurek.library.application.usecase.reading.UpdateReadingEntry
import pl.kmazurek.library.application.usecase.reading.UpdateReadingEntryCommand
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.reading.ReadingEntryId
import pl.kmazurek.library.infrastructure.logging.AuditLogger

/**
 * REST Controller for the personal reading log
 */
@RestController
@RequestMapping("/api/reading-entries")
class ReadingEntryController(
    private val addReadingEntry: AddReadingEntry,
    private val updateReadingEntry: UpdateReadingEntry,
    private val deleteReadingEntry: DeleteReadingEntry,
    private val getReadingEntry: GetReadingEntry,
    private val listReadingEntries: ListReadingEntries,
    private val getReadingSummary: GetReadingSummary,
    private val auditLogger: AuditLogger,
) {
    @GetMapping
    fun list(
        authentication: Authentication,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) rating: Int?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) bookId: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") pageSize: Int,
    ): ResponseEntity<PagedResponse<ReadingEntryDto>> {
        val query =
            ListReadingEntriesQuery(
                readerId = authentication.userId(),
                year = year,
                rating = rating,
                searchTerm = search,
                bookId = bookId?.let { BookId.fromString(it) },
                page = page,
                pageSize = pageSize,
            )
        return ResponseEntity.ok(listReadingEntries.execute(query))
    }

    @GetMapping("/summary")
    fun summary(
        authentication: Authentication,
        @RequestParam(required = false) year: Int?,
    ): ResponseEntity<ReadingSummaryDto> = ResponseEntity.ok(getReadingSummary.execute(authentication.userId(), year))

    @GetMapping("/{id}")
    fun get(
        authentication: Authentication,
        @PathVariable id: String,
    ): ResponseEntity<ReadingEntryDto> = ResponseEntity.ok(getReadingEntry.execute(ReadingEntryId.fromString(id), authentication.userId()))

    @PostMapping
    fun create(
        authentication: Authentication,
        @Valid @RequestBody request: CreateReadingEntryRequest,
    ): ResponseEntity<ReadingEntryDto> {
        val (entry, book) =
            addReadingEntry.execute(
                AddReadingEntryCommand(
                    readerId = authentication.userId(),
                    book = request.book,
                    finishedYear = request.finishedYear,
                    finishedMonth = request.finishedMonth,
                    rating = request.rating,
                    review = request.review,
                ),
            )
        auditLogger.log("READING_ENTRY_CREATED", details = mapOf("entryId" to entry.id, "bookId" to book.id))
        return ResponseEntity.status(HttpStatus.CREATED).body(ReadingEntryDto.fromDomain(entry, book))
    }

    @PutMapping("/{id}")
    fun update(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateReadingEntryRequest,
    ): ResponseEntity<ReadingEntryDto> {
        val (entry, book) =
            updateReadingEntry.execute(
                UpdateReadingEntryCommand(
                    entryId = ReadingEntryId.fromString(id),
                    readerId = authentication.userId(),
                    finishedYear = request.finishedYear,
                    finishedMonth = request.finishedMonth,
                    rating = request.rating,
                    review = request.review,
                ),
            )
        auditLogger.log("READING_ENTRY_UPDATED", details = mapOf("entryId" to id))
        return ResponseEntity.ok(ReadingEntryDto.fromDomain(entry, book))
    }

    @DeleteMapping("/{id}")
    fun delete(
        authentication: Authentication,
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        deleteReadingEntry.execute(ReadingEntryId.fromString(id), authentication.userId())
        auditLogger.log("READING_ENTRY_DELETED", details = mapOf("entryId" to id))
        return ResponseEntity.noContent().build()
    }
}
