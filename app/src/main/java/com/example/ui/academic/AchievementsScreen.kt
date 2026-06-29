package com.example.ui.academic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val examGrades by viewModel.examGrades.collectAsState()
    val exams by viewModel.exams.collectAsState()

    // Active students only for achievements
    val activeStudents = remember(students) { students.filter { !it.isArchived } }

    // 1. Calculate Best Attendance Winner
    val bestAttendanceWinner = remember(activeStudents, attendance) {
        if (activeStudents.isEmpty() || attendance.isEmpty()) null
        else {
            activeStudents.map { student ->
                val studentAttendance = attendance.filter { it.studentId == student.id }
                val total = studentAttendance.size
                val presents = studentAttendance.count { it.status == "present" || it.status == "late" }
                val rate = if (total > 0) presents.toFloat() / total else 0f
                student to rate
            }
            .filter { it.second > 0f }
            .maxByOrNull { it.second }
        }
    }

    // 2. Calculate Highest Grades Winner
    val highestGradesWinner = remember(activeStudents, examGrades) {
        if (activeStudents.isEmpty() || examGrades.isEmpty()) null
        else {
            activeStudents.map { student ->
                val studentGrades = examGrades.filter { it.studentId == student.id }
                val average = if (studentGrades.isNotEmpty()) {
                    studentGrades.map { it.score }.average().toFloat()
                } else 0f
                student to average
            }
            .filter { it.second > 0f }
            .maxByOrNull { it.second }
        }
    }

    // 3. Calculate Most Punctual Payments Winner
    val punctualPaymentsWinner = remember(activeStudents, payments) {
        if (activeStudents.isEmpty() || payments.isEmpty()) null
        else {
            activeStudents.map { student ->
                val studentPaymentsCount = payments.count { it.studentId == student.id }
                student to studentPaymentsCount
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
        }
    }

    // 4. Calculate Ideal Student (Overall MVP)
    val idealStudentWinner = remember(activeStudents, attendance, examGrades, payments) {
        if (activeStudents.isEmpty()) null
        else {
            activeStudents.map { student ->
                // Attendance component (40 points max)
                val studentAttendance = attendance.filter { it.studentId == student.id }
                val attRate = if (studentAttendance.isNotEmpty()) {
                    studentAttendance.count { it.status == "present" || it.status == "late" }.toFloat() / studentAttendance.size
                } else 0.5f // Neutral default if no history

                // Grades component (40 points max)
                val studentGrades = examGrades.filter { it.studentId == student.id }
                val gradeRate = if (studentGrades.isNotEmpty()) {
                    val avg = studentGrades.map { it.score }.average()
                    // Map score 100 max to a 0..1 scale
                    val maxPossible = 100.0
                    (avg / maxPossible).coerceIn(0.0, 1.0).toFloat()
                } else 0.5f

                // Payments component (20 points max)
                val paymentCount = payments.count { it.studentId == student.id }
                val paymentScore = if (paymentCount > 0) 1.0f else 0.0f

                val compositeScore = (attRate * 40f) + (gradeRate * 40f) + (paymentScore * 20f)
                Triple(student, compositeScore, Pair(attRate, gradeRate))
            }
            .maxByOrNull { it.second }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "لوحة الإنجازات والتميز 🏆",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("achievements_back_btn")) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero Title Banner
            item {
                AchievementsHeroBanner()
            }

            // Ideal Student Shield (The Highest Honor)
            item {
                if (idealStudentWinner != null) {
                    val (student, score, metrics) = idealStudentWinner
                    val (attRate, gradeRate) = metrics
                    AchievementCategoryCard(
                        title = "درع الطالب المثالي 👑",
                        subtitle = "التميز الشامل في الحضور والدرجات والالتزام بالسداد",
                        studentName = student.name,
                        studentGrade = student.grade,
                        achievementDetail = "معدل الحضور: ${(attRate * 100).toInt()}% • نسبة التفوق الأكاديمي: ${(gradeRate * 100).toInt()}%",
                        cardColor = Brush.horizontalGradient(listOf(Color(0xFFFFD700).copy(alpha = 0.15f), Color(0xFFFFA500).copy(alpha = 0.08f))),
                        accentColor = Color(0xFFFFD700),
                        icon = Icons.Default.EmojiEvents,
                        congratsMsg = "أحسنت يا بطل! لقد أحرزت أعلى درجة تقييم تراكمي شاملة للعام الدراسي الحالي."
                    )
                } else {
                    EmptyAchievementPlaceholder(title = "درع الطالب المثالي 👑", reason = "لم يتم تحديد طالب مثالي لعدم توفر بيانات كافية.")
                }
            }

            // Best Attendance Card
            item {
                if (bestAttendanceWinner != null) {
                    val (student, rate) = bestAttendanceWinner
                    AchievementCategoryCard(
                        title = "🏆 الحضور والالتزام المثالي",
                        subtitle = "صاحب السجل الأنقى والأعلى في نسبة حضور الحصص",
                        studentName = student.name,
                        studentGrade = student.grade,
                        achievementDetail = "نسبة الحضور المسجلة: ${(rate * 100).toInt()}%",
                        cardColor = Brush.horizontalGradient(listOf(Color(0xFF2196F3).copy(alpha = 0.15f), Color(0xFF00BCD4).copy(alpha = 0.08f))),
                        accentColor = Color(0xFF2196F3),
                        icon = Icons.Default.CalendarToday,
                        congratsMsg = "نشيد بالتزامك الدائم بالوقت والمواظبة على طلب العلم!"
                    )
                } else {
                    EmptyAchievementPlaceholder(title = "🏆 الحضور والالتزام المثالي", reason = "لا توجد حركات حضور مسجلة بعد.")
                }
            }

            // Highest Grades Card
            item {
                if (highestGradesWinner != null) {
                    val (student, avg) = highestGradesWinner
                    AchievementCategoryCard(
                        title = "🏆 وسام التفوق الأكاديمي",
                        subtitle = "الحاصل على التقييم الأعلى ودرجة الامتياز في الاختبارات",
                        studentName = student.name,
                        studentGrade = student.grade,
                        achievementDetail = "متوسط درجات الاختبارات: ${String.format("%.1f", avg)}",
                        cardColor = Brush.horizontalGradient(listOf(Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF8BC34A).copy(alpha = 0.08f))),
                        accentColor = Color(0xFF4CAF50),
                        icon = Icons.Default.AutoAwesome,
                        congratsMsg = "عبقري متميز يستحق الفخر والتقدير لمستواه الأكاديمي الرائع."
                    )
                } else {
                    EmptyAchievementPlaceholder(title = "🏆 وسام التفوق الأكاديمي", reason = "لا توجد درجات اختبارات مسجلة بعد.")
                }
            }

            // Punctual Payments Card
            item {
                if (punctualPaymentsWinner != null) {
                    val (student, count) = punctualPaymentsWinner
                    AchievementCategoryCard(
                        title = "🏆 الالتزام بالسداد والاستقرار المالي",
                        subtitle = "الأكثر التزاماً وانتظاماً بسداد رسوم الاشتراك والدروس",
                        studentName = student.name,
                        studentGrade = student.grade,
                        achievementDetail = "إجمالي حركات السداد الملتزم بها: $count عمليات دفع",
                        cardColor = Brush.horizontalGradient(listOf(Color(0xFFE91E63).copy(alpha = 0.15f), Color(0xFF9C27B0).copy(alpha = 0.08f))),
                        accentColor = Color(0xFFE91E63),
                        icon = Icons.Default.AccountBalanceWallet,
                        congratsMsg = "كل الشكر والامتنان لأولياء الأمور الكرام على مساهمتهم الفعالة والتزامهم."
                    )
                } else {
                    EmptyAchievementPlaceholder(title = "🏆 الالتزام بالسداد والاستقرار المالي", reason = "لا توجد عمليات دفع مسجلة في كشوفات الحساب حالياً.")
                }
            }
        }
    }
}

@Composable
fun AchievementsHeroBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD700).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "لوحة الشرف وتكريم المتميزين 🌟",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "تُحتسب أوسمة التميز تلقائياً ومباشرة بناءً على السجلات الفعلية للحضور والدرجات والانتظام المالي بقاعدة البيانات.",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun AchievementCategoryCard(
    title: String,
    subtitle: String,
    studentName: String,
    studentGrade: String,
    achievementDetail: String,
    cardColor: Brush,
    accentColor: Color,
    icon: ImageVector,
    congratsMsg: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(cardColor)
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Winner Student Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = studentName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "الصف الدراسي: $studentGrade",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats detail pill
                Text(
                    text = achievementDetail,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Congratulations message
                Text(
                    text = "🎉 $congratsMsg",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyAchievementPlaceholder(title: String, reason: String) {
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
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reason,
                fontSize = 11.sp,
                color = TextTertiary,
                lineHeight = 15.sp
            )
        }
    }
}
