package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseSystemScreen(
    onCenterSelected: () -> Unit,
    onTeacherSelected: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AppViewModel = viewModel()
) {
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الرجوع لتسجيل الدخول",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundDark)
                .drawBehind {
                    // Soft glowing radial violet light in the top-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryIndigo.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(x = size.width, y = 0f),
                            radius = size.width * 0.9f
                        ),
                        radius = size.width * 0.9f,
                        center = Offset(x = size.width, y = 0f)
                    )
                    
                    // Soft emerald green in the bottom-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(SuccessColor.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(x = 0f, y = size.height),
                            radius = size.width * 0.9f
                        ),
                        radius = size.width * 0.9f,
                        center = Offset(x = 0f, y = size.height)
                    )

                    // Clean technological circular layout
                    drawCircle(
                        color = PrimaryIndigoLight.copy(alpha = 0.05f),
                        radius = size.width * 0.5f,
                        center = Offset(x = size.width * 0.5f, y = size.height * 0.15f),
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
                // Large Header Icon Accent
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(PrimaryIndigo.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                        .border(1.dp, PrimaryIndigoLight.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "تخصيص واجهة النظام",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "اختر طبيعة العمل المناسبة لإدارة مركزك أو فصولك التعليمية بذكاء وسهولة",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(44.dp))

                // Modern Double Selection Options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Option 1: Center Mode
                    Card(
                        onClick = {
                            viewModel.setSystemType("center")
                            onCenterSelected()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(PrimaryIndigo.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                                    .border(1.dp, PrimaryIndigoLight.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "سنتر تعليمي",
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "نظام السنتر التعليمي",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "إدارة شاملة للمجموعات، المعلمين المتعددين، كشوفات الطلاب، والعمليات المالية والأرباح.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Option 2: Teacher Mode
                    Card(
                        onClick = {
                            viewModel.setSystemType("teacher")
                            onTeacherSelected()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(SuccessColor.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                                    .border(1.dp, SuccessColorLight.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "معلم فردي",
                                    tint = SuccessColorLight,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "نظام المعلم الفردي",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "مخصص للمدرس الفردي لمتابعة تلاميذه المباشرين، تحصيل الاشتراكات، وحضور وغياب المجموعات الخاصة.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "يمكنك دائماً تغيير هذا الاختيار لاحقاً من شاشة الإعدادات الخاصة بك.",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
