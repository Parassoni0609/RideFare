package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SavedRoute
import com.example.data.local.SearchHistoryItem
import com.example.data.model.*
import com.example.data.repository.PlaceSearchResult
import com.example.data.repository.RideRepository
import com.example.data.repository.RouteResult
import com.example.data.repository.RouteRoutingService
import com.example.util.LocationHelper
import com.example.util.PriceAlertNotificationHelper
import com.example.util.UserLocationData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SortOrder(val label: String) {
    CHEAPEST("Lowest Estimate"), FASTEST("Estimated Pickup"),
    SHORTEST("Estimated Trip"), SAVINGS("Estimate Difference")
}

data class RideFareUiState(
    val selectedCity: CityInfo = CityData.supportedCities.first(),
    val pickupText: String = "",
    val dropText: String = "",
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val dropLat: Double? = null,
    val dropLng: Double? = null,
    val isCurrentLocationActive: Boolean = false,
    val isLocatingUser: Boolean = false,
    val distanceKm: Float = 0f,
    val trafficCondition: TrafficCondition = TrafficCondition.NORMAL,
    val weatherCondition: WeatherOrTimeCondition = WeatherOrTimeCondition.REGULAR,
    val selectedCategory: VehicleCategory = VehicleCategory.ALL,
    val selectedProviderFilter: RideProvider? = null,
    val sortOrder: SortOrder = SortOrder.CHEAPEST,
    // These are comparison models, not verified provider availability.
    val availableProviders: List<RideProvider> = RideProvider.entries.toList(),
    val allRideOptions: List<RideOption> = emptyList(),
    val filteredRideOptions: List<RideOption> = emptyList(),
    val cheapestOverall: RideOption? = null,
    val fastestPickupOverall: RideOption? = null,
    val providerComparisons: List<ProviderComparison> = emptyList(),
    val cheapestInCategory: RideOption? = null,
    val selectedRideForBreakdown: RideOption? = null,
    val isComparing: Boolean = false,
    val isCityDialogVisible: Boolean = false,
    val isSavedRoutesDialogVisible: Boolean = false,
    val notificationMessage: String? = null,
    val routePoints: List<GeoPoint> = emptyList(),
    val isRouting: Boolean = false,
    val routeUnavailable: Boolean = false,
    val lastEtaRefreshTime: Long = System.currentTimeMillis()
) {
    val hasResolvedEndpoints: Boolean get() = pickupText.isNotBlank() && dropText.isNotBlank() &&
        validCoordinates(pickupLat, pickupLng) && validCoordinates(dropLat, dropLng)
}

