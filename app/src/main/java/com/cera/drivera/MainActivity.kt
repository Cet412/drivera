package com.cera.drivera

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cera.drivera.services.DmsForegroundService
import com.cera.drivera.ui.theme.DmsError
import com.cera.drivera.ui.theme.DmsSuccess
import com.cera.drivera.ui.theme.DmsWarning
import com.cera.drivera.ui.theme.DriveraAccentBlue
import com.cera.drivera.ui.theme.DriveraBackground
import com.cera.drivera.ui.theme.DriveraSurface
import com.cera.drivera.ui.theme.DriveraTheme
import com.cera.drivera.ui.theme.TextOnBrand
import com.cera.drivera.ui.theme.TextPrimary
import com.cera.drivera.ui.theme.WorkSans
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        if (cameraGranted && notificationGranted) {
            checkOverlayPermission()
        } else {
            Toast.makeText(this, "Izin Kamera & Notifikasi Wajib Diberikan!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initPermissionPipeline()

        setContent {
            DriveraTheme {
                DriveraMainScreen(
                    onStartService = { startDmsService() },
                    onStopService = { stopDmsService() }
                )
            }
        }
    }

    private fun startDmsService() {
        val intent = Intent(this, DmsForegroundService::class.java)
        stopService(intent) // Membersihkan memori state lama jika ada
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopDmsService() {
        val intent = Intent(this, DmsForegroundService::class.java)
        stopService(intent)
    }

    private fun initPermissionPipeline() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}

@Composable
fun DriveraMainScreen(
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    var isSystemActive by remember { mutableStateOf(false) }

    // State baru untuk menampung status dari Service
    var dmsStatus by remember { mutableStateOf("SAFE") }

    // Efek Samping: Mendaftar untuk mendengarkan Broadcast dari Service
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val newStatus = intent?.getStringExtra("STATUS") ?: "SAFE"
                dmsStatus = newStatus
            }
        }
        val filter = IntentFilter("DMS_STATE_UPDATE")
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)

        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }

    // Menentukan warna utama UI berdasarkan status AI
    val targetUiColor = when {
        !isSystemActive -> DriveraBackground // Mati: Gelap total
        dmsStatus == "CRITICAL" -> DmsError // Kantuk: Merah
        dmsStatus == "WARNING" -> DmsWarning // Transisi berkedip: Oranye (opsional ditampilkan)
        else -> DriveraBackground // SAFE: Kembali ke gelap untuk mencegah silau malam hari
    }

    // Menentukan teks ON / OFF / CRITICAL
    val displayText = when {
        !isSystemActive -> "OFF"
        dmsStatus == "CRITICAL" -> "WARNING"
        else -> "ON"
    }

    // Warna teks indikator
    val statusTextColor by animateColorAsState(
        targetValue = when {
            !isSystemActive -> TextPrimary
            dmsStatus == "CRITICAL" -> TextOnBrand // Teks putih di atas background merah
            dmsStatus == "WARNING" -> DmsWarning
            else -> DmsSuccess
        },
        animationSpec = tween(durationMillis = 300), label = "statusColor"
    )

    // Animasi perubahan warna background utama yang mulus
    val backgroundColor by animateColorAsState(
        targetValue = targetUiColor,
        animationSpec = tween(durationMillis = 400), label = "bgColor"
    )

    // Warna tombol power
    val buttonColor by animateColorAsState(
        targetValue = if (isSystemActive) DmsSuccess else DriveraSurface,
        animationSpec = tween(durationMillis = 300), label = "btnColor"
    )

    val blueCurveTranslation by animateDpAsState(
        targetValue = if (isSystemActive) 150.dp else 0.dp,
        animationSpec = tween(durationMillis = 600), label = "curveTranslation"
    )
    val contentVerticalOffset by animateDpAsState(
        targetValue = if (isSystemActive) 0.dp else (-60).dp,
        animationSpec = tween(durationMillis = 600), label = "contentOffset"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        // ROOT BOX
        Box(modifier = Modifier.fillMaxSize()) {

            // --- LAPISAN BAWAH: GRUP DEKORATIF & INTERAKSI ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = blueCurveTranslation.toPx()
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // 1. Kanvas Biru
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.25f)
                        quadraticBezierTo(
                            size.width / 2f, -size.height * 0.15f,
                            size.width, size.height * 0.25f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path = path, color = DriveraAccentBlue)
                }

                // 2. Tombol Power Interaktif
                Box(
                    modifier = Modifier
                        .offset(y = -10.dp)
                        .size(80.dp)
                        .shadow(
                            elevation = if (isSystemActive) 12.dp else 4.dp,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(buttonColor)
                        .clickable {
                            isSystemActive = !isSystemActive
                            if (isSystemActive) {
                                onStartService()
                            } else {
                                onStopService()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle Power",
                        tint = TextOnBrand,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // 3. AREA KALIBRASI WAJAH (Menggantikan Kartu Diagnostik)
                AnimatedVisibility(
                    visible = !isSystemActive,
                    enter = fadeIn(animationSpec = tween(250)),
                    exit = fadeOut(animationSpec = tween(250)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter) // Menempel di dasar layar
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                ) {
                    // Injeksi komponen kamera dengan dimensi absolut
                    FaceCalibrationPreview(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp) // Tinggi proporsional agar tidak menabrak tombol power
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            } // Tutup Lapisan Bawah

            // --- LAPISAN ATAS: TIPOGRAFI UTAMA ---
            // Dikeluarkan dari grup bawah agar bisa bergerak bebas di seluruh layar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = contentVerticalOffset),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DRIVERA",
                    color = if (dmsStatus == "CRITICAL") TextOnBrand else TextPrimary,
                    fontFamily = WorkSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 48.sp,
                    letterSpacing = 0.16.em
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Driver Monitoring System Agent",
                    color = TextPrimary,
                    fontFamily = WorkSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    letterSpacing = 0.05.em
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = displayText,
                    color = statusTextColor,
                    fontFamily = WorkSans,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.16.em
                )
            }
        }
    }
}

