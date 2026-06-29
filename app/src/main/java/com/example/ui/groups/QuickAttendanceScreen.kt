package com.example.ui.groups

import android.app.DatePickerDialog
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Attendance
import com.example.data.model.Group
import com.example.data.model.Student
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAttendanceScreen(
    initialGroupId: String? = null,
    viewModel: GroupsViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()

    // Filters from ViewModel
    val selectedGradeFilter by viewModel.selectedGradeFilter.collectAsStateWithLifecycle()
    val selectedGroupFilter by viewModel.selectedGroupFilter.collectAsStateWithLifecycle()
    val selectedCourseFilter by viewModel.selectedCourseFilter.collectAsStateWithLifecycle()

    // Initialize group filter if initialGroupId is provided
    LaunchedEffect(initialGroupId) {
        if (initialGroupId != null) {
            viewModel.setGroupFilter(initialGroupId)
        }
    }

    // Selected Date
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ar")) }

    // Unique lists for filter dropdowns
    val gradesList = remember(students) {
        students.map { it.grade }.filter { it.isNotEmpty() }.distinct()
    }
    val coursesList = remember(students) {
        students.map { it.customCourse }.filter { it.isNotEmpty() }.distinct()
    }

    // Dropdown visibility states
    var showGradeDropdown by remember { mutableStateOf(false) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var showCourseDropdown by remember { mutableStateOf(false) }

    // Filter students
    val filteredStudents = remember(students, selectedGradeFilter, selectedGroupFilter, selectedCourseFilter) {
        students.filter { student ->
            student.isActive &&
                    (selectedGradeFilter == null || student.grade == selectedGradeFilter) &&
                    (selectedGroupFilter == null || student.groupId == selectedGroupFilter) &&
                    (selectedCourseFilter == null || student.customCourse == selectedCourseFilter)
        }
    }

    // Efficient Attendance Map for the selected day
    val startOfDay = remember(selectedDate) { getStartOfDay(selectedDate) }
    val endOfDay = remember(selectedDate) { startOfDay + (24 * 60 * 60 * 1000) - 1 }
    
    val currentDayAttendanceMap = remember(attendance, selectedDate) {
        attendance.filter { it.date in startOfDay..endOfDay }
            .associateBy { it.studentId }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            // Elegant Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            text = "تسجيل الحضور السريع",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "سجل الحضور والغياب بنقرة واحدة",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Date Picker Button
                Card(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = selectedDate
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = Calendar.getInstance()
                                selectedCal.set(year, month, dayOfMonth)
                                selectedDate = selectedCal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(dateFormat.format(Date(selectedDate)), fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filters Section (Horizontal Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group Filter Chip
                Box {
                    FilterChip(
                        selected = selectedGroupFilter != null,
                        onClick = { showGroupDropdown = true },
                        label = {
                            val groupName = groups.find { it.id == selectedGroupFilter }?.name ?: "كل المجموعات"
                            Text(groupName, fontSize = 11.sp)
                        },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryIndigoLight,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedGroupFilter != null, borderColor = BorderColor, selectedBorderColor = PrimaryIndigoLight)
                    )
                    DropdownMenu(
                        expanded = showGroupDropdown,
                        onDismissRequest = { showGroupDropdown = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("كل المجموعات", color = TextPrimary) },
                            onClick = {
                                viewModel.setGroupFilter(null)
                                showGroupDropdown = false
                            }
                        )
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name, color = TextPrimary) },
                                onClick = {
                                    viewModel.setGroupFilter(group.id)
                                    showGroupDropdown = false
                                }
                            )
                        }
                    }
                }

                // Grade Filter Chip (السنة الدراسية)
                Box {
                    FilterChip(
                        selected = selectedGradeFilter != null,
                        onClick = { showGradeDropdown = true },
                        label = { Text(selectedGradeFilter ?: "كل السنوات الدراسية", fontSize = 11.sp) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryIndigoLight,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedGradeFilter != null, borderColor = BorderColor, selectedBorderColor = PrimaryIndigoLight)
                    )
                    DropdownMenu(
                        expanded = showGradeDropdown,
                        onDismissRequest = { showGradeDropdown = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("كل السنوات الدراسية", color = TextPrimary) },
                            onClick = {
                                viewModel.setGradeFilter(null)
                                showGradeDropdown = false
                            }
                        )
                        gradesList.forEach { grade ->
                            DropdownMenuItem(
                                text = { Text(grade, color = TextPrimary) },
                                onClick = {
                                    viewModel.setGradeFilter(grade)
                                    showGradeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Course Filter Chip
                Box {
                    FilterChip(
                        selected = selectedCourseFilter != null,
                        onClick = { showCourseDropdown = true },
                        label = { Text(selectedCourseFilter ?: "كل الكورسات", fontSize = 11.sp) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryIndigoLight,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedCourseFilter != null, borderColor = BorderColor, selectedBorderColor = PrimaryIndigoLight)
                    )
                    DropdownMenu(
                        expanded = showCourseDropdown,
                        onDismissRequest = { showCourseDropdown = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("كل الكورسات", color = TextPrimary) },
                            onClick = {
                                viewModel.setCourseFilter(null)
                                showCourseDropdown = false
                            }
                        )
                        coursesList.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course, color = TextPrimary) },
                                onClick = {
                                    viewModel.setCourseFilter(course)
                                    showCourseDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // One-Click Mass Action Buttons (تسجيل الحضور بضغطة واحدة / تسجيل الغياب بضغطة واحدة)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mass Present
                Button(
                    onClick = {
                        if (filteredStudents.isEmpty()) {
                            Toast.makeText(context, "لا توجد طلاب في القائمة لتسجيلهم", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // We register all filtered students as present
                        filteredStudents.forEach { student ->
                            viewModel.toggleSingleStudentAttendance(student.id, "present", selectedDate)
                        }
                        Toast.makeText(context, "تم تسجيل الجميع كـ حضور", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل الجميع حضور", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Mass Absent
                Button(
                    onClick = {
                        if (filteredStudents.isEmpty()) {
                            Toast.makeText(context, "لا توجد طلاب في القائمة لتسجيلهم", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        filteredStudents.forEach { student ->
                            viewModel.toggleSingleStudentAttendance(student.id, "absent", selectedDate)
                        }
                        Toast.makeText(context, "تم تسجيل الجميع كـ غياب", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل الجميع غياب", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Students Attendance List
            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonOff, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا يوجد طلاب نشطين ضمن الفلاتر المحددة.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val attRecord = currentDayAttendanceMap[student.id]
                        val currentStatus = attRecord?.status ?: "not_recorded"

                        AttendanceStudentRow(
                            student = student,
                            currentStatus = currentStatus,
                            groupName = groups.find { it.id == student.groupId }?.name ?: "بدون مجموعة",
                            onStatusSelect = { newStatus ->
                                viewModel.toggleSingleStudentAttendance(student.id, newStatus, selectedDate)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceStudentRow(
    student: Student,
    currentStatus: String,
    groupName: String,
    onStatusSelect: (String) -> Unit
) {
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Student Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "المجموعة: $groupName",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    if (student.grade.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  ${student.grade}",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Quick Toggle Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Present Button
                val isPresent = currentStatus == "present"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPresent) SuccessColor.copy(alpha = 0.2f) else SurfaceLightDark)
                        .border(1.dp, if (isPresent) SuccessColor else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { onStatusSelect("present") }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "حضور",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPresent) SuccessColorLight else TextSecondary
                    )
                }

                // Absent Button
                val isAbsent = currentStatus == "absent"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAbsent) ErrorColor.copy(alpha = 0.2f) else SurfaceLightDark)
                        .border(1.dp, if (isAbsent) ErrorColor else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { onStatusSelect("absent") }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "غياب",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAbsent) ErrorColorLight else TextSecondary
                    )
                }

                // Late / Special Toggle
                val isLate = currentStatus == "late"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLate) WarningColor.copy(alpha = 0.2f) else SurfaceLightDark)
                        .border(1.dp, if (isLate) WarningColor else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { onStatusSelect("late") }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تأخير",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLate) WarningColor else TextTertiary
                    )
                }
            }
        }
    }
}

private fun getStartOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
