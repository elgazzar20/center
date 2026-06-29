package com.example.util.agent

import com.example.util.AiQueryEngine
import java.util.Locale

object AiIntentParser {

    fun parseArabicIntent(query: String): ParsedCommand {
        val normalized = AiQueryEngine.normalizeArabic(query)
        val entities = mutableMapOf<String, String>()

        // 1. Confirm / Cancel Intent Detection
        if (normalized.equals("نعم") || normalized.equals("موافق") || normalized.equals("اكد") || normalized.equals("تاكيد") || normalized.contains("موافق على") || normalized.contains("نفذ العملية")) {
            return ParsedCommand(AgentIntent.CONFIRM_ACTION, 1.0f, query)
        }
        if (normalized.equals("لا") || normalized.equals("الغاء") || normalized.equals("تراجع") || normalized.equals("ارفض") || normalized.contains("لا تفعل") || normalized.contains("الغ العملية")) {
            return ParsedCommand(AgentIntent.CANCEL_ACTION, 1.0f, query)
        }

        // 2. Add Student
        if (containsAny(normalized, listOf("اضف طالب", "تسجيل طالب", "طالب جديد", "اضافه طالب", "اضف تلميذ"))) {
            extractAddStudentEntities(normalized, query, entities)
            return ParsedCommand(AgentIntent.ADD_STUDENT, 0.9f, query, entities)
        }

        // 3. Edit Student
        if (containsAny(normalized, listOf("عدل طالب", "تعديل طالب", "تعديل بيانات", "تغيير بيانات", "عدل بيانات"))) {
            extractStudentName(normalized, query, entities)
            return ParsedCommand(AgentIntent.EDIT_STUDENT, 0.85f, query, entities)
        }

        // 4. Delete Student
        if (containsAny(normalized, listOf("احذف طالب", "حذف طالب", "مسح طالب", "احذف الطالب", "حذف الطالب"))) {
            extractStudentName(normalized, query, entities)
            return ParsedCommand(AgentIntent.DELETE_STUDENT, 0.95f, query, entities)
        }

        // 5. Mark Absence
        if (containsAny(normalized, listOf("سجل غياب", "غياب طالب", "غياب الطالب", "غايب اليوم", "تسجيل غياب"))) {
            extractStudentOrGroup(normalized, query, entities)
            return ParsedCommand(AgentIntent.MARK_ABSENCE, 0.9f, query, entities)
        }

        // 6. Mark Attendance
        if (containsAny(normalized, listOf("سجل حضور", "حضور طالب", "حضور الطالب", "حاضر اليوم", "تسجيل حضور", "حضر اليوم"))) {
            extractStudentOrGroup(normalized, query, entities)
            return ParsedCommand(AgentIntent.MARK_ATTENDANCE, 0.9f, query, entities)
        }

        // 7. Create Group
        if (containsAny(normalized, listOf("انشئ مجموعه", "مجموعه جديده", "عمل مجموعه", "انشاء مجموعه", "اضف مجموعه", "اضافه مجموعه"))) {
            extractGroupEntities(normalized, query, entities)
            return ParsedCommand(AgentIntent.CREATE_GROUP, 0.9f, query, entities)
        }

        // 8. Add Payment
        if (containsAny(normalized, listOf("سجل دفعه", "سجل دفع", "دفع الطالب", "استلمت من", "دفع مصاريف", "سداد اشتراك", "دفع اشتراك", "اضف دفعه"))) {
            extractPaymentEntities(normalized, query, entities)
            return ParsedCommand(AgentIntent.ADD_PAYMENT, 0.9f, query, entities)
        }

        // 9. Generate Report
        if (containsAny(normalized, listOf("انشئ تقرير", "تقرير مالي", "تقرير حضور", "تقرير طالب", "كشف حساب", "اعمل تقرير", "تصدير تقرير", "تقرير ارباح", "تقرير المصروفات"))) {
            extractReportEntities(normalized, query, entities)
            return ParsedCommand(AgentIntent.GENERATE_REPORT, 0.9f, query, entities)
        }

        // 10. Student Risk/Analytics
        if (containsAny(normalized, listOf("تراجع", "المعرضين للتراجع", "مستوى الطلاب", "تحليل الطلاب", "تقييم الطلاب", "طلاب كسلانين", "الطلاب الضعاف", "ضعف مستوى", "تقييم ذكي"))) {
            return ParsedCommand(AgentIntent.STUDENT_ANALYTICS, 0.85f, query, entities)
        }

        // 11. Archive Student
        if (containsAny(normalized, listOf("ارشف", "ارشفت", "نقل للارشيف", "طالب مؤرشف", "ارشفه الطالب", "الغاء تنشيط الطالب"))) {
            extractStudentName(normalized, query, entities)
            return ParsedCommand(AgentIntent.ARCHIVE_STUDENT, 0.9f, query, entities)
        }

        // 12. Send Parent Report
        if (containsAny(normalized, listOf("ارسل تقرير لولي الامر", "ارسل تقرير", "ابعث تقرير الحضور", "تقرير ولي الامر", "ارسال الحضور لولي", "تقرير واتساب"))) {
            extractStudentName(normalized, query, entities)
            return ParsedCommand(AgentIntent.SEND_PARENT_REPORT, 0.85f, query, entities)
        }

        // 13. Create Assignment
        if (containsAny(normalized, listOf("اضف واجب", "واجب جديد", "انشئ واجب", "تسجيل واجب", "اضافه واجب"))) {
            extractAssignmentEntities(normalized, query, entities)
            return ParsedCommand(AgentIntent.CREATE_ASSIGNMENT, 0.9f, query, entities)
        }

        // 14. Add Exam Result
        if (containsAny(normalized, listOf("سجل درجه", "درجه الامتحان", "سجل علامه", "علامه الطالب", "درجه الطالب", "رصد درجات", "اضف درجه"))) {
            extractExamResultEntities(normalized, query, entities)
            return ParsedCommand(AgentIntent.ADD_EXAM_RESULT, 0.85f, query, entities)
        }

        // 15. Search Student
        if (containsAny(normalized, listOf("ابحث عن", "فين الطالب", "بحث عن", "ابحث الطالب", "دور على"))) {
            extractStudentName(normalized, query, entities)
            return ParsedCommand(AgentIntent.SEARCH_STUDENT, 0.9f, query, entities)
        }

        // 16. View Statistics / Financial Analytics
        if (containsAny(normalized, listOf("احصائيات", "ارقام السنتر", "كم اجمالي", "اجمالي الايرادات", "ارباح السنتر", "عرض الاحصائيات", "كم الدخل"))) {
            return ParsedCommand(AgentIntent.VIEW_STATISTICS, 0.8f, query, entities)
        }

        return ParsedCommand(AgentIntent.UNKNOWN, 0.0f, query)
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun extractStudentName(normalized: String, raw: String, entities: MutableMap<String, String>) {
        val studentNameRegex = "(اسم|اسمه|الطالب|طالب|احذف|عدل|ارشف|عن|للطالب|حساب)\\s+([\\u0600-\\u06FF]+(?:\\s+[\\u0600-\\u06FF]+){1,3})".toRegex()
        val match = studentNameRegex.find(raw)
        if (match != null) {
            entities["student_name"] = match.groupValues[2].trim()
        }
    }

    private fun extractAddStudentEntities(normalized: String, raw: String, entities: MutableMap<String, String>) {
        // Name extraction
        val nameRegex = "(اسم|اسمه|الجديد|طالب)\\s+([\\u0600-\\u06FF]+(?:\\s+[\\u0600-\\u06FF]+){1,3})".toRegex()
        val nameMatch = nameRegex.find(raw)
        if (nameMatch != null) {
            entities["student_name"] = nameMatch.groupValues[2].trim()
        }

        // Grade extraction
        val grades = listOf(
            "الاول الثانوي" to "الصف الأول الثانوي",
            "الثاني الثانوي" to "الصف الثاني الثانوي",
            "الثالث الثانوي" to "الصف الثالث الثانوي",
            "اول ثانوي" to "الصف الأول الثانوي",
            "ثاني ثانوي" to "الصف الثاني الثانوي",
            "ثالث ثانوي" to "الصف الثالث الثانوي",
            "الاول الاعدادي" to "الصف الأول الإعدادي",
            "الثاني الاعدادي" to "الصف الثاني الإعدادي",
            "الثالث الاعدادي" to "الصف الثالث الإعدادي"
        )
        for ((kw, standard) in grades) {
            if (normalized.contains(kw)) {
                entities["grade"] = standard
                break
            }
        }

        // Phone extraction
        val phoneRegex = "(?:01[0125][0-9]{8})".toRegex()
        val phones = phoneRegex.findAll(raw).map { it.value }.toList()
        if (phones.isNotEmpty()) {
            entities["student_phone"] = phones[0]
            if (phones.size > 1) {
                entities["parent_phone"] = phones[1]
            } else {
                entities["parent_phone"] = phones[0]
            }
        }
    }

    private fun extractStudentOrGroup(normalized: String, raw: String, entities: MutableMap<String, String>) {
        // Check if there is a group keyword
        if (normalized.contains("مجموعه") || normalized.contains("مجموعات")) {
            val groupRegex = "(مجموعه|مجموعه السبت|مجموعه الاحد|مجموعه الاثنين|مجموعه الثلاثاء|مجموعه الاربعاء|مجموعه الخميس|مجموعه الجمعه)\\s+([\\u0600-\\u06FF0-9]+)".toRegex()
            val groupMatch = groupRegex.find(raw)
            if (groupMatch != null) {
                entities["group_name"] = groupMatch.groupValues[2].trim()
                entities["target_type"] = "group"
                return
            }
        }

        // Otherwise assume student name
        extractStudentName(normalized, raw, entities)
        entities["target_type"] = "student"
    }

    private fun extractGroupEntities(normalized: String, raw: String, entities: MutableMap<String, String>) {
        val groupRegex = "(باسم|اسمها|مجموعه)\\s+([\\u0600-\\u06FF0-9]+(?:\\s+[\\u0600-\\u06FF0-9]+){0,2})".toRegex()
        val groupMatch = groupRegex.find(raw)
        if (groupMatch != null) {
            entities["group_name"] = groupMatch.groupValues[2].trim()
        }

        // Schedule / Classroom
        if (normalized.contains("قاعه") || normalized.contains("فصل")) {
            val roomRegex = "(قاعه|فصل)\\s+([0-9a-zA-Z\\u0600-\\u06FF]+)".toRegex()
            roomRegex.find(raw)?.let {
                entities["classroom"] = it.groupValues[2]
            }
        }
    }

    private fun extractPaymentEntities(normalized: String, raw: String, entities: MutableMap<String, String>) {
        // Name
        extractStudentName(normalized, raw, entities)

        // Amount
        val amountRegex = "\\b([0-9]+)\\s*(?:جنيها|جنيه|جم|ريال|دولار|درهم|L.E|le)?\\b".toRegex()
        val amountMatch = amountRegex.find(raw)
        if (amountMatch != null) {
            entities["amount"] = amountMatch.groupValues[1]
        }

        // Month
        val monthsMap = mapOf(
            "يناير" to "01", "فبراير" to "02", "مارس" to "03", "ابريل" to "04",
            "مايو" to "05", "يونيو" to "06", "يوليو" to "07", "اغسطس" to "08",
            "سبتمبر" to "09", "اكتوبر" to "10", "نوفمبر" to "11", "ديسمبر" to "12"
        )
        for ((name, code) in monthsMap) {
            if (raw.contains(name)) {
                val calendar = java.util.Calendar.getInstance()
                val year = calendar.get(java.util.Calendar.YEAR).toString()
                entities["month"] = "$year-$code"
                entities["month_name"] = name
                break
            }
        }
    }

    private fun extractReportEntities(normalized: String, raw: String, entities: MutableMap<String, String>) {
        if (normalized.contains("مالي") || normalized.contains("ارباح") || normalized.contains("ايرادات") || normalized.contains("مصروفات")) {
            entities["report_type"] = "financial"
        } else if (normalized.contains("حضور") || normalized.contains("غياب")) {
            entities["report_type"] = "attendance"
        } else if (normalized.contains("مجموعه") || normalized.contains("كشف المجموعه")) {
            entities["report_type"] = "group"
            val groupRegex = "(مجموعه)\\s+([\\u0600-\\u06FF0-9]+)".toRegex()
            groupRegex.find(raw)?.let {
                entities["group_name"] = it.groupValues[2]
            }
        } else {
            entities["report_type"] = "student"
            extractStudentName(normalized, raw, entities)
        }
    }

    private fun extractAssignmentEntities(normalized: String, raw: String, entities: MutableMap<String, String>) {
        val titleRegex = "(واجب|بعنوان|عنوانه|اسم)\\s+([\\u0600-\\u06FF0-9]+(?:\\s+[\\u0600-\\u06FF0-9]+){0,4})".toRegex()
        titleRegex.find(raw)?.let {
            entities["assignment_title"] = it.groupValues[2].trim()
        }

        val groupRegex = "(لمجموعه|مجموعه)\\s+([\\u0600-\\u06FF0-9]+)".toRegex()
        groupRegex.find(raw)?.let {
            entities["group_name"] = it.groupValues[2].trim()
        }
    }

    private fun extractExamResultEntities(normalized: String, raw: String, entities: MutableMap<String, String>) {
        extractStudentName(normalized, raw, entities)

        val scoreRegex = "([0-9]+)\\s*(?:من|على|/)?\\s*([0-9]+)?".toRegex()
        scoreRegex.find(raw)?.let {
            entities["score"] = it.groupValues[1]
            if (it.groupValues[2].isNotEmpty()) {
                entities["total_marks"] = it.groupValues[2]
            }
        }
    }
}
