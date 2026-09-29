package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SavedRoute
import com.example.data.local.SearchHistoryItem
import com.example.data.model.CityData
import com.example.data.model.CityInfo
import com.example.data.model.PresetLocation
import com.example.data.model.RideOption
import com.example.data.model.RideProvider
import com.example.data.model.TrafficCondition
import com.example.data.model.VehicleCategory
import com.example.data.model.WeatherOrTimeCondition
import com.example.data.repository.RideRepository
import com.example.data.repository.RouteRoutingService
import com.example.util.LocationHelper
import com.example.util.UserLocationData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SortOrder(val label: String) {
    CHEAPEST("Lowest Fare"),
    FASTEST("Fastest Pickup"),
    SHORTEST("Shortest Trip"),
    SAVINGS("Max Savings")
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
    val isLocatingUser: Boolean = true,
    val distanceKm: Float = 0.0f,
    val trafficCondition: TrafficCondition = TrafficCondition.NORMAL,
    val weatherCondition: WeatherOrTimeCondition = WeatherOrTimeCondition.REGULAR,
    val selectedCategory: VehicleCategory = VehicleCategory.ALL,
    val selectedProviderFilter: RideProvider? = null,
    val sortOrder: SortOrder = SortOrder.CHEAPEST,
    val availableProviders: List<RideProvider> = listOf(RideProvider.OLA, RideProvider.UBER, RideProvider.RAPIDO),
    val isNoRideAppAvailable: Boolean = false,
    val unavailableCityName: String = "",
    val unavailableMessage: String = "",
    val allRideOptions: List<RideOption> = emptyList(),
    val filteredRideOptions: List<RideOption> = emptyList(),
    val cheapestOverall: RideOption? = null,
    val fastestPickupOverall: RideOption? = null,
    val providerComparisons: List<com.example.data.model.ProviderComparison> = emptyList(),
    val cheapestInCategory: RideOption? = null,
    val selectedRideForBreakdown: RideOption? = null,
    val isComparing: Boolean = false,
    val isCityDialogVisible: Boolean = false,
    val isSavedRoutesDialogVisible: Boolean = false,
    val notificationMessage: String? = null,
    val lastEtaRefreshTime: Long = System.currentTimeMillis()
)

