package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToStudents: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onAiAssistantClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val refreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        state = refreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = SurfaceDark,
                color = PrimaryIndigoLight,
                state = refreshState
            )
        }
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "DashboardState"
        ) { state ->
            when (state) {
                is DashboardUiState.Loading -> {
                    DashboardSkeleton()
                }
                is DashboardUiState.Success -> {
                    DashboardContent(
                        data = state,
                        onNavigateToStudents = onNavigateToStudents,
                        onNavigateToAttendance = onNavigateToAttendance,
                        onNavigateToFinance = onNavigateToFinance,
                        onNavigateToReports = onNavigateToReports,
                        onAiAssistantClick = onAiAssistantClick
                    )
                }
                is DashboardUiState.Error -> {
                    DashboardError(message = state.message, onRetry = { viewModel.refresh() })
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    data: DashboardUiState.Success,
    onNavigateToStudents: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToReports: () -> Unit,
    onAiAssistantClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Welcome Header is handled by TopAppBar in CenterDashboardScreen

        // Search Bar AI Command Center
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clickable { onAiAssistantClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "اسأل Nexora AI...",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Quick Statistics
        item {
            Text(
                text = "إحصائيات سريعة",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.School,
                    iconColor = WarningColor,
                    value = data.totalStudents.toString(),
                    label = "عدد الطلاب"
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    iconColor = TextSecondary,
                    value = data.totalTeachers.toString(),
                    label = "عدد المدرسين"
                )
            }
        }

        // 3. Financial Overview
        item {
            Text(
                text = "نظرة مالية",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingUp,
                        iconColor = SuccessColor,
                        value = String.format(Locale.US, "%,.0f", data.todaysIncome) + " ${data.currency}",
                        label = "دخل اليوم"
                    )
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AttachMoney,
                        iconColor = SuccessColorLight,
                        value = String.format(Locale.US, "%,.0f", data.totalFeesCollected) + " ${data.currency}",
                        label = "دخل الشهر"
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = PrimaryIndigoLight,
                        value = String.format(Locale.US, "%,.0f", data.totalRevenue) + " ${data.currency}",
                        label = "الإيرادات"
                    )
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MoneyOff,
                        iconColor = ErrorColorLight,
                        value = String.format(Locale.US, "%,.0f", data.totalExpenses) + " ${data.currency}",
                        label = "المصروفات"
                    )
                }
                DashboardStatCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.MonetizationOn,
                    iconColor = if (data.netProfit >= 0) SuccessColor else ErrorColor,
                    value = String.format(Locale.US, "%,.0f", data.netProfit) + " ${data.currency}",
                    label = "صافي الربح"
                )
            }
        }

        // 3.5 Financial Flow Chart removed per request

        // 4. Today's Activity Board
        item {
            Text(
                text = "لوحة نشاط اليوم",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActivityStatBox(modifier = Modifier.weight(1f), title = "الحضور", value = data.presentToday.toString(), color = SuccessColor)
                        ActivityStatBox(modifier = Modifier.weight(1f), title = "الغياب", value = data.absentToday.toString(), color = ErrorColor)
                        ActivityStatBox(modifier = Modifier.weight(1f), title = "متأخرين", value = data.studentsLateInPayment.toString(), color = WarningColor)
                    }
                    
                    HorizontalDivider(color = BorderColor)
                    
                    // Timeline
                    val timelineItems = remember(data) {
                        val items = mutableListOf<TimelineItem>()
                        
                        val cal = Calendar.getInstance()
                        val currentYear = cal.get(Calendar.YEAR)
                        val currentMonth = cal.get(Calendar.MONTH)
                        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                        
                        fun getMsFromSchedule(schedule: String): Long {
                            val match = Regex("\\d+").find(schedule)
                            var hour = match?.value?.toIntOrNull() ?: 0
                            if (schedule.contains("م") && hour != 12) hour += 12
                            val classCal = Calendar.getInstance().apply {
                                set(currentYear, currentMonth, currentDay, hour, 0, 0)
                            }
                            return classCal.timeInMillis
                        }
                        
                        data.currentClasses.forEach { group ->
                            items.add(TimelineItem.ClassItem(group, true, getMsFromSchedule(group.schedule)))
                        }
                        
                        data.upcomingClasses.forEach { group ->
                            items.add(TimelineItem.ClassItem(group, false, getMsFromSchedule(group.schedule)))
                        }
                        
                        data.recentOperations.forEach { log ->
                            items.add(TimelineItem.OperationItem(log, log.timestamp))
                        }
                        
                        items.sortedByDescending { it.timestamp }
                    }
                    
                    if (timelineItems.isEmpty()) {
                        Text(
                            text = "لا يوجد نشاط اليوم حتى الآن",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)
                        )
                    } else {
                        Column {
                            timelineItems.forEachIndexed { index, item ->
                                TimelineItemRow(item = item, isLast = index == timelineItems.size - 1)
                            }
                        }
                    }
                }
            }
        }

        // 5. Quick Actions
        item {
            Text(
                text = "الوصول السريع للمهام",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GridMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "الطلاب",
                        description = "إدارة وسجلات الطلاب",
                        icon = Icons.Default.School,
                        accentColor = PrimaryIndigo,
                        testTag = "nav_students_card",
                        onClick = onNavigateToStudents
                    )
                    GridMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "الحضور",
                        description = "تسجيل ومتابعة الغياب",
                        icon = Icons.Default.EventAvailable,
                        accentColor = SuccessColor,
                        testTag = "nav_attendance_card",
                        onClick = onNavigateToAttendance
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GridMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "المالية",
                        description = "المدفوعات والمصروفات",
                        icon = Icons.Default.Payments,
                        accentColor = WarningColor,
                        testTag = "nav_finance_card",
                        onClick = onNavigateToFinance
                    )
                    GridMenuCard(
                        modifier = Modifier.weight(1f),
                        title = "التقارير",
                        description = "تقارير الأداء والمحاسبة",
                        icon = Icons.Default.Analytics,
                        accentColor = ErrorColor,
                        testTag = "nav_reports_card",
                        onClick = onNavigateToReports
                    )
                }
            }
        }



    }
}

