package com.example.sidequest.data.places

import com.example.sidequest.domain.adventure.QuestCity
import com.example.sidequest.domain.adventure.QuestPlace
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

enum class PlaceLookupError { NOT_FOUND, NETWORK, UNAVAILABLE }

class PlaceLookupException(val reason: PlaceLookupError, cause: Throwable? = null) :
    Exception("Place lookup failed: ${reason.name}", cause)

class PhotonPlacesService internal constructor(
    private val responseLoader: (suspend (String, List<Pair<String, String>>) -> String)?,
) {
    constructor() : this(null)

    private val cityCache = LinkedHashMap<String, List<QuestCity>>()
    private val placeCache = LinkedHashMap<String, List<QuestPlace>>()

    suspend fun searchCities(query: String): List<QuestCity> {
        val trimmed = query.trim()
        if (trimmed.length !in 1..100) throw PlaceLookupException(PlaceLookupError.NOT_FOUND)
        val key = trimmed.lowercase(Locale.ROOT)
        return requestMutex.withLock {
            cityCache[key]?.let { return@withLock it }
            val response = load(
                "api/",
                listOf(
                    "q" to trimmed,
                    "limit" to "5",
                    "lang" to "en",
                    "osm_tag" to "place:city",
                ),
            )
            val cityMatches = PhotonResponseParser.cities(response)
            val settlementMatches = PhotonResponseParser.cities(
                load(
                    "api/",
                    listOf(
                        "q" to trimmed,
                        "limit" to "5",
                        "lang" to "en",
                        "osm_tag" to "place:town",
                        "osm_tag" to "place:village",
                        "osm_tag" to "place:hamlet",
                        "osm_tag" to "place:municipality",
                    ),
                ),
            )
            // Both groups stay visible: fuzzy cities must not hide a matching village,
            // and numerous namesakes must not hide a major city with an alternate spelling.
            val cities = buildList {
                repeat(5) { index ->
                    cityMatches.getOrNull(index)?.let(::add)
                    settlementMatches.getOrNull(index)?.let(::add)
                }
            }.distinctBy(QuestCity::id)
            if (cities.isEmpty()) throw PlaceLookupException(PlaceLookupError.NOT_FOUND)
            if (cityCache.size >= CACHE_SIZE) cityCache.remove(cityCache.keys.first())
            cityCache[key] = cities
            cities
        }
    }

    suspend fun chapterPlaces(city: QuestCity, durationMinutes: Int): List<QuestPlace> {
        val radiusKm = when {
            durationMinutes <= 30 -> 0.5
            durationMinutes <= 60 -> 1.0
            durationMinutes <= 90 -> 1.5
            else -> 2.0
        }
        val key = "${city.id}:${city.latitude}:${city.longitude}:$radiusKm"
        return requestMutex.withLock {
            placeCache[key]?.let { return@withLock it }
            val response = load(
                "reverse/",
                listOf(
                    "lat" to city.latitude.toString(),
                    "lon" to city.longitude.toString(),
                    "radius" to radiusKm.toString(),
                    "limit" to "12",
                    "lang" to "en",
                    "osm_tag" to "leisure:park",
                    "osm_tag" to "place:square",
                ),
            )
            val places = PhotonResponseParser.places(response, city, radiusKm).ifEmpty {
                listOf(
                    QuestPlace(
                        id = city.id,
                        name = city.name,
                        category = QuestPlace.SETTLEMENT_AREA_CATEGORY,
                        latitude = city.latitude,
                        longitude = city.longitude,
                        sourceUrl = city.sourceUrl,
                    ),
                )
            }
            val chapterPlaces = List(3) { places[it % places.size] }
            if (placeCache.size >= CACHE_SIZE) placeCache.remove(placeCache.keys.first())
            placeCache[key] = chapterPlaces
            chapterPlaces
        }
    }

    private suspend fun load(path: String, parameters: List<Pair<String, String>>): String {
        responseLoader?.let { return it(path, parameters) }
        val waitMillis = 1_000L - (System.nanoTime() - lastRequestNanos) / 1_000_000L
        if (waitMillis > 0) delay(waitMillis)
        lastRequestNanos = System.nanoTime()
        return try {
            withContext(Dispatchers.IO) {
                val query = parameters.joinToString("&") { (key, value) ->
                    "$key=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
                }
                val connection = URL("$ENDPOINT/$path?$query").openConnection() as HttpsURLConnection
                try {
                    connection.connectTimeout = 8_000
                    connection.readTimeout = 8_000
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", "SideQuest/0.1 (https://github.com/TodosievNikolay/SideQuest)")
                    if (connection.responseCode != HttpsURLConnection.HTTP_OK ||
                        connection.contentLengthLong > MAX_RESPONSE_BYTES
                    ) {
                        throw PlaceLookupException(PlaceLookupError.UNAVAILABLE)
                    }
                    val coroutineContext = currentCoroutineContext()
                    connection.inputStream.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8_192)
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count == -1) break
                            if (output.size() + count > MAX_RESPONSE_BYTES) {
                                throw PlaceLookupException(PlaceLookupError.UNAVAILABLE)
                            }
                            output.write(buffer, 0, count)
                        }
                        coroutineContext.ensureActive()
                        output.toString(Charsets.UTF_8.name())
                    }
                } finally {
                    connection.disconnect()
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw PlaceLookupException(PlaceLookupError.NETWORK, error)
        }
    }

    companion object {
        private const val ENDPOINT = "https://photon.komoot.io"
        private const val MAX_RESPONSE_BYTES = 1_048_576
        private const val CACHE_SIZE = 24
        private val requestMutex = Mutex()
        private var lastRequestNanos = 0L
    }
}

