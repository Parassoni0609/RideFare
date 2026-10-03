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
import com.example.ui.components.ProviderPriceCard
import com.example.data.model.RideProvider
import com.example.BuildConfig
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

    fun fetchLocation() { viewModel.fetchCurrentLocation(context) }

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
                onRefreshClick = { viewModel.refreshEstimates() }
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

            item(key = "estimate_and_privacy_notice") {
                Text(
                    text = "Live provider prices are not connected. Check each provider app for the current quote; RideFare does not rank providers using sample rates.\n\n" +
                        (if (BuildConfig.GOOGLE_PLACES_API_KEY.isNotBlank()) "Place search sends queries and search-area coordinates to Google." else "Limited search uses Photon and Android's geocoder.") +
                        " Routing sends endpoint coordinates to OSRM. Favorites are stored on this device. No background price monitoring.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!uiState.hasResolvedEndpoints) {
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
                                    text = "Select Pickup and Destination",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Select a result for both addresses, then open a provider to check its current price.",
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
                items(RideProvider.entries.toList(), key = { it.name }) { provider ->
                    ProviderPriceCard(provider) {
                        IntentHelper.openProvider(context, provider, uiState.pickupText, uiState.dropText,
                            uiState.pickupLat, uiState.pickupLng, uiState.dropLat, uiState.dropLng)
                    }
                }

            }
        }
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
                viewModel.updatePriceAlert(route.id, enabled, threshold)
            },
            onTestPriceAlert = { route -> viewModel.checkPriceTarget(route, context) },
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
                viewModel.saveCurrentRoute(title, priceAlertEnabled, priceThreshold)
                isSaveDialogVisible = false
            },
            onDismiss = { isSaveDialogVisible = false }
        )
    }
}
