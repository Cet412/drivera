package com.cera.drivera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cera.drivera.ui.theme.DriveraSurface
import com.cera.drivera.ui.theme.TextPrimary
import com.cera.drivera.ui.theme.DriveraAccentBlue
import com.cera.drivera.ui.theme.WorkSans

@Composable
fun HamburgerMenuDrawer(
    isVisible: Boolean,
    onClose: () -> Unit,
    onProfileClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onAboutClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Dark overlay
        AnimatedVisibility(
            visible = isVisible,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onClose() }
        ) {}
        
        // Drawer menu
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxHeight()
                .width(280.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(DriveraSurface)
                    .padding(0.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Menu",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = WorkSans
                    )
                    
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Menu items
                MenuItem(
                    icon = Icons.Default.SwapHoriz,
                    label = "Ubah Profil Pengemudi",
                    onClick = {
                        onProfileClicked()
                        onClose()
                    }
                )
                
                MenuItem(
                    icon = Icons.Default.Settings,
                    label = "Pengaturan",
                    onClick = {
                        onSettingsClicked()
                        onClose()
                    }
                )
                
                MenuItem(
                    icon = Icons.Default.Info,
                    label = "Tentang",
                    onClick = {
                        onAboutClicked()
                        onClose()
                    }
                )
                
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = DriveraAccentBlue
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            fontSize = 16.sp,
            color = TextPrimary,
            fontFamily = WorkSans,
            fontWeight = FontWeight.Medium
        )
    }
}