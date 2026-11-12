package com.darakav.barid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    private fun release(tag: String, asset: String, extra: String = "") =
        """{"tag_name":"$tag"$extra,"assets":[{"name":"notes.txt","browser_download_url":"https://example.com/notes.txt","size":3},
        {"name":"$asset","browser_download_url":"https://github.com/ali-mirsalari/barid/releases/download/$tag/$asset","size":2720162}]}"""

    @Test
    fun readsTheVersionAndItsApk() {
        val r = Updates.parse(release("v1.0.4", "barid-1.0.4.apk"))!!
        assertEquals("1.0.4", r.version)
        assertTrue(r.apkUrl.endsWith("/v1.0.4/barid-1.0.4.apk"))
        assertEquals(2720162L, r.size)
    }

    @Test
    fun ignoresReleasesWithoutAMatchingApkOrNotFinal() {
        assertNull(Updates.parse(release("v1.0.4", "barid-1.0.3.apk")))
        assertNull(Updates.parse(release("nightly", "barid-nightly.apk")))
        assertNull(Updates.parse(release("v1.0.4", "barid-1.0.4.apk", ",\"prerelease\":true")))
        assertNull(Updates.parse(release("v1.0.4", "barid-1.0.4.apk", ",\"draft\":true")))
    }

    @Test
    fun comparesVersionsByNumber() {
        assertTrue(Updates.newer("1.0.10", "1.0.9"))
        assertTrue(Updates.newer("1.1", "1.0.9"))
        assertTrue(Updates.newer("2.0.0", "1.9.9"))
        assertFalse(Updates.newer("1.0.3", "1.0.3"))
        assertFalse(Updates.newer("1.0", "1.0.0"))
        assertFalse(Updates.newer("1.0.2", "1.0.3"))
    }
}