internal object PhotonResponseParser {
    fun cities(json: String): List<QuestCity> = features(json).mapNotNull { feature ->
        val properties = feature["properties"] as? JsonObject ?: return@mapNotNull null
        if (properties.text("osm_key") != "place" ||
            properties.text("osm_value") !in setOf("city", "town", "village", "hamlet", "municipality")
        ) {
            return@mapNotNull null
        }
        val coordinates = feature.coordinates() ?: return@mapNotNull null
        val source = properties.source() ?: return@mapNotNull null
        val name = properties.text("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
        val country = properties.text("country")
        QuestCity(
            id = source.first,
            name = name,
            region = properties.text("state").ifBlank { properties.text("county") },
            country = country,
            latitude = coordinates.first,
            longitude = coordinates.second,
            sourceUrl = source.second,
        )
    }.distinctBy(QuestCity::id)

    fun places(json: String, city: QuestCity, radiusKm: Double): List<QuestPlace> =
        features(json).mapNotNull { feature ->
            val properties = feature["properties"] as? JsonObject ?: return@mapNotNull null
            val category = when (properties.text("osm_key") to properties.text("osm_value")) {
                "leisure" to "park" -> "Park"
                "place" to "square" -> "Square"
                else -> return@mapNotNull null
            }
            val country = properties.text("country")
            if (country.isNotBlank() && city.country.isNotBlank() &&
                !country.equals(city.country, ignoreCase = true)
            ) {
                return@mapNotNull null
            }
            val coordinates = feature.coordinates() ?: return@mapNotNull null
            if (distanceKm(city.latitude, city.longitude, coordinates.first, coordinates.second) > radiusKm) {
                return@mapNotNull null
            }
            val source = properties.source() ?: return@mapNotNull null
            val name = properties.text("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
            QuestPlace(
                id = source.first,
                name = name,
                category = category,
                latitude = coordinates.first,
                longitude = coordinates.second,
                sourceUrl = source.second,
            )
        }.distinctBy(QuestPlace::id)
            .sortedBy { distanceKm(city.latitude, city.longitude, it.latitude, it.longitude) }
            .take(3)

    private fun features(json: String): List<JsonObject> {
        val root = try {
            Json.parseToJsonElement(json) as? JsonObject
        } catch (error: SerializationException) {
            throw PlaceLookupException(PlaceLookupError.UNAVAILABLE, error)
        }
        val features = root?.get("features") as? JsonArray
            ?: throw PlaceLookupException(PlaceLookupError.UNAVAILABLE)
        return features.mapNotNull { it as? JsonObject }
    }

    private fun JsonObject.coordinates(): Pair<Double, Double>? {
        val geometry = this["geometry"] as? JsonObject ?: return null
        if (geometry.text("type") != "Point") return null
        val values = geometry["coordinates"] as? JsonArray ?: return null
        if (values.size != 2) return null
        val longitude = (values[0] as? JsonPrimitive)?.doubleOrNull ?: return null
        val latitude = (values[1] as? JsonPrimitive)?.doubleOrNull ?: return null
        if (!latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0
        ) {
            return null
        }
        return latitude to longitude
    }

    private fun JsonObject.source(): Pair<String, String>? {
        val type = when (text("osm_type")) {
            "N" -> "node"
            "W" -> "way"
            "R" -> "relation"
            else -> return null
        }
        val id = (this["osm_id"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 } ?: return null
        return "$type:$id" to "https://www.openstreetmap.org/$type/$id"
    }

    private fun JsonObject.text(key: String): String =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim().orEmpty()
}

private fun distanceKm(latitude: Double, longitude: Double, otherLatitude: Double, otherLongitude: Double): Double {
    val latitudeDelta = Math.toRadians(otherLatitude - latitude)
    val longitudeDelta = Math.toRadians(otherLongitude - longitude)
    val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(Math.toRadians(latitude)) * cos(Math.toRadians(otherLatitude)) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return 6_371.0 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
}
