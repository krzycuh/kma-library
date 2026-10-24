package pl.kmazurek.library.domain.repository

import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Repository interface (port) for the Book aggregate
 */
interface BookRepository {
    fun findById(id: BookId): Book?

    fun findByIds(ids: Collection<BookId>): Map<BookId, Book>

    fun findByIsbn(isbn: Isbn): Book?

    fun findByNormalizedKey(key: String): Book?

    fun search(
        query: String,
        limit: Int,
    ): List<Book>

    fun save(book: Book): Book

    fun deleteById(id: BookId)
}
