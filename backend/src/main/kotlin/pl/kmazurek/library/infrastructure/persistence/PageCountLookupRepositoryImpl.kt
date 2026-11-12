package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.PageCountLookup
import pl.kmazurek.library.domain.repository.PageCountLookupRepository
import pl.kmazurek.library.infrastructure.persistence.entity.PageCountLookupJpaEntity
import pl.kmazurek.library.infrastructure.persistence.mapper.BookMapper
import java.util.UUID

@Repository
interface JpaPageCountLookupRepository : JpaRepository<PageCountLookupJpaEntity, UUID>

@Component
class PageCountLookupRepositoryImpl(
    private val jpaRepository: JpaPageCountLookupRepository,
    private val jpaBookRepository: JpaBookRepository,
) : PageCountLookupRepository {
    override fun findBooksPendingLookup(limit: Int): List<Book> =
        jpaBookRepository.findPendingPageCountLookup(PageRequest.of(0, limit)).map { BookMapper.toDomain(it) }

    override fun countBooksPendingLookup(): Long = jpaBookRepository.countPendingPageCountLookup()

    override fun save(lookup: PageCountLookup) {
        jpaRepository.save(
            PageCountLookupJpaEntity(
                bookId = lookup.bookId.value,
                attemptedAt = lookup.attemptedAt,
                outcome = lookup.outcome.name,
                queryKind = lookup.queryKind?.name,
                provider = lookup.provider,
                pageCount = lookup.pageCount,
            ),
        )
    }
}
