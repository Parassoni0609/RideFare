package com.example.data.repository

import com.example.data.local.RideDao
import com.example.data.local.SavedRoute
import com.example.data.local.SearchHistoryItem
import com.example.data.model.RideOption
import com.example.data.model.TrafficCondition
import com.example.data.model.WeatherOrTimeCondition
import com.example.util.FareCalculator
import kotlinx.coroutines.flow.Flow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RideRepository(private val rideDao: RideDao) {

    val savedRoutes: Flow<List<SavedRoute>> = rideDao.getAllSavedRoutes()
    val searchHistory: Flow<List<SearchHistoryItem>> = rideDao.getSearchHistory()

    fun compareRides(
        pickup: String,
        drop: String,
        distanceKm: Float,
        traffic: TrafficCondition,
        weather: WeatherOrTimeCondition,
        availableProviders: List<com.example.data.model.RideProvider> = listOf(
            com.example.data.model.RideProvider.OLA,
            com.example.data.model.RideProvider.UBER,
            com.example.data.model.RideProvider.RAPIDO
        )
    ): List<RideOption> {
        return FareCalculator.calculateAllRides(
            pickup = pickup,
            drop = drop,
            distanceKm = distanceKm,
            traffic = traffic,
            weather = weather,
            availableProviders = availableProviders
        )
    }

    suspend fun getSavedRoute(id: Long): SavedRoute? = rideDao.getSavedRoute(id)

    suspend fun saveRoute(
        title: String,
        pickup: String,
        drop: String,
        distanceKm: Float,
        priceAlertEnabled: Boolean = false,
        priceThreshold: Int = 250,
        pickupLat: Double? = null,
        pickupLng: Double? = null,
        dropLat: Double? = null,
        dropLng: Double? = null,
        cityId: String = "",
        traffic: TrafficCondition = TrafficCondition.NORMAL,
        weather: WeatherOrTimeCondition = WeatherOrTimeCondition.REGULAR
    ): Long {
        val route = SavedRoute(
            title = title,
            pickupName = pickup,
            dropName = drop,
            distanceKm = distanceKm,
            isFavorite = true,
            priceAlertEnabled = priceAlertEnabled,
            priceThreshold = priceThreshold.coerceAtLeast(1),
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            dropLat = dropLat,
            dropLng = dropLng,
            cityId = cityId,
            trafficCondition = traffic.name,
            weatherCondition = weather.name
        )
        return rideDao.insertSavedRoute(route)
    }

    suspend fun updatePriceAlert(routeId: Long, enabled: Boolean, threshold: Int) {
        rideDao.updatePriceAlert(routeId, enabled, threshold)
    }

    suspend fun deleteRoute(route: SavedRoute) {
        rideDao.deleteSavedRoute(route)
    }

    suspend fun recordSearch(
        pickup: String,
        drop: String,
        distanceKm: Float,
        cheapestProvider: String,
        cheapestService: String,
        cheapestFare: Int,
        maxFare: Int
    ) {
        val historyItem = SearchHistoryItem(
            pickupName = pickup,
            dropName = drop,
            distanceKm = distanceKm,
            cheapestProvider = cheapestProvider,
            cheapestService = cheapestService,
            cheapestFare = cheapestFare,
            maxFare = maxFare
        )
        rideDao.insertSearchHistory(historyItem)
    }

    suspend fun clearHistory() {
        rideDao.clearHistory()
    }

    // Helper to estimate accurate driving road distance in km between two lat/lng
    fun estimateGeoDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        val straightKm = results[0] / 1000f

        // Real-world road tortuosity factor based on urban road network layouts
        val roadFactor = when {
            straightKm < 3.0f -> 1.35f
            straightKm < 15.0f -> 1.25f
            straightKm < 40.0f -> 1.18f
            else -> 1.12f
        }
        val calculatedRoadKm = straightKm * roadFactor
        return (kotlin.math.round(calculatedRoadKm * 10f) / 10f).coerceAtLeast(0.3f)
    }
}
