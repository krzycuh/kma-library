package pl.kmazurek.library.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.infrastructure.catalog.CachingBookMetadataProvider
import pl.kmazurek.library.infrastructure.catalog.GoogleBooksBookMetadataProvider
import pl.kmazurek.library.infrastructure.catalog.NoOpBookMetadataProvider
import pl.kmazurek.library.infrastructure.catalog.OpenLibraryBookMetadataProvider
import java.net.ProxySelector
import java.net.http.HttpClient
import java.time.Duration

/**
 * External book catalog: app.catalog.provider = none | openlibrary | google
 */
@Configuration
class CatalogConfig(
    @Value("\${app.catalog.provider:none}") private val provider: String,
    @Value("\${app.catalog.timeout:3s}") private val timeout: Duration,
    @Value("\${app.catalog.openlibrary.base-url:https://openlibrary.org}") private val openLibraryBaseUrl: String,
    @Value("\${app.catalog.google.base-url:https://www.googleapis.com}") private val googleBaseUrl: String,
    @Value("\${app.catalog.google.api-key:}") private val googleApiKey: String,
) {
    private val logger = LoggerFactory.getLogger(CatalogConfig::class.java)

    @Bean
    fun bookMetadataProvider(
        restClientBuilder: RestClient.Builder,
        cacheManager: CacheManager,
    ): BookMetadataProvider {
        val adapter =
            when (provider.trim().lowercase()) {
                "none", "" -> return NoOpBookMetadataProvider()
                "openlibrary" -> OpenLibraryBookMetadataProvider(restClient(restClientBuilder, openLibraryBaseUrl))
                "google" -> GoogleBooksBookMetadataProvider(restClient(restClientBuilder, googleBaseUrl), googleApiKey)
                else -> throw IllegalStateException("Unknown app.catalog.provider '$provider' (expected none, openlibrary or google)")
            }
        logger.info("Book metadata provider: {} (timeout {})", provider, timeout)
        return CachingBookMetadataProvider(adapter, cacheManager)
    }

    private fun restClient(
        builder: RestClient.Builder,
        baseUrl: String,
    ): RestClient {
        val httpClient =
            HttpClient.newBuilder()
                .connectTimeout(timeout)
                .proxy(ProxySelector.getDefault())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(timeout) }
        return builder.clone()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader("User-Agent", "KMA-Library (https://github.com/krzycuh/kma-library)")
            .build()
    }
}
