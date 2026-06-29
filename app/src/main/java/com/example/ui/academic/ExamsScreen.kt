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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.Exam
import com.example.data.model.ExamGrade
import com.example.data.model.Group
import com.example.data.model.Student
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(
    viewModel: AppViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val allStudents by viewModel.students.collectAsStateWithLifecycle()
    val allExamGrades by viewModel.examGrades.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }

    // Navigation sub-states: 0 = Exams List, 1 = Grade Recording, 2 = Statistics View
    var activeSubView by remember { mutableIntStateOf(0) }
    var activeExamForGrading by remember { mutableStateOf<Exam?>(null) }
    var activeExamForStats by remember { mutableStateOf<Exam?>(null) }

    // Dialog flags
    var showAddDialog by remember { mutableStateOf(false) }
    var examToEdit by remember { mutableStateOf<Exam?>(null) }
    var examToDelete by remember { mutableStateOf<Exam?>(null) }

    val filteredExams = exams.filter {
        val matchesSearch = it.name.contains(searchQuery, ignoreCase = true)
        val matchesGroup = selectedGroupFilter == null || it.groupId == selectedGroupFilter
        matchesSearch && matchesGroup
    }

    // Render Sub-Views
    when (activeSubView) {
        1 -> {
            activeExamForGrading?.let { exam ->
                GradeRecordingView(
                    exam = exam,
                    allStudents = allStudents,
                    allGrades = allExamGrades,
                    onBackClick = {
                        activeSubView = 0
                        activeExamForGrading = null
                    },
                    onSaveGrades = { gradesList ->
                        viewModel.saveExamGrades(gradesList)
                        activeSubView = 0
                        activeExamForGrading = null
                        Toast.makeText(context, "تم حفظ درجات الطلاب بنجاح 🎉", Toast.LENGTH_SHORT).show()
                    }
                )
            } ?: run { activeSubView = 0 }
        }
        2 -> {
            activeExamForStats?.let { exam ->
                ExamStatisticsView(
                    exam = exam,
                    allStudents = allStudents,
                    allGrades = allExamGrades,
                    onBackClick = {
                        activeSubView = 0
                        activeExamForStats = null
                    }
                )
            } ?: run { activeSubView = 0 }
        }
        else -> {
            // Main list view
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "إدارة الامتحانات والتقييمات",
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
                        Icon(Icons.Default.Add, contentDescription = "إضافة امتحان جديد")
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
                            placeholder = { Text("بحث عن امتحان...", color = TextTertiary, fontSize = 14.sp) },
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

                        // Group filter dropdown
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

                    // Exams List
                    if (filteredExams.isEmpty()) {
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
                                        imageVector = Icons.Default.AssignmentLate,
                                        contentDescription = null,
                                        tint = PrimaryIndigoLight,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "لا توجد امتحانات مضافة حالياً",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اضغط على زر الإضافة لتسجيل امتحان جديد وتنزيل الدرجات للطلاب.",
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
                            items(filteredExams, key = { it.id }) { exam ->
                                val examGradesCount = allExamGrades.count { it.examId == exam.id }
                                ExamCard(
                                    exam = exam,
                                    gradesCount = examGradesCount,
                                    onEditClick = { examToEdit = exam },
                                    onDeleteClick = { examToDelete = exam },
                                    onRecordGradesClick = {
                                        activeExamForGrading = exam
                                        activeSubView = 1
                                    },
                                    onViewStatsClick = {
                                        activeExamForStats = exam
                                        activeSubView = 2
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Exam Dialog
    if (showAddDialog) {
        ExamFormDialog(
            groups = groups,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, totalMarks, date, groupId, groupName ->
                viewModel.addExam(
                    name = name,
                    totalMarks = totalMarks,
                    date = date,
                    groupId = groupId,
                    groupName = groupName,
                    onExamAdded = { newExam ->
                        // Proactively open grading for the newly added exam!
                        activeExamForGrading = newExam
                        activeSubView = 1
                    }
                )
                showAddDialog = false
                Toast.makeText(context, "تمت إضافة الامتحان بنجاح 👍", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Exam Dialog
    examToEdit?.let { exam ->
        ExamFormDialog(
            exam = exam,
            groups = groups,
            onDismiss = { examToEdit = null },
            onConfirm = { name, totalMarks, date, groupId, groupName ->
                viewModel.updateExam(
                    exam.copy(
                        name = name,
                        totalMarks = totalMarks,
                        date = date,
                        groupId = groupId,
                        groupName = groupName
                    )
                )
                examToEdit = null
                Toast.makeText(context, "تم تعديل بيانات الامتحان بنجاح 👍", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Exam Dialog
    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = {
                Text(
                    text = "حذف الامتحان",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "تحذير: سيتم حذف امتحان \"${exam.name}\" وجميع درجات الطلاب المسجلة له بشكل نهائي. هل تريد الاستمرار بالفعل؟",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExam(exam.id)
                        examToDelete = null
                        Toast.makeText(context, "تم حذف الامتحان والدرجات الملحقة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("نعم، حذف بالكامل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { examToDelete = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun ExamCard(
    exam: Exam,
    gradesCount: Int,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRecordGradesClick: () -> Unit,
    onViewStatsClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ar")) }

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
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = exam.groupName.ifEmpty { "غير محدد" },
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
                text = exam.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(14.dp))
                    Text(text = "الدرجة الكلية: ${exam.totalMarks}", fontSize = 12.sp, color = TextSecondary)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.People, contentDescription = null, tint = SuccessColorLight, modifier = Modifier.size(14.dp))
                    Text(text = "المسجلين: $gradesCount طالباً", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                Text(text = "تاريخ الامتحان: ${sdf.format(Date(exam.date))}", fontSize = 11.sp, color = TextTertiary)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = BorderColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRecordGradesClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.BorderColor, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("رصد الدرجات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onViewStatsClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessColorLight),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SuccessColorLight.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("التقارير والإحصائيات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeRecordingView(
    exam: Exam,
    allStudents: List<Student>,
    allGrades: List<ExamGrade>,
    onBackClick: () -> Unit,
    onSaveGrades: (List<ExamGrade>) -> Unit
) {
    val context = LocalContext.current

    // Filter students belonging to this group
    val groupStudents = remember(allStudents, exam.groupId) {
        allStudents.filter { it.groupId == exam.groupId }
    }

    // Load existing grades mapping
    val existingGradesMap = remember(allGrades, exam.id) {
        allGrades.filter { it.examId == exam.id }.associateBy { it.studentId }
    }

    // Keep state of students scores
    val studentScores = remember(groupStudents, existingGradesMap) {
        val map = mutableStateMapOf<String, String>()
        groupStudents.forEach { s ->
            val existing = existingGradesMap[s.id]
            map[s.id] = existing?.score?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
        }
        map
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "تسجيل ورصد درجات الطلاب",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${exam.name} - الدرجة الكلية: ${exam.totalMarks}",
                            fontSize = 11.sp,
                            color = PrimaryIndigoLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
                actions = {
                    Spacer(modifier = Modifier.width(48.dp)) // balance title centering
                }
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            if (groupStudents.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.PeopleOutline, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Text("لا يوجد طلاب مسجلين في هذه المجموعة حالياً!", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(groupStudents, key = { it.id }) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = student.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (student.notes.isNotEmpty()) {
                                        Text(text = student.notes, fontSize = 11.sp, color = TextTertiary)
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = studentScores[student.id] ?: "",
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                                studentScores[student.id] = input
                                            }
                                        },
                                        placeholder = { Text("-", color = TextTertiary, textAlign = TextAlign.Center) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(80.dp),
                                        textStyle = LocalTextStyle.current.copy(
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = SurfaceLightDark,
                                            unfocusedContainerColor = SurfaceLightDark,
                                            focusedBorderColor = PrimaryIndigo,
                                            unfocusedBorderColor = BorderColor
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )

                                    Text(
                                        text = "/ ${exam.totalMarks}",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Save Button
                Button(
                    onClick = {
                        val finalGrades = mutableListOf<ExamGrade>()
                        var hasError = false
                        for (student in groupStudents) {
                            val input = studentScores[student.id] ?: ""
                            if (input.isNotEmpty()) {
                                val score = input.toDoubleOrNull() ?: 0.0
                                if (score > exam.totalMarks) {
                                    hasError = true
                                    break
                                }
                                val existingId = existingGradesMap[student.id]?.id ?: UUID.randomUUID().toString()
                                finalGrades.add(
                                    ExamGrade(
                                        id = existingId,
                                        examId = exam.id,
                                        studentId = student.id,
                                        studentName = student.name,
                                        score = score
                                    )
                                )
                            }
                        }

                        if (hasError) {
                            Toast.makeText(context, "الرجاء التأكد أن درجة الطالب لا تتجاوز الدرجة الكلية للأمتحان ⚠️", Toast.LENGTH_LONG).show()
                        } else {
                            onSaveGrades(finalGrades)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp, top = 8.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Text("حفظ الدرجات المدخلة", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamStatisticsView(
    exam: Exam,
    allStudents: List<Student>,
    allGrades: List<ExamGrade>,
    onBackClick: () -> Unit
) {
    // Filter grades for this exam
    val examGrades = remember(allGrades, exam.id) {
        allGrades.filter { it.examId == exam.id }
    }

    val totalGraded = examGrades.size
    val totalStudentsInGroup = allStudents.count { it.groupId == exam.groupId }

    // Computations
    val avgScore = remember(examGrades) {
        if (examGrades.isEmpty()) 0.0 else examGrades.map { it.score }.sum() / examGrades.size
    }

    val successRate = remember(examGrades, exam.totalMarks) {
        if (examGrades.isEmpty()) 0.0 else {
            val passScore = exam.totalMarks / 2.0
            val passingCount = examGrades.count { it.score >= passScore }
            (passingCount.toDouble() / examGrades.size) * 100.0
        }
    }

    val rankedStudents = remember(examGrades) {
        examGrades.sortedByDescending { it.score }
    }

    val bestPerforming = remember(rankedStudents) {
        rankedStudents.take(3)
    }

    val df = remember { DecimalFormat("##.#") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تقارير إحصائيات الامتحان",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Exam Title Summary Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = exam.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigoLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "المجموعة: ${exam.groupName.ifEmpty { "غير محددة" }} | الدرجة الكلية للورقة: ${exam.totalMarks}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "إجمالي من رصدت درجاتهم: $totalGraded من أصل $totalStudentsInGroup طالب بالمجموعة.",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }
            }

            if (examGrades.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.InsertChartOutlined, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Text("لم يتم إدخال أي درجات بعد لاستعراض الإحصائيات ⚠️", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // Key Stats row (Average, Success rate)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Average Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryIndigo.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Percent, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("متوسط الدرجات", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${df.format(avgScore)} / ${exam.totalMarks}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Pass Rate Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val isHighPass = successRate >= 50.0
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background((if (isHighPass) SuccessColor else ErrorColor).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isHighPass) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isHighPass) SuccessColorLight else ErrorColorLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("نسبة النجاح (50%+)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${df.format(successRate)}%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHighPass) SuccessColorLight else ErrorColorLight
                                )
                            }
                        }
                    }
                }

                // Best Performing Students Card (Top 3)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                                Text("أفضل الطلاب أداءً في الامتحان 🏆", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            bestPerforming.forEachIndexed { index, grade ->
                                val medalColor = when (index) {
                                    0 -> Color(0xFFFFD700) // Gold
                                    1 -> Color(0xFFC0C0C0) // Silver
                                    else -> Color(0xFFCD7F32) // Bronze
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(medalColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(text = grade.studentName, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    }

                                    Text(
                                        text = "${grade.score} درجة",
                                        color = SuccessColorLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                if (index < bestPerforming.size - 1) {
                                    Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                // Overall Leaderboard (ترتيب جميع الطلاب)
                item {
                    Text(
                        text = "جدول الترتيب والدرجات لكامل الطلاب (${rankedStudents.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                itemsIndexed(rankedStudents) { index, grade ->
                    val isPass = grade.score >= (exam.totalMarks / 2.0)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Rank Number
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceLightDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = grade.studentName,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${grade.score} / ${exam.totalMarks}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPass) SuccessColorLight else ErrorColorLight
                                )

                                Surface(
                                    color = (if (isPass) SuccessColor else ErrorColor).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isPass) "ناجح" else "راسب",
                                        color = if (isPass) SuccessColorLight else ErrorColorLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamFormDialog(
    exam: Exam? = null,
    groups: List<Group>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, totalMarks: Double, date: Long, groupId: String, groupName: String) -> Unit
) {
    var name by remember { mutableStateOf(exam?.name ?: "") }
    var totalMarksInput by remember { mutableStateOf(exam?.totalMarks?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }

    // Group Selection
    var selectedGroupId by remember { mutableStateOf(exam?.groupId ?: "") }
    var selectedGroupName by remember { mutableStateOf(exam?.groupName ?: "") }
    var showGroupDropdown by remember { mutableStateOf(false) }

    // Date selection
    val calendar = remember {
        Calendar.getInstance().apply {
            if (exam != null) {
                timeInMillis = exam.date
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
                text = if (exam == null) "إضافة امتحان جديد" else "تعديل بيانات الامتحان",
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الامتحان") },
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
                    value = totalMarksInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null) {
                            totalMarksInput = input
                        }
                    },
                    label = { Text("الدرجة الكلية للامتحان") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                // Group Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedGroupName.ifEmpty { "اختر المجموعة" },
                        onValueChange = {},
                        label = { Text("المجموعة المسؤولة") },
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
                                text = { Text("لا توجد مجموعات مسجلة! قم بإنشاء مجموعة أولاً", color = ErrorColorLight) },
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

                // Date Picker trigger
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
                            text = "تاريخ الامتحان: $day/${month + 1}/$year",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            val totalMarks = totalMarksInput.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (name.isBlank() || selectedGroupId.isBlank() || totalMarks <= 0.0) {
                        return@Button
                    }
                    val selectedCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    onConfirm(name, totalMarks, selectedCal.timeInMillis, selectedGroupId, selectedGroupName)
                },
                enabled = name.isNotBlank() && selectedGroupId.isNotBlank() && totalMarks > 0.0,
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