@Composable
fun GridMenuCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(125.dp)
            .testTag(testTag)
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        BorderColor,
                        accentColor.copy(alpha = 0.2f),
                        BorderColor
                    )
                ),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DashboardSkeleton() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // AI Center Skeleton
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmer()
            )
        }

        // Subtitle skeleton
        item {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
        }

        // 2x2 Grid Skeletons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f).height(115.dp).clip(RoundedCornerShape(20.dp)).shimmer())
                    Box(modifier = Modifier.weight(1f).height(115.dp).clip(RoundedCornerShape(20.dp)).shimmer())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f).height(115.dp).clip(RoundedCornerShape(20.dp)).shimmer())
                    Box(modifier = Modifier.weight(1f).height(115.dp).clip(RoundedCornerShape(20.dp)).shimmer())
                }
            }
        }

        // Today Overview Skeleton
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmer()
            )
        }
    }
}

@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerAnimation"
    )
    val shimmerColors = listOf(
        Color(0xFF1E1E24),
        Color(0xFF2C2C35),
        Color(0xFF1E1E24)
    )
    return this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        )
    )
}

@Composable
fun DashboardStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        BorderColor,
                        iconColor.copy(alpha = 0.25f),
                        BorderColor
                    )
                ),
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(iconColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InsightCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun DashboardError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorColorLight,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "حدث خطأ أثناء تحميل البيانات",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("إعادة المحاولة")
            }
        }
    }
}

sealed class TimelineItem {
    abstract val timestamp: Long
    data class ClassItem(val group: com.example.data.model.Group, val isCurrent: Boolean, override val timestamp: Long) : TimelineItem()
    data class OperationItem(val log: com.example.data.model.RemoteActivityLog, override val timestamp: Long) : TimelineItem()
}

