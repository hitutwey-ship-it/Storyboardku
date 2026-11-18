package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildStoryboardTypography(scale: Float = 1.0f): Typography {
    fun spScaled(base: Float) = (base * scale).sp

    return Typography(
        displaySmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = spScaled(28f),
            lineHeight = spScaled(34f)
        ),
        headlineMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = spScaled(22f),
            lineHeight = spScaled(28f)
        ),
        headlineSmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = spScaled(19f),
            lineHeight = spScaled(25f)
        ),
        titleLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = spScaled(18f),
            lineHeight = spScaled(24f)
        ),
        titleMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = spScaled(16f),
            lineHeight = spScaled(22f)
        ),
        titleSmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = spScaled(14f),
            lineHeight = spScaled(20f)
        ),
        bodyLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = spScaled(16f),
            lineHeight = spScaled(24f)
        ),
        bodyMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = spScaled(14f),
            lineHeight = spScaled(21f)
        ),
        bodySmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = spScaled(12.5f),
            lineHeight = spScaled(18f)
        ),
        labelLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = spScaled(14f),
            lineHeight = spScaled(20f)
        ),
        labelMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = spScaled(12f),
            lineHeight = spScaled(16f)
        ),
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = spScaled(11f),
            lineHeight = spScaled(15f)
        )
    )
}
