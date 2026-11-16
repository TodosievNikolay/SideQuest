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

class MainActivity : ComponentActivity() {
    private val adventureViewModel by viewModels<AdventureGenerationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SideQuestTheme {
                SideQuestApp(adventureViewModel)
            }
        }
    }
}

@Composable
private fun SideQuestApp(adventureViewModel: AdventureGenerationViewModel) {
    var showingSetup by rememberSaveable { mutableStateOf(false) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val generationState by adventureViewModel.state.collectAsStateWithLifecycle()
    val preview = (generationState as? AdventureGenerationState.Ready)?.adventure

    val closeSetup = {
        adventureViewModel.reset()
        showingSetup = false
    }
    BackHandler(enabled = showingSetup) {
        if (preview != null) {
            adventureViewModel.reset()
        } else {
            closeSetup()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            val contentModifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
            when {
                !showingSetup -> HomeScreen(
                    onStartAdventure = { showingSetup = true },
                    modifier = contentModifier,
                )
                preview != null -> AdventurePreviewScreen(
                    adventure = preview,
                    onBack = adventureViewModel::reset,
                    modifier = contentModifier,
                )
                else -> saveableStateHolder.SaveableStateProvider("adventureSetup") {
                    AdventureSetupScreen(
                        state = generationState,
                        onGenerate = adventureViewModel::generate,
                        onBack = closeSetup,
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}
