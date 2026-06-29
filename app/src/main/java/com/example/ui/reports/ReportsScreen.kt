package com.example.ui.reports

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
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: AppViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null,
    onAiAssistantClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // State from ViewModel
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val groups by viewModel.teachers.collectAsStateWithLifecycle() // Actually, let's load groups correctly
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()

    // Since 'groups' table comes from appDao directly, we might need Groups list.
    // Let's create an elegant temporary load for groups, or query it using a remember.
    // Wait, let's load groups from AppDatabase directly, or let's inspect where groups are stored in ViewModel.
    // Ah, AppViewModel does not expose groups directly, but GroupsViewModel does. 
    // Wait! Can we get groups from database in AppViewModel or query from database?
    // Let's look at com.example.data.database.AppDatabase.getDatabase(context).appDao().getAllGroups()
    val database = remember { com.example.data.database.AppDatabase.getDatabase(context) }
    val groupsListState = remember { database.appDao().getAllGroups() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val groupsList = groupsListState.value

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Student, 1: Group, 2: Financial
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "نظام التقارير الذكي",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "استخراج ومشاركة تقارير PDF احترافية لسنترك التعليمي",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                        }
                    }
                },
                actions = {
                    if (onAiAssistantClick != null) {
                        IconButton(
                            onClick = onAiAssistantClick,
                            modifier = Modifier.testTag("reports_ai_assistant_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Nexora AI Assistant",
                                tint = PrimaryIndigoLight
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    scrolledContainerColor = SurfaceDark
                ),
                scrollBehavior = scrollBehavior,
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Elegant Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = PrimaryIndigoLight,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryIndigoLight
                    )
                },
                divider = { HorizontalDivider(color = BorderColor) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("تقرير طالب فردي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) PrimaryIndigoLight else TextSecondary) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("تقرير مجموعة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) PrimaryIndigoLight else TextSecondary) },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("تقرير مالي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 2) PrimaryIndigoLight else TextSecondary) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("سجل العمليات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 3) PrimaryIndigoLight else TextSecondary) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "ReportsTabChange"
            ) { tab ->
                when (tab) {
                    0 -> StudentReportTab(
                        profile = profile,
                        students = students,
                        groups = groupsList,
                        attendance = attendance,
                        payments = payments
                    )
                    1 -> GroupReportTab(
                        profile = profile,
                        groups = groupsList,
                        students = students,
                        attendance = attendance,
                        payments = payments
                    )
                    2 -> FinancialReportTab(
                        profile = profile,
                        payments = payments,
                        students = students,
                        groups = groupsList,
                        expenses = expenses
                    )
                    3 -> ActivityLogScreen(
                        viewModel = viewModel,
                        onBackClick = { selectedTab = 0 },
                        isEmbedded = true
                    )
                }
            }
        }
    }
}

/**
 * 1. STUDENT REPORT TAB
 */
