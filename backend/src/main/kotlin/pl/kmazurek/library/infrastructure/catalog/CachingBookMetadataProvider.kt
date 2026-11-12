package pl.kmazurek.library.infrastructure.catalog

import org.springframework.cache.CacheManager
import pl.kmazurek.library.application.service.BookCandidate
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Caches lookups by ISBN and title/author searches, including "not found" answers. Failures (exceptions) are not cached.
 */
class CachingBookMetadataProvider(
    private val delegate: BookMetadataProvider,
    cacheManager: CacheManager,
) : BookMetadataProvider {
    private val cache = requireNotNull(cacheManager.getCache(CACHE_NAME)) { "Cache $CACHE_NAME is not available" }

    override fun lookupByIsbn(isbn: Isbn): BookMetadata? {
        cache.get(isbn.value)?.let { return it.get() as BookMetadata? }
        val metadata = delegate.lookupByIsbn(isbn)
        cache.put(isbn.value, metadata)
        return metadata
    }

    override fun searchByTitleAndAuthor(
        title: String,
        author: String?,
    ): List<BookCandidate> {
        val key = "search:${title.lowercase()}|${author?.lowercase().orEmpty()}"
        cache.get(key)?.let {
            @Suppress("UNCHECKED_CAST")
            return it.get() as List<BookCandidate>
        }
        val candidates = delegate.searchByTitleAndAuthor(title, author)
        cache.put(key, candidates)
        return candidates
    }

    companion object {
        const val CACHE_NAME = "book-metadata"
    }
}
