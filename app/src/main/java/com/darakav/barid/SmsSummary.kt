package com.darakav.barid

/**
 * What the home screen shows for a forwarded message: bank, direction and amount. Read on the phone
 * for display only; the paired server does the real parsing. Best effort: unknown parts are null.
 */
data class SmsSummary(val bank: String?, val direction: Direction?, val amount: Long?, val toman: Boolean) {
    enum class Direction { IN, OUT }

    companion object {
        private const val NUMBER = "(\\d{1,3}(?:,\\d{3})+|\\d+)"
        private val balance = Regex("(?:مانده|موجودی)[^\\d\\n]{0,12}[-+]?\\s*$NUMBER[ ]*[-+]?")
        private val identifier = Regex("(?:حساب|کارت|سپرده|شماره)\\s*[:：]?\\s*[\\d.*/\\-]{4,}")
        private val dotted = Regex("\\d+(?:\\.\\d+){2,}")
        private val date = Regex("\\d{1,4}[/\\-_]\\d{1,2}(?:[/\\-_]\\d{1,4})?")
        private val time = Regex("\\d{1,2}:\\d{2}(?::\\d{2})?")
        private val labelled = Regex(
            "(برداشت|واریز|برگشت[^\\d\\n:]{0,12}|خرید[^\\d\\n:]{0,16}|انتقال[^\\d\\n:]{0,16}|مبلغ|پایا|ساتنا|قسط|کارمزد|پرداخت)" +
                "\\s*[:：]?\\s*([+-]?)\\s*$NUMBER[ ]*([+-]?)",
        )
        private val signedBefore = Regex("(?m)^\\s*([+-])\\s*(\\d{1,3}(?:,\\d{3})+|\\d{4,})\\s*$")
        private val signedAfter = Regex("(?m)^\\s*(\\d{1,3}(?:,\\d{3})+|\\d{4,})\\s*([+-])\\s*$")
        private val grouped = Regex("\\d{1,3}(?:,\\d{3})+")
        private val creditWords = Regex("واریز|برگشت|بستانکار|دریافت")
        private val debitWords = Regex("برداشت|خرید|پرداخت|کارمزد|قسط|انتقال|بدهکار")

        fun read(sender: String?, raw: String): SmsSummary {
            val text = SmsFilter.normalize(raw).replace('٬', ',').replace('−', '-').replace('–', '-')
            val bank = Banks.detect(sender, text)?.code
            val body = listOf(balance, identifier, dotted, date, time).fold(text) { acc, r -> r.replace(acc, " ") }
            var sign: Char? = null
            var label: String? = null
            var number: String? = null
            labelled.find(body)?.let { m ->
                label = m.groupValues[1]
                number = m.groupValues[3]
                sign = (m.groupValues[2] + m.groupValues[4]).firstOrNull()
            }
            if (number == null) signedBefore.find(body)?.let { number = it.groupValues[2]; sign = it.groupValues[1][0] }
            if (number == null) signedAfter.find(body)?.let { number = it.groupValues[1]; sign = it.groupValues[2][0] }
            if (number == null) grouped.findAll(body).toList().singleOrNull()?.let { number = it.value }
            val amount = number?.replace(",", "")?.toLongOrNull()?.takeIf { it in 1..999_999_999_999_999 }
            val direction = when {
                sign == '+' -> Direction.IN
                sign == '-' -> Direction.OUT
                label != null && creditWords.containsMatchIn(label!!) -> Direction.IN
                label != null && debitWords.containsMatchIn(label!!) -> Direction.OUT
                creditWords.containsMatchIn(body) -> Direction.IN
                debitWords.containsMatchIn(body) -> Direction.OUT
                else -> null
            }
            return SmsSummary(bank, if (amount == null) null else direction, amount, text.contains("تومان"))
        }
    }
}
