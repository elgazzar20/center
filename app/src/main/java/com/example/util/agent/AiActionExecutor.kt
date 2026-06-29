package com.example.util.agent

import com.example.data.model.*
import com.example.data.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

object AiActionExecutor {

    fun execute(
        parsed: ParsedCommand,
        viewModel: AppViewModel,
        students: List<Student>,
        groups: List<Group>,
        teachers: List<Teacher>
    ): AgentResult {
        val entities = parsed.entities

        return when (parsed.intent) {
            AgentIntent.ADD_STUDENT -> {
                val name = entities["student_name"] ?: return AgentResult.Error("الرجاء تحديد اسم الطالب.")
                val grade = entities["grade"] ?: "الصف الأول الثانوي"
                val phone = entities["student_phone"] ?: ""
                val parentPhone = entities["parent_phone"] ?: ""
                val parentName = entities["parent_name"] ?: "ولي أمر الطالب"

                // Add student via ViewModel
                viewModel.addStudent(
                    name = name,
                    parentName = parentName,
                    parentPhone = parentPhone,
                    studentPhone = phone,
                    grade = grade,
                    customCourse = "",
                    teacherId = "me",
                    monthlyFee = 150.0,
                    discount = 0.0,
                    isExempt = false,
                    notes = "تمت الإضافة تلقائياً بواسطة Nexora AI Agent"
                )

                AgentResult.Success(
                    message = "تمت إضافة الطالب الجديد بنجاح! 🎉\n• الاسم: $name\n• الصف: $grade\n• هاتف الطالب: ${phone.ifBlank { "غير مسجل" }}\n• هاتف ولي الأمر: ${parentPhone.ifBlank { "غير مسجل" }}"
                )
            }

            AgentIntent.EDIT_STUDENT -> {
                val nameQuery = entities["student_name"] ?: return AgentResult.Error("لم يتم العثور على اسم الطالب المراد تعديله.")
                val student = students.find { it.name.contains(nameQuery) || nameQuery.contains(it.name) }
                    ?: return AgentResult.Error("عذراً، لم أجد طالباً باسم '$nameQuery' في قاعدة البيانات لتعديله.")

                val updatedName = entities["student_name_new"] ?: student.name
                val updatedGrade = entities["grade"] ?: student.grade
                val updatedPhone = entities["student_phone"] ?: student.studentPhone
                val updatedParentPhone = entities["parent_phone"] ?: student.parentPhone

                viewModel.updateStudent(
                    id = student.id,
                    name = updatedName,
                    parentName = student.parentName,
                    parentPhone = updatedParentPhone,
                    studentPhone = updatedPhone,
                    grade = updatedGrade,
                    customCourse = student.customCourse,
                    teacherId = student.teacherId,
                    monthlyFee = student.monthlyFee,
                    discount = student.discount,
                    isExempt = student.isExempt,
                    notes = student.notes + "\n(تم تعديل البيانات بواسطة AI Agent)"
                )

                AgentResult.Success("تم تحديث بيانات الطالب '${student.name}' بنجاح! ✏️")
            }

            AgentIntent.DELETE_STUDENT -> {
                val nameQuery = entities["student_name"] ?: return AgentResult.Error("يرجى تحديد اسم الطالب لحذفه.")
                val student = students.find { it.name.contains(nameQuery) || nameQuery.contains(it.name) }
                    ?: return AgentResult.Error("عذراً، لم يتم العثور على طالب باسم '$nameQuery'.")

                // Return confirmation requirement for safety!
                AgentResult.ConfirmationRequired(
                    message = "هل أنت متأكد من حذف الطالب '${student.name}'؟ لا يمكن التراجع عن هذه الخطوة وسيتم مسح كافة سجلات حضوره ودفعاته.",
                    pendingAction = PendingAgentAction.DeleteStudent(student.id, student.name)
                )
            }

            AgentIntent.MARK_ATTENDANCE -> {
                val targetType = entities["target_type"] ?: "student"
                if (targetType == "group") {
                    val groupName = entities["group_name"] ?: return AgentResult.Error("يرجى تحديد اسم المجموعة.")
                    val group = groups.find { it.name.contains(groupName) || groupName.contains(it.name) }
                        ?: return AgentResult.Error("لم يتم العثور على مجموعة باسم '$groupName'.")

                    val groupStudents = students.filter { it.groupId == group.id || it.customCourse.contains(group.name) }
                    if (groupStudents.isEmpty()) {
                        return AgentResult.Error("مجموعة '$groupName' فارغة حالياً ولا تحتوي على أي طلاب.")
                    }

                    val today = System.currentTimeMillis()
                    val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(today))
                    val attendanceRecords = groupStudents.map { student ->
                        Attendance(
                            studentId = student.id,
                            teacherId = student.teacherId,
                            date = today,
                            status = "present",
                            month = monthStr,
                            notes = "حضور جماعي للمجموعة بواسطة AI Agent"
                        )
                    }
                    viewModel.saveAttendanceBatch(attendanceRecords)
                    AgentResult.Success("تم تسجيل حضور جميع طلاب مجموعة '$groupName' بنجاح! ✅ (عدد الطلاب: ${groupStudents.size})")
                } else {
                    val nameQuery = entities["student_name"] ?: return AgentResult.Error("يرجى تحديد اسم الطالب لتسجيل حضوره.")
                    val student = students.find { it.name.contains(nameQuery) || nameQuery.contains(it.name) }
                        ?: return AgentResult.Error("لم أجد طالباً باسم '$nameQuery' لتسجيل حضوره.")

                    val today = System.currentTimeMillis()
                    val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(today))
                    val record = Attendance(
                        studentId = student.id,
                        teacherId = student.teacherId,
                        date = today,
                        status = "present",
                        month = monthStr,
                        notes = "حضور مسجل بواسطة AI Agent"
                    )
                    viewModel.saveAttendanceBatch(listOf(record))
                    AgentResult.Success("تم تسجيل حضور الطالب '${student.name}' بنجاح! ✅")
                }
            }

