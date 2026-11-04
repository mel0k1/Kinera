package org.kinera.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Kinera type: confident, compact display for expressive headers,
// humanist body for comfortable long-form reading.
private val Display = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    fontSize = 38.sp,
    lineHeight = 44.sp,
    letterSpacing = (-0.02).em,
)

private val Headline = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.015).em,
)

private val Title = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.em,
)

private val Body = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.01.em,
)

private val Label = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.02.em,
)

val KineraTypography = Typography(
    displaySmall = Display,
    displayMedium = Display.copy(fontSize = 44.sp, lineHeight = 50.sp),
    headlineMedium = Headline,
    headlineSmall = Headline.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = Title,
    titleMedium = Title.copy(fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = Title.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = Body,
    bodyMedium = Body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = Body.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = Label.copy(fontSize = 15.sp),
    labelMedium = Label,
    labelSmall = Label.copy(fontSize = 11.sp, lineHeight = 16.sp),
)
