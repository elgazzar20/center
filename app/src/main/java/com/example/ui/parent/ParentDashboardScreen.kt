package com.example.ui.parent

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    onLogout: () -> Unit,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = com.example.util.FirebaseSafe.auth
    val firestore = com.example.util.FirebaseSafe.firestore
    val userId = auth?.currentUser?.uid
    
    var parentName by remember { mutableStateOf("ولي الأمر") }
    var linkedStudentIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val allStudents by viewModel.students.collectAsState(initial = emptyList())
    val attendance by viewModel.attendance.collectAsState(initial = emptyList())
    val payments by viewModel.payments.collectAsState(initial = emptyList())
    
    val alertsList = remember(linkedStudentIds, allStudents, attendance, payments) {
        val alerts = mutableListOf<String>()
        val currentMonthStr = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
        
        val linkedStudents = allStudents.filter { it.id in linkedStudentIds }
        for (student in linkedStudents) {
            // Check Late Payments
            val hasPaid = payments.any { it.studentId == student.id && it.month == currentMonthStr }
            if (student.monthlyFee > 0 && !student.isExempt && !hasPaid) {
                alerts.add("⚠️ تنبيه مالي: لم يتم سداد رسوم شهر ($currentMonthStr) للطالب: ${student.name}")
            }
            
            // Check Recent Absences (last 7 days)
            val studentAttendance = attendance.filter { it.studentId == student.id }
            val recentAbsences = studentAttendance.filter { 
                it.status == "absent" && (System.currentTimeMillis() - it.date) < 7 * 24 * 60 * 60 * 1000 
            }
            if (recentAbsences.isNotEmpty()) {
                alerts.add("❌ غياب: تم تسجيل غياب الطالب: ${student.name} لعدد ${recentAbsences.size} حصة مؤخراً.")
            }
        }
        alerts
    }
    
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkCodeInput by remember { mutableStateOf("") }
    var showCameraScanner by remember { mutableStateOf(false) }
    
    var selectedStudentId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        val hasLocalSession = ParentSessionManager.isLoggedIn(context)
        val activeUid = userId ?: if (hasLocalSession) ParentSessionManager.getUid(context) else null
        
        if (activeUid != null) {
            isLoading = true
            // Load local cache session first for instant rendering
            if (hasLocalSession) {
                parentName = ParentSessionManager.getName(context)
                linkedStudentIds = ParentSessionManager.getLinkedStudents(context)
            }
            
            if (firestore != null) {
                try {
                    val doc = firestore.collection("parents").document(activeUid).get().await()
                    if (doc.exists()) {
                        parentName = doc.getString("name") ?: "ولي الأمر"
                        val linked = doc.get("linkedStudents") as? List<String> ?: emptyList()
                        linkedStudentIds = linked
                        
                        // Sync back to SharedPreferences
                        ParentSessionManager.saveSession(
                            context = context,
                            uid = activeUid,
                            name = parentName,
                            email = doc.getString("email") ?: "",
                            phone = doc.getString("phone") ?: "",
                            linkedStudents = linked
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ParentAuth", "Dashboard: Error fetching parent profile from Firestore: ${e.message}")
                } finally {
                    isLoading = false
                }
            } else {
                isLoading = false
            }
            
            // Sync academic details of linked students to local database
            if (linkedStudentIds.isNotEmpty()) {
                viewModel.saveParentAndLinkLocally(
                    parentId = activeUid,
                    name = parentName,
                    email = ParentSessionManager.getEmail(context),
                    phone = ParentSessionManager.getPhone(context),
                    studentIds = linkedStudentIds
                )
                viewModel.startRealtimeSyncForParent(linkedStudentIds)
                viewModel.fetchAndSyncLinkedStudents(linkedStudentIds)
            }
        } else {
            // Demo mode fallback
            val demoStudents = allStudents.filter { it.parentPhone == "parent@demo.com" }.map { it.id }
            if (demoStudents.isNotEmpty()) {
                linkedStudentIds = demoStudents
                parentName = "ولي الأمر (تجريبي)"
            }
            isLoading = false
        }
    }

    if (selectedStudentId != null) {
        // Show the detailed Student Portal view
        com.example.ui.academic.ParentPortalScreen(
            initialStudentId = selectedStudentId,
            viewModel = viewModel,
            onBackClick = { selectedStudentId = null }
        )
        return
    }

    val onUnlinkStudent: (Student) -> Unit = { student ->
        coroutineScope.launch {
            try {
                val newList = linkedStudentIds - student.id
                val activeUid = userId ?: if (ParentSessionManager.isLoggedIn(context)) ParentSessionManager.getUid(context) else null
                
                // Update SharedPreferences
                ParentSessionManager.saveLinkedStudents(context, newList)
                linkedStudentIds = newList
                
                if (activeUid != null && firestore != null) {
                    try {
                        firestore.collection("parents").document(activeUid).update("linkedStudents", newList).await()
                    } catch (fe: Exception) {
                        android.util.Log.e("ParentAuth", "Dashboard: Failed to unlink student in Firestore: ${fe.message}")
                    }
                }
                
                // Delete local ParentStudentLink relationship in Room
                if (activeUid != null) {
                    viewModel.removeParentStudentLinkLocally(activeUid, student.id)
                }
                
                // Restart realtime synchronization for the new reduced list
                viewModel.startRealtimeSyncForParent(newList)
                
                // Delete student locally so they are removed from the parent's dashboard view
                viewModel.deleteStudent(student.id)
                Toast.makeText(context, "تم إلغاء ربط الطالب بنجاح", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "حدث خطأ أثناء إلغاء الربط: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("مرحباً بك، $parentName", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { 
                        if (alertsList.isEmpty()) {
                            Toast.makeText(context, "لا توجد تنبيهات جديدة حالياً", Toast.LENGTH_SHORT).show()
                        } else {
                            showNotificationsDialog = true
                        }
                    }) {
                        BadgedBox(
                            badge = {
                                if (alertsList.isNotEmpty()) {
                                    Badge { Text(alertsList.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "التنبيهات", tint = TextPrimary)
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "تسجيل الخروج", tint = ErrorColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark, titleContentColor = TextPrimary),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLinkDialog = true },
                containerColor = PrimaryIndigo,
                modifier = Modifier.testTag("parent_add_student_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "ربط طالب جديد", tint = Color.White)
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryIndigo)
            }
        } else {
            val linkedStudents = allStudents.filter { it.id in linkedStudentIds || it.qrCode in linkedStudentIds || it.parentCode in linkedStudentIds }
            
            if (linkedStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("لا يوجد طلاب مرتبطين بحسابك", color = TextSecondary, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showLinkDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier.testTag("parent_link_now_btn")
                        ) {
                            Text("ربط طالب الآن")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("الطلاب المرتبطين", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(linkedStudents) { student ->
                        StudentLinkCard(
                            student = student,
                            onClick = { selectedStudentId = student.id },
                            onUnlinkClick = { onUnlinkStudent(student) }
                        )
                    }
                }
            }
        }
    }
    
    if (showNotificationsDialog && alertsList.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Text(
                    text = "التنبيهات العاجلة 🔔",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    alertsList.forEach { alert ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, BorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = alert,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Right,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("إغلاق", color = PrimaryIndigoLight, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("ربط طالب", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("أدخل كود ولي الأمر (Parent Code) أو رقم مسار الطالب (Student ID) أو امسح رمز QR", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = linkCodeInput,
                        onValueChange = { linkCodeInput = it },
                        label = { Text("الكود") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showCameraScanner = true }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "تصوير ومسح الكود باستخدام الكاميرا", tint = PrimaryIndigoLight)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("link_code_input")
                    )

                    if (showCameraScanner) {
                        com.example.ui.components.CameraQrScannerDialog(
                            onDismiss = { showCameraScanner = false },
                            onCodeScanned = { scannedCode ->
                                linkCodeInput = scannedCode
                                showCameraScanner = false
                                Toast.makeText(context, "تم مسح الكود بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = linkCodeInput.trim()
                        val upperCode = code.uppercase()
                        if (code.isNotEmpty()) {
                            coroutineScope.launch {
                                try {
                                     // 1. Check local database first for speed
                                     var student = allStudents.find { 
                                         it.id == code || it.qrCode == code || it.parentCode == code ||
                                         it.qrCode == upperCode || it.parentCode == upperCode
                                     }

                                     // 2. If not found locally, query Firestore in parallel using async
                                     if (student == null && firestore != null) {
                                         try {
                                             kotlinx.coroutines.coroutineScope {
                                                  val d1 = async {
                                                      try {
                                                          val doc = firestore.collection("students").document(code).get().await()
                                                          if (doc.exists()) doc.toObject(Student::class.java) else null
                                                      } catch (e: Exception) { null }
                                                  }
                                                  val d2 = async {
                                                      try {
                                                          val query = firestore.collection("students").whereEqualTo("parentCode", upperCode).get().await()
                                                          if (!query.isEmpty) query.documents.first().toObject(Student::class.java) else null
                                                      } catch (e: Exception) { null }
                                                  }
                                                  val d3 = async {
                                                      try {
                                                          val query = firestore.collection("students").whereEqualTo("qrCode", upperCode).get().await()
                                                          if (!query.isEmpty) query.documents.first().toObject(Student::class.java) else null
                                                      } catch (e: Exception) { null }
                                                  }
                                                  student = d1.await() ?: d2.await() ?: d3.await()
                                              }
                                         } catch (e: Exception) {
                                             android.util.Log.e("ParentAuth", "Dashboard: Parallel search error: ${e.message}", e)
                                         }
                                     }
                                    
                                    val finalStudent = student
                                     if (finalStudent != null) {
                                         val activeUid = userId ?: if (ParentSessionManager.isLoggedIn(context)) ParentSessionManager.getUid(context) else null
                                         if (activeUid != null) {
                                             val newList = (linkedStudentIds + finalStudent.id).distinct()
                                             android.util.Log.d("ParentAuth", "Dashboard: Saving updated linkedStudents list to parents/$activeUid: $newList")

                                             ParentSessionManager.saveLinkedStudents(context, newList)
                                             linkedStudentIds = newList

                                             if (firestore != null) {
                                                 try {
                                                     val updateData = hashMapOf("linkedStudents" to newList)
                                                     firestore.collection("parents").document(activeUid)
                                                         .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                                                         .await()
                                                 } catch (fe: Exception) {
                                                     android.util.Log.e("ParentAuth", "Dashboard: Firestore update failed: ${fe.message}. Saved locally.")
                                                 }
                                             }

                                             // Sync this student and all their academic data to local Room
                                             viewModel.addDirectStudent(finalStudent)
                                             viewModel.fetchAndSyncLinkedStudents(listOf(finalStudent.id))
                                             android.util.Log.d("ParentAuth", "Dashboard: Linked student ${finalStudent.name} successfully updated & synced locally.")

                                             Toast.makeText(context, "تم ربط الطالب ${finalStudent.name} بنجاح", Toast.LENGTH_SHORT).show()
                                         } else {
                                             val newList = (linkedStudentIds + finalStudent.id).distinct()
                                             linkedStudentIds = newList
                                             viewModel.addDirectStudent(finalStudent)
                                             Toast.makeText(context, "تم ربط الطالب ${finalStudent.name} بنجاح (وضع غير متصل)", Toast.LENGTH_SHORT).show()
                                         }
                                         showLinkDialog = false
                                         linkCodeInput = ""
                                     } else {
                                        android.util.Log.w("ParentAuth", "Dashboard: Student with code '$code' was not found.")
                                        Toast.makeText(context, "الكود غير صحيح أو الطالب غير مسجل بالمنظومة. يرجى مراجعة الكود.", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ParentAuth", "Dashboard: Linking exception occurred: ${e.message}", e)
                                    Toast.makeText(context, "تعذر ربط الطالب: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier.testTag("confirm_link_btn")
                ) {
                    Text("تأكيد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }, modifier = Modifier.testTag("cancel_link_btn")) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun StudentLinkCard(student: Student, onClick: () -> Unit, onUnlinkClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        BorderColor,
                        PrimaryIndigo.copy(alpha = 0.25f),
                        BorderColor
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .testTag("student_link_card_${student.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryIndigoLight.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.firstOrNull()?.toString() ?: "ط",
                    color = PrimaryIndigoLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الصف: ${student.grade.ifEmpty { "غير محدد" }}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(
                onClick = onUnlinkClick,
                modifier = Modifier
                    .testTag("unlink_btn_${student.id}")
                    .clip(RoundedCornerShape(10.dp))
                    .background(ErrorColor.copy(alpha = 0.08f))
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "إلغاء ربط الطالب",
                    tint = ErrorColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
