package com.example.data.model

enum class AccountType {
    OWNER,
    ADMIN,
    SECRETARY,
    TEACHER,
    PARENT
}

enum class Permission(val displayName: String, val category: String) {
    // Student Permissions
    VIEW_STUDENTS("عرض الطلاب", "الطلاب"),
    ADD_STUDENTS("إضافة طالب", "الطلاب"),
    EDIT_STUDENTS("تعديل بيانات الطالب", "الطلاب"),
    DELETE_STUDENTS("حذف الطلاب", "الطلاب"),

    // Teacher Permissions
    VIEW_TEACHERS("عرض المدرسين", "المدرسين"),
    ADD_TEACHERS("إضافة مدرس", "المدرسين"),
    EDIT_TEACHERS("تعديل بيانات المدرس", "المدرسين"),
    DELETE_TEACHERS("حذف المدرسين", "المدرسين"),

    // Financial Permissions
    VIEW_FINANCIALS("عرض المالية", "المالية"),
    EDIT_REVENUE("تعديل المالية", "المالية"),

    // Reports Permissions
    VIEW_REPORTS("عرض التقارير", "التقارير"),
    EXPORT_REPORTS("تصدير التقارير", "التقارير"),

    // Settings
    MANAGE_SETTINGS("إدارة الإعدادات", "الإعدادات"),

    // User / RBAC Permissions
    MANAGE_USERS("إدارة المستخدمين والصلاحيات", "المستخدمين"),

    // Room Permissions
    MANAGE_CLASSROOMS("إدارة القاعات والصفوف", "القاعات"),

    // Attendance
    VIEW_ATTENDANCE("عرض الحضور", "المرئيات"),
    MARK_ATTENDANCE("تسجيل الحضور والغياب", "المرئيات"),

    // Grades
    VIEW_GRADES("عرض الدرجات", "الأكاديمي"),
    MANAGE_GRADES("إدارة ورصد الدرجات والامتحانات", "الأكاديمي"),

    // Assignments
    VIEW_ASSIGNMENTS("عرض الواجبات", "الأكاديمي"),
    MANAGE_ASSIGNMENTS("إدارة الواجبات والمهام", "الأكاديمي")
}

data class UserRbac(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val role: AccountType = AccountType.SECRETARY,
    val permissions: List<String> = emptyList(), // Serialized for Firestore
    val isActive: Boolean = true,
    val isTempPasswordActive: Boolean = false,
    val tempPassword: String = "",
    val centerId: String = ""
)
