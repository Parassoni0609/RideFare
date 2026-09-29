package com.example.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
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

    /**
     * Live Google Maps-like keyword place autocomplete search.
     * Supports places, shops, societies, hospitals, tech parks, airports, metro stations, roads across India.
     */
    suspend fun searchPlaces(
        context: Context,
        query: String,
        userLat: Double? = null,
        userLng: Double? = null,
        cityNameHint: String? = null
    ): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val results = mutableListOf<PlaceSearchResult>()

        var conn: HttpURLConnection? = null
        // 1. Live Photon OpenStreetMap Places Search (Autocomplete search engine)
        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val urlString = if (userLat != null && userLng != null && userLat != 0.0) {
                "https://photon.komoot.io/api/?q=$encodedQuery&lat=$userLat&lon=$userLng&limit=10"
            } else {
                "https://photon.komoot.io/api/?q=$encodedQuery&limit=10"
            }

            conn = URL(urlString).openConnection() as HttpURLConnection
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "RideFareApp/1.0")

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val features = json.optJSONArray("features")
                if (features != null) {
                    for (i in 0 until features.length()) {
                        val feature = features.getJSONObject(i)
                        val geometry = feature.optJSONObject("geometry")
                        val coordinates = geometry?.optJSONArray("coordinates")
                        val properties = feature.optJSONObject("properties")

                        if (coordinates != null && coordinates.length() >= 2 && properties != null) {
                            val lng = coordinates.getDouble(0)
                            val lat = coordinates.getDouble(1)
                            val name = properties.optString("name", "")
                            val street = properties.optString("street", "")
                            val city = properties.optString("city", properties.optString("county", properties.optString("state", "")))
                            val country = properties.optString("country", "")
                            val type = properties.optString("type", properties.optString("osm_value", "general"))

                            if (name.isNotBlank() || street.isNotBlank()) {
                                val title = if (name.isNotBlank()) name else street
                                val subParts = listOfNotNull(
                                    if (name.isNotBlank() && street.isNotBlank()) street else null,
                                    properties.optString("district", ""),
                                    city,
                                    properties.optString("postcode", "")
                                ).filter { it.isNotBlank() && !it.equals(title, ignoreCase = true) }

                                val subtitle = if (subParts.isNotEmpty()) subParts.joinToString(", ") else country

                                val category = when {
                                    type.contains("aerodrome") || type.contains("airport") || title.contains("airport", ignoreCase = true) -> "airport"
                                    type.contains("station") || type.contains("railway") || title.contains("railway", ignoreCase = true) || title.contains("station", ignoreCase = true) -> "transit"
                                    type.contains("subway") || type.contains("metro") || title.contains("metro", ignoreCase = true) -> "metro"
                                    type.contains("mall") || type.contains("shop") || type.contains("supermarket") || title.contains("mall", ignoreCase = true) -> "shopping"
                                    type.contains("hospital") || type.contains("clinic") || title.contains("hospital", ignoreCase = true) -> "hospital"
                                    type.contains("office") || type.contains("commercial") || type.contains("industrial") -> "work"
                                    else -> "general"
                                }

                                val dist = if (userLat != null && userLng != null && userLat != 0.0) {
                                    val r = FloatArray(1)
                                    android.location.Location.distanceBetween(userLat, userLng, lat, lng, r)
                                    (round((r[0] / 1000f) * 10f) / 10f)
                                } else null

                                results.add(
                                    PlaceSearchResult(
                                        title = title,
                                        subtitle = subtitle,
                                        latitude = lat,
                                        longitude = lng,
                                        category = category,
                                        distanceKm = dist
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        } finally {
            conn?.disconnect()
        }
        coroutineContext.ensureActive()

        // 2. Android Geocoder
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val queryWithHint = if (!cityNameHint.isNullOrBlank() && !trimmed.contains(cityNameHint, ignoreCase = true)) {
                "$trimmed, $cityNameHint, India"
            } else {
                "$trimmed, India"
            }

            @Suppress("DEPRECATION")
            var addresses: List<Address>? = geocoder.getFromLocationName(queryWithHint, 6)
            if (addresses.isNullOrEmpty() && queryWithHint != trimmed) {
                @Suppress("DEPRECATION")
                addresses = geocoder.getFromLocationName(trimmed, 6)
            }

            if (!addresses.isNullOrEmpty()) {
                for (addr in addresses) {
                    val lat = addr.latitude
                    val lng = addr.longitude
                    val feature = addr.featureName ?: addr.thoroughfare ?: trimmed
                    val subLocality = addr.subLocality ?: addr.locality ?: ""

                    val full = listOfNotNull(
                        addr.thoroughfare,
                        addr.subLocality,
                        addr.locality,
                        addr.adminArea,
                        addr.postalCode
                    ).filter { it.isNotBlank() && !it.equals(feature, ignoreCase = true) }.joinToString(", ")

                    val dist = if (userLat != null && userLng != null && userLat != 0.0) {
                        val r = FloatArray(1)
                        android.location.Location.distanceBetween(userLat, userLng, lat, lng, r)
                        (round((r[0] / 1000f) * 10f) / 10f)
                    } else null

                    val alreadyPresent = results.any {
                        Math.abs(it.latitude - lat) < 0.001 && Math.abs(it.longitude - lng) < 0.001
                    }

                    if (!alreadyPresent) {
                        results.add(
                            PlaceSearchResult(
                                title = feature,
                                subtitle = if (full.isNotBlank()) full else subLocality,
                                latitude = lat,
                                longitude = lng,
                                category = if (feature.contains("Airport", ignoreCase = true)) "airport" else if (feature.contains("Metro", ignoreCase = true)) "metro" else "general",
                                distanceKm = dist
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        results.distinctBy { "${it.title}_${it.latitude}_${it.longitude}" }
    }
}
