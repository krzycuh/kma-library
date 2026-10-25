package pl.kmazurek.library.domain.model.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IsbnTest {
    @Test
    fun `should accept valid ISBN-13 with hyphens`() {
        assertEquals("9788308055144", Isbn.parse("978-83-08-05514-4").value)
    }

    @Test
    fun `should accept valid ISBN-13 with spaces`() {
        assertEquals("9780306406157", Isbn.parse("978 0306 40615 7").value)
    }

    @Test
    fun `should convert ISBN-10 to ISBN-13`() {
        assertEquals("9780306406157", Isbn.parse("0-306-40615-2").value)
    }

    @Test
    fun `should accept ISBN-10 with X check digit`() {
        assertEquals("9780804429573", Isbn.parse("080442957X").value)
    }

    @Test
    fun `should reject wrong ISBN-13 checksum`() {
        assertThrows<IllegalArgumentException> { Isbn.parse("9780306406158") }
    }

    @Test
    fun `should reject wrong ISBN-10 checksum`() {
        assertThrows<IllegalArgumentException> { Isbn.parse("0306406153") }
    }

    @Test
    fun `should reject wrong length`() {
        assertThrows<IllegalArgumentException> { Isbn.parse("12345") }
    }

    @Test
    fun `parseOrNull should return null for blank and invalid input`() {
        assertNull(Isbn.parseOrNull(null))
        assertNull(Isbn.parseOrNull("  "))
        assertNull(Isbn.parseOrNull("not-an-isbn"))
        assertEquals("9788308055144", Isbn.parseOrNull("9788308055144")?.value)
    }

    @Test
    fun `should convert back to ISBN-10 when it exists`() {
        assertEquals("8871351460", Isbn.parse("9788871351469").toIsbn10())
        assertEquals("080442957X", Isbn.parse("080442957X").toIsbn10())
        assertEquals("0306406152", Isbn.parse("9780306406157").toIsbn10())
        assertNull(Isbn.parse("9791032305690").toIsbn10())
    }
}
