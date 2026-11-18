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
        assertEquals("Clue 1 points to an everyday pattern.", adventure.chapters.first().storyContext)
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
        assertInvalid(
            response().replace(
                "Take a short note.",
                "a".repeat(AdventureResponseParser.MAX_COMPLETION_LENGTH + 1),
            ),
        )
        assertInvalid(response().replace(", \"completionCriteria\": \"Take a short note.\"", ""))
    }

    @Test
    fun requiresShortStoryContextForNewChapters() {
        val context = "Clue 1 points to an everyday pattern."
        assertInvalid(response().replace("\"storyContext\": \"$context\",", ""))
        assertInvalid(response().replace(context, " "))
        assertInvalid(response().replace(context, "a".repeat(AdventureResponseParser.MAX_STORY_CONTEXT_LENGTH + 1)))
    }

    @Test
    fun rejectsRepeatedTitlesAndMissions() {
        assertInvalid(response().replace("\"Chapter 2\"", "\"chapter 1\""))
        assertInvalid(response().replace("Find architectural detail 2.", "Find architectural detail 1."))
    }

    @Test
    fun usesMappedPlacesAndCityFromTheApplication() {
        val city = QuestCity("city:1", "Odesa", "Odesa Oblast", "Ukraine", 46.48, 30.73, "https://www.openstreetmap.org/relation/1")
        val places = (1..3).map { index ->
            QuestPlace("way:$index", "Square $index", "Square", 46.48, 30.73, "https://www.openstreetmap.org/way/$index")
        }
        val adventure = AdventureResponseParser.parse(response(), request.copy(city = city), places)

        assertEquals(city.displayName, adventure.cityName)
        assertEquals(places, adventure.chapters.map { it.place })
    }

    @Test
    fun rejectsCityAdventureWithoutThreeMappedLocations() {
        val city = QuestCity("city:1", "Odesa", "Odesa Oblast", "Ukraine", 46.48, 30.73, "https://www.openstreetmap.org/relation/1")
        assertInvalid(response(), request.copy(city = city))
    }

    @Test
    fun preservesOneSettlementAreaAcrossThreeDistinctMissions() {
        val city = QuestCity("node:12", "Small Village", "", "Poland", 54.7, 18.0, "https://www.openstreetmap.org/node/12")
        val area = QuestPlace(city.id, city.name, QuestPlace.SETTLEMENT_AREA_CATEGORY, city.latitude, city.longitude, city.sourceUrl)

        val adventure = AdventureResponseParser.parse(response(), request.copy(city = city), List(3) { area })

        assertEquals(List(3) { area }, adventure.chapters.map { it.place })
        assertEquals(3, adventure.chapters.distinctBy { it.mission }.size)
        assertEquals(city.displayName, adventure.cityName)
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
            """
                {
                    "title": "Chapter $index",
                    "storyContext": "Clue $index points to an everyday pattern.",
                    "mission": "Find architectural detail $index.", "completionCriteria": "Take a short note."
                }
            """.trimIndent()
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
