package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedRoute
import com.example.data.local.SearchHistoryItem
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700

@Composable
fun SavedRoutesDialog(
    savedRoutes: List<SavedRoute>,
    searchHistory: List<SearchHistoryItem>,
    onSelectRoute: (SavedRoute) -> Unit,
    onDeleteRoute: (SavedRoute) -> Unit,
    onTogglePriceAlert: (SavedRoute, Boolean, Int) -> Unit,
    onTestPriceAlert: (SavedRoute) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Saved (${savedRoutes.size})")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recent (${searchHistory.size})")
                        }
                    }
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                if (selectedTab == 0) {
                    if (savedRoutes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No saved routes yet.\nTap the bookmark icon to save your regular commutes and set price drop alerts!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(savedRoutes, key = { it.id }) { route ->
                                SavedRouteCardItem(
                                    route = route,
                                    onSelect = { onSelectRoute(route) },
                                    onDelete = { onDeleteRoute(route) },
                                    onToggleAlert = { enabled, threshold ->
                                        onTogglePriceAlert(route, enabled, threshold)
                                    },
                                    onTestAlert = { onTestPriceAlert(route) }
                                )
                            }
                        }
                    }
                } else {
                    if (searchHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No search history yet.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(searchHistory, key = { it.id }) { item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "${item.pickupName} ➔ ${item.dropName}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Best: ${item.cheapestService} @ ₹${item.cheapestFare}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = EmeraldSavings,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = "${item.distanceKm} km",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        },
        confirmButton = {
            if (selectedTab == 1 && searchHistory.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("Clear History")
                }
            }
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun SavedRouteCardItem(
    route: SavedRoute,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onToggleAlert: (Boolean, Int) -> Unit,
    onTestAlert: () -> Unit
) {
    val quickThresholds = listOf(150, 200, 250, 300, 400, 500)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(
                1.dp,
                if (route.priceAlertEnabled) EmeraldSavings.copy(alpha = 0.4f) else Slate200,
                RoundedCornerShape(14.dp)
            ),
        color = if (route.priceAlertEnabled) EmeraldSavings.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Title, Distance, Delete & Select Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${route.pickupName} ➔ ${route.dropName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "${route.distanceKm} km",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = ElectricBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Use Route button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onSelect),
                        color = ElectricBluePrimary.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = "Use",
                                tint = ElectricBluePrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Load",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBluePrimary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dedicated Price Drop Alert Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (route.priceAlertEnabled) EmeraldSavings.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (route.priceAlertEnabled) EmeraldSavings.copy(alpha = 0.35f) else Slate200
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (route.priceAlertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Price Drop Alert",
                                tint = if (route.priceAlertEnabled) EmeraldSavings else Slate400,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Price Drop Alert",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (route.priceAlertEnabled) EmeraldSavings else Slate700
                                )
                            )
                        }

                        Switch(
                            checked = route.priceAlertEnabled,
                            onCheckedChange = { isChecked ->
                                onToggleAlert(isChecked, route.priceThreshold)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldSavings,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Slate200
                            ),
                            modifier = Modifier.size(width = 38.dp, height = 24.dp)
                        )
                    }

                    // Expanded Threshold Selector when Alert is Active
                    AnimatedVisibility(visible = route.priceAlertEnabled) {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                text = "Notify if lowest fare drops below: ₹${route.priceThreshold}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = EmeraldSavings,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Quick Threshold Selector Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                quickThresholds.forEach { price ->
                                    val isSelected = route.priceThreshold == price
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onToggleAlert(true, price) }
                                            .border(
                                                1.dp,
                                                if (isSelected) EmeraldSavings else Slate200,
                                                RoundedCornerShape(6.dp)
                                            ),
                                        color = if (isSelected) EmeraldSavings else Color.White
                                    ) {
                                        Text(
                                            text = "≤ ₹$price",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = if (isSelected) Color.White else Slate700
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Test Alert notification button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(onClick = onTestAlert)
                                        .border(1.dp, EmeraldSavings.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                                    color = Color.White
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = EmeraldSavings,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Test Notification",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldSavings,
                                                fontSize = 9.sp
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
