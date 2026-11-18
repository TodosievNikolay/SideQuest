package com.example.sidequest

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sidequest.data.ai.FirebaseAdventureGenerator
import com.example.sidequest.data.places.PhotonPlacesService
import com.example.sidequest.domain.adventure.AdventureMood
import com.example.sidequest.domain.adventure.AdventureParty
import com.example.sidequest.domain.adventure.AdventureRequest
import com.example.sidequest.domain.adventure.QuestPlace
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseCityGenerationTest {
    @Test
    fun warsawQuestUsesMappedParksAndSquares(): Unit = runBlocking {
        assumeTrue(
            "Enable explicitly to send requests to Photon and Firebase.",
            InstrumentationRegistry.getArguments().getString("citySmokeTest") == "true",
        )
        val places = PhotonPlacesService()
        val matches = places.searchCities("Warsaw, Poland")
        val city = matches.first { it.country == "Poland" }
        val chapterPlaces = places.chapterPlaces(city, 60)
        val request = AdventureRequest(
            durationMinutes = 60,
            mood = AdventureMood.DISCOVERY,
            party = AdventureParty.SOLO,
            maxBudgetPln = 0,
            interests = "architecture",
            city = city,
        )
        val adventure = FirebaseAdventureGenerator(places).generate(request)

        assertEquals(city.displayName, adventure.cityName)
        assertEquals(chapterPlaces, adventure.chapters.map { it.place })
        assertEquals(3, adventure.chapters.size)
        assertTrue(adventure.estimatedDurationMinutes in 1..60)
        assertEquals(0, adventure.estimatedCostPln)
        assertTrue(chapterPlaces.all { it.sourceUrl.startsWith("https://www.openstreetmap.org/") })
        Log.i("CityGenerationCheck", "Generated in ${adventure.cityName}: ${chapterPlaces.joinToString { it.name }}")
    }

    @Test
    fun smallSettlementQuestUsesMappedSettlementArea(): Unit = runBlocking {
        assumeTrue(
            "Enable explicitly to send requests to Photon and Firebase.",
            InstrumentationRegistry.getArguments().getString("settlementSmokeTest") == "true",
        )
        val places = PhotonPlacesService()
        val city = places.searchCities("Żarnowiec, Poland").firstOrNull {
            it.name == "Żarnowiec" && it.country == "Poland" &&
                it.region.contains("Pomeranian", ignoreCase = true)
        }
        checkNotNull(city) { "Photon did not return Żarnowiec in Pomeranian Voivodeship, Poland." }
        val chapterPlaces = places.chapterPlaces(city, 30)

        assertEquals(3, chapterPlaces.size)
        assertTrue(
            "This smoke test requires a settlement with no nearby mapped POIs.",
            chapterPlaces.all { it.category == QuestPlace.SETTLEMENT_AREA_CATEGORY },
        )
        chapterPlaces.forEach { place ->
            assertEquals(city.id, place.id)
            assertEquals(city.name, place.name)
            assertEquals(city.latitude, place.latitude, 0.0)
            assertEquals(city.longitude, place.longitude, 0.0)
            assertEquals(city.sourceUrl, place.sourceUrl)
        }

        val request = AdventureRequest(
            durationMinutes = 30,
            mood = AdventureMood.DISCOVERY,
            party = AdventureParty.SOLO,
            maxBudgetPln = 0,
            interests = "everyday patterns",
            city = city,
        )
        val adventure = FirebaseAdventureGenerator(places).generate(request)

        assertEquals(city.displayName, adventure.cityName)
        assertEquals(3, adventure.chapters.size)
        assertEquals(chapterPlaces, adventure.chapters.map { it.place })
        assertTrue(adventure.estimatedDurationMinutes in 1..30)
        assertEquals(0, adventure.estimatedCostPln)
        assertTrue(adventure.introduction.isNotBlank())
        assertTrue(adventure.chapters.all {
            !it.storyContext.isNullOrBlank() && it.mission.isNotBlank() && it.completionCriteria.isNotBlank()
        })
        Log.i("CityGenerationCheck", "Generated settlement-area quest in ${city.displayName}: ${city.sourceUrl}")
    }

    @Test
    fun worldwideSettlementLookupRetainsRequestedCountry(): Unit = runBlocking {
        assumeTrue(
            "Enable explicitly to run four settlement searches with Photon.",
            InstrumentationRegistry.getArguments().getString("cityLookupSmokeTest") == "true",
        )
        val places = PhotonPlacesService()
        val examples = listOf(
            LookupExample("Odesa, Ukraine", "Odesa", "Ukraine"),
            LookupExample("Nowy Tomysl", "Nowy Tomyśl", "Poland"),
            LookupExample("Żarnowiec, Poland", "Żarnowiec", "Poland", "Pomeranian"),
            LookupExample("Y", "Y", "France"),
        )

        examples.forEach { example ->
            val city = places.searchCities(example.query).firstOrNull {
                it.name == example.name && it.country == example.country &&
                    (example.regionPart == null || it.region.contains(example.regionPart, ignoreCase = true))
            }
            checkNotNull(city) {
                "Photon did not return ${example.name}, ${example.country} for '${example.query}'."
            }
            assertTrue(city.sourceUrl.startsWith("https://www.openstreetmap.org/"))
            assertTrue(city.latitude.isFinite() && city.latitude in -90.0..90.0)
            assertTrue(city.longitude.isFinite() && city.longitude in -180.0..180.0)
            assertTrue(city.displayName.contains(example.name))
            assertTrue(city.displayName.contains(example.country))
            Log.i("CityLookupCheck", "${example.query}: ${city.displayName} (${city.sourceUrl})")
        }
    }

    private data class LookupExample(
        val query: String,
        val name: String,
        val country: String,
        val regionPart: String? = null,
    )
}
