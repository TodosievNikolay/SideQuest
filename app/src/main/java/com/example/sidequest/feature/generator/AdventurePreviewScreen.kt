package com.example.sidequest.feature.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sidequest.R
import com.example.sidequest.domain.adventure.Adventure
import com.example.sidequest.feature.quest.QuestChapterCard
import com.example.sidequest.feature.quest.MappedPlacesNotice

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdventurePreviewScreen(
    adventure: Adventure,
    onBack: () -> Unit,
    onStartQuest: () -> Unit,
    startingQuest: Boolean,
    startFailed: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(onClick = onBack, enabled = !startingQuest) {
            Text(stringResource(R.string.edit_adventure_options))
        }
        Text(
            text = adventure.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        adventure.cityName?.let { city ->
            Text(
                text = city,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Estimate(
                text = stringResource(R.string.estimated_duration_minutes, adventure.estimatedDurationMinutes),
            )
            val cost = if (adventure.estimatedCostPln == 0) {
                stringResource(R.string.estimated_cost_free)
            } else {
                stringResource(R.string.estimated_cost_pln, adventure.estimatedCostPln)
            }
            Estimate(text = cost)
        }
        Text(
            text = adventure.introduction,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStartQuest,
                enabled = !startingQuest,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(stringResource(if (startingQuest) R.string.quest_starting else R.string.start_quest))
            }
            if (startFailed) {
                Text(
                    text = stringResource(R.string.quest_start_error),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        adventure.chapters.forEachIndexed { index, chapter ->
            QuestChapterCard(
                label = stringResource(R.string.adventure_chapter_number, index + 1),
                chapter = chapter,
            )
        }
        if (adventure.chapters.any { it.place != null }) {
            MappedPlacesNotice()
        }
    }
}

@Composable
private fun Estimate(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
