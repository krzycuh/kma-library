package pl.kmazurek.library.infrastructure.persistence.mapper

import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.reading.FinishedPeriod
import pl.kmazurek.library.domain.model.reading.Rating
import pl.kmazurek.library.domain.model.reading.ReadingEntry
import pl.kmazurek.library.domain.model.reading.ReadingEntryId
import pl.kmazurek.library.domain.model.reading.ReadingStatus
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.infrastructure.persistence.entity.ReadingEntryJpaEntity

object ReadingEntryMapper {
    fun toDomain(jpa: ReadingEntryJpaEntity): ReadingEntry =
        ReadingEntry(
            id = ReadingEntryId(jpa.id),
            readerId = UserId(jpa.readerId),
            bookId = BookId(jpa.bookId),
            status = ReadingStatus.valueOf(jpa.status),
            finishedOn = jpa.finishedYear?.let { FinishedPeriod(it.toInt(), jpa.finishedMonth?.toInt()) },
            rating = jpa.rating?.let { Rating(it.toInt()) },
            review = jpa.review,
            createdAt = jpa.createdAt,
            updatedAt = jpa.updatedAt,
        )

    fun toJpa(domain: ReadingEntry): ReadingEntryJpaEntity =
        ReadingEntryJpaEntity(
            id = domain.id.value,
            readerId = domain.readerId.value,
            bookId = domain.bookId.value,
            status = domain.status.name,
            finishedYear = domain.finishedOn?.year?.toShort(),
            finishedMonth = domain.finishedOn?.month?.toShort(),
            rating = domain.rating?.value?.toShort(),
            review = domain.review,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
}
