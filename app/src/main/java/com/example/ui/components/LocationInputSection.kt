package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CityData
import com.example.data.model.CityInfo
import com.example.data.model.PresetLocation
import com.example.data.repository.PlaceSearchResult
import com.example.data.repository.PlaceSearchService
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import kotlinx.coroutines.delay

enum class ActiveSearchField {
    NONE,
    PICKUP,
    DROP
}

@Composable
fun LocationInputSection(
    pickup: String,
    drop: String,
    currentCity: CityInfo,
    isCurrentLocationActive: Boolean,
    isLocating: Boolean,
    pickupLat: Double? = null,
    pickupLng: Double? = null,
    onPickupChange: (String) -> Unit,
    onDropChange: (String) -> Unit,
    onSwapClick: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    onSelectLocation: (PresetLocation, Boolean) -> Unit,
    onSelectPlaceResult: (PlaceSearchResult, Boolean) -> Unit,
    onSelectCustomText: (String, Boolean) -> Unit,
    onSaveRouteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeField by remember { mutableStateOf(ActiveSearchField.NONE) }
    var selectedCategoryFilter by remember { mutableStateOf("all") }
    var isSearchingLive by remember { mutableStateOf(false) }
    var liveSearchResults by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }

    val activeQuery = when (activeField) {
        ActiveSearchField.PICKUP -> pickup
        ActiveSearchField.DROP -> drop
        ActiveSearchField.NONE -> ""
    }

    // Trigger live Google Maps-like keyword search with 300ms debounce
    LaunchedEffect(activeQuery, activeField, currentCity.name) {
        if (activeField == ActiveSearchField.NONE || activeQuery.length < 2 || activeQuery.startsWith("Current Location")) {
            liveSearchResults = emptyList()
            isSearchingLive = false
            return@LaunchedEffect
        }

        isSearchingLive = true
        delay(300) // Debounce typing
        try {
            val results = PlaceSearchService.searchPlaces(
                context = context,
                query = activeQuery,
                userLat = pickupLat,
                userLng = pickupLng,
                cityNameHint = currentCity.name
            )
            liveSearchResults = results
        } catch (_: Exception) {
            liveSearchResults = emptyList()
        } finally {
            isSearchingLive = false
        }
    }

    // Local presets for fast empty-query discovery
    val presetSuggestions = remember(activeQuery, currentCity.id, activeField) {
        if (activeField == ActiveSearchField.NONE) {
            emptyList()
        } else if (activeQuery.isBlank() || activeQuery.startsWith("Current Location")) {
            currentCity.popularLocations
        } else {
            CityData.searchSuggestions(activeQuery, currentCity.id)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("location_input_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connecting dots column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(EmeraldSavings)
                    )
                    Spacer(
                        modifier = Modifier
                            .width(2.dp)
                            .height(28.dp)
                            .background(Slate200)
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(RoseError)
                    )
                }

                // Input fields
                Column(modifier = Modifier.weight(1f)) {
                    // Pickup input with GPS button & live search
                    LocationField(
                        value = pickup,
                        onValueChange = {
                            onPickupChange(it)
                            activeField = ActiveSearchField.PICKUP
                        },
                        onFocusChanged = { isFocused ->
                            if (isFocused) {
                                activeField = ActiveSearchField.PICKUP
                            }
                        },
                        placeholder = if (isLocating) "Detecting current location via GPS..." else "Search pickup location (Google Maps style)...",
                        label = "PICKUP LOCATION",
                        testTag = "pickup_location_input",
                        trailingContent = {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = {
                                        onCurrentLocationClick()
                                        activeField = ActiveSearchField.NONE
                                    })
                                    .testTag("use_gps_pickup_button"),
                                color = if (isCurrentLocationActive) EmeraldSavings.copy(alpha = 0.15f) else (if (isLocating) ElectricBluePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = "My GPS",
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isCurrentLocationActive) EmeraldSavings else ElectricBluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = when {
                                            isCurrentLocationActive -> "GPS Live"
                                            isLocating -> "Locating..."
                                            else -> "Use GPS"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isCurrentLocationActive -> EmeraldSavings
                                                isLocating -> ElectricBluePrimary
                                                else -> Slate700
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    // Drop input with live search
                    LocationField(
                        value = drop,
                        onValueChange = {
                            onDropChange(it)
                            activeField = ActiveSearchField.DROP
                        },
                        onFocusChanged = { isFocused ->
                            if (isFocused) {
                                activeField = ActiveSearchField.DROP
                            }
                        },
                        placeholder = "Search destination, mall, metro, hospital...",
                        label = "DROP DESTINATION",
                        testTag = "drop_location_input"
                    )
                }

                // Swap & Save Actions
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    IconButton(
                        onClick = onSwapClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("swap_locations_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Swap Locations",
                            tint = ElectricBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    IconButton(
                        onClick = onSaveRouteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("save_favorite_route_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Bookmark Route",
                            tint = EmeraldSavings,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // AUTO-SUGGESTION DROPDOWN (Google Maps-like live search)
            AnimatedVisibility(
                visible = activeField != ActiveSearchField.NONE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val isSearchingPickup = activeField == ActiveSearchField.PICKUP

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(
                            1.dp,
                            if (isSearchingPickup) EmeraldSavings.copy(alpha = 0.35f) else ElectricBluePrimary.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        ),
                    color = Slate100.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSearchingLive) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isSearchingPickup) EmeraldSavings else ElectricBluePrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isSearchingPickup) Icons.Default.MyLocation else Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSearchingPickup) EmeraldSavings else ElectricBluePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSearchingPickup) {
                                        "SEARCHING PICKUP (${currentCity.name}):"
                                    } else {
                                        "SEARCHING DESTINATION (${currentCity.name}):"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSearchingPickup) EmeraldSavings else ElectricBluePrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }

                            IconButton(
                                onClick = { activeField = ActiveSearchField.NONE },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close suggestions",
                                    tint = Slate400,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Quick Category Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val categories = listOf(
                                "all" to "✨ All",
                                "metro" to "🚇 Metro",
                                "airport" to "✈️ Airport",
                                "transit" to "🚆 Stations",
                                "hospital" to "🏥 Hospitals",
                                "work" to "🏢 Tech Parks",
                                "shopping" to "🛍️ Malls"
                            )
                            categories.forEach { (catKey, catLabel) ->
                                val isSelected = selectedCategoryFilter == catKey
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) {
                                        if (isSearchingPickup) EmeraldSavings else ElectricBluePrimary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color.Transparent else Slate200
                                    ),
                                    modifier = Modifier.clickable {
                                        selectedCategoryFilter = if (isSelected && catKey != "all") "all" else catKey
                                    }
                                ) {
                                    Text(
                                        text = catLabel,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Slate700
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // If user is selecting PICKUP: Show 1-tap "Use Current GPS Location"
                        if (isSearchingPickup) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onCurrentLocationClick()
                                        activeField = ActiveSearchField.NONE
                                    },
                                color = EmeraldSavings.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Current GPS Location",
                                        tint = EmeraldSavings,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Use Current GPS Location",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = EmeraldSavings
                                            )
                                        )
                                        Text(
                                            text = "Detects your live location for instant driver pickup",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = Slate700
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 1. LIVE GOOGLE MAPS-STYLE SEARCH RESULTS (Photon + Geocoder)
                        if (liveSearchResults.isNotEmpty()) {
                            val filteredLive = if (selectedCategoryFilter == "all") {
                                liveSearchResults
                            } else {
                                liveSearchResults.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
                            }

                            filteredLive.take(7).forEach { place ->
                                LivePlaceItemRow(
                                    place = place,
                                    keyword = activeQuery,
                                    isPickupMode = isSearchingPickup,
                                    onClick = {
                                        onSelectPlaceResult(place, activeField == ActiveSearchField.DROP)
                                        activeField = ActiveSearchField.NONE
                                    }
                                )
                            }
                        } else if (presetSuggestions.isNotEmpty()) {
                            // 2. Preset suggestions fallback / discovery
                            val filteredPresets = if (selectedCategoryFilter == "all") {
                                presetSuggestions
                            } else {
                                presetSuggestions.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
                            }

                            filteredPresets.take(6).forEach { loc ->
                                SuggestionItemRow(
                                    location = loc,
                                    keyword = activeQuery,
                                    isPickupMode = isSearchingPickup,
                                    onClick = {
                                        onSelectLocation(loc, activeField == ActiveSearchField.DROP)
                                        activeField = ActiveSearchField.NONE
                                    }
                                )
                            }
                        }

                        // Custom query fallback
                        if (activeQuery.isNotBlank() && !activeQuery.startsWith("Current Location")) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectCustomText(activeQuery, activeField == ActiveSearchField.DROP)
                                        activeField = ActiveSearchField.NONE
                                    },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Custom location",
                                        tint = if (isSearchingPickup) EmeraldSavings else ElectricBluePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Search exact: \"$activeQuery\"",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = "Query map coordinates for this text",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = Slate400
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LivePlaceItemRow(
    place: PlaceSearchResult,
    keyword: String,
    isPickupMode: Boolean,
    onClick: () -> Unit
) {
    val icon = when (place.category.lowercase()) {
        "airport" -> Icons.Default.FlightTakeoff
        "transit" -> Icons.Default.Train
        "metro" -> Icons.Default.Subway
        "shopping" -> Icons.Default.LocalMall
        "hospital" -> Icons.Default.LocalHospital
        "work" -> Icons.Default.Work
        else -> Icons.Default.Place
    }

    val iconColor = when (place.category.lowercase()) {
        "airport" -> Color(0xFF6366F1)
        "transit" -> Color(0xFFE11D48)
        "metro" -> Color(0xFF0284C7)
        "shopping" -> Color(0xFFD97706)
        "hospital" -> Color(0xFFDC2626)
        "work" -> Color(0xFF0D9488)
        else -> if (isPickupMode) EmeraldSavings else ElectricBluePrimary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("place_suggestion_${place.title.take(15)}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Highlight matching characters in title
                Text(
                    text = highlightKeyword(place.title, keyword),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )
                if (place.subtitle.isNotBlank()) {
                    Text(
                        text = place.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                }
            }

            if (place.distanceKm != null && place.distanceKm > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate100,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = "${place.distanceKm} km",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionItemRow(
    location: PresetLocation,
    keyword: String,
    isPickupMode: Boolean,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (location.category.lowercase()) {
        "airport" -> Icons.Default.FlightTakeoff
        "transit" -> Icons.Default.Train
        "metro" -> Icons.Default.Subway
        "shopping" -> Icons.Default.LocalMall
        "work" -> Icons.Default.Work
        else -> Icons.Default.Place
    }

    val iconColor = when (location.category.lowercase()) {
        "airport" -> Color(0xFF6366F1)
        "transit" -> Color(0xFFE11D48)
        "metro" -> Color(0xFF0284C7)
        "shopping" -> Color(0xFFD97706)
        "work" -> Color(0xFF0D9488)
        else -> if (isPickupMode) EmeraldSavings else ElectricBluePrimary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("suggestion_${location.name.take(15)}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightKeyword(location.name, keyword),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )
                Text(
                    text = location.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

private fun highlightKeyword(text: String, keyword: String): androidx.compose.ui.text.AnnotatedString {
    if (keyword.isBlank() || !text.contains(keyword, ignoreCase = true)) {
        return buildAnnotatedString { append(text) }
    }

    val startIndex = text.indexOf(keyword, ignoreCase = true)
    val endIndex = startIndex + keyword.length

    return buildAnnotatedString {
        append(text.substring(0, startIndex))
        withStyle(
            style = SpanStyle(
                color = ElectricBluePrimary,
                fontWeight = FontWeight.ExtraBold
            )
        ) {
            append(text.substring(startIndex, endIndex))
        }
        append(text.substring(endIndex))
    }
}

@Composable
private fun LocationField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    placeholder: String,
    label: String,
    testTag: String,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            )
            trailingContent?.invoke()
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onFocusChanged(it.isFocused) }
                    .testTag(testTag),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(ElectricBluePrimary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    fontSize = 13.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear $label",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
