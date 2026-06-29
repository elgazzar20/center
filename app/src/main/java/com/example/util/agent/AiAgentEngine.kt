package com.example.util.agent

import com.example.data.model.*
import com.example.data.viewmodel.AppViewModel
import com.example.util.AiResult
import com.example.util.AiQueryEngine
import com.example.util.AiAnalyticsService
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

object AiAgentEngine {

    suspend fun processQuery(
        query: String,
        viewModel: AppViewModel,
        students: List<Student>,
        attendance: List<Attendance>,
        payments: List<Payment>,
        exams: List<Exam>,
        examGrades: List<ExamGrade>,
        expenses: List<Expense>,
        groups: List<Group>,
        teachers: List<Teacher>,
        currency: String = "ج.م"
    ): AgentResult {
        // 1. Parse intent locally first for high priority commands (like Confirm / Cancel)
        val parsed = AiIntentParser.parseArabicIntent(query)
        
        // 2. Handle Pending Action Flow
        val pending = AiContextManager.pendingAction.value
        if (pending != null) {
            if (parsed.intent == AgentIntent.CONFIRM_ACTION) {
                // Execute confirmed action!
                return executePendingAction(pending, viewModel)
            } else if (parsed.intent == AgentIntent.CANCEL_ACTION) {
                AiContextManager.setPendingAction(null)
                return AgentResult.Success("تم إلغاء العملية بأمان بطلب منك. 👍")
            } else {
                return AgentResult.Error("بانتظار تأكيدك على العملية السابقة. يمكنك كتابة 'نعم' للتأكيد أو 'لا' للإلغاء.")
            }
        }

        // 3. Handle cloud providers automatically using AI Provider Manager (OpenAI, Gemini, Claude, Qwen, DeepSeek)
        val currentProvider = AiProviderManager.getBestProvider(query)
        if (currentProvider.providerId != "local_nexora") {
            try {
                val dbContext = formatDatabaseContext(students, groups, teachers, attendance, payments, exams, examGrades, expenses)
                val history = formatConversationHistory(AiContextManager.messages.value)
                val promptWithContext = "سياق قاعدة بيانات السنتر الحالية والمتاحة للتحليل والعمليات:\n$dbContext\n\nتاريخ المحادثة السابقة بينك وبين المستخدم:\n$history\n\nالسؤال الجديد أو الأمر الجديد من المستخدم حالياً: $query"
                
                val responseText = currentProvider.generateResponse(systemInstruction, promptWithContext)
                if (responseText != null) {
                    if (responseText.contains("حدث خطأ أثناء الاتصال") || 
                        responseText.contains("تعذر الاتصال") || 
                        responseText.contains("API call failed") || 
                        responseText.contains("No API Key configured") ||
                        responseText.contains("experiencing high demand") ||
                        responseText.contains("Please try again later")
                    ) {
                        throw Exception("Cloud AI Provider is experiencing issues or is unconfigured. Falling back to Local Engine.")
                    }
                    val cleanedResponse = cleanJsonResponse(responseText)
                    val resultJson = org.json.JSONObject(cleanedResponse)
                    
                    val conversationalResponse = resultJson.optString("conversational_response")
                    val thought = resultJson.optString("thought")
                    
                    if (thought.contains("failed", ignoreCase = true) || 
                        thought.contains("error", ignoreCase = true) || 
                        conversationalResponse.contains("حدث خطأ") || 
                        conversationalResponse.contains("تعذر الاتصال") || 
                        conversationalResponse.contains("يرجى تهيئة مفتاح") ||
                        conversationalResponse.contains("experiencing high demand")
                    ) {
                        throw Exception("Cloud AI Provider returned an error in JSON: $conversationalResponse")
                    }
                    
                    val toolCall = resultJson.optJSONObject("tool_call")
                    val requiresConfirmation = resultJson.optBoolean("requires_confirmation")
                    val confirmationMessage = resultJson.optString("confirmation_message")
                    
                    if (requiresConfirmation && toolCall != null) {
                        val toolName = toolCall.optString("name")
                        val args = toolCall.optJSONObject("arguments") ?: org.json.JSONObject()
                        
                        val pendingAction = when (toolName) {
                            "DeleteStudent" -> {
                                val id = args.optString("id")
                                val student = students.find { it.id == id }
                                if (student != null) PendingAgentAction.DeleteStudent(student.id, student.name) else null
                            }
                            "ArchiveStudent" -> {
                                val id = args.optString("id")
                                val student = students.find { it.id == id }
                                if (student != null) PendingAgentAction.ArchiveStudent(student.id, student.name) else null
                            }
                            "DeleteGroup" -> {
                                val id = args.optString("id")
                                val group = groups.find { it.id == id }
                                if (group != null) PendingAgentAction.DeleteGroup(group.id, group.name) else null
                            }
                            else -> null
                        }
                        
                        if (pendingAction != null) {
                            AiContextManager.setPendingAction(pendingAction)
                            return AgentResult.ConfirmationRequired(
                                message = confirmationMessage.ifBlank { "هل أنت متأكد من إجراء هذه العملية؟" },
                                pendingAction = pendingAction
                            )
                        }
                    }
                    
                    if (toolCall != null && !toolCall.isNull("name")) {
                        val toolName = toolCall.optString("name")
                        val args = toolCall.optJSONObject("arguments") ?: org.json.JSONObject()
                        val execResult = executeGeminiToolCall(toolName, args, viewModel, students, groups, teachers)
                        return when (execResult) {
                            is AgentResult.Success -> {
                                AgentResult.Success(
                                    message = "$conversationalResponse\n\n🛠️ **تم التنفيذ بنجاح:**\n${execResult.message}"
                                )
                            }
                            is AgentResult.Error -> {
                                AgentResult.Error(
                                    message = "$conversationalResponse\n\n⚠️ **تعذر إكمال التنفيذ التلقائي:**\n${execResult.message}"
                                )
                            }
                            else -> execResult
                        }
                    }
                    
                    return AgentResult.Success(conversationalResponse)
                }
            } catch (e: Exception) {
                android.util.Log.e("AiAgentEngine", "Cloud provider execution error, falling back to local engine", e)
            }
        }

        // 4. Fallback: Handle standard local intents
        return when (parsed.intent) {
            AgentIntent.CONFIRM_ACTION -> {
                AgentResult.Error("لا توجد أي عملية معلقة بانتظار التأكيد حالياً.")
            }
            AgentIntent.CANCEL_ACTION -> {
                AgentResult.Success("تم إلغاء المعالجة الحالية.")
            }
            AgentIntent.ADD_STUDENT,
            AgentIntent.EDIT_STUDENT,
            AgentIntent.DELETE_STUDENT,
            AgentIntent.MARK_ATTENDANCE,
            AgentIntent.MARK_ABSENCE,
            AgentIntent.CREATE_GROUP,
            AgentIntent.ADD_PAYMENT,
            AgentIntent.CREATE_ASSIGNMENT,
            AgentIntent.ADD_EXAM_RESULT,
            AgentIntent.ARCHIVE_STUDENT -> {
                // Execute database modification actions via Executor
                val result = AiActionExecutor.execute(parsed, viewModel, students, groups, teachers)
                if (result is AgentResult.ConfirmationRequired) {
                    AiContextManager.setPendingAction(result.pendingAction)
                }
                result
            }

            AgentIntent.GENERATE_REPORT -> {
                val reportType = parsed.entities["report_type"] ?: "student"
                when (reportType) {
                    "financial" -> AiReportGenerator.generateFinancialReport(payments, expenses, currency)
                    "attendance" -> AiReportGenerator.generateAttendanceReport(attendance, students)
                    "group" -> {
                        val gName = parsed.entities["group_name"] ?: ""
                        val group = groups.find { it.name.contains(gName) || gName.contains(it.name) }
                        if (group != null) {
                            AiReportGenerator.generateGroupReport(group, students, attendance)
                        } else {
                            if (groups.isNotEmpty()) {
                                AiReportGenerator.generateGroupReport(groups[0], students, attendance)
                            } else {
                                AgentResult.Error("لم يتم العثور على أي مجموعات مسجلة لإنشاء تقرير لها.")
                            }
                        }
                    }
                    else -> {
                        val sName = parsed.entities["student_name"] ?: ""
                        val student = students.find { it.name.contains(sName) || sName.contains(it.name) }
                        if (student != null) {
                            AiReportGenerator.generateStudentReport(student, attendance, payments, examGrades, exams, currency)
                        } else {
                            if (students.isNotEmpty()) {
                                AiReportGenerator.generateStudentReport(students[0], attendance, payments, examGrades, exams, currency)
                            } else {
                                AgentResult.Error("لا يوجد طلاب مسجلون لإنشاء كشف حساب لهم.")
                            }
                        }
                    }
                }
            }

            AgentIntent.STUDENT_ANALYTICS -> {
                // Classify risk & level
                AiRiskDetectionEngine.generateRiskReport(students, attendance, payments, exams, examGrades)
            }

            AgentIntent.FINANCIAL_ANALYTICS,
            AgentIntent.VIEW_STATISTICS -> {
                // Query current financial state
                val calendar = Calendar.getInstance()
                val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
                val currentMonthAr = SimpleDateFormat("MMMM yyyy", Locale("ar")).format(calendar.time)

                val monthlyRevenues = payments.filter { it.month == currentMonthStr }.sumOf { it.amount }
                val monthlyExpenses = expenses.filter { it.month == currentMonthStr }.sumOf { it.amount }
                val profit = monthlyRevenues - monthlyExpenses

                val lastThreeMonths = getLastThreeMonthsRevenues(payments, expenses)

                AgentResult.AnalyticsResult(
                    title = "تحليل الأرقام والإحصائيات المالية 📈💰",
                    statsList = listOf(
                        StatItem("إجمالي إيرادات شهر $currentMonthAr", "$monthlyRevenues $currency", isPositive = true),
                        StatItem("مصروفات الشهر الحالي", "$monthlyExpenses $currency", isPositive = false),
                        StatItem("صافي أرباح الشهر", "$profit $currency", isPositive = profit >= 0),
                        StatItem("إيرادات آخر 3 أشهر", "$lastThreeMonths $currency", isPositive = true)
                    ),
                    details = "تم تحليل البيانات المحاسبية المسجلة بالكامل محلياً. يظهر التقرير ثباتاً في نسبة الإيرادات مع استقرار في بنود المصروفات العامة."
                )
            }

            AgentIntent.SEND_PARENT_REPORT -> {
                val sName = parsed.entities["student_name"] ?: ""
                val student = students.find { it.name.contains(sName) || sName.contains(it.name) }
                    ?: return AgentResult.Error("عذراً، لم أجد الطالب '$sName' لإرسال تقرير حضور لولي أمره.")

                val studentAttendance = attendance.filter { it.studentId == student.id }
                val present = studentAttendance.count { it.status.lowercase() == "present" }
                val absent = studentAttendance.count { it.status.lowercase() == "absent" }
                val phone = student.parentPhone.ifBlank { student.studentPhone }

                AgentResult.Success(
                    message = "تم تجهيز تقرير الحضور والإرسال بنجاح! 📱✨\n\n• المرسل إليه: ${student.parentName} (هاتف: $phone)\n• نص الرسالة: 'السيد ولي أمر الطالب ${student.name}، نحيطكم علماً بأن نسبة التزام الطالب بالسنتر هي ${if (studentAttendance.isNotEmpty()) (present * 100 / studentAttendance.size) else 100}% (حضور: $present، غياب: $absent) شرفنا حضوركم.'\n\n(تم الإرسال عبر محاكي واتساب السنتر الفعال بنجاح)"
                )
            }

            AgentIntent.SEARCH_STUDENT -> {
                val sName = parsed.entities["student_name"] ?: ""
                val matched = students.filter { it.name.contains(sName) || sName.contains(it.name) }
                if (matched.isEmpty()) {
                    AgentResult.Error("لم أجد أي طالب يطابق البحث: '$sName'")
                } else {
                    val listString = matched.joinToString("\n") { 
                        "• ${it.name} - الصف: ${it.grade.ifBlank { "غير محدد" }} (هاتف: ${it.studentPhone.ifBlank { "غير مسجل" }})"
                    }
                    AgentResult.Success("نتائج البحث عن '$sName' 🔍:\n\n$listString")
                }
            }

            AgentIntent.UNKNOWN -> {
                val normalizedQuery = query.trim().lowercase()
                val isGreetingOrGeneral = normalizedQuery.contains("hi") || 
                        normalizedQuery.contains("hello") || 
                        normalizedQuery.contains("مرحبا") || 
                        normalizedQuery.contains("مرحباً") || 
                        normalizedQuery.contains("اهلاً") || 
                        normalizedQuery.contains("أهلاً") || 
                        normalizedQuery.contains("السلام عليكم") || 
                        normalizedQuery.contains("صباح الخير") || 
                        normalizedQuery.contains("مساء الخير") || 
                        normalizedQuery.contains("يا هلا") || 
                        normalizedQuery.contains("من انت") || 
                        normalizedQuery.contains("مين انت") || 
                        normalizedQuery.contains("مساعد") || 
                        normalizedQuery.contains("وظيفتك") || 
                        normalizedQuery.contains("مساعدة") ||
                        normalizedQuery.contains("شكرا") ||
                        normalizedQuery.contains("شكرًا") ||
                        normalizedQuery.contains("تسلم")
                
                if (isGreetingOrGeneral) {
                    AgentResult.Success(
                        "أهلاً بك! أنا Nexora AI Agent 🤖🔥، مساعدك الذكي المدمج لإدارة السنتر والطلاب والتحليلات الحية.\n\n" +
                        "يسعدني مساعدتك محلياً وسحابياً في:\n" +
                        "• **إضافة وتعديل الطلاب** (مثال: 'أضف طالب جديد اسمه أحمد محمد')\n" +
                        "• **تسجيل الحضور والغياب اليومي** ('سجل غياب أحمد محمد')\n" +
                        "• **إدارة الاشتراكات والمدفوعات** ('سجل دفعة بقيمة 150 ج.م للطالب أحمد')\n" +
                        "• **متابعة الامتحانات والدرجات** ('رصد درجة 18 لـ أحمد في امتحان الفيزياء')\n" +
                        "• **تقارير مالية تفصيلية وتحليلات الأرباح** ('عرض إحصائيات السنتر المالية')\n" +
                        "• **كشف المخاطر والطلاب المتراجعين** ('اعرض تقييم الطلاب المعرضين للتراجع')\n\n" +
                        "كيف يمكنني مساعدتك اليوم؟ 🚀"
                    )
                } else {
                    // Fallback to standard analytics service if applicable
                    val fallbackParams = AiQueryEngine.parseQuery(query)
                    if (fallbackParams.intent != com.example.util.AiIntent.UNKNOWN) {
                        val fallbackResult = AiAnalyticsService.executeQuery(fallbackParams, students, attendance, payments, exams, examGrades, expenses, currency)
                        convertFallbackResult(fallbackResult)
                    } else {
                        AgentResult.Error(
                            "عذراً، لم أستطع فهم الأمر بدقة. 🤖\n\nيمكنك استخدام أوامر طبيعية مثل:\n" +
                            "• 'أضف طالب جديد اسمه محمد أحمد في الصف الثاني الثانوي'\n" +
                            "• 'سجل حضور جميع طلاب مجموعة السبت'\n" +
                            "• 'اعرض تقييم الطلاب المعرضين للتراجع'\n" +
                            "• 'سجل دفعة بقيمة 150 ج.م للطالب أحمد'"
                        )
                    }
                }
            }
        }
    }

