package com.darakav.barid

/**
 * Decides on the phone which messages leave it: bank transaction and balance messages only, never
 * one-time passwords or verification codes. Darakav applies the same rules again on the server.
 */
object SmsFilter {
    private val secret = Regex(
        "رمز\\s*(?:پویا|دوم|یکبار|یک\\s*بار|اینترنتی|موقت)|کد\\s*(?:تایید|تأیید|فعال\\s*سازی|امنیتی|ورود|یکبار)" +
            "|(?:^|[^\\p{L}])(?:otp|one[- ]time|verification code|passcode)(?:[^\\p{L}]|$)|رمز\\s*:?\\s*\\d{4,}",
        RegexOption.IGNORE_CASE,
    )
    // Words of an actual account movement or balance. Currency words alone ("ریال", "تومان") are not
    // enough: invoices, shops and bill reminders use them too.
    private val transaction = Regex("مانده|موجودی|برداشت|واریز|خرید|انتقال|پایا|ساتنا|قسط|کارمزد|بستانکار|بدهکار|برگشت")
    // From a sender that is not a recognized bank, only these mark a bank message (Darakav's server uses
    // the same rule): invoices, shops and payment apps send amounts with "خرید" or "پرداخت" too.
    private val accountWords = Regex("مانده|موجودی|برداشت|واریز")
    // For those senders a balance word counts only with an amount on its line ("موجودی: 1,000"), not as a
    // "مشاهده موجودی" link, and wallet, points, discount and cashback notices stay: an app's wallet is not
    // a bank account.
    private val depositOrWithdrawal = Regex("برداشت|واریز")
    private val balanceWithAmount = Regex("(?:مانده|موجودی)[^\\n]{0,40}?(?:\\d{1,3}(?:[,٬]\\d{3})+|\\d{4,})")
    private val wallet = Regex("کیف\\s*پول|امتیاز|کد\\s*تخفیف|کش\\s*بک|cashback|wallet", RegexOption.IGNORE_CASE)
    private val currency = Regex("ریال|تومان|\\d\\s*R\\b")
    // Not a transaction: a request waiting for the owner's reply (a loan application to confirm), or a
    // notice of money on its way ("… جهت واریز به حساب شما … ارسال گردید", a dividend notice); the
    // bank's own SMS reports the actual deposit.
    // Reminders ("قسط … سررسید می‌شود") and future deposits ("واریز می‌گردد", "طی روزهای آتی") too.
    private val request = Regex(
        "در\\s*صورت\\s*(?:عدم\\s*)?تا[یئ]ید|درخواست\\s*تسهیلات|جهت\\s*واریز|سهامدار\\s*محترم" +
            "|سررسید\\s*می\\s*شود|واریز\\s*(?:می\\s*گردد|می\\s*شود|خواهد\\s*شد)|روزهای\\s*آتی",
    )
    private val amount = Regex("\\d{1,3}(?:[,٬]\\d{3})+|[+-]\\s*\\d{4,}|\\d{4,}\\s*[+-]")

    fun normalize(text: String): String = buildString(text.length) {
        for (c in text) append(
            when (c) {
                in '۰'..'۹' -> '0' + (c - '۰')
                in '٠'..'٩' -> '0' + (c - '٠')
                'ي', 'ى' -> 'ی'
                'ك' -> 'ک'
                '‌', '‏', '‎' -> ' '
                else -> c
            },
        )
    }

    // Android's SMS Retriever: code messages start with "<#>" or end with the app's 11-character hash
    // (mixed-case base64), so apps can read the code automatically. Only one-time codes carry it.
    private val retrieverHash = Regex("^(?=[A-Za-z0-9+/]*[a-z])(?=[A-Za-z0-9+/]*[A-Z])[A-Za-z0-9+/]{11}$")

    private fun forAutofill(text: String): Boolean =
        text.trimStart().startsWith("<#>") || retrieverHash.matches(text.trimEnd().substringAfterLast('\n').trim())

    fun isSecret(text: String): Boolean = secret.containsMatchIn(normalize(text)) || forAutofill(text)

    /**
     * True for messages worth forwarding: an amount-like number, and from a recognized bank a word of
     * an account movement or a currency; from any other sender a balance with its amount, or a deposit or
     * withdrawal word, and no wallet notice.
     */
    fun shouldForward(sender: String?, text: String): Boolean {
        if (text.isBlank() || text.length > 1600) return false
        val t = normalize(text)
        if (secret.containsMatchIn(t) || forAutofill(text)) return false
        if (!amount.containsMatchIn(t) || request.containsMatchIn(t)) return false
        val bank = Banks.detect(sender, t)
        // Mobile-shaped numbers (09…/+989…) are people, except that some banks send from such lines
        // (Blu uses a 0999 number): those messages still name the bank and show a balance or withdrawal.
        val s = sender?.replace(" ", "") ?: ""
        if (Regex("^(\\+98|0098|0)?9\\d{9}$").matches(s)) return bank != null && accountWords.containsMatchIn(t)
        if (bank == null)
            return !wallet.containsMatchIn(t) &&
                (depositOrWithdrawal.containsMatchIn(t) || balanceWithAmount.containsMatchIn(t))
        return transaction.containsMatchIn(t) || currency.containsMatchIn(t)
    }
}