@Composable
fun StudentReportTab(
    profile: Profile?,
    students: List<Student>,
    groups: List<Group>,
    attendance: List<Attendance>,
    payments: List<Payment>
) {
    val context = LocalContext.current
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredStudents = remember(students, searchQuery) {
        students.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Dropdown Search Input
        Text("اختر الطالب لإنشاء التقرير له:", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery.ifEmpty { selectedStudent?.name ?: "" },
                onValueChange = {
                    searchQuery = it
                    dropdownExpanded = true
                },
                placeholder = { Text("ابحث عن اسم الطالب...", color = TextTertiary) },
                trailingIcon = {
                    IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp)
            )

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                if (filteredStudents.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("لا يوجد طلاب بهذا الاسم", color = TextSecondary) },
                        onClick = {}
                    )
                } else {
                    filteredStudents.take(8).forEach { student ->
                        DropdownMenuItem(
                            text = { Text(student.name, color = TextPrimary) },
                            onClick = {
                                selectedStudent = student
                                searchQuery = ""
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (selectedStudent == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("الرجاء اختيار طالب لعرض الإحصائيات وبطاقة التقارير", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            val student = selectedStudent!!
            val studentGroup = groups.find { it.id == student.groupId }
            val studentAttendance = attendance.filter { it.studentId == student.id }
            val studentPayments = payments.filter { it.studentId == student.id }

            // Compute statistics
            val presentCount = studentAttendance.count { it.status == "present" }
            val absentCount = studentAttendance.count { it.status == "absent" }
            val lateCount = studentAttendance.count { it.status == "late" }
            val totalAtt = studentAttendance.size
            val commitmentRate = if (totalAtt > 0) ((presentCount + lateCount).toFloat() / totalAtt * 100).toInt() else 100

            val totalPaid = studentPayments.sumOf { it.amount }
            
            // Expected fees based on months joined
            val monthsJoined = maxOf(1, getMonthsDifference(student.registrationDate, System.currentTimeMillis()))
            val expectedFee = student.netFee * monthsJoined
            val arrears = maxOf(0.0, expectedFee - totalPaid)
            val currency = profile?.currency ?: "ج.م"

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Actions Banner
                item {
                    ReportActionCard(
                        title = "تصدير تقرير الطالب الفردي",
                        onShare = {
                            val file = PdfGeneratorService.generateStudentReport(context, profile, student, studentGroup, studentAttendance, studentPayments)
                            PdfGeneratorService.sharePdf(context, file)
                        },
                        onPrint = {
                            val file = PdfGeneratorService.generateStudentReport(context, profile, student, studentGroup, studentAttendance, studentPayments)
                            PdfGeneratorService.printPdf(context, file, "تقرير طالب - ${student.name}")
                        },
                        onSave = {
                            val file = PdfGeneratorService.generateStudentReport(context, profile, student, studentGroup, studentAttendance, studentPayments)
                            PdfGeneratorService.savePdfToDownloads(context, file, "student_${student.name.replace(" ", "_")}_report.pdf")
                        }
                    )
                }

                // Student Card Preview
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryIndigo.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = PrimaryIndigoLight)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(student.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("كود الطالب: #${student.id.take(6).uppercase()}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = BorderColor)

                            // Properties
                            ReportInfoRow("المجموعة الدراسية", studentGroup?.name ?: "غير مسجل بمجموعة")
                            ReportInfoRow("السنة الدراسية", student.grade.ifEmpty { "غير محدد" })
                            ReportInfoRow("الكورس / المادة", student.customCourse.ifEmpty { "غير محدد" })
                            ReportInfoRow("هاتف الطالب", student.studentPhone.ifEmpty { "غير مسجل" })
                            ReportInfoRow("ولي الأمر", "${student.parentName} (${student.parentPhone.ifEmpty { "بدون هاتف" }})")
                        }
                    }
                }

                // Stats Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsSmallCard(
                            title = "نسبة الالتزام",
                            value = "$commitmentRate%",
                            color = SuccessColorLight,
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                        StatsSmallCard(
                            title = "حضور / غياب",
                            value = "$presentCount / $absentCount",
                            color = WarningColor,
                            icon = Icons.Default.FactCheck,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Financial Summary
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("الذمة المالية للعام الحالي", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(14.dp))

                            ReportInfoRow("قيمة الاشتراك الشهري", "${student.monthlyFee} $currency")
                            ReportInfoRow("الخصم المطبق", "${student.discount}%")
                            ReportInfoRow("الصافي المطلوب شهرياً", "${student.netFee} $currency")
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderColor)

                            ReportInfoRow("إجمالي المبالغ المسددة", "$totalPaid $currency", color = SuccessColorLight)
                            ReportInfoRow("إجمالي المتأخرات المطلوبة", "$arrears $currency", color = if (arrears > 0) ErrorColorLight else TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. GROUP REPORT TAB
 */
@Composable
fun GroupReportTab(
    profile: Profile?,
    groups: List<Group>,
    students: List<Student>,
    attendance: List<Attendance>,
    payments: List<Payment>
) {
    val context = LocalContext.current
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Dropdown selection
        Text("اختر المجموعة لتصدير التقرير:", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedGroup?.name ?: "اضغط للاختيار من المجموعات...",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { dropdownExpanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dropdownExpanded = true },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = false
            )

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                if (groups.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("لا يوجد مجموعات حالية بالسنتر", color = TextSecondary) },
                        onClick = {}
                    )
                } else {
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name, color = TextPrimary) },
                            onClick = {
                                selectedGroup = group
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (selectedGroup == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("اختر مجموعة دراسية لاستعراض الإحصائيات الشاملة وتحميل ملف PDF", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            val group = selectedGroup!!
            val studentsInGroup = remember(students, group) {
                students.filter { it.groupId == group.id }
            }

            // Calculations
            val studentCount = studentsInGroup.size
            val studentIds = studentsInGroup.map { it.id }.toSet()
            val groupAttendance = attendance.filter { it.studentId in studentIds }
            val totalAtt = groupAttendance.size
            val presentAtt = groupAttendance.count { it.status == "present" || it.status == "late" }
            val attendancePercent = if (totalAtt > 0) (presentAtt.toFloat() / totalAtt * 100).toInt() else 100

            val groupPayments = payments.filter { it.studentId in studentIds }
            val revenues = groupPayments.sumOf { it.amount }

            var totalArrears = 0.0
            studentsInGroup.forEach { student ->
                val sPay = groupPayments.filter { it.studentId == student.id }.sumOf { it.amount }
                val months = maxOf(1, getMonthsDifference(student.registrationDate, System.currentTimeMillis()))
                val expected = student.netFee * months
                totalArrears += maxOf(0.0, expected - sPay)
            }

            val currency = profile?.currency ?: "ج.م"

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // PDF Actions
                item {
                    ReportActionCard(
                        title = "تصدير تقرير المجموعة الشامل",
                        onShare = {
                            val file = PdfGeneratorService.generateGroupReport(context, profile, group, studentsInGroup, attendance, payments)
                            PdfGeneratorService.sharePdf(context, file)
                        },
                        onPrint = {
                            val file = PdfGeneratorService.generateGroupReport(context, profile, group, studentsInGroup, attendance, payments)
                            PdfGeneratorService.printPdf(context, file, "تقرير مجموعة - ${group.name}")
                        },
                        onSave = {
                            val file = PdfGeneratorService.generateGroupReport(context, profile, group, studentsInGroup, attendance, payments)
                            PdfGeneratorService.savePdfToDownloads(context, file, "group_${group.name.replace(" ", "_")}_report.pdf")
                        }
                    )
                }

                // Stats overview
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatsSmallCard(
                            title = "الطلاب",
                            value = "$studentCount طلاب",
                            color = PrimaryIndigoLight,
                            icon = Icons.Default.Groups,
                            modifier = Modifier.weight(1f)
                        )
                        StatsSmallCard(
                            title = "نسبة حضورها",
                            value = "$attendancePercent%",
                            color = SuccessColorLight,
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Stats overview 2
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatsSmallCard(
                            title = "الإيرادات المحصلة",
                            value = "$revenues $currency",
                            color = SuccessColorLight,
                            icon = Icons.Default.Payments,
                            modifier = Modifier.weight(1f)
                        )
                        StatsSmallCard(
                            title = "المتأخرات المعلقة",
                            value = "$totalArrears $currency",
                            color = ErrorColorLight,
                            icon = Icons.Default.MoneyOff,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Student list in group header
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("قائمة طلاب المجموعة وموقفهم المالي", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(14.dp))

                            if (studentsInGroup.isEmpty()) {
                                Text("لا يوجد طلاب مسجلين في هذه المجموعة حالياً.", color = TextSecondary, fontSize = 12.sp)
                            } else {
                                studentsInGroup.forEach { student ->
                                    val sPay = groupPayments.filter { it.studentId == student.id }.sumOf { it.amount }
                                    val months = maxOf(1, getMonthsDifference(student.registrationDate, System.currentTimeMillis()))
                                    val expected = student.netFee * months
                                    val due = maxOf(0.0, expected - sPay)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(student.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(if (student.isExempt) "طالب معفي (منحة)" else "رسوم الكورس: ${student.netFee} $currency", color = TextSecondary, fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("المتأخرات: $due $currency", color = if (due > 0) ErrorColorLight else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("المدفوع: $sPay $currency", color = SuccessColorLight, fontSize = 10.sp)
                                        }
                                    }
                                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. FINANCIAL REPORT TAB
 */
@Composable
fun FinancialReportTab(
    profile: Profile?,
    payments: List<Payment>,
    students: List<Student>,
    groups: List<Group>,
    expenses: List<com.example.data.model.Expense>
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
    val currentYearStr = SimpleDateFormat("yyyy", Locale.US).format(calendar.time)

    val currency = profile?.currency ?: "ج.م"

    // Calculations
    val monthlyRevenues = remember(payments, currentMonthStr) {
        payments.filter { it.month == currentMonthStr }.sumOf { it.amount }
    }
    val yearlyRevenues = remember(payments, currentYearStr) {
        payments.filter { it.month.startsWith(currentYearStr) }.sumOf { it.amount }
    }
    val monthlyExpenses = remember(expenses, currentMonthStr) {
        expenses.filter { it.month == currentMonthStr }.sumOf { it.amount }
    }
    val netProfit = monthlyRevenues - monthlyExpenses

    // Late students count and exempt list
    val exemptStudents = remember(students) { students.filter { it.isExempt } }
    
    val lateStudents = remember(students, payments) {
        students.map { student ->
            val sPayments = payments.filter { it.studentId == student.id }
            val paid = sPayments.sumOf { it.amount }
            val months = maxOf(1, getMonthsDifference(student.registrationDate, System.currentTimeMillis()))
            val expected = student.netFee * months
            val due = maxOf(0.0, expected - paid)
            Pair(student, due)
        }.filter { it.second > 0 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // PDF Export card
        item {
            ReportActionCard(
                title = "تصدير التقرير المالي الختامي",
                onShare = {
                    val file = PdfGeneratorService.generateFinancialReport(context, profile, payments, students, groups, expenses)
                    PdfGeneratorService.sharePdf(context, file)
                },
                onPrint = {
                    val file = PdfGeneratorService.generateFinancialReport(context, profile, payments, students, groups, expenses)
                    PdfGeneratorService.printPdf(context, file, "التقرير المالي - $currentMonthStr")
                },
                onSave = {
                    val file = PdfGeneratorService.generateFinancialReport(context, profile, payments, students, groups, expenses)
                    PdfGeneratorService.savePdfToDownloads(context, file, "financial_report_${currentMonthStr}.pdf")
                }
            )
        }

        // Row of Stats (Revenues / Expenses)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatsSmallCard(
                    title = "إيرادات الشهر ($currentMonthStr)",
                    value = "$monthlyRevenues $currency",
                    color = SuccessColorLight,
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
                StatsSmallCard(
                    title = "مصروفات الشهر",
                    value = "$monthlyExpenses $currency",
                    color = ErrorColorLight,
                    icon = Icons.Default.MoneyOff,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Profit card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("صافي الأرباح لشهر $currentMonthStr", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("$netProfit $currency", color = if (netProfit >= 0) SuccessColorLight else ErrorColorLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (netProfit >= 0) SuccessColor.copy(alpha = 0.15f) else ErrorColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (netProfit >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (netProfit >= 0) SuccessColorLight else ErrorColorLight
                        )
                    }
                }
            }
        }

        // Yearly stats card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("إجمالي الحسابات للعام الحالي $currentYearStr", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    ReportInfoRow("إجمالي إيرادات السنة المحصلة", "$yearlyRevenues $currency", color = SuccessColorLight)
                    ReportInfoRow("عدد الطلاب الذين لديهم متأخرات", "${lateStudents.size} طالباً", color = ErrorColorLight)
                    ReportInfoRow("عدد الطلاب المستفيدين من إعفاء (منحة)", "${exemptStudents.size} طلاب", color = PrimaryIndigoLight)
                }
            }
        }

        // List of Late Students (preview)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("الطلاب المتأخرين في الدفع (أعلى 5)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (lateStudents.isEmpty()) {
                        Text("ممتاز! لا يوجد أي طلاب متأخرين في السداد حالياً.", color = SuccessColorLight, fontSize = 12.sp)
                    } else {
                        lateStudents.sortedByDescending { it.second }.take(5).forEach { (student, arrears) ->
                            val gName = groups.find { it.id == student.groupId }?.name ?: "بدون مجموعة"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(student.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("المجموعة: $gName", color = TextSecondary, fontSize = 11.sp)
                                }
                                Text("$arrears $currency", color = ErrorColorLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * REUSABLE COMPONENTS FOR REPORTS
 */
@Composable
fun ReportActionCard(
    title: String,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        BorderColor,
                        PrimaryIndigo.copy(alpha = 0.2f),
                        BorderColor
                    )
                ),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Share
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Print
                Button(
                    onClick = onPrint,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طباعة", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                // Save
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReportInfoRow(label: String, value: String, color: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatsSmallCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(
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
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun getMonthsDifference(startMillis: Long, endMillis: Long): Int {
    val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
    val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
    
    val yearsDiff = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
    val monthsDiff = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
    
    return maxOf(1, (yearsDiff * 12) + monthsDiff)
}
