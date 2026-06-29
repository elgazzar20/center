package com.example.ui.reports

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import com.example.util.agent.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe AppViewModel StateFlows for real-time local analytics
    val students by viewModel.students.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val examGrades by viewModel.examGrades.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    val currency = profile?.currency ?: "ج.م"

    // Context manager states
    val chatMessages by AiContextManager.messages.collectAsStateWithLifecycle()
    val pendingAction by AiContextManager.pendingAction.collectAsStateWithLifecycle()
    val currentProvider by AiContextManager.currentProvider.collectAsStateWithLifecycle()
    val isThinkingModeEnabled by AiContextManager.isThinkingModeEnabled.collectAsStateWithLifecycle()

    var inputQuery by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingTimer by remember { mutableStateOf(0) }

    // Proactive recommendations state
    val recommendations = remember(students, attendance, payments, exams, examGrades) {
        AiRecommendationEngine.generateRecommendations(students, attendance, payments, exams, examGrades)
    }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom of chat when messages count or thinking state changes
    LaunchedEffect(chatMessages.size, isThinking) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Speech Recording timer simulation
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingTimer = 0
            while (isRecordingVoice && recordingTimer < 10) {
                delay(1000)
                recordingTimer++
            }
            if (isRecordingVoice) {
                isRecordingVoice = false
                // Auto-submit a random voice command simulation!
                val simulatedVoiceCommands = listOf(
                    "سجل غياب أحمد محمد اليوم",
                    "أضف طالب جديد اسمه محمد أحمد في الصف الثاني الثانوي",
                    "اعرض الطلاب المتأخرين في الدفع",
                    "أنشئ تقريراً مالياً لهذا الشهر"
                )
                val picked = simulatedVoiceCommands.random()
                inputQuery = picked
                Toast.makeText(context, "🎤 تم التعرف على الصوت: \"$picked\"", Toast.LENGTH_LONG).show()
            }
        }
    }

    val onSendAgentMessage = { queryText: String ->
        if (queryText.isNotBlank()) {
            // 1. Add user message
            AiContextManager.addMessage("user", queryText)
            AiContextManager.saveMessageHistory(context)
            val currentText = queryText
            inputQuery = ""
            keyboardController?.hide()

            // 2. Trigger thinking simulation
            isThinking = true

            coroutineScope.launch {
                try {
                    // 3. Process via AiAgentEngine
                    val result = AiAgentEngine.processQuery(
                        query = currentText,
                        viewModel = viewModel,
                        students = students,
                        attendance = attendance,
                        payments = payments,
                        exams = exams,
                        examGrades = examGrades,
                        expenses = expenses,
                        groups = groups,
                        teachers = teachers,
                        currency = currency
                    )

                    // 4. Extract reply text
                    val replyText = when (result) {
                        is AgentResult.Success -> result.message
                        is AgentResult.Error -> result.message
                        is AgentResult.ConfirmationRequired -> result.message
                        is AgentResult.ReportResult -> "تم توليد تقرير بنجاح! يمكنكم تصفحه كجدول تفاعلي أو ملف PDF."
                        is AgentResult.AnalyticsResult -> "إليك تحليل الأرقام والبيانات الإحصائية المطلوبة:"
                        is AgentResult.RiskReportResult -> "تم توليد تقرير كشف المخاطر والمستويات بنجاح:"
                    }

                    isThinking = false
                    
                    // Create agent message with empty text and stream characters for fluid ChatGPT-style experience
                    val messageId = java.util.UUID.randomUUID().toString()
                    AiContextManager.addMessageWithId(messageId, "agent", "", result)
                    
                    var currentTextStream = ""
                    for (char in replyText) {
                        currentTextStream += char
                        AiContextManager.updateMessageText(messageId, currentTextStream)
                        delay(10) // Smooth character-by-character typing speed
                    }
                    AiContextManager.saveMessageHistory(context)
                } catch (e: Exception) {
                    isThinking = false
                    val messageId = java.util.UUID.randomUUID().toString()
                    val errorReply = "عذراً، حدث خطأ غير متوقع أثناء معالجة الأمر: ${e.localizedMessage}"
                    AiContextManager.addMessageWithId(messageId, "agent", "")
                    
                    var currentTextStream = ""
                    for (char in errorReply) {
                        currentTextStream += char
                        AiContextManager.updateMessageText(messageId, currentTextStream)
                        delay(10)
                    }
                    AiContextManager.saveMessageHistory(context)
                }
            }
        }
    }

    val voicePresetSuggestions = listOf(
        "أضف طالب جديد",
        "سجل غياب طالب",
        "تقرير مالي شهري",
        "كشف المخاطر"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Nexora AI Agent 🤖🔥",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SuccessColor)
                            )
                        }
                        Text(
                            text = "نظام الذكاء الاصطناعي المتكامل لإدارة السنتر والتحليلات الذكية",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("ai_agent_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        AiContextManager.clearChat(context)
                        Toast.makeText(context, "تم مسح المحادثة بنجاح!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "مسح المحادثة", tint = TextPrimary)
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
        ) {
            // Minimalist status bar displaying background-routed unified model Core
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeProviderName by AiProviderManager.activeProviderName.collectAsStateWithLifecycle()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SuccessColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "بوابة Nexora AI الموحدة نشطة 🔒",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessColorLight
                    )
                }
                Text(
                    text = activeProviderName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary
                )
            }

            // Proactive Smart Recommendations Panel (Scrollable alerts)
            if (recommendations.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TipsAndUpdates,
                                    contentDescription = "توصيات ذكية",
                                    tint = WarningColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "توصيات وتنبيهات ذكية تلقائية 💡",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryIndigo.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "تحليل حي",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigoLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recommendations.forEach { rec ->
                                Box(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceLightDark)
                                        .border(
                                            1.dp,
                                            if (rec.severity == "high") Color(0xFFEF4444).copy(alpha = 0.3f) else BorderColor,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = rec.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rec.severity == "high") ErrorColorLight else PrimaryIndigoLight
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = rec.description,
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            maxLines = 2,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chat Message History
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    AgentChatBubble(
                        message = msg,
                        currency = currency,
                        onConfirmClick = { onSendAgentMessage("نعم") },
                        onCancelClick = { onSendAgentMessage("لا") },
                        onRegenerateClick = {
                            val userMessages = chatMessages.filter { it.sender == "user" }
                            if (userMessages.isNotEmpty()) {
                                onSendAgentMessage(userMessages.last().text)
                            }
                        }
                    )
                }

                if (isThinking) {
                    item {
                        AgentThinkingBubble()
                    }
                }
            }

            // Voice Simulation status banner
            AnimatedVisibility(visible = isRecordingVoice) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD32F2F).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFD32F2F).copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جاري الاستماع للأمر الصوتي... (اضغط مجدداً للإيقاف)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "00:${String.format("%02d", recordingTimer)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Suggestions horizontal row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                voicePresetSuggestions.forEach { suggestion ->
                    val icon = when (suggestion) {
                        "أضف طالب جديد" -> Icons.Default.PersonAdd
                        "سجل غياب طالب" -> Icons.Default.CheckCircleOutline
                        "تقرير مالي شهري" -> Icons.Default.AccountBalanceWallet
                        "كشف المخاطر" -> Icons.Default.TrendingDown
                        else -> Icons.Default.Lightbulb
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                            .clickable {
                                when (suggestion) {
                                    "أضف طالب جديد" -> {
                                        inputQuery = "أضف طالب جديد اسمه محمد أحمد في الصف الثاني الثانوي"
                                    }
                                    "سجل غياب طالب" -> {
                                        inputQuery = "سجل غياب أحمد محمد اليوم"
                                    }
                                    "تقرير مالي شهري" -> {
                                        inputQuery = "أنشئ تقريراً مالياً لهذا الشهر"
                                    }
                                    "كشف المخاطر" -> {
                                        inputQuery = "اعرض تقييم الطلاب المعرضين للتراجع"
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("ai_preset_$suggestion"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = suggestion,
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = suggestion,
                            fontSize = 11.sp,
                            color = PrimaryIndigoLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Input Bar with Integrated Microphone Button for Voice Commands
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Microphone Voice Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecordingVoice) {
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                )
                            } else {
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(SurfaceLightDark, BackgroundDark)
                                )
                            }
                        )
                        .border(1.dp, if (isRecordingVoice) Color(0xFFF87171) else BorderColor, CircleShape)
                        .clickable {
                            isRecordingVoice = !isRecordingVoice
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecordingVoice) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "أمر صوتي",
                        tint = if (isRecordingVoice) Color.White else PrimaryIndigoLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = { Text("اكتب أمراً باللغة العربية للـ AI (مثال: أضف طالب...)", color = TextTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputQuery.isNotBlank()) {
                                onSendAgentMessage(inputQuery)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigoLight,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_input_text_field")
                )

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputQuery.isNotBlank()) {
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(PrimaryIndigoLight, PrimaryIndigo)
                                )
                            } else {
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(SurfaceLightDark, BackgroundDark)
                                )
                            }
                        )
                        .border(1.dp, if (inputQuery.isNotBlank()) PrimaryIndigoLight else BorderColor, CircleShape)
                        .clickable(enabled = inputQuery.isNotBlank()) {
                            onSendAgentMessage(inputQuery)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال",
                        tint = if (inputQuery.isNotBlank()) Color.White else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }


}

