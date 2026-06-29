package com.example.ui.auth

import android.widget.Toast
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Safe Firebase Auth initialization
    val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
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
                    // Soft glowing radial success green in top-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(SuccessColor.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(x = 0f, y = 0f),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(x = 0f, y = 0f)
                    )
                    
                    // Soft glowing radial primary violet/indigo in bottom-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryIndigo.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(x = size.width, y = size.height),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(x = size.width, y = size.height)
                    )

                    // Subtle geometric grid accent for modern visual depth
                    drawCircle(
                        color = SuccessColorLight.copy(alpha = 0.03f),
                        radius = size.width * 0.4f,
                        center = Offset(x = size.width * 0.5f, y = size.height * 0.2f),
                        style = Stroke(width = 1.dp.toPx())
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
                // Header Icon Accent
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(SuccessColor.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .border(1.dp, SuccessColorLight.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AppRegistration,
                        contentDescription = null,
                        tint = SuccessColorLight,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "إنشاء حساب جديد",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "سجل الآن وابدأ في تنظيم الحصص والمعلمين والطلاب",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Input Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(24.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم بالكامل") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SuccessColor,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark,
                            focusedLabelColor = SuccessColorLight,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SuccessColor,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark,
                            focusedLabelColor = SuccessColorLight,
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
                            focusedBorderColor = SuccessColor,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark,
                            focusedLabelColor = SuccessColorLight,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                                Toast.makeText(context, "الرجاء تعبئة جميع الحقول", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!com.example.util.ValidationUtils.isValidAndTrustedEmail(email)) {
                                Toast.makeText(context, "البريد الإلكتروني المدخل غير صالح أو غير موثوق (الرجاء عدم استخدام بريد مؤقت)", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (password.length < 6) {
                                Toast.makeText(context, "كلمة المرور يجب أن لا تقل عن 6 أحرف", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            auth?.createUserWithEmailAndPassword(email.trim(), password)
                                ?.addOnCompleteListener { taskResult ->
                                    isLoading = false
                                    if (taskResult.isSuccessful) {
                                        val user = taskResult.result?.user
                                        user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                                            if (emailTask.isSuccessful) {
                                                Toast.makeText(context, "تم تسجيل الحساب بنجاح! تم إرسال رابط التحقق إلى بريدك الإلكتروني. يرجى تفعيل الحساب قبل تسجيل الدخول.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "تم تسجيل الحساب ولكن فشل إرسال بريد التحقق: ${emailTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        onRegisterSuccess()
                                    } else {
                                        Toast.makeText(context, "فشل تسجيل الحساب: ${taskResult.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                } ?: run {
                                isLoading = false
                                Toast.makeText(context, "خطأ: نظام المصادقة السحابي غير متصل أو غير مهيأ حالياً.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                "إنشاء الحساب والبدء",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        "لديك حساب بالفعل؟ سجل دخول الآن",
                        color = SuccessColorLight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
