package com.example.ui.finance

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PaymentHistory
import com.example.data.model.Student
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    viewModel: PaymentsViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null,
    isEmbedded: Boolean = false
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val paymentHistory by viewModel.paymentHistory.collectAsStateWithLifecycle()
    val studentTeacherCrossRefs by viewModel.studentTeacherCrossRefs.collectAsStateWithLifecycle()

    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()

    val currency = profile?.currency ?: "ج.م"

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "PAID", "PARTIAL", "UNPAID", "EXEMPT"
    var activeTab by remember { mutableIntStateOf(0) } // 0: Students & Receivables, 1: Payment History

    // Dialog flags
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedStudentForPayment by remember { mutableStateOf<Student?>(null) }
    var paymentToEdit by remember { mutableStateOf<PaymentHistory?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentHistory?>(null) }

    val monthsList = listOf(
        "01" to "يناير", "02" to "فبراير", "03" to "مارس", "04" to "أبريل",
        "05" to "مايو", "06" to "يونيو", "07" to "يوليو", "08" to "أغسطس",
        "09" to "سبتمبر", "10" to "أكتوبر", "11" to "نوفمبر", "12" to "ديسمبر"
    )

    val yearsList = listOf("2024", "2025", "2026", "2027", "2028")

    Scaffold(
        containerColor = if (isEmbedded) Color.Transparent else BackgroundDark,
        floatingActionButton = {
            if (activeTab == 0 && !isEmbedded) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PrimaryIndigo,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "تسجيل دفعة")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top App Bar
            if (!isEmbedded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                            }
                        }
                        Text(
                            text = "نظام المحاسبة والرسوم",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Month/Year Selectors (Compact Chips)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var showMonthMenu by remember { mutableStateOf(false) }
                        var showYearMenu by remember { mutableStateOf(false) }

                        // Month Chip
                        Box {
                            AssistChip(
                                onClick = { showMonthMenu = true },
                                label = { Text(monthsList.find { it.first == selectedMonth }?.second ?: selectedMonth) },
                                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = PrimaryIndigoLight,
                                    leadingIconContentColor = PrimaryIndigoLight,
                                    containerColor = SurfaceDark
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                            )
                            DropdownMenu(
                                expanded = showMonthMenu,
                                onDismissRequest = { showMonthMenu = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                monthsList.forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name, color = TextPrimary) },
                                        onClick = {
                                            viewModel.setMonth(code)
                                            showMonthMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year Chip
                        Box {
                            AssistChip(
                                onClick = { showYearMenu = true },
                                label = { Text(selectedYear) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = PrimaryIndigoLight,
                                    containerColor = SurfaceDark
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                            )
                            DropdownMenu(
                                expanded = showYearMenu,
                                onDismissRequest = { showYearMenu = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                yearsList.forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text(yr, color = TextPrimary) },
                                        onClick = {
                                            viewModel.setYear(yr)
                                            showYearMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Embedded Compact Header (Only show selectors, aligned to start/end as convenient)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showMonthMenu by remember { mutableStateOf(false) }
                    var showYearMenu by remember { mutableStateOf(false) }

                    // Month Chip
                    Box {
                        AssistChip(
                            onClick = { showMonthMenu = true },
                            label = { Text(monthsList.find { it.first == selectedMonth }?.second ?: selectedMonth) },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = PrimaryIndigoLight,
                                leadingIconContentColor = PrimaryIndigoLight,
                                containerColor = SurfaceDark
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        )
                        DropdownMenu(
                            expanded = showMonthMenu,
                            onDismissRequest = { showMonthMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            monthsList.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name, color = TextPrimary) },
                                    onClick = {
                                        viewModel.setMonth(code)
                                        showMonthMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Year Chip
                    Box {
                        AssistChip(
                            onClick = { showYearMenu = true },
                            label = { Text(selectedYear) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = PrimaryIndigoLight,
                                containerColor = SurfaceDark
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        )
                        DropdownMenu(
                            expanded = showYearMenu,
                            onDismissRequest = { showYearMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            yearsList.forEach { yr ->
                                DropdownMenuItem(
                                    text = { Text(yr, color = TextPrimary) },
                                    onClick = {
                                        viewModel.setYear(yr)
                                        showYearMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Tab Rows
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = SurfaceDark,
                contentColor = PrimaryIndigo,
                divider = { HorizontalDivider(color = BorderColor) }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("المستحقات والطلاب", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    selectedContentColor = PrimaryIndigoLight,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("سجل الدفعات السابقة", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    selectedContentColor = PrimaryIndigoLight,
                    unselectedContentColor = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Views
            when (activeTab) {
                0 -> {
                    // Search and Filter Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث باسم الطالب...", fontSize = 13.sp, color = TextTertiary) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = PrimaryIndigoLight,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Status Filter Bottom Sheet
                        var showFilterSheet by remember { mutableStateOf(false) }
                        
                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "تصفية", tint = TextPrimary)
                        }
                        
                        if (showFilterSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showFilterSheet = false },
                                containerColor = SurfaceDark
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("تصفية المدفوعات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    
                                    // Time period filter
                                    Text("الفترة الزمنية", fontSize = 13.sp, color = TextSecondary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val timeFilters = listOf("يومي", "أسبوعي", "شهري", "سنوي")
                                        timeFilters.forEach { filter ->
                                            val isSelected = false // Visual only for now, logic relies on month/year selectors
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else SurfaceLightDark)
                                                    .border(1.dp, if (isSelected) PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    .clickable { /* TBD if logic requires */ }
                                            ) {
                                                Text(filter, fontSize = 12.sp, color = if (isSelected) PrimaryIndigoLight else TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }

                                    // Status filter
                                    Text("حالة الدفع", fontSize = 13.sp, color = TextSecondary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val filters = listOf(
                                            "ALL" to "الكل",
                                            "PAID" to "مسدد بالكامل",
                                            "PARTIAL" to "مسدد جزئياً",
                                            "UNPAID" to "غير مسدد",
                                            "EXEMPT" to "معفي"
                                        )
                                        filters.forEach { (key, label) ->
                                            val isSelected = statusFilter == key
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else SurfaceLightDark)
                                                    .border(1.dp, if (isSelected) PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
                                                    .clickable { statusFilter = key }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(label, fontSize = 12.sp, color = if (isSelected) PrimaryIndigoLight else TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Compute list of students with stats
                    val activeStudents = students.filter { it.isActive && !it.isArchived }
                    val filteredStudents = activeStudents.filter { student ->
                        val matchesSearch = student.name.contains(searchQuery, ignoreCase = true)
                        
                        // Compute accounting metrics
                        val netFee = student.getActualNetFee(studentTeacherCrossRefs)
                        val paymentsForMonth = paymentHistory.filter { 
                            it.studentId == student.id && it.month == selectedMonth && it.year == selectedYear
                        }
                        val paidAmount = paymentsForMonth.sumOf { it.amount }

                        val calculatedStatus = when {
                            student.isExempt -> "EXEMPT"
                            paidAmount >= netFee -> "PAID"
                            paidAmount > 0.0 -> "PARTIAL"
                            else -> "UNPAID"
                        }

                        val matchesFilter = statusFilter == "ALL" || statusFilter == calculatedStatus

                        matchesSearch && matchesFilter
                    }

                    if (filteredStudents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا توجد بيانات مطابقة.", color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredStudents, key = { it.id }) { student ->
                                val teacherName = teachers.find { it.id == student.teacherId }?.name ?: "غير محدد"
                                
                                // Financial Computations
                                val studentCrossRefs = studentTeacherCrossRefs.filter { it.studentId == student.id }
                                val baseFee = if (studentCrossRefs.isNotEmpty()) {
                                    studentCrossRefs.sumOf { it.customFee }
                                } else {
                                    student.monthlyFee
                                }
                                val discountPercent = student.discount
                                val discountValue = baseFee * (discountPercent / 100.0)
                                val netFee = student.getActualNetFee(studentTeacherCrossRefs)
                                val studentPayments = paymentHistory.filter { 
                                    it.studentId == student.id && it.month == selectedMonth && it.year == selectedYear
                                }
                                val paidAmount = studentPayments.sumOf { it.amount }
                                val remainingAmount = maxOf(0.0, netFee - paidAmount)

                                val currentStatus = when {
                                    student.isExempt -> "EXEMPT"
                                    paidAmount >= netFee -> "PAID"
                                    paidAmount > 0.0 -> "PARTIAL"
                                    else -> "UNPAID"
                                }

                                StudentAccountingCard(
                                    student = student,
                                    teacherName = teacherName,
                                    baseFee = baseFee,
                                    discountValue = discountValue,
                                    netFee = netFee,
                                    paidAmount = paidAmount,
                                    remainingAmount = remainingAmount,
                                    status = currentStatus,
                                    currency = currency,
                                    onAddPaymentClick = {
                                        selectedStudentForPayment = student
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // List of previous payment history records
                    val filteredPayments = paymentHistory.filter {
                        it.month == selectedMonth && it.year == selectedYear
                    }

                    if (filteredPayments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا توجد دفعات مسجلة لهذا الشهر.", color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(filteredPayments, key = { it.id }) { payment ->
                                val student = students.find { it.id == payment.studentId }
                                val sName = student?.name ?: "طالب محذوف"
                                val tName = teachers.find { it.id == student?.teacherId }?.name ?: "غير محدد"

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("المعلم: $tName", fontSize = 12.sp, color = TextSecondary)
                                            if (payment.notes.isNotEmpty()) {
                                                Text("ملاحظات: ${payment.notes}", fontSize = 11.sp, color = TextTertiary)
                                            }
                                            Text(
                                                text = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date(payment.paymentDate)),
                                                fontSize = 11.sp,
                                                color = TextTertiary
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("+${payment.amount} $currency", color = SuccessColorLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(PrimaryIndigo.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(payment.paymentMethod, fontSize = 10.sp, color = PrimaryIndigoLight)
                                                }
                                            }

                                            IconButton(onClick = { paymentToEdit = payment }) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PrimaryIndigoLight, modifier = Modifier.size(18.dp))
                                            }

                                            IconButton(onClick = { paymentToDelete = payment }) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColorLight, modifier = Modifier.size(18.dp))
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
    }

    // Add Payment Dialog (Generic / Floating FAB)
    if (showAddDialog) {
        var selectedStudent by remember { mutableStateOf<Student?>(null) }
        var amountStr by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("كاش") }
        var notes by remember { mutableStateOf("") }
        var showStudentDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("تسجيل دفعة جديدة", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Student Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedStudent?.name ?: "اختر الطالب...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الطالب") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStudentDropdown = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryIndigoLight,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        DropdownMenu(
                            expanded = showStudentDropdown,
                            onDismissRequest = { showStudentDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                        ) {
                            students.filter { it.isActive }.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name, color = TextPrimary) },
                                    onClick = {
                                        selectedStudent = s
                                        // Auto fill amount with netFee
                                        val calculatedPaid = paymentHistory.filter { 
                                            it.studentId == s.id && it.month == selectedMonth && it.year == selectedYear
                                        }.sumOf { it.amount }
                                        val net = s.getActualNetFee(studentTeacherCrossRefs)
                                        val remaining = maxOf(0.0, net - calculatedPaid)
                                        amountStr = remaining.toString()
                                        showStudentDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("المبلغ المستلم") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    // Method Dropdown
                    var showMethodDropdown by remember { mutableStateOf(false) }
                    val methods = listOf("كاش", "فودافون كاش", "فيزا", "تحويل بنكي", "آخر")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = method,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("طريقة الدفع") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMethodDropdown = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryIndigoLight,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        DropdownMenu(
                            expanded = showMethodDropdown,
                            onDismissRequest = { showMethodDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                        ) {
                            methods.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m, color = TextPrimary) },
                                    onClick = {
                                        method = m
                                        showMethodDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val student = selectedStudent
                        val amount = amountStr.toDoubleOrNull()
                        if (student != null && amount != null && amount > 0.0) {
                            viewModel.addPayment(
                                studentId = student.id,
                                amount = amount,
                                method = method,
                                notes = notes,
                                month = selectedMonth,
                                year = selectedYear,
                                date = System.currentTimeMillis()
                            )
                            showAddDialog = false
                            Toast.makeText(context, "تم تسجيل الدفعة تلقائياً", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى تعبئة كافة الحقول بشكل صحيح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("تسجيل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Add Payment Dialog (Directly clicked from Student Card)
    if (selectedStudentForPayment != null) {
        val student = selectedStudentForPayment!!
        
        // Compute remaining amount
        val studentCrossRefs = studentTeacherCrossRefs.filter { it.studentId == student.id }
        val baseFee = if (studentCrossRefs.isNotEmpty()) {
            studentCrossRefs.sumOf { it.customFee }
        } else {
            student.monthlyFee
        }
        val discountPercent = student.discount
        val discountValue = baseFee * (discountPercent / 100.0)
        val netFee = student.getActualNetFee(studentTeacherCrossRefs)
        val studentPayments = paymentHistory.filter { 
            it.studentId == student.id && it.month == selectedMonth && it.year == selectedYear
        }
        val paidAmount = studentPayments.sumOf { it.amount }
        val remainingAmount = maxOf(0.0, netFee - paidAmount)

        var amountStr by remember { mutableStateOf(remainingAmount.toString()) }
        var method by remember { mutableStateOf("كاش") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedStudentForPayment = null },
            title = { Text("تسجيل دفعة لـ ${student.name}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("المستحق المتبقي: $remainingAmount $currency", fontSize = 13.sp, color = TextSecondary)

                    // Amount
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("المبلغ المستلم") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    // Method Dropdown
                    var showMethodDropdown by remember { mutableStateOf(false) }
                    val methods = listOf("كاش", "فودافون كاش", "فيزا", "تحويل بنكي", "آخر")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = method,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("طريقة الدفع") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMethodDropdown = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryIndigoLight,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        DropdownMenu(
                            expanded = showMethodDropdown,
                            onDismissRequest = { showMethodDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                        ) {
                            methods.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m, color = TextPrimary) },
                                    onClick = {
                                        method = m
                                        showMethodDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull()
                        if (amount != null && amount > 0.0) {
                            viewModel.addPayment(
                                studentId = student.id,
                                amount = amount,
                                method = method,
                                notes = notes,
                                month = selectedMonth,
                                year = selectedYear,
                                date = System.currentTimeMillis()
                            )
                            selectedStudentForPayment = null
                            Toast.makeText(context, "تم تسجيل الدفعة بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("تسجيل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedStudentForPayment = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Edit Payment Dialog
    if (paymentToEdit != null) {
        val payment = paymentToEdit!!
        val student = students.find { it.id == payment.studentId }
        val sName = student?.name ?: "طالب"

        var amountStr by remember { mutableStateOf(payment.amount.toString()) }
        var method by remember { mutableStateOf(payment.paymentMethod) }
        var notes by remember { mutableStateOf(payment.notes) }

        AlertDialog(
            onDismissRequest = { paymentToEdit = null },
            title = { Text("تعديل الدفعة لـ $sName", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Amount
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("المبلغ المستلم") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    // Method Dropdown
                    var showMethodDropdown by remember { mutableStateOf(false) }
                    val methods = listOf("كاش", "فودافون كاش", "فيزا", "تحويل بنكي", "آخر")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = method,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("طريقة الدفع") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMethodDropdown = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryIndigoLight,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        DropdownMenu(
                            expanded = showMethodDropdown,
                            onDismissRequest = { showMethodDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                        ) {
                            methods.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m, color = TextPrimary) },
                                    onClick = {
                                        method = m
                                        showMethodDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull()
                        if (amount != null && amount > 0.0) {
                            val updated = payment.copy(
                                amount = amount,
                                paymentMethod = method,
                                notes = notes
                            )
                            viewModel.updatePayment(updated)
                            paymentToEdit = null
                            Toast.makeText(context, "تم تعديل الدفعة بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("حفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToEdit = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Delete Payment Confirmation Dialog
    if (paymentToDelete != null) {
        val payment = paymentToDelete!!
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("حذف الدفعة", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذه الدفعة المحددة بقيمة ${payment.amount} $currency؟ لا يمكن التراجع عن هذا الإجراء.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(payment.id)
                        paymentToDelete = null
                        Toast.makeText(context, "تم حذف الدفعة بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("تأكيد الحذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun StudentAccountingCard(
    student: Student,
    teacherName: String,
    baseFee: Double,
    discountValue: Double,
    netFee: Double,
    paidAmount: Double,
    remainingAmount: Double,
    status: String, // "PAID", "PARTIAL", "UNPAID", "EXEMPT"
    currency: String,
    onAddPaymentClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Name & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "المعلم: $teacherName",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                StatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Accounting Details Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLightDark, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AccountingMetric(label = "الأساسي", value = baseFee, currency = currency)
                AccountingMetric(label = "الخصم", value = discountValue, currency = currency, isAccent = discountValue > 0)
                AccountingMetric(label = "المستحق", value = netFee, currency = currency)
                AccountingMetric(label = "المدفوع", value = paidAmount, currency = currency, color = SuccessColorLight)
                AccountingMetric(label = "المتبقي", value = remainingAmount, currency = currency, color = if (remainingAmount > 0) ErrorColorLight else TextPrimary)
            }

            // CTA Button to register payment
            if (status != "PAID" && status != "EXEMPT") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddPaymentClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo.copy(alpha = 0.15f),
                        contentColor = PrimaryIndigoLight
                    )
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل سداد نقدي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AccountingMetric(
    label: String,
    value: Double,
    currency: String,
    color: Color = TextPrimary,
    isAccent: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        val formatted = NumberFormat.getNumberInstance(Locale.US).format(value.toInt())
        Text(
            text = "$formatted $currency",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAccent) WarningColor else color
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, bg, textCol) = when (status) {
        "PAID" -> Triple("مسدد بالكامل", SuccessColor.copy(alpha = 0.12f), SuccessColorLight)
        "PARTIAL" -> Triple("مسدد جزئياً", WarningColor.copy(alpha = 0.12f), WarningColor)
        "EXEMPT" -> Triple("معفي", PrimaryIndigo.copy(alpha = 0.12f), PrimaryIndigoLight)
        else -> Triple("غير مسدد", ErrorColor.copy(alpha = 0.12f), ErrorColorLight)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textCol
        )
    }
}
