package com.cera.drivera.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cera.drivera.R


val WorkSans = FontFamily(
    Font(R.font.worksans_regular, FontWeight.Normal),
    Font(R.font.worksans_medium, FontWeight.Medium),
    Font(R.font.worksans_semibold, FontWeight.SemiBold),
)
// Set of Material typography styles to start with
val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 47.78.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.16.em
    ),
    bodyLarge = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )

)