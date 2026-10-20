package com.tk.toolkit.tv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import com.tk.toolkit.tv.R

/**
 * Inter (SIL Open Font License, see third_party/inter), a free face close to Helvetica Neue,
 * used for the clock header. Subset to Latin characters to keep the APK small.
 */
val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

val Typography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Light, fontSize = 64.sp, letterSpacing = (-1.5).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Light, fontSize = 40.sp, letterSpacing = (-0.8).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 2.4.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.6.sp),
)
