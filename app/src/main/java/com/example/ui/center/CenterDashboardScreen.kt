package com.example.ui.center

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterDashboardScreen(
    onLogout: () -> Unit,
    viewModel: AppViewModel = viewModel(),
    onAiAssistantClick: (() -> Unit)? = null,
    onCommunicationCenterClick: (() -> Unit)? = null,
    onNavigateToClassrooms: (() -> Unit)? = null,
    onNavigateToCenterSchedule: (() -> Unit)? = null,
    onNavigateToUserManagement: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var financeScreen by remember { mutableStateOf("dashboard") }
    var activeGroupIdForAttendance by remember { mutableStateOf<String?>(null) }
    var selectedStudentIdForProfile by remember { mutableStateOf<String?>(null) }
    
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val studentTeacherCrossRefs by viewModel.studentTeacherCrossRefs.collectAsStateWithLifecycle()
    val activeStudents = remember(students) { students.filter { !it.isArchived } }
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendance.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Dialog state controllers
    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var showEditTeacherDialog by remember { mutableStateOf(false) }
    var selectedTeacherToEdit by remember { mutableStateOf<Teacher?>(null) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var showFabBottomSheet by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (selectedTab == 0) {
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
                                    text = "مرحباً بك،",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = profile?.centerName ?: "مستخدم",
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
            } else {
                val title = when (selectedTab) {
                    1 -> "إدارة المعلمين"
                    2 -> "إدارة الطلاب"
                    3 -> "سجل الحضور"
                    4 -> "الإدارة المالية"
                    5 -> "الإعدادات"
                    6 -> "إدارة المجموعات"
                    7 -> "جدول الحصص"
                    8 -> "التقارير"
                    9 -> "النسخ الاحتياطي"
                    10 -> "الامتحانات"
                    11 -> "التواصل"
                    12 -> "الماسح الضوئي"
                    13 -> "ملف الطالب"
                    14 -> "المستخدمون والصلاحيات"
                    15 -> "التقارير الأكاديمية"
                    else -> ""
                }
                
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { selectedTab = 0 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الرئيسية")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BackgroundDark,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = TextPrimary
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { 
                selectedTab = it 
                financeScreen = "dashboard"
            })
        },
        containerColor = BackgroundDark,
        floatingActionButton = {
            val shouldShowFab = selectedTab in listOf(0, 1, 2, 3, 5, 6, 8, 10, 11, 14, 15) || 
                               (selectedTab == 4 && financeScreen == "dashboard")
            if (shouldShowFab) {
                FloatingActionButton(
                    onClick = { showFabBottomSheet = true },
                    containerColor = PrimaryIndigo,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("global_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إجراء سريع")
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
                        0 -> com.example.ui.dashboard.DashboardScreen(
                            onNavigateToStudents = { selectedTab = 2 },
                            onNavigateToAttendance = { selectedTab = 3 },
                            onNavigateToFinance = { selectedTab = 4 },
                            onNavigateToReports = { selectedTab = 8 },
                            onAiAssistantClick = { onAiAssistantClick?.invoke() }
                        )
                        1 -> TeachersTab(
                            teachers = teachers,
                            students = activeStudents,
                            groups = groups,
                            studentTeacherCrossRefs = studentTeacherCrossRefs,
                            onDelete = { viewModel.deleteTeacher(it) },
                            onEdit = { teacher ->
                                selectedTeacherToEdit = teacher
                                showEditTeacherDialog = true
                            },
                            onAddClick = { showAddTeacherDialog = true }
                        )
                        2 -> StudentsTab(
                            students = students,
                            teachers = teachers,
                            studentTeacherCrossRefs = studentTeacherCrossRefs,
                            payments = payments,
                            onDelete = { viewModel.deleteStudent(it) },
                            onWhatsApp = { phone, name ->
                                val template = "مرحباً يا فندم، نود إعلامكم بخصوص الطالب/ة $name..."
                                sendWhatsAppMessage(context, phone, template)
                            },
                            viewModel = viewModel,
                            onNavigateToQrScanner = { selectedTab = 12 },
                            onNavigateToStudentProfile = { studentId ->
                                selectedStudentIdForProfile = studentId
                                selectedTab = 13
                            },
                            onNavigateToAtRisk = { selectedTab = 14 }
                        )
                        12 -> com.example.ui.academic.QRScannerScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 2 }
                        )
                        3 -> AttendanceTab(
                            students = activeStudents,
                            teachers = teachers,
                            studentTeacherCrossRefs = studentTeacherCrossRefs,
                            attendanceList = attendanceList,
                            onSave = { batch -> viewModel.saveAttendanceBatch(batch) },
                            onWhatsApp = { phone, name, subject ->
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val template = "نود إعلامكم بأن الطالب/ة $name قد تغيب عن حصة $subject اليوم بتاريخ $dateStr."
                                sendWhatsAppMessage(context, phone, template)
                            }
                        )
                        4 -> FinanceTab(
                            payments = payments,
                            expenses = expenses,
                            students = activeStudents,
                            teachers = teachers,
                            onAddPaymentClick = { showAddPaymentDialog = true },
                            onAddExpenseClick = { showAddExpenseDialog = true },
                            onDeletePayment = { viewModel.deletePayment(it) },
                            onDeleteExpense = { viewModel.deleteExpense(it) },
                            viewModel = viewModel,
                            currentScreen = financeScreen,
                            onScreenChange = { financeScreen = it }
                        )
                        5 -> SettingsTab(
                            profile = profile,
                            onUpdateProfile = { name, phone, sysType, centerName, whatsapp, currency ->
                                viewModel.updateProfile(name, phone, sysType, centerName, whatsapp, currency)
                                Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            onLogout = onLogout,
                            onNavigateToTeachers = { selectedTab = 1 },
                            onNavigateToGroups = { selectedTab = 6 },
                            onNavigateToReports = { selectedTab = 8 },
                            onNavigateToAssignments = { selectedTab = 10 },
                            onNavigateToExams = { selectedTab = 11 },
                            onNavigateToAchievements = { selectedTab = 15 },
                            onNavigateToAtRisk = { selectedTab = 14 },
                            onNavigateToBackup = { selectedTab = 9 },
                            onNavigateToCommunicationCenter = { onCommunicationCenterClick?.invoke() },
                            onNavigateToClassrooms = { onNavigateToClassrooms?.invoke() },
                            onNavigateToCenterSchedule = { onNavigateToCenterSchedule?.invoke() },
                            onNavigateToUserManagement = { onNavigateToUserManagement?.invoke() }
                        )
                        6 -> com.example.ui.groups.GroupsScreen(
                            onBackClick = { selectedTab = 0 },
                            onNavigateToQuickAttendance = { groupId ->
                                activeGroupIdForAttendance = groupId
                                selectedTab = 7
                            }
                        )
                        7 -> com.example.ui.groups.QuickAttendanceScreen(
                            initialGroupId = activeGroupIdForAttendance,
                            onBackClick = { selectedTab = 6 }
                        )
                        8 -> com.example.ui.reports.ReportsScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 0 },
                            onAiAssistantClick = onAiAssistantClick
                        )
                        9 -> com.example.ui.backup.BackupScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 5 }
                        )
                        10 -> com.example.ui.academic.AssignmentsScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 0 }
                        )
                        11 -> com.example.ui.academic.ExamsScreen(
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 0 }
                        )
                        13 -> com.example.ui.academic.StudentProfileScreen(
                            studentId = selectedStudentIdForProfile ?: "",
                            viewModel = viewModel,
                            onBackClick = { selectedTab = 2 }
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
            if (showAddTeacherDialog) {
                AddTeacherDialog(
                    onDismiss = { showAddTeacherDialog = false },
                    onConfirm = { name, subject, phone, salaryType, salaryVal, notes, stages ->
                        viewModel.addTeacher(
                            name = name, subject = subject, phone = phone, salaryType = salaryType, salaryValue = salaryVal, notes = notes, stages = stages,
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            },
                            onSuccess = {
                                showAddTeacherDialog = false
                            }
                        )
                    }
                )
            }

            if (showEditTeacherDialog && selectedTeacherToEdit != null) {
                EditTeacherDialog(
                    teacher = selectedTeacherToEdit!!,
                    onDismiss = {
                        showEditTeacherDialog = false
                        selectedTeacherToEdit = null
                    },
                    onConfirm = { name, subject, phone, salaryType, salaryVal, notes, stages ->
                        viewModel.updateTeacher(selectedTeacherToEdit!!.id, name, subject, phone, salaryType, salaryVal, notes, stages)
                        showEditTeacherDialog = false
                        selectedTeacherToEdit = null
                    }
                )
            }

            if (showAddStudentDialog) {
                AddStudentDialog(
                    teachers = teachers,
                    onDismiss = { showAddStudentDialog = false },
                    onConfirm = { name, parentName, parentPhone, studentPhone, grade, customCourse, teacherId, fee, discount, isExempt, notes, type, count, price, total, teacherIds, teacherIdToFee, onSuccess, onError ->
                        viewModel.addStudent(
                            name = name, parentName = parentName, parentPhone = parentPhone, studentPhone = studentPhone,
                            grade = grade, customCourse = customCourse, teacherId = teacherId, monthlyFee = fee, discount = discount,
                            isExempt = isExempt, notes = notes, studentType = type, privateSessionsCount = count, privateSessionPrice = price, privateTotalAmount = total,
                            teacherIds = teacherIds,
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
                    students = students,
                    teachers = teachers,
                    onDismiss = { showAddPaymentDialog = false },
                    onConfirm = { studentId, teacherId, amount, month, method, notes ->
                        viewModel.addPayment(studentId, teacherId, amount, month, method, notes)
                        showAddPaymentDialog = false
                    }
                )
            }

            if (showAddExpenseDialog) {
                AddExpenseDialog(
                    onDismiss = { showAddExpenseDialog = false },
                    onConfirm = { cat, amount, desc, isMonthly, month ->
                        viewModel.addExpense(cat, amount, desc, isMonthly, month)
                        showAddExpenseDialog = false
                    }
                )
            }

            if (showAddGroupDialog) {
                AddGroupDialog(
                    onDismiss = { showAddGroupDialog = false },
                    onConfirm = { name, teacherName, classroom, schedule, notes ->
                        viewModel.addGroup(
                            name = name, teacherName = teacherName, classroom = classroom, schedule = schedule, notes = notes,
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            },
                            onSuccess = {
                                showAddGroupDialog = false
                            }
                        )
                    }
                )
            }

            if (showAddExamDialog) {
                AddExamDialog(
                    groups = groups,
                    onDismiss = { showAddExamDialog = false },
                    onConfirm = { name, totalMarks, date, groupId, groupName ->
                        viewModel.addExam(name, totalMarks, date, groupId, groupName)
                        showAddExamDialog = false
                    }
                )
            }

            if (showFabBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showFabBottomSheet = false },
                    containerColor = SurfaceDark,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "إضافة إجراء سريع",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        
                        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.ADD_STUDENTS).value) {
                            BottomSheetActionRow(
                                title = "إضافة طالب جديد",
                                subtitle = "تسجيل بيانات طالب وربطه بولي أمر ومعلم",
                                icon = Icons.Default.PersonAdd,
                                color = PrimaryIndigoLight,
                                onClick = {
                                    showFabBottomSheet = false
                                    showAddStudentDialog = true
                                }
                            )
                        }
                        
                        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.ADD_TEACHERS).value) {
                            BottomSheetActionRow(
                                title = "إضافة معلم جديد",
                                subtitle = "تسجيل معلم جديد وتحديد نظام حسابه المالي",
                                icon = Icons.Default.Person,
                                color = PrimaryIndigoLight,
                                onClick = {
                                    showFabBottomSheet = false
                                    showAddTeacherDialog = true
                                }
                            )
                        }
                        
                        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.MANAGE_CLASSROOMS).value) {
                            BottomSheetActionRow(
                                title = "إضافة مجموعة جديدة",
                                subtitle = "إنشاء مجموعة دراسية وتحديد جدول الحصص",
                                icon = Icons.Default.Groups,
                                color = SuccessColorLight,
                                onClick = {
                                    showFabBottomSheet = false
                                    showAddGroupDialog = true
                                }
                            )
                        }
                        
                        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.EDIT_REVENUE).value) {
                            BottomSheetActionRow(
                                title = "تسجيل دفعة / رسوم",
                                subtitle = "رصد وتوثيق عملية دفع جديدة لطالب",
                                icon = Icons.Default.Payments,
                                color = WarningColor,
                                onClick = {
                                    showFabBottomSheet = false
                                    showAddPaymentDialog = true
                                }
                            )
                        }
                        
                        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.MANAGE_GRADES).value) {
                            BottomSheetActionRow(
                                title = "إضافة امتحان جديد",
                                subtitle = "جدولة ورصد درجات امتحان لمجموعة معينة",
                                icon = Icons.Default.Quiz,
                                color = ErrorColorLight,
                                onClick = {
                                    showFabBottomSheet = false
                                    showAddExamDialog = true
                                }
                            )
                        }
                        
                        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.EDIT_REVENUE).value) {
                            BottomSheetActionRow(
                                title = "إضافة مصروف جديد",
                                subtitle = "توثيق بند صرف مالي من ميزانية السنتر",
                                icon = Icons.Default.ReceiptLong,
                                color = TextSecondary,
                                onClick = {
                                    showFabBottomSheet = false
                                    showAddExpenseDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopHeader(profile: Profile?, onAiAssistantClick: (() -> Unit)? = null) {
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
                    .background(PrimaryIndigoDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = profile?.centerName ?: "Nexora",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "نظام الإدارة المتكامل",
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
                        .testTag("dashboard_ai_assistant_button"),
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

// ------------------ TAB 0: DASHBOARD ------------------
@Composable
fun DashboardTab(
    teachers: List<Teacher>,
    students: List<Student>,
    payments: List<Payment>,
    expenses: List<Expense>,
    onViewAllActivities: () -> Unit
) {
    val totalRevenue = payments.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Stat Cards Grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Group,
                        iconColor = PrimaryIndigoLight,
                        badgeText = "+12%",
                        badgeColor = PrimaryIndigoLight,
                        badgeBg = PrimaryIndigo.copy(alpha = 0.1f),
                        value = students.size.toString(),
                        label = "إجمالي الطلاب"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Payments,
                        iconColor = SuccessColorLight,
                        badgeText = "ج.م",
                        badgeColor = SuccessColorLight,
                        badgeBg = SuccessColor.copy(alpha = 0.1f),
                        value = String.format(Locale.US, "%,.0f", totalRevenue),
                        label = "إيرادات الشهر"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Person,
                        iconColor = WarningColor,
                        value = teachers.size.toString(),
                        label = "المعلمون"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = ErrorColorLight,
                        value = String.format(Locale.US, "%,.0f", totalExpenses),
                        label = "المصروفات"
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Revenue chart removed per request

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر الحركات المالية",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "عرض الكل",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryIndigoLight,
                    modifier = Modifier.clickable { onViewAllActivities() }
                )
            }
        }

        if (payments.isEmpty() && expenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد حركات مالية مسجلة حالياً.", color = TextTertiary, fontSize = 13.sp)
                }
            }
        } else {
            // Merge & Sort payments and expenses by date DESC
            val activities = (payments.map { true to it } + expenses.map { false to it })
                .sortedByDescending {
                    if (it.first) (it.second as Payment).date else (it.second as Expense).date
                }
                .take(5)

            items(activities, key = { if (it.first) "P_${(it.second as Payment).id}" else "E_${(it.second as Expense).id}" }) { (isPayment, item) ->
                if (isPayment) {
                    val pay = item as Payment
                    val studentName = students.find { it.id == pay.studentId }?.name ?: "طالب"
                    RecentActivityItem(
                        modifier = Modifier.animateItem(),
                        icon = Icons.Default.AddCircle,
                        iconColor = SuccessColor,
                        iconBgColor = SuccessColor.copy(alpha = 0.1f),
                        title = "تحصيل رسوم: $studentName",
                        subtitle = "شهر ${pay.month} • نقدي",
                        amount = "+${pay.amount} ج.م",
                        amountColor = SuccessColorLight
                    )
                } else {
                    val exp = item as Expense
                    RecentActivityItem(
                        modifier = Modifier.animateItem(),
                        icon = Icons.Default.RemoveCircle,
                        iconColor = ErrorColor,
                        iconBgColor = ErrorColor.copy(alpha = 0.1f),
                        title = "مصروف: ${exp.category}",
                        subtitle = exp.description,
                        amount = "-${exp.amount} ج.م",
                        amountColor = ErrorColorLight
                    )
                }
            }
        }
    }
}

