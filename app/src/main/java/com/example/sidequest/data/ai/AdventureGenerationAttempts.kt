package com.example.sidequest.data.ai

import com.example.sidequest.domain.adventure.Adventure
import com.example.sidequest.domain.adventure.InvalidAdventureResponseException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull

internal const val PRIMARY_ADVENTURE_MODEL = "gemini-3.5-flash-lite"
internal const val BACKUP_ADVENTURE_MODEL = "gemini-3.1-flash-lite"

internal class AdventureGenerationTimeoutException : Exception("Adventure generation timed out.")

internal fun Throwable.isAdventureGenerationTimeout(): Boolean =
    generateSequence(this) { it.cause }.take(8).any {
        it is AdventureGenerationTimeoutException || it is RequestTimeoutException || it is SocketTimeoutException
    }

internal fun Throwable.isAdventureServiceBusy(): Boolean =
    this is ServerException &&
        message?.startsWith("This model is currently experiencing high demand", ignoreCase = true) == true

internal suspend fun requestAdventureWithFallback(
    prompt: String,
    primaryTimeoutMillis: Long = 22_000L,
    backupTimeoutMillis: Long = 28_000L,
    request: suspend (modelName: String, timeoutMillis: Long, prompt: String) -> Adventure,
): Adventure {
    suspend fun attempt(modelName: String, timeoutMillis: Long, message: String): Adventure =
        withTimeoutOrNull(timeoutMillis) { request(modelName, timeoutMillis, message) }
            ?: throw AdventureGenerationTimeoutException()

    return try {
        attempt(PRIMARY_ADVENTURE_MODEL, primaryTimeoutMillis, prompt)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        currentCoroutineContext().ensureActive()
        val backupPrompt = when {
            error is InvalidAdventureResponseException ->
                "$prompt\nUse shorter sentences and include the story context in every chapter."
            error.isAdventureGenerationTimeout() || error.isAdventureServiceBusy() -> prompt
            else -> throw error
        }
        attempt(BACKUP_ADVENTURE_MODEL, backupTimeoutMillis, backupPrompt)
    }
}
