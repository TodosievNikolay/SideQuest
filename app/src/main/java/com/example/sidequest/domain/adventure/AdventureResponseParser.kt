package com.example.sidequest.domain.adventure

import java.util.UUID
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

class InvalidAdventureResponseException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

object AdventureResponseParser {
    const val CHAPTER_COUNT = 3

    private const val MAX_TITLE_LENGTH = 100
    private const val MAX_INTRODUCTION_LENGTH = 800
    private const val MAX_MISSION_LENGTH = 800
    private const val MAX_COMPLETION_LENGTH = 400

    fun parse(json: String, request: AdventureRequest): Adventure {
        val response = try {
            Json.parseToJsonElement(json) as? JsonObject
        } catch (error: SerializationException) {
            throw InvalidAdventureResponseException("The response contains invalid JSON.", error)
        } ?: throw InvalidAdventureResponseException("The response must be a JSON object.")

        val chapters = response["chapters"] as? JsonArray
        if (chapters == null || chapters.size != CHAPTER_COUNT) {
            throw InvalidAdventureResponseException("The adventure must have exactly three chapters.")
        }

        return Adventure(
            id = UUID.randomUUID().toString(),
            title = response.requiredText("title", MAX_TITLE_LENGTH),
            introduction = response.requiredText("introduction", MAX_INTRODUCTION_LENGTH),
            estimatedDurationMinutes = response.requiredInt("estimatedDurationMinutes", 1..request.durationMinutes),
            estimatedCostPln = response.requiredInt("estimatedCostPln", 0..request.maxBudgetPln),
            chapters = chapters.map { value ->
                val chapter = value as? JsonObject
                    ?: throw InvalidAdventureResponseException("Each chapter must be a JSON object.")
                AdventureChapter(
                    title = chapter.requiredText("title", MAX_TITLE_LENGTH),
                    mission = chapter.requiredText("mission", MAX_MISSION_LENGTH),
                    completionCriteria = chapter.requiredText("completionCriteria", MAX_COMPLETION_LENGTH),
                )
            },
        )
    }

    private fun JsonObject.requiredText(key: String, maxLength: Int): String {
        val value = this[key] as? JsonPrimitive
        if (value?.isString != true) {
            throw InvalidAdventureResponseException("$key must be a string.")
        }
        val trimmed = value.content.trim()
        if (trimmed.isEmpty() || trimmed.length > maxLength) {
            throw InvalidAdventureResponseException("$key is empty or too long.")
        }
        return trimmed
    }

    private fun JsonObject.requiredInt(key: String, range: IntRange): Int {
        val primitive = this[key] as? JsonPrimitive
        val value = if (primitive?.isString == false) primitive.intOrNull else null
        if (value == null || value !in range) {
            throw InvalidAdventureResponseException("$key must be an integer within the allowed range.")
        }
        return value
    }
}
