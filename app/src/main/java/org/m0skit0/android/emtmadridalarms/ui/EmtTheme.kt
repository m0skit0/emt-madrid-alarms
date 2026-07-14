package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EmtLightColorScheme = lightColorScheme(
    primary = Color(0xFFD62839),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFFFFC145),
    onSecondary = Color(0xFF312100),
    secondaryContainer = Color(0xFFFFE08F),
    onSecondaryContainer = Color(0xFF251A00),
    tertiary = Color(0xFF006D8A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB6EBF5),
    onTertiaryContainer = Color(0xFF001F26),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE8EAED),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777E),
    outlineVariant = Color(0xFFC4C7CD),
)

private val EmtDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD62839),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF93000E),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFC145),
    onSecondary = Color(0xFF312100),
    secondaryContainer = Color(0xFF4A3500),
    onSecondaryContainer = Color(0xFFFFE08F),
    tertiary = Color(0xFF4FD5FF),
    onTertiary = Color(0xFF003542),
    tertiaryContainer = Color(0xFF004D65),
    onTertiaryContainer = Color(0xFFB6EBF5),
    background = Color(0xFF121418),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1E2025),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF2D3037),
    onSurfaceVariant = Color(0xFFC4C6CD),
    outline = Color(0xFF8E9098),
    outlineVariant = Color(0xFF44474F),
)

private val EmtTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

private val EmtShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
internal fun EmtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EmtDarkColorScheme else EmtLightColorScheme,
        typography = EmtTypography,
        shapes = EmtShapes,
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
