package pl.kmazurek.library.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.kmazurek.library.domain.repository.LibraryRepository
import pl.kmazurek.library.domain.service.LibraryAccessPolicy

/**
 * Configuration for domain services (plain classes, registered as beans here)
 */
@Configuration
class ServiceConfig {
    @Bean
    fun libraryAccessPolicy(libraryRepository: LibraryRepository): LibraryAccessPolicy = LibraryAccessPolicy(libraryRepository)
}
