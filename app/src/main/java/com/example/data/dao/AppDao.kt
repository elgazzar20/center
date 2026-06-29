package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Profile
    @Query("SELECT * FROM profiles WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<Profile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    // Teachers
    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher)

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteTeacher(id: String)

    // Students
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE teacherId = :teacherId ORDER BY name ASC")
    fun getStudentsByTeacher(teacherId: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudent(id: String)

    // Student-Teacher Cross References (Many-to-Many)
    @Query("""
        SELECT t.* FROM teachers t
        INNER JOIN student_teacher_cross_ref ref ON t.id = ref.teacherId
        WHERE ref.studentId = :studentId
        ORDER BY t.name ASC
    """)
    fun getTeachersForStudent(studentId: String): Flow<List<Teacher>>

    @Query("""
        SELECT s.* FROM students s
        INNER JOIN student_teacher_cross_ref ref ON s.id = ref.studentId
        WHERE ref.teacherId = :teacherId
        ORDER BY s.name ASC
    """)
    fun getStudentsForTeacher(teacherId: String): Flow<List<Student>>

    @Query("SELECT * FROM student_teacher_cross_ref")
    fun getAllStudentTeacherCrossRefs(): Flow<List<StudentTeacherCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentTeacherCrossRef(crossRef: StudentTeacherCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentTeacherCrossRefs(crossRefs: List<StudentTeacherCrossRef>)

    @Query("DELETE FROM student_teacher_cross_ref WHERE studentId = :studentId")
    suspend fun deleteStudentTeacherCrossRefsForStudent(studentId: String)

    @Query("DELETE FROM student_teacher_cross_ref WHERE studentId = :studentId AND teacherId = :teacherId")
    suspend fun deleteStudentTeacherCrossRef(studentId: String, teacherId: String)

    // Attendance
    @Query("SELECT * FROM attendance ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE teacherId = :teacherId AND date = :date")
    fun getAttendanceByTeacherAndDate(teacherId: String, date: Long): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: List<Attendance>)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendance(id: String)

    // Payments
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePayment(id: String)

    // PaymentHistory (Accounting System)
    @Query("SELECT * FROM payment_history ORDER BY paymentDate DESC")
    fun getAllPaymentHistory(): Flow<List<PaymentHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentHistory(paymentHistory: PaymentHistory)

    @Query("DELETE FROM payment_history WHERE id = :id")
    suspend fun deletePaymentHistory(id: String)


    // Groups System
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<Group>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: String)


    // Expenses
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)

    // Clear queries for backup restoration
    @Query("DELETE FROM students")
    suspend fun clearStudents()

    @Query("DELETE FROM attendance")
    suspend fun clearAttendance()

    @Query("DELETE FROM payments")
    suspend fun clearPayments()

    @Query("DELETE FROM groups")
    suspend fun clearGroups()

    @Query("DELETE FROM profiles")
    suspend fun clearProfiles()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM payment_history")
    suspend fun clearPaymentHistory()

    @Query("DELETE FROM teachers")
    suspend fun clearTeachers()

    // Assignment DAO methods
    @Query("SELECT * FROM assignments ORDER BY dueDate ASC")
    fun getAllAssignments(): Flow<List<Assignment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignment(id: String)

    @Query("DELETE FROM assignments")
    suspend fun clearAssignments()

    // Exam DAO methods
    @Query("SELECT * FROM exams ORDER BY date DESC")
    fun getAllExams(): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExam(id: String)

    @Query("DELETE FROM exams")
    suspend fun clearExams()

    // ExamGrade DAO methods
    @Query("SELECT * FROM exam_grades WHERE examId = :examId")
    fun getGradesForExam(examId: String): Flow<List<ExamGrade>>

    @Query("SELECT * FROM exam_grades")
    fun getAllExamGrades(): Flow<List<ExamGrade>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamGrades(grades: List<ExamGrade>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamGrade(grade: ExamGrade)

    @Query("DELETE FROM exam_grades WHERE id = :id")
    suspend fun deleteExamGrade(id: String)

    @Query("DELETE FROM exam_grades WHERE examId = :examId")
    suspend fun deleteGradesForExam(examId: String)

    @Query("DELETE FROM exam_grades")
    suspend fun clearExamGrades()

    // StudentNote DAO methods
    @Query("SELECT * FROM student_notes WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getNotesForStudent(studentId: String): Flow<List<StudentNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentNote(note: StudentNote)

    @Query("DELETE FROM student_notes WHERE id = :id")
    suspend fun deleteStudentNote(id: String)

    @Query("DELETE FROM student_notes")
    suspend fun clearStudentNotes()

    // ActivityLog DAO methods
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivityLogs()

    // MessageTemplate DAO methods
    @Query("SELECT * FROM message_templates ORDER BY createdAt DESC")
    fun getAllMessageTemplates(): Flow<List<MessageTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageTemplate(template: MessageTemplate)

    @Query("DELETE FROM message_templates WHERE id = :id")
    suspend fun deleteMessageTemplate(id: String)

    @Query("DELETE FROM message_templates")
    suspend fun clearMessageTemplates()

    // CommunicationLog DAO methods
    @Query("SELECT * FROM communication_logs ORDER BY sentAt DESC")
    fun getAllCommunicationLogs(): Flow<List<CommunicationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunicationLog(log: CommunicationLog)

    @Query("DELETE FROM communication_logs WHERE id = :id")
    suspend fun deleteCommunicationLog(id: String)

    @Query("DELETE FROM communication_logs")
    suspend fun clearCommunicationLogs()

    // Staff DAO methods
    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun getAllStaff(): Flow<List<Staff>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: Staff)

    @Query("DELETE FROM staff WHERE id = :id")
    suspend fun deleteStaff(id: String)

    @Query("DELETE FROM staff")
    suspend fun clearStaff()

    // ScheduleEvent DAO methods
    @Query("SELECT * FROM schedule_events ORDER BY startTime ASC")
    fun getAllScheduleEvents(): Flow<List<ScheduleEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEvent(event: ScheduleEvent)

    @Query("DELETE FROM schedule_events WHERE id = :id")
    suspend fun deleteScheduleEvent(id: String)

    @Query("DELETE FROM schedule_events")
    suspend fun clearScheduleEvents()

    // Classroom DAO methods
    @Query("SELECT * FROM classrooms ORDER BY name ASC")
    fun getAllClassrooms(): Flow<List<Classroom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassroom(classroom: Classroom)

    @Query("DELETE FROM classrooms WHERE id = :id")
    suspend fun deleteClassroom(id: String)

    // Parent & Parent-Student Links
    @Query("SELECT * FROM parents WHERE id = :id LIMIT 1")
    fun getParentByIdFlow(id: String): Flow<Parent?>

    @Query("SELECT * FROM parents WHERE id = :id LIMIT 1")
    suspend fun getParentById(id: String): Parent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParent(parent: Parent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParentStudentLink(link: ParentStudentLink)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParentStudentLinks(links: List<ParentStudentLink>)

    @Query("DELETE FROM parent_student_links WHERE parentId = :parentId AND studentId = :studentId")
    suspend fun deleteParentStudentLink(parentId: String, studentId: String)

    @Query("DELETE FROM parent_student_links WHERE parentId = :parentId")
    suspend fun deleteLinksForParent(parentId: String)

    @Query("""
        SELECT s.* FROM students s
        INNER JOIN parent_student_links link ON s.id = link.studentId
        WHERE link.parentId = :parentId
        ORDER BY s.name ASC
    """)
    fun getStudentsForParent(parentId: String): Flow<List<Student>>

    @Query("SELECT studentId FROM parent_student_links WHERE parentId = :parentId")
    fun getLinkedStudentIdsForParent(parentId: String): Flow<List<String>>

    @Query("SELECT studentId FROM parent_student_links WHERE parentId = :parentId")
    suspend fun getLinkedStudentIdsForParentDirect(parentId: String): List<String>

    @Query("DELETE FROM classrooms")
    suspend fun clearClassrooms()

    @Query("DELETE FROM student_teacher_cross_ref")
    suspend fun clearStudentTeacherCrossRefs()

    @Query("DELETE FROM parents")
    suspend fun clearParents()

    @Query("DELETE FROM parent_student_links")
    suspend fun clearParentStudentLinks()
}
