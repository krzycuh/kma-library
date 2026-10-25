package pl.kmazurek.library.domain.model.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
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

    @Test
    fun `fillGapsFrom should fill only missing fields and keep the external id with its source`() {
        val book = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Lem"), publishedYear = 1961, createdByUserId = creator)

        val filled =
            book.fillGapsFrom(
                isbn = Isbn.parse("9788308055144"),
                publishedYear = 2008,
                coverUrl = "https://covers.example/solaris.jpg",
                source = BookSource.OPEN_LIBRARY,
                externalId = "OL1M",
            )

        assertEquals("9788308055144", filled.isbn?.value)
        assertEquals(1961, filled.publishedYear)
        assertEquals("https://covers.example/solaris.jpg", filled.coverUrl)
        assertEquals(BookSource.OPEN_LIBRARY, filled.source)
        assertEquals("OL1M", filled.externalId)
        assertEquals(book.title, filled.title)
    }

    @Test
    fun `fillGapsFrom should return the same instance when nothing is missing`() {
        val book =
            Book.create(
                BookTitle.of("Solaris"),
                AuthorName.parseList("Lem"),
                Isbn.parse("9788308055144"),
                1961,
                "https://covers.example/a.jpg",
                BookSource.GOOGLE_BOOKS,
                "g1",
                creator,
            )

        assertSame(
            book,
            book.fillGapsFrom(Isbn.parse("9780140328721"), 2000, "https://covers.example/b.jpg", BookSource.OPEN_LIBRARY, "OL1M"),
        )
    }

    @Test
    fun `absorb should take missing data from the duplicate and refuse merging into itself`() {
        val target = Book.create(BookTitle.of("Solaris"), AuthorName.parseList("Stanisław Lem"), createdByUserId = creator)
        val duplicate =
            Book.create(
                BookTitle.of("Solaris."),
                AuthorName.parseList("S. Lem"),
                Isbn.parse("9788308055144"),
                1961,
                createdByUserId = creator,
            )

        val merged = target.absorb(duplicate)

        assertEquals(target.id, merged.id)
        assertEquals("Solaris", merged.title.value)
        assertEquals("9788308055144", merged.isbn?.value)
        assertEquals(1961, merged.publishedYear)
        assertThrows<IllegalArgumentException> { target.absorb(target) }
    }
}
