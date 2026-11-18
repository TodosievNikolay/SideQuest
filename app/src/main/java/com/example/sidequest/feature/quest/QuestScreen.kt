package com.example.sidequest.feature.quest

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sidequest.R
import com.example.sidequest.domain.adventure.AdventureChapter
import com.example.sidequest.domain.adventure.isSettlementArea
import com.example.sidequest.domain.adventure.PlayerProgress
import com.example.sidequest.domain.adventure.QuestProgress

@Composable
fun QuestScreen(
    questState: QuestLoadState,
    saving: Boolean,
    saveFailed: Boolean,
    onCompleteMission: (String, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quest = (questState as? QuestLoadState.Ready)?.quest
    key(quest?.adventure?.id, quest?.completedChapters) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (quest?.isComplete == true) {
                CompletedQuest(quest = quest, onBack = onBack)
            } else {
                TextButton(onClick = onBack, enabled = !saving) {
                    Text(stringResource(R.string.back_to_home))
                }
                val chapter = quest?.currentChapter
                when {
                    questState == QuestLoadState.Loading -> QuestStatus(R.string.quest_loading)
                    questState == QuestLoadState.Failed -> QuestStatus(R.string.quest_load_error, error = true)
                    quest == null -> QuestStatus(R.string.no_active_quest)
                    chapter == null -> QuestStatus(R.string.quest_load_error, error = true)
                    else -> CurrentMission(
                        quest = quest,
                        chapter = chapter,
                        saving = saving,
                        saveFailed = saveFailed,
                        onCompleteMission = onCompleteMission,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentMission(
    quest: QuestProgress,
    chapter: AdventureChapter,
    saving: Boolean,
    saveFailed: Boolean,
    onCompleteMission: (String, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = quest.adventure.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        quest.adventure.cityName?.let { city ->
            Text(
                text = city,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            text = quest.adventure.introduction,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    QuestChapterCard(
        label = stringResource(
            R.string.quest_chapter_progress,
            quest.completedChapters + 1,
            quest.adventure.chapters.size,
        ),
        chapter = chapter,
    )
    if (chapter.place != null) {
        MappedPlacesNotice()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.mission_xp_reward, PlayerProgress.XP_PER_CHAPTER),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge,
        )
        Button(
            onClick = { onCompleteMission(quest.adventure.id, quest.completedChapters) },
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(stringResource(if (saving) R.string.quest_saving_progress else R.string.complete_mission))
        }
        if (saveFailed) {
            QuestStatus(R.string.quest_progress_error, error = true)
        }
    }
}

@Composable
private fun CompletedQuest(quest: QuestProgress, onBack: () -> Unit) {
    Text(
        text = stringResource(R.string.quest_completed),
        modifier = Modifier.semantics {
            heading()
            liveRegion = LiveRegionMode.Polite
        },
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = quest.adventure.title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            quest.adventure.cityName?.let { city ->
                Text(
                    text = city,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.quest_completed_chapters, quest.adventure.chapters.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    R.string.quest_xp_earned,
                    quest.completedChapters * PlayerProgress.XP_PER_CHAPTER,
                ),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    Button(
        onClick = onBack,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.back_to_home))
    }
}

@Composable
private fun QuestStatus(message: Int, error: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = stringResource(message),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = if (error) colors.error else colors.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun QuestChapterCard(label: String, chapter: AdventureChapter) {
    val storyContext = chapter.storyContext?.takeIf { it.isNotBlank() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = chapter.title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            chapter.place?.let { place ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (place.isSettlementArea) {
                            stringResource(R.string.settlement_area_name, place.name)
                        } else {
                            place.name
                        },
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (place.isSettlementArea) {
                        Text(
                            text = stringResource(R.string.settlement_area_note),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    val mapZoom = if (place.isSettlementArea) 15 else 17
                    ExternalMapLink(
                        label = stringResource(R.string.open_map),
                        uri = "https://www.openstreetmap.org/?mlat=${place.latitude}&mlon=${place.longitude}#map=$mapZoom/${place.latitude}/${place.longitude}",
                    )
                }
            }
            if (storyContext != null) {
                Text(
                    text = storyContext,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (storyContext != null) {
                    Text(
                        text = stringResource(R.string.chapter_task),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(text = chapter.mission, style = MaterialTheme.typography.bodyMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.completion_criteria),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = chapter.completionCriteria,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun MappedPlacesNotice() {
    Column {
        Text(
            text = stringResource(R.string.mapped_places_note),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        ExternalMapLink(
            label = stringResource(R.string.openstreetmap_attribution),
            uri = "https://www.openstreetmap.org/copyright",
        )
    }
}

@Composable
private fun ExternalMapLink(label: String, uri: String) {
    val uriHandler = LocalUriHandler.current
    var openFailed by remember(uri) { mutableStateOf(false) }
    Column {
        TextButton(
            onClick = {
                openFailed = try {
                    uriHandler.openUri(uri)
                    false
                } catch (_: ActivityNotFoundException) {
                    true
                } catch (_: IllegalArgumentException) {
                    true
                }
            },
        ) {
            Text(label)
        }
        if (openFailed) {
            Text(
                text = stringResource(R.string.map_open_error),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
