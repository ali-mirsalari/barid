package com.darakav.barid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic messages in the shapes Iranian banks use; account numbers are fake. */
class SmsSummaryTest {
    @Test
    fun labelledWithdrawalWithTrailingSign() {
        val s = SmsSummary.read("700717", "بانك ملي ايران\nبرداشت:500,000-\nحساب:10000\nمانده:23,488,359\n0705-20:46")
        assertEquals("melli", s.bank)
        assertEquals(SmsSummary.Direction.OUT, s.direction)
        assertEquals(500_000L, s.amount)
        assertFalse(s.toman)
    }

    @Test
    fun signedLineIgnoresDottedAccountDateAndBalance() {
        val s = SmsSummary.read("ResalatBank", "10.1234567.1 \n-13,550,000 \n07/11_00:12 \nمانده: 428,428,242")
        assertEquals("resalat", s.bank)
        assertEquals(SmsSummary.Direction.OUT, s.direction)
        assertEquals(13_550_000L, s.amount)
    }

    @Test
    fun persianDigitsDepositByName() {
        val s = SmsSummary.read("+98100", "*بانک تجارت*\nحساب: 0100000000001\nواریز: ۵٬۰۰۰٬۰۰۰ ریال")
        assertEquals("tejarat", s.bank)
        assertEquals(SmsSummary.Direction.IN, s.direction)
        assertEquals(5_000_000L, s.amount)
    }

    @Test
    fun refundIsADepositAndTomanIsKept() {
        val s = SmsSummary.read("Bank", "بانک ملت\nبرگشت خرید: 120,000 تومان\nکارت 6104****1234")
        assertEquals("mellat", s.bank)
        assertEquals(SmsSummary.Direction.IN, s.direction)
        assertEquals(120_000L, s.amount)
        assertTrue(s.toman)
    }

    @Test
    fun plusSignedCredit() {
        val s = SmsSummary.read("B.QMEHRIRAN", "قرض الحسنه مهر ايران\n+4,692,800\nمانده: 12,000,000")
        assertEquals("mehriran", s.bank)
        assertEquals(SmsSummary.Direction.IN, s.direction)
        assertEquals(4_692_800L, s.amount)
    }

    @Test
    fun balanceOnlyHasNoAmount() {
        val s = SmsSummary.read("700717", "بانک ملی\nموجودی حساب: 9,000,000")
        assertNull(s.amount)
        assertNull(s.direction)
    }

    @Test
    fun unknownBankStillReadsAmount() {
        val s = SmsSummary.read("Unknown", "خرید از فروشگاه: 85,000 ریال")
        assertNull(s.bank)
        assertEquals(SmsSummary.Direction.OUT, s.direction)
        assertEquals(85_000L, s.amount)
    }
}
