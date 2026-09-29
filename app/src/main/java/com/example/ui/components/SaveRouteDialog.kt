package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700

@Composable
fun SaveRouteDialog(
    pickup: String,
    drop: String,
    onSave: (title: String, priceAlertEnabled: Boolean, priceThreshold: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("Route to ${drop.take(15)}") }
    var priceAlertEnabled by remember { mutableStateOf(false) }
    var priceThreshold by remember { mutableIntStateOf(250) }
    val quickThresholds = listOf(150, 200, 250, 300, 400, 500)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save Favorite Route",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Name this route for fast 1-tap rate comparison:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("Route Label") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_route_title_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$pickup ➔ $drop",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Price Alert Toggle Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (priceAlertEnabled) EmeraldSavings.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (priceAlertEnabled) EmeraldSavings.copy(alpha = 0.4f) else Slate200
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (priceAlertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = if (priceAlertEnabled) EmeraldSavings else Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Price Drop Alert",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (priceAlertEnabled) EmeraldSavings else Slate700
                                        )
                                    )
                                    Text(
                                        text = "Notify when fare drops below target",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = Slate400
                                        )
                                    )
                                }
                            }

                            Switch(
                                checked = priceAlertEnabled,
                                onCheckedChange = { priceAlertEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldSavings,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Slate200
                                ),
                                modifier = Modifier.size(width = 38.dp, height = 24.dp)
                            )
                        }

                        AnimatedVisibility(visible = priceAlertEnabled) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "Alert Threshold: ≤ ₹$priceThreshold",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSavings
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    quickThresholds.forEach { price ->
                                        val isSelected = priceThreshold == price
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { priceThreshold = price }
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
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, priceAlertEnabled, priceThreshold) },
                modifier = Modifier.testTag("confirm_save_route_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
