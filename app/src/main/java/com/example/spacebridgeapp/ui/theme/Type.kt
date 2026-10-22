package com.example.spacebridgeapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Font utama Plus Jakarta Sans dengan fallback sistem yang aman (DESIGN.md Bagian 3)
val PlusJakartaSans = FontFamily.SansSerif

val Typography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                letterSpacing = (-0.02).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.02).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.01).sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.01).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 28.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )
