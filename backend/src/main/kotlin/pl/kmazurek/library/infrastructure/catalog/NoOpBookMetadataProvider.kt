package pl.kmazurek.library.infrastructure.catalog

import pl.kmazurek.library.application.service.BookMetadata
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.domain.model.catalog.Isbn

/**
 * Used when no external catalog is configured (app.catalog.providers=none): never finds anything.
 */
class NoOpBookMetadataProvider : BookMetadataProvider {
    override fun lookupByIsbn(isbn: Isbn): BookMetadata? = null
}
