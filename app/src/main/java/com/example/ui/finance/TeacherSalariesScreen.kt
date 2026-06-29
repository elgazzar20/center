package com.example.ui.finance

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSalariesScreen(
    viewModel: AppViewModel,
    onBackClick: (() -> Unit)? = null,
    isEmbedded: Boolean = false
) {
    val teachers by viewModel.teachers.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currency = profile?.currency ?: "ج.م"
    val context = LocalContext.current

    var selectedMonth by remember { mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.US).format(Date())) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    val recentMonths = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        for (i in 0..5) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            list.add(sdf.format(c.time))
        }
        list
    }

    val content = @Composable {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isEmbedded) 0.dp else 24.dp)
        ) {
            if (!isEmbedded) {
                Text(
                    text = "رواتب المعلمين",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Month Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { monthDropdownExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("الشهر: $selectedMonth", color = TextPrimary)
                }
                DropdownMenu(
                    expanded = monthDropdownExpanded,
                    onDismissRequest = { monthDropdownExpanded = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    recentMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month, color = TextPrimary) },
                            onClick = {
                                selectedMonth = month
                                monthDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (teachers.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("لا يوجد معلمون مسجلون.", color = TextTertiary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(teachers) { teacher ->
                        val teacherPaymentsThisMonth = payments.filter { it.teacherId == teacher.id && it.month == selectedMonth }
                        val totalCollected = teacherPaymentsThisMonth.sumOf { it.amount }
                        
                        val computedSalary = when (teacher.salaryType) {
                            "percentage" -> totalCollected * (teacher.salaryValue / 100.0)
                            "fixed" -> teacher.salaryValue
                            else -> 0.0
                        }

                        // Check if already paid this month
                        val descriptionStr = "راتب المعلم: ${teacher.name} - شهر $selectedMonth"
                        val isPaid = expenses.any { it.category == "مرتبات" && it.description.contains(teacher.name) && it.month == selectedMonth }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigoLight)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(teacher.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(teacher.subject, fontSize = 12.sp, color = TextSecondary)
                                        }
                                    }
                                    if (isPaid) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تم الدفع", color = SuccessColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = BorderColor)
                                Spacer(modifier = Modifier.height(12.dp))

                                val salaryTypeArabic = when (teacher.salaryType) {
                                    "fixed" -> "راتب ثابت"
                                    "percentage" -> "نسبة مئوية (${teacher.salaryValue}%)"
                                    else -> "بدون"
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("نظام الحساب: $salaryTypeArabic", fontSize = 12.sp, color = TextSecondary)
                                        if (teacher.salaryType == "percentage") {
                                            Text("متحصلات الطلاب: $totalCollected $currency", fontSize = 12.sp, color = TextSecondary)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("المستحق", fontSize = 12.sp, color = TextSecondary)
                                        Text("$computedSalary $currency", fontSize = 16.sp, color = PrimaryIndigoLight, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (!isPaid && computedSalary > 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            viewModel.addExpense(
                                                category = "مرتبات",
                                                amount = computedSalary,
                                                description = descriptionStr,
                                                isMonthly = true,
                                                month = selectedMonth
                                            )
                                            Toast.makeText(context, "تم تسجيل دفع الراتب بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تسجيل الدفع")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isEmbedded) {
        content()
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("رواتب المعلمين", color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { onBackClick?.invoke() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
                )
            },
            containerColor = BackgroundDark
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                content()
            }
        }
    }
}
