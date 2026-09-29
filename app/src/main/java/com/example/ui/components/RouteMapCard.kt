package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.GeoPoint
import com.example.data.model.validCoordinates
import kotlin.math.max

/** Schematic preview of the shared OSRM route. No simulated roads, traffic or vehicles. */
@Composable
fun RouteMapCard(
    pickupName: String, dropName: String,
    pickupLat: Double? = null, pickupLng: Double? = null,
    dropLat: Double? = null, dropLng: Double? = null,
    distanceKm: Float, durationMins: Int,
    isCurrentLocationActive: Boolean, isLocatingUser: Boolean,
    onUseCurrentLocationClick: () -> Unit, onSwapClick: () -> Unit,
    routePoints: List<GeoPoint>, isRouting: Boolean, routeUnavailable: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ready = pickupName.isNotBlank() && dropName.isNotBlank() &&
        validCoordinates(pickupLat, pickupLng) && validCoordinates(dropLat, dropLng)
    Card(modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Route preview", style = MaterialTheme.typography.titleMedium)
            Text(when {
                !ready -> "Select both addresses to view a route."
                isRouting -> "Loading driving route…"
                routeUnavailable -> "Driving route unavailable. Distance is approximate."
                routePoints.isEmpty() -> "Route preview unavailable."
                else -> "Schematic driving route • ${distanceKm} km • estimated ${durationMins} min"
            }, style = MaterialTheme.typography.bodySmall)
            if (isRouting) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (ready && routePoints.size > 1) {
                Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                    val minLat = routePoints.minOf { it.lat }; val minLng = routePoints.minOf { it.lng }
                    val latSpan = max(0.0001, routePoints.maxOf { it.lat } - minLat)
                    val lngSpan = max(0.0001, routePoints.maxOf { it.lng } - minLng)
                    val points = routePoints.map { Offset(
                        (size.width * (0.1 + 0.8 * (it.lng - minLng) / lngSpan)).toFloat(),
                        (size.height * (0.9 - 0.8 * (it.lat - minLat) / latSpan)).toFloat()) }
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, Color(0xFF2563EB), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(Color(0xFF059669), 7.dp.toPx(), points.first())
                    drawCircle(Color(0xFFE11D48), 7.dp.toPx(), points.last())
                }
            }
            Text("Provider coverage and traffic are unverified. Open Maps for actual navigation.",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onUseCurrentLocationClick, enabled = !isLocatingUser) {
                    Text(if (isLocatingUser) "Locating…" else if (isCurrentLocationActive) "Update GPS" else "Use GPS")
                }
                TextButton(onClick = onSwapClick) { Text("Swap") }
                TextButton(enabled = ready, onClick = {
                    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$pickupLat,$pickupLng&destination=$dropLat,$dropLng&travelmode=driving")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                        Toast.makeText(context, "No Maps app or browser is available.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Open Maps") }
            }
        }
    }
}
