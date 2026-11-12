package pl.kmazurek.library.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "book_page_count_lookups")
class PageCountLookupJpaEntity(
    @Id
    @Column(name = "book_id", columnDefinition = "UUID")
    var bookId: UUID,
    @Column(name = "attempted_at", nullable = false)
    var attemptedAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false, length = 20)
    var outcome: String,
    @Column(name = "query_kind", length = 20)
    var queryKind: String? = null,
    @Column(length = 30)
    var provider: String? = null,
    @Column(name = "page_count")
    var pageCount: Int? = null,
)
