package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RideProvider
import com.example.data.model.VehicleCategory
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.OlaGreen
import com.example.ui.theme.RapidoYellow
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.UberBlack
import com.example.ui.viewmodel.SortOrder

@Composable
fun CategoryAndSortRow(
    selectedCategory: VehicleCategory,
    selectedProvider: RideProvider?,
    selectedSort: SortOrder,
    onCategorySelect: (VehicleCategory) -> Unit,
    onProviderToggle: (RideProvider?) -> Unit,
    onSortSelect: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Vehicle Category Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VehicleCategory.values().forEach { cat ->
                val isSelected = cat == selectedCategory
                val icon: ImageVector = when (cat) {
                    VehicleCategory.ALL -> Icons.Default.AllInclusive
                    VehicleCategory.BIKE -> Icons.Default.DirectionsBike
                    VehicleCategory.AUTO -> Icons.Default.ElectricRickshaw
                    VehicleCategory.CAB_ECONOMY -> Icons.Default.DirectionsCar
                    VehicleCategory.CAB_PREMIUM -> Icons.Default.LocalTaxi
                    VehicleCategory.CAB_XL -> Icons.Default.DirectionsCar
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCategorySelect(cat) }
                        .border(
                            1.dp,
                            if (isSelected) ElectricBluePrimary else Color.Transparent,
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("category_chip_${cat.name}"),
                    color = if (isSelected) ElectricBluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = if (isSelected) 0.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = cat.title,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) ElectricBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ElectricBluePrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))

        // Sort & App Filter Sub-row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort chip: Lowest Fare / Fastest / Savings
            SortOrder.values().forEach { sort ->
                val isSelected = sort == selectedSort
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSortSelect(sort) }
                        .border(
                            1.dp,
                            if (isSelected) Slate700 else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        ),
                    color = if (isSelected) Slate700 else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = sort.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 18.dp)
                    .background(Slate200)
            )
            Spacer(modifier = Modifier.width(4.dp))

            // App Filter Badges (Uber, Ola, Rapido)
            RideProvider.values().forEach { provider ->
                val isSelected = selectedProvider == provider
                val brandColor = when (provider) {
                    RideProvider.UBER -> UberBlack
                    RideProvider.OLA -> OlaGreen
                    RideProvider.RAPIDO -> Color(0xFFD97706)
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onProviderToggle(provider) }
                        .border(
                            1.dp,
                            if (isSelected) brandColor else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        ),
                    color = if (isSelected) brandColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(brandColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
