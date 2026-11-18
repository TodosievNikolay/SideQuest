package com.example.sidequest.data.places

import com.example.sidequest.domain.adventure.QuestCity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhotonResponseParserTest {
    private val city = QuestCity(
        id = "relation:336075",
        name = "Warsaw",
        region = "Masovian Voivodeship",
        country = "Poland",
        latitude = 52.23,
        longitude = 21.0,
        sourceUrl = "https://www.openstreetmap.org/relation/336075",
    )

    @Test
    fun retainsCitiesInDifferentCountriesAndRejectsInvalidCoordinates() {
        val response = collection(
            feature(1, "Warsaw", "place", "city", "Poland", 21.0, 52.23),
            feature(2, "Warsaw", "place", "town", "United States", -84.9, 38.78),
            feature(3, "Invalid City", "place", "city", "Poland", 21.0, 91.0),
            feature(4, "Not a City", "tourism", "museum", "Poland", 21.0, 52.23),
        )

        val cities = PhotonResponseParser.cities(response)

        assertEquals(2, cities.size)
        assertEquals(listOf("Poland", "United States"), cities.map(QuestCity::country))
        assertEquals(52.23, cities.first().latitude, 0.00001)
        assertEquals(21.0, cities.first().longitude, 0.00001)
        assertEquals("https://www.openstreetmap.org/node/1", cities.first().sourceUrl)
    }

    @Test
    fun acceptsSmallSettlementsAndPreservesMissingCountryWithoutInventingOne() {
        val response = collection(
            feature(1, "Nowy Tomyśl", "place", "town", "Poland", 16.129, 52.318),
            feature(2, "Żarnowiec", "place", "village", "Poland", 18.081, 54.789),
            feature(3, "Y", "place", "hamlet", null, 2.991, 49.803),
            feature(4, "Local Municipality", "place", "municipality", "France", 2.992, 49.804),
        )

        val cities = PhotonResponseParser.cities(response)

        assertEquals(listOf("Nowy Tomyśl", "Żarnowiec", "Y", "Local Municipality"), cities.map { it.name })
        assertEquals("", cities[2].country)
        assertEquals("Y", cities[2].displayName)
        assertEquals("https://www.openstreetmap.org/node/3", cities[2].sourceUrl)
    }

    @Test
    fun keepsOnlyNearbyParksAndSquaresInTheSelectedCountry() {
        val response = collection(
            feature(1, "Further Park", "leisure", "park", "Poland", 21.005, 52.23),
            feature(2, "Central Square", "place", "square", "Poland", 21.001, 52.23),
            feature(3, "Outside Radius", "leisure", "park", "Poland", 23.0, 52.23),
            feature(4, "Different Country", "leisure", "park", "Germany", 21.001, 52.23),
            feature(5, "Inside a Museum", "tourism", "artwork", "Poland", 21.001, 52.23),
            feature(0, "Invalid Source", "leisure", "park", "Poland", 21.001, 52.23),
            feature(2, "Duplicate Square", "place", "square", "Poland", 21.001, 52.23),
        )

        val places = PhotonResponseParser.places(response, city, 1.0)

        assertEquals(listOf("Central Square", "Further Park"), places.map { it.name })
        assertEquals(listOf("Square", "Park"), places.map { it.category })
    }

    @Test
    fun acceptsMissingPoiCountryOnlyWithinRadiusAndStillRejectsKnownMismatch() {
        val response = collection(
            feature(1, "Nearby Park", "leisure", "park", null, 21.001, 52.23),
            feature(2, "Distant Park", "leisure", "park", null, 23.0, 52.23),
            feature(3, "Known Mismatch", "leisure", "park", "Germany", 21.001, 52.23),
        )

        val places = PhotonResponseParser.places(response, city, 1.0)

        assertEquals(listOf("Nearby Park"), places.map { it.name })
    }

    @Test
    fun usesRadiusWhenSelectedSettlementCountryIsMissing() {
        val response = collection(
            feature(1, "Nearby Park", "leisure", "park", "Poland", 21.001, 52.23),
            feature(2, "Distant Park", "leisure", "park", "Poland", 23.0, 52.23),
        )

        val places = PhotonResponseParser.places(response, city.copy(country = ""), 1.0)

        assertEquals(listOf("Nearby Park"), places.map { it.name })
    }

    @Test
    fun failsOnMalformedSourceData() {
        assertThrows(PlaceLookupException::class.java) { PhotonResponseParser.cities("{") }
        assertThrows(PlaceLookupException::class.java) { PhotonResponseParser.cities("{}") }
    }

    @Test
    fun cityDisplayOmitsDuplicateRegionAndEmptyParts() {
        assertEquals("Warsaw, Poland", city.copy(region = "Warsaw").displayName)
        assertEquals("Warsaw, Poland", city.copy(region = "").displayName)
    }

    private fun collection(vararg features: String) = """{"features":[${features.joinToString()}]}"""

    private fun feature(
        id: Long,
        name: String,
        key: String,
        value: String,
        country: String?,
        longitude: Double,
        latitude: Double,
    ) = """
        {
            "properties": {
                "osm_type": "N",
                "osm_id": $id,
                "name": "$name",
                "osm_key": "$key",
                "osm_value": "$value"${country?.let { ",\"country\":\"$it\"" }.orEmpty()}
            },
            "geometry": {"type":"Point","coordinates":[$longitude,$latitude]}
        }
    """.trimIndent()
}