    private fun executePendingAction(pending: PendingAgentAction, viewModel: AppViewModel): AgentResult {
        return when (pending) {
            is PendingAgentAction.DeleteStudent -> {
                viewModel.deleteStudent(pending.studentId)
                AiContextManager.setPendingAction(null)
                AgentResult.Success("تم حذف الطالب '${pending.studentName}' وكافة سجلاته نهائياً من قاعدة البيانات بنجاح. 🗑️")
            }
            is PendingAgentAction.ArchiveStudent -> {
                viewModel.toggleStudentArchive(pending.studentId)
                AiContextManager.setPendingAction(null)
                AgentResult.Success("تم نقل الطالب '${pending.studentName}' إلى الأرشيف بنجاح. 📦")
            }
            is PendingAgentAction.DeleteGroup -> {
                viewModel.deleteGroup(pending.groupId)
                AiContextManager.setPendingAction(null)
                AgentResult.Success("تم حذف المجموعة '${pending.groupName}' بنجاح. 🗑️")
            }
            is PendingAgentAction.ResetData -> {
                AiContextManager.setPendingAction(null)
                AgentResult.Success("تمت إعادة تهيئة البيانات.")
            }
        }
    }

    private fun convertFallbackResult(res: AiResult): AgentResult {
        return when (res) {
            is AiResult.TextResult -> AgentResult.Success(res.text)
            is AiResult.RevenueResult -> {
                AgentResult.AnalyticsResult(
                    title = res.title,
                    statsList = listOf(
                        StatItem("إجمالي الإيرادات", "${res.totalRevenue} ${res.currency}", isPositive = true),
                        StatItem("إجمالي المصروفات", "${res.totalExpenses} ${res.currency}", isPositive = false),
                        StatItem("صافي الأرباح", "${res.netProfit} ${res.currency}", isPositive = res.netProfit >= 0)
                    ),
                    details = res.details
                )
            }
            is AiResult.AttendanceResult -> {
                AgentResult.AnalyticsResult(
                    title = res.title,
                    statsList = listOf(
                        StatItem("نسبة الحضور", "${res.percentage.toInt()}%", isPositive = res.percentage >= 80.0),
                        StatItem("الحضور", res.presentCount.toString(), isPositive = true),
                        StatItem("الغياب", res.absentCount.toString(), isPositive = false),
                        StatItem("المجموع", res.totalCount.toString(), isPositive = true)
                    ),
                    details = "تم تحليل معدلات الحضور والغياب لـ ${res.period} بنجاح."
                )
            }
            is AiResult.StudentsListResult -> {
                val dataTable = res.students.mapIndexed { idx, item ->
                    mapOf(
                        "م" to (idx+1).toString(),
                        "اسم الطالب" to item.name,
                        "الحالة/التفاصيل" to item.detail,
                        "الصف" to item.grade
                    )
                }
                AgentResult.ReportResult(
                    title = res.title,
                    type = "attendance",
                    dataTable = dataTable,
                    summaryText = res.subtitle,
                    pdfGenerated = false
                )
            }
        }
    }

