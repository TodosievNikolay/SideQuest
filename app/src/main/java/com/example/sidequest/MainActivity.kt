package com.example.sidequest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sidequest.core.ui.SideQuestTheme
import com.example.sidequest.feature.generator.AdventureGenerationState
import com.example.sidequest.feature.generator.AdventureGenerationViewModel
import com.example.sidequest.feature.generator.AdventurePreviewScreen
import com.example.sidequest.feature.generator.AdventureSetupScreen
import com.example.sidequest.feature.home.HomeScreen
import com.example.sidequest.feature.quest.QuestLoadState
import com.example.sidequest.feature.quest.QuestProgressViewModel
import com.example.sidequest.feature.quest.QuestScreen
import com.example.sidequest.feature.quest.QuestWriteState

class MainActivity : ComponentActivity() {
    private val adventureViewModel by viewModels<AdventureGenerationViewModel>()
    private val questViewModel by viewModels<QuestProgressViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SideQuestTheme {
                SideQuestApp(adventureViewModel, questViewModel)
            }
        }
    }
}

private enum class Destination { HOME, SETUP, QUEST }

@Composable
private fun SideQuestApp(
    adventureViewModel: AdventureGenerationViewModel,
    questViewModel: QuestProgressViewModel,
) {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val generationState by adventureViewModel.state.collectAsStateWithLifecycle()
    val citySearchState by adventureViewModel.citySearchState.collectAsStateWithLifecycle()
    val selectedCity by adventureViewModel.selectedCity.collectAsStateWithLifecycle()
    val questState by questViewModel.questState.collectAsStateWithLifecycle()
    val questWriteState by questViewModel.writeState.collectAsStateWithLifecycle()
    val preview = (generationState as? AdventureGenerationState.Ready)?.adventure
    val savedQuest = (questState as? QuestLoadState.Ready)?.quest
    val savingQuest = questWriteState == QuestWriteState.SAVING || questWriteState == QuestWriteState.STARTED

    val goHome = {
        questViewModel.clearWriteResult()
        adventureViewModel.reset()
        destination = Destination.HOME
    }
    val editAdventure = {
        questViewModel.clearWriteResult()
        adventureViewModel.reset()
    }
    BackHandler(enabled = destination != Destination.HOME) {
        if (!savingQuest) {
            if (destination == Destination.SETUP && preview != null) {
                editAdventure()
            } else {
                goHome()
            }
        }
    }

    LaunchedEffect(questWriteState, questState) {
        if (questWriteState == QuestWriteState.STARTED &&
            (savedQuest?.isComplete == false || questState == QuestLoadState.Failed)
        ) {
            destination = Destination.QUEST
            adventureViewModel.reset()
            questViewModel.clearWriteResult()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            val contentModifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
            when {
                destination == Destination.HOME -> HomeScreen(
                    onStartAdventure = { destination = Destination.SETUP },
                    onContinueAdventure = { destination = Destination.QUEST },
                    questState = questState,
                    modifier = contentModifier,
                )
                destination == Destination.QUEST -> QuestScreen(
                    questState = questState,
                    saving = savingQuest,
                    saveFailed = questWriteState == QuestWriteState.FAILED,
                    onCompleteMission = questViewModel::completeChapter,
                    onBack = goHome,
                    modifier = contentModifier,
                )
                preview != null -> AdventurePreviewScreen(
                    adventure = preview,
                    onBack = editAdventure,
                    onStartQuest = { questViewModel.startQuest(preview) },
                    startingQuest = savingQuest,
                    startFailed = questWriteState == QuestWriteState.FAILED,
                    modifier = contentModifier,
                )
                else -> saveableStateHolder.SaveableStateProvider("adventureSetup") {
                    AdventureSetupScreen(
                        state = generationState,
                        onGenerate = adventureViewModel::generate,
                        onBack = goHome,
                        citySearchState = citySearchState,
                        selectedCity = selectedCity,
                        onSearchCity = adventureViewModel::searchCities,
                        onSelectCity = adventureViewModel::selectCity,
                        onCityQueryChanged = adventureViewModel::clearCitySelection,
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}
