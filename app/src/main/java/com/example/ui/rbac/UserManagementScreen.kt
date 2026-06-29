package com.example.ui.rbac

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.AccountType
import com.example.data.model.Permission
import com.example.data.model.UserRbac
import com.example.ui.theme.*
import com.example.util.rbac.RbacDefaults
import com.example.util.rbac.RbacManager
import com.example.data.model.RemoteActivityLog
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var usersList by remember { mutableStateOf<List<UserRbac>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Users, 1 = Remote Logs

    // Dialog & Editing state
    var selectedUser by remember { mutableStateOf<UserRbac?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }

    // Fetch user list helper
    val fetchUsers = {
        isLoading = true
        coroutineScope.launch {
            usersList = RbacManager.getAllUsers()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        fetchUsers()
    }

    val filteredUsers = remember(usersList, searchQuery) {
        usersList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "إدارة المستخدمين والصلاحيات",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الرجوع",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { fetchUsers() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        floatingActionButton = {
            if (activeSubTab == 0) {
                FloatingActionButton(
                    onClick = { showAddUserDialog = true },
                    containerColor = PrimaryIndigo,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "إضافة مستخدم")
                }
            }
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Sub-Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(SurfaceDark, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeSubTab == 0) PrimaryIndigo else Color.Transparent)
                            .clickable { activeSubTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "الموظفون والصلاحيات",
                            color = if (activeSubTab == 0) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeSubTab == 1) PrimaryIndigo else Color.Transparent)
                            .clickable { activeSubTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "سجل النشاطات السحابي",
                            color = if (activeSubTab == 1) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (activeSubTab == 0) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث باسم المستخدم أو البريد الإلكتروني...", color = TextTertiary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigoLight)
                    }
                } else if (filteredUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "لا يوجد مستخدمون حالياً",
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "اضغط على زر الإضافة بالأسفل لإنشاء مستخدم جديد وصلاحيات مخصصة له.",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredUsers) { user ->
                            UserRbacCard(
                                user = user,
                                onClick = {
                                    selectedUser = user
                                    showEditDialog = true
                                }
                            )
                        }
                    }
                }
            } else {
                RemoteActivityLogTab()
            }
        }

            // Edit User Dialog
            if (showEditDialog && selectedUser != null) {
                EditUserRbacDialog(
                    user = selectedUser!!,
                    onDismiss = {
                        showEditDialog = false
                        selectedUser = null
                    },
                    onSave = { updatedUser ->
                        coroutineScope.launch {
                            val success = RbacManager.updateUserRbac(updatedUser)
                            if (success) {
                                Toast.makeText(context, "تم حفظ تعديلات المستخدم والصلاحيات بنجاح!", Toast.LENGTH_SHORT).show()
                                com.example.util.ActivityLogManager.logAction(
                                    "تعديل موظف",
                                    "تم تعديل الموظف: ${updatedUser.name} ودوره ${updatedUser.role.name} وحالته ${if (updatedUser.isActive) "نشط" else "معطل"}"
                                )
                                fetchUsers()
                                showEditDialog = false
                                selectedUser = null
                            } else {
                                Toast.makeText(context, "فشل في تحديث بيانات الصلاحيات.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDelete = { userId ->
                        coroutineScope.launch {
                            val success = RbacManager.deleteUserRbac(userId)
                            if (success) {
                                Toast.makeText(context, "تم حذف المستخدم وصلاحياته بنجاح!", Toast.LENGTH_SHORT).show()
                                com.example.util.ActivityLogManager.logAction(
                                    "حذف موظف",
                                    "تم حذف حساب الموظف صاحب المعرف: $userId"
                                )
                                fetchUsers()
                                showEditDialog = false
                                selectedUser = null
                            } else {
                                Toast.makeText(context, "فشل في حذف المستخدم.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // Add User Dialog
            if (showAddUserDialog) {
                AddUserRbacDialog(
                    onDismiss = { showAddUserDialog = false },
                    onSave = { newUser ->
                        Toast.makeText(context, "تم إضافة الموظف الجديد وصلاحياته بنجاح!", Toast.LENGTH_SHORT).show()
                        fetchUsers()
                        showAddUserDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun UserRbacCard(
    user: UserRbac,
    onClick: () -> Unit
) {
    val roleColor = when (user.role) {
        AccountType.OWNER -> ErrorColorLight
        AccountType.ADMIN -> WarningColor
        AccountType.SECRETARY -> SuccessColorLight
        AccountType.TEACHER -> PrimaryIndigoLight
        AccountType.PARENT -> Color.Gray
    }

    val roleName = when (user.role) {
        AccountType.OWNER -> "مالك النظام (Owner)"
        AccountType.ADMIN -> "مدير (Admin)"
        AccountType.SECRETARY -> "سكرتارية (Secretary)"
        AccountType.TEACHER -> "مدرس (Teacher)"
        AccountType.PARENT -> "ولي أمر (Parent)"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(roleColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (user.role) {
                        AccountType.OWNER -> Icons.Default.AdminPanelSettings
                        AccountType.ADMIN -> Icons.Default.SupervisorAccount
                        AccountType.SECRETARY -> Icons.Default.SupportAgent
                        AccountType.TEACHER -> Icons.Default.School
                        AccountType.PARENT -> Icons.Default.FamilyRestroom
                    },
                    contentDescription = null,
                    tint = roleColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifBlank { "مستخدم بدون اسم" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = user.email.ifBlank { "لا يوجد بريد إلكتروني" },
                    fontSize = 12.sp,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(roleColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = roleName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleColor
                        )
                    }

                    Text(
                        text = "• ${user.permissions.size} صلاحيات نشطة",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = TextTertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserRbacDialog(
    user: UserRbac,
    onDismiss: () -> Unit,
    onSave: (UserRbac) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var isActive by remember { mutableStateOf(user.isActive) }
    var selectedRole by remember { mutableStateOf(user.role) }
    var userPermissions by remember { mutableStateOf(user.permissions.toMutableStateList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعديل مستخدم وصلاحياته",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextTertiary)
                    }
                }

                Divider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الموظف بالكامل") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "حالة الحساب (نشط / معطل)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isActive) "الحساب نشط ويستطيع تسجيل الدخول" else "الحساب معطل وممنوع من تسجيل الدخول",
                                fontSize = 11.sp,
                                color = if (isActive) SuccessColorLight else ErrorColorLight
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SuccessColor,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceLightDark
                            )
                        )
                    }

                    Text(
                        text = "البريد الإلكتروني: ${user.email}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Button(
                        onClick = {
                            com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(user.email)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "تم إرسال رابط إعادة تعيين كلمة المرور إلى البريد الإلكتروني", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "حدث خطأ أثناء إرسال الرابط", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLightDark),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إرسال رابط إعادة تعيين كلمة المرور", color = TextPrimary)
                    }

                    // Role Selection Block
                    Text(
                        text = "نوع الحساب والمسؤولية:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccountType.values().forEach { role ->
                            val isSelected = selectedRole == role
                            val btnColor = when (role) {
                                AccountType.OWNER -> ErrorColorLight
                                AccountType.ADMIN -> WarningColor
                                AccountType.SECRETARY -> SuccessColorLight
                                AccountType.TEACHER -> PrimaryIndigoLight
                                AccountType.PARENT -> Color.Gray
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) btnColor else SurfaceLightDark)
                                    .border(1.dp, if (isSelected) btnColor else BorderColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedRole = role
                                        // Reset permissions to default list for role
                                        userPermissions.clear()
                                        userPermissions.addAll(RbacDefaults.getDefaultPermissionsForRole(role).map { it.name })
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = when (role) {
                                        AccountType.OWNER -> "مالك (Owner)"
                                        AccountType.ADMIN -> "مدير (Admin)"
                                        AccountType.SECRETARY -> "سكرتير"
                                        AccountType.TEACHER -> "مدرس"
                                        AccountType.PARENT -> "ولي أمر"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }

                    Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Permissions list grouped by category
                    Text(
                        text = "صلاحيات الوصول وتفويض الحساب 🔒",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Permission.values().groupBy { it.category }.forEach { (category, perms) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, BorderColor, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundDark.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigoLight,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                perms.forEach { permission ->
                                    val isChecked = userPermissions.contains(permission.name)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) {
                                                    userPermissions.remove(permission.name)
                                                } else {
                                                    userPermissions.add(permission.name)
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = permission.displayName,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    userPermissions.add(permission.name)
                                                } else {
                                                    userPermissions.remove(permission.name)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = PrimaryIndigoLight,
                                                uncheckedColor = TextTertiary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSave(
                                user.copy(
                                    name = name,
                                    phone = phone,
                                    isActive = isActive,
                                    role = selectedRole,
                                    permissions = userPermissions.toList()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ErrorColor.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف المستخدم", tint = ErrorColorLight)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف صلاحيات المستخدم") },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا المستخدم من قائمة الصلاحيات؟ لن يستطيع الوصول إلى أي صفحة إلا بعد إضافة صلاحيات جديدة له.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(user.userId)
                        showDeleteConfirm = false
                    }
                ) {
                    Text("نعم، احذف", color = ErrorColorLight)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserRbacDialog(
    onDismiss: () -> Unit,
    onSave: (UserRbac) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var tempPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(AccountType.SECRETARY) }
    var userPermissions by remember { mutableStateOf(mutableStateListOf<String>()) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    // Synchronize default permissions when role is selected
    LaunchedEffect(selectedRole) {
        userPermissions.clear()
        userPermissions.addAll(RbacDefaults.getDefaultPermissionsForRole(selectedRole).map { it.name })
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إضافة مستخدم جديد وصلاحياته",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextTertiary)
                    }
                }

                Divider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = "" },
                        label = { Text("البريد الإلكتروني للغرض الربط بـ Firebase") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = "" },
                        label = { Text("الاسم بالكامل") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMsg = "" },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it; errorMsg = "" },
                        label = { Text("كلمة المرور المؤقتة (سيُجبر على تغييرها عند أول دخول)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigoLight,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMsg.isNotEmpty()) {
                        Text(
                            text = errorMsg,
                            color = ErrorColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Role Selection Block
                    Text(
                        text = "نوع الحساب والمسؤولية:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccountType.values().forEach { role ->
                            val isSelected = selectedRole == role
                            val btnColor = when (role) {
                                AccountType.OWNER -> ErrorColorLight
                                AccountType.ADMIN -> WarningColor
                                AccountType.SECRETARY -> SuccessColorLight
                                AccountType.TEACHER -> PrimaryIndigoLight
                                AccountType.PARENT -> Color.Gray
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) btnColor else SurfaceLightDark)
                                    .border(1.dp, if (isSelected) btnColor else BorderColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedRole = role
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = when (role) {
                                        AccountType.OWNER -> "مالك (Owner)"
                                        AccountType.ADMIN -> "مدير (Admin)"
                                        AccountType.SECRETARY -> "سكرتير"
                                        AccountType.TEACHER -> "مدرس"
                                        AccountType.PARENT -> "ولي أمر"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }

                    Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Permissions list grouped by category
                    Text(
                        text = "صلاحيات الوصول وتفويض الحساب 🔒",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Permission.values().groupBy { it.category }.forEach { (category, perms) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, BorderColor, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundDark.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigoLight,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                perms.forEach { permission ->
                                    val isChecked = userPermissions.contains(permission.name)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) {
                                                    userPermissions.remove(permission.name)
                                                } else {
                                                    userPermissions.add(permission.name)
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = permission.displayName,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    userPermissions.add(permission.name)
                                                } else {
                                                    userPermissions.remove(permission.name)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = PrimaryIndigoLight,
                                                uncheckedColor = TextTertiary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (email.isBlank() || name.isBlank() || tempPassword.isBlank()) {
                                errorMsg = "يرجى ملء جميع الحقول المطلوبة بما في ذلك كلمة المرور المؤقتة"
                                return@Button
                            }
                            if (tempPassword.length < 6) {
                                errorMsg = "كلمة المرور المؤقتة يجب أن تكون 6 أحرف على الأقل"
                                return@Button
                            }
                            
                            isSaving = true
                            errorMsg = ""
                            coroutineScope.launch {
                                try {
                                    val mainApp = com.google.firebase.FirebaseApp.getInstance()
                                    val apiKey = mainApp.options.apiKey
                                    if (apiKey.isNullOrEmpty()) {
                                        errorMsg = "تعذر العثور على مفتاح API في إعدادات Firebase"
                                        return@launch
                                    }
                                    
                                    val registeredUid = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        val url = java.net.URL("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey")
                                        val connection = url.openConnection() as java.net.HttpURLConnection
                                        connection.requestMethod = "POST"
                                        connection.setRequestProperty("Content-Type", "application/json")
                                        connection.doOutput = true
                                        
                                        val payload = org.json.JSONObject().apply {
                                            put("email", email.trim())
                                            put("password", tempPassword)
                                            put("returnSecureToken", true)
                                        }.toString()
                                        
                                        connection.outputStream.use { os ->
                                            val input = payload.toByteArray(Charsets.UTF_8)
                                            os.write(input, 0, input.size)
                                        }
                                        
                                        if (connection.responseCode == 200) {
                                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                                            android.util.Log.d("UserMgmt", "Firebase Auth creation SUCCESS")
                                            val json = org.json.JSONObject(response)
                                            json.optString("localId", null) as String?
                                        } else {
                                            val errorResponse = try { connection.errorStream.bufferedReader().use { it.readText() } } catch(e: Exception) { "" }
                                            android.util.Log.e("UserMgmt", "Firebase Auth creation FAILED: $errorResponse")
                                            var friendlyMsg = "تعذر إتمام التسجيل"
                                            try {
                                                val errJson = org.json.JSONObject(errorResponse).getJSONObject("error")
                                                val msg = errJson.getString("message")
                                                friendlyMsg = when {
                                                    msg.contains("EMAIL_EXISTS") -> "البريد الإلكتروني مسجل بالفعل"
                                                    msg.contains("WEAK_PASSWORD") -> "كلمة المرور ضعيفة جداً"
                                                    msg.contains("INVALID_EMAIL") -> "صيغة البريد الإلكتروني غير صحيحة"
                                                    msg.contains("OPERATION_NOT_ALLOWED") -> "خاصية التسجيل بالبريد غير مفعلة في إعدادات Firebase"
                                                    msg.contains("TOO_MANY_ATTEMPTS") -> "محاولات كثيرة، يرجى المحاولة لاحقاً"
                                                    else -> "خطأ: $msg"
                                                }
                                            } catch (e: Exception) {
                                                friendlyMsg = "فشل التسجيل: تحقق من الاتصال بالإنترنت"
                                            }
                                            throw Exception(friendlyMsg)
                                        }
                                    }
                                    
                                    if (registeredUid != null) {
                                        android.util.Log.d("UserMgmt", "Firebase Auth successful, creating Firestore doc for uid: $registeredUid")
                                        // 3. Save UserRbac document to Firestore with temp password flags
                                        val newUser = UserRbac(
                                            userId = registeredUid,
                                            email = email.trim(),
                                            name = name.trim(),
                                            phone = phone.trim(),
                                            role = selectedRole,
                                            permissions = userPermissions.toList(),
                                            isActive = true,
                                            isTempPasswordActive = true,
                                            tempPassword = tempPassword,
                                            centerId = RbacManager.currentUserRbac.value?.centerId?.ifEmpty { null }
                                                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                        )
                                        
                                        val rbacSuccess = RbacManager.updateUserRbac(newUser)
                                        if (rbacSuccess) {
                                            android.util.Log.d("UserMgmt", "Firestore document created successfully")
                                            // Log the action (Requirement: من أضاف موظف)
                                            com.example.util.ActivityLogManager.logAction(
                                                "إنشاء موظف",
                                                "تم إنشاء حساب الموظف الجديد: \${newUser.name} ودوره \${newUser.role.name}"
                                            )
                                            
                                            onSave(newUser)
                                        } else {
                                            android.util.Log.e("UserMgmt", "Failed to create Firestore document")
                                            errorMsg = "تم إنشاء الحساب لكن فشل حفظ الصلاحيات في السحابة"
                                        }
                                    } else {
                                        android.util.Log.e("UserMgmt", "Registered UID is null")
                                        errorMsg = "فشل في تسجيل الموظف بالخادم (UID is null)"
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("UserMgmt", "Exception during user creation", e)
                                    errorMsg = e.localizedMessage ?: "حدث خطأ غير متوقع أثناء تسجيل الحساب"
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("إضافة المستخدم", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        enabled = !isSaving
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteActivityLogTab() {
    var logs by remember { mutableStateOf<List<RemoteActivityLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val fetchLogs = {
        isLoading = true
        coroutineScope.launch {
            logs = com.example.util.ActivityLogManager.getAllLogs()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        fetchLogs()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "أحدث 200 عملية مسجلة بالسحابة 🔒",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            IconButton(onClick = { fetchLogs() }) {
                Icon(Icons.Default.Refresh, contentDescription = "تحديث السجلات", tint = PrimaryIndigoLight)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryIndigoLight)
            }
        } else if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد نشاطات مسجلة حالياً.", color = TextTertiary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, BorderColor, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val chipBg = when (log.action) {
                                    "تسجيل الدخول" -> SuccessColor.copy(alpha = 0.15f)
                                    "إضافة طالب", "إضافة مجموعة", "إنشاء موظف" -> PrimaryIndigo.copy(alpha = 0.15f)
                                    "حذف طالب", "حذف دفعة", "حذف مجموعة", "حذف موظف" -> ErrorColor.copy(alpha = 0.15f)
                                    "تعديل دفعة", "تعديل طالب", "تعديل موظف", "تغيير كلمة المرور" -> WarningColor.copy(alpha = 0.15f)
                                    else -> SurfaceLightDark
                                }
                                val chipText = when (log.action) {
                                    "تسجيل الدخول" -> SuccessColorLight
                                    "إضافة طالب", "إضافة مجموعة", "إنشاء موظف" -> PrimaryIndigoLight
                                    "حذف طالب", "حذف دفعة", "حذف مجموعة", "حذف موظف" -> ErrorColorLight
                                    "تعديل دفعة", "تعديل طالب", "تعديل موظف", "تغيير كلمة المرور" -> WarningColor
                                    else -> TextPrimary
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(chipBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = log.action, color = chipText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                val sdf = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale.getDefault())
                                Text(
                                    text = sdf.format(java.util.Date(log.timestamp)),
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = log.details,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "المنفّذ: ${log.userName} (${log.userEmail})",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
