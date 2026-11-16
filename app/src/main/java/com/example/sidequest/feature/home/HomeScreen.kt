package com.example.sidequest.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sidequest.R

@Composable
fun HomeScreen(
    onStartAdventure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        AdventureCard(onStartAdventure)
        ExplorerProgress()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.active_quest),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = stringResource(R.string.no_active_quest),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun AdventureCard(onStartAdventure: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val colors = MaterialTheme.colorScheme
    val accent = colors.secondary.copy(alpha = 0.18f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF182D23), Color(0xFF274436))))
            .border(1.dp, colors.outlineVariant, shape),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.91f, size.height * 0.11f)
            for (ring in 1..4) {
                drawCircle(
                    color = accent,
                    radius = ring * size.width * 0.094f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            drawCircle(color = colors.primary, radius = 3.dp.toPx(), center = center)
        }
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_headline),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.home_description),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = onStartAdventure,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.start_adventure))
            }
        }
    }
}

@Composable
private fun ExplorerProgress() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.explorer_name),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressTile(
                label = stringResource(R.string.player_level),
                value = stringResource(R.string.starting_level),
                modifier = Modifier.weight(1f),
            )
            ProgressTile(
                label = stringResource(R.string.player_xp),
                value = stringResource(R.string.starting_xp),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProgressTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
