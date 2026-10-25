package pl.kmazurek.library.infrastructure.catalog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.web.client.RestClient
import pl.kmazurek.library.application.service.BookMetadataProvider
import pl.kmazurek.library.config.CatalogConfig

class CatalogConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            // Like SpringApplication: lets @Value turn "3s" into a Duration
            .withInitializer { it.beanFactory.conversionService = ApplicationConversionService.getSharedInstance() }
            .withUserConfiguration(CatalogConfig::class.java)
            .withBean(RestClient.Builder::class.java, { RestClient.builder() })
            .withBean(CacheManager::class.java, { ConcurrentMapCacheManager() })

    @Test
    fun `should use no external catalog when providers are none or empty`() {
        listOf("none", "", " none , ").forEach { providers ->
            contextRunner.withPropertyValues("app.catalog.providers=$providers").run { context ->
                assertThat(context.getBean(BookMetadataProvider::class.java)).isInstanceOf(NoOpBookMetadataProvider::class.java)
            }
        }
    }

    @Test
    fun `should wrap single and chained providers in the cache`() {
        listOf("openlibrary", "google", "openlibrary,google", "Google, OpenLibrary").forEach { providers ->
            contextRunner.withPropertyValues("app.catalog.providers=$providers").run { context ->
                assertThat(context.getBean(BookMetadataProvider::class.java)).isInstanceOf(CachingBookMetadataProvider::class.java)
            }
        }
    }

    @Test
    fun `should fail fast on an unknown provider`() {
        contextRunner.withPropertyValues("app.catalog.providers=openlibrary,amazon").run { context ->
            assertThat(context).hasFailed()
        }
    }
}
