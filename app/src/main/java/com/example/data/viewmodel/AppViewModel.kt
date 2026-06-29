package com.example.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.tasks.await

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    val profile: StateFlow<Profile?>
    val teachers: StateFlow<List<Teacher>>
    val students: StateFlow<List<Student>>
    val attendance: StateFlow<List<Attendance>>
    val payments: StateFlow<List<Payment>>
    val paymentHistory: StateFlow<List<PaymentHistory>>
    val expenses: StateFlow<List<Expense>>
    val assignments: StateFlow<List<Assignment>>
    val exams: StateFlow<List<Exam>>
    val examGrades: StateFlow<List<ExamGrade>>
    val groups: StateFlow<List<Group>>
    val activityLogs: StateFlow<List<ActivityLog>>
    val messageTemplates: StateFlow<List<MessageTemplate>>
    val communicationLogs: StateFlow<List<CommunicationLog>>
    val scheduleEvents: StateFlow<List<ScheduleEvent>>
    val classrooms: StateFlow<List<Classroom>>
    val studentTeacherCrossRefs: StateFlow<List<StudentTeacherCrossRef>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
        
        // Auto-sync when RBAC is loaded
        viewModelScope.launch {
            com.example.util.rbac.RbacManager.currentUserRbac.collect { rbac ->
                if (rbac != null && rbac.centerId.isNotEmpty()) {
                    syncData()
                }
            }
        }

        studentTeacherCrossRefs = repository.getAllStudentTeacherCrossRefs().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        classrooms = repository.allClassrooms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        scheduleEvents = repository.allScheduleEvents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activityLogs = repository.allActivityLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        groups = repository.allGroups.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        messageTemplates = repository.allMessageTemplates.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        communicationLogs = repository.allCommunicationLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        assignments = repository.allAssignments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        exams = repository.allExams.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        examGrades = repository.allExamGrades.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        profile = repository.profile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        teachers = repository.allTeachers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        students = repository.allStudents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        attendance = repository.allAttendance.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        payments = repository.allPayments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        paymentHistory = repository.allPaymentHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        expenses = repository.allExpenses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize default profile if empty
        viewModelScope.launch {
            repository.profile.firstOrNull()?.let {
                // profile exists
            } ?: run {
                repository.saveProfile(Profile())
            }

            // Initialize default message templates if empty
            val templates = repository.allMessageTemplates.firstOrNull() ?: emptyList()
            if (templates.isEmpty()) {
                repository.addMessageTemplate(
                    MessageTemplate(
                        title = "إشعار غياب طالب",
                        channel = "WhatsApp",
                        category = "attendance",
                        content = "عزيزي ولي أمر الطالب {student_name}، نود إحاطتكم علماً بأن الطالب قد تغيب عن حضور حصة اليوم لـ {subject}. نرجو المتابعة للتأكد من مصلحته."
                    )
                )
                repository.addMessageTemplate(
                    MessageTemplate(
                        title = "إشعار متأخرات مادية",
                        channel = "WhatsApp",
                        category = "fees",
                        content = "عزيزي ولي أمر الطالب {student_name}، نود تذكيركم بموعد سداد الرسوم الشهرية المستحقة وقدرها {amount} {currency} لشهر {month}. شاكرين تعاونكم المستمر."
                    )
                )
                repository.addMessageTemplate(
                    MessageTemplate(
                        title = "إشعار نتيجة امتحان",
                        channel = "WhatsApp",
                        category = "exam",
                        content = "عزيزي ولي أمر الطالب {student_name}، تم اليوم رصد نتيجة امتحان {exam_name} وحصل الطالب على درجة {score} من {total_marks}. نتمنى له دوام التوفيق والتميز."
                    )
                )
                repository.addMessageTemplate(
                    MessageTemplate(
                        title = "إشعار عدم تسليم الواجب",
                        channel = "WhatsApp",
                        category = "homework",
                        content = "مرحباً يا فندم، نود إبلاغكم بأن الطالب {student_name} لم يقم بتسليم الواجب المطلوب لدرس {assignment_name} اليوم. يرجى حثه على الالتزام والتحضير الجيد للدرس القادم."
                    )
                )
                repository.addMessageTemplate(
                    MessageTemplate(
                        title = "رسالة عامة ترحيبية",
                        channel = "WhatsApp",
                        category = "custom",
                        content = "مرحباً {student_name}، يسعدنا انضمامك إلينا في مركزنا التعليمي. نتمنى لك رحلة دراسية موفقة ومليئة بالنجاح والإنجازات!"
                    )
                )
            }

            // Initialize default students if empty (Auto-seeding)
            try {
                val existingStudents = repository.allStudents.first()
                val currentUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
                if (existingStudents.isEmpty() && currentUser == null) {
                    android.util.Log.d("AppViewModel", "Students table is empty and no user is logged in. Triggering generateDemoData() to seed student database.")
                    generateDemoData()
                }
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Error checking/seeding empty student database: ${e.message}")
            }
        }
    }

    // Profile Actions
    fun updateProfile(name: String, phone: String, systemType: String, centerName: String, whatsappNumber: String, currency: String) {
        viewModelScope.launch {
            val current = profile.value ?: Profile()
            repository.saveProfile(
                current.copy(
                    name = name,
                    phone = phone,
                    systemType = systemType,
                    centerName = centerName,
                    whatsappNumber = whatsappNumber,
                    currency = currency
                )
            )
        }
    }

    fun setSystemType(systemType: String) {
        viewModelScope.launch {
            val current = profile.value ?: Profile()
            repository.saveProfile(current.copy(systemType = systemType))
        }
    }

    // Teacher Actions
    fun addTeacher(
        name: String, subject: String, phone: String, salaryType: String, salaryValue: Double, notes: String, stages: String = "",
        onError: (String) -> Unit = {}, onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val isDuplicate = teachers.value.any { it.name.equals(name, ignoreCase = true) }
            if (isDuplicate) {
                onError("يوجد معلم مسجل بنفس الاسم")
                return@launch
            }
            val teacher = Teacher(
                name = name,
                subject = subject,
                phone = phone,
                salaryType = salaryType,
                salaryValue = salaryValue,
                notes = notes,
                stages = stages
            )
            repository.addTeacher(teacher)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("teachers")?.document(teacher.id)?.set(teacher)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            onSuccess()
        }
    }

    fun updateTeacher(id: String, name: String, subject: String, phone: String, salaryType: String, salaryValue: Double, notes: String, stages: String = "") {
        viewModelScope.launch {
            val teacher = teachers.value.find { it.id == id }
            if (teacher != null) {
                val updated = teacher.copy(
                    name = name,
                    subject = subject,
                    phone = phone,
                    salaryType = salaryType,
                    salaryValue = salaryValue,
                    notes = notes,
                    stages = stages,
                    lastUpdated = System.currentTimeMillis()
                )
                repository.addTeacher(updated)
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.util.FirebaseSafe.getCenterCollection("teachers")?.document(updated.id)?.set(updated)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    fun deleteTeacher(id: String) {
        viewModelScope.launch {
            repository.deleteTeacher(id)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("teachers")?.document(id)?.delete()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Student Actions
    fun addStudent(
        name: String,
        parentName: String,
        parentPhone: String,
        studentPhone: String,
        grade: String,
        customCourse: String,
        teacherId: String,
        monthlyFee: Double,
        discount: Double,
        isExempt: Boolean,
        notes: String,
        studentType: String = "GROUP",
        privateSessionsCount: Int = 0,
        privateSessionPrice: Double = 0.0,
        privateTotalAmount: Double = 0.0,
        teacherIds: List<String> = emptyList(),
        teacherIdToFee: Map<String, Double> = emptyMap(),
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val isNameDuplicate = students.value.any { it.name.trim().equals(name.trim(), ignoreCase = true) }
                val isPhoneDuplicate = if (studentPhone.isNotBlank()) {
                    students.value.any { it.studentPhone.isNotBlank() && it.studentPhone.trim() == studentPhone.trim() }
                } else false

                if (isNameDuplicate) {
                    onError("يوجد طالب مسجل بنفس الاسم")
                    return@launch
                }
                if (isPhoneDuplicate) {
                    onError("يوجد طالب مسجل بنفس رقم الهاتف")
                    return@launch
                }

                val uniqueCode = "STU-" + java.util.UUID.randomUUID().toString().replace("-", "").take(6).uppercase(java.util.Locale.ROOT)
                val activeCenterId = com.example.util.rbac.RbacManager.currentUserRbac.value?.centerId?.ifEmpty { null }
                    ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val student = Student(
                    name = name.trim(),
                    parentName = parentName.trim(),
                    parentPhone = parentPhone.trim(),
                    studentPhone = studentPhone.trim(),
                    grade = grade,
                    customCourse = customCourse,
                    teacherId = teacherId,
                    monthlyFee = monthlyFee,
                    discount = discount,
                    isExempt = isExempt,
                    notes = notes,
                    studentType = studentType,
                    privateSessionsCount = privateSessionsCount,
                    privateSessionPrice = privateSessionPrice,
                    privateTotalAmount = privateTotalAmount,
                    qrCode = uniqueCode,
                    parentCode = uniqueCode,
                    centerId = activeCenterId
                )
                repository.addStudent(student)

                // Insert many-to-many teacher cross references
                val refs = teacherIds.map { tId -> 
                    StudentTeacherCrossRef(
                        studentId = student.id, 
                        teacherId = tId, 
                        customFee = teacherIdToFee[tId] ?: monthlyFee
                    ) 
                }
                if (refs.isNotEmpty()) {
                    repository.insertStudentTeacherCrossRefs(refs)
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        refs.forEach { ref ->
                            try {
                                val docId = "${ref.studentId}_${ref.teacherId}"
                                com.example.util.FirebaseSafe.getCenterCollection("student_teacher_cross_refs")?.document(docId)?.set(ref)
                            } catch (fe: Exception) {
                                // Ignore
                            }
                        }
                    }
                } else if (teacherId.isNotBlank()) {
                    val ref = StudentTeacherCrossRef(student.id, teacherId, customFee = monthlyFee)
                    repository.insertStudentTeacherCrossRef(ref)
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val docId = "${ref.studentId}_${ref.teacherId}"
                            com.example.util.FirebaseSafe.getCenterCollection("student_teacher_cross_refs")?.document(docId)?.set(ref)
                        } catch (fe: Exception) {
                            // Ignore
                        }
                    }
                }

                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.util.FirebaseSafe.getCenterCollection("students")?.document(student.id)?.set(student)
                        com.example.util.FirebaseSafe.firestore?.collection("students")?.document(student.id)?.set(student)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                logActivity("إضافة طالب", student.name)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء حفظ الطالب")
            }
        }
    }

    fun addDirectStudent(student: Student) {
        viewModelScope.launch {
            repository.addStudent(student)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("students")?.document(student.id)?.set(student)?.await()
                com.example.util.FirebaseSafe.firestore?.collection("students")?.document(student.id)?.set(student)?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun fetchAndSyncLinkedStudents(studentIds: List<String>) {
        val firestore = com.example.util.FirebaseSafe.firestore ?: return
        viewModelScope.launch {
            try {
                for (studentId in studentIds) {
                    val studentDoc = firestore.collection("students").document(studentId).get().await()
                    if (studentDoc.exists()) {
                        val student = studentDoc.toObject(Student::class.java)
                        if (student != null) {
                            repository.addStudent(student)
                            
                            val centerId = student.centerId
                            val baseRef = if (centerId.isNotEmpty()) {
                                firestore.collection("centers").document(centerId)
                            } else null

                            val attendanceCollection = baseRef?.collection("attendance") ?: firestore.collection("attendance")
                            val attendanceQuery = attendanceCollection
                                .whereEqualTo("studentId", studentId)
                                .get()
                                .await()
                            if (!attendanceQuery.isEmpty) {
                                val attList = attendanceQuery.documents.mapNotNull { it.toObject(Attendance::class.java) }
                                if (attList.isNotEmpty()) {
                                    repository.saveAttendance(attList)
                                }
                            }
                            
                            val paymentsCollection = baseRef?.collection("payments") ?: firestore.collection("payments")
                            val paymentsQuery = paymentsCollection
                                .whereEqualTo("studentId", studentId)
                                .get()
                                .await()
                            for (doc in paymentsQuery.documents) {
                                val pay = doc.toObject(Payment::class.java)
                                if (pay != null) repository.addPayment(pay)
                            }
                            
                            val gradesCollection = baseRef?.collection("exam_grades") ?: firestore.collection("exam_grades")
                            val gradesQuery = gradesCollection
                                .whereEqualTo("studentId", studentId)
                                .get()
                                .await()
                            val syncedExamIds = mutableSetOf<String>()
                            for (doc in gradesQuery.documents) {
                                val grade = doc.toObject(ExamGrade::class.java)
                                if (grade != null) {
                                    repository.addExamGrade(grade)
                                    syncedExamIds.add(grade.examId)
                                }
                            }
                            
                            val examsCollection = baseRef?.collection("exams") ?: firestore.collection("exams")
                            for (examId in syncedExamIds) {
                                val examDoc = examsCollection.document(examId).get().await()
                                if (examDoc.exists()) {
                                    val exam = examDoc.toObject(Exam::class.java)
                                    if (exam != null) repository.addExam(exam)
                                }
                            }
                            
                            student.groupId?.let { groupId ->
                                val groupsCollection = baseRef?.collection("groups") ?: firestore.collection("groups")
                                val groupDoc = groupsCollection.document(groupId).get().await()
                                if (groupDoc.exists()) {
                                    val grp = groupDoc.toObject(Group::class.java)
                                    if (grp != null) repository.addGroup(grp)
                                }
                                
                                val assignmentsCollection = baseRef?.collection("assignments") ?: firestore.collection("assignments")
                                val assignmentsQuery = assignmentsCollection
                                    .whereEqualTo("groupId", groupId)
                                    .get()
                                    .await()
                                for (doc in assignmentsQuery.documents) {
                                    val assign = doc.toObject(Assignment::class.java)
                                    if (assign != null) repository.addAssignment(assign)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // --- Realtime Firestore Sync and Parent Database Relationship Operations ---
    private val parentFirestoreListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    fun clearParentListeners() {
        parentFirestoreListeners.forEach { it.remove() }
        parentFirestoreListeners.clear()
        android.util.Log.d("RealtimeSync", "Cleared all active Firestore parent listeners.")
    }

    override fun onCleared() {
        super.onCleared()
        clearParentListeners()
    }

    fun saveParentAndLinkLocally(
        parentId: String,
        name: String,
        email: String,
        phone: String,
        studentIds: List<String>
    ) {
        viewModelScope.launch {
            try {
                // Save Parent
                val parent = Parent(id = parentId, name = name, email = email, phone = phone)
                repository.insertParent(parent)
                
                // Save ParentStudentLinks
                val links = studentIds.map { ParentStudentLink(parentId = parentId, studentId = it) }
                repository.insertParentStudentLinks(links)
                
                android.util.Log.d("ParentDB", "Successfully saved Parent and ParentStudentLinks in local Room DB.")
            } catch (e: Exception) {
                android.util.Log.e("ParentDB", "Error saving Parent / Links locally: ${e.message}")
            }
        }
    }

    fun removeParentStudentLinkLocally(parentId: String, studentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteParentStudentLink(parentId, studentId)
                android.util.Log.d("ParentDB", "Successfully deleted ParentStudentLink for parent $parentId and student $studentId.")
            } catch (e: Exception) {
                android.util.Log.e("ParentDB", "Error deleting ParentStudentLink locally: ${e.message}")
            }
        }
    }

    fun getStudentsForParentFlow(parentId: String): Flow<List<Student>> {
        return repository.getStudentsForParent(parentId)
    }

    fun startRealtimeSyncForParent(studentIds: List<String>) {
        val firestore = com.example.util.FirebaseSafe.firestore ?: return
        clearParentListeners()
        android.util.Log.d("RealtimeSync", "Starting real-time synchronization for student IDs: $studentIds")
        
        viewModelScope.launch {
            for (studentId in studentIds) {
                if (studentId.isBlank()) continue
                
                // Fetch student first to obtain centerId
                val localStudent = students.value.find { it.id == studentId }
                val centerId: String = if (localStudent != null && localStudent.centerId.isNotEmpty()) {
                    localStudent.centerId
                } else {
                    try {
                        firestore.collection("students").document(studentId).get().await().getString("centerId") ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                }

                val baseRef = if (centerId.isNotEmpty()) {
                    firestore.collection("centers").document(centerId)
                } else null

                // 1. Listen to Student Profile Document
                try {
                    val studentListener = firestore.collection("students").document(studentId)
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                android.util.Log.e("RealtimeSync", "Error listening to student $studentId: ${e.message}")
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                val student = snapshot.toObject(Student::class.java)
                                if (student != null) {
                                    viewModelScope.launch {
                                        repository.addStudent(student)
                                        student.groupId?.let { groupId ->
                                            syncGroupAndAssignmentsRealtime(groupId, centerId)
                                        }
                                    }
                                }
                            }
                        }
                    parentFirestoreListeners.add(studentListener)
                } catch (ex: Exception) {
                    android.util.Log.e("RealtimeSync", "Failed to register student listener: ${ex.message}")
                }
                
                // 2. Listen to Attendance Records
                try {
                    val attendanceCollection = baseRef?.collection("attendance") ?: firestore.collection("attendance")
                    val attendanceListener = attendanceCollection
                        .whereEqualTo("studentId", studentId)
                        .addSnapshotListener { snapshots, e ->
                            if (e != null) {
                                android.util.Log.e("RealtimeSync", "Error listening to attendance for $studentId: ${e.message}")
                                return@addSnapshotListener
                            }
                            if (snapshots != null) {
                                val attList = snapshots.documents.mapNotNull { it.toObject(Attendance::class.java) }
                                viewModelScope.launch {
                                    if (attList.isNotEmpty()) {
                                        repository.saveAttendance(attList)
                                    }
                                }
                            }
                        }
                    parentFirestoreListeners.add(attendanceListener)
                } catch (ex: Exception) {
                    android.util.Log.e("RealtimeSync", "Failed to register attendance listener: ${ex.message}")
                }
                
                // 3. Listen to Payments Records
                try {
                    val paymentsCollection = baseRef?.collection("payments") ?: firestore.collection("payments")
                    val paymentsListener = paymentsCollection
                        .whereEqualTo("studentId", studentId)
                        .addSnapshotListener { snapshots, e ->
                            if (e != null) {
                                android.util.Log.e("RealtimeSync", "Error listening to payments for $studentId: ${e.message}")
                                return@addSnapshotListener
                            }
                            if (snapshots != null) {
                                val payList = snapshots.documents.mapNotNull { it.toObject(Payment::class.java) }
                                viewModelScope.launch {
                                    for (pay in payList) {
                                        repository.addPayment(pay)
                                    }
                                }
                            }
                        }
                    parentFirestoreListeners.add(paymentsListener)
                } catch (ex: Exception) {
                    android.util.Log.e("RealtimeSync", "Failed to register payments listener: ${ex.message}")
                }
                
                // 4. Listen to Exam Grades
                try {
                    val gradesCollection = baseRef?.collection("exam_grades") ?: firestore.collection("exam_grades")
                    val gradesListener = gradesCollection
                        .whereEqualTo("studentId", studentId)
                        .addSnapshotListener { snapshots, e ->
                            if (e != null) {
                                android.util.Log.e("RealtimeSync", "Error listening to grades for $studentId: ${e.message}")
                                return@addSnapshotListener
                            }
                            if (snapshots != null) {
                                val gradesList = snapshots.documents.mapNotNull { it.toObject(ExamGrade::class.java) }
                                viewModelScope.launch {
                                    val examIds = mutableSetOf<String>()
                                    for (grade in gradesList) {
                                        repository.addExamGrade(grade)
                                        examIds.add(grade.examId)
                                    }
                                    for (examId in examIds) {
                                        syncExamRealtime(examId, centerId)
                                    }
                                }
                            }
                        }
                    parentFirestoreListeners.add(gradesListener)
                } catch (ex: Exception) {
                    android.util.Log.e("RealtimeSync", "Failed to register grades listener: ${ex.message}")
                }

                // 5. Listen to Student Notes
                try {
                    val notesCollection = baseRef?.collection("student_notes") ?: firestore.collection("student_notes")
                    val notesListener = notesCollection
                        .whereEqualTo("studentId", studentId)
                        .addSnapshotListener { snapshots, e ->
                            if (e != null) {
                                android.util.Log.e("RealtimeSync", "Error listening to notes for $studentId: ${e.message}")
                                return@addSnapshotListener
                            }
                            if (snapshots != null) {
                                val notesList = snapshots.documents.mapNotNull { it.toObject(StudentNote::class.java) }
                                viewModelScope.launch {
                                    for (note in notesList) {
                                        repository.addStudentNote(note)
                                    }
                                }
                            }
                        }
                    parentFirestoreListeners.add(notesListener)
                } catch (ex: Exception) {
                    android.util.Log.e("RealtimeSync", "Failed to register student_notes listener: ${ex.message}")
                }

                // 6. Listen to Communication Logs (acting as notifications)
                try {
                    val commsCollection = baseRef?.collection("communication_logs") ?: firestore.collection("communication_logs")
                    val commsListener = commsCollection
                        .whereEqualTo("studentId", studentId)
                        .addSnapshotListener { snapshots, e ->
                            if (e != null) {
                                android.util.Log.e("RealtimeSync", "Error listening to communications for $studentId: ${e.message}")
                                return@addSnapshotListener
                            }
                            if (snapshots != null) {
                                val commsList = snapshots.documents.mapNotNull { it.toObject(CommunicationLog::class.java) }
                                viewModelScope.launch {
                                    for (comm in commsList) {
                                        repository.addCommunicationLog(comm)
                                    }
                                }
                            }
                        }
                    parentFirestoreListeners.add(commsListener)
                } catch (ex: Exception) {
                    android.util.Log.e("RealtimeSync", "Failed to register communication_logs listener: ${ex.message}")
                }
            }
        }
    }

    private fun syncGroupAndAssignmentsRealtime(groupId: String, centerId: String) {
        val firestore = com.example.util.FirebaseSafe.firestore ?: return
        val baseRef = if (centerId.isNotEmpty()) {
            firestore.collection("centers").document(centerId)
        } else null
        
        // Listen to Group Document
        try {
            val groupsCollection = baseRef?.collection("groups") ?: firestore.collection("groups")
            val groupListener = groupsCollection.document(groupId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val group = snapshot.toObject(Group::class.java)
                        if (group != null) {
                            viewModelScope.launch {
                                repository.addGroup(group)
                            }
                        }
                    }
                }
            parentFirestoreListeners.add(groupListener)
        } catch (ex: Exception) {}
        
        // Listen to Group's Assignments
        try {
            val assignmentsCollection = baseRef?.collection("assignments") ?: firestore.collection("assignments")
            val assignmentsListener = assignmentsCollection
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        val list = snapshots.documents.mapNotNull { it.toObject(Assignment::class.java) }
                        viewModelScope.launch {
                            for (item in list) {
                                repository.addAssignment(item)
                            }
                        }
                    }
                }
            parentFirestoreListeners.add(assignmentsListener)
        } catch (ex: Exception) {}
    }

    private fun syncExamRealtime(examId: String, centerId: String) {
        val firestore = com.example.util.FirebaseSafe.firestore ?: return
        val baseRef = if (centerId.isNotEmpty()) {
            firestore.collection("centers").document(centerId)
        } else null
        
        try {
            val examsCollection = baseRef?.collection("exams") ?: firestore.collection("exams")
            val examListener = examsCollection.document(examId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val exam = snapshot.toObject(Exam::class.java)
                        if (exam != null) {
                            viewModelScope.launch {
                                repository.addExam(exam)
                            }
                        }
                    }
                }
            parentFirestoreListeners.add(examListener)
        } catch (ex: Exception) {}
    }

    fun updateStudent(
        id: String,
        name: String,
        parentName: String,
        parentPhone: String,
        studentPhone: String,
        grade: String,
        customCourse: String,
        teacherId: String,
        monthlyFee: Double,
        discount: Double,
        isExempt: Boolean,
        notes: String,
        studentType: String = "GROUP",
        privateSessionsCount: Int = 0,
        privateSessionPrice: Double = 0.0,
        privateTotalAmount: Double = 0.0,
        teacherIds: List<String> = emptyList(),
        teacherIdToFee: Map<String, Double> = emptyMap(),
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val isNameDuplicate = students.value.any { it.name.trim().equals(name.trim(), ignoreCase = true) && it.id != id }
                val isPhoneDuplicate = if (studentPhone.isNotBlank()) {
                    students.value.any { it.studentPhone.isNotBlank() && it.studentPhone.trim() == studentPhone.trim() && it.id != id }
                } else false

                if (isNameDuplicate) {
                    onError("يوجد طالب آخر مسجل بنفس الاسم")
                    return@launch
                }
                if (isPhoneDuplicate) {
                    onError("يوجد طالب آخر مسجل بنفس رقم الهاتف")
                    return@launch
                }

                val student = students.value.find { it.id == id }
                if (student != null) {
                    val updated = student.copy(
                        name = name.trim(),
                        parentName = parentName.trim(),
                        parentPhone = parentPhone.trim(),
                        studentPhone = studentPhone.trim(),
                        grade = grade,
                        customCourse = customCourse,
                        teacherId = teacherId,
                        monthlyFee = monthlyFee,
                        discount = discount,
                        isExempt = isExempt,
                        notes = notes,
                        studentType = studentType,
                        privateSessionsCount = privateSessionsCount,
                        privateSessionPrice = privateSessionPrice,
                        privateTotalAmount = privateTotalAmount,
                        lastUpdated = System.currentTimeMillis()
                    )
                    repository.addStudent(updated)

                    // Sync many-to-many teacher associations
                    repository.deleteStudentTeacherCrossRefsForStudent(id)
                    // Note: we don't have a reliable way to delete them from Firestore without querying first,
                    // but the sync engine will overwrite or just merge. To be safe, we can just push new ones.
                    val refs = teacherIds.map { tId -> 
                        StudentTeacherCrossRef(
                            studentId = id, 
                            teacherId = tId, 
                            customFee = teacherIdToFee[tId] ?: monthlyFee
                        ) 
                    }
                    if (refs.isNotEmpty()) {
                        repository.insertStudentTeacherCrossRefs(refs)
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            refs.forEach { ref ->
                                try {
                                    val docId = "${ref.studentId}_${ref.teacherId}"
                                    com.example.util.FirebaseSafe.getCenterCollection("student_teacher_cross_refs")?.document(docId)?.set(ref)
                                } catch (fe: Exception) {
                                    // Ignore
                                }
                            }
                        }
                    } else if (teacherId.isNotBlank()) {
                        val ref = StudentTeacherCrossRef(id, teacherId, customFee = monthlyFee)
                        repository.insertStudentTeacherCrossRef(ref)
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val docId = "${ref.studentId}_${ref.teacherId}"
                                com.example.util.FirebaseSafe.getCenterCollection("student_teacher_cross_refs")?.document(docId)?.set(ref)
                            } catch (fe: Exception) {
                                // Ignore
                            }
                        }
                    }

                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            com.example.util.FirebaseSafe.getCenterCollection("students")?.document(updated.id)?.set(updated)
                            com.example.util.FirebaseSafe.firestore?.collection("students")?.document(updated.id)?.set(updated)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    logActivity("تعديل طالب", name)
                    onSuccess()
                } else {
                    onError("لم يتم العثور على الطالب")
                }
            } catch (e: Exception) {
                onError(e.message ?: "حدث خطأ أثناء تعديل الطالب")
            }
        }
    }

    fun regenerateStudentQr(studentId: String) {
        viewModelScope.launch {
            val student = students.value.find { it.id == studentId }
            if (student != null) {
                val newQr = "STU-" + java.util.UUID.randomUUID().toString().replace("-", "").take(6).uppercase(java.util.Locale.ROOT)
                val updated = student.copy(qrCode = newQr, parentCode = newQr, lastUpdated = System.currentTimeMillis())
                repository.addStudent(updated)
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.util.FirebaseSafe.getCenterCollection("students")?.document(updated.id)?.set(updated)
                        com.example.util.FirebaseSafe.firestore?.collection("students")?.document(updated.id)?.set(updated)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    fun initializeStudentQrIfEmpty(student: Student) {
        if (student.qrCode.isEmpty()) {
            viewModelScope.launch {
                val newQr = "STU_" + java.util.UUID.randomUUID().toString().replace("-", "").take(8).uppercase(java.util.Locale.ROOT)
                val updated = student.copy(qrCode = newQr, lastUpdated = System.currentTimeMillis())
                repository.addStudent(updated)
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("students")?.document(updated.id)?.set(updated)?.await()
                    com.example.util.FirebaseSafe.firestore?.collection("students")?.document(updated.id)?.set(updated)?.await()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    sealed class QrScanResult {
        object Idle : QrScanResult()
        data class Success(val studentName: String, val message: String) : QrScanResult()
        data class Error(val message: String) : QrScanResult()
        data class AlreadyScanned(val studentName: String, val message: String) : QrScanResult()
    }

    private val _qrScanResult = MutableStateFlow<QrScanResult>(QrScanResult.Idle)
    val qrScanResult: StateFlow<QrScanResult> = _qrScanResult.asStateFlow()

    fun resetQrScanResult() {
        _qrScanResult.value = QrScanResult.Idle
    }

    fun processScannedQrCode(qrCodeString: String) {
        viewModelScope.launch {
            val qr = qrCodeString.trim()
            if (qr.isEmpty()) {
                _qrScanResult.value = QrScanResult.Error("رمز QR فارغ أو غير صالح")
                return@launch
            }

            // Find the student matching this QR code
            val student = students.value.find { it.qrCode == qr }
            if (student == null) {
                _qrScanResult.value = QrScanResult.Error("طالب غير مسجل! لم يتم العثور على طالب بهذا الرمز: $qr")
                return@launch
            }

            // Get start and end of today
            val today = System.currentTimeMillis()
            val startOfToday = getStartOfDay(today)
            val endOfToday = startOfToday + (24 * 60 * 60 * 1000) - 1

            // Check if already registered today
            val alreadyRegistered = attendance.value.find {
                it.studentId == student.id && it.date in startOfToday..endOfToday
            }

            if (alreadyRegistered != null) {
                if (alreadyRegistered.status == "present") {
                    _qrScanResult.value = QrScanResult.AlreadyScanned(
                        studentName = student.name,
                        message = "تم تسجيل حضور الطالب '${student.name}' مسبقاً اليوم!"
                    )
                } else {
                    // Update status to present
                    val updatedRecord = alreadyRegistered.copy(status = "present", date = today, lastUpdated = System.currentTimeMillis())
                    repository.saveAttendance(listOf(updatedRecord))
                    try {
                        com.example.util.FirebaseSafe.getCenterCollection("attendance")?.document(updatedRecord.id)?.set(updatedRecord)?.await()
                    } catch (e: Exception) {
                        // Ignore
                    }
                    logActivity("تسجيل حضور", student.name)
                    _qrScanResult.value = QrScanResult.Success(
                        studentName = student.name,
                        message = "تم تحديث حالة الطالب '${student.name}' إلى حاضر بنجاح!"
                    )
                }
            } else {
                // Insert new present record
                val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
                val monthStr = monthFormat.format(Date(today))
                val newRecord = Attendance(
                    studentId = student.id,
                    teacherId = student.teacherId,
                    date = today,
                    status = "present",
                    month = monthStr,
                    notes = "تسجيل حضور عبر QR",
                    lastUpdated = System.currentTimeMillis()
                )
                repository.saveAttendance(listOf(newRecord))
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("attendance")?.document(newRecord.id)?.set(newRecord)?.await()
                } catch (e: Exception) {
                    // Ignore
                }
                logActivity("تسجيل حضور", student.name)
                _qrScanResult.value = QrScanResult.Success(
                    studentName = student.name,
                    message = "تم تسجيل حضور الطالب '${student.name}' بنجاح!"
                )
            }
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun deleteStudent(id: String) {
        viewModelScope.launch {
            val studentName = students.value.find { it.id == id }?.name ?: id
            repository.deleteStudent(id)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("students")?.document(id)?.delete()
                    com.example.util.FirebaseSafe.firestore?.collection("students")?.document(id)?.delete()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            logActivity("حذف طالب", studentName)
        }
    }

    // Attendance Actions
    fun saveAttendanceBatch(attendanceList: List<Attendance>) {
        viewModelScope.launch {
            repository.saveAttendance(attendanceList)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val firestore = com.example.util.FirebaseSafe.firestore
                    if (firestore != null) {
                        for (att in attendanceList) {
                            firestore.collection("attendance").document(att.id).set(att)
                            com.example.util.FirebaseSafe.getCenterCollection("attendance")?.document(att.id)?.set(att)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
            
            val currentStudents = students.value
            // Log attendance for each student in the batch
            attendanceList.forEach { att ->
                val sName = currentStudents.find { it.id == att.studentId }?.name ?: "طالب"
                val statusStr = if (att.status == "present") "حضور" else "غياب"
                logActivity("تسجيل $statusStr", sName)
            }
            
            // Send immediate local notification for each student registered as absent
            val absentList = attendanceList.filter { it.status == "absent" }
            if (absentList.isNotEmpty()) {
                val currentStudents = students.value
                absentList.forEach { att ->
                    val studentName = currentStudents.find { it.id == att.studentId }?.name ?: "طالب"
                    com.example.notification.NotificationHelper.showNotification(
                        context = getApplication(),
                        title = "❌ غياب مسجل",
                        body = "تم تسجيل غياب الطالب: $studentName اليوم",
                        channelId = com.example.notification.NotificationHelper.ATTENDANCE_CHANNEL_ID
                    )
                }
            }
        }
    }

    // Payment Actions
    fun addPayment(studentId: String, teacherId: String, amount: Double, month: String, method: String, notes: String) {
        viewModelScope.launch {
            val payment = Payment(
                studentId = studentId,
                teacherId = teacherId,
                amount = amount,
                month = month,
                method = method,
                notes = notes
            )
            repository.addPayment(payment)
            
            // Extract month and year from "YYYY-MM"
            val yearStr = if (month.contains("-")) month.substringBefore("-") else java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
            val monthStr = if (month.contains("-")) month.substringAfter("-") else String.format("%02d", java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)
            
            val record = PaymentHistory(
                id = payment.id,
                studentId = studentId,
                amount = amount,
                paymentDate = payment.date,
                paymentMethod = method,
                notes = notes,
                month = monthStr,
                year = yearStr,
                lastUpdated = System.currentTimeMillis()
            )
            repository.addPaymentHistory(record)

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("payments")?.document(payment.id)?.set(payment)
                    com.example.util.FirebaseSafe.getCenterCollection("payment_history")?.document(record.id)?.set(record)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            val studentName = students.value.find { it.id == studentId }?.name ?: studentId
            logActivity("إضافة دفعة", "$studentName ($amount)")
        }
    }

    fun deletePayment(id: String) {
        viewModelScope.launch {
            val payment = payments.value.find { it.id == id }
            val amount = payment?.amount ?: 0.0
            val sName = students.value.find { it.id == payment?.studentId }?.name ?: ""
            repository.deletePayment(id)
            repository.deletePaymentHistory(id)

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("payments")?.document(id)?.delete()
                    com.example.util.FirebaseSafe.getCenterCollection("payment_history")?.document(id)?.delete()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            logActivity("حذف دفعة", "$sName ($amount)")
        }
    }

    // PaymentHistory Actions (Accounting System)
    fun addPaymentHistory(studentId: String, amount: Double, method: String, notes: String, month: String, year: String, date: Long) {
        viewModelScope.launch {
            val record = PaymentHistory(
                studentId = studentId,
                amount = amount,
                paymentDate = date,
                paymentMethod = method,
                notes = notes,
                month = month,
                year = year,
                lastUpdated = System.currentTimeMillis()
            )
            repository.addPaymentHistory(record)

            val monthCode = "$year-$month" // "YYYY-MM"
            val student = students.value.find { it.id == studentId }
            val payment = Payment(
                id = record.id,
                studentId = studentId,
                teacherId = student?.teacherId ?: "",
                amount = amount,
                month = monthCode,
                date = date,
                method = method,
                notes = notes
            )
            repository.addPayment(payment)

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("payment_history")?.document(record.id)?.set(record)
                    com.example.util.FirebaseSafe.getCenterCollection("payments")?.document(payment.id)?.set(payment)
                } catch (e: Exception) {}
            }
            val studentName = students.value.find { it.id == studentId }?.name ?: studentId
            logActivity("إضافة دفعة", "$studentName ($amount)")
        }
    }

    fun updatePaymentHistory(paymentHistory: PaymentHistory) {
        viewModelScope.launch {
            val updated = paymentHistory.copy(lastUpdated = System.currentTimeMillis())
            repository.addPaymentHistory(updated)

            val monthCode = "${paymentHistory.year}-${paymentHistory.month}" // "YYYY-MM"
            val student = students.value.find { it.id == paymentHistory.studentId }
            val payment = Payment(
                id = paymentHistory.id,
                studentId = paymentHistory.studentId,
                teacherId = student?.teacherId ?: "",
                amount = paymentHistory.amount,
                month = monthCode,
                date = paymentHistory.paymentDate,
                method = paymentHistory.paymentMethod,
                notes = paymentHistory.notes
            )
            repository.addPayment(payment)

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("payment_history")?.document(updated.id)?.set(updated)
                    com.example.util.FirebaseSafe.getCenterCollection("payments")?.document(payment.id)?.set(payment)
                } catch (e: Exception) {}
            }
            val studentName = students.value.find { it.id == paymentHistory.studentId }?.name ?: paymentHistory.studentId
            logActivity("تعديل دفعة", "$studentName (${paymentHistory.amount})")
        }
    }

    fun deletePaymentHistory(id: String) {
        viewModelScope.launch {
            val record = paymentHistory.value.find { it.id == id }
            val amount = record?.amount ?: 0.0
            val sName = students.value.find { it.id == record?.studentId }?.name ?: ""
            repository.deletePaymentHistory(id)
            repository.deletePayment(id)

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("payment_history")?.document(id)?.delete()
                    com.example.util.FirebaseSafe.getCenterCollection("payments")?.document(id)?.delete()
                } catch (e: Exception) {}
            }
            logActivity("حذف دفعة", "$sName ($amount)")
        }
    }

    // Expense Actions
    fun addExpense(category: String, amount: Double, description: String, isMonthly: Boolean, month: String) {
        viewModelScope.launch {
            val expense = Expense(
                title = description,
                category = category,
                amount = amount,
                description = description,
                isMonthly = isMonthly,
                month = month,
                lastUpdated = System.currentTimeMillis()
            )
            repository.addExpense(expense)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("expenses")?.document(expense.id)?.set(expense)
                } catch (e: Exception) {}
            }
        }
    }

    fun addExpense(title: String, amount: Double, category: String, date: Long, notes: String) {
        viewModelScope.launch {
            val expense = Expense(
                title = title,
                amount = amount,
                category = category,
                date = date,
                notes = notes,
                description = title,
                lastUpdated = System.currentTimeMillis()
            )
            repository.addExpense(expense)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("expenses")?.document(expense.id)?.set(expense)
                } catch (e: Exception) {}
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            val updated = expense.copy(lastUpdated = System.currentTimeMillis())
            repository.addExpense(updated)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("expenses")?.document(updated.id)?.set(updated)
                } catch (e: Exception) {}
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("expenses")?.document(id)?.delete()
                } catch (e: Exception) {}
            }
        }
    }

    // Assignments Actions
    fun addAssignment(title: String, description: String, dueDate: Long, groupId: String, groupName: String, notes: String) {
        viewModelScope.launch {
            val assignment = Assignment(
                title = title,
                description = description,
                dueDate = dueDate,
                groupId = groupId,
                groupName = groupName,
                notes = notes,
                lastUpdated = System.currentTimeMillis()
            )
            repository.addAssignment(assignment)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("assignments")?.document(assignment.id)?.set(assignment)?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun updateAssignment(assignment: Assignment) {
        viewModelScope.launch {
            val updated = assignment.copy(lastUpdated = System.currentTimeMillis())
            repository.addAssignment(updated)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("assignments")?.document(updated.id)?.set(updated)?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun deleteAssignment(id: String) {
        viewModelScope.launch {
            repository.deleteAssignment(id)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("assignments")?.document(id)?.delete()?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Exams Actions
    fun addExam(name: String, totalMarks: Double, date: Long, groupId: String, groupName: String, onExamAdded: (Exam) -> Unit = {}) {
        viewModelScope.launch {
            val exam = Exam(
                name = name,
                totalMarks = totalMarks,
                date = date,
                groupId = groupId,
                groupName = groupName,
                lastUpdated = System.currentTimeMillis()
            )
            repository.addExam(exam)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("exams")?.document(exam.id)?.set(exam)?.await()
            } catch (e: Exception) {
                // Ignore
            }
            onExamAdded(exam)
        }
    }

    fun updateExam(exam: Exam) {
        viewModelScope.launch {
            val updated = exam.copy(lastUpdated = System.currentTimeMillis())
            repository.addExam(updated)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("exams")?.document(updated.id)?.set(updated)?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun deleteExam(id: String) {
        viewModelScope.launch {
            repository.deleteExam(id)
            repository.deleteGradesForExam(id)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("exams")?.document(id)?.delete()?.await()
                // Also delete exam grades from Firestore for this exam
                val gradesQuery = com.example.util.FirebaseSafe.getCenterCollection("exam_grades")
                    ?.whereEqualTo("examId", id)?.get()?.await()
                gradesQuery?.documents?.forEach { doc ->
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // ExamGrades Actions
    fun saveExamGrades(grades: List<ExamGrade>) {
        viewModelScope.launch {
            val updatedGrades = grades.map { it.copy(lastUpdated = System.currentTimeMillis()) }
            repository.addExamGrades(updatedGrades)
            try {
                for (grade in updatedGrades) {
                    com.example.util.FirebaseSafe.getCenterCollection("exam_grades")?.document(grade.id)?.set(grade)?.await()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun saveExamGrade(grade: ExamGrade) {
        viewModelScope.launch {
            val updated = grade.copy(lastUpdated = System.currentTimeMillis())
            repository.addExamGrade(updated)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("exam_grades")?.document(updated.id)?.set(updated)?.await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun getGradesForExam(examId: String): Flow<List<ExamGrade>> {
        return repository.getGradesForExam(examId)
    }

    // Student Archive Actions
    fun toggleStudentArchive(studentId: String) {
        viewModelScope.launch {
            val student = students.value.find { it.id == studentId }
            if (student != null) {
                val updated = student.copy(isArchived = !student.isArchived, lastUpdated = System.currentTimeMillis())
                repository.addStudent(updated)
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("students")?.document(updated.id)?.set(updated)?.await()
                } catch (e: Exception) {}
            }
        }
    }

    // Student Note Actions
    fun getNotesForStudent(studentId: String): Flow<List<StudentNote>> {
        return repository.getNotesForStudent(studentId)
    }

    fun addStudentNote(studentId: String, noteText: String, createdBy: String) {
        viewModelScope.launch {
            val note = StudentNote(
                studentId = studentId,
                note = noteText,
                createdBy = createdBy
            )
            repository.addStudentNote(note)
        }
    }

    fun deleteStudentNote(id: String) {
        viewModelScope.launch {
            repository.deleteStudentNote(id)
        }
    }

    // Group Actions
    fun addGroup(
        name: String, teacherName: String, classroom: String, schedule: String, notes: String,
        onError: (String) -> Unit = {}, onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val isDuplicate = groups.value.any { it.name.equals(name, ignoreCase = true) }
            if (isDuplicate) {
                onError("توجد مجموعة مسجلة بنفس الاسم")
                return@launch
            }
            val group = Group(
                name = name,
                teacherName = teacherName,
                classroom = classroom,
                schedule = schedule,
                notes = notes
            )
            repository.addGroup(group)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("groups")?.document(group.id)?.set(group)?.await()
            } catch (e: Exception) {
                // Ignore
            }
            logActivity("إضافة مجموعة", name)
            onSuccess()
        }
    }

    fun deleteGroup(id: String) {
        viewModelScope.launch {
            val groupName = groups.value.find { it.id == id }?.name ?: id
            repository.deleteGroup(id)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("groups")?.document(id)?.delete()?.await()
            } catch (e: Exception) {
                // Ignore
            }
            logActivity("حذف مجموعة", groupName)
        }
    }

    fun logActivity(action: String, targetId: String, details: String = "") {
        viewModelScope.launch {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val currentRbac = com.example.util.rbac.RbacManager.currentUserRbac.value
            
            val userDisplay = when {
                currentRbac != null -> currentRbac.name
                currentUser != null -> currentUser.email ?: "مستخدم"
                else -> profile.value?.name?.ifBlank { "المشرف" } ?: "المشرف"
            }
            
            // Log locally to Room
            val log = ActivityLog(
                action = action,
                userId = currentUser?.uid ?: "local",
                userName = userDisplay,
                targetId = targetId,
                details = details
            )
            repository.addActivityLog(log)

            // Log remotely to Firestore Activity Logs
            com.example.util.ActivityLogManager.logAction(action, "$action لـ: $targetId $details")
        }
    }

    fun clearActivityLogs() {
        viewModelScope.launch {
            repository.clearActivityLogs()
        }
    }

    // Message Template Actions
    fun addMessageTemplate(title: String, channel: String, category: String, content: String) {
        viewModelScope.launch {
            val template = MessageTemplate(
                title = title,
                channel = channel,
                category = category,
                content = content
            )
            repository.addMessageTemplate(template)
            logActivity("إضافة قالب رسائل", title)
        }
    }

    fun deleteMessageTemplate(id: String) {
        viewModelScope.launch {
            val title = messageTemplates.value.find { it.id == id }?.title ?: id
            repository.deleteMessageTemplate(id)
            logActivity("حذف قالب رسائل", title)
        }
    }

    // Communication Log Actions
    fun addCommunicationLog(studentId: String, studentName: String, recipient: String, channel: String, message: String, status: String = "SUCCESS") {
        viewModelScope.launch {
            val log = CommunicationLog(
                studentId = studentId,
                studentName = studentName,
                recipient = recipient,
                channel = channel,
                message = message,
                status = status
            )
            repository.addCommunicationLog(log)
        }
    }

    fun deleteCommunicationLog(id: String) {
        viewModelScope.launch {
            repository.deleteCommunicationLog(id)
        }
    }

    fun clearCommunicationLogs() {
        viewModelScope.launch {
            repository.clearCommunicationLogs()
        }
    }

    fun addScheduleEvent(event: ScheduleEvent, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            // Advanced conflict check
            val existingEvents = repository.allScheduleEvents.firstOrNull() ?: emptyList()
            
            // Check classroom conflict
            if (event.classroomId.isNotEmpty()) {
                val roomConflict = existingEvents.any { it.id != event.id && 
                    it.classroomId == event.classroomId &&
                    (event.startTime < it.endTime && event.endTime > it.startTime)
                }
                if (roomConflict) {
                    onError("القاعة محجوزة في هذا التوقيت")
                    return@launch
                }
            }

            // Check teacher conflict
            if (event.teacherId.isNotEmpty()) {
                val teacherConflict = existingEvents.any { it.id != event.id && 
                    it.teacherId == event.teacherId &&
                    (event.startTime < it.endTime && event.endTime > it.startTime)
                }
                if (teacherConflict) {
                    onError("المدرس لديه حصة أخرى في نفس التوقيت")
                    return@launch
                }
            }
            
            repository.addScheduleEvent(event)
            onSuccess()
        }
    }

    fun deleteScheduleEvent(id: String) {
        viewModelScope.launch {
            repository.deleteScheduleEvent(id)
        }
    }

    fun addClassroom(classroom: Classroom, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val isDuplicate = classrooms.value.any { it.name.equals(classroom.name, ignoreCase = true) }
            if (isDuplicate) {
                onError("توجد قاعة بنفس الاسم بالفعل")
                return@launch
            }
            repository.addClassroom(classroom)
        }
    }

    fun deleteClassroom(id: String) {
        viewModelScope.launch {
            repository.deleteClassroom(id)
        }
    }

    fun generateDemoData() {
        viewModelScope.launch {
            // Generate 5 teachers
            val t1 = Teacher(name = "أحمد محمد", subject = "لغة عربية", phone = "01000000001", salaryType = "fixed", salaryValue = 5000.0)
            val t2 = Teacher(name = "محمود علي", subject = "رياضيات", phone = "01000000002", salaryType = "fixed", salaryValue = 6000.0)
            val t3 = Teacher(name = "سعيد إبراهيم", subject = "لغة إنجليزية", phone = "01000000003", salaryType = "percentage", salaryValue = 40.0)
            val t4 = Teacher(name = "خالد مصطفى", subject = "فيزياء", phone = "01000000004", salaryType = "fixed", salaryValue = 5500.0)
            val t5 = Teacher(name = "فاطمة الزهراء", subject = "كيمياء", phone = "01000000005", salaryType = "percentage", salaryValue = 35.0)

            repository.addTeacher(t1)
            repository.addTeacher(t2)
            repository.addTeacher(t3)
            repository.addTeacher(t4)
            repository.addTeacher(t5)

            val teacherList = listOf(t1, t2, t3, t4, t5)

            // Add 3 groups
            val g1 = Group(name = "الصف الأول الثانوي - أ", teacherName = t1.name, classroom = "القاعة 1", schedule = "السبت والثلاثاء 4 م")
            val g2 = Group(name = "الصف الثاني الثانوي - رياضيات", teacherName = t2.name, classroom = "القاعة 2", schedule = "الأحد والأربعاء 5 م")
            val g3 = Group(name = "الصف الثالث الإعدادي - انجليزي", teacherName = t3.name, classroom = "القاعة 3", schedule = "الاثنين والخميس 6 م")

            repository.addGroup(g1)
            repository.addGroup(g2)
            repository.addGroup(g3)

            val groupsList = listOf(g1, g2, g3)

            val firestore = com.example.util.FirebaseSafe.firestore
            if (firestore != null) {
                try {
                    for (t in teacherList) {
                        firestore.collection("teachers").document(t.id).set(t)
                        com.example.util.FirebaseSafe.getCenterCollection("teachers")?.document(t.id)?.set(t)
                    }
                    for (g in groupsList) {
                        firestore.collection("groups").document(g.id).set(g)
                        com.example.util.FirebaseSafe.getCenterCollection("groups")?.document(g.id)?.set(g)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Seeding", "Error seeding teachers/groups to Firestore: ${e.message}")
                }
            }

            // Generate 30 students
            val firstNames = listOf("أحمد", "محمد", "يوسف", "عمر", "علي", "محمود", "كريم", "خالد", "حسن", "حسين", "طارق", "سامي", "هاني", "وائل", "رامي", "فاطمة", "مريم", "سارة", "نورهان", "آية", "ندى", "سلمى", "ياسمين", "نور", "ليلى", "هند", "أمل", "منى", "ريهام", "دينا")
            val parentNames = listOf("السيد", "إبراهيم", "مصطفى", "عباس", "عبدالله", "عثمان", "توفيق", "سعد", "جابر", "رضا")

            for (i in 0 until 30) {
                val t = teacherList.random()
                val g = groupsList.random()
                val uniqueCode = "STU-" + java.util.UUID.randomUUID().toString().replace("-", "").take(6).uppercase(java.util.Locale.ROOT)
                val activeCenterId = com.example.util.rbac.RbacManager.currentUserRbac.value?.centerId?.ifEmpty { null }
                    ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val student = Student(
                    name = "${firstNames[i]} ${parentNames.random()} ${parentNames.random()}",
                    parentName = "والد ${firstNames[i]}",
                    parentPhone = if (i == 0) "parent@demo.com" else "011000000${String.format("%02d", i)}", // First student belongs to demo parent
                    studentPhone = "012000000${String.format("%02d", i)}",
                    grade = "متنوع",
                    customCourse = g.name,
                    teacherId = t.id,
                    monthlyFee = 300.0,
                    qrCode = uniqueCode,
                    parentCode = uniqueCode,
                    centerId = activeCenterId
                )
                repository.addStudent(student)
                
                if (firestore != null) {
                    try {
                        firestore.collection("students").document(student.id).set(student)
                        com.example.util.FirebaseSafe.getCenterCollection("students")?.document(student.id)?.set(student)
                    } catch (e: Exception) {
                        android.util.Log.e("Seeding", "Error seeding student ${student.name} to Firestore: ${e.message}")
                    }
                }

                // Add demo data for the first student so parent dashboard looks good
                if (i == 0) {
                    // Add attendance
                    val now = System.currentTimeMillis()
                    val attList = listOf(
                        Attendance(studentId = student.id, teacherId = t.id, date = now - 86400000 * 2, status = "present", month = "2026-06"),
                        Attendance(studentId = student.id, teacherId = t.id, date = now - 86400000 * 1, status = "absent", month = "2026-06"),
                        Attendance(studentId = student.id, teacherId = t.id, date = now, status = "present", month = "2026-06")
                    )
                    repository.saveAttendance(attList)
                    
                    // Add payment
                    val payHistory = PaymentHistory(studentId = student.id, amount = 300.0, paymentDate = now - 86400000 * 10, paymentMethod = "cash", notes = "قسط شهر يونيو", month = "2026-06", year = "2026")
                    repository.addPaymentHistory(payHistory)
                    
                    // Add exam and grades
                    val demoExam = Exam(name = "امتحان تجريبي يونيو", totalMarks = 100.0, date = now - 86400000 * 5, groupId = g.id, groupName = g.name)
                    repository.addExam(demoExam)
                    
                    val grade = ExamGrade(examId = demoExam.id, studentId = student.id, score = 85.0)
                    repository.addExamGrade(grade)

                    if (firestore != null) {
                        try {
                            for (att in attList) {
                                firestore.collection("attendance").document(att.id).set(att)
                                com.example.util.FirebaseSafe.getCenterCollection("attendance")?.document(att.id)?.set(att)
                            }
                            firestore.collection("payments").document(student.id + "_p1").set(
                                Payment(id = student.id + "_p1", studentId = student.id, teacherId = t.id, amount = 300.0, month = "2026-06", date = now - 86400000 * 10, method = "cash", notes = "قسط شهر يونيو")
                            )
                            com.example.util.FirebaseSafe.getCenterCollection("payments")?.document(student.id + "_p1")?.set(
                                Payment(id = student.id + "_p1", studentId = student.id, teacherId = t.id, amount = 300.0, month = "2026-06", date = now - 86400000 * 10, method = "cash", notes = "قسط شهر يونيو")
                            )
                            firestore.collection("exams").document(demoExam.id).set(demoExam)
                            com.example.util.FirebaseSafe.getCenterCollection("exams")?.document(demoExam.id)?.set(demoExam)
                            
                            firestore.collection("exam_grades").document(grade.id).set(grade)
                            com.example.util.FirebaseSafe.getCenterCollection("exam_grades")?.document(grade.id)?.set(grade)
                        } catch (e: Exception) {
                            android.util.Log.e("Seeding", "Error seeding demo details to Firestore: ${e.message}")
                        }
                    }
                }
            }
            
            logActivity("تهيئة", "تم إضافة البيانات التجريبية بنجاح")
        }
    }

    fun syncData(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val syncEngine = com.example.data.sync.SyncEngine(getApplication())
            val success = syncEngine.syncAll()
            onComplete(success)
        }
    }

    fun clearAllLocalData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.clearAllLocalData()
                onComplete()
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Error clearing all local data: ${e.message}")
                onComplete()
            }
        }
    }
}
