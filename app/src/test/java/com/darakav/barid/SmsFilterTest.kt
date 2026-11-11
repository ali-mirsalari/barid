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

    @Test
    fun transferConfirmationCodesStayOnThePhone() {
        // Synthetic: a mobile-bank transfer code without a colon, ending with the autofill app hash.
        assertFalse(SmsFilter.shouldForward("RefahBank", "بانک رفاه\nهمراه بانک-پل\n مبلغ:12,500,000R\n به:\nIR12**3400\n رمز 48213\n03/14 10:05\nQx7PdLm2vZa"))
        assertFalse(SmsFilter.shouldForward("BankX", "بانک ایکس مبلغ 2,000,000 ریال رمز ۴۸۲۱۳"))
        assertFalse(SmsFilter.shouldForward("BankX", "<#> بانک ایکس مبلغ 2,000,000 ریال کد 4821"))
        assertFalse(SmsFilter.shouldForward("BankX", "بانک ایکس خرید 2,000,000 ریال\nکد: 4821\nFA9b+Qe/1Zk"))
    }

    @Test
    fun ordinaryTransactionsAreNotMistakenForCodes() {
        assertTrue(SmsFilter.shouldForward("BankX", "بانک ایکس\nخرید رمزارز\nبرداشت: 2,000,000 ریال\nمانده: 9,000,000"))
        assertTrue(SmsFilter.shouldForward("BankX", "بانک ایکس\nبرداشت: 2,000,000 ریال\nحساب\n01000000001"))
        assertTrue(SmsFilter.shouldForward("BankX", "بانک ایکس\nواریز: 2,000,000 ریال\nکد پیگیری 123456"))
    }
}
