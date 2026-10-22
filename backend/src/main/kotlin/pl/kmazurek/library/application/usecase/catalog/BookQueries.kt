package pl.kmazurek.library.application.usecase.catalog

import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.repository.BookRepository

/**
 * Use Case: search the catalog (autocomplete)
 */
@Service
class SearchBooks(
    private val bookRepository: BookRepository,
) {
    fun execute(
        query: String?,
        limit: Int = 10,
    ): List<Book> {
        val q = query?.trim().orEmpty()
        if (q.length < 2) return emptyList()
        return bookRepository.search(q, limit.coerceIn(1, 50))
    }
}

/**
 * Use Case: get a single catalog entry
 */
@Service
class GetBook(
    private val bookRepository: BookRepository,
) {
    fun execute(id: BookId): Book = bookRepository.findById(id) ?: throw BookNotFoundException("Book $id not found")
}
