package com.example.ui.reports

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActivityLog
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit,
    isEmbedded: Boolean = false
) {
    val context = LocalContext.current
    val logs by viewModel.activityLogs.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedActionFilter by remember { mutableStateOf("الكل") }
    var isDescending by remember { mutableStateOf(true) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    val actionFilters = listOf(
        "الكل",
        "إضافة طالب",
        "تعديل طالب",
        "حذف طالب",
        "تسجيل حضور",
        "تسجيل غياب",
        "إضافة دفعة",
        "تعديل دفعة",
        "حذف دفعة"
    )

    // Filter and Sort Logs
    val filteredAndSortedLogs = remember(logs, searchQuery, selectedActionFilter, isDescending) {
        var result = logs

        // 1. Filter by Action Category
        if (selectedActionFilter != "الكل") {
            result = result.filter { log ->
                log.action.equals(selectedActionFilter, ignoreCase = true)
            }
        }

        // 2. Filter by Search Query
        if (searchQuery.isNotEmpty()) {
            result = result.filter { log ->
                log.action.contains(searchQuery, ignoreCase = true) ||
                        log.targetId.contains(searchQuery, ignoreCase = true) ||
                        log.userId.contains(searchQuery, ignoreCase = true)
            }
        }

        // 3. Sort
        if (isDescending) {
            result.sortedByDescending { log -> log.timestamp }
        } else {
            result.sortedBy { log -> log.timestamp }
        }
    }

    if (isEmbedded) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp)
        ) {
            LogMainColumnContent(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedActionFilter = selectedActionFilter,
                onActionFilterChange = { selectedActionFilter = it },
                actionFilters = actionFilters,
                filteredAndSortedLogs = filteredAndSortedLogs,
                isDescending = isDescending,
                onIsDescendingChange = { isDescending = it },
                onClearClick = { showClearConfirmation = true },
                logsNotEmpty = logs.isNotEmpty(),
                isEmbedded = true
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "سجل العمليات والنشاطات 📜",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("activity_log_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward, // RTL back arrow
                                contentDescription = "رجوع",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        if (logs.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearConfirmation = true },
                                modifier = Modifier.testTag("activity_log_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "مسح السجل",
                                    tint = ErrorColorLight
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
                )
            },
            containerColor = BackgroundDark
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                LogMainColumnContent(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedActionFilter = selectedActionFilter,
                    onActionFilterChange = { selectedActionFilter = it },
                    actionFilters = actionFilters,
                    filteredAndSortedLogs = filteredAndSortedLogs,
                    isDescending = isDescending,
                    onIsDescendingChange = { isDescending = it },
                    onClearClick = { showClearConfirmation = true },
                    logsNotEmpty = logs.isNotEmpty(),
                    isEmbedded = false
                )
            }
        }
    }

    // Clear confirmation dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = {
                Text(
                    text = "تأكيد مسح السجل",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في مسح سجل العمليات بالكامل؟ لا يمكن التراجع عن هذا الإجراء.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearActivityLogs()
                        showClearConfirmation = false
                        Toast.makeText(context, "تم مسح السجل بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_clear_logs")
                ) {
                    Text("نعم، مسح", color = ErrorColorLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ColumnScope.LogMainColumnContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedActionFilter: String,
    onActionFilterChange: (String) -> Unit,
    actionFilters: List<String>,
    filteredAndSortedLogs: List<ActivityLog>,
    isDescending: Boolean,
    onIsDescendingChange: (Boolean) -> Unit,
    onClearClick: () -> Unit,
    logsNotEmpty: Boolean,
    isEmbedded: Boolean
) {
    // Search Bar
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("البحث في السجل أو الطلاب...", color = TextTertiary, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "بحث",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "مسح البحث",
                        tint = TextSecondary
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryIndigoLight,
            unfocusedBorderColor = BorderColor,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("activity_log_search_input")
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Action Filter Chips (Horizontal Scrollable Row)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actionFilters.forEach { filter ->
            val isSelected = selectedActionFilter == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else SurfaceDark)
                    .border(
                        1.dp,
                        if (isSelected) PrimaryIndigoLight else BorderColor,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onActionFilterChange(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("activity_log_filter_chip_$filter")
            ) {
                Text(
                    text = filter,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) PrimaryIndigoLight else TextSecondary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Sort & Info row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "عدد العمليات: ${filteredAndSortedLogs.size}",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEmbedded && logsNotEmpty) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("activity_log_clear_button_embedded")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "مسح السجل",
                        tint = ErrorColorLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Sort Order Toggle Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .clickable { onIsDescendingChange(!isDescending) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = "ترتيب",
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDescending) "الأحدث أولاً" else "الأقدم أولاً",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Logs Render Area
    if (filteredAndSortedLogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "لا توجد نشاطات مسجلة مطابقة للبحث.",
                    color = TextTertiary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("activity_logs_list")
        ) {
            items(filteredAndSortedLogs, key = { it.id }) { log ->
                ActivityLogItemCard(log = log)
            }
        }
    }
}

@Composable
fun ActivityLogItemCard(log: ActivityLog) {
    val dateStr = remember(log.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    // Determine color and icon based on action type
    val actionColor = remember(log.action) {
        when {
            log.action.contains("إضافة طالب") -> Color(0xFF4CAF50) // Green
            log.action.contains("تعديل طالب") -> Color(0xFFFF9800) // Orange
            log.action.contains("حذف طالب") -> Color(0xFFF44336) // Red
            log.action.contains("حضور") -> Color(0xFF3F51B5) // Indigo
            log.action.contains("غياب") -> Color(0xFFE91E63) // Pink
            log.action.contains("إضافة دفعة") -> Color(0xFF009688) // Teal
            log.action.contains("تعديل دفعة") -> Color(0xFF9C27B0) // Purple
            log.action.contains("حذف دفعة") -> Color(0xFFFF5722) // Deep Orange
            else -> PrimaryIndigoLight
        }
    }

    val icon = remember(log.action) {
        when {
            log.action.contains("إضافة طالب") -> Icons.Default.PersonAdd
            log.action.contains("تعديل طالب") -> Icons.Default.EditCalendar
            log.action.contains("حذف طالب") -> Icons.Default.PersonRemove
            log.action.contains("حضور") -> Icons.Default.CheckCircle
            log.action.contains("غياب") -> Icons.Default.Cancel
            log.action.contains("إضافة دفعة") -> Icons.Default.Paid
            log.action.contains("تعديل دفعة") -> Icons.Default.AppRegistration
            log.action.contains("حذف دفعة") -> Icons.Default.PriceCheck
            else -> Icons.Default.Bookmark
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .testTag("activity_log_item_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon indicator
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(actionColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.action,
                        color = actionColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Text(
                        text = dateStr,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.targetId,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "المشغل: ${log.userId}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
