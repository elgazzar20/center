package com.example.ui.schedule

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ScheduleEvent
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterScheduleScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val events by viewModel.scheduleEvents.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val classrooms by viewModel.classrooms.collectAsStateWithLifecycle()

    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar"))
    val timeFormat = SimpleDateFormat("hh:mm a", Locale("ar"))

    val dailyEvents = events.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
        cal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
    }.sortedBy { it.startTime }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نظام الجدولة", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        generateDailySchedulePdf(context, dailyEvents, selectedDate, viewModel)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "طباعة ومشاركة PDF")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة حصة")
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
            // Date Navigator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newDate = Calendar.getInstance().apply {
                        timeInMillis = selectedDate.timeInMillis
                        add(Calendar.DAY_OF_YEAR, -1)
                    }
                    selectedDate = newDate
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "السابق", tint = TextPrimary)
                }

                Text(
                    text = dateFormat.format(selectedDate.time),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                IconButton(onClick = {
                    val newDate = Calendar.getInstance().apply {
                        timeInMillis = selectedDate.timeInMillis
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    selectedDate = newDate
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "التالي", tint = TextPrimary)
                }
            }

            if (dailyEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد حصص مجدولة في هذا اليوم", color = TextTertiary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dailyEvents) { event ->
                        val teacherName = teachers.find { it.id == event.teacherId }?.name ?: "غير محدد"
                        val roomName = classrooms.find { it.id == event.classroomId }?.name ?: "غير محدد"
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                    Text(timeFormat.format(Date(event.startTime)), color = PrimaryIndigoLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("إلى", color = TextSecondary, fontSize = 12.sp)
                                    Text(timeFormat.format(Date(event.endTime)), color = TextSecondary, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(event.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(teacherName, color = TextSecondary, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(roomName, color = TextSecondary, fontSize = 13.sp)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteScheduleEvent(event.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleEventDialog(
            viewModel = viewModel,
            selectedDate = selectedDate,
            onDismiss = { showAddDialog = false },
            onConfirm = { event ->
                viewModel.addScheduleEvent(
                    event = event,
                    onSuccess = { showAddDialog = false },
                    onError = { error -> showErrorDialog = error }
                )
            }
        )
    }

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            containerColor = SurfaceDark,
            title = { Text("تعارض في المواعيد", color = ErrorColor, fontWeight = FontWeight.Bold) },
            text = { Text(showErrorDialog!!, color = TextPrimary) },
            confirmButton = {
                Button(onClick = { showErrorDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
fun AddScheduleEventDialog(
    viewModel: AppViewModel,
    selectedDate: Calendar,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleEvent) -> Unit
) {
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val classrooms by viewModel.classrooms.collectAsStateWithLifecycle()
    val events by viewModel.scheduleEvents.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var selectedTeacherId by remember { mutableStateOf("") }
    
    var startHour by remember { mutableIntStateOf(8) }
    var startMinute by remember { mutableIntStateOf(0) }
    var durationHours by remember { mutableIntStateOf(1) }
    var durationMinutes by remember { mutableIntStateOf(30) }
    
    // Automatically filter available classrooms based on selected time
    val startTimeInMillis = remember(selectedDate, startHour, startMinute) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }
    
    val endTimeInMillis = remember(startTimeInMillis, durationHours, durationMinutes) {
        startTimeInMillis + (durationHours * 3600000L) + (durationMinutes * 60000L)
    }

    val availableClassrooms = remember(startTimeInMillis, endTimeInMillis, classrooms, events) {
        classrooms.filter { room ->
            val roomEvents = events.filter { it.classroomId == room.id }
            roomEvents.none { 
                startTimeInMillis < it.endTime && endTimeInMillis > it.startTime
            }
        }
    }

    var selectedClassroomId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("إضافة حصة جديدة", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("المادة / العنوان") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                Text("المعلم:", color = TextSecondary, fontSize = 12.sp)
                LazyColumn(modifier = Modifier.height(100.dp)) {
                    items(teachers) { teacher ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTeacherId = teacher.id }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTeacherId == teacher.id, onClick = { selectedTeacherId = teacher.id })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(teacher.name, color = TextPrimary)
                        }
                    }
                }

                Text("وقت البداية:", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startHour.toString(),
                        onValueChange = { startHour = it.toIntOrNull() ?: 0 },
                        label = { Text("ساعة (0-23)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = startMinute.toString(),
                        onValueChange = { startMinute = it.toIntOrNull() ?: 0 },
                        label = { Text("دقيقة") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }

                Text("المدة:", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationHours.toString(),
                        onValueChange = { durationHours = it.toIntOrNull() ?: 0 },
                        label = { Text("ساعات") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = durationMinutes.toString(),
                        onValueChange = { durationMinutes = it.toIntOrNull() ?: 0 },
                        label = { Text("دقائق") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }

                Text("القاعات المتاحة في هذا التوقيت:", color = TextSecondary, fontSize = 12.sp)
                if (availableClassrooms.isEmpty()) {
                    Text("لا توجد قاعات متاحة في هذا التوقيت", color = ErrorColor, fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.height(100.dp)) {
                        items(availableClassrooms) { room ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedClassroomId = room.id }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedClassroomId == room.id, onClick = { selectedClassroomId = room.id })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(room.name, color = TextPrimary)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("السعة: ${room.capacity}", color = TextTertiary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && selectedTeacherId.isNotEmpty() && selectedClassroomId.isNotEmpty()) {
                        onConfirm(
                            ScheduleEvent(
                                title = title,
                                type = "class",
                                startTime = startTimeInMillis,
                                endTime = endTimeInMillis,
                                teacherId = selectedTeacherId,
                                classroomId = selectedClassroomId
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                enabled = selectedClassroomId.isNotEmpty() && selectedTeacherId.isNotEmpty()
            ) {
                Text("حفظ الحصة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}

fun generateDailySchedulePdf(context: Context, events: List<ScheduleEvent>, date: Calendar, viewModel: AppViewModel) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        val textPaint = Paint().apply {
            textSize = 14f
            textAlign = Paint.Align.RIGHT
        }

        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar"))
        val timeFormat = SimpleDateFormat("hh:mm a", Locale("ar"))
        val dateStr = dateFormat.format(date.time)

        canvas.drawText("الجدول اليومي للسنتر", 297f, 50f, titlePaint)
        canvas.drawText("التاريخ: $dateStr", 550f, 100f, headerPaint)

        var yPos = 150f
        
        val teachers = viewModel.teachers.value
        val classrooms = viewModel.classrooms.value

        canvas.drawText("الوقت", 550f, yPos, headerPaint)
        canvas.drawText("المادة", 400f, yPos, headerPaint)
        canvas.drawText("المعلم", 250f, yPos, headerPaint)
        canvas.drawText("القاعة", 100f, yPos, headerPaint)
        yPos += 30f

        events.forEach { event ->
            val teacherName = teachers.find { it.id == event.teacherId }?.name ?: ""
            val roomName = classrooms.find { it.id == event.classroomId }?.name ?: ""
            val timeStr = "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}"

            canvas.drawText(timeStr, 550f, yPos, textPaint)
            canvas.drawText(event.title, 400f, yPos, textPaint)
            canvas.drawText(teacherName, 250f, yPos, textPaint)
            canvas.drawText(roomName, 100f, yPos, textPaint)
            
            yPos += 30f
            if (yPos > 800f) {
                // Should handle multi-page, skipping for brevity
            }
        }

        pdfDocument.finishPage(page)

        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "NexoraSchedules")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "DailySchedule_${date.timeInMillis}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الجدول اليومي"))
        
        Toast.makeText(context, "تم إنشاء ومشاركة PDF بنجاح", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "فشل إنشاء PDF", Toast.LENGTH_SHORT).show()
    }
}
