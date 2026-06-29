package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.viewmodel.AppViewModel
import com.example.ui.auth.ChooseSystemScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RegisterScreen
import com.example.ui.center.CenterDashboardScreen
import com.example.ui.teacher.TeacherDashboardScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHOOSE_SYSTEM = "choose_system"
    const val CENTER_DASHBOARD = "center_dashboard"
    const val TEACHER_DASHBOARD = "teacher_dashboard"
    const val PARENT_DASHBOARD = "parent_dashboard"
    const val PARENT_LOGIN = "parent_login"
    const val PARENT_REGISTER = "parent_register"
    const val PARENT_PORTAL = "parent_portal"
    const val AI_ASSISTANT = "ai_assistant"
    const val COMMUNICATION_CENTER = "communication_center"
    const val SCHEDULE = "schedule"
    const val CLASSROOMS = "classrooms"
    const val CENTER_SCHEDULE = "center_schedule"
    const val USER_MANAGEMENT = "user_management"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = viewModel()
) {
    val auth = try {
        com.google.firebase.auth.FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val userRbac by com.example.util.rbac.RbacManager.currentUserRbac.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var isChangingPassword by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (userRbac != null) {
        if (!userRbac!!.isActive) {
            // Block everything with deactivated account overlay
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "تعطيل الحساب",
                            tint = ErrorColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "الحساب معطل",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "تم تعطيل حسابك من قبل إدارة النظام. يرجى مراجعة مالك السنتر أو المسؤول لاسترداد الصلاحيات.",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                try {
                                    auth?.signOut()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تسجيل الخروج", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (userRbac!!.isTempPasswordActive) {
            // Dialog forcing Password Change on first login
            var newPassword by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var errorMsg by remember { mutableStateOf("") }
            
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "تغيير كلمة المرور",
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "تحديث كلمة المرور",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هذا هو أول دخول لك للنظام. يرجى اختيار كلمة مرور جديدة وخاصة لتأمين حسابك.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; errorMsg = "" },
                            label = { Text("كلمة المرور الجديدة") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = PrimaryIndigoLight,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMsg = "" },
                            label = { Text("تأكيد كلمة المرور الجديدة") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = PrimaryIndigoLight,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        
                        if (errorMsg.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMsg,
                                color = ErrorColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = {
                                if (newPassword.length < 6) {
                                    errorMsg = "يجب أن تكون كلمة المرور 6 أحرف على الأقل"
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    errorMsg = "كلمات المرور غير متطابقة"
                                    return@Button
                                }
                                
                                isChangingPassword = true
                                scope.launch {
                                    try {
                                        val firebaseUser = auth?.currentUser
                                        if (firebaseUser != null) {
                                            firebaseUser.updatePassword(newPassword).await()
                                            
                                            // Update UserRbac document
                                            val updatedRbac = userRbac!!.copy(
                                                isTempPasswordActive = false,
                                                tempPassword = ""
                                            )
                                            val success = com.example.util.rbac.RbacManager.updateUserRbac(updatedRbac)
                                            if (success) {
                                                com.example.util.ActivityLogManager.logAction(
                                                    "تغيير كلمة المرور",
                                                    "قام المستخدم ${updatedRbac.name} بتحديث كلمة المرور المؤقتة بنجاح"
                                                )
                                                isChangingPassword = false
                                            } else {
                                                errorMsg = "حدث خطأ أثناء تحديث الصلاحيات في السحابة"
                                                isChangingPassword = false
                                            }
                                        } else {
                                            errorMsg = "المستخدم غير مصادق عليه"
                                            isChangingPassword = false
                                        }
                                    } catch (e: Exception) {
                                        errorMsg = e.localizedMessage ?: "فشل تحديث كلمة المرور في النظام"
                                        isChangingPassword = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isChangingPassword
                        ) {
                            if (isChangingPassword) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("حفظ كلمة المرور الجديدة", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dynamic system navigation handler for startup, login, registration, and live setting changes
    val systemType = profile?.systemType
    LaunchedEffect(auth?.currentUser, systemType, userRbac) {
        val currentUser = auth?.currentUser
        
        if (currentUser != null && userRbac != null && userRbac?.role == com.example.data.model.AccountType.PARENT) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != Routes.PARENT_DASHBOARD && currentRoute != Routes.PARENT_LOGIN && currentRoute != Routes.PARENT_REGISTER) {
                navController.navigate(Routes.PARENT_DASHBOARD) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            val rbac = userRbac
            if (currentUser != null && rbac != null) {
                val rbacRole = rbac.role
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                
                // If they are on Login, Register, or Choose System, auto-direct them to the right place
                if (currentRoute == Routes.LOGIN || currentRoute == Routes.REGISTER || currentRoute == Routes.CHOOSE_SYSTEM) {
                    val targetRoute = when {
                        rbacRole == com.example.data.model.AccountType.TEACHER -> Routes.TEACHER_DASHBOARD
                        rbacRole == com.example.data.model.AccountType.SECRETARY || rbacRole == com.example.data.model.AccountType.ADMIN -> Routes.CENTER_DASHBOARD
                        rbacRole == com.example.data.model.AccountType.OWNER -> {
                            val sysType = systemType ?: "not_chosen"
                            if (sysType == "not_chosen") {
                                Routes.CHOOSE_SYSTEM
                            } else {
                                if (sysType == "teacher") Routes.TEACHER_DASHBOARD else Routes.CENTER_DASHBOARD
                            }
                        }
                        else -> null
                    }
                    if (targetRoute != null && currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                } else if (currentRoute != Routes.PARENT_DASHBOARD) {
                    // If they are already in the app and change the setting in Settings tab, switch dashboards instantly!
                    val targetRoute = when {
                        rbacRole == com.example.data.model.AccountType.TEACHER -> Routes.TEACHER_DASHBOARD
                        rbacRole == com.example.data.model.AccountType.SECRETARY || rbacRole == com.example.data.model.AccountType.ADMIN -> Routes.CENTER_DASHBOARD
                        else -> {
                            val sysType = systemType ?: "not_chosen"
                            if (sysType == "teacher") Routes.TEACHER_DASHBOARD else Routes.CENTER_DASHBOARD
                        }
                    }
                    if (currentRoute != targetRoute && systemType != "not_chosen") {
                        navController.navigate(targetRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToParentPortal = { navController.navigate(Routes.PARENT_LOGIN) },
                onLoginSuccess = {
                    viewModel.syncData()
                    
                    val rbacRole = com.example.util.rbac.RbacManager.currentUserRbac.value?.role
                    val targetRoute = when {
                        rbacRole == com.example.data.model.AccountType.TEACHER -> Routes.TEACHER_DASHBOARD
                        rbacRole == com.example.data.model.AccountType.SECRETARY || rbacRole == com.example.data.model.AccountType.ADMIN -> Routes.CENTER_DASHBOARD
                        else -> {
                            val currentType = profile?.systemType ?: "not_chosen"
                            if (currentType == "not_chosen") {
                                Routes.CHOOSE_SYSTEM
                            } else {
                                if (currentType == "teacher") Routes.TEACHER_DASHBOARD else Routes.CENTER_DASHBOARD
                            }
                        }
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    try {
                        auth?.signOut()
                    } catch (e: Exception) {}
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CHOOSE_SYSTEM) {
            ChooseSystemScreen(
                onCenterSelected = {
                    navController.navigate(Routes.CENTER_DASHBOARD) {
                        popUpTo(Routes.CHOOSE_SYSTEM) { inclusive = true }
                    }
                },
                onTeacherSelected = {
                    navController.navigate(Routes.TEACHER_DASHBOARD) {
                        popUpTo(Routes.CHOOSE_SYSTEM) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    try {
                        auth?.signOut()
                    } catch (e: Exception) {}
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.CHOOSE_SYSTEM) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }
        composable(Routes.CENTER_DASHBOARD) {
            CenterDashboardScreen(
                onLogout = {
                    viewModel.clearAllLocalData {
                        try {
                            auth?.signOut()
                        } catch (e: Exception) {}
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }
                },
                viewModel = viewModel,
                onAiAssistantClick = { navController.navigate(Routes.AI_ASSISTANT) },
                onCommunicationCenterClick = { navController.navigate(Routes.COMMUNICATION_CENTER) },
                onNavigateToClassrooms = { navController.navigate(Routes.CLASSROOMS) },
                onNavigateToCenterSchedule = { navController.navigate(Routes.CENTER_SCHEDULE) },
                onNavigateToUserManagement = { navController.navigate(Routes.USER_MANAGEMENT) }
            )
        }
        composable(Routes.TEACHER_DASHBOARD) {
            TeacherDashboardScreen(
                onLogout = {
                    viewModel.clearAllLocalData {
                        try {
                            auth?.signOut()
                        } catch (e: Exception) {}
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }
                },
                viewModel = viewModel,
                onAiAssistantClick = { navController.navigate(Routes.AI_ASSISTANT) },
                onNavigateToSchedule = { navController.navigate(Routes.SCHEDULE) }
            )
        }
        composable(Routes.PARENT_LOGIN) {
            com.example.ui.parent.ParentLoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.PARENT_REGISTER) },
                onBackClick = { navController.popBackStack() },
                onLoginSuccess = {
                    viewModel.syncData()
                    navController.navigate(Routes.PARENT_DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }
        composable(Routes.PARENT_REGISTER) {
            com.example.ui.parent.ParentRegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    try {
                        auth?.signOut()
                    } catch (e: Exception) {}
                    navController.navigate(Routes.PARENT_LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }
        composable(Routes.PARENT_DASHBOARD) {
            com.example.ui.parent.ParentDashboardScreen(
                onLogout = {
                    viewModel.clearAllLocalData {
                        try {
                            auth?.signOut()
                        } catch (e: Exception) {}
                        com.example.ui.parent.ParentSessionManager.clearSession(context)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }
                },
                viewModel = viewModel
            )
        }
        composable(Routes.PARENT_PORTAL + "?studentId={studentId}") { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId")
            com.example.ui.academic.ParentPortalScreen(
                initialStudentId = studentId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.AI_ASSISTANT) {
            com.example.ui.reports.AiAssistantScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.COMMUNICATION_CENTER) {
            com.example.ui.center.CommunicationCenterScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.SCHEDULE) {
            com.example.ui.schedule.ScheduleScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.CLASSROOMS) {
            com.example.ui.schedule.ClassroomsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.CENTER_SCHEDULE) {
            com.example.ui.schedule.CenterScheduleScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.USER_MANAGEMENT) {
            com.example.ui.rbac.UserManagementScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
