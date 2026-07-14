package org.m0skit0.android.emtmadridalarms.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EmtLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFD62839),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFFFFC145),
    onSecondary = Color(0xFF312100),
    secondaryContainer = Color(0xFFFFE08F),
    onSecondaryContainer = Color(0xFF251A00),
    tertiary = Color(0xFF005F73),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB6EBF5),
    onTertiaryContainer = Color(0xFF001F26),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF23191A),
    surface = Color.White,
    onSurface = Color(0xFF23191A),
    surfaceVariant = Color(0xFFF6DEDD),
    onSurfaceVariant = Color(0xFF564142),
    outline = Color(0xFF8B7172),
)

@Composable
internal fun EmtTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmtLightColorScheme,
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
