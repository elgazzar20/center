package com.example.ui.backup

import android.app.Application
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: AppViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val appDao = remember { AppDatabase.getDatabase(context).appDao() }
    val backupManager = remember { BackupManager(context, appDao) }
    val restoreManager = remember { RestoreManager(context, appDao) }

    // Collect data sizes from ViewModel for displaying counts
    val students by viewModel.students.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val paymentHistory by viewModel.paymentHistory.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    
    // Group size can be loaded dynamically
    var groupsCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            appDao.getAllGroups().collect { groupsList ->
                groupsCount = groupsList.size
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var lastBackupTime by remember { mutableLongStateOf(backupManager.getLastBackupTimestamp()) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var isOperating by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Launcher for exporting backup
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isOperating = true
                val success = backupManager.exportBackupToJson(uri)
                isOperating = false
                if (success) {
                    lastBackupTime = backupManager.getLastBackupTimestamp()
                    Toast.makeText(context, "تم تصدير النسخة الاحتياطية بنجاح 📁", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "فشل تصدير النسخة الاحتياطية ❌", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Launcher for importing backup
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "النسخ الاحتياطي والاستعادة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
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
                actions = {
                    // Spacer to balance the centered title
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Hero Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PrimaryIndigo.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "حماية وحفظ بياناتك بالكامل",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يمكنك تصدير نسخة احتياطية مشفرة بجميع تفاصيل السنتر واستعادتها في أي وقت أوفلاين 100%.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Last backup time
                            Surface(
                                color = SurfaceLightDark,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = SuccessColorLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (lastBackupTime > 0L) {
                                            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("ar"))
                                            "آخر نسخة احتياطية: " + sdf.format(Date(lastBackupTime))
                                        } else {
                                            "لم يتم عمل نسخة احتياطية بعد"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (lastBackupTime > 0L) SuccessColorLight else TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }

                // Statistics Section Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "البيانات المشمولة بالنسخ والمسجلة حالياً",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BackupStatRow(label = "الطلاب المسجلين", count = students.size, icon = Icons.Default.Person)
                                BackupStatRow(label = "المجموعات الحالية", count = groupsCount, icon = Icons.Default.Groups)
                                BackupStatRow(label = "سجلات الحضور والغياب", count = attendance.size, icon = Icons.Default.FactCheck)
                                BackupStatRow(label = "المدفوعات والمقبوضات", count = payments.size + paymentHistory.size, icon = Icons.Default.Payments)
                                BackupStatRow(label = "المصروفات المسجلة", count = expenses.size, icon = Icons.Default.AccountBalanceWallet)
                                BackupStatRow(label = "المعلمون والمساعدون", count = teachers.size, icon = Icons.Default.SupervisedUserCircle)
                            }
                        }
                    }
                }

                // Action Buttons Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val dateStr = SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.US).format(Date())
                                exportLauncher.launch("center_plus_backup_$dateStr.json")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isOperating
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                                Text("إنشاء نسخة احتياطية جديدة (تصدير)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderColor),
                            enabled = !isOperating
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryIndigoLight)
                                Text("استعادة البيانات من ملف (استيراد)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigoLight)
                            }
                        }
                    }
                }
            }

            // Spinner Overlay when exporting/importing
            if (isOperating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryIndigoLight)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جاري معالجة البيانات...", color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // High-priority Confirmation Dialog for Restoring
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ErrorColorLight)
                    Text("تنبيه استعادة البيانات", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                }
            },
            text = {
                Text(
                    text = "تحذير: سيتم حذف جميع البيانات الحالية في التطبيق واستبدالها بالبيانات الواردة في ملف النسخ الاحتياطي. لا يمكن التراجع عن هذه العملية بعد إتمامها. هل تريد الاستمرار بالفعل؟",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        val uri = pendingRestoreUri
                        if (uri != null) {
                            coroutineScope.launch {
                                isOperating = true
                                val success = restoreManager.restoreBackupFromJson(uri)
                                isOperating = false
                                pendingRestoreUri = null
                                if (success) {
                                    Toast.makeText(context, "تم استعادة النسخة الاحتياطية بنجاح بنسبة 100% 🎉", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "فشل في استعادة النسخة الاحتياطية. يرجى التأكد من سلامة الملف ❌", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("نعم، استعادة وحذف الحالي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreUri = null
                    }
                ) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun BackupStatRow(
    label: String,
    count: Int,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 13.sp, color = TextSecondary)
        }
        Text(
            text = "$count سجل",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}