// ------------------ TAB 1: TEACHERS ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersTab(
    teachers: List<Teacher>,
    students: List<Student>,
    groups: List<Group>,
    studentTeacherCrossRefs: List<com.example.data.model.StudentTeacherCrossRef>,
    onDelete: (String) -> Unit,
    onEdit: (Teacher) -> Unit,
    onAddClick: () -> Unit
) {
    var searchKeyword by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf("الكل") }
    var selectedStageFilter by remember { mutableStateOf("الكل") }
    var selectedStudentCountFilter by remember { mutableStateOf("الكل") } // الكل, أقل من 10, من 10 إلى 50, أكثر من 50
    var selectedSort by remember { mutableStateOf("الاسم") } // الاسم, الأكثر طلاباً, الأقل طلاباً
    var showFilterSheet by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("cards") } // "cards", "table", "compact"
    var selectedTeacherForProfile by remember { mutableStateOf<Teacher?>(null) }

    val subjectsList = remember(teachers) {
        listOf("الكل") + teachers.flatMap { it.subject.split("، ") }.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    
    val stagesList = remember(teachers) {
        listOf("الكل") + teachers.flatMap { it.stages.split("، ") }.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    val teacherStats = remember(teachers, students, groups, studentTeacherCrossRefs) {
        teachers.associateWith { teacher ->
            val teacherStudents = students.filter { s ->
                studentTeacherCrossRefs.any { it.studentId == s.id && it.teacherId == teacher.id } || 
                (s.teacherId == teacher.id && studentTeacherCrossRefs.none { it.studentId == s.id })
            }
            val groupsCount = groups.count { it.teacherName == teacher.name }
            val revenue = teacherStudents.sumOf { it.netFee }
            val dues = when (teacher.salaryType) {
                "percentage" -> revenue * (teacher.salaryValue / 100.0)
                "fixed" -> teacher.salaryValue
                else -> 0.0
            }
            
            mapOf(
                "studentsCount" to teacherStudents.size,
                "groupsCount" to groupsCount,
                "revenue" to revenue,
                "dues" to dues
            )
        }
    }

    var filteredTeachers = teachers.filter { teacher ->
        val matchesSearch = teacher.name.contains(searchKeyword, ignoreCase = true) || 
                            teacher.subject.contains(searchKeyword, ignoreCase = true) || 
                            teacher.stages.contains(searchKeyword, ignoreCase = true)
        val matchesSubject = selectedSubjectFilter == "الكل" || teacher.subject.contains(selectedSubjectFilter)
        val matchesStage = selectedStageFilter == "الكل" || teacher.stages.contains(selectedStageFilter)
        
        val count = (teacherStats[teacher]?.get("studentsCount") as? Int) ?: 0
        val matchesStudentCount = when (selectedStudentCountFilter) {
            "أقل من 10" -> count < 10
            "من 10 إلى 50" -> count in 10..50
            "أكثر من 50" -> count > 50
            else -> true
        }
        
        matchesSearch && matchesSubject && matchesStage && matchesStudentCount
    }
    
    filteredTeachers = when (selectedSort) {
        "الأكثر طلاباً" -> filteredTeachers.sortedByDescending { teacherStats[it]?.get("studentsCount") as? Int ?: 0 }
        "الأقل طلاباً" -> filteredTeachers.sortedBy { teacherStats[it]?.get("studentsCount") as? Int ?: 0 }
        else -> filteredTeachers.sortedBy { it.name }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("تصفية وترتيب المعلمين", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                
                if (subjectsList.size > 1) {
                    FilterSection(title = "المادة الدراسية", options = subjectsList, selectedOption = selectedSubjectFilter, onOptionSelected = { selectedSubjectFilter = it })
                }
                if (stagesList.size > 1) {
                    FilterSection(title = "المرحلة التعليمية", options = stagesList, selectedOption = selectedStageFilter, onOptionSelected = { selectedStageFilter = it })
                }
                FilterSection(
                    title = "عدد الطلاب",
                    options = listOf("الكل", "أقل من 10", "من 10 إلى 50", "أكثر من 50"),
                    selectedOption = selectedStudentCountFilter,
                    onOptionSelected = { selectedStudentCountFilter = it }
                )
                
                HorizontalDivider(color = BorderColor)
                Text("ترتيب حسب", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                FilterSection(title = "", options = listOf("الاسم", "الأكثر طلاباً", "الأقل طلاباً"), selectedOption = selectedSort, onOptionSelected = { selectedSort = it })
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "إدارة المعلمين",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة معلم", fontSize = 12.sp)
            }
        }

        // Aggregate Statistics Cards
        val totalTeachers = teachers.size
        val totalStudentsInTeachers = teachers.sumOf { teacherStats[it]?.get("studentsCount") as? Int ?: 0 }
        val totalRevenue = teachers.sumOf { teacherStats[it]?.get("revenue") as? Double ?: 0.0 }
        val totalDues = teachers.sumOf { teacherStats[it]?.get("dues") as? Double ?: 0.0 }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Teacher count card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .width(130.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(totalTeachers.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("إجمالي المعلمين", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Student count card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .width(130.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessColorLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = SuccessColorLight, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(totalStudentsInTeachers.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("إجمالي الطلاب", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Total revenue card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .width(140.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${String.format(java.util.Locale.US, "%,.0f", totalRevenue)} ج.م", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("إجمالي الإيرادات", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Total dues card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .width(140.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = WarningColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${String.format(java.util.Locale.US, "%,.0f", totalDues)} ج.م", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("إجمالي المستحقات", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                placeholder = { Text("ابحث باسم المعلم، المادة، أو المرحلة...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = PrimaryIndigoLight,
                    unfocusedLabelColor = TextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryIndigo.copy(alpha = 0.1f))
                    .border(1.dp, PrimaryIndigoLight, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "فلاتر",
                    tint = PrimaryIndigoLight
                )
            }
        }

        // View Mode Toggle Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text("طريقة العرض:", fontSize = 12.sp, color = TextSecondary)
            
            // Cards Icon
            IconButton(
                onClick = { viewMode = "cards" },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (viewMode == "cards") PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (viewMode == "cards") PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "بطاقات",
                    tint = if (viewMode == "cards") PrimaryIndigoLight else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Table Icon
            IconButton(
                onClick = { viewMode = "table" },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (viewMode == "table") PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (viewMode == "table") PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = "جدول",
                    tint = if (viewMode == "table") PrimaryIndigoLight else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Compact Icon
            IconButton(
                onClick = { viewMode = "compact" },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (viewMode == "compact") PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (viewMode == "compact") PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ViewList,
                    contentDescription = "قائمة مبسطة",
                    tint = if (viewMode == "compact") PrimaryIndigoLight else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (filteredTeachers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("لا يوجد معلمون مطابقون.", color = TextTertiary)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(if (viewMode == "table") 6.dp else 12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (viewMode == "table") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المعلم / المادة", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Right)
                                Text("الطلاب", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                                Text("المجموعات", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                                Text("المستحقات", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                                Text("إجراءات", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                items(filteredTeachers, key = { it.id }) { teacher ->
                    val stats = teacherStats[teacher]
                    val studentsCount = stats?.get("studentsCount") as? Int ?: 0
                    val groupsCount = stats?.get("groupsCount") as? Int ?: 0
                    val revenue = stats?.get("revenue") as? Double ?: 0.0
                    val dues = stats?.get("dues") as? Double ?: 0.0
                    
                    when (viewMode) {
                        "table" -> {
                            Card(
                                onClick = { selectedTeacherForProfile = teacher },
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(teacher.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Right)
                                        Text(teacher.subject.ifEmpty { "مادة غير محددة" }, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Right)
                                    }
                                    Text(studentsCount.toString(), modifier = Modifier.weight(1f), fontSize = 12.sp, color = TextPrimary, textAlign = TextAlign.Center)
                                    Text(groupsCount.toString(), modifier = Modifier.weight(1f), fontSize = 12.sp, color = TextPrimary, textAlign = TextAlign.Center)
                                    Text("${String.format(java.util.Locale.US, "%,.0f", dues)} ج.م", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarningColor, textAlign = TextAlign.Center)
                                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onEdit(teacher) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PrimaryIndigoLight, modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(onClick = { onDelete(teacher.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColorLight, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                        "compact" -> {
                            Card(
                                onClick = { selectedTeacherForProfile = teacher },
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(CircleShape).background(PrimaryIndigo.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(teacher.name.firstOrNull()?.toString() ?: "م", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigoLight)
                                        }
                                        Column {
                                            Text(teacher.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("${teacher.subject.ifEmpty { "مادة غير محددة" }} • ${studentsCount} طالباً", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { onEdit(teacher) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PrimaryIndigoLight, modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(onClick = { onDelete(teacher.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColorLight, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Card(
                                onClick = { selectedTeacherForProfile = teacher },
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Box(
                                                modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryIndigo.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigoLight)
                                            }
                                            Column {
                                                Text(
                                                    text = teacher.name,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = teacher.subject.ifEmpty { "مادة غير محددة" },
                                                    fontSize = 13.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = { onEdit(teacher) }) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PrimaryIndigoLight)
                                            }
                                            IconButton(onClick = { onDelete(teacher.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColorLight)
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = BorderColor)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TeacherStatItem(
                                            modifier = Modifier.weight(1f),
                                            icon = Icons.Default.School,
                                            label = "طالب",
                                            value = studentsCount.toString(),
                                            color = PrimaryIndigoLight
                                        )
                                        TeacherStatItem(
                                            modifier = Modifier.weight(1f),
                                            icon = Icons.Default.Groups,
                                            label = "مجموعة",
                                            value = groupsCount.toString(),
                                            color = SuccessColorLight
                                        )
                                        TeacherStatItem(
                                            modifier = Modifier.weight(1.2f),
                                            icon = Icons.Default.AccountBalanceWallet,
                                            label = "الإيرادات",
                                            value = String.format(java.util.Locale.US, "%,.0f", revenue),
                                            color = SuccessColor
                                        )
                                        TeacherStatItem(
                                            modifier = Modifier.weight(1.2f),
                                            icon = Icons.Default.Payments,
                                            label = "المستحقات",
                                            value = String.format(java.util.Locale.US, "%,.0f", dues),
                                            color = WarningColor
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

    if (selectedTeacherForProfile != null) {
        TeacherProfileDialog(
            teacher = selectedTeacherForProfile!!,
            students = students,
            groups = groups,
            studentTeacherCrossRefs = studentTeacherCrossRefs,
            onDismiss = { selectedTeacherForProfile = null }
        )
    }
}

@Composable
fun TeacherStatItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherProfileDialog(
    teacher: Teacher,
    students: List<Student>,
    groups: List<Group>,
    studentTeacherCrossRefs: List<com.example.data.model.StudentTeacherCrossRef>,
    onDismiss: () -> Unit
) {
    val teacherStudents = remember(teacher, students, studentTeacherCrossRefs) {
        students.filter { s ->
            studentTeacherCrossRefs.any { it.studentId == s.id && it.teacherId == teacher.id } || 
            (s.teacherId == teacher.id && studentTeacherCrossRefs.none { it.studentId == s.id })
        }
    }
    
    val teacherGroups = remember(teacher, groups) {
        groups.filter { it.teacherName == teacher.name }
    }
    
    val totalRevenue = teacherStudents.sumOf { it.monthlyFee * (1.0 - it.discount / 100.0) }
    val dues = when (teacher.salaryType) {
        "percentage" -> totalRevenue * (teacher.salaryValue / 100.0)
        "fixed" -> teacher.salaryValue
        else -> 0.0
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, BorderColor, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = teacher.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = teacher.subject.ifEmpty { "مادة غير محددة" },
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceLightDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Students Card
                        Card(
                            modifier = Modifier.weight(1f).border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLightDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.School, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(teacherStudents.size.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("طالباً نشطاً", fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        // Groups Card
                        Card(
                            modifier = Modifier.weight(1f).border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLightDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = SuccessColorLight, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(teacherGroups.size.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("مجموعات", fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        // Dues Card
                        Card(
                            modifier = Modifier.weight(1.2f).border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLightDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = WarningColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${String.format(java.util.Locale.US, "%,.0f", dues)} ج.م", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarningColor)
                                Text("مستحقات المعلم", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Salary Info
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLightDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("نظام الحساب والرواتب", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("نوع الراتب:", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = if (teacher.salaryType == "percentage") "نسبة مئوية من الإيرادات" else "راتب ثابت شهري",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("قيمة نظام الحساب:", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = if (teacher.salaryType == "percentage") "${teacher.salaryValue}%" else "${String.format(java.util.Locale.US, "%,.0f", teacher.salaryValue)} ج.م",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي إيرادات الطلاب:", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%,.0f", totalRevenue)} ج.م",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessColorLight
                                )
                            }
                        }
                    }

                    // Stages & Phone
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLightDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("معلومات التواصل والمراحل", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("رقم الهاتف:", fontSize = 12.sp, color = TextSecondary)
                                Text(teacher.phone.ifEmpty { "غير متوفر" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المراحل المشمولة:", fontSize = 12.sp, color = TextSecondary)
                                Text(teacher.stages.ifEmpty { "غير محدد" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }

                    // Groups List Block
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "المجموعات التي يدرسها (${teacherGroups.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (teacherGroups.isEmpty()) {
                            Text("لا توجد مجموعات مسجلة باسم هذا المعلم.", fontSize = 11.sp, color = TextTertiary)
                        } else {
                            teacherGroups.forEach { group ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(group.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(group.schedule.ifEmpty { "بدون موعد" }, fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PrimaryIndigo.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(group.classroom.ifEmpty { "قاعة غير محددة" }, fontSize = 10.sp, color = PrimaryIndigoLight)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Students List Block
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "الطلاب المسجلون تحت إشرافه (${teacherStudents.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (teacherStudents.isEmpty()) {
                            Text("لا يوجد طلاب مسجلون مع هذا المعلم حالياً.", fontSize = 11.sp, color = TextTertiary)
                        } else {
                            teacherStudents.take(15).forEach { student ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(student.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(student.grade, fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Text(
                                            text = if (student.studentType == "PRIVATE") "برايفت" else "مجموعات",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (student.studentType == "PRIVATE") WarningColor else SuccessColorLight
                                        )
                                    }
                                }
                            }
                            if (teacherStudents.size > 15) {
                                Text(
                                    text = "+ ${teacherStudents.size - 15} طلاب آخرين...",
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            }
        }
    }
}

// ------------------ TAB 2: STUDENTS ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsTab(
    students: List<Student>,
    teachers: List<Teacher>,
    studentTeacherCrossRefs: List<com.example.data.model.StudentTeacherCrossRef>,
    payments: List<Payment>,
    onDelete: (String) -> Unit,
    onWhatsApp: (String, String) -> Unit,
    viewModel: AppViewModel,
    onNavigateToQrScanner: () -> Unit,
    onNavigateToStudentProfile: (String) -> Unit,
    onNavigateToAtRisk: () -> Unit
) {
    var searchKeyword by remember { mutableStateOf("") }
    var selectedGradeFilter by remember { mutableStateOf("الكل") }
    var selectedStageFilter by remember { mutableStateOf("الكل") }
    var selectedGroupFilter by remember { mutableStateOf("الكل") }
    var selectedCourseFilter by remember { mutableStateOf("الكل") }
    var selectedTypeFilter by remember { mutableStateOf("الكل") } // الكل, مجموعات, برايفت
    var selectedTeacherFilter by remember { mutableStateOf("الكل") }
    var selectedLateFilter by remember { mutableStateOf("الكل") } // الكل, نعم, لا
    var selectedExemptFilter by remember { mutableStateOf("الكل") } // الكل, نعم, لا
    var selectedAttendanceFilter by remember { mutableStateOf("الكل") } // الكل, منتظم, غياب متكرر

    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendance.collectAsStateWithLifecycle()

    var selectedStudentForQr by remember { mutableStateOf<Student?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("cards") } // "cards", "table", "compact"

    if (selectedStudentForQr != null) {
        val student = selectedStudentForQr!!
        LaunchedEffect(student.id) {
            viewModel.initializeStudentQrIfEmpty(student)
        }
        val currentStudent = students.find { it.id == student.id } ?: student

        StudentQrDialog(
            student = currentStudent,
            onDismiss = { selectedStudentForQr = null },
            onRegenerate = { viewModel.regenerateStudentQr(student.id) },
            onPrint = { bitmap ->
                com.example.util.QrCodeGenerator.printQrCode(viewModel.getApplication(), bitmap, student.name)
            },
            onShare = { bitmap ->
                com.example.util.QrCodeGenerator.shareQrCode(viewModel.getApplication(), bitmap, student.name)
            }
        )
    }

    val currentMonthStr = remember {
        java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
    }

    val isStudentPaidThisMonth = { studentId: String ->
        payments.any { it.studentId == studentId && it.month == currentMonthStr }
    }

    val gradesList = remember(students) { listOf("الكل") + students.map { it.grade }.filter { it.isNotEmpty() }.distinct().sorted() }
    val groupsList = remember(groups) { listOf("الكل") + groups.map { it.name }.filter { it.isNotEmpty() }.distinct().sorted() }
    val coursesList = remember(students) { listOf("الكل") + students.map { it.customCourse }.filter { it.isNotEmpty() }.distinct().sorted() }
    val teacherNamesList = remember(teachers, students, studentTeacherCrossRefs) {
        listOf("الكل") + students.flatMap { s ->
            val studentTeacherIds = studentTeacherCrossRefs.filter { it.studentId == s.id }.map { it.teacherId }
            val actualTeacherIds = if (studentTeacherIds.isNotEmpty()) studentTeacherIds else listOf(s.teacherId).filter { it.isNotBlank() }
            teachers.filter { actualTeacherIds.contains(it.id) }.map { it.name }
        }.distinct().sorted()
    }

    val getStudentStatusInfo = { student: Student ->
        val studentAttendance = attendanceList.filter { it.studentId == student.id }
        val totalClasses = studentAttendance.size
        val presentClasses = studentAttendance.count { it.status == "P" }
        val attendanceRate = if (totalClasses > 0) presentClasses.toDouble() / totalClasses else 1.0
        val hasUnpaid = student.monthlyFee > 0 && !isStudentPaidThisMonth(student.id)

        when {
            attendanceRate < 0.65 || (hasUnpaid && !student.isExempt && student.monthlyFee > 150) -> Triple("معرض للتراجع", ErrorColor, ErrorColor.copy(alpha = 0.15f))
            attendanceRate < 0.85 || hasUnpaid -> Triple("بحاجة لمتابعة", WarningColor, WarningColor.copy(alpha = 0.15f))
            else -> Triple("منتظم", SuccessColorLight, SuccessColor.copy(alpha = 0.15f))
        }
    }

    val filteredStudents = students.filter { student ->
        if (student.isArchived) return@filter false
        val matchesSearch = student.name.contains(searchKeyword, ignoreCase = true) || 
                            student.qrCode.contains(searchKeyword, ignoreCase = true) ||
                            student.parentName.contains(searchKeyword, ignoreCase = true) ||
                            student.studentPhone.contains(searchKeyword) ||
                            student.parentPhone.contains(searchKeyword) ||
                            student.grade.contains(searchKeyword, ignoreCase = true) ||
                            student.customCourse.contains(searchKeyword, ignoreCase = true)
        val matchesGrade = selectedGradeFilter == "الكل" || student.grade == selectedGradeFilter
        val matchesStage = selectedStageFilter == "الكل" || student.grade.contains(selectedStageFilter, ignoreCase = true)
        val matchedGroupObj = groups.find { it.name == selectedGroupFilter }
        val matchesGroup = selectedGroupFilter == "الكل" || student.groupId == matchedGroupObj?.id
        val matchesCourse = selectedCourseFilter == "الكل" || student.customCourse == selectedCourseFilter
        val matchesType = when(selectedTypeFilter) {
            "مجموعات" -> student.studentType == "GROUP"
            "برايفت" -> student.studentType == "PRIVATE"
            else -> true
        }
        val studentTeacherIds = studentTeacherCrossRefs.filter { it.studentId == student.id }.map { it.teacherId }
        val actualTeacherIds = if (studentTeacherIds.isNotEmpty()) studentTeacherIds else listOf(student.teacherId).filter { it.isNotBlank() }
        val teacherNamesForStudent = teachers.filter { actualTeacherIds.contains(it.id) }.map { it.name }
        val matchesTeacher = selectedTeacherFilter == "الكل" || teacherNamesForStudent.contains(selectedTeacherFilter)
        val hasUnpaid = student.monthlyFee > 0 && !isStudentPaidThisMonth(student.id)
        val matchesLate = when(selectedLateFilter) {
            "نعم" -> hasUnpaid
            "لا" -> !hasUnpaid
            else -> true
        }
        val matchesExempt = when(selectedExemptFilter) {
            "نعم" -> student.isExempt
            "لا" -> !student.isExempt
            else -> true
        }
        val attendanceStatus = getStudentStatusInfo(student).first
        val matchesAttendance = when(selectedAttendanceFilter) {
            "منتظم" -> attendanceStatus == "منتظم"
            "غياب متكرر" -> attendanceStatus == "معرض للتراجع" || attendanceStatus == "بحاجة لمتابعة"
            else -> true
        }
        matchesSearch && matchesGrade && matchesStage && matchesGroup && matchesCourse && matchesType && matchesTeacher && matchesLate && matchesExempt && matchesAttendance
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("تصفية الطلاب", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                
                FilterSection(title = "المرحلة التعليمية", options = listOf("الكل", "الابتدائي", "الإعدادي", "الثانوي", "KG"), selectedOption = selectedStageFilter, onOptionSelected = { selectedStageFilter = it })
                FilterSection(title = "السنة الدراسية", options = gradesList, selectedOption = selectedGradeFilter, onOptionSelected = { selectedGradeFilter = it })
                FilterSection(title = "المادة", options = coursesList, selectedOption = selectedCourseFilter, onOptionSelected = { selectedCourseFilter = it })
                FilterSection(title = "المعلم", options = teacherNamesList, selectedOption = selectedTeacherFilter, onOptionSelected = { selectedTeacherFilter = it })
                if (groupsList.size > 1) {
                    FilterSection(title = "المجموعة", options = groupsList, selectedOption = selectedGroupFilter, onOptionSelected = { selectedGroupFilter = it })
                }
                FilterSection(title = "طبيعة الطالب", options = listOf("الكل", "مجموعات", "برايفت"), selectedOption = selectedTypeFilter, onOptionSelected = { selectedTypeFilter = it })
                FilterSection(title = "المتأخرون في الدفع", options = listOf("الكل", "نعم", "لا"), selectedOption = selectedLateFilter, onOptionSelected = { selectedLateFilter = it })
                FilterSection(title = "الطلاب المعفيين", options = listOf("الكل", "نعم", "لا"), selectedOption = selectedExemptFilter, onOptionSelected = { selectedExemptFilter = it })
                FilterSection(title = "سجل الحضور", options = listOf("الكل", "منتظم", "غياب متكرر"), selectedOption = selectedAttendanceFilter, onOptionSelected = { selectedAttendanceFilter = it })
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "دليل الطلاب والمتابعة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            IconButton(
                onClick = onNavigateToQrScanner,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryIndigo.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "مسح حضور QR",
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                placeholder = { Text("ابحث بالاسم، الكود، الهاتف، المدرس، المادة...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = PrimaryIndigoLight,
                    unfocusedLabelColor = TextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryIndigo.copy(alpha = 0.1f))
                    .border(1.dp, PrimaryIndigoLight, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "فلاتر",
                    tint = PrimaryIndigoLight
                )
            }
        }

        // View Mode Toggle Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text("طريقة العرض:", fontSize = 12.sp, color = TextSecondary)
            
            // Cards Icon
            IconButton(
                onClick = { viewMode = "cards" },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (viewMode == "cards") PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (viewMode == "cards") PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "بطاقات",
                    tint = if (viewMode == "cards") PrimaryIndigoLight else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Table Icon
            IconButton(
                onClick = { viewMode = "table" },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (viewMode == "table") PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (viewMode == "table") PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = "جدول",
                    tint = if (viewMode == "table") PrimaryIndigoLight else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Compact Icon
            IconButton(
                onClick = { viewMode = "compact" },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (viewMode == "compact") PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (viewMode == "compact") PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ViewList,
                    contentDescription = "قائمة مبسطة",
                    tint = if (viewMode == "compact") PrimaryIndigoLight else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (filteredStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "لا توجد نتائج مطابقة لخيارات التصفية",
                        color = TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (viewMode == "table") 6.dp else 10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (viewMode == "table") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الطالب / الصف", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Right)
                                Text("الحالة", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                                Text("الرسوم", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                                Text("إجراءات", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                items(filteredStudents, key = { it.id }) { student ->
                    val statusInfo = getStudentStatusInfo(student)
                    val matchedGroup = groups.find { it.id == student.groupId }?.name ?: "بدون مجموعة"

                    when (viewMode) {
                        "table" -> {
                            Card(
                                onClick = { onNavigateToStudentProfile(student.id) },
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(student.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Right)
                                        Text("${student.grade} • $matchedGroup", fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Right)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(statusInfo.third)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(statusInfo.first, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusInfo.second)
                                    }
                                    
                                    val feeAmountText = if (student.isExempt) "معفى" else "${student.netFee} ج.م"
                                    Text(
                                        text = feeAmountText,
                                        modifier = Modifier.weight(1.2f),
                                        fontSize = 11.sp,
                                        color = if (student.isExempt) SuccessColorLight else TextTertiary,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Row(
                                        modifier = Modifier.weight(1.2f),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { selectedStudentForQr = student }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.QrCode, contentDescription = "QR", tint = TextTertiary, modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                val phone = student.parentPhone.ifEmpty { student.studentPhone }
                                                onWhatsApp(phone, student.name)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.SendToMobile, contentDescription = "واتساب", tint = SuccessColorLight, modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(onClick = { onDelete(student.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                        "compact" -> {
                            Card(
                                onClick = { onNavigateToStudentProfile(student.id) },
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(CircleShape).background(PrimaryIndigo.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(student.name.firstOrNull()?.toString() ?: "ط", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigoLight)
                                        }
                                        Column {
                                            Text(student.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("${student.grade} • $matchedGroup", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { selectedStudentForQr = student }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.QrCode, contentDescription = "QR", tint = TextTertiary, modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(onClick = { onDelete(student.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            CompactStudentItem(
                                modifier = Modifier.animateItem(),
                                student = student,
                                groupName = matchedGroup,
                                statusText = statusInfo.first,
                                statusColor = statusInfo.second,
                                statusBg = statusInfo.third,
                                onQrClick = { selectedStudentForQr = student },
                                onDelete = { onDelete(student.id) },
                                onWhatsApp = {
                                    val phone = student.parentPhone.ifEmpty { student.studentPhone }
                                    onWhatsApp(phone, student.name)
                                },
                                onClick = { onNavigateToStudentProfile(student.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else SurfaceLightDark)
                        .border(1.dp, if (isSelected) PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { onOptionSelected(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) PrimaryIndigoLight else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun CompactStudentItem(
    student: Student,
    groupName: String,
    statusText: String,
    statusColor: Color,
    statusBg: Color,
    modifier: Modifier = Modifier,
    onQrClick: () -> Unit,
    onDelete: () -> Unit,
    onWhatsApp: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Modern Initials Avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryIndigo.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.name.firstOrNull()?.toString() ?: "ط",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigoLight
                        )
                    }

                    Column {
                        Text(
                            text = student.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${student.grade} • $groupName",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "كود ولي الأمر: ${student.parentCode}",
                            fontSize = 11.sp,
                            color = PrimaryIndigoLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Beautifully designed Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary info (Fees / Details)
                val feeText = if (student.isExempt) {
                    "إعفاء كلي من الرسوم"
                } else if (student.studentType == "PRIVATE") {
                    "${student.privateSessionsCount} حصص × ${student.privateSessionPrice} = ${student.privateTotalAmount} ج.م"
                } else {
                    "الرسوم الشهرية: ${student.netFee} ج.م"
                }
                Text(
                    text = feeText,
                    fontSize = 11.sp,
                    color = if (student.isExempt) SuccessColorLight else TextTertiary,
                    fontWeight = FontWeight.Medium
                )

                // Actions row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onQrClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "عرض رمز QR للحضور",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SendToMobile,
                            contentDescription = "تواصل واتساب",
                            tint = SuccessColorLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الطالب",
                            tint = ErrorColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupHeaderRow(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark.copy(alpha = 0.6f))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = subtitle, fontSize = 11.sp, color = TextTertiary)
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ------------------ TAB 3: ATTENDANCE ------------------
@Composable
fun AttendanceTab(
    students: List<Student>,
    teachers: List<Teacher>,
    studentTeacherCrossRefs: List<com.example.data.model.StudentTeacherCrossRef>,
    attendanceList: List<Attendance>,
    onSave: (List<Attendance>) -> Unit,
    onWhatsApp: (String, String, String) -> Unit
) {
    var selectedTeacherId by remember { mutableStateOf("") }
    val formattedToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayTimestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(formattedToday)?.time ?: System.currentTimeMillis()

    // Filter students by selected teacher
    val filteredStudents = if (selectedTeacherId.isEmpty()) emptyList() else students.filter { s ->
        studentTeacherCrossRefs.any { it.studentId == s.id && it.teacherId == selectedTeacherId } || (s.teacherId == selectedTeacherId && studentTeacherCrossRefs.none { it.studentId == s.id })
    }

    // State holding attendance map
    val attendanceMap = remember { mutableStateMapOf<String, String>() }

    // Initialize attendance status when selected teacher changes
    LaunchedEffect(selectedTeacherId) {
        attendanceMap.clear()
        filteredStudents.forEach { student ->
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
            text = "تسجيل الحضور اليومي",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Teacher selector dropdown
        var dropdownExpanded by remember { mutableStateOf(false) }
        val selectedTeacherName = teachers.find { it.id == selectedTeacherId }?.name ?: "اختر المعلم لعرض الطلاب"

        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Button(
                onClick = { dropdownExpanded = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedTeacherName, color = TextPrimary)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                }
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).background(SurfaceDark)
            ) {
                teachers.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t.name, color = TextPrimary) },
                        onClick = {
                            selectedTeacherId = t.id
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        if (selectedTeacherId.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("يرجى اختيار معلم لعرض طلابه وتسجيل حضورهم.", color = TextTertiary, textAlign = TextAlign.Center)
            }
        } else if (filteredStudents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("لا يوجد طلاب مسجلون تحت هذا المعلم بعد.", color = TextTertiary, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredStudents, key = { it.id }) { student ->
                    val status = attendanceMap[student.id] ?: "present"
                    Card(
                        modifier = Modifier
                            .animateItem()
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

                            // Quick attendance status selectors
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
                                AttendanceButton(
                                    label = "متأخر",
                                    isSelected = status == "late",
                                    activeColor = WarningColor,
                                    onClick = { attendanceMap[student.id] = "late" }
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
                        val batch = filteredStudents.map { student ->
                            Attendance(
                                studentId = student.id,
                                teacherId = selectedTeacherId,
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
                        val absentStudents = filteredStudents.filter { attendanceMap[it.id] == "absent" }
                        val teacherSubject = teachers.find { it.id == selectedTeacherId }?.subject ?: "الحصة"
                        absentStudents.forEach { student ->
                            if (student.parentPhone.isNotEmpty()) {
                                onWhatsApp(student.parentPhone, student.name, teacherSubject)
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
fun AttendanceButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent
    val contentColor = if (isSelected) activeColor else TextSecondary
    val borderColor = if (isSelected) activeColor else BorderColor

    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------ TAB 4: FINANCE ------------------
@Composable
fun FinanceTab(
    payments: List<Payment>,
    expenses: List<Expense>,
    students: List<Student>,
    teachers: List<Teacher>,
    onAddPaymentClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onDeletePayment: (String) -> Unit,
    onDeleteExpense: (String) -> Unit,
    viewModel: AppViewModel,
    currentScreen: String,
    onScreenChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (currentScreen == "dashboard") {
            Text(
                text = "الإدارة المالية",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            // Add navigation cards for the different financial sections
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinanceNavCard(
                    title = "المتحصلات",
                    icon = Icons.Default.Payments,
                    color = SuccessColor,
                    onClick = { onScreenChange("payments") },
                    modifier = Modifier.weight(1f)
                )
                FinanceNavCard(
                    title = "المصروفات",
                    icon = Icons.Default.ReceiptLong,
                    color = ErrorColorLight,
                    onClick = { onScreenChange("expenses") },
                    modifier = Modifier.weight(1f)
                )
                FinanceNavCard(
                    title = "الرواتب",
                    icon = Icons.Default.Person,
                    color = PrimaryIndigoLight,
                    onClick = { onScreenChange("salaries") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                "dashboard" -> {
                    com.example.ui.finance.FinancialDashboardScreen(viewModel = viewModel, isEmbedded = true)
                }
                "payments" -> {
                    com.example.ui.finance.PaymentsScreen(
                        isEmbedded = false,
                        onBackClick = { onScreenChange("dashboard") }
                    )
                }
                "expenses" -> {
                    com.example.ui.finance.ExpensesScreen(
                        viewModel = viewModel,
                        isEmbedded = false,
                        onBackClick = { onScreenChange("dashboard") }
                    )
                }
                "salaries" -> {
                    com.example.ui.finance.TeacherSalariesScreen(
                        viewModel = viewModel,
                        isEmbedded = false,
                        onBackClick = { onScreenChange("dashboard") }
                    )
                }
            }
        }
    }
}

@Composable
fun FinanceNavCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(90.dp)
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        BorderColor,
                        color.copy(alpha = 0.25f),
                        BorderColor
                    )
                ),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

// ------------------ TAB 5: SETTINGS ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    profile: Profile?,
    onUpdateProfile: (String, String, String, String, String, String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToTeachers: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToAtRisk: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToCommunicationCenter: () -> Unit,
    onNavigateToClassrooms: () -> Unit,
    onNavigateToCenterSchedule: () -> Unit,
    onNavigateToUserManagement: () -> Unit = {}
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var phone by remember { mutableStateOf(profile?.phone ?: "") }
    var centerName by remember { mutableStateOf(profile?.centerName ?: "") }
    var whatsapp by remember { mutableStateOf(profile?.whatsappNumber ?: "") }
    var currency by remember { mutableStateOf(profile?.currency ?: "ج.م") }
    var systemType by remember { mutableStateOf(profile?.systemType ?: "center") }

    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            phone = it.phone
            centerName = it.centerName
            whatsapp = it.whatsappNumber
            currency = it.currency
            systemType = it.systemType
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section: System Features Grid
        item {
            Text(
                text = "أقسام النظام والتحكم",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "المعلمون",
                        icon = Icons.Default.Person,
                        accentColor = PrimaryIndigoLight,
                        onClick = onNavigateToTeachers
                    )
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "المجموعات",
                        icon = Icons.Default.Groups,
                        accentColor = SuccessColorLight,
                        onClick = onNavigateToGroups
                    )
                }
                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "التقارير",
                        icon = Icons.Default.Analytics,
                        accentColor = ErrorColorLight,
                        onClick = onNavigateToReports
                    )
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "الواجبات",
                        icon = Icons.Default.Assignment,
                        accentColor = PrimaryIndigo,
                        onClick = onNavigateToAssignments
                    )
                }
                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "الامتحانات",
                        icon = Icons.Default.Quiz,
                        accentColor = WarningColor,
                        onClick = onNavigateToExams
                    )
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "لوحة الشرف",
                        icon = Icons.Default.EmojiEvents,
                        accentColor = Color(0xFFFFD700),
                        onClick = onNavigateToAchievements
                    )
                }
                // Row 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "الإنذار المبكر",
                        icon = Icons.Default.TrendingDown,
                        accentColor = ErrorColor,
                        onClick = onNavigateToAtRisk
                    )
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "النسخ الاحتياطي",
                        icon = Icons.Default.CloudQueue,
                        accentColor = PrimaryIndigoLight,
                        onClick = onNavigateToBackup
                    )
                }
                // Row 5
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "مركز الاتصالات",
                        icon = Icons.Default.Campaign,
                        accentColor = SuccessColorLight,
                        onClick = onNavigateToCommunicationCenter
                    )
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "القاعات",
                        icon = Icons.Default.MeetingRoom,
                        accentColor = PrimaryIndigo,
                        onClick = onNavigateToClassrooms
                    )
                }
                // Row 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MoreMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "نظام الجدولة",
                        icon = Icons.Default.DateRange,
                        accentColor = WarningColor,
                        onClick = onNavigateToCenterSchedule
                    )
                    if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.MANAGE_USERS).value) {
                        MoreMenuCard(
                            modifier = Modifier.weight(1f),
                            title = "المستخدمون والصلاحيات",
                            icon = Icons.Default.AdminPanelSettings,
                            accentColor = Color(0xFF9C27B0),
                            onClick = onNavigateToUserManagement
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Section: Account & Profile Configuration
        item {
            Text(
                text = "الإعدادات والملف الشخصي",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المدير المسؤول") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = PrimaryIndigoLight,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم هاتف المدير") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = PrimaryIndigoLight,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = centerName,
                onValueChange = { centerName = it },
                label = { Text("اسم السنتر التعليمي") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = PrimaryIndigoLight,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("رقم واتساب الإرسال والتواصل") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = PrimaryIndigoLight,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it },
                label = { Text("العملة الرسمية (مثال: ج.م)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = PrimaryIndigoLight,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "تخصيص نظام العمل الحالي",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "اختر طبيعة واجهة العمل للتبديل الفوري بين وضع السنتر التعليمي ووضع المعلم الفردي.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = systemType == "center",
                            onClick = { systemType = "center" },
                            label = { Text("نظام السنتر") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.2f),
                                selectedLabelColor = PrimaryIndigoLight
                            )
                        )
                        FilterChip(
                            selected = systemType == "teacher",
                            onClick = { systemType = "teacher" },
                            label = { Text("نظام المعلم الفردي") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SuccessColor.copy(alpha = 0.2f),
                                selectedLabelColor = SuccessColorLight
                            )
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onUpdateProfile(name, phone, systemType, centerName, whatsapp, currency) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("حفظ جميع التغييرات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        item {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.4f))
            ) {
                Text("تسجيل الخروج من التطبيق", color = ErrorColorLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun MoreMenuCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

// ------------------ CORE REUSABLE WIDGETS ------------------
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    badgeText: String? = null,
    badgeColor: Color = Color.Unspecified,
    badgeBg: Color = Color.Transparent,
    value: String,
    label: String
) {
    Box(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(24.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                if (badgeText != null) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        color = badgeColor,
                        modifier = Modifier
                            .background(badgeBg, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
fun RevenueChart() {
    var animateChart by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateChart = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(24.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تحليل الإيرادات والنمو",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(PrimaryIndigo, CircleShape))
                Text(
                    text = "آخر 6 أشهر",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // Main Chart Area with horizontal dashed guides
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            // Dash grid guides
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(1f, 0.66f, 0.33f, 0f).forEach { opacity ->
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = BorderColor.copy(alpha = BorderColor.alpha * opacity),
                        thickness = 1.dp
                    )
                }
            }

            // Columns Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.42f, 0.65f, 0.55f, 0.85f, 0.72f, 1f)
                val labels = listOf("12K", "18K", "15K", "24K", "20K", "30K")
                val months = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو")
                val gradients = listOf(
                    listOf(Color(0xFF2E2E3E), Color(0xFF1E1E28)),
                    listOf(Color(0xFF2E2E3E), Color(0xFF1E1E28)),
                    listOf(Color(0xFF2E2E3E), Color(0xFF1E1E28)),
                    listOf(PrimaryIndigoLight.copy(alpha = 0.5f), PrimaryIndigo.copy(alpha = 0.3f)),
                    listOf(PrimaryIndigoLight, PrimaryIndigo),
                    listOf(PrimaryIndigo, PrimaryIndigoDark)
                )
                
                heights.forEachIndexed { index, heightFraction ->
                    // Animated height
                    val animHeight by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (animateChart) heightFraction else 0f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 800,
                            delayMillis = index * 80,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        ),
                        label = "ChartBarHeight"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Value label above the bar
                        Text(
                            text = labels[index],
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (index >= 3) PrimaryIndigoLight else TextTertiary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Animated gradient bar
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .fillMaxHeight(animHeight)
                                .padding(horizontal = 5.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = gradients[index]),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Month Labels underneath the bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val months = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو")
            months.forEach { month ->
                Text(
                    text = month,
                    fontSize = 10.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RecentActivityItem(
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextTertiary
            )
        }
        
        Text(
            text = amount,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(1.dp, BorderColor)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = Icons.Default.Dashboard, 
            label = "الرئيسية", 
            isSelected = selectedTab == 0, 
            onClick = { onTabSelected(0) }
        )
        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.VIEW_STUDENTS).value) {
            BottomNavItem(
                icon = Icons.Default.School, 
                label = "الطلاب", 
                isSelected = selectedTab == 2 || selectedTab == 12 || selectedTab == 13, 
                onClick = { onTabSelected(2) }
            )
        }
        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.VIEW_TEACHERS).value) {
            BottomNavItem(
                icon = Icons.Default.People, 
                label = "المعلمون", 
                isSelected = selectedTab == 1, 
                onClick = { onTabSelected(1) }
            )
        }
        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.VIEW_ATTENDANCE).value) {
            BottomNavItem(
                icon = Icons.Default.EventAvailable, 
                label = "الحضور", 
                isSelected = selectedTab == 3, 
                onClick = { onTabSelected(3) }
            )
        }
        if (com.example.util.rbac.RbacManager.hasPermissionAsState(com.example.data.model.Permission.VIEW_FINANCIALS).value) {
            BottomNavItem(
                icon = Icons.Default.Payments, 
                label = "المالية", 
                isSelected = selectedTab == 4, 
                onClick = { onTabSelected(4) }
            )
        }
        val isMoreSelected = selectedTab == 5 || selectedTab == 6 || 
                             selectedTab == 7 || selectedTab == 8 || selectedTab == 9 || 
                             selectedTab == 10 || selectedTab == 11 || selectedTab == 14 || 
                             selectedTab == 15
        BottomNavItem(
            icon = Icons.Default.Menu, 
            label = "المزيد", 
            isSelected = isMoreSelected, 
            onClick = { onTabSelected(5) }
        )
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) PrimaryIndigoLight else TextTertiary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .widthIn(min = 60.dp)
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

// ------------------ POPUP DIALOG FORMS ------------------
@Composable
fun AddTeacherDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, subject: String, phone: String, salaryType: String, salaryValue: Double, notes: String, stages: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salaryType by remember { mutableStateOf("none") } // fixed, percentage, none
    var salaryValue by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val defaultSubjects = listOf("العربي", "الرياضيات", "العلوم", "الدراسات", "اللغة الإنجليزية", "الفيزياء", "الكيمياء", "الأحياء")
    var selectedSubjects by remember { mutableStateOf(emptySet<String>()) }
    var customSubjectsList by remember { mutableStateOf(emptyList<String>()) }
    var showCustomSubjectInput by remember { mutableStateOf(false) }
    var customSubjectText by remember { mutableStateOf("") }

    val defaultStages = listOf("المرحلة الابتدائية", "المرحلة الإعدادية", "المرحلة الثانوية", "كورسات")
    var selectedStages by remember { mutableStateOf(emptySet<String>()) }
    var customStagesList by remember { mutableStateOf(emptyList<String>()) }
    var showCustomStageInput by remember { mutableStateOf(false) }
    var customStageText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة معلم جديد", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المعلم") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                // Subjects Header and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المواد الدراسية (اختر واحدة أو أكثر):", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { showCustomSubjectInput = !showCustomSubjectInput },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryIndigoLight)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مادة أخرى", fontSize = 11.sp)
                    }
                }

                if (showCustomSubjectInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customSubjectText,
                            onValueChange = { customSubjectText = it },
                            label = { Text("اسم المادة الجديدة", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (customSubjectText.trim().isNotEmpty()) {
                                    val cleanText = customSubjectText.trim()
                                    if (!customSubjectsList.contains(cleanText)) {
                                        customSubjectsList = customSubjectsList + cleanText
                                    }
                                    selectedSubjects = selectedSubjects + cleanText
                                    customSubjectText = ""
                                    showCustomSubjectInput = false
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryIndigo, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "إضافة")
                        }
                    }
                }

                // Grid of Subjects
                val chunkedSubjects = (defaultSubjects + customSubjectsList).chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedSubjects.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                val isSelected = selectedSubjects.contains(item)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSubjects = if (isSelected) {
                                            selectedSubjects - item
                                        } else {
                                            selectedSubjects + item
                                        }
                                    },
                                    label = { Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryIndigo,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceLightDark,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Stages Header and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المراحل التعليمية (اختر واحدة أو أكثر):", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { showCustomStageInput = !showCustomStageInput },
                        colors = ButtonDefaults.textButtonColors(contentColor = SuccessColorLight)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مرحلة أخرى", fontSize = 11.sp)
                    }
                }

                if (showCustomStageInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customStageText,
                            onValueChange = { customStageText = it },
                            label = { Text("اسم المرحلة الجديدة", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (customStageText.trim().isNotEmpty()) {
                                    val cleanText = customStageText.trim()
                                    if (!customStagesList.contains(cleanText)) {
                                        customStagesList = customStagesList + cleanText
                                    }
                                    selectedStages = selectedStages + cleanText
                                    customStageText = ""
                                    showCustomStageInput = false
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = SuccessColor, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "إضافة")
                        }
                    }
                }

                // Grid of Stages
                val chunkedStages = (defaultStages + customStagesList).chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedStages.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                val isSelected = selectedStages.contains(item)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedStages = if (isSelected) {
                                            selectedStages - item
                                        } else {
                                            selectedStages + item
                                        }
                                    },
                                    label = { Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SuccessColor,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceLightDark,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                // Salary type simple picker
                Text("نظام الحساب والراتب:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { salaryType = "none" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (salaryType == "none") PrimaryIndigo else SurfaceLightDark),
                        modifier = Modifier.weight(1f)
                    ) { Text("بدون", fontSize = 11.sp) }
                    Button(
                        onClick = { salaryType = "fixed" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (salaryType == "fixed") PrimaryIndigo else SurfaceLightDark),
                        modifier = Modifier.weight(1f)
                    ) { Text("ثابت", fontSize = 11.sp) }
                    Button(
                        onClick = { salaryType = "percentage" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (salaryType == "percentage") PrimaryIndigo else SurfaceLightDark),
                        modifier = Modifier.weight(1f)
                    ) { Text("نسبة %", fontSize = 11.sp) }
                }

                if (salaryType != "none") {
                    OutlinedTextField(
                        value = salaryValue,
                        onValueChange = { salaryValue = it },
                        label = { Text(if (salaryType == "fixed") "الراتب الثابت بالجنيه" else "النسبة المئوية (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && selectedSubjects.isNotEmpty()) {
                        val valDouble = salaryValue.toDoubleOrNull() ?: 0.0
                        val subjectsString = selectedSubjects.joinToString("، ")
                        val stagesString = selectedStages.joinToString("، ")
                        onConfirm(name, subjectsString, phone, salaryType, valDouble, notes, stagesString)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                enabled = name.isNotEmpty() && selectedSubjects.isNotEmpty()
            ) {
                Text("إضافة")
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
fun EditTeacherDialog(
    teacher: Teacher,
    onDismiss: () -> Unit,
    onConfirm: (name: String, subject: String, phone: String, salaryType: String, salaryValue: Double, notes: String, stages: String) -> Unit
) {
    var name by remember { mutableStateOf(teacher.name) }
    var phone by remember { mutableStateOf(teacher.phone) }
    var salaryType by remember { mutableStateOf(teacher.salaryType) }
    var salaryValue by remember { mutableStateOf(teacher.salaryValue.toString()) }
    var notes by remember { mutableStateOf(teacher.notes) }

    val defaultSubjects = listOf("العربي", "الرياضيات", "العلوم", "الدراسات", "اللغة الإنجليزية", "الفيزياء", "الكيمياء", "الأحياء")
    var selectedSubjects by remember { 
        mutableStateOf(
            teacher.subject.split("، ").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        )
    }
    val initialCustomSubjects = remember(teacher.subject) {
        teacher.subject.split("، ").map { it.trim() }.filter { it.isNotEmpty() && !defaultSubjects.contains(it) }
    }
    var customSubjectsList by remember { mutableStateOf(initialCustomSubjects) }
    var showCustomSubjectInput by remember { mutableStateOf(false) }
    var customSubjectText by remember { mutableStateOf("") }

    val defaultStages = listOf("المرحلة الابتدائية", "المرحلة الإعدادية", "المرحلة الثانوية", "كورسات")
    var selectedStages by remember { 
        mutableStateOf(
            teacher.stages.split("، ").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        )
    }
    val initialCustomStages = remember(teacher.stages) {
        teacher.stages.split("، ").map { it.trim() }.filter { it.isNotEmpty() && !defaultStages.contains(it) }
    }
    var customStagesList by remember { mutableStateOf(initialCustomStages) }
    var showCustomStageInput by remember { mutableStateOf(false) }
    var customStageText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات المعلم", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المعلم (إجباري) *") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                // Subjects Header and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المواد الدراسية (اختر واحدة أو أكثر):", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { showCustomSubjectInput = !showCustomSubjectInput },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryIndigoLight)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مادة أخرى", fontSize = 11.sp)
                    }
                }

                if (showCustomSubjectInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customSubjectText,
                            onValueChange = { customSubjectText = it },
                            label = { Text("اسم المادة الجديدة", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (customSubjectText.trim().isNotEmpty()) {
                                    val cleanText = customSubjectText.trim()
                                    if (!customSubjectsList.contains(cleanText)) {
                                        customSubjectsList = customSubjectsList + cleanText
                                    }
                                    selectedSubjects = selectedSubjects + cleanText
                                    customSubjectText = ""
                                    showCustomSubjectInput = false
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryIndigo, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "إضافة")
                        }
                    }
                }

                // Grid of Subjects
                val chunkedSubjects = (defaultSubjects + customSubjectsList).chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedSubjects.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                val isSelected = selectedSubjects.contains(item)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSubjects = if (isSelected) {
                                            selectedSubjects - item
                                        } else {
                                            selectedSubjects + item
                                        }
                                    },
                                    label = { Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryIndigo,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceLightDark,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Stages Header and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المراحل التعليمية (اختر واحدة أو أكثر):", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { showCustomStageInput = !showCustomStageInput },
                        colors = ButtonDefaults.textButtonColors(contentColor = SuccessColorLight)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مرحلة أخرى", fontSize = 11.sp)
                    }
                }

                if (showCustomStageInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customStageText,
                            onValueChange = { customStageText = it },
                            label = { Text("اسم المرحلة الجديدة", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (customStageText.trim().isNotEmpty()) {
                                    val cleanText = customStageText.trim()
                                    if (!customStagesList.contains(cleanText)) {
                                        customStagesList = customStagesList + cleanText
                                    }
                                    selectedStages = selectedStages + cleanText
                                    customStageText = ""
                                    showCustomStageInput = false
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = SuccessColor, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "إضافة")
                        }
                    }
                }

                // Grid of Stages
                val chunkedStages = (defaultStages + customStagesList).chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedStages.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                val isSelected = selectedStages.contains(item)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedStages = if (isSelected) {
                                            selectedStages - item
                                        } else {
                                            selectedStages + item
                                        }
                                    },
                                    label = { Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SuccessColor,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceLightDark,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Text("نظام الحساب والراتب:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { salaryType = "none" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (salaryType == "none") PrimaryIndigo else SurfaceLightDark),
                        modifier = Modifier.weight(1f)
                    ) { Text("بدون", fontSize = 11.sp) }
                    Button(
                        onClick = { salaryType = "fixed" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (salaryType == "fixed") PrimaryIndigo else SurfaceLightDark),
                        modifier = Modifier.weight(1f)
                    ) { Text("ثابت", fontSize = 11.sp) }
                    Button(
                        onClick = { salaryType = "percentage" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (salaryType == "percentage") PrimaryIndigo else SurfaceLightDark),
                        modifier = Modifier.weight(1f)
                    ) { Text("نسبة %", fontSize = 11.sp) }
                }

                if (salaryType != "none") {
                    OutlinedTextField(
                        value = salaryValue,
                        onValueChange = { salaryValue = it },
                        label = { Text(if (salaryType == "fixed") "الراتب الثابت بالجنيه" else "النسبة المئوية (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && selectedSubjects.isNotEmpty()) {
                        val valDouble = salaryValue.toDoubleOrNull() ?: 0.0
                        val subjectsString = selectedSubjects.joinToString("، ")
                        val stagesString = selectedStages.joinToString("، ")
                        onConfirm(name, subjectsString, phone, salaryType, valDouble, notes, stagesString)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                enabled = name.isNotEmpty() && selectedSubjects.isNotEmpty()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentDialog(
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        parentName: String,
        parentPhone: String,
        studentPhone: String,
        grade: String,
        customCourse: String,
        teacherId: String,
        fee: Double,
        discount: Double,
        isExempt: Boolean,
        notes: String,
        type: String,
        count: Int,
        price: Double,
        total: Double,
        teacherIds: List<String>,
        teacherIdToFee: Map<String, Double>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var studentPhone by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var customCourse by remember { mutableStateOf("") }
    var selectedTeacherIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var teacherFees by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var monthlyFee by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("0.0") }
    var notes by remember { mutableStateOf("") }
    var isExempt by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var studentType by remember { mutableStateOf("GROUP") } // GROUP or PRIVATE
    var privateSessionsCount by remember { mutableStateOf("") }
    var privateSessionPrice by remember { mutableStateOf("") }
    var privateTotalAmount by remember { mutableStateOf("") }

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
        title = { Text("إضافة طالب جديد", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Name (Required)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطالب (إجباري) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Parent Phone (Optional)
                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("رقم هاتف ولي الأمر (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                // Student Phone (Optional)
                OutlinedTextField(
                    value = studentPhone,
                    onValueChange = { studentPhone = it },
                    label = { Text("رقم هاتف الطالب (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                // Grade (Dropdown from KG1 to 3rd Secondary)
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

                // Custom Course (Optional add-on course)
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
                                        Text("${t.name} (${t.salaryValue} ج.م)", color = TextPrimary)
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

                // Student Type
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
                    // Exempt Toggle (معفي)
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
                                    Text("${teacher.name} (الافتراضي: ${teacher.salaryValue} ج.م)", modifier = Modifier.weight(1f), color = TextPrimary)
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
            val context = LocalContext.current
            Button(
                onClick = {
                    if (isSaving) return@Button
                    if (name.trim().isEmpty()) {
                        Toast.makeText(context, "الرجاء إدخال اسم الطالب", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (grade.trim().isEmpty()) {
                        Toast.makeText(context, "الرجاء اختيار الصف الدراسي", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedTeacherIds.isEmpty()) {
                        Toast.makeText(context, "الرجاء اختيار معلم واحد على الأقل", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (studentType == "PRIVATE" && (privateSessionsCount.toIntOrNull() ?: 0) <= 0) {
                        Toast.makeText(context, "الرجاء إدخال عدد حصص صحيح", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

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

                    isSaving = true
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
                        teacherIdToFee,
                        {
                            isSaving = false
                            Toast.makeText(context, "✅ تم إضافة الطالب بنجاح", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        { error ->
                            isSaving = false
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الحفظ...")
                } else {
                    Text("إضافة")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("إلغاء", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun AddPaymentDialog(
    students: List<Student>,
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, String, String) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf("") }
    var selectedTeacherId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("يونيو 2026") }
    var method by remember { mutableStateOf("cash") } // cash, transfer, other
    var notes by remember { mutableStateOf("") }

    var studentDropdownExpanded by remember { mutableStateOf(false) }

    // Pre-fill amount when student is selected
    LaunchedEffect(selectedStudentId) {
        students.find { it.id == selectedStudentId }?.let {
            amount = it.netFee.toString()
            selectedTeacherId = it.teacherId
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحصيل دفعة مالية جديدة", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("اختر الطالب لتسجيل الدفع:", color = TextSecondary, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { studentDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(students.find { it.id == selectedStudentId }?.name ?: "اختر الطالب")
                    }
                    DropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        students.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name, color = TextPrimary) },
                                onClick = {
                                    selectedStudentId = s.id
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("المبلغ المستلم بالجنيه") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("الشهر الدراسي (مثال: يونيو 2026)") })
                
                Text("طريقة الدفع:", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { method = "cash" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (method == "cash") SuccessColor else SurfaceLightDark),
                        modifier = Modifier.weight(1f)
                    ) { Text("نقدي") }
                    Button(
                        onClick = { method = "transfer" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (method == "transfer") SuccessColor else SurfaceLightDark),
                        modifier = Modifier.weight(1f)
                    ) { Text("تحويل") }
                }

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStudentId.isNotEmpty()) {
                        val amountDouble = amount.toDoubleOrNull() ?: 0.0
                        onConfirm(selectedStudentId, selectedTeacherId, amountDouble, month, method, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("تسجيل دفع")
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
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Boolean, String) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isMonthly by remember { mutableStateOf(false) }
    val formattedMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل مصروف جديد", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("تصنيف المصروف (مثال: إيجار، كهرباء)") })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("المبلغ بالجنيه") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("تفاصيل ووصف إضافي") })
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = isMonthly, onCheckedChange = { isMonthly = it })
                    Text("مصروف شهري متكرر ثابت", color = TextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (category.isNotEmpty()) {
                        val amountDouble = amount.toDoubleOrNull() ?: 0.0
                        onConfirm(category, amountDouble, description, isMonthly, formattedMonth)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("تسجيل")
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

// ------------------ WHATSAPP API & PERSONALIZATION HELPERS ------------------
fun sendWhatsAppMessage(context: Context, phone: String, message: String) {
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

@Composable
fun StudentQrDialog(
    student: Student,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
    onPrint: (android.graphics.Bitmap) -> Unit,
    onShare: (android.graphics.Bitmap) -> Unit
) {
    val qrBitmap = remember(student.qrCode) {
        if (student.qrCode.isNotEmpty()) {
            com.example.util.QrCodeGenerator.generateQrCode(student.qrCode)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "رمز QR للحضور: ${student.name}",
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
                if (student.qrCode.isEmpty()) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("جاري تهيئة رمز QR فريد...", color = TextSecondary, fontSize = 13.sp)
                } else if (qrBitmap != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(220.dp)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Student QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "كود الطالب: ${student.qrCode}",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "كود ولي الأمر لربط التطبيق: ${student.parentCode}",
                        fontSize = 14.sp,
                        color = PrimaryIndigoLight,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("فشل إنشاء رمز QR", color = ErrorColor, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { qrBitmap?.let { onPrint(it) } },
                        enabled = qrBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_qr_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة QR", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { qrBitmap?.let { onShare(it) } },
                        enabled = qrBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_qr_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة QR", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                OutlinedButton(
                    onClick = onRegenerate,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningColor),
                    border = BorderStroke(1.dp, WarningColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("regenerate_qr_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة إنشاء رمز QR جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("إغلاق النافذة", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مجموعة جديدة", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المجموعة *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("اسم المعلم") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { Text("القاعة / الفصل") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("المواعيد والجدول الدراسي") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotEmpty()) onConfirm(name, teacherName, classroom, schedule, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة", color = Color.White)
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
fun AddExamDialog(
    groups: List<Group>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var totalMarks by remember { mutableStateOf("") }
    var selectedGroupIdx by remember { mutableStateOf(-1) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة امتحان جديد", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الامتحان *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = totalMarks,
                    onValueChange = { totalMarks = it },
                    label = { Text("الدرجة النهائية *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Text("المجموعة المستهدفة:", color = TextSecondary, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { dropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedGroupIdx != -1) groups[selectedGroupIdx].name else "اختر المجموعة")
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        groups.forEachIndexed { index, group ->
                            DropdownMenuItem(
                                text = { Text(group.name, color = TextPrimary) },
                                onClick = {
                                    selectedGroupIdx = index
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val marks = totalMarks.toDoubleOrNull() ?: 100.0
                    val groupId = if (selectedGroupIdx != -1) groups[selectedGroupIdx].id else ""
                    val groupName = if (selectedGroupIdx != -1) groups[selectedGroupIdx].name else ""
                    if (name.isNotEmpty() && groupId.isNotEmpty()) {
                        onConfirm(name, marks, System.currentTimeMillis(), groupId, groupName)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة", color = Color.White)
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
fun BottomSheetActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}
