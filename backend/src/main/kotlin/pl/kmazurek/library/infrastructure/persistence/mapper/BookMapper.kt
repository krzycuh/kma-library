package pl.kmazurek.library.infrastructure.persistence.mapper

import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.catalog.BookSource
import pl.kmazurek.library.domain.model.catalog.BookTitle
import pl.kmazurek.library.domain.model.catalog.Isbn
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.infrastructure.persistence.entity.BookJpaEntity

object BookMapper {
    private const val AUTHOR_SEPARATOR = "; "

    fun toDomain(jpa: BookJpaEntity): Book =
        Book(
            id = BookId(jpa.id),
            title = BookTitle(jpa.title),
            authors = jpa.authors.split(";").map { it.trim() }.filter { it.isNotEmpty() }.map { AuthorName(it) },
            isbn = jpa.isbn13?.let { Isbn.fromStored(it) },
            publishedYear = jpa.publishedYear,
            coverUrl = jpa.coverUrl,
            source = BookSource.valueOf(jpa.source),
            externalId = jpa.externalId,
            createdByUserId = jpa.createdByUserId?.let { UserId(it) },
            createdAt = jpa.createdAt,
            updatedAt = jpa.updatedAt,
        )

    fun toJpa(domain: Book): BookJpaEntity =
        BookJpaEntity(
            id = domain.id.value,
            title = domain.title.value,
            authors = domain.authors.joinToString(AUTHOR_SEPARATOR) { it.value },
            normalizedKey = domain.normalizedKey,
            isbn13 = domain.isbn?.value,
            publishedYear = domain.publishedYear,
            coverUrl = domain.coverUrl,
            source = domain.source.name,
            externalId = domain.externalId,
            createdByUserId = domain.createdByUserId?.value,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
}
