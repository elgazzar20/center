package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.util.FirebaseSafe
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class SyncEngine(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val dao = database.appDao()

    private fun getCollection(firestore: FirebaseFirestore, name: String): com.google.firebase.firestore.CollectionReference? {
        val centerId = com.example.util.rbac.RbacManager.currentUserRbac.value?.centerId
        if (centerId.isNullOrEmpty()) return null
        return firestore.collection("centers").document(centerId).collection(name)
    }

    suspend fun syncAll(): Boolean {
        if (!FirebaseSafe.isAvailable) {
            Log.d("SyncEngine", "Firebase is not available. Skipping sync.")
            return false
        }
        return try {
            uploadAll()
            downloadAll()
            Log.d("SyncEngine", "Sync completed successfully.")
            true
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error during syncAll: ${e.message}", e)
            false
        }
    }

    suspend fun uploadAll() {
        val firestore = FirebaseSafe.firestore ?: return
        val centerId = com.example.util.rbac.RbacManager.currentUserRbac.value?.centerId
        if (centerId.isNullOrEmpty()) {
            Log.e("SyncEngine", "Cannot upload: centerId is empty")
            return
        }

        val centerDoc = firestore.collection("centers").document(centerId)

        // 1. Sync Teachers
        try {
            val localTeachers = dao.getAllTeachers().first()
            for (teacher in localTeachers) {
                val docRef = centerDoc.collection("teachers").document(teacher.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(teacher.lastUpdated, snapshot)) {
                    docRef.set(teacher).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading teachers: ${e.message}")
        }

        // 2. Sync Groups
        try {
            val localGroups = dao.getAllGroups().first()
            for (group in localGroups) {
                val docRef = centerDoc.collection("groups").document(group.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(group.lastUpdated, snapshot)) {
                    docRef.set(group).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading groups: ${e.message}")
        }

        // 3. Sync Students
        try {
            val localStudents = dao.getAllStudents().first()
            for (student in localStudents) {
                val docRef = centerDoc.collection("students").document(student.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(student.lastUpdated, snapshot)) {
                    docRef.set(student).await()
                    try {
                        firestore.collection("students").document(student.id).set(student).await()
                    } catch (e: Exception) {
                        Log.e("SyncEngine", "Failed to duplicate student ${student.name} to root: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading students: ${e.message}")
        }

        // 4. Sync Attendance
        try {
            val localAttendance = dao.getAllAttendance().first()
            for (attendance in localAttendance) {
                val docRef = centerDoc.collection("attendance").document(attendance.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(attendance.lastUpdated, snapshot)) {
                    docRef.set(attendance).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading attendance: ${e.message}")
        }

        // 5. Sync Payments
        try {
            val localPayments = dao.getAllPayments().first()
            for (payment in localPayments) {
                val docRef = centerDoc.collection("payments").document(payment.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(payment.lastUpdated, snapshot)) {
                    docRef.set(payment).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading payments: ${e.message}")
        }

        // 6. Sync Exams
        try {
            val localExams = dao.getAllExams().first()
            for (exam in localExams) {
                val docRef = centerDoc.collection("exams").document(exam.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(exam.lastUpdated, snapshot)) {
                    docRef.set(exam).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading exams: ${e.message}")
        }

        // 7. Sync Exam Grades
        try {
            val localExamGrades = dao.getAllExamGrades().first()
            for (grade in localExamGrades) {
                val docRef = centerDoc.collection("exam_grades").document(grade.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(grade.lastUpdated, snapshot)) {
                    docRef.set(grade).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading exam grades: ${e.message}")
        }

        // 8. Sync PaymentHistory
        try {
            val localPaymentHistory = dao.getAllPaymentHistory().first()
            for (history in localPaymentHistory) {
                val docRef = centerDoc.collection("payment_history").document(history.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(history.lastUpdated, snapshot)) {
                    docRef.set(history).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading payment history: ${e.message}")
        }

        // 9. Sync Expenses
        try {
            val localExpenses = dao.getAllExpenses().first()
            for (expense in localExpenses) {
                val docRef = centerDoc.collection("expenses").document(expense.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(expense.lastUpdated, snapshot)) {
                    docRef.set(expense).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading expenses: ${e.message}")
        }

        // 10. Sync Assignments
        try {
            val localAssignments = dao.getAllAssignments().first()
            for (assignment in localAssignments) {
                val docRef = centerDoc.collection("assignments").document(assignment.id)
                val snapshot = docRef.get().await()
                if (shouldUpload(assignment.lastUpdated, snapshot)) {
                    docRef.set(assignment).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading assignments: ${e.message}")
        }

        // 11. Sync StudentTeacherCrossRefs
        try {
            val localCrossRefs = dao.getAllStudentTeacherCrossRefs().first()
            for (crossRef in localCrossRefs) {
                val docId = "${crossRef.studentId}_${crossRef.teacherId}"
                val docRef = centerDoc.collection("student_teacher_cross_refs").document(docId)
                val snapshot = docRef.get().await()
                if (shouldUpload(crossRef.lastUpdated, snapshot)) {
                    docRef.set(crossRef).await()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error uploading cross refs: ${e.message}")
        }
    }

    suspend fun downloadAll() {
        val firestore = FirebaseSafe.firestore ?: return
        val centerId = com.example.util.rbac.RbacManager.currentUserRbac.value?.centerId
        if (centerId.isNullOrEmpty()) {
            Log.e("SyncEngine", "Cannot download: centerId is empty")
            return
        }

        val centerDoc = firestore.collection("centers").document(centerId)

        // 1. Sync Teachers
        try {
            val querySnapshot = centerDoc.collection("teachers").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(Teacher::class.java) ?: continue
                val localList = dao.getAllTeachers().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertTeacher(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading teachers: ${e.message}")
        }

        // 2. Sync Groups
        try {
            val querySnapshot = centerDoc.collection("groups").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(Group::class.java) ?: continue
                val localList = dao.getAllGroups().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertGroup(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading groups: ${e.message}")
        }

        // 3. Sync Students
        try {
            val querySnapshot = centerDoc.collection("students").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(Student::class.java) ?: continue
                val localList = dao.getAllStudents().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertStudent(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading students: ${e.message}")
        }

        // 4. Sync Attendance
        try {
            val querySnapshot = centerDoc.collection("attendance").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(Attendance::class.java) ?: continue
                val localList = dao.getAllAttendance().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertAttendance(listOf(remote))
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading attendance: ${e.message}")
        }

        // 5. Sync Payments
        try {
            val querySnapshot = centerDoc.collection("payments").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(Payment::class.java) ?: continue
                val localList = dao.getAllPayments().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertPayment(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading payments: ${e.message}")
        }

        // 6. Sync Exams
        try {
            val querySnapshot = centerDoc.collection("exams").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(Exam::class.java) ?: continue
                val localList = dao.getAllExams().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertExam(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading exams: ${e.message}")
        }

        // 7. Sync Exam Grades
        try {
            val querySnapshot = centerDoc.collection("exam_grades").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(ExamGrade::class.java) ?: continue
                val localList = dao.getAllExamGrades().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertExamGrade(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading exam grades: ${e.message}")
        }

        // 8. Sync PaymentHistory
        try {
            val querySnapshot = centerDoc.collection("payment_history").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(PaymentHistory::class.java) ?: continue
                val localList = dao.getAllPaymentHistory().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertPaymentHistory(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading payment history: ${e.message}")
        }

        // 9. Sync Expenses
        try {
            val querySnapshot = centerDoc.collection("expenses").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(Expense::class.java) ?: continue
                val localList = dao.getAllExpenses().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertExpense(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading expenses: ${e.message}")
        }

        // 10. Sync Assignments
        try {
            val querySnapshot = centerDoc.collection("assignments").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(Assignment::class.java) ?: continue
                val localList = dao.getAllAssignments().first()
                val local = localList.find { it.id == remote.id }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertAssignment(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading assignments: ${e.message}")
        }

        // 11. Sync StudentTeacherCrossRefs
        try {
            val querySnapshot = centerDoc.collection("student_teacher_cross_refs").get().await()
            for (doc in querySnapshot.documents) {
                val remote = doc.toObject(StudentTeacherCrossRef::class.java) ?: continue
                val localList = dao.getAllStudentTeacherCrossRefs().first()
                val local = localList.find { it.studentId == remote.studentId && it.teacherId == remote.teacherId }
                if (local == null || remote.lastUpdated > local.lastUpdated) {
                    dao.insertStudentTeacherCrossRef(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Error downloading cross refs: ${e.message}")
        }

    }

    private fun shouldUpload(localLastUpdated: Long, remoteSnapshot: DocumentSnapshot): Boolean {
        if (!remoteSnapshot.exists()) return true
        val remoteLastUpdated = remoteSnapshot.getLong("lastUpdated") ?: 0L
        return localLastUpdated > remoteLastUpdated
    }
}
