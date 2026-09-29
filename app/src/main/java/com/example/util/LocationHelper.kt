package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class UserLocationData(
    val latitude: Double,
    val longitude: Double,
    val addressName: String,
    val shortName: String,
    val cityName: String = ""
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        onSuccess: (UserLocationData) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Location permission not granted. Please allow location access to use your current location.")
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val geocoded = reverseGeocode(context, location.latitude, location.longitude)
                        onSuccess(
                            UserLocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                addressName = geocoded.first,
                                shortName = geocoded.second,
                                cityName = geocoded.third
                            )
                        )
                    } else {
                        // Fallback to last known location or system LocationManager
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                val geocoded = reverseGeocode(context, lastLoc.latitude, lastLoc.longitude)
                                onSuccess(
                                    UserLocationData(
                                        latitude = lastLoc.latitude,
                                        longitude = lastLoc.longitude,
                                        addressName = geocoded.first,
                                        shortName = geocoded.second,
                                        cityName = geocoded.third
                                    )
                                )
                            } else {
                                fallbackToLocationManager(context, onSuccess, onError)
                            }
                        }.addOnFailureListener {
                            fallbackToLocationManager(context, onSuccess, onError)
                        }
                    }
                }
                .addOnFailureListener {
                    fallbackToLocationManager(context, onSuccess, onError)
                }
        } catch (e: Exception) {
            fallbackToLocationManager(context, onSuccess, onError)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLocationManager(
        context: Context,
        onSuccess: (UserLocationData) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var bestLoc: Location? = null

            for (provider in providers) {
                val l = lm.getLastKnownLocation(provider) ?: continue
                if (bestLoc == null || l.accuracy < bestLoc.accuracy) {
                    bestLoc = l
                }
            }

            if (bestLoc != null) {
                val geocoded = reverseGeocode(context, bestLoc.latitude, bestLoc.longitude)
                onSuccess(
                    UserLocationData(
                        latitude = bestLoc.latitude,
                        longitude = bestLoc.longitude,
                        addressName = geocoded.first,
                        shortName = geocoded.second,
                        cityName = geocoded.third
                    )
                )
            } else {
                onError("Could not detect GPS location. Please ensure location is enabled.")
            }
        } catch (e: Exception) {
            onError("Failed to obtain location: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    fun reverseGeocode(context: Context, lat: Double, lng: Double): Triple<String, String, String> {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val subLocality = addr.subLocality ?: addr.locality ?: addr.featureName ?: "Current Location"
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                val full = listOfNotNull(
                    addr.thoroughfare,
                    addr.subLocality,
                    addr.locality,
                    addr.postalCode
                ).filter { it.isNotBlank() }.joinToString(", ")

                val readable = if (full.isNotBlank()) full else "$subLocality, $city"
                return Triple(readable, subLocality, city)
            }
        } catch (_: Exception) {
            // Geocoder fallback
        }
        val formattedCoords = String.format(Locale.US, "%.4f, %.4f", lat, lng)
        return Triple("Current GPS ($formattedCoords)", "Current Location", "")
    }

    suspend fun forwardGeocode(
        context: Context,
        locationName: String,
        cityHint: String? = null
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (locationName.isBlank()) return@withContext null
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val query = if (!cityHint.isNullOrBlank() && !locationName.contains(cityHint, ignoreCase = true)) {
                "$locationName, $cityHint"
            } else {
                locationName
            }
            var addresses = geocoder.getFromLocationName(query, 1)
            if (addresses.isNullOrEmpty() && query != locationName) {
                addresses = geocoder.getFromLocationName(locationName, 1)
            }
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                return@withContext Pair(addr.latitude, addr.longitude)
            }
        } catch (_: Exception) {
            // Geocoder network failure
        }
        null
    }
}
