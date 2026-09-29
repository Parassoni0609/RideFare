package com.example.ridefare

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.*
import com.example.ui.viewmodel.RideFareViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RideFareViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeRideDao
    private lateinit var context: Context
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        dao = FakeRideDao()
        context = ApplicationProvider.getApplicationContext()
    }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun model(route: suspend (Double, Double, Double, Double) -> RouteResult = { _, _, _, _ ->
        RouteResult(emptyList(), null, null) }) = RideFareViewModel(RideRepository(dao), route = route)
    private fun resolve(vm: RideFareViewModel) {
        vm.selectPlaceResult(PlaceSearchResult("A", "", 12.9, 77.6), false)
        vm.selectPlaceResult(PlaceSearchResult("B", "", 13.0, 77.7), true)
    }
    private fun favorite() = SavedRoute(id = 7, title = "Home", pickupName = "Saved A", dropName = "Saved B",
        distanceKm = 10f, pickupLat = 28.6, pickupLng = 77.2, dropLat = 28.7, dropLng = 77.3,
        cityId = "delhi", trafficCondition = "HEAVY", weatherCondition = "RAIN")

    @Test fun `editing either endpoint immediately invalidates estimates and old coordinates`() = runTest(dispatcher) {
        val vm = model(); resolve(vm); runCurrent()
        assertTrue(vm.uiState.value.allRideOptions.isNotEmpty())
        vm.onDropChange("Unresolved destination")
        assertNull(vm.uiState.value.dropLat)
        assertEquals(0f, vm.uiState.value.distanceKm)
        assertTrue(vm.uiState.value.allRideOptions.isEmpty())
        resolve(vm); runCurrent()
        vm.onPickupChange("")
        assertNull(vm.uiState.value.pickupLat)
        assertTrue(vm.uiState.value.allRideOptions.isEmpty())
    }

    @Test fun `favorites restore endpoints and conditions and legacy routes never retain old coordinates`() = runTest(dispatcher) {
        val vm = model(); resolve(vm); runCurrent()
        vm.applySavedRoute(favorite()); runCurrent()
        val restored = vm.uiState.value
        assertEquals(28.6, restored.pickupLat!!, 0.0)
        assertEquals(77.3, restored.dropLng!!, 0.0)
        assertEquals(TrafficCondition.HEAVY, restored.trafficCondition)
        assertEquals(WeatherOrTimeCondition.RAIN, restored.weatherCondition)
        assertEquals("delhi", restored.selectedCity.id)
        vm.applySavedRoute(SavedRoute(title = "Legacy", pickupName = "Unresolved A", dropName = "Unresolved B", distanceKm = 9f))
        assertNull(vm.uiState.value.pickupLat)
        assertNull(vm.uiState.value.dropLat)
        assertTrue(vm.uiState.value.allRideOptions.isEmpty())
    }

    @Test fun `saving stores coordinates and never seeds artificial favorites`() = runTest(dispatcher) {
        val vm = model(); advanceUntilIdle()
        assertTrue(dao.routes.value.isEmpty())
        resolve(vm); runCurrent()
        vm.onTrafficChange(TrafficCondition.JAMMED)
        vm.saveCurrentRoute("Work", true, 200); runCurrent()
        val saved = dao.routes.value.single()
        assertEquals(12.9, saved.pickupLat!!, 0.0)
        assertEquals(13.0, saved.dropLat!!, 0.0)
        assertEquals("JAMMED", saved.trafficCondition)
        assertEquals(200, saved.priceThreshold)
    }

    @Test fun `pickup and drop geocoding run independently and selection cancels outdated work`() = runTest(dispatcher) {
        val calls = mutableListOf<String>()
        val vm = RideFareViewModel(RideRepository(dao), geocode = { _, query, _ ->
            calls.add(query); delay(100)
            if (query.startsWith("Pickup")) Pair(12.9, 77.6) else Pair(13.0, 77.7)
        }, route = { _, _, _, _ -> RouteResult(emptyList(), null, null) })
        vm.onPickupChange("Pickup query", context)
        vm.onDropChange("Drop query", context)
        advanceUntilIdle()
        assertEquals(setOf("Pickup query", "Drop query"), calls.toSet())
        assertTrue(vm.uiState.value.hasResolvedEndpoints)
        vm.onDropChange("Drop stale", context)
        advanceTimeBy(550)
        vm.selectPlaceResult(PlaceSearchResult("Chosen", "", 14.0, 78.0), true)
        advanceUntilIdle()
        assertEquals(14.0, vm.uiState.value.dropLat!!, 0.0)
    }

    @Test fun `late route results cannot change a cleared destination`() = runTest(dispatcher) {
        var requests = 0
        val vm = model { _, _, _, _ ->
            requests++
            withContext(NonCancellable) { delay(1000) }
            RouteResult(listOf(GeoPoint(12.9, 77.6), GeoPoint(13.0, 77.7)), 99f, 80)
        }
        resolve(vm); runCurrent()
        assertEquals(1, requests)
        vm.onDropChange(""); advanceUntilIdle()
        assertEquals(0f, vm.uiState.value.distanceKm)
        assertTrue(vm.uiState.value.routePoints.isEmpty())
        assertTrue(vm.uiState.value.allRideOptions.isEmpty())
    }

    @Test fun `city changes clear both endpoints and notification route ids restore their own favorite`() = runTest(dispatcher) {
        val vm = model(); resolve(vm); runCurrent()
        vm.selectCity(CityData.getCityByNameOrId("Delhi"))
        assertNull(vm.uiState.value.pickupLat)
        assertNull(vm.uiState.value.dropLat)
        dao.routes.value = listOf(favorite())
        vm.openSavedRoute(7); runCurrent()
        assertEquals("Saved A", vm.uiState.value.pickupText)
        assertEquals(28.6, vm.uiState.value.pickupLat!!, 0.0)
    }

    @Test fun `manual target checks use saved conditions instead of the currently viewed route`() = runTest(dispatcher) {
        val vm = model()
        vm.onTrafficChange(TrafficCondition.LIGHT)
        vm.onWeatherChange(WeatherOrTimeCondition.REGULAR)
        val saved = favorite().copy(priceThreshold = 1)
        val expected = com.example.util.FareCalculator.calculateAllRides(saved.pickupName, saved.dropName,
            saved.distanceKm, TrafficCondition.HEAVY, WeatherOrTimeCondition.RAIN).minOf { it.totalFare }
        vm.checkPriceTarget(saved, context)
        assertTrue(vm.uiState.value.notificationMessage!!.contains("Estimated ₹$expected"))
        assertTrue(vm.uiState.value.notificationMessage!!.contains("above"))
    }

    @Test fun `routing failure is explicit and manual distance is not overwritten by in-flight routing`() = runTest(dispatcher) {
        val vm = model { _, _, _, _ -> delay(1000); RouteResult(emptyList(), 99f, 80) }
        resolve(vm); runCurrent()
        vm.onDistanceChange(6f); advanceUntilIdle()
        assertEquals(6f, vm.uiState.value.distanceKm)
        val failed = model(); resolve(failed); runCurrent()
        assertTrue(failed.uiState.value.routeUnavailable)
        assertTrue(failed.uiState.value.routePoints.isEmpty())
    }
}

