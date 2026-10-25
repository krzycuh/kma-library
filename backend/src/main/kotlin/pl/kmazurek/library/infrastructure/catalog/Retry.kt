package pl.kmazurek.library.infrastructure.catalog

import org.springframework.web.client.HttpServerErrorException

private const val RETRY_DELAY_MILLIS = 300L

/**
 * Catalog APIs (Google Books especially) answer 5xx now and then and succeed right after.
 * One quick retry on a 5xx only; timeouts are not retried, they already cost the whole timeout.
 */
internal fun <T> retryOnceOnServerError(call: () -> T): T =
    try {
        call()
    } catch (ex: HttpServerErrorException) {
        Thread.sleep(RETRY_DELAY_MILLIS)
        call()
    }
