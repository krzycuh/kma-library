package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.catalog.Isbn
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.infrastructure.persistence.mapper.BookMapper

@Component
class BookRepositoryImpl(
    private val jpaRepository: JpaBookRepository,
) : BookRepository {
    override fun findById(id: BookId): Book? = jpaRepository.findById(id.value).map { BookMapper.toDomain(it) }.orElse(null)

    override fun findByIds(ids: Collection<BookId>): Map<BookId, Book> {
        if (ids.isEmpty()) return emptyMap()
        return jpaRepository.findAllById(ids.map { it.value })
            .map { BookMapper.toDomain(it) }
            .associateBy { it.id }
    }

    override fun findByIsbn(isbn: Isbn): Book? = jpaRepository.findByIsbn13(isbn.value)?.let { BookMapper.toDomain(it) }

    override fun findByNormalizedKey(key: String): Book? = jpaRepository.findFirstByNormalizedKey(key)?.let { BookMapper.toDomain(it) }

    override fun search(
        query: String,
        limit: Int,
    ): List<Book> {
        val pattern = "%${query.lowercase()}%"
        val exactIsbn = Isbn.parseOrNull(query)?.value ?: ""
        return jpaRepository.search(pattern, exactIsbn, PageRequest.of(0, limit)).map { BookMapper.toDomain(it) }
    }

    override fun save(book: Book): Book = BookMapper.toDomain(jpaRepository.save(BookMapper.toJpa(book)))
}
