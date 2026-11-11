package com.darakav.barid

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsFilterTest {
    @Test
    fun forwardsBankTransactions() {
        assertTrue(SmsFilter.shouldForward("700717", "بانك ملي ايران\nبرداشت:500,000-\nحساب:10000\nمانده:23,488,359\n0705-20:46"))
        assertTrue(SmsFilter.shouldForward("ResalatBank", "10.1234567.1 \n-13,550,000 \n07/11_00:12 \nمانده: 428,428,242"))
        assertTrue(SmsFilter.shouldForward("TejaratBank", "*بانک تجارت*\nحساب: 0100000000001\nواریز: ۵٬۰۰۰٬۰۰۰ ریال"))
    }

    @Test
    fun keepsSecretsAndPersonalMessagesOnThePhone() {
        assertFalse(SmsFilter.shouldForward("700717", "رمز پویا: 552211 مبلغ 1,000,000 ریال"))
        assertFalse(SmsFilter.shouldForward("BankMellat", "کد تایید شما 123456"))
        assertFalse(SmsFilter.shouldForward("Shop", "Your OTP is 552211"))
        assertFalse(SmsFilter.shouldForward("09121234567", "واریز کردم 1,000,000 تومان"))
        assertFalse(SmsFilter.shouldForward("Promo", "جشنواره بزرگ تخفیف"))
        assertFalse(SmsFilter.shouldForward("700717", ""))
    }
}