            AgentIntent.MARK_ABSENCE -> {
                val targetType = entities["target_type"] ?: "student"
                if (targetType == "group") {
                    val groupName = entities["group_name"] ?: return AgentResult.Error("يرجى تحديد اسم المجموعة لتسجيل غيابهم.")
                    val group = groups.find { it.name.contains(groupName) || groupName.contains(it.name) }
                        ?: return AgentResult.Error("لم يتم العثور على مجموعة باسم '$groupName'.")

                    val groupStudents = students.filter { it.groupId == group.id || it.customCourse.contains(group.name) }
                    if (groupStudents.isEmpty()) {
                        return AgentResult.Error("مجموعة '$groupName' لا تحتوي على طلاب.")
                    }

                    val today = System.currentTimeMillis()
                    val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(today))
                    val attendanceRecords = groupStudents.map { student ->
                        Attendance(
                            studentId = student.id,
                            teacherId = student.teacherId,
                            date = today,
                            status = "absent",
                            month = monthStr,
                            notes = "غياب جماعي للمجموعة بواسطة AI Agent"
                        )
                    }
                    viewModel.saveAttendanceBatch(attendanceRecords)
                    AgentResult.Success("تم تسجيل غياب جميع طلاب مجموعة '$groupName' بنجاح! ❌ (عدد الطلاب: ${groupStudents.size})")
                } else {
                    val nameQuery = entities["student_name"] ?: return AgentResult.Error("يرجى تحديد اسم الطالب لتسجيل غيابه.")
                    val student = students.find { it.name.contains(nameQuery) || nameQuery.contains(it.name) }
                        ?: return AgentResult.Error("عذراً، لم أجد طالباً باسم '$nameQuery' لتسجيل غيابه.")

                    val today = System.currentTimeMillis()
                    val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(today))
                    val record = Attendance(
                        studentId = student.id,
                        teacherId = student.teacherId,
                        date = today,
                        status = "absent",
                        month = monthStr,
                        notes = "غياب مسجل بواسطة AI Agent"
                    )
                    viewModel.saveAttendanceBatch(listOf(record))
                    AgentResult.Success("تم تسجيل غياب الطالب '${student.name}' بنجاح! ❌ (وإرسال إشعار لولي أمره تلقائياً)")
                }
            }

            AgentIntent.CREATE_GROUP -> {
                val groupName = entities["group_name"] ?: return AgentResult.Error("يرجى تحديد اسم المجموعة المراد إنشاؤها.")
                viewModel.addGroup(
                    name = groupName,
                    teacherName = "مدرس المادة",
                    classroom = entities["classroom"] ?: "القاعة الرئيسية",
                    schedule = "السبت والثلاثاء 4:00 مساءً",
                    notes = "مجموعة منشأة تلقائياً بواسطة AI Agent"
                )
                AgentResult.Success("تم إنشاء المجموعة الجديدة [ $groupName ] بنجاح! 👥✨")
            }

            AgentIntent.ADD_PAYMENT -> {
                val nameQuery = entities["student_name"] ?: return AgentResult.Error("يرجى تحديد اسم الطالب لتسجيل الدفعة له.")
                val student = students.find { it.name.contains(nameQuery) || nameQuery.contains(it.name) }
                    ?: return AgentResult.Error("عذراً، لم أجد طالباً مسجلاً باسم '$nameQuery' لتسجيل دفعة مالية له.")

                val amount = entities["amount"]?.toDoubleOrNull() ?: student.netFee
                val calendar = Calendar.getInstance()
                val monthCode = entities["month"] ?: SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
                val monthName = entities["month_name"] ?: "هذا الشهر"

                // Save Payment Record
                viewModel.addPayment(
                    studentId = student.id,
                    teacherId = student.teacherId,
                    amount = amount,
                    month = monthCode,
                    method = "كاش",
                    notes = "دفعة مسجلة عبر AI Agent"
                )

                // Log into payment accounting database (PaymentHistory)
                val monthVal = monthCode.substringAfter("-")
                val yearVal = monthCode.substringBefore("-")
                viewModel.addPaymentHistory(
                    studentId = student.id,
                    amount = amount,
                    method = "كاش",
                    notes = "سداد اشتراك شهر $monthName",
                    month = monthVal,
                    year = yearVal,
                    date = System.currentTimeMillis()
                )

                AgentResult.Success("تم تسجيل سداد اشتراك الطالب '${student.name}' بمبلغ $amount ج.م لشهر $monthName بنجاح! 💵💰")
            }

            AgentIntent.CREATE_ASSIGNMENT -> {
                val title = entities["assignment_title"] ?: "واجب منزلي جديد"
                val groupName = entities["group_name"] ?: "السبت"
                val group = groups.find { it.name.contains(groupName) || groupName.contains(it.name) }
                val groupId = group?.id ?: "all"
                
                viewModel.addAssignment(
                    title = title,
                    description = "يرجى حل الأسئلة المحددة في مذكرة المادة والالتزام بالموعد المحدد.",
                    dueDate = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L, // 3 days from now
                    groupId = groupId,
                    groupName = group?.name ?: groupName,
                    notes = "تم الإنشاء عبر AI Agent"
                )

                AgentResult.Success("تم إنشاء الواجب الجديد [ $title ] لمجموعة '$groupName' بنجاح! 📝")
            }

            AgentIntent.ADD_EXAM_RESULT -> {
                val nameQuery = entities["student_name"] ?: return AgentResult.Error("يرجى تحديد اسم الطالب لرصد درجته.")
                val student = students.find { it.name.contains(nameQuery) || nameQuery.contains(it.name) }
                    ?: return AgentResult.Error("لم أجد طالباً مسجلاً باسم '$nameQuery'.")

                val score = entities["score"]?.toDoubleOrNull() ?: 18.0
                val totalMarks = entities["total_marks"]?.toDoubleOrNull() ?: 20.0

                // Create a temporary exam to hold the score
                viewModel.addExam(
                    name = "اختبار دوري - رصد ذكي",
                    totalMarks = totalMarks,
                    date = System.currentTimeMillis(),
                    groupId = student.groupId ?: "all",
                    groupName = "المجموعة العامة",
                    onExamAdded = { exam ->
                        viewModel.saveExamGrade(
                            ExamGrade(
                                examId = exam.id,
                                studentId = student.id,
                                studentName = student.name,
                                score = score,
                                notes = "رصد آلي"
                            )
                        )
                    }
                )

                AgentResult.Success("تم رصد وتسجيل درجة الطالب '${student.name}' بنجاح! 📊\n• الدرجة: $score من $totalMarks\n• التقييم: ${((score/totalMarks)*100).toInt()}%")
            }

            AgentIntent.ARCHIVE_STUDENT -> {
                val nameQuery = entities["student_name"] ?: return AgentResult.Error("يرجى تحديد اسم الطالب لأرشفته.")
                val student = students.find { it.name.contains(nameQuery) || nameQuery.contains(it.name) }
                    ?: return AgentResult.Error("لم يتم العثور على طالب باسم '$nameQuery'.")

                // Confirmation flow
                AgentResult.ConfirmationRequired(
                    message = "هل أنت متأكد من أرشفة الطالب '${student.name}'؟ سيتم إخفاؤه من الكشوفات الفعالة والحضور النشط ويمكنك فك الأرشفة لاحقاً.",
                    pendingAction = PendingAgentAction.ArchiveStudent(student.id, student.name)
                )
            }

            else -> {
                AgentResult.Error("عذراً، لا يمكنني تنفيذ هذا الأمر مباشرة حالياً.")
            }
        }
    }
}
