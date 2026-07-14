package com.cera.drivera.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.cera.drivera.data.FaceAnalyzer
import com.cera.drivera.ui.theme.DriveraBackground
import com.cera.drivera.ui.theme.DriveraAccentBlue
import com.cera.drivera.ui.theme.DmsWarning
import com.cera.drivera.ui.theme.DmsSuccess
import com.cera.drivera.ui.theme.TextPrimary
import com.cera.drivera.ui.theme.WorkSans
import kotlinx.coroutines.delay

@Composable
fun CalibrationWizardScreen(
    driverName: String,
    onCalibrationComplete: (earOpen: Double, earClosed: Double) -> Unit,
    onCalibrationFailed: () -> Unit
) {
    var calibrationStep by remember { mutableStateOf(0) } // 0: Buka Mata, 1: Tutup Mata, 2: Complete
    var currentEAR by remember { mutableStateOf(0.0) }
    var earOpenValue by remember { mutableStateOf(0.0) }
    var earClosedValue by remember { mutableStateOf(0.0) }
    var isFaceDetected by remember { mutableStateOf(false) }
    var isCapturingData by remember { mutableStateOf(false) }
    var captureCountdown by remember { mutableStateOf(3) }
    var showTimeoutWarning by remember { mutableStateOf(false) }
    var stepStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var faceAnalyzer by remember { mutableStateOf<FaceAnalyzer?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    // Initialize face analyzer
    LaunchedEffect(Unit) {
        faceAnalyzer = FaceAnalyzer(context) { earValue ->
            currentEAR = earValue
            isFaceDetected = earValue > 0.01 // Minimal threshold untuk mendeteksi ada wajah
        }
    }
    
    // Handle step timeout (10 detik per step)
    LaunchedEffect(calibrationStep, stepStartTime) {
        showTimeoutWarning = false
        val timeoutMillis = 10_000L // 10 detik timeout
        
        while (true) {
            delay(1000)
            val elapsed = System.currentTimeMillis() - stepStartTime
            
            if (elapsed >= timeoutMillis && calibrationStep < 2 && !isCapturingData) {
                showTimeoutWarning = true
                break
            }
            
            if (calibrationStep >= 2) break
            if (isCapturingData) break // Stop timeout check saat sedang capturing
        }
    }
    
    // Handle data capture countdown
    LaunchedEffect(isCapturingData, captureCountdown) {
        if (isCapturingData) {
            while (captureCountdown > 0) {
                delay(1000)
                captureCountdown--
            }
            
            // Capture data setelah countdown selesai
            when (calibrationStep) {
                0 -> earOpenValue = currentEAR
                1 -> earClosedValue = currentEAR
            }
            
            // Lanjut ke step berikutnya
            isCapturingData = false
            captureCountdown = 3
            
            if (calibrationStep == 0) {
                calibrationStep = 1
                stepStartTime = System.currentTimeMillis() // Reset timer untuk step 2
            } else if (calibrationStep == 1) {
                calibrationStep = 2
            }
        }
    }
    
    // UI composable untuk timeout warning
    if (showTimeoutWarning) {
        TimeoutWarningDialog(
            onRetry = {
                showTimeoutWarning = false
                stepStartTime = System.currentTimeMillis() // Reset timer
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DriveraBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Kalibrasi untuk $driverName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = WorkSans
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = when (calibrationStep) {
                    0 -> "Step 1/2 - Mata Terbuka"
                    1 -> "Step 2/2 - Mata Tertutup"
                    else -> "Kalibrasi Selesai"
                },
                fontSize = 14.sp,
                color = TextPrimary.copy(alpha = 0.7f),
                fontFamily = WorkSans
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Camera preview with face guide
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black, shape = RoundedCornerShape(20.dp))
                    .border(2.dp, DriveraAccentBlue, shape = RoundedCornerShape(20.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderRef = cameraProvider
                            
                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context)
                            ) { imageProxy ->
                                try {
                                    faceAnalyzer?.analyze(imageProxy)
                                } catch (e: Exception) {
                                    Log.e("CalibrationWizard", "Error analyzing frame", e)
                                }
                            }
                            
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("CalibrationWizard", "Error binding camera", e)
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
                
                // Face center guide frame
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(160.dp)
                        .border(
                            width = 2.dp,
                            color = if (isFaceDetected) DmsSuccess else DmsWarning,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
                
                // Overlay untuk capturing
                if (isCapturingData) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Loading ring
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(
                                        width = 4.dp,
                                        color = DriveraAccentBlue,
                                        shape = CircleShape
                                    )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Mengambil data...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = WorkSans
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "$captureCountdown",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = DriveraAccentBlue,
                                fontFamily = WorkSans
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Wajah Terdeteksi",
                        fontSize = 12.sp,
                        color = TextPrimary.copy(alpha = 0.7f),
                        fontFamily = WorkSans
                    )
                    Text(
                        text = if (isFaceDetected) "✓" else "✗",
                        fontSize = 20.sp,
                        color = if (isFaceDetected) DmsSuccess else DmsWarning,
                        fontFamily = WorkSans,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Step",
                        fontSize = 12.sp,
                        color = TextPrimary.copy(alpha = 0.7f),
                        fontFamily = WorkSans
                    )
                    Text(
                        text = "${calibrationStep + 1}/2",
                        fontSize = 20.sp,
                        color = DriveraAccentBlue,
                        fontFamily = WorkSans,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "EAR Value",
                        fontSize = 12.sp,
                        color = TextPrimary.copy(alpha = 0.7f),
                        fontFamily = WorkSans
                    )
                    Text(
                        text = String.format("%.3f", currentEAR),
                        fontSize = 20.sp,
                        color = DriveraAccentBlue,
                        fontFamily = WorkSans,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instructions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (calibrationStep) {
                        0 -> "Pastikan wajah Anda dalam frame biru"
                        1 -> "Sekarang pejamkan mata perlahan-lahan"
                        else -> "Kalibrasi selesai!"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontFamily = WorkSans,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = when (calibrationStep) {
                        0 -> "Pastikan mata terbuka lebar dan pandangan lurus"
                        1 -> "Tutup mata sepenuhnya (seperti tidur)"
                        else -> "Data EAR terbuka dan tertutup sudah terekam"
                    },
                    fontSize = 14.sp,
                    color = TextPrimary.copy(alpha = 0.7f),
                    fontFamily = WorkSans,
                    textAlign = TextAlign.Center
                )
                
                // Captured values display
                AnimatedVisibility(
                    visible = calibrationStep == 2,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .background(DmsSuccess.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("EAR Terbuka", fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.7f))
                                Text(String.format("%.3f", earOpenValue), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("EAR Tertutup", fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.7f))
                                Text(String.format("%.3f", earClosedValue), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action buttons
            when {
                calibrationStep < 2 && !isCapturingData -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Primary action button
                        Button(
                            onClick = {
                                if (isFaceDetected) {
                                    isCapturingData = true
                                    stepStartTime = System.currentTimeMillis() // Reset timeout timer
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = isFaceDetected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DriveraAccentBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                when (calibrationStep) {
                                    0 -> "Ambil Data Mata Terbuka"
                                    else -> "Ambil Data Mata Tertutup"
                                },
                                color = Color.White,
                                fontFamily = WorkSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Cancel button
                        Button(
                            onClick = onCalibrationFailed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Batalkan Kalibrasi",
                                color = TextPrimary.copy(alpha = 0.6f),
                                fontFamily = WorkSans,
                                fontSize = 14.sp
                            )
                        }
                        
                        if (!isFaceDetected) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "⚠️ Wajah tidak terdeteksi. Pastikan kamera menghadap ke depan dan wajah dalam frame.",
                                fontSize = 12.sp,
                                color = DmsWarning,
                                fontFamily = WorkSans,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                calibrationStep == 2 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { onCalibrationComplete(earOpenValue, earClosedValue) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DmsSuccess),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Selesaikan Kalibrasi",
                                color = Color.White,
                                fontFamily = WorkSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = onCalibrationFailed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Batalkan & Kembali",
                                color = TextPrimary.copy(alpha = 0.6f),
                                fontFamily = WorkSans,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            faceAnalyzer?.clear()
            cameraProviderRef?.unbindAll()
        }
    }
}

@Composable
private fun TimeoutWarningDialog(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .background(DriveraBackground, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                modifier = Modifier.size(48.dp),
                tint = DmsWarning
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Waktu Kalibrasi Habis",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = WorkSans,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Proses kalibrasi memakan waktu terlalu lama. Pastikan wajah berada dalam frame biru dan kondisi pencahayaan cukup.",
                fontSize = 14.sp,
                color = TextPrimary.copy(alpha = 0.8f),
                fontFamily = WorkSans,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DriveraAccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Coba Lagi",
                    color = Color.White,
                    fontFamily = WorkSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
