package com.example.ui.center

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import com.example.util.StudentAnalyticsEngine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationCenterScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Send, 1: Templates, 2: History

    val students by viewModel.students.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val templates by viewModel.messageTemplates.collectAsStateWithLifecycle()
    val logs by viewModel.communicationLogs.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val grades by viewModel.examGrades.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مركز الاتصالات الموحد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
                modifier = Modifier.border(0.5.dp, BorderColor)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs Row
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = SurfaceDark,
                contentColor = PrimaryIndigo,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = PrimaryIndigoLight
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("إرسال رسالة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = PrimaryIndigoLight,
                    unselectedContentColor = TextTertiary
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.FileCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("القوالب الجاهزة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = PrimaryIndigoLight,
                    unselectedContentColor = TextTertiary
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("سجل الإرسال", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = PrimaryIndigoLight,
                    unselectedContentColor = TextTertiary
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "CommTabTransition"
            ) { tab ->
                when (tab) {
                    0 -> SendMessageTab(
                        students = students,
                        groups = groups,
                        templates = templates,
                        attendance = attendance,
                        payments = payments,
                        exams = exams,
                        grades = grades,
                        viewModel = viewModel
                    )
                    1 -> TemplatesTab(
                        templates = templates,
                        onAddTemplate = { title, channel, category, content ->
                            viewModel.addMessageTemplate(title, channel, category, content)
                        },
                        onDeleteTemplate = { id ->
                            viewModel.deleteMessageTemplate(id)
                        }
                    )
                    2 -> HistoryLogTab(
                        logs = logs,
                        onClearLogs = { viewModel.clearCommunicationLogs() },
                        onDeleteLog = { id -> viewModel.deleteCommunicationLog(id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMessageTab(
    students: List<Student>,
    groups: List<Group>,
    templates: List<MessageTemplate>,
    attendance: List<Attendance>,
    payments: List<Payment>,
    exams: List<Exam>,
    grades: List<ExamGrade>,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val activeStudents = remember(students) { students.filter { !it.isArchived } }

    var selectedChannel by remember { mutableStateOf("WhatsApp") } // WhatsApp, SMS, Email, Notification
    var targetType by remember { mutableStateOf("individual") } // individual, group, grade, late_payment

    var selectedStudentId by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf("") }

    var selectedTemplateId by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }

    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var templateDropdownExpanded by remember { mutableStateOf(false) }

    // Bulk dispatching dialog state
    var bulkDispatchList by remember { mutableStateOf<List<BulkRecipient>>(emptyList()) }
    var showBulkDispatchDialog by remember { mutableStateOf(false) }

    // Grades options
    val gradeOptions = remember(activeStudents) {
        activeStudents.map { it.grade }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    // Populate template text on selection
    LaunchedEffect(selectedTemplateId) {
        val tmpl = templates.find { it.id == selectedTemplateId }
        if (tmpl != null) {
            messageText = tmpl.content
            selectedChannel = tmpl.channel
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Channel Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "قناة الاتصال المفضلة",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChannelOptionButton(
                            title = "WhatsApp",
                            icon = Icons.Default.Chat,
                            isSelected = selectedChannel == "WhatsApp",
                            color = SuccessColor,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedChannel = "WhatsApp" }
                        )
                        ChannelOptionButton(
                            title = "SMS",
                            icon = Icons.Default.Sms,
                            isSelected = selectedChannel == "SMS",
                            color = PrimaryIndigoLight,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedChannel = "SMS" }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChannelOptionButton(
                            title = "البريد الإلكتروني",
                            icon = Icons.Default.Email,
                            isSelected = selectedChannel == "Email",
                            color = WarningColor,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedChannel = "Email" }
                        )
                        ChannelOptionButton(
                            title = "التنبيهات",
                            icon = Icons.Default.Notifications,
                            isSelected = selectedChannel == "Notification",
                            color = ErrorColorLight,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedChannel = "Notification" }
                        )
                    }
                }
            }
        }

        // Target Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "المستهدفون من الرسالة",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Target Type selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLightDark)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TargetTypeTab(
                            title = "طالب فردي",
                            isSelected = targetType == "individual",
                            modifier = Modifier.weight(1f),
                            onClick = { targetType = "individual" }
                        )
                        TargetTypeTab(
                            title = "مجموعة",
                            isSelected = targetType == "group",
                            modifier = Modifier.weight(1f),
                            onClick = { targetType = "group" }
                        )
                        TargetTypeTab(
                            title = "صف دراسي",
                            isSelected = targetType == "grade",
                            modifier = Modifier.weight(1f),
                            onClick = { targetType = "grade" }
                        )
                        TargetTypeTab(
                            title = "متأخرات مالية",
                            isSelected = targetType == "late_payment",
                            modifier = Modifier.weight(1.2f),
                            onClick = { targetType = "late_payment" }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic inputs based on target
                    when (targetType) {
                        "individual" -> {
                            val currentStudentName = activeStudents.find { it.id == selectedStudentId }?.name ?: "اختر الطالب المستهدف"
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { studentDropdownExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(currentStudentName, color = TextPrimary)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                                DropdownMenu(
                                    expanded = studentDropdownExpanded,
                                    onDismissRequest = { studentDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(SurfaceDark)
                                        .fillMaxWidth(0.9f)
                                ) {
                                    activeStudents.forEach { std ->
                                        DropdownMenuItem(
                                            text = { Text("${std.name} (${std.grade})", color = TextPrimary) },
                                            onClick = {
                                                selectedStudentId = std.id
                                                studentDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        "group" -> {
                            val currentGroupName = groups.find { it.id == selectedGroupId }?.name ?: "اختر المجموعة المستهدفة"
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { groupDropdownExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(currentGroupName, color = TextPrimary)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                                DropdownMenu(
                                    expanded = groupDropdownExpanded,
                                    onDismissRequest = { groupDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(SurfaceDark)
                                        .fillMaxWidth(0.9f)
                                ) {
                                    groups.forEach { grp ->
                                        DropdownMenuItem(
                                            text = { Text(grp.name, color = TextPrimary) },
                                            onClick = {
                                                selectedGroupId = grp.id
                                                groupDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        "grade" -> {
                            val currentGradeName = if (selectedGrade.isEmpty()) "اختر الصف الدراسي" else selectedGrade
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { gradeDropdownExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(currentGradeName, color = TextPrimary)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                                DropdownMenu(
                                    expanded = gradeDropdownExpanded,
                                    onDismissRequest = { gradeDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(SurfaceDark)
                                        .fillMaxWidth(0.9f)
                                ) {
                                    if (gradeOptions.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("لا توجد صفوف دراسية مسجلة للطلاب", color = TextSecondary) },
                                            onClick = { gradeDropdownExpanded = false }
                                        )
                                    } else {
                                        gradeOptions.forEach { grd ->
                                            DropdownMenuItem(
                                                text = { Text(grd, color = TextPrimary) },
                                                onClick = {
                                                    selectedGrade = grd
                                                    gradeDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "late_payment" -> {
                            val lateCount = remember(activeStudents, payments) {
                                activeStudents.count { std ->
                                    val analytics = StudentAnalyticsEngine.calculateAnalytics(std, attendance, payments, emptyList(), grades)
                                    !std.isExempt && analytics.unpaidMonthsCount >= 1
                                }
                            }
                            Text(
                                text = "سيتم إرسال الرسالة إلى جميع الطلاب غير المعفيين والذين لديهم متأخرات مالية تبلغ شهراً واحداً أو أكثر.\n(عدد الطلاب المستحقين حالياً: $lateCount طالباً)",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Template Selection & Compose
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "محتوى الرسالة والقوالب",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Template picker
                    val currentTemplateName = templates.find { it.id == selectedTemplateId }?.title ?: "اختر قالباً جاهزاً لتعبئة سريعة"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { templateDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentTemplateName, color = TextPrimary, maxLines = 1)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                            }
                        }
                        DropdownMenu(
                            expanded = templateDropdownExpanded,
                            onDismissRequest = { templateDropdownExpanded = false },
                            modifier = Modifier
                                .background(SurfaceDark)
                                .fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون قالب (كتابة رسالة مخصصة)", color = WarningColor) },
                                onClick = {
                                    selectedTemplateId = ""
                                    messageText = ""
                                    templateDropdownExpanded = false
                                }
                            )
                            templates.forEach { tmpl ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val badgeColor = when (tmpl.channel) {
                                                "WhatsApp" -> SuccessColor
                                                "SMS" -> PrimaryIndigoLight
                                                "Email" -> WarningColor
                                                else -> ErrorColorLight
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(badgeColor)
                                            )
                                            Text(tmpl.title, color = TextPrimary)
                                        }
                                    },
                                    onClick = {
                                        selectedTemplateId = tmpl.id
                                        templateDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Guide for tags
                    Text(
                        text = "الكلمات المفتاحية المدعومة: \n{student_name} (اسم الطالب) • {parent_name} (ولي الأمر) • {amount} (المبلغ) • {month} (الشهر) • {subject} (المادة) • {exam_name} (الامتحان) • {score} (الدرجة)",
                        fontSize = 11.sp,
                        color = WarningColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Compose text field
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("صياغة الرسالة") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = PrimaryIndigoLight,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceLightDark,
                            unfocusedContainerColor = SurfaceLightDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Real-time Preview Card
                    if (messageText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "معاينة تجريبية للرسالة:",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceLightDark)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            val previewText = remember(messageText) {
                                replacePlaceholders(
                                    messageText,
                                    "أحمد محمد",
                                    "محمد سليمان",
                                    "150",
                                    "يونيو",
                                    "الفيزياء",
                                    "امتحان الشهر",
                                    "18",
                                    "20"
                                )
                            }
                            Text(
                                text = previewText,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Action Trigger
        item {
            Button(
                onClick = {
                    if (messageText.isBlank()) {
                        Toast.makeText(context, "الرجاء كتابة نص الرسالة أولاً", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Collect recipients
                    val targetRecipients = mutableListOf<BulkRecipient>()
                    when (targetType) {
                        "individual" -> {
                            val student = activeStudents.find { it.id == selectedStudentId }
                            if (student == null) {
                                Toast.makeText(context, "الرجاء اختيار الطالب أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            targetRecipients.add(getStudentRecipientData(student, attendance, payments, grades))
                        }
                        "group" -> {
                            if (selectedGroupId.isEmpty()) {
                                Toast.makeText(context, "الرجاء اختيار المجموعة أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val groupStudents = activeStudents.filter { it.groupId == selectedGroupId }
                            if (groupStudents.isEmpty()) {
                                Toast.makeText(context, "لا يوجد طلاب في هذه المجموعة حالياً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            groupStudents.forEach { std ->
                                targetRecipients.add(getStudentRecipientData(std, attendance, payments, grades))
                            }
                        }
                        "grade" -> {
                            if (selectedGrade.isEmpty()) {
                                Toast.makeText(context, "الرجاء اختيار الصف الدراسي أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val gradeStudents = activeStudents.filter { it.grade == selectedGrade }
                            if (gradeStudents.isEmpty()) {
                                Toast.makeText(context, "لا يوجد طلاب في هذا الصف الدراسي حالياً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            gradeStudents.forEach { std ->
                                targetRecipients.add(getStudentRecipientData(std, attendance, payments, grades))
                            }
                        }
                        "late_payment" -> {
                            val lateStudents = activeStudents.filter { std ->
                                val analytics = StudentAnalyticsEngine.calculateAnalytics(std, attendance, payments, emptyList(), grades)
                                !std.isExempt && analytics.unpaidMonthsCount >= 1
                            }
                            if (lateStudents.isEmpty()) {
                                Toast.makeText(context, "الحمد لله، لا يوجد متأخرات مالية مستحقة لدى أي من الطلاب حالياً!", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            lateStudents.forEach { std ->
                                targetRecipients.add(getStudentRecipientData(std, attendance, payments, grades))
                            }
                        }
                    }

                    // Set message previews on recipients
                    val finalizedRecipients = targetRecipients.map { recipient ->
                        recipient.copy(
                            finalText = replacePlaceholders(
                                messageText,
                                recipient.studentName,
                                recipient.parentName,
                                recipient.feeNet,
                                recipient.lateMonth,
                                recipient.lastSubject,
                                recipient.lastExamName,
                                recipient.lastExamScore,
                                recipient.lastExamTotal
                            )
                        )
                    }

                    if (finalizedRecipients.isEmpty()) return@Button

                    if (selectedChannel == "Notification") {
                        // Notifications can be executed fully automatically
                        finalizedRecipients.forEach { rec ->
                            viewModel.addCommunicationLog(
                                studentId = rec.studentId,
                                studentName = rec.studentName,
                                recipient = rec.parentPhone.ifBlank { rec.studentPhone },
                                channel = selectedChannel,
                                message = rec.finalText,
                                status = "SUCCESS"
                            )
                        }
                        Toast.makeText(context, "تم إرسال ${finalizedRecipients.size} إشعار بنجاح وحفظهم في السجل!", Toast.LENGTH_LONG).show()
                        messageText = ""
                        selectedTemplateId = ""
                    } else {
                        // Open Bulk Interactive Dispatcher Dialog
                        bulkDispatchList = finalizedRecipients
                        showBulkDispatchDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("send_communication_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إرسال الآن", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    // Interactive Bulk Dispatcher Dialog
    if (showBulkDispatchDialog) {
        var completedCount by remember { mutableStateOf(0) }
        val dispatcherList = remember { mutableStateListOf<BulkRecipient>().apply { addAll(bulkDispatchList) } }

        AlertDialog(
            onDismissRequest = { showBulkDispatchDialog = false },
            title = {
                Text(
                    text = "مرسل الرسائل التفاعلي (${dispatcherList.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "نظراً لسياسات الأمان في الأجهزة، يرجى فتح الرسالة وإرسالها لكل مستلم يدوياً. اضغط على زر الإرسال لتوجيهك فوراً:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dispatcherList) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (item.isSent) SurfaceLightDark else SurfaceDark)
                                    .border(1.dp, if (item.isSent) SuccessColor.copy(alpha = 0.5f) else BorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.studentName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(if (item.parentPhone.isNotEmpty()) item.parentPhone else item.studentPhone, fontSize = 10.sp, color = TextSecondary)
                                }

                                if (item.isSent) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(16.dp))
                                        Text("تم", color = SuccessColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            val phone = if (item.parentPhone.isNotEmpty()) item.parentPhone else item.studentPhone
                                            val mail = item.studentEmail

                                            when (selectedChannel) {
                                                "WhatsApp" -> {
                                                    sendWhatsAppMessage(context, phone, item.finalText)
                                                }
                                                "SMS" -> {
                                                    sendSmsMessage(context, phone, item.finalText)
                                                }
                                                "Email" -> {
                                                    sendEmailMessage(context, mail.ifBlank { "test@example.com" }, "إشعار من المركز التعليمي", item.finalText)
                                                }
                                            }

                                            // Mark as sent in memory
                                            val index = dispatcherList.indexOf(item)
                                            if (index != -1) {
                                                dispatcherList[index] = item.copy(isSent = true)
                                            }

                                            // Log to DB
                                            viewModel.addCommunicationLog(
                                                studentId = item.studentId,
                                                studentName = item.studentName,
                                                recipient = if (selectedChannel == "Email") mail else phone,
                                                channel = selectedChannel,
                                                message = item.finalText,
                                                status = "SUCCESS"
                                            )

                                            completedCount = dispatcherList.count { it.isSent }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إرسال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBulkDispatchDialog = false
                        Toast.makeText(context, "تم إرسال وحفظ سجلات الرسائل المكتملة!", Toast.LENGTH_SHORT).show()
                        messageText = ""
                        selectedTemplateId = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                ) {
                    Text("تم إنهاء العملية", color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesTab(
    templates: List<MessageTemplate>,
    onAddTemplate: (String, String, String, String) -> Unit,
    onDeleteTemplate: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryIndigo,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "قالب جديد")
            }
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (templates.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FileCopy, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("لا توجد قوالب جاهزة حالياً", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(templates) { tmpl ->
                    TemplateRowItem(template = tmpl, onDelete = { onDeleteTemplate(tmpl.id) })
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var channel by remember { mutableStateOf("WhatsApp") } // WhatsApp, SMS, Email, Notification
        var category by remember { mutableStateOf("custom") } // attendance, fees, exam, homework, custom
        var content by remember { mutableStateOf("") }

        var categoryExpanded by remember { mutableStateOf(false) }
        var channelExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("إنشاء قالب رسالة جديد", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم القالب *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Channel selector
                    Text("قناة الإرسال:", color = TextSecondary, fontSize = 12.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { channelExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(channel, color = TextPrimary)
                        }
                        DropdownMenu(
                            expanded = channelExpanded,
                            onDismissRequest = { channelExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            listOf("WhatsApp", "SMS", "Email", "Notification").forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text(ch, color = TextPrimary) },
                                    onClick = {
                                        channel = ch
                                        channelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Category Selector
                    Text("نوع الإشعار:", color = TextSecondary, fontSize = 12.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { categoryExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val categoryText = when (category) {
                                "attendance" -> "إشعار غياب طالب"
                                "fees" -> "إشعار رسوم ومستحقات مادية"
                                "exam" -> "إشعار نتيجة امتحان"
                                "homework" -> "إشعار واجب مدرسي"
                                else -> "رسالة مخصصة عامة"
                            }
                            Text(categoryText, color = TextPrimary)
                        }
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            listOf(
                                "attendance" to "إشعار غياب طالب",
                                "fees" to "إشعار رسوم ومستحقات مادية",
                                "exam" to "إشعار نتيجة امتحان",
                                "homework" to "إشعار واجب مدرسي",
                                "custom" to "رسالة مخصصة عامة"
                            ).forEach { (catKey, catValue) ->
                                DropdownMenuItem(
                                    text = { Text(catValue, color = TextPrimary) },
                                    onClick = {
                                        category = catKey
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Content Compose
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("صياغة القالب الافتراضي *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotEmpty() && content.isNotEmpty()) {
                            onAddTemplate(title, channel, category, content)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("إضافة القالب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun TemplateRowItem(
    template: MessageTemplate,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        BorderColor,
                        PrimaryIndigo.copy(alpha = 0.2f),
                        BorderColor
                    )
                ),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (badgeColor, badgeText) = when (template.channel) {
                        "WhatsApp" -> SuccessColor to "WhatsApp"
                        "SMS" -> PrimaryIndigoLight to "SMS"
                        "Email" -> WarningColor to "Email"
                        else -> ErrorColorLight to "Notification"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    val catText = when (template.category) {
                        "attendance" -> "إشعار غياب"
                        "fees" -> "إشعار رسوم"
                        "exam" -> "إشعار امتحان"
                        "homework" -> "إشعار واجب"
                        else -> "مخصص"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(catText, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorColor.copy(alpha = 0.08f))
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف القالب", tint = ErrorColor, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = template.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = template.content,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun HistoryLogTab(
    logs: List<CommunicationLog>,
    onClearLogs: () -> Unit,
    onDeleteLog: (String) -> Unit
) {
    var searchKeyword by remember { mutableStateOf("") }
    val filteredLogs = remember(logs, searchKeyword) {
        if (searchKeyword.isBlank()) logs else {
            logs.filter {
                it.studentName.contains(searchKeyword, ignoreCase = true) ||
                        it.message.contains(searchKeyword, ignoreCase = true) ||
                        it.recipient.contains(searchKeyword)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Clear Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                placeholder = { Text("ابحث باسم الطالب أو نص الرسالة...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextSecondary) }
            )

            if (logs.isNotEmpty()) {
                Button(
                    onClick = onClearLogs,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor.copy(alpha = 0.2f), contentColor = ErrorColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("مسح السجل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("لا توجد رسائل مرسلة مسجلة في التاريخ حالياً", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(filteredLogs) { log ->
                    CommunicationHistoryRowItem(log = log, onDelete = { onDeleteLog(log.id) })
                }
            }
        }
    }
}

@Composable
fun CommunicationHistoryRowItem(
    log: CommunicationLog,
    onDelete: () -> Unit
) {
    val dateStr = remember(log.sentAt) {
        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault())
        sdf.format(Date(log.sentAt))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        BorderColor,
                        PrimaryIndigo.copy(alpha = 0.2f),
                        BorderColor
                    )
                ),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (log.studentName.isNotEmpty()) log.studentName else "رسالة عامة / مخصص",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${log.recipient} • $dateStr",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (badgeColor, badgeText) = when (log.channel) {
                        "WhatsApp" -> SuccessColor to "WhatsApp"
                        "SMS" -> PrimaryIndigoLight to "SMS"
                        "Email" -> WarningColor to "Email"
                        else -> ErrorColorLight to "Notification"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ErrorColor.copy(alpha = 0.08f))
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = ErrorColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(12.dp)
            ) {
                Text(
                    text = log.message,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ------------------ HELPER SUB-COMPONENTS & MATHS ------------------

@Composable
fun ChannelOptionButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (isSelected) color else BorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) color else TextSecondary, modifier = Modifier.size(20.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) color else TextSecondary)
        }
    }
}

@Composable
fun TargetTypeTab(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryIndigo else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else TextTertiary
        )
    }
}

// Data class to represent finalized recipient information for placeholder replacement
data class BulkRecipient(
    val studentId: String,
    val studentName: String,
    val parentName: String,
    val parentPhone: String,
    val studentPhone: String,
    val studentEmail: String,
    val feeNet: String,
    val lateMonth: String,
    val lastSubject: String,
    val lastExamName: String,
    val lastExamScore: String,
    val lastExamTotal: String,
    val finalText: String = "",
    val isSent: Boolean = false
)

fun getStudentRecipientData(
    student: Student,
    attendanceList: List<Attendance>,
    paymentList: List<Payment>,
    examGradesList: List<ExamGrade>
): BulkRecipient {
    // Basic formatting
    val studentId = student.id
    val name = student.name
    val parentName = student.parentName.ifBlank { "ولي الأمر" }
    val parentPhone = student.parentPhone.ifBlank { student.studentPhone }
    val studentPhone = student.studentPhone
    val studentEmail = "" // Or some fallback

    // Financials
    val netFee = student.netFee
    val feeNetStr = String.format(Locale.getDefault(), "%.1f", netFee)

    // Unpaid/Late month calculation (using current month as fallback)
    val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val paidForThisMonth = paymentList.any { it.studentId == studentId && it.month == currentMonthStr }
    val lateMonthStr = if (paidForThisMonth) "تم السداد" else currentMonthStr

    // Last Exam/Grades
    val studentGrades = examGradesList.filter { it.studentId == studentId }
    val lastGrade = studentGrades.maxByOrNull { it.createdAt }
    val lastScoreStr = lastGrade?.score?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "غير متوفر"
    val lastExamTotalStr = "20" // default total score
    val lastExamNameStr = lastGrade?.notes?.ifBlank { "امتحان المتابعة" } ?: "امتحان المتابعة"

    // Subject/Teacher context
    val lastSubjectStr = student.customCourse.ifBlank { "المادة المقررة" }

    return BulkRecipient(
        studentId = studentId,
        studentName = name,
        parentName = parentName,
        parentPhone = parentPhone,
        studentPhone = studentPhone,
        studentEmail = studentEmail,
        feeNet = feeNetStr,
        lateMonth = lateMonthStr,
        lastSubject = lastSubjectStr,
        lastExamName = lastExamNameStr,
        lastExamScore = lastScoreStr,
        lastExamTotal = lastExamTotalStr
    )
}

// Replace template placeholders dynamically
fun replacePlaceholders(
    template: String,
    studentName: String,
    parentName: String,
    amount: String,
    month: String,
    subject: String,
    examName: String,
    score: String,
    totalMarks: String
): String {
    return template
        .replace("{student_name}", studentName)
        .replace("{parent_name}", parentName)
        .replace("{amount}", amount)
        .replace("{month}", month)
        .replace("{subject}", subject)
        .replace("{exam_name}", examName)
        .replace("{score}", score)
        .replace("{total_marks}", totalMarks)
}

// Action intent helper for SMS
fun sendSmsMessage(context: Context, phone: String, message: String) {
    try {
        val uri = Uri.parse("smsto:$phone")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق الرسائل النصية القصيرة", Toast.LENGTH_SHORT).show()
    }
}

// Action intent helper for Email
fun sendEmailMessage(context: Context, email: String, subject: String, body: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق البريد الإلكتروني", Toast.LENGTH_SHORT).show()
    }
}
