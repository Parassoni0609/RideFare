package com.example.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import com.example.data.model.CityData
import com.example.data.model.validCoordinates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.round

data class PlaceSearchResult(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val category: String = "general",
    val distanceKm: Float? = null
)

object PlaceSearchService {
    // Search focus only: never used as a selected pickup/drop coordinate.
    internal fun searchFocus(lat: Double?, lng: Double?, city: String?): Pair<Double, Double>? {
        if (validCoordinates(lat, lng)) return lat!! to lng!!
        val places = CityData.supportedCities.firstOrNull { it.name == city }?.popularLocations
        return places?.takeIf { it.isNotEmpty() }?.let {
            it.map { p -> p.lat }.average() to it.map { p -> p.lng }.average()
        }
    }

    internal fun photonUrl(query: String, lat: Double?, lng: Double?, city: String?): String {
        val focus = searchFocus(lat, lng, city)
        return Uri.parse("https://photon.komoot.io/api/").buildUpon()
            .appendQueryParameter("q", query.trim())
            .appendQueryParameter("countrycode", "IN")
            .appendQueryParameter("limit", "10")
            .appendQueryParameter("lang", "en")
            .apply {
                focus?.let {
                    appendQueryParameter("lat", it.first.toString())
                    appendQueryParameter("lon", it.second.toString())
                    appendQueryParameter("zoom", "11")
                    appendQueryParameter("location_bias_scale", "0.2")
                }
            }.build().toString()
    }

    /** India-focused autocomplete. Empty matches and unavailable services are distinct. */
    suspend fun searchPlaces(
        context: Context,
        query: String,
        userLat: Double? = null,
        userLng: Double? = null,
        cityNameHint: String? = null
    ): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        var photonSucceeded = false
        val connection = URL(photonUrl(trimmed, userLat, userLng, cityNameHint))
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "RideFare/1.2 (Android)")
            if (connection.responseCode != 200) throw IOException("Place search unavailable")
            val results = parsePhoton(connection.inputStream.bufferedReader().use { it.readText() }, userLat, userLng)
            ensureActive()
            photonSucceeded = true
            // Do not hold valid suggestions behind a slow or unavailable device geocoder.
            if (results.isNotEmpty()) return@withContext results
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Try the device service before reporting a network failure.
        } finally {
            connection.disconnect()
        }
        ensureActive()
        try {
            if (!Geocoder.isPresent()) throw IOException("No device geocoder")
            val geocoder = Geocoder(context, Locale.getDefault())
            val hint = cityNameHint?.takeIf { it.isNotBlank() && !trimmed.contains(it, true) }
            val qualified = listOfNotNull(trimmed, hint, "India").joinToString(", ")
            var addresses = geocode(geocoder, qualified)
            if (addresses.isEmpty()) addresses = geocode(geocoder, "$trimmed, India")
            ensureActive()
            addresses.filter { it.hasLatitude() && it.hasLongitude() &&
                validCoordinates(it.latitude, it.longitude) && it.countryCode.equals("IN", true)
            }.map { address ->
                val title = address.featureName ?: address.thoroughfare ?: trimmed
                PlaceSearchResult(title, listOfNotNull(address.thoroughfare, address.subLocality,
                    address.locality, address.adminArea, address.postalCode)
                    .filter { it.isNotBlank() && !it.equals(title, true) }.distinct().joinToString(", "),
                    address.latitude, address.longitude, category(title, ""),
                    distance(userLat, userLng, address.latitude, address.longitude))
            }.distinctBy { Triple(it.title, it.latitude, it.longitude) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!photonSucceeded) throw IOException("Place search unavailable. Check your connection and retry.", e)
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocode(geocoder: Geocoder, query: String): List<Address> {
        if (Build.VERSION.SDK_INT < 33) return geocoder.getFromLocationName(query, 8).orEmpty()
        return withTimeoutOrNull(4000) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(query, 8, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resumeWithException(IOException("Geocoder unavailable"))
                    }
                })
            }
        } ?: throw IOException("Geocoder timed out")
    }

    internal fun parsePhoton(json: String, lat: Double?, lng: Double?): List<PlaceSearchResult> {
        val features = JSONObject(json).getJSONArray("features")
        return (0 until features.length()).mapNotNull { i ->
            val feature = features.optJSONObject(i) ?: return@mapNotNull null
            val p = feature.optJSONObject("properties") ?: return@mapNotNull null
            // Also validate responses: protects against servers ignoring the country filter.
            if (!p.optString("countrycode").equals("IN", true)) return@mapNotNull null
            val coordinates = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return@mapNotNull null
            val x = coordinates.optDouble(0, Double.NaN)
            val y = coordinates.optDouble(1, Double.NaN)
            if (!validCoordinates(y, x)) return@mapNotNull null
            val title = p.optString("name").ifBlank { p.optString("street") }
            if (title.isBlank()) return@mapNotNull null
            val subtitle = listOf("housenumber", "street", "district", "city", "county", "state", "postcode")
                .map { p.optString(it) }.filter { it.isNotBlank() && !it.equals(title, true) }
                .distinct().joinToString(", ")
            PlaceSearchResult(title, subtitle, y, x,
                category(title, p.optString("osm_value")), distance(lat, lng, y, x))
        }.distinctBy { Triple(it.title, it.latitude, it.longitude) }
    }

    private fun category(title: String, type: String): String {
        val value = "$title $type".lowercase(Locale.ROOT)
        return when {
            "metro" in value || "subway" in value -> "metro"
            "airport" in value || "aerodrome" in value -> "airport"
            "station" in value || "railway" in value -> "transit"
            "mall" in value || "shop" in value || "supermarket" in value -> "shopping"
            "hospital" in value || "clinic" in value -> "hospital"
            "office" in value || "commercial" in value || "industrial" in value -> "work"
            else -> "general"
        }
    }

    private fun distance(lat: Double?, lng: Double?, toLat: Double, toLng: Double): Float? {
        if (!validCoordinates(lat, lng)) return null
        val result = FloatArray(1)
        Location.distanceBetween(lat!!, lng!!, toLat, toLng, result)
        return round(result[0] / 100f) / 10f
    }
}
