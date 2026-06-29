package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AiIntent {
    MOST_ABSENT,
    REVENUE_BY_MONTH,
    LATE_PAYMENTS,
    TOP_PERFORMING_STUDENTS,
    ATTENDANCE_RATE,
    UNKNOWN
}

data class QueryParams(
    val intent: AiIntent,
    val rawQuery: String,
    val monthNumber: String? = null, // "01" to "12"
    val yearString: String? = null,  // "2026", etc.
    val monthNameAr: String? = null
)

object AiQueryEngine {

    fun normalizeArabic(text: String): String {
        var result = text.trim().lowercase()
        // Replace different shapes of Alif with bare Alif
        result = result.replace("[إأآ]".toRegex(), "ا")
        // Replace Ta Marbouta with Haa
        result = result.replace("ة".toRegex(), "ه")
        // Replace Ya with Alif Maqsura/normal Ya normalization
        result = result.replace("ى".toRegex(), "ي")
        // Remove Arabic diacritics (harakat)
        result = result.replace("[\u064B-\u0652]".toRegex(), "")
        return result
    }

    fun parseQuery(query: String): QueryParams {
        val normalized = normalizeArabic(query)

        // 1. Check Most Absent Student Intent
        if (containsAny(normalized, listOf("اكثر غياب", "اكتر غياب", "اكثر الطلاب غيابا", "مين بيغيب", "الطلاب الغايبين", "اكثر واحد غايب", "اكتر طالب بيغيب", "غياب الطلاب"))) {
            return QueryParams(AiIntent.MOST_ABSENT, query)
        }

        // 2. Check Late Payments Intent
        if (containsAny(normalized, listOf("تاخر في الدفع", "متأخرين في الدفع", "المتأخرين بالدفع", "مين مدفعش", "غير مسددين", "الطلاب المتأخرين", "متأخرين بالدفع", "الطلاب الذين لم يدفعوا", "شغل المديونيات", "مديونيات"))) {
            return QueryParams(AiIntent.LATE_PAYMENTS, query)
        }

        // 3. Check Top Performing Students Intent
        if (containsAny(normalized, listOf("افضل الطلاب", "احسن الطلاب", "الطلاب المتفوقين", "الطلاب الاوائل", "اعلي درجات", "الطلاب المتميزين", "المتفوقين", "افضل اداء"))) {
            return QueryParams(AiIntent.TOP_PERFORMING_STUDENTS, query)
        }

        // 4. Check Attendance Rate Intent
        if (containsAny(normalized, listOf("نسبه الحضور", "معدل الحضور", "نسبه حضور", "الحضور هذا الاسبوع", "كم حضور", "احصائيات الحضور"))) {
            return QueryParams(AiIntent.ATTENDANCE_RATE, query)
        }

        // 5. Check Revenue / Finances Intent
        if (containsAny(normalized, listOf("ايراد", "ايرادات", "ارباح", "دخل", "فلوس", "مقبوضات", "حسابات", "الربح", "المصروفات"))) {
            val (monthNum, year, monthName) = extractMonthAndYear(normalized)
            return QueryParams(
                intent = AiIntent.REVENUE_BY_MONTH,
                rawQuery = query,
                monthNumber = monthNum,
                yearString = year,
                monthNameAr = monthName
            )
        }

        return QueryParams(AiIntent.UNKNOWN, query)
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun extractMonthAndYear(text: String): Triple<String?, String?, String?> {
        val calendar = Calendar.getInstance()
        var year = SimpleDateFormat("yyyy", Locale.US).format(calendar.time)

        // Simple year detection (e.g. 2024, 2025, 2026, etc.)
        val yearRegex = "\\b(202[0-9])\\b".toRegex()
        val yearMatch = yearRegex.find(text)
        if (yearMatch != null) {
            year = yearMatch.groupValues[1]
        }

        // Mapping Arabic month names to standard numbers
        val monthsMap = mapOf(
            "يناير" to "01", "جانفي" to "01",
            "فبراير" to "02", "فيفري" to "02",
            "مارس" to "03",
            "ابريل" to "04", "افريل" to "04",
            "مايو" to "05", "مي" to "05",
            "يونيو" to "06", "جوان" to "06",
            "يوليو" to "07", "جويلية" to "07",
            "اغسطس" to "08", "اوت" to "08",
            "سبتمبر" to "09",
            "اكتوبر" to "10",
            "نوفمبر" to "11",
            "ديسمبر" to "12"
        )

        for ((name, code) in monthsMap) {
            if (text.contains(normalizeArabic(name)) || text.contains(name)) {
                return Triple(code, year, name)
            }
        }

        // Fallback: Check for digits representing month (e.g., "شهر 5" or "شهر 05")
        val monthDigitRegex = "شهر\\s*(0?[1-9]|1[0-2])".toRegex()
        val match = monthDigitRegex.find(text)
        if (match != null) {
            val monthVal = match.groupValues[1].toInt()
            val formattedMonth = String.format(Locale.US, "%02d", monthVal)
            val monthArName = getArabicMonthName(formattedMonth)
            return Triple(formattedMonth, year, monthArName)
        }

        // Default to current month if no month name is specified
        val currentMonthNum = SimpleDateFormat("02", Locale.US).format(calendar.time) // Placeholder logic or calendar extraction
        val currentMonthCode = String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1)
        val currentMonthName = getArabicMonthName(currentMonthCode)
        return Triple(currentMonthCode, year, currentMonthName)
    }

    private fun getArabicMonthName(monthCode: String): String {
        return when (monthCode) {
            "01" -> "يناير"
            "02" -> "فبراير"
            "03" -> "مارس"
            "04" -> "أبريل"
            "05" -> "مايو"
            "06" -> "يونيو"
            "07" -> "يوليو"
            "08" -> "أغسطس"
            "09" -> "سبتمبر"
            "10" -> "أكتوبر"
            "11" -> "نوفمبر"
            "12" -> "ديسمبر"
            else -> "هذا الشهر"
        }
    }
}
