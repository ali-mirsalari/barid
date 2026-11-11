package com.darakav.barid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(pairing.darakav)
        assertTrue(pairing.trusted)
        assertEquals("Authorization" to "Bearer $key", pairing.auth)
    }

    @Test
    fun rejectsAnythingElse() {
        assertNull(Pairing.parse(null))
        assertNull(Pairing.parse("hello"))
        assertNull(Pairing.parse("darakav-sms:1:not-base64!"))
        assertNull(Pairing.parse("barid:1:not-base64!"))
        assertNull(Pairing.parse(code("https://evil.example/collect", key)))
        assertNull(Pairing.parse(code("ftp://darakav.com/api/sms/ingest", key)))
        assertNull(Pairing.parse(code("https://darakav.com/api/sms/ingest", "abc")))
        assertNull(Pairing.parse("darakav://pair?x=1"))
        assertNull(Pairing.parse("barid:1:" + Base64.getUrlEncoder().encodeToString("{\"u\":\"javascript:alert(1)\"}".toByteArray())))
    }

    @Test
    fun darakavCodesForOtherHostsAreNotTrusted() {
        assertFalse(Pairing.parse(code("https://darakav.com.evil.example/api/sms/ingest", key))!!.trusted)
        assertFalse(Pairing.parse(code("http://192.168.1.5:8080/api/sms/ingest", key))!!.trusted)
        assertTrue(Pairing.parse(code("https://staging.darakav.com/api/sms/ingest", key))!!.trusted)
    }

    @Test
    fun baridCodesRoundTrip() {
        val custom = Pairing.custom(" https://n8n.example.org/webhook/sms ", "s3cret", "My n8n", "X-Api-Key", "{\"text\":\"{{text}}\"}")!!
        assertEquals("https://n8n.example.org/webhook/sms", custom.url)
        assertEquals("X-Api-Key" to "s3cret", custom.auth)
        assertFalse(custom.darakav)
        assertFalse(custom.trusted)
        assertEquals(custom, Pairing.parse(custom.encode()))
        assertEquals(custom, Pairing.parse(custom.link()))
        assertTrue(custom.encode().startsWith("barid:1:"))

        val open = Pairing.custom("http://10.0.0.2:5000/sms", "", "", "", "")!!
        assertNull(open.auth)
        assertEquals(open, Pairing.parse(open.encode()))
    }

    @Test
    fun customFieldsAreValidated() {
        assertEquals(Pairing.Field.URL, Pairing.problem("example.com/sms", "", "", ""))
        assertEquals(Pairing.Field.URL, Pairing.problem("ftp://example.com/sms", "", "", ""))
        assertEquals(Pairing.Field.URL, Pairing.problem("https://", "", "", ""))
        assertEquals(Pairing.Field.KEY, Pairing.problem("https://example.com", "a\nb", "", ""))
        assertEquals(Pairing.Field.KEY, Pairing.problem("https://example.com", "", "X-Api-Key", ""))
        assertEquals(Pairing.Field.HEADER, Pairing.problem("https://example.com", "k", "Bad Header", ""))
        assertEquals(Pairing.Field.HEADER, Pairing.problem("https://example.com", "k", "Host", ""))
        assertEquals(Pairing.Field.TEMPLATE, Pairing.problem("https://example.com", "", "", "{\"from\":\"{{from}}\"}"))
        assertNull(Pairing.problem("https://example.com:8443/a?b=c", "k", "", ""))
    }
}
