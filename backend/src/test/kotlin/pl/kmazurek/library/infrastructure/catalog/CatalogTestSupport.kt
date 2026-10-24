package pl.kmazurek.library.infrastructure.catalog

import okhttp3.mockwebserver.MockResponse
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

/**
 * Fixtures in src/test/resources/catalog mirror the documented response shapes of the real APIs
 * (Open Library /api/books?jscmd=data, Google Books /volumes).
 */
internal fun fixture(path: String): String =
    requireNotNull(object {}.javaClass.getResource("/catalog/$path")) { "Missing fixture $path" }.readText()

internal fun jsonResponse(
    body: String,
    status: Int = 200,
): MockResponse = MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)

internal fun testRestClient(
    baseUrl: String,
    timeout: Duration = Duration.ofSeconds(3),
): RestClient {
    val httpClient = HttpClient.newBuilder().connectTimeout(timeout).build()
    return RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(timeout) })
        .build()
}
