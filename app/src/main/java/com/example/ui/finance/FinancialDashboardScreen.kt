package com.example.ui.finance

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.Payment
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    viewModel: AppViewModel,
    onBackClick: (() -> Unit)? = null,
    isEmbedded: Boolean = false
) {
    val payments by viewModel.payments.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currency = profile?.currency ?: "ج.م"
    val context = LocalContext.current

    val currentMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val currentDayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    // Computations
    val currentDayRevenues = remember(payments, currentDayStr) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        payments.filter { sdf.format(Date(it.date)) == currentDayStr }.sumOf { it.amount }
    }
    
    val currentMonthRevenues = remember(payments, currentMonthStr) {
        payments.filter { it.month == currentMonthStr }.sumOf { it.amount }
    }
    
    val currentMonthExpenses = remember(expenses, currentMonthStr) {
        expenses.filter { it.month == currentMonthStr }.sumOf { it.amount }
    }
    
    val currentMonthProfit = currentMonthRevenues - currentMonthExpenses

    // Monthly data for the last 6 months (timeline)
    val last6Months = remember {
        val cal = Calendar.getInstance()
        val list = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        for (i in 5 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            list.add(sdf.format(c.time))
        }
        list
    }

    val monthlyRevenuesList = remember(payments, last6Months) {
        last6Months.map { m ->
            payments.filter { it.month == m }.sumOf { it.amount }
        }
    }

    val monthlyExpensesList = remember(expenses, last6Months) {
        last6Months.map { m ->
            expenses.filter { m == it.month }.sumOf { it.amount }
        }
    }

    val monthlyProfitsList = remember(monthlyRevenuesList, monthlyExpensesList) {
        monthlyRevenuesList.zip(monthlyExpensesList) { r, e -> r - e }
    }

    // Expense breakdown by category
    val categories = listOf("إيجار", "مرتبات", "كهرباء", "إنترنت", "أدوات", "أخرى")
    val categoryExpenses = remember(expenses, currentMonthStr) {
        categories.map { cat ->
            cat to expenses.filter { it.category == cat && it.month == currentMonthStr }.sumOf { it.amount }
        }.filter { it.second > 0 }
    }

    Scaffold(
        topBar = {
            if (!isEmbedded) {
                TopAppBar(
                    title = {
                        Text(
                            text = "لوحة التحكم المالية",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick, modifier = Modifier.testTag("financial_back_btn")) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "العودة للخلف",
                                    tint = TextPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BackgroundDark,
                        titleContentColor = TextPrimary
                    )
                )
            }
        },
        containerColor = if (isEmbedded) Color.Transparent else BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = if (isEmbedded) 0.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = if (isEmbedded) 0.dp else 16.dp)
        ) {
            // MAIN DASHBOARD STATS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نظرة عامة (الشهر الحالي)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { Toast.makeText(context, "جاري التصدير إلى PDF...", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryIndigo.copy(alpha = 0.1f))
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = PrimaryIndigoLight, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { Toast.makeText(context, "جاري التصدير إلى Excel...", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessColor.copy(alpha = 0.1f))
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = "Excel", tint = SuccessColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialMetricCard(
                        title = "إيرادات اليوم",
                        value = currentDayRevenues,
                        currency = currency,
                        color = PrimaryIndigoLight,
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialMetricCard(
                        title = "إيرادات الشهر",
                        value = currentMonthRevenues,
                        currency = currency,
                        color = SuccessColor,
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialMetricCard(
                        title = "المصروفات",
                        value = currentMonthExpenses,
                        currency = currency,
                        color = ErrorColorLight,
                        icon = Icons.Default.ReceiptLong,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialMetricCard(
                        title = "الأرباح",
                        value = currentMonthProfit,
                        currency = currency,
                        color = if (currentMonthProfit >= 0) SuccessColorLight else ErrorColorLight,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // EXPENSE MANAGEMENT
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "تصنيف المصروفات (الشهر الحالي)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (categoryExpenses.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد مصروفات مسجلة هذا الشهر.", color = TextTertiary, fontSize = 12.sp)
                            }
                        } else {
                            categoryExpenses.forEachIndexed { index, (cat, amt) ->
                                val percentage = if (currentMonthExpenses > 0) (amt / currentMonthExpenses * 100).toInt() else 0
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (cat) {
                                        "إيجار" -> Icons.Default.Home
                                        "مرتبات" -> Icons.Default.Group
                                        "كهرباء" -> Icons.Default.Bolt
                                        "إنترنت" -> Icons.Default.Wifi
                                        "أدوات" -> Icons.Default.Build
                                        else -> Icons.Default.Category
                                    }
                                    
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceLightDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { percentage / 100f },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = PrimaryIndigo,
                                            trackColor = SurfaceLightDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%,.0f %s", amt, currency),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text("$percentage%", fontSize = 11.sp, color = TextTertiary)
                                    }
                                }
                                if (index < categoryExpenses.size - 1) {
                                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // CHARTS
            item {
                Text(
                    text = "التحليلات البيانية",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "الإيرادات والمصروفات (آخر 6 أشهر)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        RevenuesExpensesBarChart(
                            months = last6Months.map { m ->
                                val parts = m.split("-")
                                if (parts.size == 2) {
                                    val monthNum = parts[1]
                                    val arabicMonths = mapOf(
                                        "01" to "يناير", "02" to "فبراير", "03" to "مارس", "04" to "أبريل",
                                        "05" to "مايو", "06" to "يونيو", "07" to "يوليو", "08" to "أغسطس",
                                        "09" to "سبتمبر", "10" to "أكتوبر", "11" to "نوفمبر", "12" to "ديسمبر"
                                    )
                                    arabicMonths[monthNum] ?: m
                                } else m
                            },
                            revenues = monthlyRevenuesList,
                            expenses = monthlyExpensesList
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(SuccessColor, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الإيرادات", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.size(10.dp).background(ErrorColorLight, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المصروفات", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "نمو الأرباح (آخر 6 أشهر)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        NetProfitLineChart(
                            profits = monthlyProfitsList
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun FinancialMetricCard(
    title: String,
    value: Double,
    currency: String,
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = String.format(Locale.US, "%,.0f %s", value, currency),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (value < 0 && color == SuccessColor) ErrorColorLight else color,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun RevenuesExpensesBarChart(
    months: List<String>,
    revenues: List<Double>,
    expenses: List<Double>
) {
    val maxVal = remember(revenues, expenses) {
        val maxRev = revenues.maxOrNull() ?: 1.0
        val maxExp = expenses.maxOrNull() ?: 1.0
        maxOf(maxRev, maxExp, 1.0).toFloat()
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val width = size.width
        val height = size.height

        val paddingBottom = 40f
        val paddingTop = 10f
        val chartHeight = height - paddingBottom - paddingTop

        val numBars = months.size
        val blockWidth = width / numBars
        val barWidth = 18f
        val spaceBetweenBars = 4f

        for (i in 0 until numBars) {
            val rev = revenues[i].toFloat()
            val exp = expenses[i].toFloat()

            val revHeight = if (maxVal > 0) (rev / maxVal) * chartHeight else 0f
            val expHeight = if (maxVal > 0) (exp / maxVal) * chartHeight else 0f

            val blockCenterX = i * blockWidth + (blockWidth / 2)

            // Draw Revenue Bar (Green)
            drawRoundRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(blockCenterX - barWidth - spaceBetweenBars, chartHeight - revHeight + paddingTop),
                size = Size(barWidth, revHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Draw Expense Bar (Red)
            drawRoundRect(
                color = Color(0xFFE57373),
                topLeft = Offset(blockCenterX + spaceBetweenBars, chartHeight - expHeight + paddingTop),
                size = Size(barWidth, expHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Draw Divider line
            drawLine(
                color = Color(0xFF333333),
                start = Offset(0f, chartHeight + paddingTop),
                end = Offset(width, chartHeight + paddingTop),
                strokeWidth = 2f
            )
        }
    }

    // Text labels layout in a clean Compose Row for Month names
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        months.forEach { month ->
            Text(
                text = month,
                fontSize = 10.sp,
                color = TextTertiary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NetProfitLineChart(
    profits: List<Double>
) {
    val maxVal = remember(profits) {
        val absList = profits.map { Math.abs(it) }
        maxOf(absList.maxOrNull() ?: 1.0, 1.0).toFloat()
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val width = size.width
        val height = size.height

        val paddingBottom = 20f
        val paddingTop = 20f
        val chartHeight = height - paddingBottom - paddingTop

        val numPoints = profits.size
        val stepX = width / (numPoints - 1)

        val path = Path()
        val filledPath = Path()

        val points = mutableListOf<Offset>()

        for (i in 0 until numPoints) {
            val profit = profits[i].toFloat()
            // Map profit to screen coords (-maxVal to +maxVal centered or simply 0 to maxVal)
            val percentage = (profit / maxVal).coerceIn(-1f, 1f)
            // Center is height / 2, or standard bottom-up if positive
            val y = (chartHeight / 2) - (percentage * (chartHeight / 2)) + paddingTop + (chartHeight / 4)

            val x = i * stepX
            points.add(Offset(x, y))

            if (i == 0) {
                path.moveTo(x, y)
                filledPath.moveTo(x, height)
                filledPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                filledPath.lineTo(x, y)
            }

            if (i == numPoints - 1) {
                filledPath.lineTo(x, height)
                filledPath.close()
            }
        }

        // Draw background gradient
        drawPath(
            path = filledPath,
            brush = Brush.verticalGradient(
                colors = listOf(PrimaryIndigo.copy(alpha = 0.15f), Color.Transparent)
            )
        )

        // Draw line path
        drawPath(
            path = path,
            color = PrimaryIndigoLight,
            style = Stroke(width = 6f, join = StrokeJoin.Round)
        )

        // Draw points
        points.forEach { pt ->
            drawCircle(
                color = PrimaryIndigoLight,
                radius = 8f,
                center = pt
            )
            drawCircle(
                color = SurfaceDark,
                radius = 4f,
                center = pt
            )
        }
    }
}

@Composable
fun ExpenseDonutChart(
    categoryExpenses: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val total = remember(categoryExpenses) { categoryExpenses.sumOf { it.second }.toFloat() }

    val colors = listOf(
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFFFC107),
        Color(0xFF9C27B0),
        Color(0xFFE91E63),
        Color(0xFF00BCD4)
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val minDim = minOf(width, height)
        val radius = minDim / 2
        val strokeWidth = 32f

        var startAngle = -90f

        categoryExpenses.forEachIndexed { index, (_, amt) ->
            val sweepAngle = (amt.toFloat() / total) * 360f
            val color = colors[index % colors.size]

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(minDim - strokeWidth, minDim - strokeWidth),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            )

            startAngle += sweepAngle
        }
    }
}
