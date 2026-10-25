package pl.kmazurek.library.application.usecase.catalog

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.library.domain.model.catalog.Book
import pl.kmazurek.library.domain.model.catalog.BookId
import pl.kmazurek.library.domain.repository.BookRepository
import pl.kmazurek.library.domain.repository.ReadingEntryRepository
import pl.kmazurek.library.domain.repository.ShelfItemRepository

data class MergeBooksResult(
    val target: Book,
    val movedReadingEntries: Int,
    val movedShelfItems: Int,
)

/**
 * Use Case (admin): merge a duplicate catalog entry into another one.
 * Reading entries and shelf items are moved to the target, the duplicate is deleted,
 * and the target takes over data it was missing (ISBN, year, cover).
 */
@Service
class MergeBooks(
    private val bookRepository: BookRepository,
    private val readingEntryRepository: ReadingEntryRepository,
    private val shelfItemRepository: ShelfItemRepository,
) {
    @Transactional
    fun execute(
        duplicateId: BookId,
        targetId: BookId,
    ): MergeBooksResult {
        require(duplicateId != targetId) { "Cannot merge a book into itself" }
        val duplicate = bookRepository.findById(duplicateId) ?: throw BookNotFoundException("Book $duplicateId not found")
        val target = bookRepository.findById(targetId) ?: throw BookNotFoundException("Book $targetId not found")

        val movedReadingEntries = readingEntryRepository.reassignBook(duplicate.id, target.id)
        val movedShelfItems = shelfItemRepository.reassignBook(duplicate.id, target.id)
        // Delete first: the target may take over the duplicate's (unique) ISBN
        bookRepository.deleteById(duplicate.id)

        val merged = target.absorb(duplicate)
        val saved = if (merged === target) target else bookRepository.save(merged)
        return MergeBooksResult(saved, movedReadingEntries, movedShelfItems)
    }
}