@Composable
fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DriveraSurface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontFamily = WorkSans,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontFamily = WorkSans,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.02.em
        )
    }
}

@Composable
fun FaceCalibrationPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Simpan referensi memori secara spesifik
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewUseCaseRef by remember { mutableStateOf<Preview?>(null) } // Referensi terisolasi

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        // 1. RAW CAMERA PREVIEW
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

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Simpan use-case ini ke state agar bisa dihancurkan secara spesifik nanti
                    previewUseCaseRef = preview

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        // ANTI-CRASH: Jangan pernah gunakan unbindAll() di sini!
                        // Cukup putus preview lama (jika ada recomposition) lalu pasang yang baru
                        cameraProvider.unbind(preview)
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    } catch (e: Exception) {
                        Log.e("CalibrationUI", "Gagal memuat pratinjau kamera", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // 2. OVERLAY BINGKAI MATA (Terkunci Absolut di Tengah)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .fillMaxHeight(0.55f)
                .border(
                    width = 2.dp,
                    color = Color(0xFF00FF7F).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp)
                )
        )   

        // 3. TEKS INSTRUKSI (Terkunci di Dasar)
        Text(
            text = "ALIGN EYES WITHIN THE FRAME",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.05.em,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .padding(horizontal = 16.dp)
        )
    }

    // KRUSIAL: Surgical Cleanup
    DisposableEffect(Unit) {
        onDispose {
            // Hanya putus use-case 'Preview' milik UI. Biarkan AI Service tetap hidup.
            previewUseCaseRef?.let { preview ->
                cameraProviderRef?.unbind(preview)
            }
            Log.d("CalibrationUI", "Kamera UI dilepas secara terisolasi. Service AI aman.")
        }
    }
}