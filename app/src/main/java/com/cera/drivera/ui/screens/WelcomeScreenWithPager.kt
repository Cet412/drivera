package com.cera.drivera.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.cera.drivera.ui.theme.DriveraBackground
import com.cera.drivera.ui.theme.DriveraAccentBlue
import com.cera.drivera.ui.theme.TextPrimary
import com.cera.drivera.ui.theme.DmsSuccess
import com.cera.drivera.ui.theme.WorkSans

@OptIn(ExperimentalPagerApi::class)
@Composable
fun WelcomeScreenWithPager(
    onOnboardingComplete: () -> Unit
) {
    val pagerState = rememberPagerState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DriveraBackground)
    ) {
        HorizontalPager(
            count = 5,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage1()
                1 -> WelcomePage2()
                2 -> WelcomePage3()
                3 -> WelcomePage4()
                4 -> WelcomePage5ProfileTutorial()
            }
        }
        
        // Navigation buttons at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (hidden on first page)
                if (pagerState.currentPage > 0) {
                    Button(
                        onClick = {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("Kembali", color = TextPrimary, fontFamily = WorkSans)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                
                // Next/Done button
                Button(
                    onClick = {
                        if (pagerState.currentPage < 4) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            onOnboardingComplete()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DriveraAccentBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (pagerState.currentPage == 4) "Mulai Kalibrasi" else "Lanjut",
                        color = Color.White,
                        fontFamily = WorkSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Page indicator dots
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { page ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (page == pagerState.currentPage) DriveraAccentBlue else Color.Gray,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    if (page < 4) Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun WelcomePage1() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Selamat Datang di DRIVERA",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = WorkSans,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(DriveraAccentBlue, shape = RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "👁️",
                fontSize = 60.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Driver Monitoring System menggunakan AI on-device untuk mendeteksi kantuk pengemudi secara real-time.",
            fontSize = 16.sp,
            color = TextPrimary,
            fontFamily = WorkSans,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WelcomePage2() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bagaimana Ini Bekerja?",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = WorkSans,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Step 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DriveraAccentBlue, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("1", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Kamera Depan", fontFamily = WorkSans, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Sistem menganalisis gerakan mata Anda", fontSize = 14.sp, color = TextPrimary, fontFamily = WorkSans)
            }
        }
        
        // Step 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DriveraAccentBlue, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("2", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Deteksi Real-Time", fontFamily = WorkSans, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("AI mendeteksi penutupan mata (EAR)", fontSize = 14.sp, color = TextPrimary, fontFamily = WorkSans)
            }
        }
        
        // Step 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DriveraAccentBlue, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("3", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Alert Otomatis", fontFamily = WorkSans, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Alarm dan vibration saat kantuk terdeteksi", fontSize = 14.sp, color = TextPrimary, fontFamily = WorkSans)
            }
        }
    }
}

@Composable
fun WelcomePage3() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Privasi Terjamin",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = WorkSans,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(DmsSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Check",
                modifier = Modifier.size(60.dp),
                tint = DmsSuccess
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Semua pemrosesan dilakukan di perangkat Anda. Tidak ada data yang dikirim ke cloud atau server eksternal.",
            fontSize = 16.sp,
            color = TextPrimary,
            fontFamily = WorkSans,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WelcomePage4() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Persiapan Kalibrasi",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = WorkSans,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Checklist
        val items = listOf(
            "Posisikan wajah Anda di hadapan kamera",
            "Pastikan pencahayaan cukup",
            "Bersihkan lensa kamera",
            "Siapkan 1-2 menit untuk proses kalibrasi"
        )
        
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Check",
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 12.dp),
                    tint = DmsSuccess
                )
                Text(
                    text = item,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    fontFamily = WorkSans,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun WelcomePage5ProfileTutorial() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cara Mengubah Profil",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = WorkSans,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Step by step
        val steps = listOf(
            "1. Tekan ☰ (Hamburger Menu) di bagian atas kiri layar",
            "2. Pilih 'Ubah Profil Pengemudi'",
            "3. Pilih profil dari daftar atau buat profil baru dengan kalibrasi baru",
            "💡 Tip: Buat profil terpisah untuk setiap pengemudi agar deteksi lebih akurat!"
        )
        
        steps.forEachIndexed { index, step ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(
                        color = if (index == steps.size - 1) DmsSuccess.copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = step,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    fontFamily = WorkSans
                )
            }
        }
    }
}