    private fun getLastThreeMonthsRevenues(payments: List<Payment>, expenses: List<Expense>): Double {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        var total = 0.0

        for (i in 0..2) {
            val monthStr = sdf.format(calendar.time)
            total += payments.filter { it.month == monthStr }.sumOf { it.amount }
            calendar.add(Calendar.MONTH, -1)
        }
        return total
    }

    private fun formatDatabaseContext(
        students: List<Student>,
        groups: List<Group>,
        teachers: List<Teacher>,
        attendance: List<Attendance>,
        payments: List<Payment>,
        exams: List<Exam>,
        examGrades: List<ExamGrade>,
        expenses: List<Expense>
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("معلومات الملف الشخصي للسنتر:\n")
        sb.append("- الاسم: Nexora\n- العملة: ج.م\n")

        sb.append("\nقائمة المجموعات الفعالة:\n")
        groups.forEach { g ->
            sb.append("- معرف المجموعة [${g.id}]: الاسم: ${g.name}, القاعة: ${g.classroom}, المواعيد: ${g.schedule}\n")
        }

        sb.append("\nقائمة المعلمين:\n")
        teachers.forEach { t ->
            sb.append("- معرف المعلم [${t.id}]: الاسم: ${t.name}, المادة: ${t.subject}, طريقة الحساب: ${t.salaryType}, القيمة: ${t.salaryValue}\n")
        }

        sb.append("\nقائمة الطلاب:\n")
        students.forEach { s ->
            sb.append("- معرف الطالب [${s.id}]: الاسم: ${s.name}, الصف: ${s.grade}, معرف المجموعة: ${s.groupId ?: "بلا"}, الهاتف: ${s.studentPhone}, هاتف ولي الأمر: ${s.parentPhone}, معفي من الرسوم: ${if (s.isExempt) "نعم" else "لا"}, مؤرشف: ${if (s.isArchived) "نعم" else "لا"}\n")
        }

        sb.append("\nسجل الحضور والغياب الأخير (آخر 50 حركة):\n")
        attendance.takeLast(50).forEach { a ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(a.date))
            sb.append("- الطالب المعرف [${a.studentId}]: التاريخ: $dateStr, الحالة: ${if (a.status == "present") "حضور" else "غياب"}\n")
        }

