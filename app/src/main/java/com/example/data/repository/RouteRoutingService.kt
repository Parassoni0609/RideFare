package com.example.data.repository

import com.example.data.model.GeoPoint
import com.example.data.model.validCoordinates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.round

data class RouteResult(
    val points: List<GeoPoint>,
    val distanceKm: Float?,
    val durationMinutes: Int?
)

object RouteRoutingService {

    suspend fun getDrivingRoutePoints(
        pLat: Double,
        pLng: Double,
        dLat: Double,
        dLng: Double
    ): List<GeoPoint> {
        return getDrivingRouteResult(pLat, pLng, dLat, dLng).points
    }

    suspend fun getDrivingRouteResult(
        pLat: Double,
        pLng: Double,
        dLat: Double,
        dLng: Double
    ): RouteResult = withContext(Dispatchers.IO) {
        if (!validCoordinates(pLat, pLng) || !validCoordinates(dLat, dLng)) return@withContext RouteResult(emptyList(), null, null)
        var conn: HttpURLConnection? = null
        try {
            val urlStr = "https://router.project-osrm.org/route/v1/driving/$pLng,$pLat;$dLng,$dLat?overview=full&geometries=geojson"
            conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "RideCompareApp/1.0")

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val routes = json.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val firstRoute = routes.getJSONObject(0)
                    val distanceMeters = firstRoute.optDouble("distance", -1.0)
                    val durationSeconds = firstRoute.optDouble("duration", -1.0)
                    val distanceKm = if (distanceMeters > 0) {
                        (round((distanceMeters / 1000.0) * 10.0) / 10.0).toFloat()
                    } else null
                    val durationMinutes = if (durationSeconds > 0) {
                        (durationSeconds / 60.0).toInt()
                    } else null

                    val geometry = firstRoute.optJSONObject("geometry")
                    val coordinates = geometry?.optJSONArray("coordinates")
                    if (coordinates != null && coordinates.length() > 1) {
                        val points = mutableListOf<GeoPoint>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lng = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            if (!validCoordinates(lat, lng)) return@withContext RouteResult(emptyList(), null, null)
                            points.add(GeoPoint(lat = lat, lng = lng))
                        }
                        if (points.isNotEmpty()) {
                            return@withContext RouteResult(
                                points = points,
                                distanceKm = distanceKm,
                                durationMinutes = durationMinutes
                            )
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Caller displays an unavailable state and an approximate distance.
        } finally {
            conn?.disconnect()
        }

        RouteResult(
            points = emptyList(),
            distanceKm = null,
            durationMinutes = null
        )
    }

}
