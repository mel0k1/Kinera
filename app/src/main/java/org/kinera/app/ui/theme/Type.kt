package org.kinera.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val Display = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    lineHeight = 44.sp,
    letterSpacing = (-0.015).em,
)

private val Headline = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.01).em,
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
    headlineMedium = Headline,
    headlineSmall = Headline.copy(fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = Title,
    titleMedium = Title.copy(fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = Title.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = Body,
    bodyMedium = Body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = Body.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = Label.copy(fontSize = 16.sp),
    labelMedium = Label,
    labelSmall = Label.copy(fontSize = 11.sp, lineHeight = 16.sp),
)
