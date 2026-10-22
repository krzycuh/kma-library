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
