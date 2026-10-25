package pl.kmazurek.library.config

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import java.util.Properties

class AppVersionTest {
    private fun provider(properties: Map<String, String>?): ObjectProvider<BuildProperties> {
        val provider = mockk<ObjectProvider<BuildProperties>>()
        every { provider.ifAvailable } returns properties?.let { BuildProperties(Properties().apply { putAll(it) }) }
        return provider
    }

    @Test
    fun `should read commit, release tag and build time from build info`() {
        val version = AppVersion(provider(mapOf("gitSha" to "abc1234def", "releaseTag" to "v0.2.0", "time" to "2026-10-25T17:00:00Z")))

        assertEquals("abc1234def", version.gitSha)
        assertEquals("v0.2.0", version.releaseTag)
        assertEquals("2026-10-25T17:00:00Z", version.buildTime.toString())
    }

    @Test
    fun `should say dev without build info or without a commit`() {
        val withoutBuildInfo = AppVersion(provider(null))
        assertEquals("dev", withoutBuildInfo.gitSha)
        assertNull(withoutBuildInfo.releaseTag)
        assertNull(withoutBuildInfo.buildTime)

        val localBuild = AppVersion(provider(mapOf("gitSha" to "dev", "releaseTag" to "")))
        assertEquals("dev", localBuild.gitSha)
        assertNull(localBuild.releaseTag)
    }
}
