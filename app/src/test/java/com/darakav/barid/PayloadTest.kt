package com.darakav.barid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class PayloadTest {
    private val item = Queued("1", "+98999000", "واریز \"۱۲۰٬۰۰۰\"\nمانده {{from}}", 1_762_850_000_000)

    @Test
    fun batchCarriesEveryField() {
        val json = JSONObject(Payload.batch(listOf(item)))
        val message = json.getJSONArray("messages").getJSONObject(0)
        assertEquals(item.from, message.getString("from"))
        assertEquals(item.text, message.getString("text"))
        assertEquals(item.receivedStamp, message.getLong("receivedStamp"))
        assertEquals("{\"messages\":[]}", Payload.batch(emptyList()))
    }

    @Test
    fun jsonTemplatesEscapeValuesAndNeverExpandMessageText() {
        val json = JSONObject(Payload.fill(Payload.DEFAULT_TEMPLATE, item, ZoneOffset.UTC))
        assertEquals(item.text, json.getString("text"))
        assertEquals(item.from, json.getString("from"))
        assertEquals("2025-11-11T08:33:20Z", json.getString("receivedAt"))
        assertTrue(Payload.contentType(Payload.DEFAULT_TEMPLATE).startsWith("application/json"))
    }

    @Test
    fun plainTemplatesAreSentAsIs() {
        val template = "{{from}} at {{receivedStamp}}: {{text}}"
        assertEquals("+98999000 at 1762850000000: " + item.text, Payload.fill(template, item, ZoneOffset.UTC))
        assertTrue(Payload.contentType(template).startsWith("text/plain"))
        assertTrue(Payload.contentType("").startsWith("application/json"))
    }

    @Test
    fun responsesAreClassified() {
        assertEquals(Forwarder.Result.Ok, Forwarder.classify(204, darakav = false))
        assertEquals(Forwarder.Result.Rejected, Forwarder.classify(401, darakav = true))
        assertEquals(Forwarder.Result.Rejected, Forwarder.classify(403, darakav = false))
        assertEquals(Forwarder.Result.Retry("HTTP 403"), Forwarder.classify(403, darakav = true))
        assertEquals(Forwarder.Result.Invalid, Forwarder.classify(422, darakav = false))
        assertEquals(Forwarder.Result.Retry("HTTP 503"), Forwarder.classify(503, darakav = false))
    }
}
