package pl.kmazurek.library.domain.model.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CoverUrlTest {
    @Test
    fun `should accept and trim https addresses`() {
        assertEquals(
            "https://www.storytel.com/images/cover.jpg",
            CoverUrl.parseOrNull("  https://www.storytel.com/images/cover.jpg "),
        )
    }

    @Test
    fun `should treat blank as no cover`() {
        assertNull(CoverUrl.parseOrNull(null))
        assertNull(CoverUrl.parseOrNull("   "))
    }

    @Test
    fun `should reject http, other schemes, spaces and overlong addresses`() {
        assertThrows<IllegalArgumentException> { CoverUrl.parseOrNull("http://example.com/c.jpg") }
        assertThrows<IllegalArgumentException> { CoverUrl.parseOrNull("javascript:alert(1)") }
        assertThrows<IllegalArgumentException> { CoverUrl.parseOrNull("https://") }
        assertThrows<IllegalArgumentException> { CoverUrl.parseOrNull("https://example.com/a b.jpg") }
        assertThrows<IllegalArgumentException> { CoverUrl.parseOrNull("https://example.com/" + "a".repeat(500)) }
    }
}
