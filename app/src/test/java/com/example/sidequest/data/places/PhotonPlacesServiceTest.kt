package com.example.sidequest.data.places

import com.example.sidequest.domain.adventure.QuestCity
import com.example.sidequest.domain.adventure.QuestPlace
import com.example.sidequest.domain.adventure.isSettlementArea
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotonPlacesServiceTest {
    private val village = QuestCity(
        id = "node:31529076",
        name = "Żarnowiec",
        region = "Pomeranian Voivodeship",
        country = "Poland",
        latitude = 54.7887218,
        longitude = 18.0812105,
        sourceUrl = "https://www.openstreetmap.org/node/31529076",
    )

    @Test
    fun searchesSmallSettlementsEvenWhenFuzzyCityResultsExist() = runBlocking {
        val requests = mutableListOf<List<Pair<String, String>>>()
        val service = PhotonPlacesService { path, parameters ->
            assertEquals("api/", path)
            requests += parameters
            if (parameters.contains("osm_tag" to "place:city")) {
                collection(settlement(1, "Unrelated City", "city", "United States"))
            } else {
                collection(settlement(2, "Y", "village", "France"))
            }
        }

        val cities = service.searchCities(" Y ")

        assertEquals(listOf("Unrelated City", "Y"), cities.map { it.name })
        assertEquals(2, requests.size)
        assertTrue(requests.all { it.contains("q" to "Y") && it.contains("limit" to "5") })
        assertEquals(
            listOf("place:town", "place:village", "place:hamlet", "place:municipality"),
            requests[1].filter { it.first == "osm_tag" }.map { it.second },
        )
    }

    @Test
    fun interleavesBothGroupsAndKeepsUkrainianOdesaAlongsideNamesakeVillages() = runBlocking {
        val service = PhotonPlacesService { _, parameters ->
            if (parameters.contains("osm_tag" to "place:city")) {
                collection(
                    settlement(1, "Odessa", "city", "United States"),
                    settlement(2, "Odesa", "city", "Ukraine"),
                )
            } else {
                collection(
                    settlement(3, "Odessa", "village", "United States"),
                    settlement(4, "Odessa", "village", "Canada"),
                    settlement(1, "Odessa", "city", "United States"),
                )
            }
        }

        val cities = service.searchCities("Odessa")

        assertEquals(listOf("node:1", "node:3", "node:2", "node:4"), cities.map { it.id })
        assertEquals("Ukraine", cities[2].country)
    }

    @Test
    fun keepsBothGroupsBoundedAndDeduplicatesByActualSource() = runBlocking {
        val service = PhotonPlacesService { _, parameters ->
            val cityGroup = parameters.contains("osm_tag" to "place:city")
            val start = if (cityGroup) 1 else 11
            collection(*(start..start + 7).map {
                settlement(it.toLong(), "Settlement $it", if (cityGroup) "city" else "town", "Poland")
            }.toTypedArray())
        }

        val cities = service.searchCities("Settlement")

        assertEquals(10, cities.size)
        assertEquals(listOf(1L, 11L, 2L, 12L, 3L, 13L, 4L, 14L, 5L, 15L), cities.map {
            it.id.substringAfter(':').toLong()
        })
    }

    @Test
    fun emptyValidPoiResponseUsesOnlyTheSelectedMappedSettlement() = runBlocking {
        val service = PhotonPlacesService { path, _ ->
            assertEquals("reverse/", path)
            collection()
        }

        val places = service.chapterPlaces(village, 60)
        val expected = QuestPlace(
            id = village.id,
            name = village.name,
            category = QuestPlace.SETTLEMENT_AREA_CATEGORY,
            latitude = village.latitude,
            longitude = village.longitude,
            sourceUrl = village.sourceUrl,
        )

        assertEquals(List(3) { expected }, places)
        assertTrue(places.all { it.isSettlementArea })
    }

    @Test
    fun keepsNearbyMappedPoiInsteadOfFallingBack() = runBlocking {
        val service = PhotonPlacesService { _, _ ->
            collection(
                """{
                    "properties":{"osm_type":"N","osm_id":99,"name":"Village Square",
                        "osm_key":"place","osm_value":"square","country":"Poland"},
                    "geometry":{"type":"Point","coordinates":[18.082,54.789]}
                }""",
            )
        }

        val places = service.chapterPlaces(village, 60)

        assertEquals(3, places.size)
        assertTrue(places.all { it.id == "node:99" && it.category == "Square" })
    }

    @Test
    fun preservesLookupFailuresInsteadOfInventingAnEmptyPoiResponse() {
        for (reason in listOf(PlaceLookupError.NETWORK, PlaceLookupError.UNAVAILABLE)) {
            val failure = PlaceLookupException(reason)
            val service = PhotonPlacesService { _, _ -> throw failure }

            val caught = assertThrows(PlaceLookupException::class.java) {
                runBlocking { service.chapterPlaces(village, 60) }
            }

            assertSame(failure, caught)
        }
    }

    @Test
    fun malformedPoiResponseStillFailsInsteadOfBecomingSettlementFallback() {
        val service = PhotonPlacesService { _, _ -> "{}" }

        val caught = assertThrows(PlaceLookupException::class.java) {
            runBlocking { service.chapterPlaces(village, 60) }
        }

        assertEquals(PlaceLookupError.UNAVAILABLE, caught.reason)
    }

    private fun collection(vararg features: String) = """{"features":[${features.joinToString()}]}"""

    private fun settlement(id: Long, name: String, type: String, country: String) = """
        {
            "properties":{"osm_type":"N","osm_id":$id,"name":"$name",
                "osm_key":"place","osm_value":"$type","country":"$country"},
            "geometry":{"type":"Point","coordinates":[18.0812105,54.7887218]}
        }
    """.trimIndent()
}