class RideFareViewModel(
    private val repository: RideRepository,
    private val geocode: suspend (Context, String, String?) -> Pair<Double, Double>? = LocationHelper::forwardGeocode,
    private val route: suspend (Double, Double, Double, Double) -> RouteResult = RouteRoutingService::getDrivingRouteResult
) : ViewModel() {
    private val _uiState = MutableStateFlow(RideFareUiState())
    val uiState: StateFlow<RideFareUiState> = _uiState.asStateFlow()
    val savedRoutes: StateFlow<List<SavedRoute>> = repository.savedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val searchHistory: StateFlow<List<SearchHistoryItem>> = repository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private var pickupGeocodeJob: Job? = null
    private var dropGeocodeJob: Job? = null
    private var routingJob: Job? = null
    private var locationJob: Job? = null
    private var restoreJob: Job? = null
    private var routeRevision = 0L

    private fun cancelEndpointWork(isDrop: Boolean) {
        restoreJob?.cancel()
        if (isDrop) dropGeocodeJob?.cancel() else {
            pickupGeocodeJob?.cancel()
            locationJob?.cancel()
            _uiState.update { it.copy(isLocatingUser = false) }
        }
        invalidateRoute()
    }

    private fun invalidateRoute() {
        routeRevision++
        routingJob?.cancel()
        _uiState.update { it.copy(routePoints = emptyList(), isRouting = false,
            routeUnavailable = false, distanceKm = 0f, selectedRideForBreakdown = null) }
    }

    fun onPickupChange(text: String, context: Context? = null) = changeEndpoint(text, false, context)
    fun onDropChange(text: String, context: Context? = null) = changeEndpoint(text, true, context)

    private fun changeEndpoint(text: String, isDrop: Boolean, context: Context?) {
        cancelEndpointWork(isDrop)
        val preset = findPresetLocation(text)
        _uiState.update {
            if (isDrop) it.copy(dropText = text, dropLat = preset?.lat, dropLng = preset?.lng)
            else it.copy(pickupText = text, pickupLat = preset?.lat, pickupLng = preset?.lng,
                isCurrentLocationActive = false)
        }
        updateDistanceAndRecalculate()
        // Typing is not selection. Only a chosen result, GPS, or an explicit
        // legacy-route resolution may set coordinates. Never silently accept a first match.
    }

    private fun findPresetLocation(name: String): PresetLocation? =
        CityData.supportedCities.flatMap { it.popularLocations }.firstOrNull {
            name.isNotBlank() && it.name.equals(name, true) && validCoordinates(it.lat, it.lng)
        }

    private fun triggerForwardGeocode(context: Context, query: String, isDrop: Boolean) {
        val city = _uiState.value.selectedCity
        val job = viewModelScope.launch {
            delay(500)
            val coords = geocode(context, query, city.name)
            ensureActive()
            val current = _uiState.value
            if (current.selectedCity != city || (if (isDrop) current.dropText else current.pickupText) != query) return@launch
            if (coords != null && validCoordinates(coords.first, coords.second)) {
                _uiState.update {
                    if (isDrop) it.copy(dropLat = coords.first, dropLng = coords.second)
                    else it.copy(pickupLat = coords.first, pickupLng = coords.second)
                }
                updateDistanceAndRecalculate()
            } else {
                _uiState.update { it.copy(notificationMessage = "Could not resolve this address. Select a search result or try another address.") }
            }
        }
        if (isDrop) dropGeocodeJob = job else pickupGeocodeJob = job
    }

    fun fetchCurrentLocation(context: Context) {
        cancelEndpointWork(false)
        _uiState.update { it.copy(pickupLat = null, pickupLng = null, isLocatingUser = true,
            isCurrentLocationActive = false) }
        recalculateFares(false)
        locationJob = viewModelScope.launch {
            try {
                val loc = LocationHelper.currentLocation(context.applicationContext)
                ensureActive()
                onCurrentLocationSuccess(loc)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onLocationError(e.message ?: "Could not detect location. Enter a pickup address.")
            }
        }
    }

    fun onCurrentLocationSuccess(loc: UserLocationData) {
        if (!validCoordinates(loc.latitude, loc.longitude)) {
            onLocationError("Location was invalid. Enter a pickup address.")
            return
        }
        invalidateRoute()
        _uiState.update { it.copy(
            selectedCity = if (loc.cityName.isBlank()) it.selectedCity else CityData.getCityByNameOrId(loc.cityName),
            pickupText = loc.addressName.ifBlank { loc.shortName }, pickupLat = loc.latitude, pickupLng = loc.longitude,
            isCurrentLocationActive = true, isLocatingUser = false,
            notificationMessage = "Pickup location detected. Provider availability must be confirmed in their app."
        ) }
        updateDistanceAndRecalculate()
    }

    fun onLocationError(errorMsg: String) {
        _uiState.update { it.copy(isLocatingUser = false, notificationMessage = errorMsg) }
    }

    fun selectPlaceResult(place: PlaceSearchResult, isDrop: Boolean) {
        if (!validCoordinates(place.latitude, place.longitude)) {
            _uiState.update { it.copy(notificationMessage = "Select a place with valid coordinates.") }
            return
        }
        cancelEndpointWork(isDrop)
        val label = listOf(place.title, place.subtitle).filter { it.isNotBlank() }.joinToString(", ")
        _uiState.update {
            if (isDrop) it.copy(dropText = label, dropLat = place.latitude, dropLng = place.longitude)
            else it.copy(pickupText = label, pickupLat = place.latitude, pickupLng = place.longitude,
                isCurrentLocationActive = false)
        }
        updateDistanceAndRecalculate()
    }

    fun selectLocation(preset: PresetLocation, isDrop: Boolean) =
        selectPlaceResult(PlaceSearchResult(preset.name, "", preset.lat, preset.lng, preset.category), isDrop)

    fun selectCustomLocation(text: String, isDrop: Boolean, context: Context? = null) {
        changeEndpoint(text, isDrop, null)
        if (context != null && text.trim().length >= 3) triggerForwardGeocode(context.applicationContext, text, isDrop)
    }

    private fun cancelAllEndpointWork() {
        pickupGeocodeJob?.cancel(); dropGeocodeJob?.cancel(); locationJob?.cancel(); restoreJob?.cancel()
        invalidateRoute()
    }

    fun swapLocations() {
        cancelAllEndpointWork()
        _uiState.update { it.copy(pickupText = it.dropText, dropText = it.pickupText,
            pickupLat = it.dropLat, pickupLng = it.dropLng, dropLat = it.pickupLat, dropLng = it.pickupLng,
            isCurrentLocationActive = false, isLocatingUser = false) }
        updateDistanceAndRecalculate()
    }

    private fun updateDistanceAndRecalculate() {
        val s = _uiState.value
        if (!s.hasResolvedEndpoints) {
            invalidateRoute()
            recalculateFares(false)
            return
        }
        _uiState.update { it.copy(distanceKm = repository.estimateGeoDistance(s.pickupLat!!, s.pickupLng!!, s.dropLat!!, s.dropLng!!)) }
        recalculateFares(true)
        refineDistanceWithRouting()
    }

    private fun refineDistanceWithRouting() {
        routingJob?.cancel()
        val s = _uiState.value
        if (!s.hasResolvedEndpoints) return
        val revision = ++routeRevision
        _uiState.update { it.copy(isRouting = true, routeUnavailable = false) }
        routingJob = viewModelScope.launch {
            try {
                val result = route(s.pickupLat!!, s.pickupLng!!, s.dropLat!!, s.dropLng!!)
                ensureActive()
                if (revision != routeRevision) return@launch
                _uiState.update { it.copy(routePoints = result.points,
                    distanceKm = result.distanceKm?.takeIf { km -> km.isFinite() && km > 0f } ?: it.distanceKm,
                    isRouting = false, routeUnavailable = result.points.isEmpty()) }
                recalculateFares(false)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                if (revision == routeRevision) _uiState.update { it.copy(isRouting = false, routeUnavailable = true, routePoints = emptyList()) }
            }
        }
    }

    fun onDistanceChange(newDistance: Float) {
        if (!newDistance.isFinite() || !_uiState.value.hasResolvedEndpoints) return
        routeRevision++
        routingJob?.cancel()
        _uiState.update { it.copy(distanceKm = ((newDistance * 10).roundToInt() / 10f).coerceIn(0.5f, 60f), isRouting = false) }
        recalculateFares(false)
    }
    fun onTrafficChange(traffic: TrafficCondition) {
        _uiState.update { it.copy(trafficCondition = traffic) }; recalculateFares(false)
    }
    fun onWeatherChange(weather: WeatherOrTimeCondition) {
        _uiState.update { it.copy(weatherCondition = weather) }; recalculateFares(false)
    }
    fun onCategorySelect(category: VehicleCategory) {
        _uiState.update { it.copy(selectedCategory = category) }; filterAndSortCurrentRides()
    }
    fun onProviderFilterToggle(provider: RideProvider?) {
        _uiState.update { it.copy(selectedProviderFilter = if (it.selectedProviderFilter == provider) null else provider) }
        filterAndSortCurrentRides()
    }
    fun onSortOrderSelect(sort: SortOrder) {
        _uiState.update { it.copy(sortOrder = sort) }; filterAndSortCurrentRides()
    }
    fun selectCity(city: CityInfo) {
        cancelAllEndpointWork()
        _uiState.value = RideFareUiState(selectedCity = city,
            notificationMessage = "Select pickup and destination. Provider coverage is unverified.")
    }
    fun setCityDialogVisible(visible: Boolean) { _uiState.update { it.copy(isCityDialogVisible = visible) } }
    fun setSavedRoutesDialogVisible(visible: Boolean) { _uiState.update { it.copy(isSavedRoutesDialogVisible = visible) } }
    fun showBreakdown(option: RideOption?) { _uiState.update { it.copy(selectedRideForBreakdown = option) } }

    fun openSavedRoute(id: Long) {
        cancelAllEndpointWork()
        restoreJob = viewModelScope.launch {
            val saved = repository.getSavedRoute(id)
            ensureActive()
            restoreJob = null
            if (saved != null) applySavedRoute(saved)
            else _uiState.update { it.copy(notificationMessage = "This saved route no longer exists.") }
        }
    }

    fun applySavedRoute(route: SavedRoute) {
        // Never reuse previous endpoints, including when restoring a legacy favorite.
        restoreJob?.cancel()
        pickupGeocodeJob?.cancel(); dropGeocodeJob?.cancel(); locationJob?.cancel()
        invalidateRoute()
        val pickup = findPresetLocation(route.pickupName)
        val drop = findPresetLocation(route.dropName)
        val hasPickup = validCoordinates(route.pickupLat, route.pickupLng)
        val hasDrop = validCoordinates(route.dropLat, route.dropLng)
        _uiState.value = RideFareUiState(
            selectedCity = if (route.cityId.isNotBlank()) CityData.getCityByNameOrId(route.cityId) else _uiState.value.selectedCity,
            pickupText = route.pickupName, dropText = route.dropName,
            pickupLat = if (hasPickup) route.pickupLat else pickup?.lat,
            pickupLng = if (hasPickup) route.pickupLng else pickup?.lng,
            dropLat = if (hasDrop) route.dropLat else drop?.lat,
            dropLng = if (hasDrop) route.dropLng else drop?.lng,
            trafficCondition = TrafficCondition.entries.firstOrNull { it.name == route.trafficCondition } ?: TrafficCondition.NORMAL,
            weatherCondition = WeatherOrTimeCondition.entries.firstOrNull { it.name == route.weatherCondition } ?: WeatherOrTimeCondition.REGULAR
        )
        if (_uiState.value.hasResolvedEndpoints) updateDistanceAndRecalculate()
        else _uiState.update { it.copy(notificationMessage = "This older route needs its addresses selected again before comparison or booking.") }
    }

    fun saveCurrentRoute(title: String, priceAlertEnabled: Boolean = false, priceThreshold: Int = 250) {
        val s = _uiState.value
        if (!s.hasResolvedEndpoints) {
            _uiState.update { it.copy(notificationMessage = "Resolve both addresses before saving this route.") }
            return
        }
        viewModelScope.launch {
            repository.saveRoute(title.ifBlank { "${s.pickupText.take(15)} ➔ ${s.dropText.take(15)}" },
                s.pickupText, s.dropText, s.distanceKm, false, priceThreshold,
                s.pickupLat, s.pickupLng, s.dropLat, s.dropLng, s.selectedCity.id, s.trafficCondition, s.weatherCondition)
            _uiState.update { it.copy(notificationMessage = if (priceAlertEnabled)
                "Route saved. Price targets are unavailable without live provider prices."
                else "Route saved to Favorites.") }
        }
    }

    fun updatePriceAlert(routeId: Long, enabled: Boolean, threshold: Int) {
        if (enabled) {
            _uiState.update { it.copy(notificationMessage = "Price targets are unavailable without live provider prices.") }
            return
        }
        viewModelScope.launch { repository.updatePriceAlert(routeId, false, threshold.coerceAtLeast(1)) }
    }

    fun checkPriceTarget(saved: SavedRoute, context: Context) {
        _uiState.update { it.copy(notificationMessage =
            "Live prices are unavailable. Open the provider app to check your saved target.") }
    }

    fun deleteSavedRoute(route: SavedRoute) { viewModelScope.launch { repository.deleteRoute(route) } }
    fun clearHistory() { viewModelScope.launch { repository.clearHistory() } }
    fun clearNotification() { _uiState.update { it.copy(notificationMessage = null) } }

    fun recalculateFares(isUserInitiated: Boolean) {
        val s = _uiState.value
        val rides = if (s.hasResolvedEndpoints && s.distanceKm > 0f) repository.compareRides(
            s.pickupText, s.dropText, s.distanceKm, s.trafficCondition, s.weatherCondition, s.availableProviders) else emptyList()
        val cheapest = rides.minByOrNull { it.totalFare }
        _uiState.update { it.copy(allRideOptions = rides, cheapestOverall = cheapest,
            fastestPickupOverall = rides.minByOrNull { ride -> ride.etaMinutes },
            providerComparisons = com.example.util.FareCalculator.calculateProviderComparisons(rides),
            isComparing = false, lastEtaRefreshTime = System.currentTimeMillis()) }
        filterAndSortCurrentRides()
        if (isUserInitiated && cheapest != null) viewModelScope.launch {
            repository.recordSearch(s.pickupText, s.dropText, s.distanceKm, cheapest.provider.displayName,
                cheapest.serviceName, cheapest.totalFare, rides.maxOf { it.totalFare })
        }
    }
    fun refreshEstimates() {
        recalculateFares(false)
        _uiState.update { it.copy(notificationMessage = "Live prices are unavailable. Check prices in each provider app.") }
    }
    private fun filterAndSortCurrentRides() {
        val s = _uiState.value
        var rides = s.allRideOptions.filter { (s.selectedCategory == VehicleCategory.ALL || it.category == s.selectedCategory) &&
            (s.selectedProviderFilter == null || it.provider == s.selectedProviderFilter) }
        rides = when (s.sortOrder) {
            SortOrder.CHEAPEST -> rides.sortedBy { it.totalFare }
            SortOrder.FASTEST -> rides.sortedWith(compareBy({ it.etaMinutes }, { it.totalFare }))
            SortOrder.SHORTEST -> rides.sortedWith(compareBy({ it.tripDurationMinutes }, { it.totalFare }))
            SortOrder.SAVINGS -> rides.sortedByDescending { it.savingsVsHighest }
        }
        _uiState.update { it.copy(filteredRideOptions = rides, cheapestInCategory = rides.minByOrNull { ride -> ride.totalFare }) }
    }
}

class RideFareViewModelFactory(private val repository: RideRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RideFareViewModel::class.java))
        return RideFareViewModel(repository) as T
    }
}
