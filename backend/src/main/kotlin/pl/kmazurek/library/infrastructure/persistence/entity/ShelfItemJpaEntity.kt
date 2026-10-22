package pl.kmazurek.library.infrastructure.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "shelf_items")
class ShelfItemJpaEntity(
    @Id
    @Column(columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "library_id", nullable = false, columnDefinition = "UUID")
    var libraryId: UUID,
    @Column(name = "book_id", nullable = false, columnDefinition = "UUID")
    var bookId: UUID,
    @Column(nullable = false, length = 20)
    var status: String = "ON_SHELF",
    @Column(name = "acquired_on")
    var acquiredOn: LocalDate? = null,
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
    @OneToMany(mappedBy = "shelfItem", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var loans: MutableList<LoanJpaEntity> = mutableListOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "loans")
class LoanJpaEntity(
    @Id
    @Column(columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_item_id", nullable = false)
    var shelfItem: ShelfItemJpaEntity? = null,
    @Column(name = "borrower_first_name", nullable = false, length = 100)
    var borrowerFirstName: String,
    @Column(name = "borrower_last_name", nullable = false, length = 100)
    var borrowerLastName: String,
    @Column(name = "lent_on", nullable = false)
    var lentOn: LocalDate,
    @Column(name = "due_on")
    var dueOn: LocalDate? = null,
    @Column(name = "returned_on")
    var returnedOn: LocalDate? = null,
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
