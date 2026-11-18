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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sidequest.R
import com.example.sidequest.domain.adventure.PlayerProgress
import com.example.sidequest.domain.adventure.QuestProgress
import com.example.sidequest.feature.quest.QuestLoadState

@Composable
fun HomeScreen(
    onStartAdventure: () -> Unit,
    onContinueAdventure: () -> Unit,
    questState: QuestLoadState,
    modifier: Modifier = Modifier,
) {
    val readyState = questState as? QuestLoadState.Ready
    val savedQuest = readyState?.quest
    val activeQuest = savedQuest?.takeUnless { it.isComplete }

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
        AdventureCard(
            questState = questState,
            activeQuest = activeQuest,
            onStartAdventure = onStartAdventure,
            onContinueAdventure = onContinueAdventure,
        )
        if (readyState != null) {
            ExplorerProgress(readyState.player)
            if (activeQuest == null) {
                QuestSummary(savedQuest)
            }
        }
    }
}

@Composable
private fun AdventureCard(
    questState: QuestLoadState,
    activeQuest: QuestProgress?,
    onStartAdventure: () -> Unit,
    onContinueAdventure: () -> Unit,
) {
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
                text = activeQuest?.adventure?.title ?: stringResource(R.string.home_headline),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
            )
            when (questState) {
                QuestLoadState.Loading -> QuestStatus(
                    text = stringResource(R.string.quest_loading),
                )
                QuestLoadState.Failed -> QuestStatus(
                    text = stringResource(R.string.quest_load_error),
                    color = colors.error,
                )
                is QuestLoadState.Ready -> {
                    QuestStatus(
                        text = if (activeQuest == null) {
                            stringResource(R.string.home_description)
                        } else {
                            stringResource(
                                R.string.quest_chapter_progress,
                                activeQuest.completedChapters + 1,
                                activeQuest.adventure.chapters.size,
                            )
                        },
                    )
                    Button(
                        onClick = if (activeQuest == null) onStartAdventure else onContinueAdventure,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            stringResource(
                                if (activeQuest == null) R.string.start_adventure
                                else R.string.continue_adventure,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestStatus(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = color,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun QuestSummary(completedQuest: QuestProgress?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(
                if (completedQuest == null) R.string.active_quest else R.string.last_adventure,
            ),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Text(
                text = completedQuest?.adventure?.title ?: stringResource(R.string.no_active_quest),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ExplorerProgress(player: PlayerProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.explorer_name),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressTile(
                label = stringResource(R.string.player_level),
                value = stringResource(R.string.player_stat_value, player.level),
                modifier = Modifier.weight(1f),
            )
            ProgressTile(
                label = stringResource(R.string.player_xp),
                value = stringResource(R.string.player_stat_value, player.totalXp),
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.xp_to_next_level, player.xpToNextLevel),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
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
