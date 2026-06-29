package com.example.ui.academic

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import com.example.util.StudentAnalytics
import com.example.util.StudentAnalyticsEngine
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.reports.PdfGeneratorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPortalScreen(
    initialStudentId: String? = null,
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val examGrades by viewModel.examGrades.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val teachers by viewModel.teachers.collectAsState()
    val profile by viewModel.profile.collectAsState()

    var studentCodeInput by remember { mutableStateOf("") }
    var searchedStudentId by remember { mutableStateOf<String?>(null) }
    var matchingStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    var showMultipleMatchesDialog by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }

    val searchForStudent = { inputVal: String ->
        val code = inputVal.trim()
        if (code.isEmpty()) {
            Toast.makeText(context, "الرجاء إدخال كود أو اسم أو رقم هاتف الطالب", Toast.LENGTH_SHORT).show()
        } else {
            val found = students.filter {
                it.id.equals(code, ignoreCase = true) ||
                it.qrCode.equals(code, ignoreCase = true) ||
                it.parentCode.equals(code, ignoreCase = true) ||
                it.parentPhone == code ||
                it.studentPhone == code ||
                it.name.contains(code, ignoreCase = true) ||
                it.parentName.contains(code, ignoreCase = true)
            }
            
            if (found.isNotEmpty()) {
                if (found.size == 1) {
                    searchedStudentId = found.first().id
                    Toast.makeText(context, "تم العثور على الطالب بنجاح: ${found.first().name}", Toast.LENGTH_SHORT).show()
                } else {
                    matchingStudents = found
                    showMultipleMatchesDialog = true
                }
            } else {
                Toast.makeText(context, "البيانات المدخلة غير متطابقة مع أي طالب مسجل لدينا", Toast.LENGTH_LONG).show()
            }
        }
    }

    val activeStudentId = remember(initialStudentId, searchedStudentId) {
        initialStudentId?.ifEmpty { null } ?: searchedStudentId
    }

    val student = remember(students, activeStudentId) {
        students.find { it.id == activeStudentId || it.qrCode == activeStudentId }
    }

    val studentNotes by remember(student) {
        if (student != null) viewModel.getNotesForStudent(student.id) else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "بوابة المتابعة لأولياء الأمور 👨‍👩‍👦",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    if (student != null) {
                        IconButton(onClick = { Toast.makeText(context, "جاري فتح مركز التواصل...", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.Forum, contentDescription = "مركز التواصل", tint = PrimaryIndigoLight)
                        }
                        IconButton(onClick = { 
                            try {
                                val studentAttendance = attendance.filter { it.studentId == student.id }
                                val studentPayments = payments.filter { it.studentId == student.id }
                                val studentGroup = groups.find { it.id == student.groupId }
                                
                                val pdfFile = PdfGeneratorService.generateStudentReport(
                                    context = context,
                                    profile = profile,
                                    student = student,
                                    group = studentGroup,
                                    attendanceList = studentAttendance,
                                    payments = studentPayments
                                )
                                PdfGeneratorService.sharePdf(context, pdfFile)
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل إنشاء التقرير: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "تحميل التقرير الشهري", tint = PrimaryIndigoLight)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("parent_portal_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        if (student == null) {
            // Screen to search/enter student code
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .background(BackgroundDark)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(PrimaryIndigo.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .border(1.dp, PrimaryIndigoLight.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "متابعة مستوى الطالب",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "يرجى إدخال كود الطالب الخاص بكم لمتابعة الحضور، الغياب، الواجبات والدرجات بشكل فوري.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = studentCodeInput,
                            onValueChange = { studentCodeInput = it },
                            label = { Text("كود، اسم، هاتف الطالب أو ولي الأمر") },
                            placeholder = { Text("مثال: STU_1001 أو أحمد أو 010...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showCameraScanner = true }) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "تصوير ومسح كود الطالب بالكاميرا",
                                        tint = PrimaryIndigoLight
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    searchForStudent(studentCodeInput)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = BackgroundDark,
                                unfocusedContainerColor = BackgroundDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (showCameraScanner) {
                            com.example.ui.components.CameraQrScannerDialog(
                                onDismiss = { showCameraScanner = false },
                                onCodeScanned = { scannedCode ->
                                    studentCodeInput = scannedCode
                                    showCameraScanner = false
                                    Toast.makeText(context, "تم مسح الكود بنجاح!", Toast.LENGTH_SHORT).show()
                                    searchForStudent(scannedCode)
                                }
                            )
                        }

                        Button(
                            onClick = {
                                searchForStudent(studentCodeInput)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بحث ودخول البوابة", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick access list
                if (students.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "الوصول السريع لتجربة البوابة 🚀",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "اضغط على أي طالب بالأسفل للتجربة المباشرة واستعراض المتابعة والدراسات والمستحقات:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        students.take(3).forEach { stu ->
                            val matchedGroup = groups.find { it.id == stu.groupId }?.name ?: "بدون مجموعة"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchedStudentId = stu.id
                                        Toast.makeText(context, "تم الدخول باسم: ${stu.name}", Toast.LENGTH_SHORT).show()
                                    }
                                    .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryIndigo.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stu.name.firstOrNull()?.toString() ?: "ط",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryIndigoLight
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = stu.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${stu.grade} • $matchedGroup",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PrimaryIndigo.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "عرض الملف",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryIndigoLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Multiple matches selection dialog
            if (showMultipleMatchesDialog && matchingStudents.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showMultipleMatchesDialog = false },
                    title = {
                        Text(
                            "تحديد الطالب المطلوب",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "تم العثور على أكثر من طالب يطابق مدخلات البحث. يرجى اختيار الطالب المطلوب لمتابعة مستواه:",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(matchingStudents) { stu ->
                                    val matchedGroup = groups.find { it.id == stu.groupId }?.name ?: "بدون مجموعة"
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchedStudentId = stu.id
                                                showMultipleMatchesDialog = false
                                                matchingStudents = emptyList()
                                                Toast.makeText(context, "تم تحديد الطالب: ${stu.name}", Toast.LENGTH_SHORT).show()
                                            }
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stu.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Text(
                                                    text = "${stu.grade} • $matchedGroup",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary,
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                if (stu.parentPhone.isNotEmpty()) {
                                                    Text(
                                                        text = "رقم ولي الأمر: ${stu.parentPhone}",
                                                        fontSize = 10.sp,
                                                        color = TextTertiary,
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = null,
                                                tint = PrimaryIndigoLight
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMultipleMatchesDialog = false }) {
                            Text("إلغاء", color = ErrorColorLight)
                        }
                    },
                    containerColor = SurfaceDark,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        } else {
            // Student is loaded, display complete Parent Portal
            val analytics = remember(student, attendance, payments, exams, examGrades) {
                StudentAnalyticsEngine.calculateAnalytics(student, attendance, payments, exams, examGrades)
            }

            val studentGroup = remember(groups, student) {
                groups.find { it.id == student.groupId }
            }

            val studentTeacher = remember(teachers, student) {
                teachers.find { it.id == student.teacherId }
            }

            var selectedPortalTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Exams, 2: Attendance, 3: Payments

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {
                // Welcoming Student Banner
                item {
                    ParentPortalHeaderCard(
                        student = student,
                        group = studentGroup,
                        teacher = studentTeacher,
                        onSignOutPortal = {
                            searchedStudentId = null
                            studentCodeInput = ""
                        }
                    )
                }

                // Portal Section Selector Tabs
                item {
                    PortalTabsRow(
                        selectedTab = selectedPortalTab,
                        onTabSelected = { selectedPortalTab = it }
                    )
                }

                // Dynamic Sections
                when (selectedPortalTab) {
                    0 -> { // General Dashboard (Summary and Assignments)
                        item {
                            Text(
                                text = "مؤشرات أداء الطالب العامة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MiniSummaryCard(
                                    modifier = Modifier.weight(1f),
                                    title = "نسبة الحضور",
                                    value = "${analytics.attendanceRate.toInt()}%",
                                    icon = Icons.Default.FactCheck,
                                    color = SuccessColorLight
                                )
                                MiniSummaryCard(
                                    modifier = Modifier.weight(1f),
                                    title = "متوسط الدرجات",
                                    value = if (analytics.gradedExamsCount > 0) "${analytics.averageGrade.toInt()}%" else "—",
                                    icon = Icons.Default.Grade,
                                    color = PrimaryIndigoLight
                                )
                            }
                        }
                        
                        // Performance Indicator
                        item {
                            val performanceStatus = when {
                                analytics.averageGrade >= 85 && analytics.attendanceRate >= 90 -> "ممتاز"
                                analytics.averageGrade >= 65 && analytics.attendanceRate >= 70 -> "جيد"
                                analytics.averageGrade >= 50 -> "يحتاج متابعة"
                                else -> "معرض للتراجع"
                            }
                            
                            val performanceColor = when (performanceStatus) {
                                "ممتاز" -> SuccessColor
                                "جيد" -> PrimaryIndigo
                                "يحتاج متابعة" -> WarningColor
                                else -> ErrorColor
                            }
                            
                            val performanceIcon = when (performanceStatus) {
                                "ممتاز" -> Icons.Default.SentimentVerySatisfied
                                "جيد" -> Icons.Default.SentimentSatisfied
                                "يحتاج متابعة" -> Icons.Default.SentimentNeutral
                                else -> Icons.Default.SentimentDissatisfied
                            }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, performanceColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = performanceColor.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(performanceIcon, contentDescription = null, tint = performanceColor, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("التقييم العام للأداء", color = TextSecondary, fontSize = 12.sp)
                                        Text(performanceStatus, color = performanceColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Financial Summary Section in Dashboard
                        item {
                            val monthsEnrolled = remember(student.registrationDate) {
                                getMonthsCount(student.registrationDate)
                            }
                            val totalRequired = if (student.isExempt) 0.0 else student.netFee * monthsEnrolled
                            val totalPaid = analytics.totalPaymentsAmount
                            val totalRemaining = maxOf(0.0, totalRequired - totalPaid)
                            val curr = profile?.currency ?: "ج.م"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "الوضعية المالية للرسوم",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        FinancialPillItem(
                                            title = "الرسوم المقررة",
                                            value = "$totalRequired $curr",
                                            color = TextSecondary
                                        )
                                        FinancialPillItem(
                                            title = "إجمالي المدفوع",
                                            value = "$totalPaid $curr",
                                            color = SuccessColorLight
                                        )
                                        FinancialPillItem(
                                            title = "المتبقي للاستحقاق",
                                            value = "$totalRemaining $curr",
                                            color = if (totalRemaining > 0) ErrorColorLight else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Assignments Section
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الواجبات والمهام المدرسية 📝",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "مجموع: " + assignments.filter { it.groupId == student.groupId }.size,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        val studentAssignments = assignments.filter { it.groupId == student.groupId }
                        if (studentAssignments.isEmpty()) {
                            item {
                                EmptyPlaceholderCard(
                                    icon = Icons.Default.MenuBook,
                                    text = "لا توجد واجبات مسجلة للمجموعة حالياً."
                                )
                            }
                        } else {
                            items(studentAssignments) { assignment ->
                                AssignmentRowItem(assignment = assignment)
                            }
                        }
                    }

                    1 -> { // Exams & Grades
                        val studentGrades = examGrades.filter { it.studentId == student.id }
                        if (studentGrades.isEmpty()) {
                            item {
                                EmptyPlaceholderCard(
                                    icon = Icons.Default.EmojiEvents,
                                    text = "لا توجد نتائج اختبارات معلنة بعد."
                                )
                            }
                        } else {
                            items(studentGrades) { grade ->
                                val exam = exams.find { it.id == grade.examId }
                                ExamGradeRowItem(grade = grade, exam = exam)
                            }
                        }
                    }

                    2 -> { // Attendance & Absence
                        val studentAttendance = attendance.filter { it.studentId == student.id }
                            .sortedByDescending { it.date }
                        if (studentAttendance.isEmpty()) {
                            item {
                                EmptyPlaceholderCard(
                                    icon = Icons.Default.CalendarMonth,
                                    text = "لا توجد عمليات رصد حضور أو غياب."
                                )
                            }
                        } else {
                            items(studentAttendance) { record ->
                                AttendanceHistoryRowItem(record = record)
                            }
                        }
                    }

                    3 -> { // Payments & Fees
                        val studentPayments = payments.filter { it.studentId == student.id }
                            .sortedByDescending { it.date }
                        if (studentPayments.isEmpty()) {
                            item {
                                EmptyPlaceholderCard(
                                    icon = Icons.Default.Payments,
                                    text = "لم يتم تسجيل أي سند قبض أو دفعة."
                                )
                            }
                        } else {
                            items(studentPayments) { pay ->
                                PaymentHistoryRowItem(payment = pay)
                            }
                        }
                    }

                    4 -> { // Timeline
                        item {
                            val studentAttendance = attendance.filter { it.studentId == student.id }
                            val studentPayments = payments.filter { it.studentId == student.id }
                            val studentGrades = examGrades.filter { it.studentId == student.id }
                            StudentTimelineTabContent(
                                studentId = student.id,
                                attendance = studentAttendance,
                                payments = studentPayments,
                                examGrades = studentGrades,
                                exams = exams,
                                viewModel = viewModel
                            )
                        }
                    }
                    
                    5 -> { // Student Notes
                        if (studentNotes.isEmpty()) {
                            item {
                                EmptyPlaceholderCard(
                                    icon = Icons.Default.Note,
                                    text = "لا توجد ملاحظات مسجلة للطالب حالياً."
                                )
                            }
                        } else {
                            items(studentNotes) { note ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Note,
                                                    contentDescription = null,
                                                    tint = WarningColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "بواسطة: ${note.createdBy}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(note.createdAt))
                                            Text(
                                                text = dateStr,
                                                fontSize = 10.sp,
                                                color = TextTertiary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = note.note,
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentPortalHeaderCard(
    student: Student,
    group: Group?,
    teacher: Teacher?,
    onSignOutPortal: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = student.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = student.grade,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onSignOutPortal,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ErrorColor.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "تسجيل خروج البوابة",
                        tint = ErrorColorLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PortalInfoRow(icon = Icons.Default.Groups, label = "المجموعة:", value = group?.name ?: "غير محدد")
                    PortalInfoRow(icon = Icons.Default.School, label = "المعلم:", value = teacher?.name ?: "غير محدد")
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PortalInfoRow(icon = Icons.Default.Key, label = "كود المتابعة:", value = student.qrCode.ifEmpty { student.id.take(8).uppercase() })
                    PortalInfoRow(icon = Icons.Default.CalendarToday, label = "تاريخ الانضمام:", value = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(student.registrationDate)))
                }
            }
        }
    }
}

@Composable
fun PortalInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
        Text(text = label, color = TextTertiary, fontSize = 11.sp)
        Text(text = value, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PortalTabsRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabs = listOf("الرئيسية", "الدرجات", "الحضور", "الرسوم", "السجل الزمني", "الملاحظات")
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) PrimaryIndigo else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextSecondary
                )
            }
        }
    }
}

@Composable
fun MiniSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun FinancialPillItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun AssignmentRowItem(assignment: Assignment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryIndigo.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assignment.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (assignment.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = assignment.description,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "تسليم خلال:",
                    fontSize = 9.sp,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(assignment.dueDate)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningColor
                )
            }
        }
    }
}

@Composable
fun EmptyPlaceholderCard(icon: ImageVector, text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, color = TextTertiary, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun getMonthsCount(registrationDate: Long): Int {
    val startCal = Calendar.getInstance().apply { timeInMillis = registrationDate }
    val endCal = Calendar.getInstance()
    val yearsDiff = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
    val monthsDiff = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
    return maxOf(1, (yearsDiff * 12) + monthsDiff)
}
