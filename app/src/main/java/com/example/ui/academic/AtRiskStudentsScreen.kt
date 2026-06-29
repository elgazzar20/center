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
import com.example.data.model.*
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import com.example.util.StudentAnalytics
import com.example.util.StudentAnalyticsEngine
import com.example.util.StudentEvaluation
import com.example.util.StudentEvaluationService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtRiskStudentsScreen(
    viewModel: AppViewModel,
    onNavigateToStudentProfile: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val examGrades by viewModel.examGrades.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Most Absent, 1: Most Payment Delayed, 2: Lowest Grades
    var searchKeyword by remember { mutableStateOf("") }

    // Calculate analytics for all active students
    val studentsWithAnalytics = remember(students, attendance, payments, exams, examGrades) {
        students.filter { it.isActive }.map { student ->
            val analyticData = StudentAnalyticsEngine.calculateAnalytics(
                student = student,
                allAttendance = attendance,
                allPayments = payments,
                allExams = exams,
                allExamGrades = examGrades
            )
            val eval = StudentEvaluationService.evaluateStudent(analyticData, student)
            Triple(student, analyticData, eval)
        }
    }

    // Filter students depending on activeTab criteria and search text
    val filteredRiskStudents = remember(studentsWithAnalytics, activeTab, searchKeyword) {
        val filteredBySearch = studentsWithAnalytics.filter { (student, _, _) ->
            student.name.contains(searchKeyword, ignoreCase = true) ||
                    student.grade.contains(searchKeyword, ignoreCase = true)
        }

        when (activeTab) {
            0 -> { // Most Absent (attendanceRate < 75% or absenceRate > 0 sorted by highest absence)
                filteredBySearch
                    .filter { (_, analytics, _) -> analytics.totalAttendanceCount > 0 && analytics.absenceRate > 20.0 }
                    .sortedByDescending { (_, analytics, _) -> analytics.absenceRate }
            }
            1 -> { // Most Payment Delayed (unpaid months >= 1 and not exempt, sorted by highest unpaid months)
                filteredBySearch
                    .filter { (student, analytics, _) -> !student.isExempt && analytics.unpaidMonthsCount >= 1 }
                    .sortedByDescending { (_, analytics, _) -> analytics.unpaidMonthsCount }
            }
            2 -> { // Lowest Grades (averageGrade < 60.0 or sorted by lowest average grade, filtering only those with graded exams)
                filteredBySearch
                    .filter { (_, analytics, _) -> analytics.gradedExamsCount > 0 && analytics.averageGrade < 60.0 }
                    .sortedBy { (_, analytics, _) -> analytics.averageGrade }
            }
            else -> filteredBySearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الطلاب المعرضون للتراجع الدراسي والمالي",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("at_risk_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة للخلف",
                            tint = TextPrimary
                        )
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
            // Screen Header Description Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, ErrorColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = ErrorColorLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "نظام التوجيه والمتابعة الذكية",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorColorLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "يساعدك هذا التحليل في استقصاء الطلاب المتعثرين دراسياً، ماليّاً، أو من يعانون من تكرر الغيابات لاتخاذ إجراء فوري بالتواصل مع أولياء الأمور.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Tabs to switch categories
            RiskCategoryTabs(activeTab = activeTab, onTabSelected = { activeTab = it })

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("at_risk_search_input"),
                placeholder = { Text("بحث عن طالب أو صف دراسي...", fontSize = 12.sp, color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderColor
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Students List
            if (filteredRiskStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = SuccessColorLight,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "أخبار سارة! لا يوجد طلاب يطابقون هذه الفئة حالياً.",
                            color = SuccessColorLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "جميع الطلاب في حالة التزام مالي ودراسي ممتازة.",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredRiskStudents) { (student, analytics, eval) ->
                        RiskStudentRowItem(
                            student = student,
                            analytics = analytics,
                            evaluation = eval,
                            activeTab = activeTab,
                            onViewProfile = { onNavigateToStudentProfile(student.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RiskCategoryTabs(activeTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val categories = listOf("الأكثر غياباً 🚫", "المتأخرون بالدفع 💵", "الأقل درجات 📝")
        categories.forEachIndexed { index, title ->
            val isSelected = activeTab == index
            val tabColor = when (index) {
                0 -> ErrorColorLight
                1 -> WarningColor
                2 -> PrimaryIndigoLight
                else -> PrimaryIndigo
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) tabColor.copy(alpha = 0.15f) else Color.Transparent)
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) tabColor.copy(alpha = 0.4f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) tabColor else TextSecondary
                )
            }
        }
    }
}

@Composable
fun RiskStudentRowItem(
    student: Student,
    analytics: StudentAnalytics,
    evaluation: StudentEvaluation,
    activeTab: Int,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier
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
                Column {
                    Text(
                        text = student.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = student.grade,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Main Indicator Badge based on active tab
                when (activeTab) {
                    0 -> {
                        RiskIndicatorBadge(
                            label = "نسبة الغياب",
                            value = "${analytics.absenceRate.toInt()}%",
                            color = ErrorColorLight
                        )
                    }
                    1 -> {
                        RiskIndicatorBadge(
                            label = "شهور متأخرة",
                            value = "${analytics.unpaidMonthsCount} شهور",
                            color = WarningColor
                        )
                    }
                    2 -> {
                        RiskIndicatorBadge(
                            label = "معدل الدرجات",
                            value = "${analytics.averageGrade.toInt()}%",
                            color = PrimaryIndigoLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(10.dp))

            // Show automated system explanation reason
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = evaluation.reason,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Call to action button to view profile
            Button(
                onClick = onViewProfile,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(1.dp, PrimaryIndigo.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .testTag("view_student_analysis_${student.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "استعراض التحليل المفصل والملف الأكاديمي 📊",
                    fontSize = 11.sp,
                    color = PrimaryIndigoLight,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RiskIndicatorBadge(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 9.sp, color = TextSecondary)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
