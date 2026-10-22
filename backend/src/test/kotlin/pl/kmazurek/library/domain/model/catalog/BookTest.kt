package pl.kmazurek.library.domain.model.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.domain.model.user.UserId

class BookTest {
    private val creator = UserId.generate()

    @Test
    fun `should create manual book with authors parsed from text`() {
        val book =
            Book.create(
                title = BookTitle.of("  Solaris "),
                authors = AuthorName.parseList("Stanisław Lem"),
                createdByUserId = creator,
            )

        assertEquals("Solaris", book.title.value)
        assertEquals(listOf("Stanisław Lem"), book.authors.map { it.value })
        assertEquals(BookSource.MANUAL, book.source)
    }

    @Test
    fun `should parse multiple authors separated by semicolon or comma`() {
        assertEquals(listOf("Lem", "Tokarczuk"), AuthorName.parseList("Lem; Tokarczuk").map { it.value })
        assertEquals(listOf("Lem", "Tokarczuk"), AuthorName.parseList("Lem , Tokarczuk").map { it.value })
        assertTrue(AuthorName.parseList(" ; ").isEmpty())
    }

    @Test
    fun `should require at least one author`() {
        assertThrows<IllegalArgumentException> {
            Book.create(title = BookTitle.of("X"), authors = emptyList(), createdByUserId = creator)
        }
    }

    @Test
    fun `should reject blank title`() {
        assertThrows<IllegalArgumentException> { BookTitle.of("   ") }
    }

    @Test
    fun `creator and admin can edit, others cannot`() {
        val book = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Lem"), createdByUserId = creator)
        val other = UserId.generate()

        assertTrue(book.canBeEditedBy(creator, isAdmin = false))
        assertTrue(book.canBeEditedBy(other, isAdmin = true))
        assertFalse(book.canBeEditedBy(other, isAdmin = false))
    }
}
