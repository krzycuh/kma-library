package pl.kmazurek.library.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.BookTitle

class BookDeduplicationKeyTest {
    @Test
    fun `should ignore case, diacritics and punctuation`() {
        val a = BookDeduplicationKey.of(BookTitle.of("Zbrodnia i kara!"), AuthorName.parseList("Fiodor Dostojewski"))
        val b = BookDeduplicationKey.of(BookTitle.of("zbrodnia  i kara"), AuthorName.parseList("fiodor dostojewski"))
        assertEquals(a, b)
    }

    @Test
    fun `should normalize polish letters`() {
        assertEquals("zolc gesla jazn", BookDeduplicationKey.normalize("Żółć gęślą jaźń"))
        assertEquals("lodz", BookDeduplicationKey.normalize("Łódź"))
    }

    @Test
    fun `author order should not matter`() {
        val a = BookDeduplicationKey.of(BookTitle.of("X"), AuthorName.parseList("A; B"))
        val b = BookDeduplicationKey.of(BookTitle.of("X"), AuthorName.parseList("B; A"))
        assertEquals(a, b)
    }

    @Test
    fun `different authors should give different keys`() {
        val a = BookDeduplicationKey.of(BookTitle.of("X"), AuthorName.parseList("A"))
        val b = BookDeduplicationKey.of(BookTitle.of("X"), AuthorName.parseList("B"))
        assertNotEquals(a, b)
    }
}
