package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BioPrimary,
    secondary = BioSecondary,
    tertiary = BioTertiary,
    background = BioBackground,
    surface = BioSurface,
    surfaceVariant = BioSurfaceVariant,
    onPrimary = BioBackground,
    onSecondary = BioBackground,
    onTertiary = BioBackground,
    onBackground = BioOnBackground,
    onSurface = BioOnSurface,
    onSurfaceVariant = BioOnSurfaceVariant,
    error = BioError
  )

private val LightColorScheme = DarkColorScheme // Force dark theme everywhere for premium look

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable dynamic colors
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