@Composable
fun ActivityStatBox(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = title, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
fun TimelineItemRow(item: TimelineItem, isLast: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
    ) {
        // Left timeline line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(30.dp)
        ) {
            val iconColor = when (item) {
                is TimelineItem.ClassItem -> if (item.isCurrent) SuccessColor else PrimaryIndigo
                is TimelineItem.OperationItem -> TextTertiary
            }
            val icon = when (item) {
                is TimelineItem.ClassItem -> if (item.isCurrent) Icons.Default.PlayCircle else Icons.Default.Schedule
                is TimelineItem.OperationItem -> Icons.Default.History
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
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
        
        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp, start = 8.dp)
        ) {
            when (item) {
                is TimelineItem.ClassItem -> {
                    Text(
                        text = if (item.isCurrent) "حصة حالية: ${item.group.name}" else "حصة قادمة: ${item.group.name}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "القاعة: ${item.group.classroom} | الموعد: ${item.group.schedule}", color = TextSecondary, fontSize = 11.sp)
                }
                is TimelineItem.OperationItem -> {
                    Text(text = item.log.action, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(text = item.log.details, color = TextSecondary, fontSize = 11.sp)
                    val format = remember { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US) }
                    Text(text = format.format(java.util.Date(item.timestamp)), color = TextTertiary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun FinancialFlowChart(
    revenueLast6Months: List<RevenueMonthData>,
    currency: String
) {
    if (revenueLast6Months.isEmpty()) return

    var selectedIndex by remember { mutableStateOf(-1) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("financial_flow_chart_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مخطط التدفقات المالية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "مقارنة الدخل بالمصروفات لآخر 6 أشهر (اضغط للتفاصيل)",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                
                // Legends
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SuccessColorLight)
                        )
                        Text("الدخل", fontSize = 11.sp, color = TextSecondary)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ErrorColorLight)
                        )
                        Text("المصروفات", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            // Interactive Tooltip/Detail Area
            val activeMonth = if (selectedIndex in revenueLast6Months.indices) revenueLast6Months[selectedIndex] else null
            AnimatedVisibility(
                visible = activeMonth != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (activeMonth != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BorderColor.copy(alpha = 0.3f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "شهر: ${activeMonth.monthName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "الدخل: ${String.format(Locale.US, "%,.0f", activeMonth.revenue)} $currency",
                                fontSize = 11.sp,
                                color = SuccessColorLight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "المصروفات: ${String.format(Locale.US, "%,.0f", activeMonth.expenses)} $currency",
                                fontSize = 11.sp,
                                color = ErrorColorLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Chart area
            val maxVal = remember(revenueLast6Months) {
                val maxRev = revenueLast6Months.maxOfOrNull { it.revenue } ?: 1.0
                val maxExp = revenueLast6Months.maxOfOrNull { it.expenses } ?: 1.0
                maxOf(maxRev, maxExp, 1000.0).toFloat()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(revenueLast6Months) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val blockWidth = size.width / revenueLast6Months.size
                                    val tappedIndex = (offset.x / blockWidth).toInt()
                                    selectedIndex = if (selectedIndex == tappedIndex) -1 else tappedIndex
                                }
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    
                    val paddingBottom = 20f
                    val paddingTop = 20f
                    val chartHeight = height - paddingBottom - paddingTop
                    
                    val numBars = revenueLast6Months.size
                    val blockWidth = width / numBars
                    val barWidth = 20f
                    val spaceBetweenBars = 4f
                    
                    // 1. Draw Grid Lines and Labels
                    val gridLines = 4
                    for (g in 0..gridLines) {
                        val fraction = g.toFloat() / gridLines
                        val y = chartHeight * (1f - fraction) + paddingTop
                        
                        // Draw dotted/dashed line
                        drawLine(
                            color = BorderColor.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                    
                    // 2. Draw Bars
                    for (i in 0 until numBars) {
                        val monthData = revenueLast6Months[i]
                        val rev = monthData.revenue.toFloat()
                        val exp = monthData.expenses.toFloat()
                        
                        val revHeight = if (maxVal > 0) (rev / maxVal) * chartHeight else 0f
                        val expHeight = if (maxVal > 0) (exp / maxVal) * chartHeight else 0f
                        
                        val blockCenterX = i * blockWidth + (blockWidth / 2)
                        
                        // Draw Selection highlight background
                        if (selectedIndex == i) {
                            drawRoundRect(
                                color = BorderColor.copy(alpha = 0.15f),
                                topLeft = Offset(i * blockWidth, paddingTop),
                                size = Size(blockWidth, chartHeight),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }
                        
                        // Draw Revenue Bar (Green)
                        if (revHeight > 0) {
                            drawRoundRect(
                                color = SuccessColorLight,
                                topLeft = Offset(blockCenterX - barWidth - spaceBetweenBars, chartHeight - revHeight + paddingTop),
                                size = Size(barWidth, revHeight),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                        
                        // Draw Expense Bar (Red)
                        if (expHeight > 0) {
                            drawRoundRect(
                                color = ErrorColorLight,
                                topLeft = Offset(blockCenterX + spaceBetweenBars, chartHeight - expHeight + paddingTop),
                                size = Size(barWidth, expHeight),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                    
                    // Bottom baseline
                    drawLine(
                        color = BorderColor,
                        start = Offset(0f, chartHeight + paddingTop),
                        end = Offset(width, chartHeight + paddingTop),
                        strokeWidth = 2f
                    )
                }
            }
            
            // Month Names Labels under the chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                revenueLast6Months.forEachIndexed { index, monthData ->
                    val isSelected = selectedIndex == index
                    Text(
                        text = monthData.monthName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) PrimaryIndigoLight else TextSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIndex = if (selectedIndex == index) -1 else index },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
