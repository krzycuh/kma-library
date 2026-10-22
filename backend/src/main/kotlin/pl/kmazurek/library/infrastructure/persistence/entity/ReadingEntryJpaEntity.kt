package pl.kmazurek.library.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "reading_entries")
class ReadingEntryJpaEntity(
    @Id
    @Column(columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "reader_id", nullable = false, columnDefinition = "UUID")
    var readerId: UUID,
    @Column(name = "book_id", nullable = false, columnDefinition = "UUID")
    var bookId: UUID,
    @Column(nullable = false, length = 20)
    var status: String = "FINISHED",
    @Column(name = "finished_year")
    var finishedYear: Short? = null,
    @Column(name = "finished_month")
    var finishedMonth: Short? = null,
    @Column
    var rating: Short? = null,
    @Column(columnDefinition = "TEXT")
    var review: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
