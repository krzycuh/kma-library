package pl.kmazurek.library.domain.service

import pl.kmazurek.library.domain.model.catalog.AuthorName
import pl.kmazurek.library.domain.model.catalog.BookTitle
import java.text.Normalizer

/**
 * Domain service: builds a normalized key (title + authors) used to detect duplicate catalog entries
 * when no ISBN is available. Lower-case, no diacritics, no punctuation, collapsed whitespace,
 * authors sorted so that order does not matter.
 */
object BookDeduplicationKey {
    fun of(
        title: BookTitle,
        authors: List<AuthorName>,
    ): String {
        val normalizedTitle = normalize(title.value)
        val normalizedAuthors = authors.map { normalize(it.value) }.sorted().joinToString(";")
        return "$normalizedTitle|$normalizedAuthors"
    }

    fun normalize(text: String): String {
        val withoutDiacritics =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .replace('ł', 'l')
                .replace('Ł', 'L')
        return withoutDiacritics
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
