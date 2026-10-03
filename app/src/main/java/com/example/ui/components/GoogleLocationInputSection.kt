package com.example.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.CityInfo
import com.example.data.model.validCoordinates
import com.example.data.repository.PlaceSearchResult
import com.example.data.repository.PlaceSearchService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.widget.PlaceAutocomplete
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Uses Google's own prediction UI, including attribution and session management. */
@Composable
fun GoogleLocationInputSection(
    pickup: String, drop: String, city: CityInfo, isLocating: Boolean,
    pickupLat: Double?, pickupLng: Double?,
    onPickupChange: (String) -> Unit, onDropChange: (String) -> Unit,
    onSwap: () -> Unit, onGps: () -> Unit,
    onSelect: (PlaceSearchResult, Boolean) -> Unit, onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingField by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingCity by rememberSaveable { mutableStateOf("") }
    var pendingText by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val latestSelection by rememberUpdatedState(onSelect)
    val latestRoute by rememberUpdatedState(Triple(city.id, pickup, drop))
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val field = pendingField
        pendingField = null
        val data = result.data
        if (field != null && data != null) {
            val prediction = PlaceAutocomplete.getPredictionFromIntent(data)
            if (result.resultCode == Activity.RESULT_OK && prediction != null) {
                val requestCity = pendingCity
                val requestText = pendingText
                scope.launch {
                    loading = true
                    error = null
                    try {
                        // Reinitialize after activity/process recreation if necessary.
                        if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(context.applicationContext, BuildConfig.GOOGLE_PLACES_API_KEY)
                        val request = FetchPlaceRequest.builder(prediction.placeId,
                            listOf(Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS))
                            .setSessionToken(PlaceAutocomplete.getSessionTokenFromIntent(data)).build()
                        val place = withTimeout(15000) {
                            suspendCancellableCoroutine<Place> { continuation ->
                                Places.createClient(context).fetchPlace(request)
                                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(it.place) }
                                    .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                            }
                        }
                        val location = place.location
                        check(location != null && validCoordinates(location.latitude, location.longitude))
                        val current = latestRoute
                        if (current.first == requestCity && (if (field == 1) current.third else current.second) == requestText) {
                            latestSelection(PlaceSearchResult(prediction.getPrimaryText(null).toString(),
                                place.formattedAddress.orEmpty(), location.latitude, location.longitude), field == 1)
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        error = "Google place details timed out. Tap the address to retry."
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = "Could not load this place. Tap the address to retry." +
                            ((e as? ApiException)?.let { " (Google error ${it.statusCode})" } ?: "")
                    } finally { loading = false }
                }
            } else {
                val status = PlaceAutocomplete.getResultStatusFromIntent(data)
                if (status != null && !status.isSuccess && !status.isCanceled) {
                    error = "Google search is unavailable (error ${status.statusCode}). Check network or contact the app owner."
                }
            }
        }
    }
    fun search(isDrop: Boolean) {
        error = null
        try {
            if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(context.applicationContext, BuildConfig.GOOGLE_PLACES_API_KEY)
            pendingField = if (isDrop) 1 else 0
            pendingCity = city.id
            pendingText = if (isDrop) drop else pickup
            val intent = PlaceAutocomplete.IntentBuilder()
                .setCountries(listOf("IN"))
                .setInitialQuery(pendingText)
                .apply {
                    PlaceSearchService.searchFocus(pickupLat, pickupLng, city.name)?.let { (lat, lng) ->
                        setLocationBias(CircularBounds.newInstance(LatLng(lat, lng), 50000.0))
                    }
                }.build(context)
            launcher.launch(intent)
        } catch (_: Exception) {
            pendingField = null
            error = "Google search could not start. Check this build's search configuration."
        }
    }
    Card(modifier.fillMaxWidth().padding(16.dp).testTag("google_location_input_section")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Search places with Google", style = MaterialTheme.typography.titleMedium)
            Text("Search businesses, landmarks or full addresses. Choose a result to confirm its location.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { search(false) }, enabled = !loading && !isLocating,
                modifier = Modifier.fillMaxWidth().testTag("google_pickup_search")) {
                Text("Pickup: ${pickup.ifBlank { "Search a place" }}")
            }
            OutlinedButton(onClick = { search(true) }, enabled = !loading,
                modifier = Modifier.fillMaxWidth().testTag("google_drop_search")) {
                Text("Drop: ${drop.ifBlank { "Search a place" }}")
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onGps, enabled = !loading && !isLocating) { Text(if (isLocating) "Locating…" else "Use GPS") }
                TextButton(onClick = onSwap, enabled = !loading) { Text("Swap") }
                TextButton(onClick = onSave, enabled = !loading) { Text("Save") }
            }
            Row {
                if (pickup.isNotBlank()) TextButton(onClick = { onPickupChange("") }, enabled = !loading) { Text("Clear pickup") }
                if (drop.isNotBlank()) TextButton(onClick = { onDropChange("") }, enabled = !loading) { Text("Clear drop") }
            }
        }
    }
}
