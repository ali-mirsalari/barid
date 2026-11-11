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
    fun invoicesAndShopMessagesStayOnThePhone() {
        // Synthetic: a hosting invoice with an amount in toman and a payment link.
        assertFalse(SmsFilter.shouldForward("HostCo", "هاست نمونه\nمشترک گرامی، یک فاکتور پرداخت‌نشده در حساب شما ثبت شده است.\nمبلغ فاکتور: 1,250,000 تومان\nمهلت پرداخت: 1405/09/01\nپرداخت آنلاین: https://example.com/pay"))
        assertFalse(SmsFilter.shouldForward("ShopCo", "تخفیف ویژه! همه کالاها با 500,000 تومان تخفیف"))
        assertFalse(SmsFilter.shouldForward("Telecom", "صورتحساب این ماه شما 320,000 ریال است"))
        // Synthetic payment-app receipt: the bank's own SMS reports the same purchase.
        assertFalse(SmsFilter.shouldForward("PayApp", "خرید کتاب نمونه از (فروشگاه) به مبلغ ۸۵,۰۰۰ تومان.\nکیف پول نمونه\nشناسه: 123456r"))
    }

    @Test
    fun requestsAwaitingAReplyStayOnThePhone() {
        // Synthetic loan application from a recognized bank, asking for a reply.
        // Synthetic installment reminder naming a bank, and a future reimbursement.
        assertFalse(SmsFilter.shouldForward("PayApp", "تسویه زودتر از موعد وام بانک ملت\nکاربر گرامی\nقسط بعدی شما به مبلغ ۵۰٬۰۰۰٬۰۰۰ ریال ۳ روز دیگر سررسید می شود.\nتاریخ سررسید:۱۴۰۵/۰۹/۰۱\nمشاهده و پرداخت: example.com/x"))
        assertFalse(SmsFilter.shouldForward("Insurer", "بیمه شده گرامی، مبلغ 12,345,678 ریال بابت هزینه درمانی مورد تایید قرار گرفت و طی روزهای آتی به حساب شما واریز می‌گردد."))
        // Synthetic dividend notice that names a bank: the bank's own SMS reports the deposit.
        assertFalse(SmsFilter.shouldForward("Registry", "سهامدار محترم؛ نام نمونه، مبلغ 150,000 ریال بابت سود نماد نمونه جهت واریز به حساب شما نزد بانک سامان ارسال گردید."))
        assertFalse(SmsFilter.shouldForward("RefahBank", "مشتری گرامی\nبانک رفاه درحال ثبت درخواست تسهیلات به مبلغ 500,000,000 ریال است. لطفا درصورت تایید عدد 1 را ارسال نمایید."))
    }

    @Test
    fun bankMessagesWithoutTheUsualWordsStillPass() {
        assertTrue(SmsFilter.shouldForward("BankMellat", "بانک ملت\nحساب 1234\n+2,500,000 ریال\n03/14-10:05"))
        assertTrue(SmsFilter.shouldForward("RefahBank", "بانك رفاه\nحساب 112233445\nواریز12,000,000+\nانتقال از حساب 556677889 نام نمونه \nمانده18,250,000\n5/08/14-14:45"))
        assertTrue(SmsFilter.shouldForward("BankX", "بانک ایکس\nصورتحساب کارت\nخرید: 750,000 ریال\nمانده: 4,000,000"))
    }

    @Test
    fun ordinaryTransactionsAreNotMistakenForCodes() {
        assertTrue(SmsFilter.shouldForward("BankX", "بانک ایکس\nخرید رمزارز\nبرداشت: 2,000,000 ریال\nمانده: 9,000,000"))
        assertTrue(SmsFilter.shouldForward("BankX", "بانک ایکس\nبرداشت: 2,000,000 ریال\nحساب\n01000000001"))
        assertTrue(SmsFilter.shouldForward("BankX", "بانک ایکس\nواریز: 2,000,000 ریال\nکد پیگیری 123456"))
    }
}
