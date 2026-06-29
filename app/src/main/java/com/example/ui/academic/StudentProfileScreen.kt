package com.example.ui.academic

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.model.*
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import com.example.util.EvaluationStatus
import com.example.util.StudentAnalytics
import com.example.util.StudentAnalyticsEngine
import com.example.util.StudentEvaluation
import com.example.util.StudentEvaluationService
import com.example.ui.reports.PdfGeneratorService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    studentId: String,
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val examGrades by viewModel.examGrades.collectAsState()
    val teachers by viewModel.teachers.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val studentTeacherCrossRefs by viewModel.studentTeacherCrossRefs.collectAsState()

    val student = remember(students, studentId) {
        students.find { it.id == studentId }
    }

    val studentTeacherIds = remember(studentTeacherCrossRefs, studentId) {
        studentTeacherCrossRefs.filter { it.studentId == studentId }.map { it.teacherId }
    }

    if (student == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("خطأ: لم يتم العثور على الطالب", color = ErrorColorLight, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)) {
                    Text("العودة للخلف", color = Color.White)
                }
            }
        }
        return
    }

    // Calculate Analytics & Evaluation using engine and service
    val analytics = remember(student, attendance, payments, exams, examGrades) {
        StudentAnalyticsEngine.calculateAnalytics(student, attendance, payments, exams, examGrades)
    }

    val evaluation = remember(analytics, student) {
        StudentEvaluationService.evaluateStudent(analytics, student)
    }

    var selectedSectionTab by remember { mutableStateOf(0) } // 0: Exams, 1: Attendance, 2: Payments
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الملف الشخصي والتحليل الذكي",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("profile_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة للخلف",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PrimaryIndigoLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Archive Warning Banner (Only shown if archived)
            if (student.isArchived) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, WarningColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = WarningColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = WarningColor, modifier = Modifier.size(20.dp))
                        Column {
                            Text("هذا الطالب مؤرشف حالياً 🗄️", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarningColor)
                            Text("تم نقل هذا الطالب للأرشيف مع نهاية العام الدراسي.", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Beautiful Sticky Tabs Row
            SectionTabsRow(
                selectedTab = selectedSectionTab,
                onTabSelected = { selectedSectionTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Tab Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                when (selectedSectionTab) {
                    0 -> { // Information & Analytics
                        item {
                            StudentHeaderCard(
                                student = student, 
                                teachers = teachers, 
                                studentTeacherIds = studentTeacherIds,
                                evaluation = evaluation,
                                onAddExtraSession = {
                                    val newCount = student.privateSessionsCount + 1
                                    val newTotal = newCount * student.privateSessionPrice
                                    val initialTeacherIds = if (studentTeacherIds.isNotEmpty()) studentTeacherIds else listOf(student.teacherId).filter { it.isNotBlank() }
                                    viewModel.updateStudent(
                                        student.id, student.name, student.parentName, student.parentPhone, student.studentPhone,
                                        student.grade, student.customCourse, student.teacherId, student.monthlyFee, student.discount,
                                        student.isExempt, student.notes, student.studentType, newCount, student.privateSessionPrice, newTotal,
                                        initialTeacherIds
                                    )
                                }
                            )
                        }
                        item {
                            EvaluationReasonBanner(evaluation = evaluation)
                        }
                        item {
                            Text(
                                text = "مؤشرات الأداء والتحليل الذكي",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                            MetricsGrid(analytics = analytics, isExempt = student.isExempt)
                        }
                        item {
                            StudentActionsCard(
                                student = student,
                                group = groups.find { it.id == student.groupId },
                                profile = profile,
                                studentPayments = payments.filter { it.studentId == student.id },
                                context = context,
                                onToggleArchive = { viewModel.toggleStudentArchive(student.id) }
                            )
                        }
                    }
                    1 -> { // Attendance History
                        val studentAttendance = attendance.filter { it.studentId == student.id }
                            .sortedByDescending { it.date }
                        if (studentAttendance.isEmpty()) {
                            item {
                                EmptySectionPlaceholder(
                                    icon = Icons.Default.CalendarToday,
                                    text = "لا توجد حركات حضور مسجلة لهذا الطالب."
                                )
                            }
                        } else {
                            items(studentAttendance) { record ->
                                AttendanceHistoryRowItem(record = record)
                            }
                        }
                    }
                    2 -> { // Payments / Fees
                        item {
                            val studentPayments = payments.filter { it.studentId == student.id }.sortedByDescending { it.date }
                            StudentPaymentsTabContent(student = student, payments = studentPayments, viewModel = viewModel)
                        }
                    }
                    3 -> { // Teachers
                        item {
                            StudentTeachersTabContent(
                                student = student,
                                teachers = teachers,
                                studentTeacherIds = studentTeacherIds,
                                viewModel = viewModel
                            )
                        }
                    }
                    4 -> { // Notes tab
                        item {
                            StudentNotesTabContent(studentId = student.id, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        val context = LocalContext.current
        val studentTeacherCrossRefs by viewModel.studentTeacherCrossRefs.collectAsState()
        val crossRefsForStudent = studentTeacherCrossRefs.filter { it.studentId == student.id }
        val studentTeacherIds = crossRefsForStudent.map { it.teacherId }
        val initialTeacherIds = if (studentTeacherIds.isNotEmpty()) studentTeacherIds else listOf(student.teacherId).filter { it.isNotBlank() }
        val initialTeacherFees = crossRefsForStudent.associate { it.teacherId to it.customFee.toString() }

        EditStudentDialog(
            student = student,
            teachers = teachers,
            initialTeacherIds = initialTeacherIds,
            initialTeacherFees = initialTeacherFees,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, parentName, parentPhone, studentPhone, grade, customCourse, teacherId, feeDouble, discDouble, isExempt, notes, type, privCount, privPrice, privTotal, teacherIds, teacherIdToFee ->
                viewModel.updateStudent(
                    id = student.id,
                    name = name,
                    parentName = parentName,
                    parentPhone = parentPhone,
                    studentPhone = studentPhone,
                    grade = grade,
                    customCourse = customCourse,
                    teacherId = teacherId,
                    monthlyFee = feeDouble,
                    discount = discDouble,
                    isExempt = isExempt,
                    notes = notes,
                    studentType = type,
                    privateSessionsCount = privCount,
                    privateSessionPrice = privPrice,
                    privateTotalAmount = privTotal,
                    teacherIds = teacherIds,
                    teacherIdToFee = teacherIdToFee,
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    },
                    onSuccess = {
                        showEditDialog = false
                    }
                )
            }
        )
    }
}

@Composable
fun StudentHeaderCard(
    student: Student,
    teachers: List<Teacher>,
    studentTeacherIds: List<String>,
    evaluation: StudentEvaluation,
    onAddExtraSession: () -> Unit
) {
    val teacherNames = if (studentTeacherIds.isNotEmpty()) {
        teachers.filter { studentTeacherIds.contains(it.id) }.map { it.name }.joinToString(" و ")
    } else {
        teachers.find { it.id == student.teacherId }?.name ?: "غير محدد"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = student.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${student.grade} • ${if (student.customCourse.isNotEmpty()) "كورس ${student.customCourse}" else "عام"}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                
                // Evaluation Status Pill
                val statusColor = Color(android.graphics.Color.parseColor(evaluation.colorHex))
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = evaluation.arabicStatusLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Student Info Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoDetailRow(icon = Icons.Default.Person, label = "ولي الأمر:", value = student.parentName.ifEmpty { "غير مسجل" })
                    InfoDetailRow(icon = Icons.Default.Phone, label = "هاتف ولي الأمر:", value = student.parentPhone.ifEmpty { "غير مسجل" })
                    InfoDetailRow(icon = Icons.Default.VpnKey, label = "كود ولي الأمر لربط التطبيق:", value = student.parentCode)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoDetailRow(icon = Icons.Default.School, label = "المعلمون:", value = teacherNames)
                    
                    if (student.studentType == "PRIVATE") {
                        InfoDetailRow(icon = Icons.Default.AttachMoney, label = "إجمالي المبلغ:", value = "${student.privateTotalAmount} ج.م")
                        InfoDetailRow(icon = Icons.Default.Analytics, label = "تفاصيل برايفت:", value = "${student.privateSessionsCount} حصص بـ ${student.privateSessionPrice}")
                    } else {
                        InfoDetailRow(icon = Icons.Default.AttachMoney, label = "الرسوم الشهرية:", value = "${student.netFee} ج.م")
                    }
                }
            }
            
            if (student.studentType == "PRIVATE") {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddExtraSession,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل حصة إضافية", color = PrimaryIndigoLight, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentDialog(
    student: Student,
    teachers: List<Teacher>,
    initialTeacherIds: List<String>,
    initialTeacherFees: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        parentName: String,
        parentPhone: String,
        studentPhone: String,
        grade: String,
        customCourse: String,
        teacherId: String,
        feeDouble: Double,
        discDouble: Double,
        isExempt: Boolean,
        notes: String,
        type: String,
        privCount: Int,
        privPrice: Double,
        privTotal: Double,
        teacherIds: List<String>,
        teacherIdToFee: Map<String, Double>
    ) -> Unit
) {
    var name by remember { mutableStateOf(student.name) }
    var parentName by remember { mutableStateOf(student.parentName) }
    var parentPhone by remember { mutableStateOf(student.parentPhone) }
    var studentPhone by remember { mutableStateOf(student.studentPhone) }
    var grade by remember { mutableStateOf(student.grade) }
    var customCourse by remember { mutableStateOf(student.customCourse) }
    var selectedTeacherIds by remember { mutableStateOf<List<String>>(initialTeacherIds) }
    var teacherFees by remember { mutableStateOf<Map<String, String>>(initialTeacherFees) }
    var monthlyFee by remember { mutableStateOf(student.monthlyFee.toString()) }
    var discount by remember { mutableStateOf(student.discount.toString()) }
    var notes by remember { mutableStateOf(student.notes) }
    var isExempt by remember { mutableStateOf(student.isExempt) }

    var studentType by remember { mutableStateOf(student.studentType) }
    var privateSessionsCount by remember { mutableStateOf(student.privateSessionsCount.toString()) }
    var privateSessionPrice by remember { mutableStateOf(student.privateSessionPrice.toString()) }
    var privateTotalAmount by remember { mutableStateOf(student.privateTotalAmount.toString()) }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var gradeDropdownExpanded by remember { mutableStateOf(false) }

    val gradesList = listOf(
        "الصف التمهيدي KG1",
        "الصف التمهيدي KG2",
        "الصف الأول الابتدائي",
        "الصف الثاني الابتدائي",
        "الصف الثالث الابتدائي",
        "الصف الرابع الابتدائي",
        "الصف الخامس الابتدائي",
        "الصف السادس الابتدائي",
        "الصف الأول الإعدادي",
        "الصف الثاني الإعدادي",
        "الصف الثالث الإعدادي",
        "الصف الأول الثانوي",
        "الصف الثاني الثانوي",
        "الصف الثالث الثانوي"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات الطالب", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطالب (إجباري) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("رقم هاتف ولي الأمر (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = studentPhone,
                    onValueChange = { studentPhone = it },
                    label = { Text("رقم هاتف الطالب (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("الصف الدراسي:", color = TextSecondary, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { gradeDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (grade.isNotEmpty()) grade else "اختر الصف الدراسي")
                    }
                    DropdownMenu(
                        expanded = gradeDropdownExpanded,
                        onDismissRequest = { gradeDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        gradesList.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g, color = TextPrimary) },
                                onClick = {
                                    grade = g
                                    gradeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customCourse,
                    onValueChange = { customCourse = it },
                    label = { Text("كورس إضافي بالسنتر (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Teachers selection (Multi select)
                Text("المعلمون المسؤولون (اختر معلم واحد أو أكثر) *:", color = TextSecondary, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { dropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedNames = teachers.filter { selectedTeacherIds.contains(it.id) }.map { it.name }
                        Text(
                            text = if (selectedNames.isNotEmpty()) selectedNames.joinToString(", ") else "اختر المعلمين",
                            color = TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .background(SurfaceDark)
                            .widthIn(max = 300.dp)
                    ) {
                        teachers.forEach { t ->
                            val isSelected = selectedTeacherIds.contains(t.id)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked == true) {
                                                    selectedTeacherIds = selectedTeacherIds + t.id
                                                    teacherFees = teacherFees + (t.id to "")
                                                    if (selectedTeacherIds.size == 1) {
                                                        monthlyFee = ""
                                                    }
                                                } else {
                                                    selectedTeacherIds = selectedTeacherIds - t.id
                                                    teacherFees = teacherFees - t.id
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(t.name, color = TextPrimary)
                                    }
                                },
                                onClick = {
                                    if (isSelected) {
                                        selectedTeacherIds = selectedTeacherIds - t.id
                                        teacherFees = teacherFees - t.id
                                    } else {
                                        selectedTeacherIds = selectedTeacherIds + t.id
                                        teacherFees = teacherFees + (t.id to "")
                                        if (selectedTeacherIds.size == 1) {
                                            monthlyFee = ""
                                        }
                                    }
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("تم الاختيار", color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = { dropdownExpanded = false }
                        )
                    }
                }

                Text("نوع الاشتراك:", color = TextSecondary, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { studentType = "GROUP" }) {
                        RadioButton(selected = studentType == "GROUP", onClick = { studentType = "GROUP" })
                        Text("مجموعة", color = TextPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { studentType = "PRIVATE" }) {
                        RadioButton(selected = studentType == "PRIVATE", onClick = { studentType = "PRIVATE" })
                        Text("برايفت", color = TextPrimary)
                    }
                }

                if (studentType == "PRIVATE") {
                    OutlinedTextField(
                        value = privateSessionsCount,
                        onValueChange = { 
                            privateSessionsCount = it 
                            val count = it.toIntOrNull() ?: 0
                            val price = privateSessionPrice.toDoubleOrNull() ?: 0.0
                            if (count > 0 && price > 0) {
                                privateTotalAmount = (count * price).toString()
                            }
                        },
                        label = { Text("عدد الحصص") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = privateSessionPrice,
                        onValueChange = { 
                            privateSessionPrice = it 
                            val count = privateSessionsCount.toIntOrNull() ?: 0
                            val price = it.toDoubleOrNull() ?: 0.0
                            if (count > 0 && price > 0) {
                                privateTotalAmount = (count * price).toString()
                            }
                        },
                        label = { Text("سعر الحصة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = privateTotalAmount,
                        onValueChange = { 
                            privateTotalAmount = it 
                            val total = it.toDoubleOrNull() ?: 0.0
                            val count = privateSessionsCount.toIntOrNull() ?: 0
                            if (count > 0 && total > 0) {
                                privateSessionPrice = (total / count).toString()
                            }
                        },
                        label = { Text("إجمالي المبلغ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("طالب معفي من الدفع", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("تفعيل هذا الخيار يلغي متطلبات الدفع لهذا الطالب", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isExempt,
                            onCheckedChange = { 
                                isExempt = it
                                if (it) {
                                    monthlyFee = "0.0"
                                }
                            }
                        )
                    }

                    // Monthly Fee / Amount paid (Disabled if exempt)
                    if (selectedTeacherIds.size > 1 && !isExempt) {
                        Text("الرسوم الشهرية لكل معلم (غير شامل الخصم):", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        selectedTeacherIds.forEach { tId ->
                            val teacher = teachers.find { it.id == tId }
                            if (teacher != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(teacher.name, modifier = Modifier.weight(1f), color = TextPrimary)
                                    OutlinedTextField(
                                        value = teacherFees[tId] ?: "",
                                        onValueChange = { newValue ->
                                            teacherFees = teacherFees + (tId to newValue)
                                        },
                                        placeholder = { Text("المبلغ") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(120.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = if (isExempt) "0.0 (معفي)" else monthlyFee,
                            onValueChange = { if (!isExempt) monthlyFee = it },
                            enabled = !isExempt,
                            label = { Text(if (isExempt) "المبلغ المدفوع (معفي)" else "الرسوم الشهرية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("خصم (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
         confirmButton = {
             Button(
                 onClick = {
                     if (name.isNotEmpty() && selectedTeacherIds.isNotEmpty()) {
                         val feeDouble = if (isExempt) 0.0 else (monthlyFee.toDoubleOrNull() ?: 0.0)
                         val discDouble = discount.toDoubleOrNull() ?: 0.0
                         val privCount = privateSessionsCount.toIntOrNull() ?: 0
                         val privPrice = privateSessionPrice.toDoubleOrNull() ?: 0.0
                         val privTotal = privateTotalAmount.toDoubleOrNull() ?: 0.0
                         
                         val teacherIdToFee = selectedTeacherIds.associateWith { tId ->
                             if (isExempt) 0.0 else {
                                 val customStr = teacherFees[tId]
                                 if (!customStr.isNullOrBlank()) {
                                     customStr.toDoubleOrNull() ?: 0.0
                                 } else {
                                     monthlyFee.toDoubleOrNull() ?: 0.0
                                 }
                             }
                         }

                         onConfirm(
                             name,
                             parentName,
                             parentPhone,
                             studentPhone,
                             grade,
                             customCourse,
                             selectedTeacherIds.firstOrNull() ?: "",
                             feeDouble,
                             discDouble,
                             isExempt,
                             notes,
                             studentType,
                             privCount,
                             privPrice,
                             privTotal,
                             selectedTeacherIds,
                             teacherIdToFee
                         )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                enabled = name.isNotEmpty() && selectedTeacherIds.isNotEmpty()
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun InfoDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(15.dp))
        Text(text = label, color = TextTertiary, fontSize = 11.sp)
        Text(text = value, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EvaluationReasonBanner(evaluation: StudentEvaluation) {
    val statusColor = Color(android.graphics.Color.parseColor(evaluation.colorHex))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (evaluation.status) {
                    EvaluationStatus.EXCELLENT -> Icons.Default.CheckCircle
                    EvaluationStatus.NEEDS_ATTENTION -> Icons.Default.Warning
                    EvaluationStatus.AT_RISK -> Icons.Default.Dangerous
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    text = "تقييم النظام الذكي",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = evaluation.reason,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun MetricsGrid(analytics: StudentAnalytics, isExempt: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "نسبة الحضور",
                value = "${analytics.attendanceRate.toInt()}%",
                subtitle = "${analytics.presentCount + analytics.lateCount} من ${analytics.totalAttendanceCount} حصص",
                icon = Icons.Default.FactCheck,
                color = SuccessColorLight
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "نسبة الغياب",
                value = "${analytics.absenceRate.toInt()}%",
                subtitle = "${analytics.absentCount} غيابات مسجلة",
                icon = Icons.Default.PersonOff,
                color = ErrorColorLight
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "متوسط الدرجات",
                value = if (analytics.gradedExamsCount > 0) "${analytics.averageGrade.toInt()}%" else "—",
                subtitle = if (analytics.gradedExamsCount > 0) "من ${analytics.gradedExamsCount} اختبارات" else "لا توجد اختبارات",
                icon = Icons.Default.Grade,
                color = PrimaryIndigoLight
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "الالتزام بالدفع",
                value = if (isExempt) "معفي" else "${analytics.paymentCommitmentRate.toInt()}%",
                subtitle = if (isExempt) "طالب مكفول بالكامل" else "متأخر: ${analytics.unpaidMonthsCount} شهور",
                icon = Icons.Default.Savings,
                color = WarningColor
            )
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = TextSecondary)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = TextTertiary)
        }
    }
}

@Composable
fun SectionTabsRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabs = listOf("البيانات", "الحضور", "الرسوم", "المدرسون", "الملاحظات")
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) PrimaryIndigo else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextSecondary
                )
            }
        }
    }
}

@Composable
fun ExamGradeRowItem(grade: ExamGrade, exam: Exam?) {
    val examName = exam?.name ?: "اختبار مجهول"
    val totalMarks = exam?.totalMarks ?: 100.0
    val percentage = if (totalMarks > 0) (grade.score / totalMarks) * 100.0 else 0.0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = examName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(grade.createdAt)),
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${grade.score} / ${totalMarks.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percentage >= 50.0) SuccessColorLight else ErrorColorLight
                )
                Text(
                    text = "نسبة: ${percentage.toInt()}%",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun AttendanceHistoryRowItem(record: Attendance) {
    val statusText = when (record.status.lowercase()) {
        "present" -> "حضور"
        "absent" -> "غياب"
        "late" -> "تأخير"
        "excused" -> "معذور"
        else -> record.status
    }
    
    val statusColor = when (record.status.lowercase()) {
        "present" -> SuccessColorLight
        "absent" -> ErrorColorLight
        "late" -> WarningColor
        "excused" -> PrimaryIndigoLight
        else -> TextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = SimpleDateFormat("EEEE, yyyy/MM/dd", Locale("ar")).format(Date(record.date)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                if (record.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "ملاحظة: ${record.notes}", fontSize = 11.sp, color = TextSecondary)
                }
            }
            
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun PaymentHistoryRowItem(payment: Payment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "دفع اشتراك شهر: ${payment.month}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(payment.date)),
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${payment.amount} ج.م",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuccessColorLight
                )
                Text(
                    text = "طريقة الدفع: " + if (payment.method == "cash") "كاش" else "تحويل",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun EmptySectionPlaceholder(icon: ImageVector, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, color = TextTertiary, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StudentActionsCard(
    student: Student,
    group: Group?,
    profile: Profile?,
    studentPayments: List<Payment>,
    context: Context,
    onToggleArchive: () -> Unit
) {
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var showPdfDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "الأدوات المالية وبوابة ولي الأمر 🛠️",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Generate Financial Statement PDF
                Button(
                    onClick = {
                        try {
                            val file = PdfGeneratorService.generateStudentFinancialStatement(
                                context = context,
                                profile = profile,
                                student = student,
                                group = group,
                                payments = studentPayments
                            )
                            generatedFile = file
                            showPdfDialog = true
                            Toast.makeText(context, "تم إنشاء كشف الحساب المالي كملف PDF", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "فشل إنشاء كشف الحساب: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("generate_financial_statement_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("كشف الحساب (PDF)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Share Portal Link via WhatsApp
                Button(
                    onClick = {
                        val portalLink = "https://centerplus.app/parent-portal?studentId=${student.id}"
                        val message = """
                            السلام عليكم ورحمة الله وبركاته،
                            ولي أمر الطالب: ${student.name}
                            يمكنكم الآن متابعة حضور وغياب، درجات، واجبات، والوضع المالي للطالب بشكل مباشر وفوري عبر بوابة ولي الأمر الخاصة بنا من خلال الرابط التالي:
                            $portalLink
                            كود الطالب: ${student.qrCode.ifEmpty { student.id }}
                            كود ولي الأمر لربط التطبيق: ${student.parentCode}
                        """.trimIndent()
                        
                        val phone = student.parentPhone.ifEmpty { student.studentPhone }
                        if (phone.isEmpty()) {
                            Toast.makeText(context, "لم يتم تسجيل رقم هاتف للتواصل", Toast.LENGTH_SHORT).show()
                        } else {
                            val formattedPhone = if (phone.startsWith("0")) "2$phone" else phone
                            val cleanPhone = formattedPhone.replace(Regex("[^0-9]"), "")
                            val url = "https://wa.me/$cleanPhone?text=${Uri.encode(message)}"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لم يتم العثور على تطبيق واتساب في هاتفك", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_portal_whatsapp_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بوابة المتابعة (واتساب)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onToggleArchive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("toggle_student_archive_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (student.isArchived) SuccessColor else ErrorColor.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (student.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (student.isArchived) "استرجاع الطالب من الأرشيف 📂" else "نقل الطالب إلى الأرشيف العام 🗄️",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showPdfDialog && generatedFile != null) {
        PdfActionsDialog(
            fileName = generatedFile!!.name,
            onDismiss = {
                showPdfDialog = false
                generatedFile = null
            },
            onSave = {
                generatedFile?.let {
                    PdfGeneratorService.savePdfToDownloads(context, it, "كشف_حساب_${student.name.replace(" ", "_")}.pdf")
                }
            },
            onShare = {
                generatedFile?.let {
                    PdfGeneratorService.sharePdf(context, it)
                }
            },
            onPrint = {
                generatedFile?.let {
                    PdfGeneratorService.printPdf(context, it, "كشف حساب ${student.name}")
                }
            }
        )
    }
}

@Composable
fun PdfActionsDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تم إنشاء كشف الحساب بنجاح 🎉",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = ErrorColorLight,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = fileName,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "اختر أحد الإجراءات التالية للتعامل مع ملف كشف الحساب المالي:",
                    fontSize = 12.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ في التنزيلات (Downloads)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة الملف", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onPrint,
                        colors = ButtonDefaults.buttonColors(containerColor = WarningColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة كشف الحساب", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("إغلاق وإلغاء", color = TextSecondary, fontSize = 14.sp)
                }
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun StudentNotesTabContent(studentId: String, viewModel: AppViewModel) {
    val notes by viewModel.getNotesForStudent(studentId).collectAsState(initial = emptyList())
    var noteText by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf("المشرف") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "إضافة ملاحظة جديدة 📝",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("اكتب الملاحظة هنا...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        label = { Text("بواسطة", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (noteText.trim().isNotEmpty()) {
                                viewModel.addStudentNote(studentId, noteText, authorName)
                                noteText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("حفظ الملاحظة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "الملاحظات المسجلة (${notes.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (notes.isEmpty()) {
            EmptySectionPlaceholder(
                icon = Icons.Default.NoteAlt,
                text = "لا توجد ملاحظات مسجلة لهذا الطالب حالياً."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notes.forEach { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PrimaryIndigoLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = note.createdBy,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigoLight
                                    )
                                    Text("•", color = TextTertiary)
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date(note.createdAt))
                                    Text(dateStr, fontSize = 10.sp, color = TextTertiary)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteStudentNote(note.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف الملاحظة",
                                        tint = ErrorColorLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = note.note,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TimelineEvent(
    val date: Long,
    val type: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun StudentTimelineTabContent(
    studentId: String,
    attendance: List<Attendance>,
    payments: List<Payment>,
    examGrades: List<ExamGrade>,
    exams: List<Exam>,
    viewModel: AppViewModel
) {
    val notes by viewModel.getNotesForStudent(studentId).collectAsState(initial = emptyList())
    val events = remember(attendance, payments, examGrades, exams, notes) {
        val timeline = mutableListOf<TimelineEvent>()
        
        // Attendance
        attendance.forEach { record ->
            val isPresent = record.status == "PRESENT" || record.status == "LATE"
            timeline.add(
                TimelineEvent(
                    date = record.date,
                    type = "ATTENDANCE",
                    title = if (isPresent) "حضور الحصة" else "غياب عن الحصة",
                    description = if (record.status == "LATE") "حضر متأخراً" else if (record.status == "EXCUSED") "غياب بعذر" else "",
                    icon = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    color = if (isPresent) SuccessColor else ErrorColorLight
                )
            )
        }
        
        // Payments
        payments.forEach { pay ->
            timeline.add(
                TimelineEvent(
                    date = pay.date,
                    type = "PAYMENT",
                    title = "دفع رسوم: ${pay.month}",
                    description = "المبلغ المدفوع: ${pay.amount}",
                    icon = Icons.Default.Payments,
                    color = PrimaryIndigoLight
                )
            )
        }
        
        // Exams
        examGrades.forEach { grade ->
            val exam = exams.find { it.id == grade.examId }
            val examName = exam?.name ?: "اختبار مجهول"
            val total = exam?.totalMarks ?: 100.0
            timeline.add(
                TimelineEvent(
                    date = grade.createdAt,
                    type = "EXAM",
                    title = "نتيجة اختبار: $examName",
                    description = "حصل على ${grade.score} / $total",
                    icon = Icons.Default.MenuBook,
                    color = WarningColor
                )
            )
        }
        
        // Notes
        notes.forEach { note ->
            timeline.add(
                TimelineEvent(
                    date = note.createdAt,
                    type = "NOTE",
                    title = "ملاحظة من ${note.createdBy}",
                    description = note.note,
                    icon = Icons.Default.NoteAlt,
                    color = TextSecondary
                )
            )
        }
        
        timeline.sortedByDescending { it.date }
    }

    if (events.isEmpty()) {
        EmptySectionPlaceholder(
            icon = Icons.Default.History,
            text = "لا توجد أحداث مسجلة في السجل الزمني لهذا الطالب."
        )
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            events.forEachIndexed { index, event ->
                val isLast = index == events.size - 1
                TimelineEventRow(event = event, isLast = isLast)
            }
        }
    }
}

@Composable
fun TimelineEventRow(event: TimelineEvent, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Timeline Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp).fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(event.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = event.icon,
                    contentDescription = null,
                    tint = event.color,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(BorderColor)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 24.dp)) {
            val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale("ar")).format(Date(event.date))
            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = event.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (event.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = event.description,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentPaymentsTabContent(student: Student, payments: List<Payment>, viewModel: AppViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الرسوم والمدفوعات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة دفعة", fontSize = 12.sp)
            }
        }

        if (payments.isEmpty()) {
             EmptySectionPlaceholder(icon = Icons.Default.Payments, text = "لا توجد عمليات دفع مسجلة لهذا الطالب.")
        } else {
             payments.forEach { pay ->
                  PaymentHistoryRowItem(payment = pay)
                  Spacer(modifier = Modifier.height(8.dp))
             }
        }
    }

    if (showAddDialog) {
        var amount by remember { mutableStateOf("") }
        var month by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())) }
        var method by remember { mutableStateOf("cash") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة دفعة مالية", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("المبلغ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        )
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = { Text("الشهر (YYYY-MM)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("cash" to "نقدي", "wallet" to "محفظة", "bank" to "بنكي").forEach { (id, name) ->
                            val isSelected = method == id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else SurfaceLightDark)
                                    .border(1.dp, if (isSelected) PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { method = id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(name, color = if (isSelected) PrimaryIndigoLight else TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    viewModel.addPayment(student.id, student.teacherId, amountDouble, month, method, "دفعة جديدة")
                    showAddDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء", color = TextSecondary) }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun StudentTeachersTabContent(
    student: Student,
    teachers: List<Teacher>,
    studentTeacherIds: List<String>,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    var showLinkDialog by remember { mutableStateOf(false) }
    
    val linkedTeachers = if (studentTeacherIds.isNotEmpty()) {
        teachers.filter { studentTeacherIds.contains(it.id) }
    } else {
        teachers.filter { it.id == student.teacherId }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("المعلمون المرتبطون", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Button(
                onClick = { showLinkDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ربط بمعلم", fontSize = 12.sp)
            }
        }

        if (linkedTeachers.isEmpty()) {
            EmptySectionPlaceholder(icon = Icons.Default.Group, text = "لا يوجد معلمون مرتبطون بهذا الطالب.")
        } else {
            linkedTeachers.forEach { teacher ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryIndigo.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigoLight)
                            }
                            Column {
                                Text(teacher.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(teacher.subject.ifEmpty { "غير محدد" }, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        IconButton(onClick = {
                            val newTeacherIds = linkedTeachers.map { it.id }.filter { it != teacher.id }
                            viewModel.updateStudent(
                                student.id, student.name, student.parentName, student.parentPhone, student.studentPhone,
                                student.grade, student.customCourse, newTeacherIds.firstOrNull() ?: "", student.monthlyFee, student.discount,
                                student.isExempt, student.notes, student.studentType, student.privateSessionsCount, student.privateSessionPrice, student.privateTotalAmount,
                                newTeacherIds
                            )
                        }) {
                            Icon(Icons.Default.LinkOff, contentDescription = "إلغاء الربط", tint = ErrorColorLight)
                        }
                    }
                }
            }
        }
    }

    if (showLinkDialog) {
        val availableTeachers = teachers.filter { !linkedTeachers.contains(it) }
        
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("ربط الطالب بمعلم جديد", color = TextPrimary) },
            text = {
                if (availableTeachers.isEmpty()) {
                    Text("جميع المعلمين مضافون بالفعل.", color = TextSecondary)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(availableTeachers) { teacher ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val newTeacherIds = linkedTeachers.map { it.id } + teacher.id
                                    viewModel.updateStudent(
                                        student.id, student.name, student.parentName, student.parentPhone, student.studentPhone,
                                        student.grade, student.customCourse, student.teacherId, student.monthlyFee, student.discount,
                                        student.isExempt, student.notes, student.studentType, student.privateSessionsCount, student.privateSessionPrice, student.privateTotalAmount,
                                        newTeacherIds
                                    )
                                    showLinkDialog = false
                                }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
                                Text(teacher.name, color = TextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text("إغلاق", color = TextSecondary) }
            },
            containerColor = SurfaceDark
        )
    }
}