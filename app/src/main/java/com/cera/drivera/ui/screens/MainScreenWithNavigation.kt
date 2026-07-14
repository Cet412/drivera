package com.cera.drivera.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cera.drivera.ui.components.HamburgerMenuDrawer
import com.cera.drivera.ui.theme.DriveraBackground
import com.cera.drivera.ui.theme.DriveraSurface
import com.cera.drivera.ui.theme.TextPrimary
import com.cera.drivera.ui.theme.WorkSans

@Composable
fun MainScreenWithNavigation(
    onProfileMenuClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onAboutClicked: () -> Unit,
    mainContent: @Composable () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DriveraBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar with hamburger menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DriveraSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hamburger menu button
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Title
                Text(
                    text = "DRIVERA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = WorkSans,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
                
                // Placeholder for right icon (settings, etc)
                Box(modifier = Modifier.size(40.dp))
            }
            
            // Main content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DriveraBackground)
            ) {
                mainContent()
            }
        }
        
        // Hamburger menu drawer
        HamburgerMenuDrawer(
            isVisible = isMenuOpen,
            onClose = { isMenuOpen = false },
            onProfileClicked = onProfileMenuClicked,
            onSettingsClicked = onSettingsClicked,
            onAboutClicked = onAboutClicked
        )
    }
}