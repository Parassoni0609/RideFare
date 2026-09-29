package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RideOption
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.CategoryAndSortRow
import com.example.ui.components.CheapestDealHeroCard
import com.example.ui.components.CitySelectorDialog
import com.example.ui.components.FareBreakdownModal
import com.example.ui.components.LocationInputSection
import com.example.ui.components.ProviderEtaComparisonBar
import com.example.ui.components.RideHeaderBar
import com.example.ui.components.RideOptionCard
import com.example.ui.components.RouteMapCard
import com.example.ui.components.SaveRouteDialog
import com.example.ui.components.SavedRoutesDialog
import com.example.ui.components.TripMetricsSection
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.Slate200
import com.example.ui.viewmodel.RideFareViewModel
import com.example.util.IntentHelper
import com.example.util.LocationHelper

@Composable
fun HomeScreen(
    viewModel: RideFareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedRoutes by viewModel.savedRoutes.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var isSaveDialogVisible by remember { mutableStateOf(false) }

    fun fetchLocation() {
        viewModel.setLocatingUser(true)
        LocationHelper.fetchCurrentLocation(
            context = context,
            onSuccess = { loc -> viewModel.onCurrentLocationSuccess(loc) },
            onError = { err -> viewModel.onLocationError(err) }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            viewModel.onLocationError("Location permission denied. Cannot fetch current location.")
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Price drop notifications enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!com.example.util.PriceAlertNotificationHelper.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Auto-detect GPS location on startup
        if (LocationHelper.hasLocationPermission(context)) {
            fetchLocation()
        } else {
            // Automatically prompt for location permission on start so GPS location is automatically fetched
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun requestLocationAndFetch() {
        if (LocationHelper.hasLocationPermission(context)) {
            fetchLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RideHeaderBar(
                selectedCity = uiState.selectedCity,
                onCityClick = { viewModel.setCityDialogVisible(true) },
                onSavedRoutesClick = { viewModel.setSavedRoutesDialogVisible(true) },
                onRefreshClick = { viewModel.recalculateFares(isUserInitiated = true) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_scroll"),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Interactive Route Map & GPS Locator Card
            item(key = "route_map") {
                RouteMapCard(
                    pickupName = uiState.pickupText,
                    dropName = uiState.dropText,
                    pickupLat = uiState.pickupLat,
                    pickupLng = uiState.pickupLng,
                    dropLat = uiState.dropLat,
                    dropLng = uiState.dropLng,
                    distanceKm = uiState.distanceKm,
                    durationMins = uiState.cheapestOverall?.tripDurationMinutes ?: ((uiState.distanceKm / 20f * 60).toInt()),
                    isCurrentLocationActive = uiState.isCurrentLocationActive,
                    isLocatingUser = uiState.isLocatingUser,
                    onUseCurrentLocationClick = { requestLocationAndFetch() },
                    onSwapClick = { viewModel.swapLocations() }
                )
            }

            // Location Inputs (Pickup & Drop)
            item(key = "location_inputs") {
                LocationInputSection(
                    pickup = uiState.pickupText,
                    drop = uiState.dropText,
                    currentCity = uiState.selectedCity,
                    isCurrentLocationActive = uiState.isCurrentLocationActive,
                    isLocating = uiState.isLocatingUser,
                    pickupLat = uiState.pickupLat,
                    pickupLng = uiState.pickupLng,
                    onPickupChange = { viewModel.onPickupChange(it, context) },
                    onDropChange = { viewModel.onDropChange(it, context) },
                    onSwapClick = { viewModel.swapLocations() },
                    onCurrentLocationClick = { requestLocationAndFetch() },
                    onSelectLocation = { preset, isDrop ->
                        viewModel.selectLocation(preset, isDrop)
                    },
                    onSelectPlaceResult = { placeResult, isDrop ->
                        viewModel.selectPlaceResult(placeResult, isDrop)
                    },
                    onSelectCustomText = { text, isDrop ->
                        viewModel.selectCustomLocation(text, isDrop, context)
                    },
                    onSaveRouteClick = { isSaveDialogVisible = true }
                )
            }

            if (uiState.isNoRideAppAvailable) {
                item(key = "no_service_banner") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = com.example.ui.theme.RoseError.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = com.example.ui.theme.RoseError.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = com.example.ui.theme.RoseError,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No Ride Services in ${uiState.unavailableCityName.ifBlank { "This Area" }}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.RoseError
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Ola, Uber, and Rapido are currently not operational in this region. Tap to select a supported city.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            } else if (uiState.dropText.isBlank()) {
                item(key = "enter_destination_prompt") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ElectricBluePrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = ElectricBluePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Enter Destination",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Enter where you want to go to compare live fares across Uber, Ola, and Rapido.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // Trip Distance & Traffic/Weather Surge Toggles
                item(key = "trip_metrics") {
                    TripMetricsSection(
                        distanceKm = uiState.distanceKm,
                        traffic = uiState.trafficCondition,
                        weather = uiState.weatherCondition,
                        onDistanceChange = { viewModel.onDistanceChange(it) },
                        onTrafficChange = { viewModel.onTrafficChange(it) },
                        onWeatherChange = { viewModel.onWeatherChange(it) }
                    )
                }

                // Comparison Calculating Indicator
                if (uiState.isComparing) {
                    item(key = "comparing_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = ElectricBluePrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Checking live rates across Ola, Uber, Rapido...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Hero Highlight: Cheapest Overall Deal
                uiState.cheapestOverall?.let { cheapest ->
                    item(key = "cheapest_hero_deal") {
                        CheapestDealHeroCard(
                            cheapestRide = cheapest,
                            onBookClick = {
                                IntentHelper.bookRide(
                                    context = context,
                                    option = cheapest,
                                    pickup = uiState.pickupText,
                                    drop = uiState.dropText,
                                    isCurrentLocation = uiState.isCurrentLocationActive,
                                    pickupLat = uiState.pickupLat,
                                    pickupLng = uiState.pickupLng,
                                    dropLat = uiState.dropLat,
                                    dropLng = uiState.dropLng
                                )
                            },
                            onBreakdownClick = { viewModel.showBreakdown(cheapest) },
                            onCopyLinkClick = {
                                IntentHelper.copyBookingDetails(
                                    context = context,
                                    option = cheapest,
                                    pickup = uiState.pickupText,
                                    drop = uiState.dropText
                                )
                            },
                            onShareClick = {
                                IntentHelper.shareComparison(
                                    context = context,
                                    pickup = uiState.pickupText,
                                    drop = uiState.dropText,
                                    cheapest = cheapest
                                )
                            }
                        )
                    }
                }

                // Live Provider Driver Arrival ETA & Starting Cost Comparison
                if (uiState.providerComparisons.isNotEmpty()) {
                    item(key = "provider_eta_comparison") {
                        ProviderEtaComparisonBar(
                            comparisons = uiState.providerComparisons,
                            selectedProvider = uiState.selectedProviderFilter,
                            onSelectProvider = { viewModel.onProviderFilterToggle(it) },
                            onRefreshEtas = { viewModel.refreshLiveEtas() }
                        )
                    }
                }

                // Category Tabs and Sort Row
                item(key = "category_and_sort") {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = "COMPARE ALL OPTIONS (${uiState.filteredRideOptions.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                        CategoryAndSortRow(
                            selectedCategory = uiState.selectedCategory,
                            selectedProvider = uiState.selectedProviderFilter,
                            selectedSort = uiState.sortOrder,
                            onCategorySelect = { viewModel.onCategorySelect(it) },
                            onProviderToggle = { viewModel.onProviderFilterToggle(it) },
                            onSortSelect = { viewModel.onSortOrderSelect(it) }
                        )
                    }
                }

                // Empty state if filters exclude all
                if (uiState.filteredRideOptions.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No rides matching current filter",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // List of Ride Options
                items(
                    items = uiState.filteredRideOptions,
                    key = { it.id }
                ) { ride ->
                    RideOptionCard(
                        ride = ride,
                        onBookClick = {
                            IntentHelper.bookRide(
                                context = context,
                                option = ride,
                                pickup = uiState.pickupText,
                                drop = uiState.dropText,
                                isCurrentLocation = uiState.isCurrentLocationActive,
                                pickupLat = uiState.pickupLat,
                                pickupLng = uiState.pickupLng,
                                dropLat = uiState.dropLat,
                                dropLng = uiState.dropLng
                            )
                        },
                        onBreakdownClick = { viewModel.showBreakdown(ride) },
                        onCopyLinkClick = {
                            IntentHelper.copyBookingDetails(
                                context = context,
                                option = ride,
                                pickup = uiState.pickupText,
                                drop = uiState.dropText
                            )
                        }
                    )
                }
            }
        }
    }

    // Modal: Detailed Fare Breakdown
    uiState.selectedRideForBreakdown?.let { ride ->
        FareBreakdownModal(
            rideOption = ride,
            pickup = uiState.pickupText,
            drop = uiState.dropText,
            onDismiss = { viewModel.showBreakdown(null) },
            onBookClick = {
                IntentHelper.bookRide(
                    context = context,
                    option = ride,
                    pickup = uiState.pickupText,
                    drop = uiState.dropText,
                    isCurrentLocation = uiState.isCurrentLocationActive,
                    pickupLat = uiState.pickupLat,
                    pickupLng = uiState.pickupLng,
                    dropLat = uiState.dropLat,
                    dropLng = uiState.dropLng
                )
            }
        )
    }

    // Modal: No Ride App Available in City Alert Dialog
    if (uiState.isNoRideAppAvailable) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissNoRideAppDialog() },
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = com.example.ui.theme.RoseError,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "No Ride Services Available",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (uiState.unavailableMessage.isNotBlank()) {
                            uiState.unavailableMessage
                        } else {
                            "Ola, Uber, and Rapido are currently not operational in ${uiState.unavailableCityName.ifBlank { "this region" }}. Ride-hailing apps do not provide services here."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tip: You can select a nearby metropolitan city to compare rates.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ElectricBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.dismissNoRideAppDialog()
                        viewModel.setCityDialogVisible(true)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ElectricBluePrimary
                    )
                ) {
                    Text("Choose Another City")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.dismissNoRideAppDialog() }
                ) {
                    Text("Got It")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal: City Selector Dialog
    if (uiState.isCityDialogVisible) {
        CitySelectorDialog(
            selectedCity = uiState.selectedCity,
            onCitySelected = { viewModel.selectCity(it) },
            onDismiss = { viewModel.setCityDialogVisible(false) }
        )
    }

    // Modal: Saved Routes & Search History
    if (uiState.isSavedRoutesDialogVisible) {
        SavedRoutesDialog(
            savedRoutes = savedRoutes,
            searchHistory = searchHistory,
            onSelectRoute = { viewModel.applySavedRoute(it) },
            onDeleteRoute = { viewModel.deleteSavedRoute(it) },
            onTogglePriceAlert = { route, enabled, threshold ->
                if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    !com.example.util.PriceAlertNotificationHelper.hasNotificationPermission(context)) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.updatePriceAlert(route.id, enabled, threshold, context)
            },
            onTestPriceAlert = { route ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    !com.example.util.PriceAlertNotificationHelper.hasNotificationPermission(context)) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.testPriceAlert(route, context)
            },
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { viewModel.setSavedRoutesDialogVisible(false) }
        )
    }

    // Modal: Save Route Dialog
    if (isSaveDialogVisible) {
        SaveRouteDialog(
            pickup = uiState.pickupText,
            drop = uiState.dropText,
            onSave = { title, priceAlertEnabled, priceThreshold ->
                if (priceAlertEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    !com.example.util.PriceAlertNotificationHelper.hasNotificationPermission(context)) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.saveCurrentRoute(title, priceAlertEnabled, priceThreshold, context)
                isSaveDialogVisible = false
            },
            onDismiss = { isSaveDialogVisible = false }
        )
    }
}
