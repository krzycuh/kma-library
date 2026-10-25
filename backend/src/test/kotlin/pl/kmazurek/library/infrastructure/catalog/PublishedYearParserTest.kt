package pl.kmazurek.library.infrastructure.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PublishedYearParserTest {
    @Test
    fun `should extract the year from common publication date formats`() {
        assertEquals(1961, PublishedYearParser.parse("1961"))
        assertEquals(1988, PublishedYearParser.parse("October 1, 1988"))
        assertEquals(2002, PublishedYearParser.parse("2002-03-01"))
        assertEquals(1999, PublishedYearParser.parse("c1999"))
    }

    @Test
    fun `should return null when there is no year`() {
        assertNull(PublishedYearParser.parse(null))
        assertNull(PublishedYearParser.parse("n.d."))
        assertNull(PublishedYearParser.parse("123456"))
    }
}