internal class FakeRideDao : RideDao {
    val routes = MutableStateFlow<List<SavedRoute>>(emptyList())
    private val history = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    override fun getAllSavedRoutes(): Flow<List<SavedRoute>> = routes
    override suspend fun getSavedRoute(id: Long) = routes.value.find { it.id == id }
    override suspend fun insertSavedRoute(route: SavedRoute): Long {
        val saved = route.copy(id = if (route.id > 0) route.id else (routes.value.maxOfOrNull { it.id } ?: 0) + 1)
        routes.value = routes.value + saved; return saved.id
    }
    override suspend fun updatePriceAlert(routeId: Long, enabled: Boolean, threshold: Int) {
        routes.value = routes.value.map { if (it.id == routeId) it.copy(priceAlertEnabled = enabled, priceThreshold = threshold) else it }
    }
    override suspend fun updateSavedRoute(route: SavedRoute) { routes.value = routes.value.map { if (it.id == route.id) route else it } }
    override suspend fun deleteSavedRoute(route: SavedRoute) { routes.value = routes.value.filter { it.id != route.id } }
    override fun getSearchHistory(): Flow<List<SearchHistoryItem>> = history
    override suspend fun insertSearchHistory(item: SearchHistoryItem): Long {
        history.value = history.value + item; return history.value.size.toLong()
    }
    override suspend fun clearHistory() { history.value = emptyList() }
}
