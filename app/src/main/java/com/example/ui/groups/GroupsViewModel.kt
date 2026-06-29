package com.example.ui.groups

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GroupsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    // State flows from repository
    val groups: StateFlow<List<Group>>
    val students: StateFlow<List<Student>>
    val teachers: StateFlow<List<Teacher>>
    val attendance: StateFlow<List<Attendance>>
    val profile: StateFlow<Profile?>

    // Selected Filters for Attendance / Management
    private val _selectedGradeFilter = MutableStateFlow<String?> (null)
    val selectedGradeFilter: StateFlow<String?> = _selectedGradeFilter.asStateFlow()

    private val _selectedGroupFilter = MutableStateFlow<String?> (null)
    val selectedGroupFilter: StateFlow<String?> = _selectedGroupFilter.asStateFlow()

    private val _selectedCourseFilter = MutableStateFlow<String?> (null)
    val selectedCourseFilter: StateFlow<String?> = _selectedCourseFilter.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())

        groups = repository.allGroups.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        students = repository.allStudents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        teachers = repository.allTeachers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        attendance = repository.allAttendance.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        profile = repository.profile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    // Filter Setters
    fun setGradeFilter(grade: String?) {
        _selectedGradeFilter.value = grade
    }

    fun setGroupFilter(groupId: String?) {
        _selectedGroupFilter.value = groupId
    }

    fun setCourseFilter(course: String?) {
        _selectedCourseFilter.value = course
    }

    // Group CRUD Operations
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
                notes = notes,
                lastUpdated = System.currentTimeMillis()
            )
            repository.addGroup(group)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("groups")?.document(group.id)?.set(group)
            } catch (e: Exception) {}
            onSuccess()
        }
    }

    fun updateGroup(group: Group) {
        viewModelScope.launch {
            val updated = group.copy(lastUpdated = System.currentTimeMillis())
            repository.addGroup(updated) // insert with replace
            try {
                com.example.util.FirebaseSafe.getCenterCollection("groups")?.document(updated.id)?.set(updated)
            } catch (e: Exception) {}
        }
    }

    fun deleteGroup(id: String) {
        viewModelScope.launch {
            // Unlink students in this group first
            val currentStudents = students.value.filter { it.groupId == id }
            currentStudents.forEach { student ->
                val updatedStudent = student.copy(groupId = null, lastUpdated = System.currentTimeMillis())
                repository.addStudent(updatedStudent)
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("students")?.document(updatedStudent.id)?.set(updatedStudent)
                } catch (e: Exception) {}
            }
            repository.deleteGroup(id)
            try {
                com.example.util.FirebaseSafe.getCenterCollection("groups")?.document(id)?.delete()
            } catch (e: Exception) {}
        }
    }

    // Student Group Assignment
    fun updateStudentGroup(studentId: String, groupId: String?) {
        viewModelScope.launch {
            val student = students.value.find { it.id == studentId }
            if (student != null) {
                val updated = student.copy(groupId = groupId, lastUpdated = System.currentTimeMillis())
                repository.addStudent(updated)
                try {
                    com.example.util.FirebaseSafe.getCenterCollection("students")?.document(updated.id)?.set(updated)
                } catch (e: Exception) {}
            }
        }
    }

    fun assignMultipleStudentsToGroup(groupId: String?, studentIds: List<String>) {
        viewModelScope.launch {
            studentIds.forEach { sId ->
                val student = students.value.find { it.id == sId }
                if (student != null) {
                    val updated = student.copy(groupId = groupId, lastUpdated = System.currentTimeMillis())
                    repository.addStudent(updated)
                    try {
                        com.example.util.FirebaseSafe.getCenterCollection("students")?.document(updated.id)?.set(updated)
                    } catch (e: Exception) {}
                }
            }
        }
    }

    // Quick Attendance Core Operations
    fun markAllAttendance(groupId: String, status: String, date: Long) {
        viewModelScope.launch {
            val groupStudents = students.value.filter { it.groupId == groupId && it.isActive }
            if (groupStudents.isEmpty()) return@launch

            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
            val monthStr = monthFormat.format(Date(date))

            val newAttendanceList = groupStudents.map { student ->
                Attendance(
                    studentId = student.id,
                    teacherId = student.teacherId,
                    date = date,
                    status = status,
                    month = monthStr,
                    notes = "حضور جماعي للمجموعة"
                )
            }
            repository.saveAttendance(newAttendanceList)
        }
    }

    fun toggleSingleStudentAttendance(studentId: String, status: String, date: Long) {
        viewModelScope.launch {
            val student = students.value.find { it.id == studentId } ?: return@launch
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
            val monthStr = monthFormat.format(Date(date))

            // Check if there is an existing attendance for this student on this exact day (approximate date comparison or identical timestamp)
            // Typically attendance matches by day. We can filter existing attendance for this day.
            val startOfDay = getStartOfDay(date)
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1

            val existing = attendance.value.find { 
                it.studentId == studentId && it.date in startOfDay..endOfDay
            }

            val record = if (existing != null) {
                existing.copy(status = status, date = date)
            } else {
                Attendance(
                    studentId = studentId,
                    teacherId = student.teacherId,
                    date = date,
                    status = status,
                    month = monthStr,
                    notes = "تسجيل سريع"
                )
            }
            repository.saveAttendance(listOf(record))
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
}
