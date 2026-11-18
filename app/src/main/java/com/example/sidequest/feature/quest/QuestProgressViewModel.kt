package com.example.sidequest.feature.quest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sidequest.data.quest.QuestRepository
import com.example.sidequest.domain.adventure.Adventure
import com.example.sidequest.domain.adventure.PlayerProgress
import com.example.sidequest.domain.adventure.QuestProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface QuestLoadState {
    data object Loading : QuestLoadState
    data object Failed : QuestLoadState
    data class Ready(val quest: QuestProgress?, val player: PlayerProgress) : QuestLoadState
}

enum class QuestWriteState { IDLE, SAVING, STARTED, FAILED }

class QuestProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuestRepository(application)

    val questState = repository.latestQuest
        .combine<QuestProgress?, PlayerProgress, QuestLoadState>(repository.playerProgress) { quest, player ->
            QuestLoadState.Ready(quest, player)
        }
        .catch { error ->
            Log.w("QuestProgress", "Could not load quest progress (${error.javaClass.simpleName})")
            emit(QuestLoadState.Failed)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuestLoadState.Loading)

    private val mutableWriteState = MutableStateFlow(QuestWriteState.IDLE)
    val writeState = mutableWriteState.asStateFlow()

    fun startQuest(adventure: Adventure) = write {
        if (repository.start(adventure)) QuestWriteState.STARTED else QuestWriteState.FAILED
    }

    fun completeChapter(questId: String, expectedCompletedChapters: Int) = write {
        repository.completeChapter(questId, expectedCompletedChapters)
        QuestWriteState.IDLE
    }

    fun clearWriteResult() {
        if (mutableWriteState.value != QuestWriteState.SAVING) {
            mutableWriteState.value = QuestWriteState.IDLE
        }
    }

    private fun write(operation: suspend () -> QuestWriteState) {
        if (mutableWriteState.value == QuestWriteState.SAVING) return

        mutableWriteState.value = QuestWriteState.SAVING
        viewModelScope.launch {
            mutableWriteState.value = try {
                operation()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("QuestProgress", "Could not save quest progress (${error.javaClass.simpleName})")
                QuestWriteState.FAILED
            }
        }
    }
}
