package com.example.util.rbac

import com.example.data.model.AccountType
import com.example.data.model.Permission

object RbacDefaults {
    fun getDefaultPermissionsForRole(role: AccountType): List<Permission> {
        return when (role) {
            AccountType.OWNER -> Permission.values().toList()
            AccountType.ADMIN -> listOf(
                Permission.VIEW_STUDENTS, Permission.ADD_STUDENTS, Permission.EDIT_STUDENTS, Permission.DELETE_STUDENTS,
                Permission.VIEW_TEACHERS, Permission.ADD_TEACHERS, Permission.EDIT_TEACHERS, Permission.DELETE_TEACHERS,
                Permission.VIEW_FINANCIALS, Permission.EDIT_REVENUE,
                Permission.VIEW_REPORTS, Permission.EXPORT_REPORTS,
                Permission.MANAGE_CLASSROOMS,
                Permission.MANAGE_SETTINGS,
                Permission.VIEW_ATTENDANCE, Permission.MARK_ATTENDANCE,
                Permission.VIEW_GRADES, Permission.MANAGE_GRADES,
                Permission.VIEW_ASSIGNMENTS, Permission.MANAGE_ASSIGNMENTS
            )
            AccountType.SECRETARY -> listOf(
                Permission.VIEW_STUDENTS,
                Permission.ADD_STUDENTS,
                Permission.EDIT_STUDENTS,
                Permission.VIEW_TEACHERS,
                Permission.VIEW_REPORTS,
                Permission.VIEW_ATTENDANCE,
                Permission.MARK_ATTENDANCE,
                Permission.VIEW_ASSIGNMENTS,
                Permission.VIEW_GRADES
            )
            AccountType.TEACHER -> listOf(
                Permission.VIEW_STUDENTS,
                Permission.VIEW_ATTENDANCE,
                Permission.MARK_ATTENDANCE,
                Permission.VIEW_GRADES,
                Permission.MANAGE_GRADES,
                Permission.VIEW_ASSIGNMENTS,
                Permission.MANAGE_ASSIGNMENTS
            )
            AccountType.PARENT -> listOf(
                Permission.VIEW_STUDENTS,
                Permission.VIEW_ATTENDANCE,
                Permission.VIEW_GRADES,
                Permission.VIEW_ASSIGNMENTS
            )
        }
    }
}