@Composable
fun AgentChatBubble(
    message: MessageLog,
    currency: String,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    onRegenerateClick: () -> Unit
) {
    val isUser = message.sender == "user"
    val textAlignment = if (isUser) TextAlign.Right else TextAlign.Left
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val annotatedString = androidx.compose.ui.text.buildAnnotatedString { append(message.text) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // AI Avatar with beautiful neon gradient border
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(PrimaryIndigo, PrimaryIndigoLight)
                        )
                    )
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Nexora AI",
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Speaker Name & Badge
            Text(
                text = if (isUser) "أنت" else "Nexora AI Agent 🤖",
                fontSize = 11.sp,
                color = if (isUser) PrimaryIndigoLight else TextTertiary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(PrimaryIndigo, PrimaryIndigoDark)
                            )
                        } else {
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(SurfaceDark, SurfaceLightDark)
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isUser) Color(0x33FAFAFA) else BorderColor,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
                    .widthIn(max = 285.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = textAlignment
                    )

                    // Render specific structures generated by AI Agent
                    message.result?.let { result ->
                        Spacer(modifier = Modifier.height(10.dp))
                        AgentStructuredResultCard(
                            result = result,
                            currency = currency,
                            onConfirmClick = onConfirmClick,
                            onCancelClick = onCancelClick
                        )
                    }
                }
            }

            // Copy and Regenerate mini action bar
            if (message.text.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = if (isUser) 0.dp else 4.dp, end = if (isUser) 4.dp else 0.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(annotatedString)
                            Toast.makeText(context, "تم نسخ الرسالة بنجاح 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ",
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (!isUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRegenerateClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة التوليد",
                                tint = TextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Avatar with sleek active gradient
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "أنت",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AgentStructuredResultCard(
    result: AgentResult,
    currency: String,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val context = LocalContext.current

    when (result) {
        is AgentResult.ConfirmationRequired -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "تأكيد", tint = Color(0xFFFFB300))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "مطلوب تأكيد الإجراء 🛡️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConfirmClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("نعم، أكد العملية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onCancelClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("إلغاء الأمر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        is AgentResult.ReportResult -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = result.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigoLight)
                        
                        if (result.pdfGenerated) {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "📄 تم تصدير التقرير بصيغة PDF وحفظه في الذاكرة المحلية بنجاح!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFD32F2F).copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "تحميل PDF", tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = result.summaryText, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Tabular Grid Renderer inside the App!
                    if (result.dataTable.isNotEmpty()) {
                        Text(text = "جدول البيانات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            // Header row
                            val keys = result.dataTable[0].keys.toList()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceDark)
                                    .padding(6.dp)
                            ) {
                                keys.forEach { key ->
                                    Text(
                                        text = key,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            // Data rows
                            result.dataTable.take(6).forEach { rowData ->
                                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp)
                                ) {
                                    keys.forEach { key ->
                                        Text(
                                            text = rowData[key] ?: "-",
                                            modifier = Modifier.weight(1f),
                                            fontSize = 9.sp,
                                            color = TextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        is AgentResult.AnalyticsResult -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = result.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigoLight)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = result.details, fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.statsList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = item.label, fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = item.value,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isPositive) Color(0xFF4CAF50) else Color(0xFFEF5350)
                                )
                            }
                        }
                    }
                }
            }
        }

        is AgentResult.RiskReportResult -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = result.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigoLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RiskSummaryChip(label = "معرض للتراجع", count = result.atRiskCount, color = Color(0xFFEF5350))
                        RiskSummaryChip(label = "يحتاج متابعة", count = result.attentionCount, color = Color(0xFFFFB300))
                        RiskSummaryChip(label = "ممتاز", count = result.excellentCount, color = Color(0xFF4CAF50))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // List of students evaluations
                    Text(text = "تقييم أداء وتنبؤات تراجع الطلاب:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.riskRecords.take(5).forEach { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = rec.studentName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = rec.reason, fontSize = 9.sp, color = TextSecondary, lineHeight = 12.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(android.graphics.Color.parseColor(rec.colorHex)).copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = rec.statusLabel,
                                        color = Color(android.graphics.Color.parseColor(rec.colorHex)),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        else -> {}
    }
}

@Composable
fun RiskSummaryChip(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = "$label: $count", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AgentThinkingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // AI Avatar with beautiful neon gradient border
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(PrimaryIndigo, PrimaryIndigoLight)
                    )
                )
                .padding(1.5.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Nexora AI",
                tint = PrimaryIndigoLight,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Nexora AI Agent 🤖",
                fontSize = 11.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                    .padding(12.dp)
                    .widthIn(max = 285.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = PrimaryIndigoLight,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جاري مراجعة قواعد البيانات وتنفيذ الأوامر الذكية...",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
