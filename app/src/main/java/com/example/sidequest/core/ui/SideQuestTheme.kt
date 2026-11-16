package com.example.sidequest.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SideQuestColors = darkColorScheme(
    primary = Color(0xFFFFCA76),
    onPrimary = Color(0xFF251707),
    secondary = Color(0xFF91D6C1),
    onSecondary = Color(0xFF073529),
    secondaryContainer = Color(0xFF234537),
    onSecondaryContainer = Color(0xFFC0ECDD),
    background = Color(0xFF101713),
    onBackground = Color(0xFFF2F4ED),
    surface = Color(0xFF101713),
    onSurface = Color(0xFFF2F4ED),
    surfaceContainerLow = Color(0xFF19241D),
    surfaceVariant = Color(0xFF29362D),
    onSurfaceVariant = Color(0xFFBBC9BC),
    outline = Color(0xFF819186),
    outlineVariant = Color(0xFF34473B),
)

@Composable
fun SideQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SideQuestColors, content = content)
}
