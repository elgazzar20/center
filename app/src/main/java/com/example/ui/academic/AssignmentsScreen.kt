package com.example.ui.academic

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Assignment
import com.example.data.model.Group
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    viewModel: AppViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf<String?> (null) }

    // Dialog flags
    var showAddDialog by remember { mutableStateOf(false) }
    var assignmentToEdit by remember { mutableStateOf<Assignment?>(null) }
    var assignmentToDelete by remember { mutableStateOf<Assignment?>(null) }

    val filteredAssignments = assignments.filter {
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) || 
                            it.description.contains(searchQuery, ignoreCase = true)
        val matchesGroup = selectedGroupFilter == null || it.groupId == selectedGroupFilter
        matchesSearch && matchesGroup
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إدارة الواجبات والمهام",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة واجب جديد")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Search & Group Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن واجب...", color = TextTertiary, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    singleLine = true
                )

                // Group filter chips
                var showGroupFilterMenu by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { showGroupFilterMenu = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGroupFilter != null) PrimaryIndigo else SurfaceDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = if (selectedGroupFilter != null) Color.White else PrimaryIndigoLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (selectedGroupFilter != null) {
                                    groups.find { it.id == selectedGroupFilter }?.name ?: "مجموعة"
                                } else "كل المجموعات",
                                color = if (selectedGroupFilter != null) Color.White else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showGroupFilterMenu,
                        onDismissRequest = { showGroupFilterMenu = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("كل المجموعات", color = TextPrimary) },
                            onClick = {
                                selectedGroupFilter = null
                                showGroupFilterMenu = false
                            }
                        )
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name, color = TextPrimary) },
                                onClick = {
                                    selectedGroupFilter = group.id
                                    showGroupFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Assignments list
            if (filteredAssignments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "لا توجد واجبات مضافة حالياً",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اضغط على الزر بالأسفل لإضافة واجب جديد للمجموعات.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredAssignments, key = { it.id }) { assignment ->
                        AssignmentCard(
                            assignment = assignment,
                            onEditClick = { assignmentToEdit = assignment },
                            onDeleteClick = { assignmentToDelete = assignment }
                        )
                    }
                }
            }
        }
    }

    // Add Assignment Dialog
    if (showAddDialog) {
        AssignmentFormDialog(
            groups = groups,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, dueDate, groupId, groupName, notes ->
                viewModel.addAssignment(
                    title = title,
                    description = desc,
                    dueDate = dueDate,
                    groupId = groupId,
                    groupName = groupName,
                    notes = notes
                )
                showAddDialog = false
                Toast.makeText(context, "تمت إضافة الواجب بنجاح 👍", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Assignment Dialog
    assignmentToEdit?.let { assignment ->
        AssignmentFormDialog(
            assignment = assignment,
            groups = groups,
            onDismiss = { assignmentToEdit = null },
            onConfirm = { title, desc, dueDate, groupId, groupName, notes ->
                viewModel.updateAssignment(
                    assignment.copy(
                        title = title,
                        description = desc,
                        dueDate = dueDate,
                        groupId = groupId,
                        groupName = groupName,
                        notes = notes
                    )
                )
                assignmentToEdit = null
                Toast.makeText(context, "تم تعديل الواجب بنجاح 👍", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Assignment Dialog
    assignmentToDelete?.let { assignment ->
        AlertDialog(
            onDismissRequest = { assignmentToDelete = null },
            title = {
                Text(
                    text = "حذف الواجب",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف واجب \"${assignment.title}\"؟ لا يمكن التراجع عن هذه العملية.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAssignment(assignment.id)
                        assignmentToDelete = null
                        Toast.makeText(context, "تم حذف الواجب", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("نعم، حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { assignmentToDelete = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ar")) }
    val isPastDue = assignment.dueDate < System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group badge
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = assignment.groupName.ifEmpty { "غير محدد" },
                        color = PrimaryIndigoLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = ErrorColorLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = assignment.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (assignment.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = assignment.description,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            if (assignment.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceLightDark, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = assignment.notes,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isPastDue) ErrorColorLight else SuccessColorLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "تاريخ التسليم: ${sdf.format(Date(assignment.dueDate))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isPastDue) ErrorColorLight else TextSecondary
                    )
                }

                if (isPastDue) {
                    Text(
                        text = "منتهي",
                        fontSize = 11.sp,
                        color = ErrorColorLight,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "نشط",
                        fontSize = 11.sp,
                        color = SuccessColorLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentFormDialog(
    assignment: Assignment? = null,
    groups: List<Group>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, dueDate: Long, groupId: String, groupName: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(assignment?.title ?: "") }
    var description by remember { mutableStateOf(assignment?.description ?: "") }
    var notes by remember { mutableStateOf(assignment?.notes ?: "") }
    
    // Group selection
    var selectedGroupId by remember { mutableStateOf(assignment?.groupId ?: "") }
    var selectedGroupName by remember { mutableStateOf(assignment?.groupName ?: "") }
    var showGroupDropdown by remember { mutableStateOf(false) }

    // Date selection (Due date)
    val calendar = remember {
        Calendar.getInstance().apply {
            if (assignment != null) {
                timeInMillis = assignment.dueDate
            } else {
                add(Calendar.DAY_OF_YEAR, 1) // default to tomorrow
            }
        }
    }
    
    var year by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var day by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    
    val datePickerDialog = android.app.DatePickerDialog(
        LocalContext.current,
        { _, y, m, d ->
            year = y
            month = m
            day = d
        },
        year, month, day
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (assignment == null) "إضافة واجب جديد" else "تعديل الواجب",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الواجب") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = PrimaryIndigoLight,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف أو تفاصيل الواجب") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = PrimaryIndigoLight,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2
                )

                // Group Dropdown selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedGroupName.ifEmpty { "اختر المجموعة" },
                        onValueChange = {},
                        label = { Text("المجموعة المستهدفة") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGroupDropdown = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = BorderColor,
                            disabledLabelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showGroupDropdown,
                        onDismissRequest = { showGroupDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(SurfaceDark)
                    ) {
                        if (groups.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("لا توجد مجموعات! قم بإنشاء مجموعة أولاً", color = ErrorColorLight) },
                                onClick = { showGroupDropdown = false }
                            )
                        } else {
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name, color = TextPrimary) },
                                    onClick = {
                                        selectedGroupId = group.id
                                        selectedGroupName = group.name
                                        showGroupDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Date Picker trigger Button
                Button(
                    onClick = { datePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryIndigoLight)
                        Text(
                            text = "تاريخ التسليم: $day/${month + 1}/$year",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = PrimaryIndigoLight,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || selectedGroupId.isBlank()) {
                        return@Button
                    }
                    val selectedCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }
                    onConfirm(title, description, selectedCal.timeInMillis, selectedGroupId, selectedGroupName, notes)
                },
                enabled = title.isNotBlank() && selectedGroupId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("حفظ", color = Color.White, fontWeight = FontWeight.Bold)
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
