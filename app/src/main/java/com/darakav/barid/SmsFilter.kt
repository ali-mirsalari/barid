package com.darakav.barid

/**
 * Decides on the phone which messages leave it: bank transaction and balance messages only, never
 * one-time passwords or verification codes. Darakav applies the same rules again on the server.
 */
object SmsFilter {
    private val secret = Regex(
        "رمز\\s*(?:پویا|دوم|یکبار|یک\\s*بار|اینترنتی|موقت)|کد\\s*(?:تایید|تأیید|فعال\\s*سازی|امنیتی|ورود|یکبار)" +
            "|(?:^|[^\\p{L}])(?:otp|one[- ]time|verification code|passcode)(?:[^\\p{L}]|$)|رمز\\s*:\\s*\\d{4,}",
        RegexOption.IGNORE_CASE,
    )
    private val money = Regex("مانده|موجودی|برداشت|واریز|خرید|انتقال|پایا|ساتنا|قسط|کارمزد|بانک|ریال|تومان")
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

    fun isSecret(text: String): Boolean = secret.containsMatchIn(normalize(text))

    /** True for messages worth forwarding: bank words together with an amount-like number. */
    fun shouldForward(sender: String?, text: String): Boolean {
        if (text.isBlank() || text.length > 1600) return false
        val t = normalize(text)
        if (secret.containsMatchIn(t)) return false
        // Personal numbers (09…/+989…) do not send bank messages.
        val s = sender?.replace(" ", "") ?: ""
        if (Regex("^(\\+98|0098|0)?9\\d{9}$").matches(s)) return false
        return money.containsMatchIn(t) && amount.containsMatchIn(t)
    }
}
