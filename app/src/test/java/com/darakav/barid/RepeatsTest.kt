package com.darakav.barid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepeatsTest {
    @Test
    fun theSameMessageHasOneKeyWhateverItsForm() {
        val key = Seen.key("BankMellat", "برداشت ۱۲۰٬۰۰۰ ریال\nمانده ۵۰۰٬۰۰۰")
        assertEquals(key, Seen.key("bankmellat ", "برداشت 120٬000 ريال  مانده 500٬000 "))
        assertNotEquals(key, Seen.key("BankMellat", "برداشت ۱۲۰٬۰۰۰ ریال\nمانده ۳۸۰٬۰۰۰"))
        assertNotEquals(key, Seen.key("BankMelli", "برداشت ۱۲۰٬۰۰۰ ریال\nمانده ۵۰۰٬۰۰۰"))
    }

    @Test
    fun recentKeepsOneEntryPerMessagePreferringTheSentCopy() {
        val at = 1_762_850_000_000
        val waiting = LogEntry("a", "mellat", SmsSummary.Direction.OUT, 120_000, false, at, LogEntry.Status.WAITING)
        val sent = waiting.copy(id = "b", status = LogEntry.Status.SENT, sentAt = at + 5)
        val other = waiting.copy(id = "c", receivedAt = at + 60_000)
        assertEquals(listOf("b", "c"), ActivityLog.distinct(listOf(waiting, sent, other)).map { it.id })
    }

    @Test
    fun redirectsAreFollowedOnlyWithinTheSameSite() {
        assertEquals("https://darakav.com/api/sms/ingest", Forwarder.redirectTarget("http://darakav.com/api/sms/ingest", "https://darakav.com/api/sms/ingest"))
        assertEquals("https://darakav.com/api/sms/ingest", Forwarder.redirectTarget("https://www.darakav.com/api/sms/ingest", "https://darakav.com/api/sms/ingest"))
        assertEquals("https://example.com/new", Forwarder.redirectTarget("https://example.com/old", "/new"))
        assertNull(Forwarder.redirectTarget("https://darakav.com/api/sms/ingest", "http://darakav.com/api/sms/ingest"))
        assertNull(Forwarder.redirectTarget("https://darakav.com/api/sms/ingest", "https://evil.example/collect"))
        assertNull(Forwarder.redirectTarget("https://darakav.com/api/sms/ingest", null))
    }
}
