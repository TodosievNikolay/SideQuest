package com.example.sidequest.feature.generator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sidequest.data.ai.AdventureAiUnavailableException
import com.example.sidequest.data.ai.FirebaseAdventureGenerator
import com.example.sidequest.data.ai.isAdventureGenerationTimeout
import com.example.sidequest.data.ai.isAdventureServiceBusy
import com.example.sidequest.data.places.PhotonPlacesService
import com.example.sidequest.data.places.PlaceLookupError
import com.example.sidequest.data.places.PlaceLookupException
import com.example.sidequest.domain.adventure.Adventure
import com.example.sidequest.domain.adventure.AdventureRequest
import com.example.sidequest.domain.adventure.InvalidAdventureResponseException
import com.example.sidequest.domain.adventure.QuestCity
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.PermissionMissingException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.SerializationException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import java.io.IOException
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
    BUSY,
    QUOTA,
    INVALID_RESPONSE,
    CITY_NOT_FOUND,
    CITY_NETWORK,
    CITY_UNAVAILABLE,
    UNKNOWN,
}

sealed interface CitySearchState {
    data object Idle : CitySearchState
    data object Loading : CitySearchState
    data class Found(val cities: List<QuestCity>) : CitySearchState
    data class Failed(val error: CitySearchError) : CitySearchState
}

enum class CitySearchError { NOT_FOUND, NETWORK, UNAVAILABLE }

class AdventureGenerationViewModel : ViewModel() {
    private val places = PhotonPlacesService()
    private val generator = FirebaseAdventureGenerator(places)
    private val mutableState = MutableStateFlow<AdventureGenerationState>(AdventureGenerationState.Idle)
    val state = mutableState.asStateFlow()
    private var generationJob: Job? = null
    private var citySearchJob: Job? = null
    private val mutableCitySearchState = MutableStateFlow<CitySearchState>(CitySearchState.Idle)
    val citySearchState = mutableCitySearchState.asStateFlow()
    private val mutableSelectedCity = MutableStateFlow<QuestCity?>(null)
    val selectedCity = mutableSelectedCity.asStateFlow()

    fun searchCities(query: String) {
        clearCitySelection()
        mutableCitySearchState.value = CitySearchState.Loading
        citySearchJob = viewModelScope.launch {
            try {
                val cities = places.searchCities(query)
                ensureActive()
                mutableCitySearchState.value = if (cities.isEmpty()) {
                    CitySearchState.Failed(CitySearchError.NOT_FOUND)
                } else {
                    CitySearchState.Found(cities)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: PlaceLookupException) {
                ensureActive()
                mutableCitySearchState.value = CitySearchState.Failed(
                    when (error.reason) {
                        PlaceLookupError.NOT_FOUND -> CitySearchError.NOT_FOUND
                        PlaceLookupError.NETWORK -> CitySearchError.NETWORK
                        PlaceLookupError.UNAVAILABLE -> CitySearchError.UNAVAILABLE
                    },
                )
            } catch (_: Exception) {
                ensureActive()
                mutableCitySearchState.value = CitySearchState.Failed(CitySearchError.UNAVAILABLE)
            }
        }
    }

    fun selectCity(city: QuestCity) {
        val candidates = (mutableCitySearchState.value as? CitySearchState.Found)?.cities.orEmpty()
        if (city in candidates) {
            mutableSelectedCity.value = city
        }
    }

    fun clearCitySelection() {
        citySearchJob?.cancel()
        mutableSelectedCity.value = null
        mutableCitySearchState.value = CitySearchState.Idle
    }

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
        citySearchJob?.cancel()
        if (mutableCitySearchState.value == CitySearchState.Loading) {
            mutableCitySearchState.value = CitySearchState.Idle
        }
        mutableState.value = AdventureGenerationState.Idle
    }
}

private fun Exception.toGenerationError(): AdventureGenerationError = when (this) {
    is PlaceLookupException -> when (reason) {
        PlaceLookupError.NOT_FOUND -> AdventureGenerationError.CITY_NOT_FOUND
        PlaceLookupError.NETWORK -> AdventureGenerationError.CITY_NETWORK
        PlaceLookupError.UNAVAILABLE -> AdventureGenerationError.CITY_UNAVAILABLE
    }
    is AdventureAiUnavailableException,
    is APINotConfiguredException,
    is InvalidAPIKeyException,
    is PermissionMissingException,
    is ServiceDisabledException,
    is UnsupportedUserLocationException -> AdventureGenerationError.UNAVAILABLE

    is QuotaExceededException -> AdventureGenerationError.QUOTA

    is InvalidAdventureResponseException,
    is ContentBlockedException,
    is PromptBlockedException,
    is ResponseStoppedException,
    is SerializationException -> AdventureGenerationError.INVALID_RESPONSE

    else -> when {
        isAdventureGenerationTimeout() -> AdventureGenerationError.TIMED_OUT
        isAdventureServiceBusy() -> AdventureGenerationError.BUSY
        generateSequence<Throwable>(this) { it.cause }.take(8).any { it is IOException } ->
            AdventureGenerationError.NETWORK
        else -> AdventureGenerationError.UNKNOWN
    }
}
