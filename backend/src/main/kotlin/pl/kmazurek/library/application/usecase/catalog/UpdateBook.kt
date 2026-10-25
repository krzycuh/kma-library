package pl.kmazurek.library.application.usecase.catalog

import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.catalog.CoverUrl
import pl.kmazurek.library.domain.model.catalog.Isbn
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.BookRepository

data class UpdateBookCommand(
    val bookId: BookId,
    val title: String,
    val authors: String,
    val isbn: String?,
    val publishedYear: Int?,
    val coverUrl: String?,
    val requestedBy: UserId,
    val isAdmin: Boolean,
)

/**
 * Use Case: edit a catalog entry (creator or admin only)
 */
@Service
class UpdateBook(
    private val bookRepository: BookRepository,
) {
    fun execute(command: UpdateBookCommand): Book {
        val book = bookRepository.findById(command.bookId) ?: throw BookNotFoundException("Book not found")
        if (!book.canBeEditedBy(command.requestedBy, command.isAdmin)) {
            throw BookEditForbiddenException("Only the creator or an admin can edit this book")
        }
        val isbn = command.isbn?.takeIf { it.isNotBlank() }?.let { Isbn.parse(it) }
        if (isbn != null) {
            bookRepository.findByIsbn(isbn)?.let { other ->
                require(other.id == book.id) { "Another book already has ISBN ${isbn.value}" }
            }
        }
        val authors = AuthorName.parseList(command.authors)
        require(authors.isNotEmpty()) { "At least one author is required" }
        return bookRepository.save(
            book.update(
                title = BookTitle.of(command.title),
                authors = authors,
                isbn = isbn,
                publishedYear = command.publishedYear,
                coverUrl = CoverUrl.parseOrNull(command.coverUrl),
            ),
        )
    }
}
