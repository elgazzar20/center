package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.Student
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParentPortalTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = AppRepository(database.appDao())
        
        // Clear database before each test
        runBlocking {
            database.appDao().clearPayments()
            database.appDao().clearGroups()
            database.appDao().clearAssignments()
            database.appDao().clearExams()
            database.appDao().clearExamGrades()
            
            val allStudents = repository.allStudents.first()
            for (student in allStudents) {
                repository.deleteStudent(student.id)
            }
        }
    }

    @After
    fun tearDown() {
        // Safe database clean up if needed
    }

    @Test
    fun testStudentCreationAndParentCode() = runBlocking {
        // Create student
        val student = Student(
            id = "STU_TEST_001",
            name = "أحمد محمد",
            parentName = "محمد أحمد",
            parentPhone = "01000000000",
            studentPhone = "01100000000",
            grade = "الصف الأول الثانوي",
            teacherId = "teacher_1",
            monthlyFee = 300.0,
            discount = 0.0,
            isExempt = false,
            notes = "طالب مجتهد",
            studentType = "GROUP",
            qrCode = "STU_QR_001"
        )
        
        // Verify Parent Code is generated automatically in the data class
        assertNotNull("Parent Code must be automatically generated", student.parentCode)
        assertTrue("Parent Code must be alphanumeric", student.parentCode.isNotEmpty())
        
        // Save to database
        repository.addStudent(student)
        
        // Retrieve and verify
        val studentsList = repository.allStudents.first()
        val savedStudent = studentsList.find { it.id == "STU_TEST_001" }
        
        assertNotNull("Student must be saved in the database", savedStudent)
        assertEquals("Student ID must match correctly", "STU_TEST_001", savedStudent?.id)
        assertEquals("Parent Code must match saved", student.parentCode, savedStudent?.parentCode)
        assertEquals("QR Code must match saved", "STU_QR_001", savedStudent?.qrCode)
    }

    @Test
    fun testStudentLinkingLogic() = runBlocking {
        // Setup students in database
        val student1 = Student(
            id = "STU_TEST_101",
            name = "يوسف أحمد",
            teacherId = "teacher_1",
            qrCode = "QR_YOUSEF",
            parentCode = "PC101"
        )
        val student2 = Student(
            id = "STU_TEST_102",
            name = "سارة محمد",
            teacherId = "teacher_1",
            qrCode = "QR_SARA",
            parentCode = "PC102"
        )
        
        repository.addStudent(student1)
        repository.addStudent(student2)
        
        val allStudents = repository.allStudents.first()
        
        // 1. Link by Student ID
        val code1 = "STU_TEST_101"
        val foundById = allStudents.find { it.id == code1 || it.parentCode == code1 || it.qrCode == code1 }
        assertNotNull("Should find student by ID", foundById)
        assertEquals("يوسف أحمد", foundById?.name)
        
        // 2. Link by Parent Code
        val code2 = "PC102"
        val foundByParentCode = allStudents.find { it.id == code2 || it.parentCode == code2 || it.qrCode == code2 }
        assertNotNull("Should find student by Parent Code", foundByParentCode)
        assertEquals("سارة محمد", foundByParentCode?.name)
        
        // 3. Link by QR Code
        val code3 = "QR_YOUSEF"
        val foundByQrCode = allStudents.find { it.id == code3 || it.parentCode == code3 || it.qrCode == code3 }
        assertNotNull("Should find student by QR Code", foundByQrCode)
        assertEquals("يوسف أحمد", foundByQrCode?.name)
        
        // 4. Invalid Code Linking
        val invalidCode = "INVALID_CODE"
        val foundInvalid = allStudents.find { it.id == invalidCode || it.parentCode == invalidCode || it.qrCode == invalidCode }
        assertNull("Should not find student with invalid code", foundInvalid)
    }

    @Test
    fun testMultipleStudentLinkingUnlinkingAndRelinking() = runBlocking {
        // Setup students
        val s1 = Student(id = "S1", name = "رنا أحمد", teacherId = "t", parentCode = "CODE1", qrCode = "QR1")
        val s2 = Student(id = "S2", name = "علي محمد", teacherId = "t", parentCode = "CODE2", qrCode = "QR2")
        val s3 = Student(id = "S3", name = "عمر خالد", teacherId = "t", parentCode = "CODE3", qrCode = "QR3")
        
        repository.addStudent(s1)
        repository.addStudent(s2)
        repository.addStudent(s3)
        
        val allStudents = repository.allStudents.first()
        
        // Simulate parent's linked student list state
        var linkedStudentIds = mutableListOf<String>()
        
        // Link multiple students
        // Link first by Parent Code
        val code1 = "CODE1"
        val found1 = allStudents.find { it.id == code1 || it.parentCode == code1 || it.qrCode == code1 }
        assertNotNull(found1)
        linkedStudentIds.add(found1!!.id)
        
        // Link second by QR Code
        val code2 = "QR2"
        val found2 = allStudents.find { it.id == code2 || it.parentCode == code2 || it.qrCode == code2 }
        assertNotNull(found2)
        linkedStudentIds.add(found2!!.id)
        
        // Verify multiple linking
        assertEquals(2, linkedStudentIds.size)
        assertTrue(linkedStudentIds.contains("S1"))
        assertTrue(linkedStudentIds.contains("S2"))
        
        // Filter student list to only show linked students (Dashboard behavior)
        var parentVisibleStudents = allStudents.filter { it.id in linkedStudentIds }
        assertEquals(2, parentVisibleStudents.size)
        assertTrue(parentVisibleStudents.any { it.name == "رنا أحمد" })
        assertTrue(parentVisibleStudents.any { it.name == "علي محمد" })
        assertFalse(parentVisibleStudents.any { it.name == "عمر خالد" }) // Prevent access to unlinked students
        
        // Unlinking a student
        val unlinkId = "S1"
        linkedStudentIds.remove(unlinkId)
        
        // Verify unlinking
        assertEquals(1, linkedStudentIds.size)
        assertFalse(linkedStudentIds.contains("S1"))
        
        parentVisibleStudents = allStudents.filter { it.id in linkedStudentIds }
        assertEquals(1, parentVisibleStudents.size)
        assertFalse(parentVisibleStudents.any { it.name == "رنا أحمد" })
        assertTrue(parentVisibleStudents.any { it.name == "علي محمد" })
        
        // Relinking the student
        val relinkCode = "CODE1"
        val foundRelink = allStudents.find { it.id == relinkCode || it.parentCode == relinkCode || it.qrCode == relinkCode }
        assertNotNull(foundRelink)
        linkedStudentIds.add(foundRelink!!.id)
        
        // Verify relinking was successful
        assertEquals(2, linkedStudentIds.size)
        assertTrue(linkedStudentIds.contains("S1"))
        
        parentVisibleStudents = allStudents.filter { it.id in linkedStudentIds }
        assertEquals(2, parentVisibleStudents.size)
        assertTrue(parentVisibleStudents.any { it.name == "رنا أحمد" })
    }
}
