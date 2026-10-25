package pl.kmazurek.library.infrastructure.catalog

import org.springframework.cache.CacheManager
import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Caches lookups by ISBN, including "not found" answers. Failures (exceptions) are not cached.
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

    companion object {
        const val CACHE_NAME = "book-metadata"
    }
}
