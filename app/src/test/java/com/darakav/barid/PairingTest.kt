package com.darakav.barid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder
import java.util.Base64

class PairingTest {
    private val key = "dkv_" + "A".repeat(43)

    private fun code(url: String, key: String, label: String = "Pixel") =
        "darakav-sms:1:" + Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"u\":\"$url\",\"k\":\"$key\",\"n\":\"$label\"}".toByteArray())

    @Test
    fun readsSetupCodesAndPairingLinks() {
        val setup = code("https://darakav.com/api/sms/ingest", key, "گوشی من")
        val pairing = Pairing.parse(setup)!!
        assertEquals("https://darakav.com/api/sms/ingest", pairing.url)
        assertEquals(key, pairing.key)
        assertEquals("گوشی من", pairing.label)
        assertTrue(pairing.encrypted)
        assertEquals(pairing, Pairing.parse("darakav://pair?c=" + URLEncoder.encode(setup, "UTF-8")))
        assertEquals(pairing, Pairing.parse("  $setup\n"))
    }

    @Test
    fun rejectsAnythingElse() {
        assertNull(Pairing.parse(null))
        assertNull(Pairing.parse("hello"))
        assertNull(Pairing.parse("darakav-sms:1:not-base64!"))
        assertNull(Pairing.parse(code("https://evil.example/collect", key)))
        assertNull(Pairing.parse(code("ftp://darakav.com/api/sms/ingest", key)))
        assertNull(Pairing.parse(code("https://darakav.com/api/sms/ingest", "abc")))
        assertNull(Pairing.parse("darakav://pair?x=1"))
    }
}
