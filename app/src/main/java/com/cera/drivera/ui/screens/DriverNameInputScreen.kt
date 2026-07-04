package com.cera.drivera.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cera.drivera.ui.theme.DriveraBackground
import com.cera.drivera.ui.theme.DriveraAccentBlue
import com.cera.drivera.ui.theme.DriveraSurface
import com.cera.drivera.ui.theme.TextPrimary
import com.cera.drivera.ui.theme.WorkSans

@Composable
fun DriverNameInputScreen(
    onContinue: (String) -> Unit
) {
    var driverName by remember { mutableStateOf("") }
    var isNameValid by remember { mutableStateOf(false) }
    
    LaunchedEffect(driverName) {
        isNameValid = driverName.trim().isNotEmpty() && driverName.length >= 2
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(DriveraAccentBlue.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Driver Profile",
                    modifier = Modifier.size(50.dp),
                    tint = DriveraAccentBlue
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Buat Profil Pengemudi",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = WorkSans,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Masukkan nama pengemudi untuk profil kalibrasi ini",
                fontSize = 14.sp,
                color = TextPrimary.copy(alpha = 0.7f),
                fontFamily = WorkSans,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Input field
            OutlinedTextField(
                value = driverName,
                onValueChange = { driverName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = {
                    Text(
                        "Contoh: Budi Santoso",
                        fontSize = 14.sp,
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontFamily = WorkSans
                    )
                },
                singleLine = true,
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    color = TextPrimary,
                    fontFamily = WorkSans,
                    fontSize = 16.sp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DriveraAccentBlue,
                    unfocusedBorderColor = TextPrimary.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Nama minimal 2 karakter",
                fontSize = 12.sp,
                color = if (driverName.isEmpty()) TextPrimary.copy(alpha = 0.5f) else if (isNameValid) Color(0xFF00FF7F) else Color(0xFFFF6B6B),
                fontFamily = WorkSans
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Continue button
            Button(
                onClick = { onContinue(driverName.trim()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isNameValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DriveraAccentBlue,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Lanjut ke Kalibrasi",
                    color = if (isNameValid) Color.White else TextPrimary.copy(alpha = 0.5f),
                    fontFamily = WorkSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}