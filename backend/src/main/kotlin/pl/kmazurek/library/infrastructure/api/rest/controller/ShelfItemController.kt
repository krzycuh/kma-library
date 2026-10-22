package pl.kmazurek.library.infrastructure.api.rest.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.application.dto.LendShelfItemRequest
import pl.kmazurek.library.application.dto.ReturnLoanRequest
import pl.kmazurek.library.application.dto.ShelfItemDto
import pl.kmazurek.library.application.dto.UpdateShelfItemRequest
import pl.kmazurek.library.application.usecase.shelf.GetShelfItem
import pl.kmazurek.library.application.usecase.shelf.LendShelfItem
import pl.kmazurek.library.application.usecase.shelf.LendShelfItemCommand
import pl.kmazurek.library.application.usecase.shelf.RemoveShelfItem
import pl.kmazurek.library.application.usecase.shelf.ReturnLoan
import pl.kmazurek.library.application.usecase.shelf.UpdateShelfItem
import pl.kmazurek.library.application.usecase.shelf.UpdateShelfItemCommand
import pl.kmazurek.library.domain.model.shelf.ShelfItemId
import pl.kmazurek.library.infrastructure.logging.AuditLogger

/**
 * REST Controller for single shelf items (copies) and their loans
 */
@RestController
@RequestMapping("/api/shelf-items")
class ShelfItemController(
    private val getShelfItem: GetShelfItem,
    private val updateShelfItem: UpdateShelfItem,
    private val removeShelfItem: RemoveShelfItem,
    private val lendShelfItem: LendShelfItem,
    private val returnLoan: ReturnLoan,
    private val auditLogger: AuditLogger,
) {
    @GetMapping("/{id}")
    fun get(
        authentication: Authentication,
        @PathVariable id: String,
    ): ResponseEntity<ShelfItemDto> = ResponseEntity.ok(getShelfItem.execute(authentication.userId(), ShelfItemId.fromString(id)))

    @PutMapping("/{id}")
    fun update(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateShelfItemRequest,
    ): ResponseEntity<ShelfItemDto> {
        val dto =
            updateShelfItem.execute(
                UpdateShelfItemCommand(
                    userId = authentication.userId(),
                    itemId = ShelfItemId.fromString(id),
                    acquiredOn = request.acquiredOn,
                    notes = request.notes,
                    status = request.status,
                ),
            )
        auditLogger.log("SHELF_ITEM_UPDATED", details = mapOf("itemId" to id))
        return ResponseEntity.ok(dto)
    }

    @DeleteMapping("/{id}")
    fun delete(
        authentication: Authentication,
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        removeShelfItem.execute(authentication.userId(), ShelfItemId.fromString(id))
        auditLogger.log("SHELF_ITEM_REMOVED", details = mapOf("itemId" to id))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/loans")
    fun lend(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: LendShelfItemRequest,
    ): ResponseEntity<ShelfItemDto> {
        val dto =
            lendShelfItem.execute(
                LendShelfItemCommand(
                    userId = authentication.userId(),
                    itemId = ShelfItemId.fromString(id),
                    borrowerFirstName = request.borrowerFirstName,
                    borrowerLastName = request.borrowerLastName,
                    lentOn = request.lentOn,
                    dueOn = request.dueOn,
                    notes = request.notes,
                ),
            )
        auditLogger.log("SHELF_ITEM_LENT", details = mapOf("itemId" to id))
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/{id}/return")
    fun returnItem(
        authentication: Authentication,
        @PathVariable id: String,
        @RequestBody(required = false) request: ReturnLoanRequest?,
    ): ResponseEntity<ShelfItemDto> {
        val dto = returnLoan.execute(authentication.userId(), ShelfItemId.fromString(id), request?.returnedOn)
        auditLogger.log("SHELF_ITEM_RETURNED", details = mapOf("itemId" to id))
        return ResponseEntity.ok(dto)
    }
}
