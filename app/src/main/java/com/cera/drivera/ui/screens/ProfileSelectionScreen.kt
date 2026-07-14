package com.cera.drivera.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cera.drivera.data.CalibrationProfile
import com.cera.drivera.ui.theme.DriveraBackground
import com.cera.drivera.ui.theme.DriveraAccentBlue
import com.cera.drivera.ui.theme.DriveraSurface
import com.cera.drivera.ui.theme.TextPrimary
import com.cera.drivera.ui.theme.DmsSuccess
import com.cera.drivera.ui.theme.DmsError
import com.cera.drivera.ui.theme.WorkSans
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileSelectionScreen(
    profiles: List<CalibrationProfile>,
    activeProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onCreateNewProfile: () -> Unit,
    onClose: () -> Unit
) {
    var profileToDelete by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DriveraBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ubah Profil Pengemudi",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = WorkSans
                )
                
                Button(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("✕", fontSize = 20.sp, color = TextPrimary)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profiles List
            if (profiles.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Belum Ada Profil",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = WorkSans
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Buat profil pertama Anda untuk memulai",
                        fontSize = 14.sp,
                        color = TextPrimary.copy(alpha = 0.7f),
                        fontFamily = WorkSans,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profiles) { profile ->
                        ProfileCard(
                            profile = profile,
                            isActive = profile.profileId == activeProfileId,
                            onSelect = { onSelectProfile(profile.profileId) },
                            onDelete = { profileToDelete = profile.profileId }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Create new profile button
            Button(
                onClick = onCreateNewProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DriveraAccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    tint = Color.White
                )
                Text(
                    "Buat Profil Baru",
                    color = Color.White,
                    fontFamily = WorkSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = {
                Text(
                    "Hapus Profil?",
                    color = TextPrimary,
                    fontFamily = WorkSans,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Profil yang dihapus tidak dapat dipulihkan.",
                    color = TextPrimary,
                    fontFamily = WorkSans
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProfile(profileToDelete!!)
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DmsError
                    )
                ) {
                    Text("Hapus", color = Color.White, fontFamily = WorkSans)
                }
            },
            dismissButton = {
                Button(
                    onClick = { profileToDelete = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray.copy(alpha = 0.2f)
                    )
                ) {
                    Text("Batal", color = TextPrimary, fontFamily = WorkSans)
                }
            },
            containerColor = DriveraSurface,
            textContentColor = TextPrimary
        )
    }
}

@Composable
fun ProfileCard(
    profile: CalibrationProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val createdDate = dateFormat.format(Date(profile.createdAt))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isActive) DriveraAccentBlue.copy(alpha = 0.15f) else DriveraSurface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) DriveraAccentBlue else TextPrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isActive) { onSelect() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile.driverName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontFamily = WorkSans
                        )
                        
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                modifier = Modifier.size(20.dp),
                                tint = DmsSuccess
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Dibuat: $createdDate",
                        fontSize = 12.sp,
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontFamily = WorkSans
                    )
                }
                
                // Delete button
                if (!isActive) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .background(DmsError.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = DmsError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Calibration info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EAR Terbuka",
                        fontSize = 11.sp,
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontFamily = WorkSans
                    )
                    Text(
                        text = String.format("%.3f", profile.earThresholdOpen),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = WorkSans
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EAR Tertutup",
                        fontSize = 11.sp,
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontFamily = WorkSans
                    )
                    Text(
                        text = String.format("%.3f", profile.earThresholdClosed),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = WorkSans
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Threshold",
                        fontSize = 11.sp,
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontFamily = WorkSans
                    )
                    Text(
                        text = String.format("%.3f", profile.calculatedThreshold),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DriveraAccentBlue,
                        fontFamily = WorkSans
                    )
                }
            }
        }
    }
}