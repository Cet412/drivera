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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cera.drivera.data.CalibrationPreferencesManager
import com.cera.drivera.data.CalibrationProfile
import com.cera.drivera.services.DmsForegroundService
import com.cera.drivera.ui.screens.CalibrationWizardScreen
import com.cera.drivera.ui.screens.DriverNameInputScreen
import com.cera.drivera.ui.screens.MainScreenWithNavigation
import com.cera.drivera.ui.screens.ProfileSelectionScreen
import com.cera.drivera.ui.screens.WelcomeScreenWithPager
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

// ─── Sealed class untuk navigasi antar layar ───────────────────────────────
sealed class Screen {
    object Loading : Screen()
    object Welcome : Screen()
    object DriverNameInput : Screen()
    object Calibration : Screen()
    object Main : Screen()
    object ProfileSelection : Screen()
}

// ─── Activity ──────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private lateinit var calibrationManager: CalibrationPreferencesManager

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        calibrationManager = CalibrationPreferencesManager(this)
        initPermissionPipeline()

        setContent {
            DriveraTheme {
                MainActivityNavigation(
                    calibrationManager = calibrationManager,
                    onStartService = { startDmsService() },
                    onStopService = { stopDmsService() }
                )
            }
        }
    }

    private fun startDmsService() {
        val intent = Intent(this, DmsForegroundService::class.java)
        stopService(intent)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopDmsService() {
        stopService(Intent(this, DmsForegroundService::class.java))
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
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }
}

// ─── Navigation root ───────────────────────────────────────────────────────
@Composable
fun MainActivityNavigation(
    calibrationManager: CalibrationPreferencesManager,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
    var driverName by remember { mutableStateOf("") }
    var allProfiles by remember { mutableStateOf(calibrationManager.getAllProfiles()) }
    var activeProfileId by remember { mutableStateOf(calibrationManager.getActiveProfile()?.profileId) }

    // Tentukan layar awal berdasarkan status first boot
    LaunchedEffect(Unit) {
        currentScreen = if (calibrationManager.isFirstBoot()) Screen.Welcome else Screen.Main
    }

    when (currentScreen) {

        Screen.Loading -> {
            // Layar kosong sementara LaunchedEffect menentukan tujuan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DriveraBackground)
            )
        }

        Screen.Welcome -> {
            WelcomeScreenWithPager(
                onOnboardingComplete = {
                    currentScreen = Screen.DriverNameInput
                }
            )
        }

        Screen.DriverNameInput -> {
            DriverNameInputScreen(
                onContinue = { name ->
                    driverName = name
                    currentScreen = Screen.Calibration
                }
            )
        }

        Screen.Calibration -> {
            CalibrationWizardScreen(
                driverName = driverName,
                onCalibrationComplete = { earOpen, earClosed ->
                    val newProfile = calibrationManager.createProfile(
                        driverName = driverName,
                        earOpen = earOpen,
                        earClosed = earClosed
                    )
                    calibrationManager.saveProfile(newProfile)
                    calibrationManager.setActiveProfile(newProfile.profileId)
                    calibrationManager.markFirstBootComplete()

                    allProfiles = calibrationManager.getAllProfiles()
                    activeProfileId = newProfile.profileId

                    currentScreen = Screen.Main
                },
                onCalibrationFailed = {
                    // Kembali ke DriverNameInputScreen dengan driverName tetap tersimpan
                    // (driverName masih di state, jadi tinggal navigasi balik)
                    currentScreen = Screen.DriverNameInput
                }
            )
        }

        Screen.Main -> {
            // MainScreenWithNavigation membungkus DriveraMainScreen (DMS asli)
            // sehingga hamburger menu + navigasi profil tetap tersedia
            MainScreenWithNavigation(
                onProfileMenuClicked = { currentScreen = Screen.ProfileSelection },
                onSettingsClicked = { /* TODO: Settings screen */ },
                onAboutClicked = { /* TODO: About screen */ }
            ) {
                // Konten utama DMS: power button, animasi, service management
                DriveraMainScreen(
                    onStartService = onStartService,
                    onStopService = onStopService
                )
            }
        }

        Screen.ProfileSelection -> {
            ProfileSelectionScreen(
                profiles = allProfiles,
                activeProfileId = activeProfileId,
                onSelectProfile = { profileId ->
                    calibrationManager.setActiveProfile(profileId)
                    activeProfileId = profileId
                    allProfiles = calibrationManager.getAllProfiles()
                    currentScreen = Screen.Main
                },
                onDeleteProfile = { profileId ->
                    calibrationManager.deleteProfile(profileId)
                    allProfiles = calibrationManager.getAllProfiles()
                    if (activeProfileId == profileId) activeProfileId = null
                },
                onCreateNewProfile = {
                    currentScreen = Screen.DriverNameInput
                },
                onClose = {
                    currentScreen = Screen.Main
                }
            )
        }
    }
}

