package pl.kmazurek.library.infrastructure.api.rest.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.application.dto.AddShelfItemRequest
import pl.kmazurek.library.application.dto.InviteLibraryMemberRequest
import pl.kmazurek.library.application.dto.LibraryDto
import pl.kmazurek.library.application.dto.LibraryMemberDto
import pl.kmazurek.library.application.dto.PagedResponse
import pl.kmazurek.library.application.dto.RenameLibraryRequest
import pl.kmazurek.library.application.dto.ShelfItemDto
import pl.kmazurek.library.application.usecase.shelf.AddShelfItem
import pl.kmazurek.library.application.usecase.shelf.AddShelfItemCommand
import pl.kmazurek.library.application.usecase.shelf.InviteLibraryMember
import pl.kmazurek.library.application.usecase.shelf.ListActiveLoans
import pl.kmazurek.library.application.usecase.shelf.ListLibraryMembers
import pl.kmazurek.library.application.usecase.shelf.ListMyLibraries
import pl.kmazurek.library.application.usecase.shelf.ListShelfItems
import pl.kmazurek.library.application.usecase.shelf.ListShelfItemsQuery
import pl.kmazurek.library.application.usecase.shelf.RemoveLibraryMember
import pl.kmazurek.library.application.usecase.shelf.RenameLibrary
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.ShelfItemStatus
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.infrastructure.logging.AuditLogger

/**
 * REST Controller for libraries (bookshelves) and their items
 */
@RestController
@RequestMapping("/api/libraries")
class LibraryController(
    private val listMyLibraries: ListMyLibraries,
    private val listShelfItems: ListShelfItems,
    private val addShelfItem: AddShelfItem,
    private val listActiveLoans: ListActiveLoans,
    private val listLibraryMembers: ListLibraryMembers,
    private val inviteLibraryMember: InviteLibraryMember,
    private val removeLibraryMember: RemoveLibraryMember,
    private val renameLibrary: RenameLibrary,
    private val auditLogger: AuditLogger,
) {
    @GetMapping
    fun myLibraries(authentication: Authentication): ResponseEntity<List<LibraryDto>> =
        ResponseEntity.ok(listMyLibraries.summaries(authentication.userId()))

    @PatchMapping("/{libraryId}")
    fun rename(
        authentication: Authentication,
        @PathVariable libraryId: String,
        @Valid @RequestBody request: RenameLibraryRequest,
    ): ResponseEntity<LibraryDto> {
        val dto = renameLibrary.execute(authentication.userId(), LibraryId.fromString(libraryId), request.name)
        auditLogger.log("LIBRARY_RENAMED", details = mapOf("libraryId" to libraryId))
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/{libraryId}/members")
    fun members(
        authentication: Authentication,
        @PathVariable libraryId: String,
    ): ResponseEntity<List<LibraryMemberDto>> =
        ResponseEntity.ok(listLibraryMembers.execute(authentication.userId(), LibraryId.fromString(libraryId)))

    @PostMapping("/{libraryId}/members")
    fun invite(
        authentication: Authentication,
        @PathVariable libraryId: String,
        @Valid @RequestBody request: InviteLibraryMemberRequest,
    ): ResponseEntity<LibraryMemberDto> {
        val member = inviteLibraryMember.execute(authentication.userId(), LibraryId.fromString(libraryId), request.email)
        auditLogger.log("LIBRARY_MEMBER_ADDED", details = mapOf("libraryId" to libraryId, "userId" to member.userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(member)
    }

    /**
     * Owner removes a member, or a member leaves (userId = self).
     */
    @DeleteMapping("/{libraryId}/members/{userId}")
    fun removeMember(
        authentication: Authentication,
        @PathVariable libraryId: String,
        @PathVariable userId: String,
    ): ResponseEntity<Void> {
        removeLibraryMember.execute(authentication.userId(), LibraryId.fromString(libraryId), UserId.fromString(userId))
        auditLogger.log("LIBRARY_MEMBER_REMOVED", details = mapOf("libraryId" to libraryId, "userId" to userId))
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{libraryId}/items")
    fun items(
        authentication: Authentication,
        @PathVariable libraryId: String,
        @RequestParam(required = false) status: ShelfItemStatus?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) bookId: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") pageSize: Int,
    ): ResponseEntity<PagedResponse<ShelfItemDto>> {
        val query =
            ListShelfItemsQuery(
                userId = authentication.userId(),
                libraryId = LibraryId.fromString(libraryId),
                status = status,
                searchTerm = search,
                bookId = bookId?.let { BookId.fromString(it) },
                page = page,
                pageSize = pageSize,
            )
        return ResponseEntity.ok(listShelfItems.execute(query))
    }

    @PostMapping("/{libraryId}/items")
    fun addItem(
        authentication: Authentication,
        @PathVariable libraryId: String,
        @Valid @RequestBody request: AddShelfItemRequest,
    ): ResponseEntity<ShelfItemDto> {
        val dto =
            addShelfItem.execute(
                AddShelfItemCommand(
                    userId = authentication.userId(),
                    libraryId = LibraryId.fromString(libraryId),
                    book = request.book,
                    acquiredOn = request.acquiredOn,
                    notes = request.notes,
                ),
            )
        auditLogger.log("SHELF_ITEM_ADDED", details = mapOf("itemId" to dto.id, "libraryId" to libraryId))
        return ResponseEntity.status(HttpStatus.CREATED).body(dto)
    }

    @GetMapping("/{libraryId}/loans")
    fun activeLoans(
        authentication: Authentication,
        @PathVariable libraryId: String,
    ): ResponseEntity<List<ShelfItemDto>> =
        ResponseEntity.ok(listActiveLoans.execute(authentication.userId(), LibraryId.fromString(libraryId)))
}
