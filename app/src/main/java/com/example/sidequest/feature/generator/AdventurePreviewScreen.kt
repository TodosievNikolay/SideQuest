package com.example.sidequest.feature.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sidequest.R
import com.example.sidequest.domain.adventure.Adventure
import com.example.sidequest.domain.adventure.AdventureChapter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdventurePreviewScreen(
    adventure: Adventure,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.edit_adventure_options))
        }
        Text(
            text = adventure.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
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
            style = MaterialTheme.typography.bodyLarge,
        )
        adventure.chapters.forEachIndexed { index, chapter ->
            ChapterCard(number = index + 1, chapter = chapter)
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

@Composable
private fun ChapterCard(number: Int, chapter: AdventureChapter) {
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
                    text = stringResource(R.string.adventure_chapter_number, number),
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
            Text(text = chapter.mission, style = MaterialTheme.typography.bodyMedium)
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
