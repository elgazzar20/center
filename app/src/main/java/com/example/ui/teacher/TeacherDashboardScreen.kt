package com.example.ui.teacher

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import com.example.ui.center.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    onLogout: () -> Unit,
    viewModel: AppViewModel = viewModel(),
    onAiAssistantClick: (() -> Unit)? = null,
    onNavigateToSchedule: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeGroupIdForAttendance by remember { mutableStateOf<String?>(null) }
    var selectedStudentIdForProfile by remember { mutableStateOf<String?>(null) }
    
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val studentTeacherCrossRefs by viewModel.studentTeacherCrossRefs.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendance.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Dialog state controllers
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }

    // Individual teacher context always links their students to "me" teacherId
    val teacherId = "me"

    val teacherStudents = students.filter { s ->
        studentTeacherCrossRefs.any { it.studentId == s.id && it.teacherId == teacherId } || (s.teacherId == teacherId && studentTeacherCrossRefs.none { it.studentId == s.id })
    }
    val activeTeacherStudents = remember(students, studentTeacherCrossRefs) {
        students.filter { s ->
            (studentTeacherCrossRefs.any { it.studentId == s.id && it.teacherId == teacherId } || (s.teacherId == teacherId && studentTeacherCrossRefs.none { it.studentId == s.id })) && !s.isArchived
        }
    }
    val teacherPayments = payments.filter { it.teacherId == teacherId }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(PrimaryIndigo.copy(alpha = 0.1f)).padding(6.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مرحباً بك، أستاذ",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = profile?.name ?: "مستخدم",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                },
                actions = {
                    if (onAiAssistantClick != null) {
                        IconButton(
                            onClick = onAiAssistantClick,
                            modifier = Modifier.testTag("ai_assistant_btn_top")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "المساعد الذكي", tint = WarningColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    scrolledContainerColor = SurfaceDark
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            TeacherBottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
        containerColor = BackgroundDark,
        floatingActionButton = {
            when (selectedTab) {
                1 -> {
                    FloatingActionButton(
                        onClick = { showAddStudentDialog = true },
                        containerColor = PrimaryIndigo,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة طالب")
                    }
                }
                2 -> {
                    FloatingActionButton(
                        onClick = { selectedTab = 11 },
                        containerColor = SuccessColor,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("scan_qr_fab")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح رمز الحضور QR")
                    }
                }
                3 -> {
                    FloatingActionButton(
                        onClick = { showAddPaymentDialog = true },
                        containerColor = PrimaryIndigo,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة دفعة")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> TeacherWorkspaceTab(
                            students = activeTeacherStudents,
                            groups = viewModel.groups.collectAsStateWithLifecycle().value,
                            attendanceList = attendanceList,
                            exams = viewModel.exams.collectAsStateWithLifecycle().value,
                            examGrades = viewModel.examGrades.collectAsStateWithLifecycle().value,
                            assignments = viewModel.assignments.collectAsStateWithLifecycle().value,
                            onNavigateToGroups = { selectedTab = 5 },
                            onNavigateToStudents = { selectedTab = 1 },
                            onNavigateToAssignments = { selectedTab = 9 },
                            onNavigateToExams = { selectedTab = 10 },
                            onNavigateToSchedule = onNavigateToSchedule
                        )
                        1 -> TeacherStudentsTab(
                            students = teacherStudents,
                            studentTeacherCrossRefs = studentTeacherCrossRefs,
                            payments = teacherPayments,
                            onDelete = { viewModel.deleteStudent(it) },
                            onWhatsApp = { phone, name ->
                                val template = "مرحباً يا فندم، نود إعلامكم بخصوص الطالب/ة $name..."
                                sendWhatsAppMessage(context, phone, template)
                            },
                            viewModel = viewModel,
                            onNavigateToQrScanner = { selectedTab = 11 },
                            onNavigateToStudentProfile = { studentId ->
                                selectedStudentIdForProfile = studentId
                                selectedTab = 13
                            },
                            onNavigateToAtRisk = { selectedTab = 14 }
                        )
                        11 -> com.example.ui.academic.QRScannerScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 1 }
                        )
                        2 -> TeacherAttendanceTab(
                            students = activeTeacherStudents,
                            attendanceList = attendanceList,
                            onSave = { batch -> viewModel.saveAttendanceBatch(batch) },
                            onWhatsApp = { phone, name ->
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val template = "نود إعلامكم بأن الطالب/ة $name قد تغيب عن حصة المراجعة اليوم بتاريخ $dateStr."
                                sendWhatsAppMessage(context, phone, template)
                            }
                        )
                        3 -> TeacherFinanceTab(
                            payments = teacherPayments,
                            expenses = expenses,
                            students = activeTeacherStudents,
                            onAddPaymentClick = { showAddPaymentDialog = true },
                            onDeletePayment = { viewModel.deletePayment(it) },
                            viewModel = viewModel
                        )
                        4 -> SettingsTab(
                            profile = profile,
                            onUpdateProfile = { name, phone, sysType, centerName, whatsapp, currency ->
                                viewModel.updateProfile(name, phone, sysType, centerName, whatsapp, currency)
                                Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            onLogout = onLogout,
                            onNavigateToTeachers = {
                                Toast.makeText(context, "هذه الميزة غير متوفرة في حساب المعلم", Toast.LENGTH_SHORT).show()
                            },
                            onNavigateToGroups = { selectedTab = 5 },
                            onNavigateToReports = { selectedTab = 7 },
                            onNavigateToAssignments = { selectedTab = 9 },
                            onNavigateToExams = { selectedTab = 10 },
                            onNavigateToAchievements = { selectedTab = 14 },
                            onNavigateToAtRisk = { selectedTab = 13 },
                            onNavigateToBackup = { selectedTab = 8 },
                            onNavigateToCommunicationCenter = {
                                Toast.makeText(context, "هذه الميزة غير متوفرة في حساب المعلم", Toast.LENGTH_SHORT).show()
                            },
                            onNavigateToClassrooms = {
                                Toast.makeText(context, "هذه الميزة غير متوفرة في حساب المعلم", Toast.LENGTH_SHORT).show()
                            },
                            onNavigateToCenterSchedule = { onNavigateToSchedule() }
                        )
                        5 -> com.example.ui.groups.GroupsScreen(
                            onBackClick = { selectedTab = 0 },
                            onNavigateToQuickAttendance = { groupId ->
                                activeGroupIdForAttendance = groupId
                                selectedTab = 6
                            }
                        )
                        6 -> com.example.ui.groups.QuickAttendanceScreen(
                            initialGroupId = activeGroupIdForAttendance,
                            onBackClick = { selectedTab = 5 }
                        )
                        7 -> com.example.ui.reports.ReportsScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 0 },
                            onAiAssistantClick = onAiAssistantClick
                        )
                        8 -> com.example.ui.backup.BackupScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 4 }
                        )
                        9 -> com.example.ui.academic.AssignmentsScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 0 }
                        )
                        10 -> com.example.ui.academic.ExamsScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 0 }
                        )
                        13 -> com.example.ui.academic.StudentProfileScreen(
                            studentId = selectedStudentIdForProfile ?: "",
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 1 }
                        )
                        14 -> com.example.ui.academic.AtRiskStudentsScreen(
                            viewModel = viewModel,
                            onNavigateToStudentProfile = { studentId ->
                                selectedStudentIdForProfile = studentId
                                selectedTab = 13
                            },
                            onBackClick = { selectedTab = 0 }
                        )
                        15 -> com.example.ui.academic.AchievementsScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 0 }
                        )
                    }
                }
            }

            // Dialogs
            if (showAddStudentDialog) {
                AddStudentDialog(
                    teachers = listOf(Teacher(id = "me", name = "أنا المعلم", subject = "مادتي")),
                    onDismiss = { showAddStudentDialog = false },
                    onConfirm = { name, parentName, parentPhone, studentPhone, grade, customCourse, _, fee, discount, isExempt, notes, type, count, price, total, teacherIds, teacherIdToFee, onSuccess, onError ->
                        viewModel.addStudent(
                            name = name, parentName = parentName, parentPhone = parentPhone, studentPhone = studentPhone, grade = grade,
                            customCourse = customCourse, teacherId = "me", monthlyFee = fee, discount = discount, isExempt = isExempt, notes = notes,
                            studentType = type, privateSessionsCount = count, privateSessionPrice = price, privateTotalAmount = total,
                            teacherIds = if (teacherIds.isEmpty()) listOf("me") else teacherIds,
                            teacherIdToFee = teacherIdToFee,
                            onError = onError,
                            onSuccess = {
                                showAddStudentDialog = false
                                onSuccess()
                            }
                        )
                    }
                )
            }

            if (showAddPaymentDialog) {
                AddPaymentDialog(
                    students = teacherStudents,
                    teachers = listOf(Teacher(id = "me", name = "أنا المعلم", subject = "مادتي")),
                    onDismiss = { showAddPaymentDialog = false },
                    onConfirm = { studentId, _, amount, month, method, notes ->
                        viewModel.addPayment(studentId, "me", amount, month, method, notes)
                        showAddPaymentDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun TeacherTopHeader(profile: Profile?, onAiAssistantClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(PrimaryIndigo, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = profile?.name?.ifEmpty { "المعلم الفردي" } ?: "المعلم الفردي",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "حساب المعلم المستقل",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onAiAssistantClick != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryIndigo.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .border(1.dp, PrimaryIndigoLight.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onAiAssistantClick() }
                        .testTag("teacher_dashboard_ai_assistant_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Nexora AI Assistant",
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceDark, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TeacherBottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLightDark)
            .border(1.dp, BorderColor)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(icon = Icons.Default.Dashboard, label = "الرئيسية", isSelected = selectedTab == 0, onClick = { onTabSelected(0) })
        BottomNavItem(icon = Icons.Default.School, label = "الطلاب", isSelected = selectedTab == 1, onClick = { onTabSelected(1) })
        BottomNavItem(icon = Icons.Default.Groups, label = "المجموعات", isSelected = selectedTab == 5 || selectedTab == 6, onClick = { onTabSelected(5) })
        BottomNavItem(icon = Icons.Default.EventAvailable, label = "الحضور", isSelected = selectedTab == 2, onClick = { onTabSelected(2) })
        BottomNavItem(icon = Icons.Default.Payments, label = "المالية", isSelected = selectedTab == 3, onClick = { onTabSelected(3) })
        BottomNavItem(icon = Icons.Default.Settings, label = "الإعدادات", isSelected = selectedTab == 4, onClick = { onTabSelected(4) })
    }
}

@Composable
fun TeacherWorkspaceTab(
    students: List<Student>,
    groups: List<Group>,
    attendanceList: List<Attendance>,
    exams: List<Exam>,
    examGrades: List<ExamGrade>,
    assignments: List<Assignment>,
    onNavigateToGroups: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToSchedule: () -> Unit = {}
) {
    val totalStudents = students.size
    val totalAttendance = attendanceList.size
    val presentCount = attendanceList.count { it.status == "present" }
    val attendanceRate = if (totalAttendance > 0) (presentCount.toFloat() / totalAttendance * 100).toInt() else 0
    val avgGrade = if (examGrades.isNotEmpty()) examGrades.map { it.score }.average().toInt() else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "نظرة عامة على الأداء",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Group,
                    iconColor = PrimaryIndigoLight,
                    badgeText = "طالب",
                    badgeColor = PrimaryIndigoLight,
                    badgeBg = PrimaryIndigo.copy(alpha = 0.1f),
                    value = totalStudents.toString(),
                    label = "عدد الطلاب"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconColor = SuccessColorLight,
                    badgeText = "%",
                    badgeColor = SuccessColorLight,
                    badgeBg = SuccessColor.copy(alpha = 0.1f),
                    value = attendanceRate.toString(),
                    label = "نسبة الحضور"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Grade,
                    iconColor = WarningColor,
                    badgeText = "%",
                    badgeColor = WarningColor,
                    badgeBg = WarningColor.copy(alpha = 0.1f),
                    value = avgGrade.toString(),
                    label = "متوسط الدرجات"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Class,
                    iconColor = PrimaryIndigoLight,
                    badgeText = "مجموعة",
                    badgeColor = PrimaryIndigoLight,
                    badgeBg = PrimaryIndigo.copy(alpha = 0.1f),
                    value = groups.size.toString(),
                    label = "إجمالي المجموعات"
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text(
                text = "الوصول السريع",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAccessCard(
                    icon = Icons.Default.DateRange,
                    title = "نظام الجدولة الذكي",
                    subtitle = "إدارة الحصص، المراجعات والمواعيد",
                    onClick = onNavigateToSchedule
                )
                QuickAccessCard(
                    icon = Icons.Default.Event,
                    title = "إدارة المجموعات",
                    subtitle = "تصفح المجموعات",
                    onClick = onNavigateToGroups
                )
                QuickAccessCard(
                    icon = Icons.Default.Assignment,
                    title = "الواجبات (${assignments.size})",
                    subtitle = "متابعة وتقييم الواجبات",
                    onClick = onNavigateToAssignments
                )
                QuickAccessCard(
                    icon = Icons.Default.Quiz,
                    title = "الامتحانات (${exams.size})",
                    subtitle = "إدارة وتقييم الامتحانات",
                    onClick = onNavigateToExams
                )
                QuickAccessCard(
                    icon = Icons.Default.People,
                    title = "قائمة الطلاب",
                    subtitle = "إدارة وتسجيل الطلاب",
                    onClick = onNavigateToStudents
                )
            }
        }
    }
}

@Composable
fun QuickAccessCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimaryIndigo.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun TeacherStudentsTab(
    students: List<Student>,
    studentTeacherCrossRefs: List<com.example.data.model.StudentTeacherCrossRef>,
    payments: List<Payment>,
    onDelete: (String) -> Unit,
    onWhatsApp: (String, String) -> Unit,
    viewModel: AppViewModel,
    onNavigateToQrScanner: () -> Unit,
    onNavigateToStudentProfile: (String) -> Unit,
    onNavigateToAtRisk: () -> Unit
) {
    StudentsTab(
        students = students,
        teachers = listOf(Teacher(id = "me", name = "أنا", subject = "مادتي")),
        studentTeacherCrossRefs = studentTeacherCrossRefs,
        payments = payments,
        onDelete = onDelete,
        onWhatsApp = onWhatsApp,
        viewModel = viewModel,
        onNavigateToQrScanner = onNavigateToQrScanner,
        onNavigateToStudentProfile = onNavigateToStudentProfile,
        onNavigateToAtRisk = onNavigateToAtRisk
    )
}

@Composable
fun TeacherAttendanceTab(
    students: List<Student>,
    attendanceList: List<Attendance>,
    onSave: (List<Attendance>) -> Unit,
    onWhatsApp: (String, String) -> Unit
) {
    val formattedToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayTimestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(formattedToday)?.time ?: System.currentTimeMillis()

    val attendanceMap = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(students) {
        attendanceMap.clear()
        students.forEach { student ->
            val existing = attendanceList.find { it.studentId == student.id && it.date == todayTimestamp }
            attendanceMap[student.id] = existing?.status ?: "present"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "حضور طلابي اليومي",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (students.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("لا يوجد طلاب مسجلون لعرضهم.", color = TextTertiary, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(students) { student ->
                    val status = attendanceMap[student.id] ?: "present"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = student.grade,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AttendanceButton(
                                    label = "حاضر",
                                    isSelected = status == "present",
                                    activeColor = SuccessColor,
                                    onClick = { attendanceMap[student.id] = "present" }
                                )
                                AttendanceButton(
                                    label = "غائب",
                                    isSelected = status == "absent",
                                    activeColor = ErrorColor,
                                    onClick = { attendanceMap[student.id] = "absent" }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val batch = students.map { student ->
                            Attendance(
                                studentId = student.id,
                                teacherId = "me",
                                date = todayTimestamp,
                                status = attendanceMap[student.id] ?: "present",
                                month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                            )
                        }
                        onSave(batch)
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("حفظ الحضور", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Button(
                    onClick = {
                        val absentStudents = students.filter { attendanceMap[it.id] == "absent" }
                        absentStudents.forEach { student ->
                            if (student.parentPhone.isNotEmpty()) {
                                onWhatsApp(student.parentPhone, student.name)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.SendToMobile, contentDescription = null, modifier = Modifier.size(18.dp), tint = SuccessColorLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إخطار الغائبين", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun TeacherFinanceTab(
    payments: List<Payment>,
    expenses: List<Expense>,
    students: List<Student>,
    onAddPaymentClick: () -> Unit,
    onDeletePayment: (String) -> Unit,
    viewModel: AppViewModel
) {
    var currentScreen by remember { mutableStateOf("dashboard") }
    FinanceTab(
        payments = payments,
        expenses = expenses,
        students = students,
        teachers = listOf(Teacher(id = "me", name = "أنا المعلم", subject = "مادتي")),
        onAddPaymentClick = onAddPaymentClick,
        onAddExpenseClick = {},
        onDeletePayment = onDeletePayment,
        onDeleteExpense = {},
        viewModel = viewModel,
        currentScreen = currentScreen,
        onScreenChange = { currentScreen = it }
    )
}
