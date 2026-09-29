package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class UserLocationData(
    val latitude: Double, val longitude: Double, val addressName: String,
    val shortName: String, val cityName: String = ""
)

object LocationHelper {
    fun hasLocationPermission(context: Context): Boolean = listOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
    }

    @android.annotation.SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): UserLocationData {
        check(hasLocationPermission(context)) { "Allow location access or enter a pickup address." }
        val location = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val token = CancellationTokenSource()
                continuation.invokeOnCancellation { token.cancel() }
                try {
                    LocationServices.getFusedLocationProviderClient(context)
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                        .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                        .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                } catch (_: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }?.takeIf(::isRecent) ?: withContext(Dispatchers.IO) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            manager.getProviders(true).mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .filter(::isRecent).minByOrNull { it.accuracy }
        } ?: error("Could not obtain a recent location. Enter a pickup address or enable GPS.")
        // Blocking geocoding always runs on IO; callbacks cannot block Compose's main thread.
        return withContext(Dispatchers.IO) {
            val address = reverseGeocode(context, location.latitude, location.longitude)
            ensureActive()
            UserLocationData(location.latitude, location.longitude, address.first, address.second, address.third)
        }
    }

    private fun isRecent(location: Location): Boolean {
        val ageNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return ageNanos in 0..120_000_000_000L && location.accuracy <= 1000f
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(context: Context, lat: Double, lng: Double): Triple<String, String, String> {
        try {
            val address = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)?.firstOrNull()
            if (address != null) {
                val short = address.subLocality ?: address.locality ?: address.featureName ?: "Current Location"
                val city = address.locality ?: address.subAdminArea ?: ""
                val full = listOfNotNull(address.thoroughfare, address.subLocality, address.locality, address.postalCode)
                    .filter { it.isNotBlank() }.joinToString(", ")
                return Triple(full.ifBlank { short }, short, city)
            }
        } catch (_: Exception) { }
        return Triple(String.format(Locale.US, "GPS %.5f, %.5f", lat, lng), "Current Location", "")
    }

    @Suppress("DEPRECATION")
    suspend fun forwardGeocode(context: Context, locationName: String, cityHint: String? = null): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            if (locationName.isBlank()) return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())
            val query = if (!cityHint.isNullOrBlank() && !locationName.contains(cityHint, true)) "$locationName, $cityHint" else locationName
            val result = runCatching { geocoder.getFromLocationName(query, 1)?.firstOrNull() }.getOrNull()
            ensureActive()
            result?.let { Pair(it.latitude, it.longitude) }
        }
}
