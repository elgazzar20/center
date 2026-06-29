package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import com.example.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToParentPortal: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSetupDialog by remember { mutableStateOf(false) }
    var setupDialogErrorMessage by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var isResettingPassword by remember { mutableStateOf(false) }

    // Firebase Auth instance safe initialization
    val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    // Dynamic extraction of Web Client ID
    val webClientId = remember(context) {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "34385368415-lbal8vgrp0okkhjieqebqrdop59uacsi.apps.googleusercontent.com"
        } catch (e: Exception) {
            "34385368415-lbal8vgrp0okkhjieqebqrdop59uacsi.apps.googleusercontent.com"
        }
    }

    // Google Sign-In setup
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember(context, gso) {
        GoogleSignIn.getClient(context, gso)
    }

    // StartActivityForResult Launcher for Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                // Sign in with Firebase
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                isLoading = true
                auth?.signInWithCredential(credential)
                    ?.addOnCompleteListener { taskResult ->
                        isLoading = false
                        if (taskResult.isSuccessful) {
                            Toast.makeText(context, "تم تسجيل الدخول بجوجل بنجاح", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            val msg = taskResult.exception?.localizedMessage ?: "فشل مصادقة فيربيز ببطاقة جوجل"
                            setupDialogErrorMessage = "Firebase Connection Info: $msg\n\nتأكد من تفعيل خدمة Google Sign-In في لوحة تحكم Firebase وتثبيت ملف google-services.json وتوقيع SHA-1 بشكل صحيح."
                            showSetupDialog = true
                        }
                    } ?: run {
                    isLoading = false
                    Toast.makeText(context, "خطأ: نظام المصادقة السحابي غير متصل أو غير متاح حالياً للتحقق من بيانات Google.", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                isLoading = false
                val msg = "خطأ من خدمات جوجل: كود ${e.statusCode}\n\nيحدث هذا عادة عندما لا تتوفر خدمات Google Play بشكل كامل على محاكي التطوير، أو عند عدم تطابق مفتاح التوقيع SHA-1 للتطبيق مع لوحة تحكم Firebase."
                setupDialogErrorMessage = msg
                showSetupDialog = true
            }
        } else {
            isLoading = false
            Toast.makeText(context, "تم إلغاء عملية تسجيل الدخول من قبل المستخدم", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundDark)
                .drawBehind {
                    // Draw a soft glowing radial gradient for primary color in top-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryIndigo.copy(alpha = 0.2f), Color.Transparent),
                            center = Offset(x = 0f, y = 0f),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(x = 0f, y = 0f)
                    )
                    
                    // Draw a soft glowing radial gradient for success color in bottom-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(SuccessColor.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(x = size.width, y = size.height),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(x = size.width, y = size.height)
                    )

                    // Draw subtle orbital rings to represent networks and orbits of learning
                    drawCircle(
                        color = PrimaryIndigoLight.copy(alpha = 0.04f),
                        radius = size.width * 0.45f,
                        center = Offset(x = size.width * 0.5f, y = size.height * 0.2f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = PrimaryIndigoLight.copy(alpha = 0.02f),
                        radius = size.width * 0.65f,
                        center = Offset(x = size.width * 0.5f, y = size.height * 0.2f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    // Draw abstract data flow connections
                    val path1 = Path().apply {
                        moveTo(0f, size.height * 0.4f)
                        cubicTo(
                            size.width * 0.25f, size.height * 0.35f,
                            size.width * 0.5f, size.height * 0.5f,
                            size.width, size.height * 0.45f
                        )
                    }
                    drawPath(
                        path = path1,
                        color = PrimaryIndigoLight.copy(alpha = 0.03f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    val path2 = Path().apply {
                        moveTo(0f, size.height * 0.65f)
                        cubicTo(
                            size.width * 0.35f, size.height * 0.75f,
                            size.width * 0.7f, size.height * 0.55f,
                            size.width, size.height * 0.7f
                        )
                    }
                    drawPath(
                        path = path2,
                        color = SuccessColorLight.copy(alpha = 0.02f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Logo Accent
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(PrimaryIndigo.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .border(1.dp, PrimaryIndigoLight.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "مرحباً بك في Nexora",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "نظام الإدارة الحديث والذكي لمركزك التعليمي",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Credentials Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(24.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark,
                            focusedLabelColor = PrimaryIndigoLight,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark,
                            focusedLabelColor = PrimaryIndigoLight,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (email.isEmpty() || password.isEmpty()) {
                                Toast.makeText(context, "الرجاء إدخال البريد الإلكتروني وكلمة المرور", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!com.example.util.ValidationUtils.isValidAndTrustedEmail(email)) {
                                Toast.makeText(context, "البريد الإلكتروني غير صالح أو غير موثوق", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            isLoading = true
                            auth?.signInWithEmailAndPassword(email.trim(), password)
                                ?.addOnCompleteListener { taskResult ->
                                    if (taskResult.isSuccessful) {
                                        val user = taskResult.result?.user
                                        if (user != null) {
                                            isLoading = false
                                            Toast.makeText(context, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, "خطأ في بيانات المستخدم", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "فشل تسجيل الدخول: ${taskResult.exception?.localizedMessage ?: "تأكد من صحة البيانات"}", Toast.LENGTH_LONG).show()
                                    }
                                } ?: run {
                                isLoading = false
                                Toast.makeText(context, "خطأ: نظام المصادقة السحابي غير متصل أو غير متاح حالياً.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                "دخول للنظام",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                forgotPasswordEmail = email
                                showForgotPasswordDialog = true
                            }
                        ) {
                            Text(
                                "نسيت كلمة المرور؟",
                                color = PrimaryIndigoLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = {
                                if (email.isEmpty()) {
                                    Toast.makeText(context, "الرجاء إدخال البريد الإلكتروني أولاً لإعادة إرسال التفعيل", Toast.LENGTH_LONG).show()
                                    return@TextButton
                                }
                                isLoading = true
                                auth?.signInWithEmailAndPassword(email.trim(), password)
                                    ?.addOnCompleteListener { taskResult ->
                                        if (taskResult.isSuccessful) {
                                            val user = taskResult.result?.user
                                            user?.sendEmailVerification()?.addOnCompleteListener { resendTask ->
                                                isLoading = false
                                                auth.signOut()
                                                if (resendTask.isSuccessful) {
                                                    Toast.makeText(context, "تم إعادة إرسال رابط التفعيل إلى بريدك الإلكتروني بنجاح.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "فشل إرسال رابط التفعيل: ${resendTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, "تأكد من صحة البريد الإلكتروني وكلمة المرور لإعادة إرسال التفعيل.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        ) {
                            Text(
                                "إعادة إرسال بريد التفعيل",
                                color = PrimaryIndigoLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social Division Line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                    Text("أو عبر الخدمات السحابية", color = TextTertiary, fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Google Sign In Button
                Button(
                    onClick = {
                        isLoading = true
                        try {
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        } catch (e: Exception) {
                            isLoading = false
                            setupDialogErrorMessage = "لا يمكن بدء عملية تسجيل الدخول بجوجل:\n${e.localizedMessage}\n\nيحدث هذا غالباً لعدم تثبيت خدمات Google Play على المحاكي، أو خطأ إعدادات Firebase."
                            showSetupDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Drawing google-colored G logo as standard Vector Icon fallback
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

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        "ليس لديك حساب؟ سجل الآن",
                        color = PrimaryIndigoLight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToParentPortal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("navigate_to_parent_portal_btn"),
                    border = BorderStroke(1.dp, SuccessColorLight.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessColorLight)
                ) {
                    Icon(
                        imageVector = Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        tint = SuccessColorLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "بوابة المتابعة لأولياء الأمور 👨‍👩‍👦",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Google Firebase Setup Instructions Dialog
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

            // Forgot Password Restoration Dialog
            if (showForgotPasswordDialog) {
                Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
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
                                    .background(PrimaryIndigo.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(28.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "استعادة كلمة المرور",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "أدخل بريدك الإلكتروني المسجل وسنقوم بإرسال رابط لإعادة تعيين كلمة المرور الخاصة بك.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(
                                value = forgotPasswordEmail,
                                onValueChange = { forgotPasswordEmail = it },
                                label = { Text("البريد الإلكتروني") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = BackgroundDark,
                                    unfocusedContainerColor = BackgroundDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (forgotPasswordEmail.isEmpty()) {
                                        Toast.makeText(context, "الرجاء إدخال البريد الإلكتروني", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!com.example.util.ValidationUtils.isValidAndTrustedEmail(forgotPasswordEmail)) {
                                        Toast.makeText(context, "البريد الإلكتروني غير صالح أو غير موثوق", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    isResettingPassword = true
                                    auth?.sendPasswordResetEmail(forgotPasswordEmail.trim())
                                        ?.addOnCompleteListener { task ->
                                            isResettingPassword = false
                                            if (task.isSuccessful) {
                                                Toast.makeText(context, "تم إرسال بريد استعادة كلمة المرور بنجاح. يرجى التحقق من صندوق الوارد الخاص بك.", Toast.LENGTH_LONG).show()
                                                showForgotPasswordDialog = false
                                            } else {
                                                Toast.makeText(context, "فشل إرسال البريد: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        } ?: run {
                                        isResettingPassword = false
                                        Toast.makeText(context, "خطأ: نظام المصادقة غير متصل حالياً.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isResettingPassword) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("إرسال رابط استعادة كلمة المرور", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(onClick = { showForgotPasswordDialog = false }, modifier = Modifier.fillMaxWidth()) {
                                Text("إلغاء", color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FirebaseInstructionsDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onSkipToDemo: () -> Unit
) {
    val context = LocalContext.current
    val sha1 = remember(context) { com.example.util.SignatureUtils.getCertificateSHA1(context) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
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
fun SetupStepItem(number: String, text: String) {
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
