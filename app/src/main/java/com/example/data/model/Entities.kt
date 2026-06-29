package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val phone: String = "",
    val systemType: String = "not_chosen", // "center" or "teacher" or "not_chosen"
    val centerName: String = "Nexora",
    val whatsappNumber: String = "",
    val currency: String = "ج.م",
    val linkedStudentIds: String = "", // Comma separated list of student IDs
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val subject: String = "",
    val phone: String = "",
    val salaryType: String = "none", // "fixed", "percentage", "none"
    val salaryValue: Double = 0.0,
    val isActive: Boolean = true,
    val notes: String = "",
    val stages: String = "",
    val joinDate: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val parentName: String = "",
    val parentPhone: String = "",
    val studentPhone: String = "",
    val grade: String = "",
    val customCourse: String = "",
    val teacherId: String = "", // "me" if single teacher, or teacher UUID
    val monthlyFee: Double = 0.0,
    val discount: Double = 0.0, // percentage discount, e.g. 10.0 for 10%
    val isExempt: Boolean = false,
    val isActive: Boolean = true,
    val notes: String = "",
    val registrationDate: Long = System.currentTimeMillis(),
    val groupId: String? = null,
    val qrCode: String = "",
    val parentCode: String = "",
    val isArchived: Boolean = false,
    val studentType: String = "GROUP", // "GROUP" or "PRIVATE"
    val privateSessionsCount: Int = 0,
    val privateSessionPrice: Double = 0.0,
    val privateTotalAmount: Double = 0.0,
    val centerId: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val netFee: Double
        get() = if (studentType == "PRIVATE") privateTotalAmount else if (isExempt) 0.0 else monthlyFee * (1.0 - (discount / 100.0))

    fun getActualNetFee(crossRefs: List<StudentTeacherCrossRef>): Double {
        if (isExempt) return 0.0
        if (studentType == "PRIVATE") return privateTotalAmount
        val studentCrossRefs = crossRefs.filter { it.studentId == id }
        val baseFee = if (studentCrossRefs.isNotEmpty()) {
            studentCrossRefs.sumOf { it.customFee }
        } else {
            monthlyFee
        }
        return baseFee * (1.0 - (discount / 100.0))
    }
}

@Entity(tableName = "student_notes")
data class StudentNote(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "المشرف"
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String = "",
    val teacherId: String = "",
    val date: Long = 0L, // timestamp
    val status: String = "absent", // "present", "absent", "late", "excused"
    val notes: String = "",
    val whatsappSent: Boolean = false,
    val month: String = "", // "YYYY-MM" for grouping
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String = "",
    val teacherId: String = "",
    val amount: Double = 0.0,
    val month: String = "", // "YYYY-MM"
    val date: Long = System.currentTimeMillis(),
    val method: String = "cash", // "cash", "transfer", "other"
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val amount: Double,
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val description: String = title,
    val isMonthly: Boolean = false,
    val month: String = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date(date)),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_history")
data class PaymentHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: String = "كاش", // e.g. "كاش", "فودافون كاش", "فيزا"
    val notes: String = "",
    val month: String, // e.g., "06" or "يونيو"
    val year: String, // e.g., "2026"
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val teacherName: String = "",
    val classroom: String = "",
    val schedule: String = "",
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val dueDate: Long = 0L,
    val groupId: String = "",
    val groupName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val totalMarks: Double = 0.0,
    val date: Long = 0L,
    val groupId: String = "",
    val groupName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_grades")
data class ExamGrade(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val examId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val score: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val action: String,
    val userId: String,
    val userName: String = "", // Name of the user who performed the action
    val targetId: String,
    val details: String = "", // Extra details about what was modified
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "staff")
data class Staff(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String = "",
    val role: String = "TEACHER", // "OWNER", "SECRETARY", "TEACHER"
    val permissions: String = "", // Comma separated like: "ADD_STUDENT,EDIT_STUDENT,RECORD_ATTENDANCE,ADD_PAYMENTS,VIEW_REPORTS,MANAGE_TEACHERS,MANAGE_FINANCES"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "message_templates")
data class MessageTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val channel: String, // "WhatsApp", "SMS", "Email", "Notification"
    val category: String, // "attendance", "fees", "exam", "homework", "custom"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "communication_logs")
data class CommunicationLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String = "",
    val studentName: String = "",
    val recipient: String,
    val channel: String, // "WhatsApp", "SMS", "Email", "Notification"
    val message: String,
    val status: String = "SUCCESS", // "SUCCESS", "FAILED"
    val sentAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "schedule_events")
data class ScheduleEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val type: String, // "class", "revision", "exam", "private"
    val startTime: Long,
    val endTime: Long,
    val relatedId: String = "", // e.g., groupId or studentId
    val teacherId: String = "",
    val classroomId: String = "",
    val color: Int = 0,
    val reminderTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classrooms")
data class Classroom(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val capacity: Int,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "student_teacher_cross_ref", primaryKeys = ["studentId", "teacherId"])
data class StudentTeacherCrossRef(
    val studentId: String = "",
    val teacherId: String = "",
    val customFee: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "parents")
data class Parent(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "parent_student_links", primaryKeys = ["parentId", "studentId"])
data class ParentStudentLink(
    val parentId: String = "",
    val studentId: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)



