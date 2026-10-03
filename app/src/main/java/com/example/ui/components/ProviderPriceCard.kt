package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.RideProvider

@Composable
fun ProviderPriceCard(provider: RideProvider, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("provider_price_${provider.name}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(provider.displayName, style = MaterialTheme.typography.titleMedium)
            Text("Live price unavailable", style = MaterialTheme.typography.titleSmall)
            Text("Check the current price, vehicle and availability in ${provider.displayName}." +
                if (provider == RideProvider.UBER) " Confirm pickup there to see the destination."
                else " Enter pickup and destination there.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onOpen, modifier = Modifier.testTag("open_provider_${provider.name}")) {
                Text("Check in ${provider.displayName}")
            }
        }
    }
}
