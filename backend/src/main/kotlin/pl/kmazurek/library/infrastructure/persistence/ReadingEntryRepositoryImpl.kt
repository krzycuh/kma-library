package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.model.reading.ReadingEntry
import pl.kmazurek.library.domain.model.reading.ReadingEntryId
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.ReadingEntryFilter
import pl.kmazurek.library.domain.repository.ReadingEntryRepository
import pl.kmazurek.library.infrastructure.persistence.mapper.ReadingEntryMapper

@Component
class ReadingEntryRepositoryImpl(
    private val jpaRepository: JpaReadingEntryRepository,
) : ReadingEntryRepository {
    override fun findById(id: ReadingEntryId): ReadingEntry? =
        jpaRepository.findById(id.value).map { ReadingEntryMapper.toDomain(it) }.orElse(null)

    override fun findByReader(
        readerId: UserId,
        filter: ReadingEntryFilter,
        page: Int,
        pageSize: Int,
    ): Pair<List<ReadingEntry>, Long> {
        val pattern = filter.searchTerm?.lowercase()?.let { "%$it%" }
        val result =
            jpaRepository.findByReader(
                readerId = readerId.value,
                year = filter.year?.toShort(),
                rating = filter.rating?.toShort(),
                bookId = filter.bookId?.value,
                pattern = pattern,
                pageable = PageRequest.of(page, pageSize),
            )
        return result.content.map { ReadingEntryMapper.toDomain(it) } to result.totalElements
    }

    override fun findByReaderAndYear(
        readerId: UserId,
        year: Int,
    ): List<ReadingEntry> =
        jpaRepository.findAllByReaderIdAndFinishedYear(readerId.value, year.toShort()).map { ReadingEntryMapper.toDomain(it) }

    override fun findDistinctYears(readerId: UserId): List<Int> = jpaRepository.findDistinctYears(readerId.value).map { it.toInt() }

    override fun countByReader(readerId: UserId): Long = jpaRepository.countByReaderId(readerId.value)

    override fun save(entry: ReadingEntry): ReadingEntry = ReadingEntryMapper.toDomain(jpaRepository.save(ReadingEntryMapper.toJpa(entry)))

    override fun deleteById(id: ReadingEntryId) = jpaRepository.deleteById(id.value)

    @Transactional
    override fun reassignBook(
        from: BookId,
        to: BookId,
    ): Int = jpaRepository.reassignBook(from.value, to.value)
}
