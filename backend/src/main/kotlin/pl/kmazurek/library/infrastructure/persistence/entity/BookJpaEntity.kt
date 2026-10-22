package pl.kmazurek.library.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "books")
class BookJpaEntity(
    @Id
    @Column(columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 500)
    var title: String,
    /** Authors joined with "; " */
    @Column(nullable = false, columnDefinition = "TEXT")
    var authors: String,
    @Column(name = "normalized_key", nullable = false, length = 600)
    var normalizedKey: String,
    @Column(name = "isbn13", length = 13)
    var isbn13: String? = null,
    @Column(name = "published_year")
    var publishedYear: Int? = null,
    @Column(name = "cover_url", length = 500)
    var coverUrl: String? = null,
    @Column(nullable = false, length = 30)
    var source: String = "MANUAL",
    @Column(name = "external_id", length = 100)
    var externalId: String? = null,
    @Column(name = "created_by_user_id", columnDefinition = "UUID")
    var createdByUserId: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
