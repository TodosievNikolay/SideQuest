package com.example.sidequest.feature.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sidequest.R
import com.example.sidequest.domain.adventure.AdventureMood
import com.example.sidequest.domain.adventure.AdventureParty
import com.example.sidequest.domain.adventure.AdventureRequest
import com.example.sidequest.domain.adventure.QuestCity

private val MoodLabels = mapOf(
    AdventureMood.ADVENTURE to R.string.mood_adventure,
    AdventureMood.CHILL to R.string.mood_chill,
    AdventureMood.DISCOVERY to R.string.mood_discovery,
    AdventureMood.SOCIAL to R.string.mood_social,
)

@Composable
fun AdventureSetupScreen(
    state: AdventureGenerationState,
    onGenerate: (AdventureRequest) -> Unit,
    onBack: () -> Unit,
    citySearchState: CitySearchState,
    selectedCity: QuestCity?,
    onSearchCity: (String) -> Unit,
    onSelectCity: (QuestCity) -> Unit,
    onCityQueryChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var durationMinutes by rememberSaveable { mutableIntStateOf(60) }
    var mood by rememberSaveable { mutableStateOf(AdventureMood.ADVENTURE) }
    var isGroup by rememberSaveable { mutableStateOf(false) }
    var budget by rememberSaveable { mutableStateOf("") }
    var interests by rememberSaveable { mutableStateOf("") }
    var cityQuery by rememberSaveable { mutableStateOf(selectedCity?.name.orEmpty()) }
    val focusManager = LocalFocusManager.current
    val generating = state == AdventureGenerationState.Loading
    val maxBudgetPln = if (budget.isBlank()) 0 else budget.trim().toIntOrNull()?.takeIf { it >= 0 }
    val failure = state as? AdventureGenerationState.Failed

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back_to_home))
        }
        Text(
            text = stringResource(R.string.adventure_setup),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = cityQuery,
                onValueChange = {
                    cityQuery = it.take(100)
                    onCityQueryChanged()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.city)) },
                supportingText = { Text(stringResource(R.string.city_search_hint)) },
                enabled = !generating,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        if (cityQuery.isNotBlank() && !generating && citySearchState != CitySearchState.Loading) {
                            focusManager.clearFocus()
                            onSearchCity(cityQuery.trim())
                        }
                    },
                ),
                singleLine = true,
            )
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    onSearchCity(cityQuery.trim())
                },
                enabled = cityQuery.isNotBlank() && !generating && citySearchState != CitySearchState.Loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(if (citySearchState == CitySearchState.Loading) R.string.finding_city else R.string.find_city))
            }
            if (selectedCity != null) {
                Text(
                    text = stringResource(R.string.selected_city, selectedCity.displayName),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            when (citySearchState) {
                is CitySearchState.Found -> {
                    if (selectedCity == null) {
                        citySearchState.cities.forEach { city ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    focusManager.clearFocus()
                                    onSelectCity(city)
                                },
                                label = { Text(city.displayName) },
                                enabled = !generating,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                is CitySearchState.Failed -> Text(
                    text = stringResource(citySearchState.error.messageResource()),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> Unit
            }
        }
        SetupChoices(label = stringResource(R.string.available_time)) {
            AdventureRequest.DURATIONS.forEach { minutes ->
                FilterChip(
                    selected = durationMinutes == minutes,
                    onClick = { durationMinutes = minutes },
                    label = { Text(stringResource(R.string.duration_minutes, minutes)) },
                    enabled = !generating,
                )
            }
        }
        SetupChoices(label = stringResource(R.string.mood)) {
            MoodLabels.forEach { (option, label) ->
                FilterChip(
                    selected = mood == option,
                    onClick = { mood = option },
                    label = { Text(stringResource(label)) },
                    enabled = !generating,
                )
            }
        }
        SetupChoices(label = stringResource(R.string.companions)) {
            FilterChip(
                selected = !isGroup,
                onClick = { isGroup = false },
                label = { Text(stringResource(R.string.play_solo)) },
                enabled = !generating,
            )
            FilterChip(
                selected = isGroup,
                onClick = { isGroup = true },
                label = { Text(stringResource(R.string.play_in_group)) },
                enabled = !generating,
            )
        }
        OutlinedTextField(
            value = budget,
            onValueChange = { budget = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.maximum_budget)) },
            supportingText = {
                val message = if (maxBudgetPln == null) {
                    R.string.invalid_budget
                } else {
                    R.string.budget_optional_hint
                }
                Text(stringResource(message))
            },
            enabled = !generating,
            isError = maxBudgetPln == null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
        )
        OutlinedTextField(
            value = interests,
            onValueChange = { interests = it.take(AdventureRequest.MAX_INTERESTS_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.optional_interests)) },
            enabled = !generating,
            maxLines = 3,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (failure != null) {
                Text(
                    text = stringResource(failure.error.messageResource()),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = {
                    if (maxBudgetPln != null && selectedCity != null) {
                        focusManager.clearFocus()
                        onGenerate(
                            AdventureRequest(
                                durationMinutes = durationMinutes,
                                mood = mood,
                                party = if (isGroup) AdventureParty.GROUP else AdventureParty.SOLO,
                                maxBudgetPln = maxBudgetPln,
                                interests = interests.trim(),
                                city = selectedCity,
                            ),
                        )
                    }
                },
                enabled = maxBudgetPln != null && selectedCity != null && !generating,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                if (generating) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp).size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                val label = when {
                    generating -> R.string.generating_adventure
                    failure != null -> R.string.retry_generation
                    else -> R.string.generate_adventure
                }
                Text(stringResource(label))
            }
        }
    }
}

private fun CitySearchError.messageResource(): Int = when (this) {
    CitySearchError.NOT_FOUND -> R.string.city_not_found
    CitySearchError.NETWORK -> R.string.city_network_error
    CitySearchError.UNAVAILABLE -> R.string.city_unavailable
}

private fun AdventureGenerationError.messageResource(): Int = when (this) {
    AdventureGenerationError.UNAVAILABLE -> R.string.generation_unavailable
    AdventureGenerationError.NETWORK -> R.string.generation_network_error
    AdventureGenerationError.TIMED_OUT -> R.string.generation_timeout_error
    AdventureGenerationError.BUSY -> R.string.generation_busy_error
    AdventureGenerationError.QUOTA -> R.string.generation_quota_error
    AdventureGenerationError.INVALID_RESPONSE -> R.string.generation_invalid_response
    AdventureGenerationError.CITY_NOT_FOUND -> R.string.city_places_not_found
    AdventureGenerationError.CITY_NETWORK -> R.string.city_places_network_error
    AdventureGenerationError.CITY_UNAVAILABLE -> R.string.city_unavailable
    AdventureGenerationError.UNKNOWN -> R.string.generation_unknown_error
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupChoices(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}
