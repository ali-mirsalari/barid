package com.darakav.barid

/** Iranian banks, recognized by sender ID or by the name printed in the message, for display only. */
data class Bank(val code: String, val en: String, val fa: String, val names: List<String>, val senders: List<String> = emptyList()) {
    /** Two-letter badge in English, unique per bank. */
    val abbr: String get() = ABBR[code] ?: en.take(2).uppercase(java.util.Locale.ROOT)

    private companion object {
        val ABBR = mapOf(
            "melli" to "ML", "mellat" to "MT", "tejarat" to "TJ", "saderat" to "SD", "sepah" to "SP", "keshavarzi" to "KS",
            "maskan" to "MS", "refah" to "RF", "post" to "PB", "toseesaderat" to "TS", "sanatmadan" to "SM", "pasargad" to "PA",
            "saman" to "SA", "parsian" to "PR", "eghtesadnovin" to "EN", "karafarin" to "KA", "sina" to "SI", "sarmayeh" to "SR",
            "shahr" to "SH", "dey" to "DY", "ayandeh" to "AY", "gardeshgari" to "GA", "iranzamin" to "IZ", "khavarmianeh" to "KM",
            "resalat" to "RE", "mehriran" to "MI", "melal" to "MA", "blu" to "BL", "toseetaavon" to "TT",
        )
    }
}

object Banks {
    val ALL = listOf(
        Bank("melli", "Melli", "ملی", listOf("بانک ملی", "ملی ایران"), listOf("700717", "BankMelli")),
        Bank("mellat", "Mellat", "ملت", listOf("بانک ملت"), listOf("BankMellat")),
        Bank("tejarat", "Tejarat", "تجارت", listOf("بانک تجارت"), listOf("TejaratBank")),
        Bank("saderat", "Saderat", "صادرات", listOf("بانک صادرات")),
        Bank("sepah", "Sepah", "سپه", listOf("بانک سپه")),
        Bank("keshavarzi", "Keshavarzi", "کشاورزی", listOf("بانک کشاورزی")),
        Bank("maskan", "Maskan", "مسکن", listOf("بانک مسکن")),
        Bank("refah", "Refah", "رفاه", listOf("بانک رفاه")),
        Bank("post", "Post Bank", "پست بانک", listOf("پست بانک")),
        Bank("toseesaderat", "Tosee Saderat", "توسعه صادرات", listOf("توسعه صادرات")),
        Bank("sanatmadan", "Sanat va Madan", "صنعت و معدن", listOf("صنعت و معدن")),
        Bank("pasargad", "Pasargad", "پاسارگاد", listOf("پاسارگاد")),
        Bank("saman", "Saman", "سامان", listOf("بانک سامان")),
        Bank("parsian", "Parsian", "پارسیان", listOf("پارسیان")),
        Bank("eghtesadnovin", "Eghtesad Novin", "اقتصاد نوین", listOf("اقتصاد نوین")),
        Bank("karafarin", "Karafarin", "کارآفرین", listOf("کارآفرین")),
        Bank("sina", "Sina", "سینا", listOf("بانک سینا")),
        Bank("sarmayeh", "Sarmayeh", "سرمایه", listOf("بانک سرمایه")),
        Bank("shahr", "Shahr", "شهر", listOf("بانک شهر")),
        Bank("dey", "Dey", "دی", listOf("بانک دی")),
        Bank("ayandeh", "Ayandeh", "آینده", listOf("بانک آینده")),
        Bank("gardeshgari", "Gardeshgari", "گردشگری", listOf("گردشگری")),
        Bank("iranzamin", "Iran Zamin", "ایران زمین", listOf("ایران زمین")),
        Bank("khavarmianeh", "Khavarmianeh", "خاورمیانه", listOf("خاورمیانه"), listOf("KH M BANK")),
        Bank("resalat", "Resalat", "رسالت", listOf("رسالت"), listOf("ResalatBank")),
        Bank("mehriran", "Mehr Iran", "مهر ایران", listOf("مهر ایران"), listOf("B.QMEHRIRAN")),
        Bank("melal", "Melal", "ملل", listOf("موسسه ملل", "ملل")),
        Bank("blu", "Blu", "بلو", listOf("بلو بانک", "بلوبانک", "blu"), listOf("BluBank", "blu")),
        Bank("toseetaavon", "Tosee Taavon", "توسعه تعاون", listOf("توسعه تعاون")),
    )

    /** Latin names ("blu") match in any case as a whole word, so "Blue" or "bluetooth" do not count. */
    private fun mentions(head: String, name: String): Boolean =
        if (name.all { it in 'a'..'z' }) Regex("(?<![A-Za-z])$name(?![A-Za-z])", RegexOption.IGNORE_CASE).containsMatchIn(head)
        else head.contains(name)

    fun byCode(code: String?): Bank? = ALL.firstOrNull { it.code == code }

    /** Sender first, then the longest bank name found in the first two lines. */
    fun detect(sender: String?, normalizedText: String): Bank? {
        val s = sender?.trim().orEmpty()
        ALL.firstOrNull { bank -> bank.senders.any { it.equals(s, ignoreCase = true) || s.endsWith(it) && it.all(Char::isDigit) } }?.let { return it }
        val head = normalizedText.lineSequence().take(2).joinToString(" ")
        return ALL.flatMap { bank -> bank.names.map { bank to it } }
            .filter { (_, name) -> mentions(head, name) }
            .maxByOrNull { (_, name) -> name.length }?.first
    }
}