// ─── DMS Main Content (power button, kamera, animasi) ─────────────────────
@Composable
fun DriveraMainScreen(
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    var isSystemActive by remember { mutableStateOf(false) }
    var dmsStatus by remember { mutableStateOf("SAFE") }

    // Dengarkan broadcast status dari DmsForegroundService
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                dmsStatus = intent?.getStringExtra("STATUS") ?: "SAFE"
            }
        }
        val filter = IntentFilter("DMS_STATE_UPDATE")
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }

    // Warna background berubah sesuai status
    val targetUiColor = when {
        !isSystemActive -> DriveraBackground
        dmsStatus == "CRITICAL" -> DmsError
        else -> DriveraBackground
    }

    val displayText = when {
        !isSystemActive -> "OFF"
        dmsStatus == "CRITICAL" -> "WARNING"
        else -> "ON"
    }

    val statusTextColor by animateColorAsState(
        targetValue = when {
            !isSystemActive -> TextPrimary
            dmsStatus == "CRITICAL" -> TextOnBrand
            dmsStatus == "WARNING" -> DmsWarning
            else -> DmsSuccess
        },
        animationSpec = tween(300), label = "statusColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = targetUiColor,
        animationSpec = tween(400), label = "bgColor"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isSystemActive) DmsSuccess else DriveraSurface,
        animationSpec = tween(300), label = "btnColor"
    )

    val blueCurveTranslation by animateDpAsState(
        targetValue = if (isSystemActive) 150.dp else 0.dp,
        animationSpec = tween(600), label = "curveTranslation"
    )

    val contentVerticalOffset by animateDpAsState(
        targetValue = if (isSystemActive) 0.dp else (-60).dp,
        animationSpec = tween(600), label = "contentOffset"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Lapisan bawah: kurva biru + tombol power + kamera preview ──
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
                // Kurva biru dekoratif
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

                // Tombol power
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
                            if (isSystemActive) onStartService() else onStopService()
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

                // Kamera preview (tampil saat DMS belum aktif)
                AnimatedVisibility(
                    visible = !isSystemActive,
                    enter = fadeIn(animationSpec = tween(250)),
                    exit = fadeOut(animationSpec = tween(250)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                ) {
                    FaceCalibrationPreview(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }

            // ── Lapisan atas: teks DRIVERA + status ──
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

// ─── Face calibration camera preview ──────────────────────────────────────
@Composable
fun FaceCalibrationPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewUseCaseRef by remember { mutableStateOf<Preview?>(null) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

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
                    previewUseCaseRef = preview

                    try {
                        cameraProvider.unbind(preview)
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview
                        )
                    } catch (e: Exception) {
                        Log.e("CalibrationUI", "Gagal memuat pratinjau kamera", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Bingkai panduan mata
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

        Text(
            text = "ALIGN EYES WITHIN THE FRAME",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.05.em,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .padding(horizontal = 16.dp)
        )
    }

    // Lepas hanya use-case Preview ini; biarkan AI Service tetap hidup
    DisposableEffect(Unit) {
        onDispose {
            previewUseCaseRef?.let { preview ->
                cameraProviderRef?.unbind(preview)
            }
            Log.d("CalibrationUI", "Kamera UI dilepas secara terisolasi. Service AI aman.")
        }
    }
}

// ─── Komponen diagnostik (tetap tersedia untuk kebutuhan lain) ─────────────
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