class RideFareViewModel(private val repository: RideRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RideFareUiState())
    val uiState: StateFlow<RideFareUiState> = _uiState.asStateFlow()

    val savedRoutes: StateFlow<List<SavedRoute>> = repository.savedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryItem>> = repository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var geocodeJob: Job? = null
    private var routingJob: Job? = null

    init {
        seedInitialFavoritesIfEmpty()
    }

    private fun seedInitialFavoritesIfEmpty() {
        viewModelScope.launch {
            delay(400)
            if (savedRoutes.value.isEmpty()) {
                repository.saveRoute(
                    title = "Daily Commute",
                    pickup = "Indiranagar 100ft Road",
                    drop = "Whitefield ITPL",
                    distanceKm = 14.5f
                )
                repository.saveRoute(
                    title = "Airport Run",
                    pickup = "Koramangala 4th Block",
                    drop = "Kempegowda Int'l Airport (BLR)",
                    distanceKm = 38.5f
                )
            }
        }
    }

    fun onPickupChange(newPickup: String, context: Context? = null) {
        val matchingPreset = findPresetLocation(newPickup)
        val pLat = matchingPreset?.lat ?: _uiState.value.pickupLat
        val pLng = matchingPreset?.lng ?: _uiState.value.pickupLng

        _uiState.update {
            it.copy(
                pickupText = newPickup,
                pickupLat = if (matchingPreset != null) pLat else it.pickupLat,
                pickupLng = if (matchingPreset != null) pLng else it.pickupLng,
                isCurrentLocationActive = false
            )
        }

        if (matchingPreset != null) {
            updateDistanceAndRecalculate()
        } else if (context != null && newPickup.length >= 3) {
            triggerForwardGeocode(context, newPickup, isDrop = false)
        } else {
            updateDistanceAndRecalculate()
        }
    }

    fun onDropChange(newDrop: String, context: Context? = null) {
        val matchingPreset = findPresetLocation(newDrop)
        val dLat = matchingPreset?.lat
        val dLng = matchingPreset?.lng

        _uiState.update {
            it.copy(
                dropText = newDrop,
                dropLat = if (matchingPreset != null) dLat else (if (newDrop.isBlank()) null else it.dropLat),
                dropLng = if (matchingPreset != null) dLng else (if (newDrop.isBlank()) null else it.dropLng)
            )
        }

        if (newDrop.isBlank()) {
            _uiState.update { it.copy(distanceKm = 0f) }
            recalculateFares(isUserInitiated = false)
        } else if (matchingPreset != null) {
            updateDistanceAndRecalculate()
        } else if (context != null && newDrop.length >= 3) {
            triggerForwardGeocode(context, newDrop, isDrop = true)
        } else {
            updateDistanceAndRecalculate()
        }
    }

    private fun findPresetLocation(name: String): PresetLocation? {
        if (name.isBlank()) return null
        val citySpots = _uiState.value.selectedCity.popularLocations
        val direct = citySpots.find { it.name.equals(name, ignoreCase = true) }
        if (direct != null) return direct

        val allCitiesSpots = CityData.supportedCities.flatMap { it.popularLocations }
        return allCitiesSpots.find { it.name.equals(name, ignoreCase = true) }
            ?: allCitiesSpots.find { name.contains(it.name, ignoreCase = true) || it.name.contains(name, ignoreCase = true) }
    }

    private fun triggerForwardGeocode(context: Context, query: String, isDrop: Boolean) {
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            delay(500) // Debounce
            val cityName = _uiState.value.selectedCity.name
            val coords = LocationHelper.forwardGeocode(context, query, cityName)
            if (coords != null) {
                if (isDrop) {
                    _uiState.update { it.copy(dropLat = coords.first, dropLng = coords.second) }
                } else {
                    _uiState.update { it.copy(pickupLat = coords.first, pickupLng = coords.second) }
                }
                updateDistanceAndRecalculate()
            }
        }
    }

    fun setLocatingUser(locating: Boolean) {
        _uiState.update { it.copy(isLocatingUser = locating) }
    }

    fun onCurrentLocationSuccess(loc: UserLocationData) {
        // Universal coverage check for ANY city detected via GPS
        val detectedCityName = if (loc.cityName.isNotBlank()) loc.cityName else loc.shortName
        val coverage = CityData.checkServiceCoverage(detectedCityName, loc.latitude, loc.longitude)

        val cityObj = CityData.getCityByNameOrId(detectedCityName)

        val pLat = loc.latitude
        val pLng = loc.longitude
        val dLat = _uiState.value.dropLat
        val dLng = _uiState.value.dropLng

        val dist = if (dLat != null && dLng != null && _uiState.value.dropText.isNotBlank()) {
            repository.estimateGeoDistance(pLat, pLng, dLat, dLng)
        } else {
            _uiState.value.distanceKm
        }

        if (!coverage.isAvailable) {
            _uiState.update {
                it.copy(
                    selectedCity = cityObj,
                    pickupText = loc.shortName.ifBlank { loc.addressName },
                    pickupLat = pLat,
                    pickupLng = pLng,
                    isCurrentLocationActive = true,
                    isLocatingUser = false,
                    isNoRideAppAvailable = true,
                    unavailableCityName = detectedCityName,
                    unavailableMessage = coverage.unavailableReason ?: "No ride-hailing services (Ola, Uber, Rapido) are operational in this area.",
                    availableProviders = emptyList()
                )
            }
            recalculateFares(isUserInitiated = false)
            return
        }

        _uiState.update {
            it.copy(
                selectedCity = cityObj,
                pickupText = loc.shortName.ifBlank { loc.addressName },
                pickupLat = pLat,
                pickupLng = pLng,
                distanceKm = dist,
                isCurrentLocationActive = true,
                isLocatingUser = false,
                isNoRideAppAvailable = false,
                availableProviders = coverage.availableProviders,
                notificationMessage = "GPS location detected: ${loc.shortName}"
            )
        }

        if (dLat != null && dLng != null && _uiState.value.dropText.isNotBlank()) {
            recalculateFares(isUserInitiated = true)
            refineDistanceWithRouting(pLat, pLng, dLat, dLng)
        }
    }

    fun onLocationError(errorMsg: String) {
        _uiState.update {
            it.copy(
                isLocatingUser = false,
                notificationMessage = errorMsg
            )
        }
    }

    fun dismissNoRideAppDialog() {
        _uiState.update { it.copy(isNoRideAppAvailable = false) }
    }

    fun selectPlaceResult(place: com.example.data.repository.PlaceSearchResult, isDrop: Boolean) {
        val currentState = _uiState.value
        if (isDrop) {
            val pLat = currentState.pickupLat
            val pLng = currentState.pickupLng
            val newDist = if (pLat != null && pLng != null) {
                repository.estimateGeoDistance(pLat, pLng, place.latitude, place.longitude)
            } else {
                currentState.distanceKm
            }
            _uiState.update {
                it.copy(
                    dropText = place.title,
                    dropLat = place.latitude,
                    dropLng = place.longitude,
                    distanceKm = newDist
                )
            }
            if (pLat != null && pLng != null) {
                refineDistanceWithRouting(pLat, pLng, place.latitude, place.longitude)
            }
        } else {
            val dLat = currentState.dropLat
            val dLng = currentState.dropLng
            val newDist = if (dLat != null && dLng != null && currentState.dropText.isNotBlank()) {
                repository.estimateGeoDistance(place.latitude, place.longitude, dLat, dLng)
            } else {
                currentState.distanceKm
            }
            _uiState.update {
                it.copy(
                    pickupText = place.title,
                    pickupLat = place.latitude,
                    pickupLng = place.longitude,
                    isCurrentLocationActive = false,
                    distanceKm = newDist
                )
            }
            if (dLat != null && dLng != null && currentState.dropText.isNotBlank()) {
                refineDistanceWithRouting(place.latitude, place.longitude, dLat, dLng)
            }
        }
        recalculateFares(isUserInitiated = true)
    }

    fun selectLocation(preset: PresetLocation, isDrop: Boolean) {
        val currentState = _uiState.value
        if (isDrop) {
            val pLat = currentState.pickupLat
            val pLng = currentState.pickupLng
            val newDist = if (pLat != null && pLng != null) {
                repository.estimateGeoDistance(pLat, pLng, preset.lat, preset.lng)
            } else {
                currentState.distanceKm
            }
            _uiState.update {
                it.copy(
                    dropText = preset.name,
                    dropLat = preset.lat,
                    dropLng = preset.lng,
                    distanceKm = newDist
                )
            }
            if (pLat != null && pLng != null) {
                refineDistanceWithRouting(pLat, pLng, preset.lat, preset.lng)
            }
        } else {
            val dLat = currentState.dropLat
            val dLng = currentState.dropLng
            val newDist = if (dLat != null && dLng != null && currentState.dropText.isNotBlank()) {
                repository.estimateGeoDistance(preset.lat, preset.lng, dLat, dLng)
            } else {
                currentState.distanceKm
            }
            _uiState.update {
                it.copy(
                    pickupText = preset.name,
                    pickupLat = preset.lat,
                    pickupLng = preset.lng,
                    isCurrentLocationActive = false,
                    distanceKm = newDist
                )
            }
            if (dLat != null && dLng != null && currentState.dropText.isNotBlank()) {
                refineDistanceWithRouting(preset.lat, preset.lng, dLat, dLng)
            }
        }
        recalculateFares(isUserInitiated = true)
    }

    fun selectCustomLocation(text: String, isDrop: Boolean, context: Context? = null) {
        val matchingPreset = findPresetLocation(text)
        if (matchingPreset != null) {
            selectLocation(matchingPreset, isDrop)
            return
        }

        if (isDrop) {
            _uiState.update { it.copy(dropText = text) }
            if (context != null) triggerForwardGeocode(context, text, isDrop = true)
        } else {
            _uiState.update { it.copy(pickupText = text, isCurrentLocationActive = false) }
            if (context != null) triggerForwardGeocode(context, text, isDrop = false)
        }
        recalculateFares(isUserInitiated = true)
    }

    fun swapLocations() {
        val currentPickup = _uiState.value.pickupText
        val currentDrop = _uiState.value.dropText
        val pLat = _uiState.value.pickupLat
        val pLng = _uiState.value.pickupLng
        val dLat = _uiState.value.dropLat
        val dLng = _uiState.value.dropLng

        _uiState.update {
            it.copy(
                pickupText = currentDrop,
                dropText = currentPickup,
                pickupLat = dLat,
                pickupLng = dLng,
                dropLat = pLat,
                dropLng = pLng,
                isCurrentLocationActive = false
            )
        }
        updateDistanceAndRecalculate()
    }

    private fun updateDistanceAndRecalculate() {
        val state = _uiState.value
        val pLat = state.pickupLat
        val pLng = state.pickupLng
        val dLat = state.dropLat
        val dLng = state.dropLng

        if (state.dropText.isBlank()) {
            _uiState.update { it.copy(distanceKm = 0f) }
            recalculateFares(isUserInitiated = false)
            return
        }

        if (pLat != null && pLng != null && dLat != null && dLng != null) {
            val dist = repository.estimateGeoDistance(pLat, pLng, dLat, dLng)
            _uiState.update { it.copy(distanceKm = dist) }
            recalculateFares(isUserInitiated = true)
            refineDistanceWithRouting(pLat, pLng, dLat, dLng)
        } else {
            recalculateFares(isUserInitiated = false)
        }
    }

    private fun refineDistanceWithRouting(pLat: Double, pLng: Double, dLat: Double, dLng: Double) {
        routingJob?.cancel()
        routingJob = viewModelScope.launch {
            try {
                val result = RouteRoutingService.getDrivingRouteResult(pLat, pLng, dLat, dLng)
                if (result.distanceKm != null && result.distanceKm > 0.1f) {
                    _uiState.update { it.copy(distanceKm = result.distanceKm) }
                    recalculateFares(isUserInitiated = false)
                }
            } catch (_: Exception) {}
        }
    }

    fun onDistanceChange(newDistance: Float) {
        val rounded = (newDistance * 10).roundToInt() / 10f
        _uiState.update { it.copy(distanceKm = rounded.coerceIn(0.5f, 60.0f)) }
        recalculateFares(isUserInitiated = false)
    }

    fun onTrafficChange(traffic: TrafficCondition) {
        _uiState.update { it.copy(trafficCondition = traffic) }
        recalculateFares(isUserInitiated = true)
    }

    fun onWeatherChange(weather: WeatherOrTimeCondition) {
        _uiState.update { it.copy(weatherCondition = weather) }
        recalculateFares(isUserInitiated = true)
    }

    fun onCategorySelect(category: VehicleCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
        filterAndSortCurrentRides()
    }

    fun onProviderFilterToggle(provider: RideProvider?) {
        _uiState.update {
            it.copy(
                selectedProviderFilter = if (it.selectedProviderFilter == provider) null else provider
            )
        }
        filterAndSortCurrentRides()
    }

    fun onSortOrderSelect(sort: SortOrder) {
        _uiState.update { it.copy(sortOrder = sort) }
        filterAndSortCurrentRides()
    }

    fun selectCity(city: CityInfo) {
        val coverage = CityData.checkServiceCoverage(city.name, null, null)

        if (!coverage.isAvailable) {
            _uiState.update {
                it.copy(
                    selectedCity = city,
                    dropText = "",
                    dropLat = null,
                    dropLng = null,
                    distanceKm = 0.0f,
                    isCityDialogVisible = false,
                    isNoRideAppAvailable = true,
                    unavailableCityName = city.name,
                    unavailableMessage = coverage.unavailableReason ?: "No ride-hailing apps operate in this region.",
                    availableProviders = emptyList()
                )
            }
            recalculateFares(isUserInitiated = false)
            return
        }

        _uiState.update {
            it.copy(
                selectedCity = city,
                dropText = "",
                dropLat = null,
                dropLng = null,
                distanceKm = 0.0f,
                isCityDialogVisible = false,
                isNoRideAppAvailable = false,
                availableProviders = coverage.availableProviders
            )
        }
        recalculateFares(isUserInitiated = true)
    }

    fun setCityDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isCityDialogVisible = visible) }
    }

    fun setSavedRoutesDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isSavedRoutesDialogVisible = visible) }
    }

    fun showBreakdown(option: RideOption?) {
        _uiState.update { it.copy(selectedRideForBreakdown = option) }
    }

    fun applySavedRoute(route: SavedRoute) {
        _uiState.update {
            it.copy(
                pickupText = route.pickupName,
                dropText = route.dropName,
                distanceKm = route.distanceKm,
                isSavedRoutesDialogVisible = false
            )
        }
        recalculateFares(isUserInitiated = true)
    }

    fun saveCurrentRoute(
        title: String,
        priceAlertEnabled: Boolean = false,
        priceThreshold: Int = 250,
        context: Context? = null
    ) {
        val s = _uiState.value
        val name = if (title.isBlank()) "${s.pickupText.take(15)} ➔ ${s.dropText.take(15)}" else title
        viewModelScope.launch {
            repository.saveRoute(
                title = name,
                pickup = s.pickupText,
                drop = s.dropText,
                distanceKm = s.distanceKm,
                priceAlertEnabled = priceAlertEnabled,
                priceThreshold = priceThreshold
            )
            val msg = if (priceAlertEnabled) {
                "Route saved with price alert for ≤ ₹$priceThreshold!"
            } else {
                "Route saved to Favorites!"
            }
            _uiState.update { it.copy(notificationMessage = msg) }

            if (priceAlertEnabled && context != null) {
                val testRoute = SavedRoute(
                    title = name,
                    pickupName = s.pickupText,
                    dropName = s.dropText,
                    distanceKm = s.distanceKm,
                    priceAlertEnabled = true,
                    priceThreshold = priceThreshold
                )
                com.example.util.PriceAlertNotificationHelper.sendTestPriceAlert(context, testRoute)
            }
        }
    }

    fun updatePriceAlert(
        routeId: Long,
        enabled: Boolean,
        threshold: Int,
        context: Context? = null
    ) {
        viewModelScope.launch {
            repository.updatePriceAlert(routeId, enabled, threshold)
            val updatedRoute = savedRoutes.value.find { it.id == routeId }?.copy(
                priceAlertEnabled = enabled,
                priceThreshold = threshold
            )
            val msg = if (enabled) {
                "Price alert active: Alert when fare drops below ₹$threshold"
            } else {
                "Price alert disabled"
            }
            _uiState.update { it.copy(notificationMessage = msg) }

            if (enabled && context != null && updatedRoute != null) {
                val rides = repository.compareRides(
                    pickup = updatedRoute.pickupName,
                    drop = updatedRoute.dropName,
                    distanceKm = updatedRoute.distanceKm,
                    traffic = _uiState.value.trafficCondition,
                    weather = _uiState.value.weatherCondition,
                    availableProviders = _uiState.value.availableProviders
                )
                val cheapest = rides.minByOrNull { it.totalFare }
                if (cheapest != null && cheapest.totalFare <= threshold) {
                    com.example.util.PriceAlertNotificationHelper.sendPriceDropNotification(
                        context = context,
                        route = updatedRoute,
                        lowestPrice = cheapest.totalFare,
                        providerName = cheapest.provider.displayName,
                        serviceName = cheapest.serviceName
                    )
                } else {
                    com.example.util.PriceAlertNotificationHelper.sendTestPriceAlert(context, updatedRoute)
                }
            }
        }
    }

    fun testPriceAlert(route: SavedRoute, context: Context) {
        viewModelScope.launch {
            val rides = repository.compareRides(
                pickup = route.pickupName,
                drop = route.dropName,
                distanceKm = route.distanceKm,
                traffic = _uiState.value.trafficCondition,
                weather = _uiState.value.weatherCondition,
                availableProviders = _uiState.value.availableProviders
            )
            val cheapest = rides.minByOrNull { it.totalFare }
            if (cheapest != null) {
                com.example.util.PriceAlertNotificationHelper.sendPriceDropNotification(
                    context = context,
                    route = route,
                    lowestPrice = cheapest.totalFare,
                    providerName = cheapest.provider.displayName,
                    serviceName = cheapest.serviceName
                )
            } else {
                com.example.util.PriceAlertNotificationHelper.sendTestPriceAlert(context, route)
            }
            _uiState.update { it.copy(notificationMessage = "Price drop alert test notification sent!") }
        }
    }

    fun deleteSavedRoute(route: SavedRoute) {
        viewModelScope.launch {
            repository.deleteRoute(route)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    fun recalculateFares(isUserInitiated: Boolean) {
        val state = _uiState.value
        if (state.isNoRideAppAvailable || state.dropText.isBlank() || state.distanceKm <= 0.1f) {
            _uiState.update {
                it.copy(
                    allRideOptions = emptyList(),
                    filteredRideOptions = emptyList(),
                    cheapestOverall = null,
                    fastestPickupOverall = null,
                    providerComparisons = emptyList(),
                    cheapestInCategory = null,
                    isComparing = false
                )
            }
            return
        }

        val pickup = state.pickupText.ifBlank { "Pickup Point" }
        val drop = state.dropText

        val rawList = repository.compareRides(
            pickup = pickup,
            drop = drop,
            distanceKm = state.distanceKm,
            traffic = state.trafficCondition,
            weather = state.weatherCondition,
            availableProviders = state.availableProviders
        )

        val cheapestAll = rawList.minByOrNull { it.totalFare }
        val fastestAll = rawList.minByOrNull { it.etaMinutes }
        val comparisons = com.example.util.FareCalculator.calculateProviderComparisons(rawList)

        _uiState.update {
            it.copy(
                allRideOptions = rawList,
                cheapestOverall = cheapestAll,
                fastestPickupOverall = fastestAll,
                providerComparisons = comparisons,
                lastEtaRefreshTime = System.currentTimeMillis(),
                isComparing = isUserInitiated
            )
        }

        filterAndSortCurrentRides()

        if (isUserInitiated && cheapestAll != null) {
            viewModelScope.launch {
                val maxFare = rawList.maxOfOrNull { it.totalFare } ?: cheapestAll.totalFare
                repository.recordSearch(
                    pickup = pickup,
                    drop = drop,
                    distanceKm = state.distanceKm,
                    cheapestProvider = cheapestAll.provider.displayName,
                    cheapestService = cheapestAll.serviceName,
                    cheapestFare = cheapestAll.totalFare,
                    maxFare = maxFare
                )
                delay(300)
                _uiState.update { it.copy(isComparing = false) }
            }
        }
    }

    fun refreshLiveEtas() {
        recalculateFares(isUserInitiated = false)
        _uiState.update { it.copy(notificationMessage = "Driver arrival ETAs refreshed in real-time!") }
    }

    private fun filterAndSortCurrentRides() {
        val state = _uiState.value
        var list = state.allRideOptions

        if (state.selectedCategory != VehicleCategory.ALL) {
            list = list.filter { it.category == state.selectedCategory }
        }

        if (state.selectedProviderFilter != null) {
            list = list.filter { it.provider == state.selectedProviderFilter }
        }

        list = when (state.sortOrder) {
            SortOrder.CHEAPEST -> list.sortedBy { it.totalFare }
            SortOrder.FASTEST -> list.sortedWith(compareBy({ it.etaMinutes }, { it.totalFare }))
            SortOrder.SHORTEST -> list.sortedWith(compareBy({ it.tripDurationMinutes }, { it.totalFare }))
            SortOrder.SAVINGS -> list.sortedByDescending { it.savingsVsHighest }
        }

        val cheapestInCurrentCategory = list.minByOrNull { it.totalFare }

        _uiState.update {
            it.copy(
                filteredRideOptions = list,
                cheapestInCategory = cheapestInCurrentCategory
            )
        }
    }
}

class RideFareViewModelFactory(private val repository: RideRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RideFareViewModel::class.java)) {
            return RideFareViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
