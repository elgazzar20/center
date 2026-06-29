package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    val profile: Flow<Profile?> = appDao.getProfile()
    val allTeachers: Flow<List<Teacher>> = appDao.getAllTeachers()
    val allStudents: Flow<List<Student>> = appDao.getAllStudents()
    val allAttendance: Flow<List<Attendance>> = appDao.getAllAttendance()
    val allPayments: Flow<List<Payment>> = appDao.getAllPayments()
    val allPaymentHistory: Flow<List<PaymentHistory>> = appDao.getAllPaymentHistory()
    val allGroups: Flow<List<Group>> = appDao.getAllGroups()
    val allExpenses: Flow<List<Expense>> = appDao.getAllExpenses()

    suspend fun saveProfile(profile: Profile) = appDao.insertProfile(profile)

    suspend fun addTeacher(teacher: Teacher) = appDao.insertTeacher(teacher)
    suspend fun deleteTeacher(id: String) = appDao.deleteTeacher(id)

    suspend fun addStudent(student: Student) = appDao.insertStudent(student)
    suspend fun deleteStudent(id: String) = appDao.deleteStudent(id)

    fun getStudentsByTeacher(teacherId: String): Flow<List<Student>> = appDao.getStudentsByTeacher(teacherId)

    fun getTeachersForStudent(studentId: String): Flow<List<Teacher>> = appDao.getTeachersForStudent(studentId)
    fun getStudentsForTeacher(teacherId: String): Flow<List<Student>> = appDao.getStudentsForTeacher(teacherId)
    fun getAllStudentTeacherCrossRefs(): Flow<List<StudentTeacherCrossRef>> = appDao.getAllStudentTeacherCrossRefs()
    suspend fun insertStudentTeacherCrossRef(crossRef: StudentTeacherCrossRef) = appDao.insertStudentTeacherCrossRef(crossRef)
    suspend fun insertStudentTeacherCrossRefs(crossRefs: List<StudentTeacherCrossRef>) = appDao.insertStudentTeacherCrossRefs(crossRefs)
    suspend fun deleteStudentTeacherCrossRefsForStudent(studentId: String) = appDao.deleteStudentTeacherCrossRefsForStudent(studentId)
    suspend fun deleteStudentTeacherCrossRef(studentId: String, teacherId: String) = appDao.deleteStudentTeacherCrossRef(studentId, teacherId)

    fun getAttendanceByTeacherAndDate(teacherId: String, date: Long): Flow<List<Attendance>> = 
        appDao.getAttendanceByTeacherAndDate(teacherId, date)

    suspend fun saveAttendance(attendance: List<Attendance>) = appDao.insertAttendance(attendance)
    suspend fun deleteAttendance(id: String) = appDao.deleteAttendance(id)

    suspend fun addPayment(payment: Payment) = appDao.insertPayment(payment)
    suspend fun deletePayment(id: String) = appDao.deletePayment(id)

    suspend fun addPaymentHistory(paymentHistory: PaymentHistory) = appDao.insertPaymentHistory(paymentHistory)
    suspend fun deletePaymentHistory(id: String) = appDao.deletePaymentHistory(id)

    suspend fun addGroup(group: Group) = appDao.insertGroup(group)
    suspend fun deleteGroup(id: String) = appDao.deleteGroup(id)

    suspend fun addExpense(expense: Expense) = appDao.insertExpense(expense)
    suspend fun deleteExpense(id: String) = appDao.deleteExpense(id)

    // Assignments
    val allAssignments: Flow<List<Assignment>> = appDao.getAllAssignments()
    suspend fun addAssignment(assignment: Assignment) = appDao.insertAssignment(assignment)
    suspend fun deleteAssignment(id: String) = appDao.deleteAssignment(id)

    // Exams
    val allExams: Flow<List<Exam>> = appDao.getAllExams()
    suspend fun addExam(exam: Exam) = appDao.insertExam(exam)
    suspend fun deleteExam(id: String) = appDao.deleteExam(id)

    // ExamGrades
    val allExamGrades: Flow<List<ExamGrade>> = appDao.getAllExamGrades()
    fun getGradesForExam(examId: String): Flow<List<ExamGrade>> = appDao.getGradesForExam(examId)
    suspend fun addExamGrades(grades: List<ExamGrade>) = appDao.insertExamGrades(grades)
    suspend fun addExamGrade(grade: ExamGrade) = appDao.insertExamGrade(grade)
    suspend fun deleteExamGrade(id: String) = appDao.deleteExamGrade(id)
    suspend fun deleteGradesForExam(examId: String) = appDao.deleteGradesForExam(examId)

    // StudentNotes
    fun getNotesForStudent(studentId: String): Flow<List<StudentNote>> = appDao.getNotesForStudent(studentId)
    suspend fun addStudentNote(note: StudentNote) = appDao.insertStudentNote(note)
    suspend fun deleteStudentNote(id: String) = appDao.deleteStudentNote(id)

    // ActivityLogs
    val allActivityLogs: Flow<List<ActivityLog>> = appDao.getAllActivityLogs()
    suspend fun addActivityLog(log: ActivityLog) = appDao.insertActivityLog(log)
    suspend fun clearActivityLogs() = appDao.clearActivityLogs()

    // MessageTemplates
    val allMessageTemplates: Flow<List<MessageTemplate>> = appDao.getAllMessageTemplates()
    suspend fun addMessageTemplate(template: MessageTemplate) = appDao.insertMessageTemplate(template)
    suspend fun deleteMessageTemplate(id: String) = appDao.deleteMessageTemplate(id)
    suspend fun clearMessageTemplates() = appDao.clearMessageTemplates()

    // CommunicationLogs
    val allCommunicationLogs: Flow<List<CommunicationLog>> = appDao.getAllCommunicationLogs()
    suspend fun addCommunicationLog(log: CommunicationLog) = appDao.insertCommunicationLog(log)
    suspend fun deleteCommunicationLog(id: String) = appDao.deleteCommunicationLog(id)
    suspend fun clearCommunicationLogs() = appDao.clearCommunicationLogs()

    // Staff
    val allStaff: Flow<List<Staff>> = appDao.getAllStaff()
    suspend fun addStaff(staff: Staff) = appDao.insertStaff(staff)
    suspend fun deleteStaff(id: String) = appDao.deleteStaff(id)
    suspend fun clearStaff() = appDao.clearStaff()

    // ScheduleEvent
    val allScheduleEvents: Flow<List<ScheduleEvent>> = appDao.getAllScheduleEvents()
    suspend fun addScheduleEvent(event: ScheduleEvent) = appDao.insertScheduleEvent(event)
    suspend fun deleteScheduleEvent(id: String) = appDao.deleteScheduleEvent(id)
    suspend fun clearScheduleEvents() = appDao.clearScheduleEvents()
    
    // Classroom
    val allClassrooms: Flow<List<Classroom>> = appDao.getAllClassrooms()
    suspend fun addClassroom(classroom: Classroom) = appDao.insertClassroom(classroom)
    suspend fun deleteClassroom(id: String) = appDao.deleteClassroom(id)

    // Parent & ParentStudentLink
    fun getParentByIdFlow(id: String): Flow<Parent?> = appDao.getParentByIdFlow(id)
    suspend fun getParentById(id: String): Parent? = appDao.getParentById(id)
    suspend fun insertParent(parent: Parent) = appDao.insertParent(parent)
    suspend fun insertParentStudentLink(link: ParentStudentLink) = appDao.insertParentStudentLink(link)
    suspend fun insertParentStudentLinks(links: List<ParentStudentLink>) = appDao.insertParentStudentLinks(links)
    suspend fun deleteParentStudentLink(parentId: String, studentId: String) = appDao.deleteParentStudentLink(parentId, studentId)
    suspend fun deleteLinksForParent(parentId: String) = appDao.deleteLinksForParent(parentId)
    fun getStudentsForParent(parentId: String): Flow<List<Student>> = appDao.getStudentsForParent(parentId)
    fun getLinkedStudentIdsForParent(parentId: String): Flow<List<String>> = appDao.getLinkedStudentIdsForParent(parentId)
    suspend fun getLinkedStudentIdsForParentDirect(parentId: String): List<String> = appDao.getLinkedStudentIdsForParentDirect(parentId)

    suspend fun clearAllLocalData() {
        appDao.clearStudents()
        appDao.clearAttendance()
        appDao.clearPayments()
        appDao.clearGroups()
        appDao.clearProfiles()
        appDao.clearExpenses()
        appDao.clearPaymentHistory()
        appDao.clearTeachers()
        appDao.clearAssignments()
        appDao.clearExams()
        appDao.clearExamGrades()
        appDao.clearStudentNotes()
        appDao.clearActivityLogs()
        appDao.clearMessageTemplates()
        appDao.clearCommunicationLogs()
        appDao.clearStaff()
        appDao.clearScheduleEvents()
        appDao.clearClassrooms()
        appDao.clearStudentTeacherCrossRefs()
        appDao.clearParents()
        appDao.clearParentStudentLinks()
    }
}
