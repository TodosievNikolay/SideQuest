package com.example.sidequest.data.ai

import com.example.sidequest.domain.adventure.Adventure
import com.example.sidequest.domain.adventure.AdventureChapter
import com.example.sidequest.domain.adventure.InvalidAdventureResponseException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AdventureGenerationAttemptsTest {
    @Test(timeout = 5_000)
    fun primaryDeadlineSwitchesModelsAndKeepsThePrompt() = runBlocking {
        val attempts = mutableListOf<Triple<String, Long, String>>()
        val result = requestAdventureWithFallback(PROMPT, primaryTimeoutMillis = 25, backupTimeoutMillis = 1_000) {
            modelName, timeoutMillis, prompt ->
            attempts += Triple(modelName, timeoutMillis, prompt)
            if (modelName == PRIMARY_ADVENTURE_MODEL) awaitCancellation()
            adventure
        }

        assertSame(adventure, result)
        assertEquals(
            listOf(
                Triple(PRIMARY_ADVENTURE_MODEL, 25L, PROMPT),
                Triple(BACKUP_ADVENTURE_MODEL, 1_000L, PROMPT),
            ),
            attempts,
        )
    }

    @Test(timeout = 5_000)
    fun callerCancellationStopsAfterThePrimaryModel() = runBlocking {
        val models = mutableListOf<String>()
        val generation = launch(start = CoroutineStart.UNDISPATCHED) {
            requestAdventureWithFallback(PROMPT) { modelName, _, _ ->
                models += modelName
                awaitCancellation()
            }
        }

        generation.cancelAndJoin()

        assertTrue(generation.isCancelled)
        assertEquals(listOf(PRIMARY_ADVENTURE_MODEL), models)
    }

    @Test
    fun networkErrorIsPropagatedWithoutSwitchingModels() {
        val failure = UnknownHostException("Offline")
        val models = mutableListOf<String>()
        val actual = assertThrows(UnknownHostException::class.java) {
            runBlocking {
                requestAdventureWithFallback(PROMPT) { modelName, _, _ ->
                    models += modelName
                    throw failure
                }
            }
        }

        assertEquals(failure.javaClass, actual.javaClass)
        assertEquals(failure.message, actual.message)
        assertEquals(listOf(PRIMARY_ADVENTURE_MODEL), models)
    }

    @Test(timeout = 5_000)
    fun wrappedSocketTimeoutSwitchesModelsAndBackupDeadlineEndsTheRequest() {
        val models = mutableListOf<String>()
        val failure = assertThrows(AdventureGenerationTimeoutException::class.java) {
            runBlocking {
                requestAdventureWithFallback(PROMPT, primaryTimeoutMillis = 1_000, backupTimeoutMillis = 25) {
                    modelName, _, _ ->
                    models += modelName
                    if (modelName == PRIMARY_ADVENTURE_MODEL) {
                        throw IOException("Transport error", SocketTimeoutException("Read timed out"))
                    }
                    awaitCancellation()
                }
            }
        }

        assertTrue(failure.isAdventureGenerationTimeout())
        assertEquals(listOf(PRIMARY_ADVENTURE_MODEL, BACKUP_ADVENTURE_MODEL), models)
    }

    @Test(timeout = 5_000)
    fun invalidResponseAddsGuidanceToTheBackupPrompt() = runBlocking {
        val attempts = mutableListOf<Pair<String, String>>()
        val result = requestAdventureWithFallback(PROMPT) { modelName, _, prompt ->
            attempts += modelName to prompt
            if (modelName == PRIMARY_ADVENTURE_MODEL) throw InvalidAdventureResponseException("Missing chapter")
            adventure
        }

        assertSame(adventure, result)
        assertEquals(listOf(PRIMARY_ADVENTURE_MODEL, BACKUP_ADVENTURE_MODEL), attempts.map { it.first })
        assertEquals(PROMPT, attempts[0].second)
        assertTrue(attempts[1].second.startsWith("$PROMPT\n"))
        assertTrue(attempts[1].second.length > PROMPT.length + 1)
    }

    private val adventure = Adventure(
        id = "quest",
        title = "Hidden Repeats",
        introduction = "Look for three clues on a walk.",
        estimatedDurationMinutes = 30,
        estimatedCostPln = 0,
        chapters = (1..3).map { number ->
            AdventureChapter(
                title = "Clue $number",
                mission = "Find detail $number.",
                completionCriteria = "Record detail $number.",
                storyContext = "This detail adds to the pattern.",
            )
        },
    )

    private companion object {
        const val PROMPT = "Write an adventure about architecture."
    }
}
