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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.TrafficCondition
import com.example.data.model.WeatherOrTimeCondition
import com.example.ui.theme.AmberSurge
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700

@Composable
fun TripMetricsSection(
    distanceKm: Float,
    traffic: TrafficCondition,
    weather: WeatherOrTimeCondition,
    onDistanceChange: (Float) -> Unit,
    onTrafficChange: (TrafficCondition) -> Unit,
    onWeatherChange: (WeatherOrTimeCondition) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Trip Settings",
                        tint = ElectricBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trip Distance:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$distanceKm km",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ElectricBluePrimary
                        )
                    )
                }

                // Expand/collapse customizer
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isExpanded = !isExpanded }
                        .testTag("toggle_trip_metrics_button"),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tune",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) "Less" else "Surge & Traffic",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Quick stepper row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onDistanceChange((distanceKm - 1f).coerceAtLeast(1.0f)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Minus 1 km",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Slider(
                    value = distanceKm,
                    onValueChange = onDistanceChange,
                    valueRange = 1.0f..55.0f,
                    steps = 54,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBluePrimary,
                        activeTrackColor = ElectricBluePrimary,
                        inactiveTrackColor = Slate200
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("distance_slider")
                )

                IconButton(
                    onClick = { onDistanceChange((distanceKm + 1f).coerceAtMost(55.0f)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Plus 1 km",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expandable Traffic & Surge Simulation Controls
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    // Traffic conditions
                    Text(
                        text = "TRAFFIC LEVEL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrafficCondition.values().forEach { cond ->
                            val isSelected = cond == traffic
                            ConditionPill(
                                label = cond.title,
                                isSelected = isSelected,
                                icon = Icons.Default.Traffic,
                                onClick = { onTrafficChange(cond) },
                                badge = "${cond.avgSpeedKmh.toInt()} km/h"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Weather / Time Surge
                    Text(
                        text = "WEATHER & PEAK SURGE DEMAND",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherOrTimeCondition.values().forEach { w ->
                            val isSelected = w == weather
                            val icon = when (w) {
                                WeatherOrTimeCondition.REGULAR -> Icons.Default.WbSunny
                                WeatherOrTimeCondition.RAIN -> Icons.Default.Cloud
                                WeatherOrTimeCondition.LATE_NIGHT -> Icons.Default.NightsStay
                                else -> Icons.Default.Bolt
                            }
                            val surgeTag = if (w.surgeMultiplier > 1.0f) "${w.surgeMultiplier}x" else "1.0x"
                            ConditionPill(
                                label = w.title,
                                isSelected = isSelected,
                                icon = icon,
                                onClick = { onWeatherChange(w) },
                                badge = surgeTag,
                                isSurge = w.surgeMultiplier > 1.0f
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionPill(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    badge: String,
    isSurge: Boolean = false
) {
    val bgColor = if (isSelected) {
        if (isSurge) AmberSurge.copy(alpha = 0.15f) else ElectricBluePrimary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val borderColor = if (isSelected) {
        if (isSurge) AmberSurge else ElectricBluePrimary
    } else {
        Color.Transparent
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isSelected) {
                    if (isSurge) AmberSurge else ElectricBluePrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = if (isSelected) {
                    if (isSurge) AmberSurge else ElectricBluePrimary
                } else {
                    Slate200
                },
                shape = CircleShape
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Slate700
                    )
                )
            }
        }
    }
}
