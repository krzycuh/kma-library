package pl.kmazurek.library.domain.model.catalog

import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.service.BookDeduplicationKey
import java.time.LocalDateTime

/**
 * Book Aggregate Root (Catalog context)
 * A shared catalog entry: knows nothing about who read or owns it.
 */
data class Book(
    val id: BookId,
    val title: BookTitle,
    val authors: List<AuthorName>,
    val isbn: Isbn? = null,
    val publishedYear: Int? = null,
    val coverUrl: String? = null,
    val source: BookSource = BookSource.MANUAL,
    val externalId: String? = null,
    val createdByUserId: UserId?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        require(authors.isNotEmpty()) { "Book must have at least one author" }
        publishedYear?.let { require(it in MIN_YEAR..MAX_YEAR) { "Published year out of range: $it" } }
    }

    val normalizedKey: String
        get() = BookDeduplicationKey.of(title, authors)

    fun update(
        title: BookTitle,
        authors: List<AuthorName>,
        isbn: Isbn?,
        publishedYear: Int?,
        coverUrl: String?,
    ): Book =
        copy(
            title = title,
            authors = authors,
            isbn = isbn,
            publishedYear = publishedYear,
            coverUrl = coverUrl,
            updatedAt = LocalDateTime.now(),
        )

    /**
     * Fills in data this entry is missing from another description of the same book. Never overwrites.
     * The external id travels together with its source.
     */
    fun fillGapsFrom(
        isbn: Isbn?,
        publishedYear: Int?,
        coverUrl: String?,
        source: BookSource,
        externalId: String?,
    ): Book {
        val takeExternal = this.externalId == null && externalId != null
        val filled =
            copy(
                isbn = this.isbn ?: isbn,
                publishedYear = this.publishedYear ?: publishedYear,
                coverUrl = this.coverUrl ?: coverUrl,
                source = if (takeExternal) source else this.source,
                externalId = if (takeExternal) externalId else this.externalId,
            )
        return if (filled == this) this else filled.copy(updatedAt = LocalDateTime.now())
    }

    /**
     * Result of merging a duplicate catalog entry into this one: this entry survives,
     * keeping its own data and taking from the duplicate only what it lacks.
     */
    fun absorb(duplicate: Book): Book {
        require(duplicate.id != id) { "Cannot merge a book into itself" }
        return fillGapsFrom(duplicate.isbn, duplicate.publishedYear, duplicate.coverUrl, duplicate.source, duplicate.externalId)
    }

    fun canBeEditedBy(
        userId: UserId,
        isAdmin: Boolean,
    ): Boolean = isAdmin || createdByUserId == userId

    companion object {
        const val MIN_YEAR = 0
        const val MAX_YEAR = 2200

        fun create(
            title: BookTitle,
            authors: List<AuthorName>,
            isbn: Isbn? = null,
            publishedYear: Int? = null,
            coverUrl: String? = null,
            source: BookSource = BookSource.MANUAL,
            externalId: String? = null,
            createdByUserId: UserId?,
        ): Book {
            val now = LocalDateTime.now()
            return Book(
                id = BookId.generate(),
                title = title,
                authors = authors,
                isbn = isbn,
                publishedYear = publishedYear,
                coverUrl = coverUrl,
                source = source,
                externalId = externalId,
                createdByUserId = createdByUserId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
