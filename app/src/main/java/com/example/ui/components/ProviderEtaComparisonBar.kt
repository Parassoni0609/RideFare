package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProviderComparison
import com.example.data.model.RideProvider
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.OlaGreen
import com.example.ui.theme.RapidoYellow
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.UberBlack

@Composable
fun ProviderEtaComparisonBar(
    comparisons: List<ProviderComparison>,
    selectedProvider: RideProvider?,
    onSelectProvider: (RideProvider?) -> Unit,
    onRefreshEtas: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "refreshRotate"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("provider_eta_comparison_bar"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title & Live Driver Status + Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F0FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = ElectricBluePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Pickup & fare estimates",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "Sample rates • availability unverified",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        )
                    }
                }

                // Refresh Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            rotationAngle += 360f
                            onRefreshEtas()
                        }
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recalculate estimates",
                            tint = Slate700,
                            modifier = Modifier
                                .size(13.dp)
                                .rotate(animatedRotation)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Estimate",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Slate700
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Provider Cards: Rapido, Uber, Ola
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comparisons.forEach { comp ->
                    val isSelected = selectedProvider == comp.provider
                    ProviderCard(
                        comp = comp,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) onSelectProvider(null) else onSelectProvider(comp.provider)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    comp: ProviderComparison,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandColor = when (comp.provider) {
        RideProvider.UBER -> UberBlack
        RideProvider.OLA -> OlaGreen
        RideProvider.RAPIDO -> RapidoYellow
    }

    val cardBg = if (isSelected) {
        ElectricBluePrimary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    val borderColor = if (isSelected) {
        ElectricBluePrimary
    } else if (comp.isFastest) {
        Color(0xFFF59E0B) // Amber for fastest pickup
    } else if (comp.isCheapest) {
        EmeraldSavings
    } else {
        Slate200
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp)),
        color = cardBg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Provider Name & Brand Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(brandColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = comp.provider.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Arrival Time (ETA)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (comp.isFastest) Color(0xFFD97706) else Slate700,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${comp.nearestEtaMinutes} min",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (comp.isFastest) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Text(
                text = "estimated pickup",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    color = Slate400
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Starting Price
            Surface(
                color = if (comp.isCheapest) EmeraldSavings.copy(alpha = 0.14f) else Slate200.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "est. ₹${comp.startingFare}",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (comp.isCheapest) EmeraldSavings else Slate700
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Highlight Badge
            when {
                comp.isFastest && comp.isCheapest -> {
                    BadgePill(text = "Est. value", color = Color(0xFF059669))
                }
                comp.isFastest -> {
                    BadgePill(text = "Est. fastest", color = Color(0xFFD97706))
                }
                comp.isCheapest -> {
                    BadgePill(text = "Lowest estimate", color = EmeraldSavings)
                }
                else -> {
                    Text(
                        text = "Availability unverified",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = Slate400
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.sp,
                color = color
            )
        )
    }
}
