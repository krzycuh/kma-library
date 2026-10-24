package pl.kmazurek.library.infrastructure.api.rest.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.library.application.dto.BookDto
import pl.kmazurek.library.application.dto.BookLookupDto
import pl.kmazurek.library.application.dto.CreateBookRequest
import pl.kmazurek.library.application.dto.ImportBookRequest
import pl.kmazurek.library.application.dto.UpdateBookRequest
import pl.kmazurek.library.application.usecase.catalog.FindOrCreateBook
import pl.kmazurek.library.application.usecase.catalog.FindOrCreateBookCommand
import pl.kmazurek.library.application.usecase.catalog.GetBook
import pl.kmazurek.library.application.usecase.catalog.ImportBookByIsbn
import pl.kmazurek.library.application.usecase.catalog.LookupBookByIsbn
import pl.kmazurek.library.application.usecase.catalog.SearchBooks
import pl.kmazurek.library.application.usecase.catalog.UpdateBook
import pl.kmazurek.library.application.usecase.catalog.UpdateBookCommand
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.infrastructure.logging.AuditLogger

/**
 * REST Controller for the shared book catalog
 */
@RestController
@RequestMapping("/api/books")
class BookController(
    private val searchBooks: SearchBooks,
    private val getBook: GetBook,
    private val findOrCreateBook: FindOrCreateBook,
    private val updateBook: UpdateBook,
    private val lookupBookByIsbn: LookupBookByIsbn,
    private val importBookByIsbn: ImportBookByIsbn,
    private val auditLogger: AuditLogger,
) {
    @GetMapping
    fun search(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): ResponseEntity<List<BookDto>> = ResponseEntity.ok(searchBooks.execute(search, limit).map { BookDto.fromDomain(it) })

    @GetMapping("/lookup")
    fun lookup(
        @RequestParam isbn: String,
    ): ResponseEntity<BookLookupDto> = ResponseEntity.ok(BookLookupDto.fromResult(lookupBookByIsbn.execute(isbn)))

    /**
     * Ensures the catalog has the book with this ISBN (created from external metadata if needed).
     */
    @PostMapping("/import")
    fun import(
        authentication: Authentication,
        @Valid @RequestBody request: ImportBookRequest,
    ): ResponseEntity<BookDto> {
        val book = importBookByIsbn.execute(request.isbn, authentication.userId())
        auditLogger.log("BOOK_IMPORTED", details = mapOf("bookId" to book.id, "isbn" to request.isbn))
        return ResponseEntity.ok(BookDto.fromDomain(book))
    }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: String,
    ): ResponseEntity<BookDto> = ResponseEntity.ok(BookDto.fromDomain(getBook.execute(BookId.fromString(id))))

    @PostMapping
    fun create(
        authentication: Authentication,
        @Valid @RequestBody request: CreateBookRequest,
    ): ResponseEntity<BookDto> {
        val book =
            findOrCreateBook.execute(
                FindOrCreateBookCommand(
                    title = request.title,
                    authors = request.authors,
                    isbn = request.isbn,
                    publishedYear = request.publishedYear,
                    createdBy = authentication.userId(),
                ),
            )
        auditLogger.log("BOOK_RESOLVED", details = mapOf("bookId" to book.id))
        return ResponseEntity.status(HttpStatus.CREATED).body(BookDto.fromDomain(book))
    }

    @PutMapping("/{id}")
    fun update(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateBookRequest,
    ): ResponseEntity<BookDto> {
        val book =
            updateBook.execute(
                UpdateBookCommand(
                    bookId = BookId.fromString(id),
                    title = request.title,
                    authors = request.authors,
                    isbn = request.isbn,
                    publishedYear = request.publishedYear,
                    coverUrl = request.coverUrl,
                    requestedBy = authentication.userId(),
                    isAdmin = authentication.isAdmin(),
                ),
            )
        auditLogger.log("BOOK_UPDATED", details = mapOf("bookId" to id))
        return ResponseEntity.ok(BookDto.fromDomain(book))
    }
}
