package com.example.sidequest.domain.adventure

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AdventureResponseParserTest {
    private val request = AdventureRequest(
        durationMinutes = 60,
        mood = AdventureMood.DISCOVERY,
        party = AdventureParty.SOLO,
        maxBudgetPln = 30,
        interests = "Architecture",
    )

    @Test
    fun parsesThreeChaptersAndAssignsAnIdLocally() {
        val adventure = AdventureResponseParser.parse(response(), request)
        val secondAdventure = AdventureResponseParser.parse(response(), request)

        assertEquals("Hidden Details", adventure.title)
        assertEquals(60, adventure.estimatedDurationMinutes)
        assertEquals(30, adventure.estimatedCostPln)
        assertEquals(3, adventure.chapters.size)
        assertEquals("Take a short note.", adventure.chapters.first().completionCriteria)
        assertEquals(adventure.id, UUID.fromString(adventure.id).toString())
        assertNotEquals(adventure.id, secondAdventure.id)
    }

    @Test
    fun rejectsValuesOutsideThePlayersLimits() {
        assertInvalid(response(duration = "61"))
        assertInvalid(response(duration = "0"))
        assertInvalid(response(cost = "31"))
        assertInvalid(response(cost = "-1"))
        assertInvalid(response(cost = "1"), request.copy(maxBudgetPln = 0))
        assertEquals(
            0,
            AdventureResponseParser.parse(response(cost = "0"), request.copy(maxBudgetPln = 0))
                .estimatedCostPln,
        )
    }

    @Test
    fun requiresExactlyThreeChapters() {
        assertInvalid(response(chapterCount = 2))
        assertInvalid(response(chapterCount = 4))
    }

    @Test
    fun rejectsWrongTypesAndMissingOrBlankText() {
        assertInvalid(response(duration = "\"60\""))
        assertInvalid(response(cost = "30.0"))
        assertInvalid(response().replace("\"Hidden Details\"", "true"))
        assertInvalid(response().replace("Take a short note.", " "))
        assertInvalid(response().replace("Take a short note.", "a".repeat(401)))
        assertInvalid(response().replace(", \"completionCriteria\": \"Take a short note.\"", ""))
    }

    @Test
    fun rejectsMalformedJsonAndNonObjectResponses() {
        assertInvalid("{")
        assertInvalid("[]")
    }

    private fun assertInvalid(json: String, request: AdventureRequest = this.request) {
        assertThrows(InvalidAdventureResponseException::class.java) {
            AdventureResponseParser.parse(json, request)
        }
    }

    private fun response(
        duration: String = "60",
        cost: String = "30",
        chapterCount: Int = 3,
    ): String {
        val chapters = (1..chapterCount).joinToString { index ->
            """{"title": "Chapter $index", "mission": "Find an architectural detail.", "completionCriteria": "Take a short note."}"""
        }
        return """
            {
                "id": "model-controlled-id",
                "title": "Hidden Details",
                "introduction": "Look for repeating patterns as you walk.",
                "estimatedDurationMinutes": $duration,
                "estimatedCostPln": $cost,
                "chapters": [$chapters]
            }
        """.trimIndent()
    }
}
