package com.example.ui.academic

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.viewmodel.AppViewModel
import com.example.ui.theme.*
import com.example.util.QrCodeAnalyzer
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanResult by viewModel.qrScanResult.collectAsState()

    var isScanningActive by remember { mutableStateOf(true) }
    var countdown by remember { mutableStateOf(3) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    // Handle scan result state transition and auto-resume
    LaunchedEffect(scanResult) {
        if (scanResult !is AppViewModel.QrScanResult.Idle) {
            isScanningActive = false
            countdown = 3
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            viewModel.resetQrScanResult()
            isScanningActive = true
        }
    }

    // Reset scan result state on screen launch/dispose
    DisposableEffect(Unit) {
        viewModel.resetQrScanResult()
        isScanningActive = true
        onDispose {
            viewModel.resetQrScanResult()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "مسح رمز الحضور (QR)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                // Polished Permission Prompt
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDark)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.widthIn(max = 400.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "طلب إذن الكاميرا",
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "مطلوب إذن الكاميرا لمسح رمز QR",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "يرجى منح التطبيق إذن استخدام الكاميرا حتى تتمكن من مسح الرموز وتسجيل الحضور تلقائياً ومنع التكرار.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("grant_permission_button")
                        ) {
                            Text(
                                "السماح باستخدام الكاميرا",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Camera and Finder Mask View
                val previewView = remember { PreviewView(context) }
                val executor = remember { Executors.newSingleThreadExecutor() }

                DisposableEffect(hasCameraPermission) {
                    if (hasCameraPermission) {
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build().also {
                                    it.setAnalyzer(executor, QrCodeAnalyzer { qrText ->
                                        if (isScanningActive) {
                                            viewModel.processScannedQrCode(qrText)
                                        }
                                    })
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (exc: Exception) {
                                exc.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                    onDispose {
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProvider.unbindAll()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        executor.shutdown()
                    }
                }

                // Camera Frame container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()

                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val boxSize = 260.dp.toPx()

                            val left = (canvasWidth - boxSize) / 2
                            val top = (canvasHeight - boxSize) / 2
                            val right = left + boxSize
                            val bottom = top + boxSize

                            val path = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        rect = Rect(left, top, right, bottom),
                                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                                    )
                                )
                            }

                            clipPath(path = path, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    size = size
                                )
                            }
                        }
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Target Viewfinder Frame and Scan Line
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .border(2.dp, PrimaryIndigo.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "scan_bar")
                            val scanY by infiniteTransition.animateFloat(
                                initialValue = 0.05f,
                                targetValue = 0.95f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scan_bar_y"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.TopCenter)
                                    .offset(y = 260.dp * scanY)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                SuccessColor,
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // Content Feedback Area (Top text guidance & bottom overlay state feedback)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Instruction Guidance Text
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BackgroundDark.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "وجه الكاميرا نحو رمز QR للطالب لتسجيل الحضور",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Bottom feedback cards based on Scan State Result
                        AnimatedContent(
                            targetState = scanResult,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> height } + fadeOut()
                            },
                            label = "scan_feedback_card"
                        ) { result ->
                            when (result) {
                                is AppViewModel.QrScanResult.Idle -> {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = BackgroundDark.copy(alpha = 0.9f),
                                        border = BorderStroke(1.dp, BorderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = PrimaryIndigo,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "بانتظار مسح رمز QR...",
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "يتم التحقق والتسجيل فوراً عند رصد الرمز",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                is AppViewModel.QrScanResult.Success -> {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = SurfaceDark,
                                        border = BorderStroke(2.dp, SuccessColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .testTag("success_feedback_card")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "نجاح",
                                                tint = SuccessColor,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = result.studentName,
                                                color = TextPrimary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = result.message,
                                                color = SuccessColorLight,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            LinearProgressIndicator(
                                                progress = { countdown / 3.0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(CircleShape),
                                                color = SuccessColor,
                                                trackColor = BorderColor
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "جاري الاستئناف التلقائي خلال $countdown ثوانٍ...",
                                                color = TextTertiary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                is AppViewModel.QrScanResult.AlreadyScanned -> {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = SurfaceDark,
                                        border = BorderStroke(2.dp, WarningColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .testTag("already_scanned_feedback_card")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "مسجل مسبقاً",
                                                tint = WarningColor,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = result.studentName,
                                                color = TextPrimary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = result.message,
                                                color = WarningColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            LinearProgressIndicator(
                                                progress = { countdown / 3.0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(CircleShape),
                                                color = WarningColor,
                                                trackColor = BorderColor
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "جاري الاستئناف التلقائي خلال $countdown ثوانٍ...",
                                                color = TextTertiary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                is AppViewModel.QrScanResult.Error -> {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = SurfaceDark,
                                        border = BorderStroke(2.dp, ErrorColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .testTag("error_feedback_card")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = "خطأ",
                                                tint = ErrorColor,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "خطأ في التحقق",
                                                color = ErrorColor,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = result.message,
                                                color = TextSecondary,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            LinearProgressIndicator(
                                                progress = { countdown / 3.0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(CircleShape),
                                                color = ErrorColor,
                                                trackColor = BorderColor
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "جاري الاستئناف التلقائي خلال $countdown ثوانٍ...",
                                                color = TextTertiary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
