package com.example.ui.finance

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: AppViewModel,
    onBackClick: (() -> Unit)? = null,
    isEmbedded: Boolean = false
) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currency = profile?.currency ?: "ج.م"

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }

    // Dialog state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val categories = listOf("إيجار", "مرتبات", "كهرباء", "إنترنت", "أدوات", "أخرى")
    val filterCategories = listOf("الكل") + categories

    val filteredExpenses = remember(expenses, searchQuery, selectedCategoryFilter) {
        expenses.filter { expense ->
            val matchesSearch = expense.title.contains(searchQuery, ignoreCase = true) ||
                    expense.notes.contains(searchQuery, ignoreCase = true) ||
                    expense.description.contains(searchQuery, ignoreCase = true)
            
            val matchesCategory = selectedCategoryFilter == "الكل" || expense.category == selectedCategoryFilter
            matchesSearch && matchesCategory
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (isEmbedded) Color.Transparent else BackgroundDark,
        topBar = {
            if (!isEmbedded) {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        Text(
                            text = "إدارة المصروفات 💸",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick, modifier = Modifier.testTag("expenses_back_btn")) {
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
                        scrolledContainerColor = SurfaceDark,
                        titleContentColor = TextPrimary
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isEmbedded) {
                FloatingActionButton(
                    onClick = {
                        expenseToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = PrimaryIndigo,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_expense_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة مصروف")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = if (isEmbedded) 0.dp else 20.dp)
        ) {
            // Summary Cards row
            if (!isEmbedded) {
                ExpensesSummaryRow(expenses = filteredExpenses, currency = currency)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search and Filter Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("البحث في المصروفات...", fontSize = 13.sp, color = TextTertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("expense_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

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
                            Text("تصفية المصروفات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                            // Time period filter
                            Text("الفترة الزمنية", fontSize = 13.sp, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val timeFilters = listOf("يومي", "أسبوعي", "شهري", "سنوي")
                                timeFilters.forEach { filter ->
                                    val isSelected = false
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

                            // Category filter
                            Text("التصنيف", fontSize = 13.sp, color = TextSecondary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filterCategories.forEach { category ->
                                    val isSelected = selectedCategoryFilter == category
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else SurfaceLightDark)
                                            .border(1.dp, if (isSelected) PrimaryIndigoLight else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedCategoryFilter = category }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = category,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PrimaryIndigoLight else TextPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expenses List
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد مصروفات مسجلة تطابق التصفية",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredExpenses) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            currency = currency,
                            onEdit = {
                                expenseToEdit = expense
                                showAddEditDialog = true
                            },
                            onDelete = {
                                expenseToDelete = expense
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditExpenseDialog(
            expense = expenseToEdit,
            categories = categories,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { title, amount, category, date, notes ->
                if (expenseToEdit == null) {
                    viewModel.addExpense(
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        notes = notes
                    )
                    Toast.makeText(context, "تم تسجيل المصروف بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    val updated = expenseToEdit!!.copy(
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        notes = notes,
                        description = title,
                        month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(date))
                    )
                    viewModel.updateExpense(updated)
                    Toast.makeText(context, "تم تعديل المصروف بنجاح", Toast.LENGTH_SHORT).show()
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("تأكيد الحذف ⚠️", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا المصروف النهائي؟ لا يمكن التراجع عن هذا الإجراء.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expenseToDelete!!.id)
                        Toast.makeText(context, "تم حذف المصروف", Toast.LENGTH_SHORT).show()
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColorLight)
                ) {
                    Text("حذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun ExpensesSummaryRow(expenses: List<Expense>, currency: String) {
    val totalAmount = expenses.sumOf { it.amount }
    val count = expenses.size

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("إجمالي المصروفات", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%,.1f %s", totalAmount, currency),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorColorLight
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("عدد العمليات", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$count عملية",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigoLight
                )
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryIcon = when (expense.category) {
        "إيجار" -> Icons.Default.Home
        "مرتبات" -> Icons.Default.People
        "كهرباء" -> Icons.Default.ElectricBolt
        "إنترنت" -> Icons.Default.Wifi
        "أدوات" -> Icons.Default.Build
        else -> Icons.Default.Receipt
    }

    val iconContainerColor = when (expense.category) {
        "إيجار" -> Color(0xFF2196F3).copy(alpha = 0.12f)
        "مرتبات" -> Color(0xFF4CAF50).copy(alpha = 0.12f)
        "كهرباء" -> Color(0xFFFFC107).copy(alpha = 0.12f)
        "إنترنت" -> Color(0xFF9C27B0).copy(alpha = 0.12f)
        "أدوات" -> Color(0xFFE91E63).copy(alpha = 0.12f)
        else -> TextTertiary.copy(alpha = 0.12f)
    }

    val iconTintColor = when (expense.category) {
        "إيجار" -> Color(0xFF2196F3)
        "مرتبات" -> Color(0xFF4CAF50)
        "كهرباء" -> Color(0xFFFFC107)
        "إنترنت" -> Color(0xFF9C27B0)
        "أدوات" -> Color(0xFFE91E63)
        else -> TextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = iconTintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (expense.title.isNotEmpty()) expense.title else expense.category,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = expense.category,
                            fontSize = 11.sp,
                            color = iconTintColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("•", color = TextTertiary, fontSize = 11.sp)
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(expense.date))
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                    if (expense.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = expense.notes,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.US, "-%,.0f %s", expense.amount, currency),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorColorLight
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PrimaryIndigoLight, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColorLight, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    expense: Expense?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: String, date: Long, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(expense?.title ?: "") }
    var amountText by remember { mutableStateOf(expense?.amount?.let { if (it % 1 == 0.0) String.format("%.0f", it) else it.toString() } ?: "") }
    var selectedCategory by remember { mutableStateOf(expense?.category ?: categories.first()) }
    var date by remember { mutableStateOf(expense?.date ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (expense == null) "إضافة مصروف جديد 💸" else "تعديل المصروف 📝",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم / بيان المصروف") },
                    placeholder = { Text("مثال: فاتورة الإنترنت لشهر 6") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_title_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("القيمة المالية") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_field"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Category Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("التصنيف / الفئة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("expense_category_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = BackgroundDark,
                                unfocusedContainerColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category, color = TextPrimary) },
                                    onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Date Picker Simple Representation
                var showDatePicker by remember { mutableStateOf(false) }
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("تاريخ المصروف") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "اختر تاريخ", tint = PrimaryIndigoLight)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("expense_date_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    date = it
                                }
                                showDatePicker = false
                            }) {
                                Text("موافق", color = PrimaryIndigoLight)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("إلغاء", color = TextSecondary)
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    placeholder = { Text("اكتب أي تفاصيل أخرى هنا...") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_notes_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (title.trim().isEmpty()) {
                        title = selectedCategory
                    }
                    if (amount == null || amount <= 0) {
                        // validation failure
                    } else {
                        onConfirm(title, amount, selectedCategory, date, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("expense_submit_btn")
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold, color = Color.White)
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
