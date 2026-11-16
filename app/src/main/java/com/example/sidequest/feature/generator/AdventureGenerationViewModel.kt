package com.example.sidequest.feature.generator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sidequest.data.ai.AdventureAiUnavailableException
import com.example.sidequest.data.ai.FirebaseAdventureGenerator
import com.example.sidequest.domain.adventure.Adventure
import com.example.sidequest.domain.adventure.AdventureRequest
import com.example.sidequest.domain.adventure.InvalidAdventureResponseException
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.PermissionMissingException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.SerializationException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdventureGenerationState {
    data object Idle : AdventureGenerationState
    data object Loading : AdventureGenerationState
    data class Failed(val error: AdventureGenerationError) : AdventureGenerationState
    data class Ready(val adventure: Adventure) : AdventureGenerationState
}

enum class AdventureGenerationError {
    UNAVAILABLE,
    NETWORK,
    TIMED_OUT,
    QUOTA,
    INVALID_RESPONSE,
    UNKNOWN,
}

class AdventureGenerationViewModel : ViewModel() {
    private val generator = FirebaseAdventureGenerator()
    private val mutableState = MutableStateFlow<AdventureGenerationState>(AdventureGenerationState.Idle)
    val state = mutableState.asStateFlow()
    private var generationJob: Job? = null

    fun generate(request: AdventureRequest) {
        if (generationJob?.isActive == true) return

        mutableState.value = AdventureGenerationState.Loading
        generationJob = viewModelScope.launch {
            try {
                val adventure = generator.generate(request)
                ensureActive()
                mutableState.value = AdventureGenerationState.Ready(adventure)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ensureActive()
                val failure = error.toGenerationError()
                Log.w("AdventureGeneration", "Generation failed: ${failure.name} (${error.javaClass.simpleName})")
                mutableState.value = AdventureGenerationState.Failed(failure)
            }
        }
    }

    fun reset() {
        generationJob?.cancel()
        generationJob = null
        mutableState.value = AdventureGenerationState.Idle
    }
}

private fun Exception.toGenerationError(): AdventureGenerationError = when (this) {
    is AdventureAiUnavailableException,
    is APINotConfiguredException,
    is InvalidAPIKeyException,
    is PermissionMissingException,
    is ServiceDisabledException,
    is UnsupportedUserLocationException -> AdventureGenerationError.UNAVAILABLE

    is RequestTimeoutException -> AdventureGenerationError.TIMED_OUT
    is QuotaExceededException -> AdventureGenerationError.QUOTA

    is InvalidAdventureResponseException,
    is ContentBlockedException,
    is PromptBlockedException,
    is ResponseStoppedException,
    is SerializationException -> AdventureGenerationError.INVALID_RESPONSE

    else -> {
        val networkError = generateSequence<Throwable>(this) { it.cause }
            .take(8)
            .filterIsInstance<IOException>()
            .firstOrNull()
        when (networkError) {
            is SocketTimeoutException -> AdventureGenerationError.TIMED_OUT
            null -> AdventureGenerationError.UNKNOWN
            else -> AdventureGenerationError.NETWORK
        }
    }
}