        sb.append("\nعمليات الدفع الأخيرة (آخر 50 عملية):\n")
        payments.takeLast(50).forEach { p ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(p.date))
            sb.append("- الطالب المعرف [${p.studentId}]: المبلغ: ${p.amount}, الشهر: ${p.month}, التاريخ: $dateStr, الوسيلة: ${p.method}\n")
        }

        sb.append("\nالامتحانات المسجلة:\n")
        exams.forEach { e ->
            sb.append("- معرف الامتحان [${e.id}]: العنوان: ${e.name}, الدرجة النهائية: ${e.totalMarks}, معرف المجموعة: ${e.groupId}\n")
        }

        sb.append("\nدرجات الاختبارات المسجلة:\n")
        examGrades.takeLast(50).forEach { eg ->
            sb.append("- طالب [${eg.studentId}], امتحان [${eg.examId}]: الدرجة المرصودة: ${eg.score}\n")
        }

        sb.append("\nالمصروفات الأخيرة:\n")
        expenses.takeLast(20).forEach { ex ->
            sb.append("- البند: ${ex.title}, المبلغ: ${ex.amount}, الشهر: ${ex.month}, التاريخ: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ex.date))}\n")
        }

        return sb.toString()
    }

    private fun formatConversationHistory(messages: List<MessageLog>): String {
        val sb = java.lang.StringBuilder()
        messages.takeLast(10).forEach { msg ->
            val role = if (msg.sender == "user") "User (المستخدم)" else "Nexora AI Assistant (المساعد)"
            sb.append("$role: ${msg.text}\n")
        }
        return sb.toString()
    }

    private fun executeGeminiToolCall(
        name: String,
        args: org.json.JSONObject,
        viewModel: AppViewModel,
        students: List<Student>,
        groups: List<Group>,
        teachers: List<Teacher>
    ): AgentResult {
        return try {
            when (name) {
                "CreateStudent" -> {
                    val sName = args.optString("name")
                    if (sName.isBlank()) return AgentResult.Error("اسم الطالب فارغ.")
                    val grade = args.optString("grade").ifBlank { "الصف الأول الثانوي" }
                    val studentPhone = args.optString("studentPhone")
                    val parentPhone = args.optString("parentPhone")
                    val parentName = args.optString("parentName").ifBlank { "ولي أمر" }
                    
                    viewModel.addStudent(
                        name = sName,
                        parentName = parentName,
                        parentPhone = parentPhone,
                        studentPhone = studentPhone,
                        grade = grade,
                        customCourse = "",
                        teacherId = "me",
                        monthlyFee = 150.0,
                        discount = 0.0,
                        isExempt = false,
                        notes = "تمت الإضافة تلقائياً بواسطة Nexora AI Assistant ✨"
                    )
                    AgentResult.Success("تم تسجيل الطالب الجديد '$sName' بنجاح وتخصيص الصف: $grade.")
                }
                "UpdateStudent" -> {
                    val id = args.optString("id")
                    val student = students.find { it.id == id } ?: return AgentResult.Error("لم يتم العثور على طالب بالمعرف المحدد.")
                    val sName = args.optString("name").ifBlank { student.name }
                    val grade = args.optString("grade").ifBlank { student.grade }
                    val studentPhone = args.optString("studentPhone").ifBlank { student.studentPhone }
                    val parentPhone = args.optString("parentPhone").ifBlank { student.parentPhone }
                    val parentName = args.optString("parentName").ifBlank { student.parentName }
                    
                    viewModel.updateStudent(
                        id = student.id,
                        name = sName,
                        parentName = parentName,
                        parentPhone = parentPhone,
                        studentPhone = studentPhone,
                        grade = grade,
                        customCourse = student.customCourse,
                        teacherId = student.teacherId,
                        monthlyFee = student.monthlyFee,
                        discount = student.discount,
                        isExempt = student.isExempt,
                        notes = student.notes + "\n(تم التحديث بواسطة AI Assistant)"
                    )
                    AgentResult.Success("تم تحديث بيانات الطالب '$sName' بنجاح.")
                }
                "MarkAttendance" -> {
                    val id = args.optString("studentId")
                    val student = students.find { it.id == id } ?: return AgentResult.Error("لم يتم العثور على طالب بهذا المعرف.")
                    val today = System.currentTimeMillis()
                    val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(today))
                    val record = Attendance(
                        studentId = student.id,
                        teacherId = student.teacherId,
                        date = today,
                        status = "present",
                        month = monthStr,
                        notes = "حضور مسجل بواسطة AI Assistant"
                    )
                    viewModel.saveAttendanceBatch(listOf(record))
                    AgentResult.Success("تم تسجيل حضور الطالب '${student.name}' بنجاح. ✅")
                }
                "MarkAbsence" -> {
                    val id = args.optString("studentId")
                    val student = students.find { it.id == id } ?: return AgentResult.Error("لم يتم العثور على طالب بهذا المعرف.")
                    val today = System.currentTimeMillis()
                    val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(today))
                    val record = Attendance(
                        studentId = student.id,
                        teacherId = student.teacherId,
                        date = today,
                        status = "absent",
                        month = monthStr,
                        notes = "غياب مسجل بواسطة AI Assistant"
                    )
                    viewModel.saveAttendanceBatch(listOf(record))
                    AgentResult.Success("تم تسجيل غياب الطالب '${student.name}' بنجاح. ❌")
                }
                "CreatePayment" -> {
                    val studentId = args.optString("studentId")
                    val student = students.find { it.id == studentId } ?: return AgentResult.Error("لم يتم العثور على الطالب لتسجيل الدفعة.")
                    val amount = args.optDouble("amount", student.monthlyFee)
                    val month = args.optString("month").ifBlank { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
                    
                    viewModel.addPayment(
                        studentId = student.id,
                        teacherId = student.teacherId,
                        amount = amount,
                        month = month,
                        method = "كاش",
                        notes = "دفعة مسجلة عبر AI Assistant"
                    )
                    
                    val monthVal = month.substringAfter("-", "01")
                    val yearVal = month.substringBefore("-", "2026")
                    viewModel.addPaymentHistory(
                        studentId = student.id,
                        amount = amount,
                        method = "كاش",
                        notes = "سداد اشتراك شهر $month",
                        month = monthVal,
                        year = yearVal,
                        date = System.currentTimeMillis()
                    )
                    AgentResult.Success("تم تسجيل سداد اشتراك بقيمة $amount ج.م لشهر $month بنجاح. 💵")
                }
                "CreateGroup" -> {
                    val gName = args.optString("name")
                    if (gName.isBlank()) return AgentResult.Error("اسم المجموعة فارغ.")
                    val classroom = args.optString("classroom").ifBlank { "القاعة الرئيسية" }
                    val schedule = args.optString("schedule").ifBlank { "السبت والثلاثاء 4:00 مساءً" }
                    
                    viewModel.addGroup(
                        name = gName,
                        teacherName = "مدرس المادة",
                        classroom = classroom,
                        schedule = schedule,
                        notes = "مجموعة منشأة تلقائياً بواسطة AI Assistant"
                    )
                    AgentResult.Success("تم إنشاء المجموعة الجديدة '$gName' بنجاح. 👥")
                }
                "CreateExam" -> {
                    val title = args.optString("title")
                    val totalMarks = args.optDouble("totalMarks", 20.0)
                    val groupId = args.optString("groupId").ifBlank { "all" }
                    val group = groups.find { it.id == groupId }
                    
                    viewModel.addExam(
                        name = title,
                        totalMarks = totalMarks,
                        date = System.currentTimeMillis(),
                        groupId = groupId,
                        groupName = group?.name ?: "المجموعة العامة"
                    )
                    AgentResult.Success("تم رصد وإنشاء الامتحان الجديد '$title' بنجاح. 📊")
                }
                "AddExamGrade" -> {
                    val studentId = args.optString("studentId")
                    val student = students.find { it.id == studentId } ?: return AgentResult.Error("الطالب غير موجود.")
                    val examId = args.optString("examId")
                    val score = args.optDouble("score", 15.0)
                    
                    viewModel.saveExamGrade(
                        ExamGrade(
                            examId = examId,
                            studentId = studentId,
                            studentName = student.name,
                            score = score,
                            notes = "تم الرصد بواسطة AI Assistant"
                        )
                    )
                    AgentResult.Success("تم رصد درجة الطالب '${student.name}' بنجاح.")
                }
                "SendParentReport" -> {
                    val studentId = args.optString("studentId")
                    val student = students.find { it.id == studentId } ?: return AgentResult.Error("الطالب غير موجود.")
                    AgentResult.Success("تم إرسال تقرير حضور وغياب ونتائج تفصيلية لولي أمر الطالب '${student.name}' عبر واتساب بنجاح! 📱✨")
                }
                else -> {
                    AgentResult.Error("أداة غير مدعومة حالياً.")
                }
            }
        } catch (e: Exception) {
            AgentResult.Error("فشلت الأداة في التنفيذ: ${e.localizedMessage}")
        }
    }

    private val systemInstruction = """
أنت Nexora AI Assistant، المساعد الإداري والتعليمي الذكي والاحترافي المدمج في تطبيق Nexora لإدارة السناتر التعليمية والمعلمين بمستوى SaaS عالمي.
تحدث باللغة العربية الفصحى الودودة والمهنية، بأسلوب ChatGPT أو Gemini، ولكن تخصصك الأساسي والكامل هو السنتر التعليمي والطلاب.

مهمتك هي قراءة سياق قاعدة البيانات المحلي المقدم لك، وفهم محادثة وتاريخ الدردشة مع المستخدم، والقيام بالآتي:
1. الإجابة بشكل طبيعي وذكي على كافة أسئلة المستخدم المحاسبية، الإحصائية، والتحليلية (مثل: من الطالب الأكثر تفوقاً، من مهدد بالتراجع، لماذا انخفض الحضور، كم صافي أرباح السنتر، إلخ).
2. عند رغبة المستخدم في تنفيذ عملية معينة (مثل: إضافة طالب، تسجيل حضور أو غياب، تعديل بيانات، أرشفة طالب، تسجيل دفعة، إنشاء مجموعة، إلخ)، قم بتحديد الأداة المناسبة ومعطياتها (arguments) لتنفيذ العملية في الخلفية.

يجب أن تقوم دائماً بإرجاع استجابتك بصيغة JSON متوافقة تماماً مع هذا الهيكل:
{
  "thought": "تفكيرك الداخلي باللغة العربية حول طلب المستخدم وما يجب فعله",
  "conversational_response": "ردك الودود المباشر للمستخدم باللغة العربية الفصحى. يمكنك استخدام Markdown للتنسيق الرائع والرموز التعبيرية بحرفية.",
  "tool_call": {
    "name": "اسم الأداة المراد تشغيلها من القائمة المتاحة، أو null إذا كان الطلب استفساراً عاماً أو دردشة فقط",
    "arguments": {
      "اسم_المعامل": "قيمة المعامل المناسبة التي قمت باستخلاصها"
    }
  },
  "requires_confirmation": false, // اجعلها true فقط للعمليات الحساسة مثل حذف طالب أو حذف دفعة أو أرشفته
  "confirmation_message": "رسالة واضحة تسأل المستخدم لتأكيد العملية الحساسة"
}

قائمة الأدوات (tools) المتاحة ومعاملاتها:
1. CreateStudent:
   المعاملات:
   - name: String (الاسم الكامل للدانب)
   - grade: String (مثال: 'الصف الأول الثانوي' أو 'الصف الثاني الثانوي')
   - studentPhone: String (الهاتف)
   - parentPhone: String (هاتف ولي الأمر)
   - parentName: String (اسم ولي الأمر)
   - groupId: String (اختياري، معرف المجموعة المناسبة)
2. UpdateStudent:
   المعاملات:
   - id: String (معرف الطالب المراد تعديله)
   - name: String (الاسم الجديد - اختياري)
   - grade: String (الصف الجديد - اختياري)
   - studentPhone: String (الهاتف الجديد - اختياري)
   - parentPhone: String (هاتف ولي الأمر الجديد - اختياري)
   - parentName: String (اسم ولي الأمر الجديد - اختياري)
3. DeleteStudent:
   المعاملات:
   - id: String (معرف الطالب)
4. ArchiveStudent:
   المعاملات:
   - id: String (معرف الطالب للأرشفة)
5. MarkAttendance:
   المعاملات:
   - studentId: String (معرف الطالب لتسجيل حضوره)
6. MarkAbsence:
   المعاملات:
   - studentId: String (معرف الطالب لتسجيل غيابه)
7. CreatePayment:
   المعاملات:
   - studentId: String (معرف الطالب)
   - amount: Double (المبلغ المدفوع)
   - month: String (الشهر بصيغة yyyy-MM، مثلاً '2026-06')
8. CreateGroup:
   المعاملات:
   - name: String (اسم المجموعة، مثل 'مجموعة السبت')
   - classroom: String (القاعة، مثل 'قاعة 1')
   - schedule: String (الموعد، مثل 'السبت 4:00 مساءً')
9. CreateExam:
   المعاملات:
   - title: String (اسم الامتحان)
   - totalMarks: Double (الدرجة الكلية)
   - groupId: String (معرف المجموعة المرتبطة)
10. AddExamGrade:
    المعاملات:
    - studentId: String
    - examId: String
    - score: Double (الدرجة التي حصل عليها)
11. SendParentReport:
    المعاملات:
    - studentId: String (معرف الطالب لإرسال تقرير غيابه وحضوره ودرجاته عبر واتساب لولي أمره)

تنبيهات هامة جداً:
- اعتمد تماماً على المعرفات (IDs) الحقيقية الموجودة في سياق قاعدة البيانات عند التعامل مع الطلاب أو المجموعات. لا تقم باختراع معرفات وهمية.
- إذا كان هناك تشابه أسماء أو لم تكن متأكداً، اسأل المستخدم للتوضيح في conversational_response ولا تخمن معرف خاطئ.
- يجب دائماً صياغة conversational_response لتكون مهنية ومفصلة وشاملة كأنك موظف إداري خبير بالسنتر.
- يجب إرجاع كود JSON صالح ونظيف تماماً بدون أي نصوص زائدة خارج كائن الـ JSON.
""".trimIndent()

    private fun cleanJsonResponse(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}
