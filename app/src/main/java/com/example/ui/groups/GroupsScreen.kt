package com.example.ui.groups

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
import com.example.data.model.Group
import com.example.data.model.Student
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null,
    onNavigateToQuickAttendance: (groupId: String) -> Unit
) {
    val context = LocalContext.current
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    // Dialog flags
    var showAddDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<Group?>(null) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var groupForStudentManagement by remember { mutableStateOf<Group?>(null) }

    val filteredGroups = groups.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.teacherName.contains(searchQuery, ignoreCase = true) ||
                it.classroom.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مجموعة جديدة")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        text = "نظام المجموعات والفرق",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "نظم صفوفك الدراسية وحضور الطلاب بسهولة",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المجموعة، المعلم أو القاعة...", fontSize = 13.sp, color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.GroupWork,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "لا توجد مجموعات حالياً." else "لا توجد مجموعات مطابقة لبحثك.",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Text("أنشئ أول مجموعة", color = Color.White)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredGroups, key = { it.id }) { group ->
                        val groupStudents = students.filter { it.groupId == group.id && it.isActive }
                        
                        GroupCard(
                            group = group,
                            studentCount = groupStudents.size,
                            onEditClick = { groupToEdit = group },
                            onDeleteClick = { groupToDelete = group },
                            onManageStudentsClick = { groupForStudentManagement = group },
                            onQuickAttendanceClick = { onNavigateToQuickAttendance(group.id) }
                        )
                    }
                }
            }
        }
    }

    // Create Group Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var teacherName by remember { mutableStateOf("") }
        var classroom by remember { mutableStateOf("") }
        var schedule by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        var showTeacherDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إنشاء مجموعة جديدة", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المجموعة *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    // Teacher Selector (Can type custom or select from list)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = teacherName,
                            onValueChange = { teacherName = it },
                            label = { Text("اسم المعلم") },
                            trailingIcon = {
                                IconButton(onClick = { showTeacherDropdown = !showTeacherDropdown }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "اختر المعلم")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryIndigoLight,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        DropdownMenu(
                            expanded = showTeacherDropdown,
                            onDismissRequest = { showTeacherDropdown = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            teachers.forEach { teacher ->
                                DropdownMenuItem(
                                    text = { Text(teacher.name, color = TextPrimary) },
                                    onClick = {
                                        teacherName = teacher.name
                                        showTeacherDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("القاعة / الفصل الدراسي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    OutlinedTextField(
                        value = schedule,
                        onValueChange = { schedule = it },
                        label = { Text("مواعيد المجموعة (مثال: السبت والثلاثاء 4م)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية") },
                        modifier = Modifier.fillMaxWidth(),
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
                        if (name.trim().isNotEmpty()) {
                            viewModel.addGroup(
                                name = name,
                                teacherName = teacherName,
                                classroom = classroom,
                                schedule = schedule,
                                notes = notes,
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                },
                                onSuccess = {
                                    showAddDialog = false
                                    Toast.makeText(context, "تم إنشاء المجموعة بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "يرجى كتابة اسم المجموعة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("حفظ", color = Color.White)
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

    // Edit Group Dialog
    if (groupToEdit != null) {
        val group = groupToEdit!!
        var name by remember { mutableStateOf(group.name) }
        var teacherName by remember { mutableStateOf(group.teacherName) }
        var classroom by remember { mutableStateOf(group.classroom) }
        var schedule by remember { mutableStateOf(group.schedule) }
        var notes by remember { mutableStateOf(group.notes) }

        var showTeacherDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { groupToEdit = null },
            title = { Text("تعديل تفاصيل المجموعة", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المجموعة *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = teacherName,
                            onValueChange = { teacherName = it },
                            label = { Text("اسم المعلم") },
                            trailingIcon = {
                                IconButton(onClick = { showTeacherDropdown = !showTeacherDropdown }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "اختر المعلم")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryIndigoLight,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        DropdownMenu(
                            expanded = showTeacherDropdown,
                            onDismissRequest = { showTeacherDropdown = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            teachers.forEach { teacher ->
                                DropdownMenuItem(
                                    text = { Text(teacher.name, color = TextPrimary) },
                                    onClick = {
                                        teacherName = teacher.name
                                        showTeacherDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("القاعة / الفصل الدراسي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    OutlinedTextField(
                        value = schedule,
                        onValueChange = { schedule = it },
                        label = { Text("مواعيد المجموعة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.fillMaxWidth(),
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
                        if (name.trim().isNotEmpty()) {
                            viewModel.updateGroup(
                                group.copy(
                                    name = name,
                                    teacherName = teacherName,
                                    classroom = classroom,
                                    schedule = schedule,
                                    notes = notes
                                )
                            )
                            groupToEdit = null
                            Toast.makeText(context, "تم حفظ التعديلات بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "اسم المجموعة مطلوب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("تعديل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToEdit = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Delete Group Confirmation Dialog
    if (groupToDelete != null) {
        val group = groupToDelete!!
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("حذف المجموعة", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف مجموعة \"${group.name}\"؟\nسيتم إزالة ربط جميع الطلاب بهذه المجموعة تلقائياً دون حذف بياناتهم.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(group.id)
                        groupToDelete = null
                        Toast.makeText(context, "تم حذف المجموعة بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Student Management inside Group Dialog (Adding/Removing students)
    if (groupForStudentManagement != null) {
        val group = groupForStudentManagement!!
        var sQuery by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { groupForStudentManagement = null },
            title = {
                Text(
                    "إدارة طلاب مجموعة ${group.name}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text("اختر الطلاب للانضمام لهذه المجموعة:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = sQuery,
                        onValueChange = { sQuery = it },
                        placeholder = { Text("ابحث باسم الطالب...", fontSize = 12.sp, color = TextTertiary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeStudents = students.filter { it.isActive }
                    val filteredStudents = activeStudents.filter {
                        it.name.contains(sQuery, ignoreCase = true)
                    }

                    if (filteredStudents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا يوجد طلاب مطابقين.", color = TextTertiary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredStudents) { student ->
                                val isMember = student.groupId == group.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isMember) PrimaryIndigo.copy(alpha = 0.08f) else SurfaceLightDark)
                                        .clickable {
                                            viewModel.updateStudentGroup(
                                                student.id,
                                                if (isMember) null else group.id
                                            )
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(student.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (student.groupId == null) "غير مضاف لمجموعة" else if (student.groupId == group.id) "مضاف لهذه المجموعة" else "مضاف لمجموعة أخرى",
                                            color = if (student.groupId == group.id) SuccessColorLight else TextTertiary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Checkbox(
                                        checked = isMember,
                                        onCheckedChange = { checked ->
                                            viewModel.updateStudentGroup(
                                                student.id,
                                                if (checked) group.id else null
                                            )
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = PrimaryIndigoLight,
                                            uncheckedColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { groupForStudentManagement = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun GroupCard(
    group: Group,
    studentCount: Int,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onManageStudentsClick: () -> Unit,
    onQuickAttendanceClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Group Title & Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = group.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PrimaryIndigoLight, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ErrorColorLight, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details (Grid structure using Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLightDark, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("المعلم", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = group.teacherName.ifEmpty { "غير محدد" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column {
                    Text("القاعة / الفصل", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = group.classroom.ifEmpty { "غير محدد" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column {
                    Text("عدد الطلاب", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "$studentCount طالب",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessColorLight
                    )
                }
            }

            if (group.schedule.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المواعيد: ${group.schedule}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (group.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ملاحظات: ${group.notes}",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CTAs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Manage Students
                Button(
                    onClick = onManageStudentsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceLightDark,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إدارة الطلاب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Quick Attendance Button
                Button(
                    onClick = onQuickAttendanceClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الحضور السريع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
