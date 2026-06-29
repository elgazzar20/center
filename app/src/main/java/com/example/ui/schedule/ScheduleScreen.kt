package com.example.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ScheduleEvent
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    var viewMode by remember { mutableStateOf("يومي") } // "يومي", "أسبوعي", "شهري"
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showAddDialog by remember { mutableStateOf(false) }

    val events by viewModel.scheduleEvents.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الجدول والمواعيد", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة موعد")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary,
                    actionIconContentColor = PrimaryIndigoLight
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // View Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("يومي", "أسبوعي", "شهري").forEach { mode ->
                    val isSelected = viewMode == mode
                    Surface(
                        color = if (isSelected) PrimaryIndigo else SurfaceDark,
                        shape = RoundedCornerShape(16.dp),
                        border = if (!isSelected) borderStroke(BorderColor) else null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { viewMode = mode }
                    ) {
                        Text(
                            text = mode,
                            color = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Date Navigator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val cal = selectedDate.clone() as Calendar
                    when (viewMode) {
                        "يومي" -> cal.add(Calendar.DAY_OF_MONTH, -1)
                        "أسبوعي" -> cal.add(Calendar.WEEK_OF_YEAR, -1)
                        "شهري" -> cal.add(Calendar.MONTH, -1)
                    }
                    selectedDate = cal
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "السابق", tint = TextPrimary)
                }
                
                Text(
                    text = formatDate(selectedDate, viewMode),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = {
                    val cal = selectedDate.clone() as Calendar
                    when (viewMode) {
                        "يومي" -> cal.add(Calendar.DAY_OF_MONTH, 1)
                        "أسبوعي" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                        "شهري" -> cal.add(Calendar.MONTH, 1)
                    }
                    selectedDate = cal
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "التالي", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Event List based on View Mode
            val filteredEvents = events.filter { event ->
                val eventCal = Calendar.getInstance().apply { timeInMillis = event.startTime }
                when (viewMode) {
                    "يومي" -> {
                        eventCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                        eventCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                    }
                    "أسبوعي" -> {
                        eventCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                        eventCal.get(Calendar.WEEK_OF_YEAR) == selectedDate.get(Calendar.WEEK_OF_YEAR)
                    }
                    "شهري" -> {
                        eventCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                        eventCal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH)
                    }
                    else -> false
                }
            }

            if (filteredEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد مواعيد في هذه الفترة", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(filteredEvents.sortedBy { it.startTime }) { event ->
                        EventCard(event = event) {
                            viewModel.deleteScheduleEvent(event.id)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleEventDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun EventCard(event: ScheduleEvent, onDelete: () -> Unit) {
    val formatter = SimpleDateFormat("hh:mm a", Locale("ar"))
    val startTimeStr = formatter.format(Date(event.startTime))
    val endTimeStr = formatter.format(Date(event.endTime))
    
    val (icon, color) = when (event.type) {
        "class" -> Icons.Default.Class to PrimaryIndigoLight
        "revision" -> Icons.Default.MenuBook to SuccessColorLight
        "exam" -> Icons.Default.Quiz to WarningColor
        "private" -> Icons.Default.Person to ErrorColor
        else -> Icons.Default.Event to TextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (event.description.isNotEmpty()) {
                    Text(event.description, color = TextSecondary, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$startTimeStr - $endTimeStr", color = TextSecondary, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColor)
            }
        }
    }
}

@Composable
fun AddScheduleEventDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("class") } // class, revision, exam, private

    // Simple time selection (for demo purposes)
    var selectedHour by remember { mutableStateOf("10") }
    var selectedMinute by remember { mutableStateOf("00") }
    var durationMinutes by remember { mutableStateOf("60") }

    val types = listOf("class" to "حصة", "revision" to "مراجعة", "exam" to "امتحان", "private" to "موعد خاص")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("إضافة موعد جديد", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الموعد") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("نوع الموعد:", color = TextPrimary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    types.forEach { (type, label) ->
                        Surface(
                            color = if (selectedType == type) PrimaryIndigo else SurfaceDark,
                            border = if (selectedType != type) borderStroke(BorderColor) else null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clickable { selectedType = type }
                        ) {
                            Text(
                                text = label,
                                color = if (selectedType == type) Color.White else TextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedHour,
                        onValueChange = { selectedHour = it },
                        label = { Text("ساعة (0-23)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = selectedMinute,
                        onValueChange = { selectedMinute = it },
                        label = { Text("دقيقة") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    label = { Text("المدة (بالدقائق)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, selectedHour.toIntOrNull() ?: 10)
                        cal.set(Calendar.MINUTE, selectedMinute.toIntOrNull() ?: 0)
                        val startTime = cal.timeInMillis
                        val endTime = startTime + ((durationMinutes.toIntOrNull() ?: 60) * 60 * 1000)
                        
                        val event = ScheduleEvent(
                            title = title,
                            description = description,
                            type = selectedType,
                            startTime = startTime,
                            endTime = endTime
                        )
                        viewModel.addScheduleEvent(event)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}

private fun formatDate(cal: Calendar, mode: String): String {
    val formatter = when (mode) {
        "يومي" -> SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar"))
        "أسبوعي" -> SimpleDateFormat("'الأسبوع' w, yyyy", Locale("ar"))
        "شهري" -> SimpleDateFormat("MMMM yyyy", Locale("ar"))
        else -> SimpleDateFormat("yyyy-MM-dd")
    }
    return formatter.format(cal.time)
}

@Composable
private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
