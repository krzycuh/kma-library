package pl.kmazurek.library.domain.model.reading

/**
 * Edition the reader actually used: a paper book, an e-book or an audiobook.
 * Optional on an entry: older entries and readers who don't care leave it unset.
 */
enum class ReadingFormat {
    PAPER,
    EBOOK,
    AUDIOBOOK,
}
