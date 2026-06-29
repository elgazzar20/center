package com.example.ui.parent

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentLoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: com.example.data.viewmodel.AppViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val auth = com.example.util.FirebaseSafe.auth
    val firestore = com.example.util.FirebaseSafe.firestore
    
    // Core states
    var isLoading by remember { mutableStateOf(false) }
    var checkingSession by remember { mutableStateOf(true) }
    
    // Google Account Identity
    var loggedInEmail by remember { mutableStateOf("") }
    var loggedInName by remember { mutableStateOf("") }
    var loggedInUid by remember { mutableStateOf("") }
    
    // Onboarding Student Linking states
    var showLinkOnboarding by remember { mutableStateOf(false) }
    var parentPhone by remember { mutableStateOf("") }
    var studentLinkCode by remember { mutableStateOf("") }
    var showCameraScanner by remember { mutableStateOf(false) }
    
    // Mock login fallback dialog
    var showMockLoginDialog by remember { mutableStateOf(false) }
    var mockEmailInput by remember { mutableStateOf("") }
    var mockNameInput by remember { mutableStateOf("") }
    
    var directStudentCode by remember { mutableStateOf("") }
    var showDirectCameraScanner by remember { mutableStateOf(false) }

    var showSetupDialog by remember { mutableStateOf(false) }
    var setupDialogErrorMessage by remember { mutableStateOf("") }
    
    val allStudents by viewModel.students.collectAsState()

    // Helper to verify if the parent is already registered and has linked students
    suspend fun checkParentStatusAndRoute(
        uid: String, 
        email: String, 
        name: String, 
        autoRedirectOnSuccess: Boolean = false
    ) {
        checkingSession = true
        val currentLocalLinked = ParentSessionManager.getLinkedStudents(context)
        if (firestore != null) {
            try {
                android.util.Log.d("ParentAuth", "Checking Parent status in Firestore for UID: $uid")
                val doc = firestore.collection("parents").document(uid).get().await()
                if (doc.exists()) {
                    val linked = doc.get("linkedStudents") as? List<String> ?: emptyList()
                    val phone = doc.getString("phone") ?: ""
                    if (linked.isNotEmpty()) {
                        android.util.Log.d("ParentAuth", "Found active parent profile with linked students. Restoring session.")
                        ParentSessionManager.saveSession(context, uid, name, email, phone, linked)
                        viewModel.saveParentAndLinkLocally(uid, name, email, phone, linked)
                        viewModel.startRealtimeSyncForParent(linked)
                        viewModel.syncData()
                        viewModel.fetchAndSyncLinkedStudents(linked)
                        onLoginSuccess()
                    } else {
                        android.util.Log.d("ParentAuth", "Parent profile exists but has empty linkedStudents. Prompting onboarding.")
                        ParentSessionManager.saveSession(context, uid, name, email, phone, emptyList())
                        showLinkOnboarding = true
                    }
                } else {
                    android.util.Log.d("ParentAuth", "No parent profile found in Firestore. Prompting onboarding.")
                    ParentSessionManager.saveSession(context, uid, name, email, "", emptyList())
                    showLinkOnboarding = true
                }
            } catch (e: Exception) {
                android.util.Log.e("ParentAuth", "Error loading parent profile: ${e.message}", e)
                // Fallback local check
                val localStudents = viewModel.students.value.filter { it.parentPhone == email || (it.parentPhone.startsWith("0") && email.contains(it.parentPhone)) }
                val phone = if (localStudents.isNotEmpty()) localStudents.first().parentPhone else ""
                val linkedIds = if (localStudents.isNotEmpty()) localStudents.map { it.id } else currentLocalLinked
                
                ParentSessionManager.saveSession(context, uid, name, email, phone, linkedIds)
                
                if (linkedIds.isNotEmpty()) {
                    viewModel.saveParentAndLinkLocally(uid, name, email, phone, linkedIds)
                    viewModel.startRealtimeSyncForParent(linkedIds)
                    onLoginSuccess()
                } else {
                    showLinkOnboarding = true
                }
            } finally {
                checkingSession = false
            }
        } else {
            // Local/Offline Fallback checks
            val localStudents = viewModel.students.value.filter { it.parentPhone == email || (it.parentPhone.startsWith("0") && email.contains(it.parentPhone)) }
            val phone = if (localStudents.isNotEmpty()) localStudents.first().parentPhone else ""
            val linkedIds = if (localStudents.isNotEmpty()) localStudents.map { it.id } else currentLocalLinked
            
            ParentSessionManager.saveSession(context, uid, name, email, phone, linkedIds)
            
            if (linkedIds.isNotEmpty()) {
                viewModel.saveParentAndLinkLocally(uid, name, email, phone, linkedIds)
                viewModel.startRealtimeSyncForParent(linkedIds)
                onLoginSuccess()
            } else {
                showLinkOnboarding = true
            }
            checkingSession = false
        }
    }

    // Google Sign-In setup
    val webClientId = remember(context) {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "34385368415-lbal8vgrp0okkhjieqebqrdop59uacsi.apps.googleusercontent.com"
        } catch (e: Exception) {
            "34385368415-lbal8vgrp0okkhjieqebqrdop59uacsi.apps.googleusercontent.com"
        }
    }

    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember(context, gso) {
        GoogleSignIn.getClient(context, gso)
    }

    // Google Intent launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val email = account.email ?: ""
                val name = account.displayName ?: "ولي أمر"
                
                // Validate email
                if (!com.example.util.ValidationUtils.isValidAndTrustedEmail(email)) {
                    Toast.makeText(context, "البريد الإلكتروني المختار غير موثوق أو غير صالح.", Toast.LENGTH_LONG).show()
                    return@rememberLauncherForActivityResult
                }
                
                isLoading = true
                coroutineScope.launch {
                    try {
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        if (auth != null) {
                            val authResult = auth.signInWithCredential(credential).await()
                            val user = authResult.user
                            if (user != null) {
                                loggedInEmail = user.email ?: email
                                loggedInName = user.displayName ?: name
                                loggedInUid = user.uid
                                
                                checkParentStatusAndRoute(loggedInUid, loggedInEmail, loggedInName)
                            } else {
                                throw Exception("تعذر استخراج بيانات المستخدم من فيربيز")
                            }
                        } else {
                            // Offline/Fallback mode
                            loggedInEmail = email
                            loggedInName = name
                            loggedInUid = "MOCK_G_${email.hashCode()}"
                            checkParentStatusAndRoute(loggedInUid, loggedInEmail, loggedInName)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "فشل المصادقة مع فيربيز: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            } catch (e: ApiException) {
                isLoading = false
                val msg = "خطأ من خدمات جوجل: كود ${e.statusCode}\n\nيحدث هذا عادة عندما لا تتوفر خدمات Google Play بشكل كامل على محاكي التطوير، أو عند عدم تطابق مفتاح التوقيع SHA-1 للتطبيق مع لوحة تحكم Firebase."
                setupDialogErrorMessage = msg
                showSetupDialog = true
            }
        } else {
            isLoading = false
            val msg = "تم إلغاء عملية تسجيل الدخول أو فشلت.\n\nتأكد من تفعيل خدمة Google Sign-In في لوحة تحكم Firebase وتثبيت ملف google-services.json وتوقيع SHA-1 بشكل صحيح."
            setupDialogErrorMessage = msg
            showSetupDialog = true
        }
    }

    // Checking existing session (Session Restore)
    LaunchedEffect(auth?.currentUser) {
        val currentUser = auth?.currentUser
        if (currentUser != null) {
            val email = currentUser.email ?: ""
            val name = currentUser.displayName ?: "ولي أمر"
            val uid = currentUser.uid
            
            loggedInEmail = email
            loggedInName = name
            loggedInUid = uid
            
            checkParentStatusAndRoute(uid, email, name, autoRedirectOnSuccess = true)
        } else {
            if (ParentSessionManager.isLoggedIn(context)) {
                val uid = ParentSessionManager.getUid(context) ?: "MOCK_G_UNKNOWN"
                val email = ParentSessionManager.getEmail(context)
                val name = ParentSessionManager.getName(context)
                val linked = ParentSessionManager.getLinkedStudents(context)
                
                loggedInEmail = email
                loggedInName = name
                loggedInUid = uid
                
                // Save database models and start realtime sync on session restoration
                viewModel.saveParentAndLinkLocally(uid, name, email, "", linked)
                viewModel.startRealtimeSyncForParent(linked)
                onLoginSuccess()
            } else {
                checkingSession = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showLinkOnboarding) "إكمال ربط حساب الطالب" else "تسجيل دخول ولي الأمر", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showLinkOnboarding) {
                            showLinkOnboarding = false
                            try { auth?.signOut() } catch(e:Exception){}
                            loggedInEmail = ""
                            loggedInName = ""
                            loggedInUid = ""
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark, titleContentColor = TextPrimary)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        if (checkingSession) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("جاري استعادة الجلسة والتحقق من حسابك...", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else if (showLinkOnboarding) {
            // Onboarding Student Linking Screen (عند أول دخول أو عند عدم ربط أي طالب)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "مرحباً بك: $loggedInName",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = loggedInEmail,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الحساب موثق عبر Google وبانتظار ربط الأبناء",
                                    fontSize = 12.sp,
                                    color = PrimaryIndigoLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "يرجى ربط حساب طالب واحد على الأقل لتفعيل لوحة المتابعة الخاصة بك:",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("رقم الهاتف للتواصل") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_parent_phone"),
                    placeholder = { Text("مثال: 01xxxxxxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = studentLinkCode,
                    onValueChange = { studentLinkCode = it },
                    label = { Text("كود ربط الطالب (كود المتابعة أو QR أو كود ولي الأمر)") },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showCameraScanner = true }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "تصوير الكود ومسحه باستخدام الكاميرا", tint = com.example.ui.theme.PrimaryIndigoLight)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_link_code"),
                    placeholder = { Text("مثال: STU_XXXX أو كود من 6 رموز") }
                )

                if (showCameraScanner) {
                    com.example.ui.components.CameraQrScannerDialog(
                        onDismiss = { showCameraScanner = false },
                        onCodeScanned = { scannedCode ->
                            studentLinkCode = scannedCode
                            showCameraScanner = false
                            Toast.makeText(context, "تم مسح الكود بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Sample Student Codes section
                val sampleStudents = allStudents.take(3)
                if (sampleStudents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "أكواد الطلاب للتجربة السريعة (انقر للتعبئة تلقائياً):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            sampleStudents.forEach { student ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            studentLinkCode = student.parentCode
                                            if (student.parentPhone.contains("@") || student.parentPhone.startsWith("0")) {
                                                parentPhone = student.parentPhone
                                            } else {
                                                parentPhone = "01100000000"
                                            }
                                            Toast.makeText(context, "تم اختيار الطالب: ${student.name}", Toast.LENGTH_SHORT).show()
                                        }
                                        .background(PrimaryIndigo.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(student.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("المسار: ${student.customCourse}", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = student.parentCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryIndigoLight,
                                            modifier = Modifier
                                                .background(PrimaryIndigo.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (parentPhone.isBlank() || studentLinkCode.isBlank()) {
                            Toast.makeText(context, "يرجى ملء جميع الحقول المطلوبة للمتابعة", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val code = studentLinkCode.trim()
                                val upperCode = code.uppercase()
                                var matchedStudent: com.example.data.model.Student? = null
                                
                                android.util.Log.d("ParentAuth", "Onboarding: Linking student using code: $code")
                                
                                // 1. Check local database first for speed
                                val localStudents = viewModel.students.value
                                matchedStudent = localStudents.find {
                                    it.id == code || it.qrCode == code || it.parentCode == code ||
                                    it.qrCode == upperCode || it.parentCode == upperCode
                                }

                                // 2. If not found locally, query Firestore in parallel using async
                                if (matchedStudent == null && firestore != null) {
                                    try {
                                        kotlinx.coroutines.coroutineScope {
                                             val d1 = async {
                                                 try {
                                                     val doc = firestore.collection("students").document(code).get().await()
                                                     if (doc.exists()) doc.toObject(com.example.data.model.Student::class.java) else null
                                                 } catch (e: Exception) { null }
                                             }
                                             val d2 = async {
                                                 try {
                                                     val query = firestore.collection("students").whereEqualTo("parentCode", upperCode).get().await()
                                                     if (!query.isEmpty) query.documents.first().toObject(com.example.data.model.Student::class.java) else null
                                                 } catch (e: Exception) { null }
                                             }
                                             val d3 = async {
                                                 try {
                                                     val query = firestore.collection("students").whereEqualTo("qrCode", upperCode).get().await()
                                                     if (!query.isEmpty) query.documents.first().toObject(com.example.data.model.Student::class.java) else null
                                                 } catch (e: Exception) { null }
                                             }
                                             matchedStudent = d1.await() ?: d2.await() ?: d3.await()
                                         }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ParentAuth", "Error doing parallel search in Firestore: ${e.message}")
                                    }
                                }

                                val finalStudent = matchedStudent
                                 if (finalStudent == null) {
                                     throw Exception("كود ربط الطالب المدخل غير صحيح أو غير مسجل في السنتر.")
                                 }

                                 val finalLinkedStudents = listOf(finalStudent.id)

                                // Update or create parent document in firestore (robust, won't crash if offline or permission denied)
                                val parentData = hashMapOf(
                                    "id" to loggedInUid,
                                    "uid" to loggedInUid,
                                    "name" to loggedInName,
                                    "email" to loggedInEmail,
                                    "phone" to parentPhone.trim(),
                                    "linkedStudents" to finalLinkedStudents
                                )
                                
                                if (firestore != null) {
                                    try {
                                        firestore.collection("parents").document(loggedInUid).set(parentData).await()
                                        android.util.Log.d("ParentAuth", "Onboarding: Firestore parent profile successfully created/updated.")
                                    } catch (fe: Exception) {
                                        android.util.Log.e("ParentAuth", "Onboarding: Firestore parent profile set failed: ${fe.message}. Proceeding offline.")
                                    }
                                }
                                
                                // Save session locally
                                ParentSessionManager.saveSession(
                                    context = context,
                                    uid = loggedInUid,
                                    name = loggedInName,
                                    email = loggedInEmail,
                                    phone = parentPhone.trim(),
                                    linkedStudents = finalLinkedStudents
                                )

                                // Sync locally
                                 viewModel.addDirectStudent(finalStudent)
                                 viewModel.fetchAndSyncLinkedStudents(finalLinkedStudents)

                                 viewModel.saveParentAndLinkLocally(loggedInUid, loggedInName, loggedInEmail, parentPhone.trim(), finalLinkedStudents)
                                 viewModel.startRealtimeSyncForParent(finalLinkedStudents)
                                 Toast.makeText(context, "تم ربط الطالب: ${finalStudent.name} بنجاح!", Toast.LENGTH_LONG).show()
                                showLinkOnboarding = false
                                onLoginSuccess()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "فشل ربط الطالب", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("ربط الحساب والذهاب للرئيسية 🚀", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // General Parent Google Login Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark)
                    .drawBehind {
                        // Glowing radial violet light in the top-right
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryIndigo.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(x = size.width, y = 0f),
                                radius = size.width * 0.9f
                            ),
                            radius = size.width * 0.9f,
                            center = Offset(x = size.width, y = 0f)
                        )
                        
                        // Glowing radial success emerald in bottom-left
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(SuccessColor.copy(alpha = 0.15f), Color.Transparent),
                                center = Offset(x = 0f, y = size.height),
                                radius = size.width * 0.9f
                            ),
                            radius = size.width * 0.9f,
                            center = Offset(x = 0f, y = size.height)
                        )

                        // Subtle orbital ring
                        drawCircle(
                            color = PrimaryIndigoLight.copy(alpha = 0.04f),
                            radius = size.width * 0.5f,
                            center = Offset(x = size.width * 0.5f, y = size.height * 0.22f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "بوابة أولياء الأمور والطلاب",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "نظام المتابعة الأكاديمية الشامل للمسار والدرجات والحضور",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Dynamic Student Code Login and Linking Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الدخول برمز الطالب 🔗",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "أدخل كود الطالب الفريد (كود المتابعة أو كود QR) للتحقق المباشر من قاعدة البيانات وتفعيل المتابعة.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = directStudentCode,
                                onValueChange = { directStudentCode = it },
                                label = { Text("كود الطالب (المتابعة أو QR)") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = PrimaryIndigoLight) },
                                trailingIcon = {
                                    IconButton(onClick = { showDirectCameraScanner = true }) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "مسح الكاميرا لقراءة الرمز", tint = PrimaryIndigoLight)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("direct_student_code_input"),
                                placeholder = { Text("مثال: STU_XXXX أو كود من 6 رموز") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigoLight,
                                    unfocusedBorderColor = BorderColor,
                                    focusedLabelColor = PrimaryIndigoLight
                                )
                            )

                            if (showDirectCameraScanner) {
                                com.example.ui.components.CameraQrScannerDialog(
                                    onDismiss = { showDirectCameraScanner = false },
                                    onCodeScanned = { scannedCode ->
                                        directStudentCode = scannedCode
                                        showDirectCameraScanner = false
                                        Toast.makeText(context, "تم مسح الكود بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (directStudentCode.isBlank()) {
                                        Toast.makeText(context, "يرجى إدخال رمز الطالب أولاً للمتابعة", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val code = directStudentCode.trim()
                                            val upperCode = code.uppercase()
                                            var matchedStudent: com.example.data.model.Student? = null
                                            
                                            android.util.Log.d("ParentAuth", "Direct code login verifying: $code")
                                            
                                            // 1. Check local database first for speed
                                            val localStudents = viewModel.students.value
                                            matchedStudent = localStudents.find {
                                                it.id == code || it.qrCode == code || it.parentCode == code ||
                                                it.qrCode == upperCode || it.parentCode == upperCode
                                            }

                                            // 2. If not found locally, query Firestore in parallel using async
                                            if (matchedStudent == null && firestore != null) {
                                                 try {
                                                     kotlinx.coroutines.coroutineScope {
                                                         val d1 = async {
                                                             try {
                                                                 val doc = firestore.collection("students").document(code).get().await()
                                                                 if (doc.exists()) doc.toObject(com.example.data.model.Student::class.java) else null
                                                             } catch (e: Exception) { null }
                                                         }
                                                         val d2 = async {
                                                             try {
                                                                 val query = firestore.collection("students").whereEqualTo("parentCode", upperCode).get().await()
                                                                 if (!query.isEmpty) query.documents.first().toObject(com.example.data.model.Student::class.java) else null
                                                             } catch (e: Exception) { null }
                                                         }
                                                         val d3 = async {
                                                             try {
                                                                 val query = firestore.collection("students").whereEqualTo("qrCode", upperCode).get().await()
                                                                 if (!query.isEmpty) query.documents.first().toObject(com.example.data.model.Student::class.java) else null
                                                             } catch (e: Exception) { null }
                                                         }
                                                         matchedStudent = d1.await() ?: d2.await() ?: d3.await()
                                                     }
                                                 } catch (e: Exception) {
                                                     android.util.Log.e("ParentAuth", "Error doing parallel search in Firestore: ${e.message}")
                                                 }
                                             }

                                            val finalStudent = matchedStudent
                                             if (finalStudent == null) {
                                                 throw Exception("كود ربط الطالب المدخل غير صحيح أو غير مسجل في السنتر.")
                                             }

                                             // Create custom parent credentials based on matched student
                                             val parentUid = "PARENT_STU_${finalStudent.id}"
                                             val pName = finalStudent.parentName.ifBlank { "ولي أمر ${finalStudent.name}" }
                                             val pEmail = if (finalStudent.parentPhone.contains("@")) finalStudent.parentPhone else "parent_${finalStudent.parentCode.lowercase()}@center.com"
                                             val pPhone = finalStudent.parentPhone.ifBlank { "01100000000" }
                                             val finalLinkedStudents = listOf(finalStudent.id)

                                            // Save parent session locally
                                            ParentSessionManager.saveSession(
                                                context = context,
                                                uid = parentUid,
                                                name = pName,
                                                email = pEmail,
                                                phone = pPhone,
                                                linkedStudents = finalLinkedStudents
                                            )

                                            // Update or create parent document in firestore (robust, won't crash if offline or permission denied)
                                            if (firestore != null) {
                                                try {
                                                    val parentData = hashMapOf(
                                                        "id" to parentUid,
                                                        "uid" to parentUid,
                                                        "name" to pName,
                                                        "email" to pEmail,
                                                        "phone" to pPhone,
                                                        "linkedStudents" to finalLinkedStudents
                                                    )
                                                    firestore.collection("parents").document(parentUid).set(parentData).await()
                                                } catch (fe: Exception) {
                                                    android.util.Log.e("ParentAuth", "Direct login Firestore parent profile set failed: ${fe.message}. Proceeding offline.")
                                                }
                                            }

                                            // Sync locally
                                             viewModel.addDirectStudent(finalStudent)
                                  viewModel.saveParentAndLinkLocally(parentUid, pName, pEmail, pPhone, finalLinkedStudents)
                                  viewModel.startRealtimeSyncForParent(finalLinkedStudents)
                                             viewModel.fetchAndSyncLinkedStudents(finalLinkedStudents)

                                             Toast.makeText(context, "تم تسجيل الدخول بنجاح لمتابعة الطالب: ${finalStudent.name}", Toast.LENGTH_LONG).show()
                                            onLoginSuccess()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, e.message ?: "فشل تسجيل الدخول", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("direct_student_code_submit_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("تسجيل الدخول والمتابعة 🚀", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor.copy(alpha = 0.5f))
                        Text(
                            text = "أو طرق تسجيل الدخول البديلة",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor.copy(alpha = 0.5f))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Real Google Authentication Button
                    Button(
                        onClick = {
                            isLoading = true
                            try {
                                val signInIntent = googleSignInClient.signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "تعذر بدء تسجيل جوجل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.White, RoundedCornerShape(4.dp))
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "تسجيل الدخول بواسطة Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Emulator-Friendly / Testing Google Sign-In Fallback Button
                    OutlinedButton(
                        onClick = { showMockLoginDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = PrimaryIndigoLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "محاكاة تسجيل الدخول (لبيئة الاختبار)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryIndigoLight
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Info card about authentication security
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "نظام حماية متطور يمنع الحسابات الوهمية والبريد الإلكتروني غير الموثق ويضمن سرية بيانات أبنائكم.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Beautiful simulated Google Login Dialog
    if (showMockLoginDialog) {
        AlertDialog(
            onDismissRequest = { showMockLoginDialog = false },
            title = {
                Text(
                    text = "محاكاة تسجيل دخول Google",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "محاكاة الدخول بحساب جوجل للتحقق من كافة سيناريوهات الربط والتشغيل السحابي في المحاكي.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = mockNameInput,
                        onValueChange = { mockNameInput = it },
                        label = { Text("الاسم الكامل بجوجل") },
                        modifier = Modifier.fillMaxWidth().testTag("mock_google_name"),
                        placeholder = { Text("مثال: أحمد محمد") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mockEmailInput,
                        onValueChange = { mockEmailInput = it },
                        label = { Text("البريد الإلكتروني لجوجل") },
                        modifier = Modifier.fillMaxWidth().testTag("mock_google_email"),
                        placeholder = { Text("example@gmail.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = mockEmailInput.trim()
                        val name = mockNameInput.trim().ifEmpty { "ولي أمر تجريبي" }
                        
                        if (email.isEmpty()) {
                            Toast.makeText(context, "يرجى كتابة البريد الإلكتروني الموثق أولاً", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // Prevent disposable or invalid emails explicitly!
                        if (!com.example.util.ValidationUtils.isValidAndTrustedEmail(email)) {
                            Toast.makeText(context, "البريد الإلكتروني المدخل وهمي أو غير موثوق. يرجى إدخال حساب Gmail حقيقي وموثق.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        
                        showMockLoginDialog = false
                        isLoading = true
                        
                        coroutineScope.launch {
                            try {
                                loggedInEmail = email
                                loggedInName = name
                                loggedInUid = "SIM_G_${email.hashCode()}"
                                
                                checkParentStatusAndRoute(loggedInUid, loggedInEmail, loggedInName)
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل محاكاة تسجيل الدخول: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("دخول سحابي محاكى")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMockLoginDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    if (showSetupDialog) {
        FirebaseInstructionsDialog(
            errorMessage = setupDialogErrorMessage,
            onDismiss = { showSetupDialog = false },
            onSkipToDemo = {
                showSetupDialog = false
                onLoginSuccess()
            }
        )
    }
}

@Composable
private fun FirebaseInstructionsDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onSkipToDemo: () -> Unit
) {
    val context = LocalContext.current
    val sha1 = remember(context) { com.example.util.SignatureUtils.getCertificateSHA1(context) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(WarningColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = WarningColor, modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "دليل الربط بـ Firebase",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = errorMessage,
                    fontSize = 11.sp,
                    color = ErrorColorLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "بصمة SHA-1 لجهازك الحالي (اضغط للنسخ) 📋",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sha1,
                            color = PrimaryIndigoLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(sha1))
                                    Toast.makeText(context, "تم نسخ بصمة SHA-1 بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                                .background(SurfaceDark, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "خطوات ربط وتسجيل الدخول بحساب Google:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                Spacer(modifier = Modifier.height(8.dp))

                SetupStepItem(number = "١", text = "سجل بموقع Firebase Console وأنشئ مشروعاً جديداً.")
                SetupStepItem(number = "٢", text = "قم بإضافة تطبيق أندرويد بالاسم البرمجي com.example.")
                SetupStepItem(number = "٣", text = "أدخل مفتاح التوقيع SHA-1 الخاص بجهازك لتمكين Google Auth.")
                SetupStepItem(number = "٤", text = "حمّل ملف google-services.json وضعه داخل مجلد app/ بالكامل.")
                SetupStepItem(number = "٥", text = "قم بتفعيل خيار 'Google' كمزود خدمة في قسم Authentication.")

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSkipToDemo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("المتابعة بالوضع التجريبي (ديمو)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("إغلاق وإعادة المحاولة", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SetupStepItem(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Right,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(PrimaryIndigo.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigoLight
            )
        }
    }
}


object ParentSessionManager {
    fun saveSession(
        context: android.content.Context,
        uid: String,
        name: String,
        email: String,
        phone: String,
        linkedStudents: List<String>
    ) {
        val prefs = context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("parent_uid", uid)
            .putString("parent_name", name)
            .putString("parent_email", email)
            .putString("parent_phone", phone)
            .putString("linked_students", linkedStudents.distinct().joinToString(","))
            .apply()
    }

    fun getLinkedStudents(context: android.content.Context): List<String> {
        val prefs = context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE)
        val joined = prefs.getString("linked_students", "") ?: ""
        return if (joined.isBlank()) emptyList() else joined.split(",").filter { it.isNotBlank() }
    }

    fun saveLinkedStudents(context: android.content.Context, linkedStudents: List<String>) {
        val prefs = context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("linked_students", linkedStudents.distinct().joinToString(",")).apply()
    }

    fun getUid(context: android.content.Context): String? {
        val prefs = context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("parent_uid", null)
    }

    fun getName(context: android.content.Context): String {
        val prefs = context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("parent_name", "ولي الأمر") ?: "ولي الأمر"
    }

    fun getEmail(context: android.content.Context): String {
        val prefs = context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("parent_email", "") ?: ""
    }

    fun getPhone(context: android.content.Context): String {
        val prefs = context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("parent_phone", "") ?: ""
    }

    fun isLoggedIn(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean("is_logged_in", false)
    }

    fun clearSession(context: android.content.Context) {
        val prefs = context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}


