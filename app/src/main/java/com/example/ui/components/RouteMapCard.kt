package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CityData
import com.example.data.model.GeoPoint
import com.example.data.repository.RouteRoutingService
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun RouteMapCard(
    pickupName: String,
    dropName: String,
    pickupLat: Double? = null,
    pickupLng: Double? = null,
    dropLat: Double? = null,
    dropLng: Double? = null,
    distanceKm: Float,
    durationMins: Int,
    isCurrentLocationActive: Boolean,
    isLocatingUser: Boolean,
    onUseCurrentLocationClick: () -> Unit,
    onSwapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMapExpanded by remember { mutableStateOf(false) }
    var showStepDetails by remember { mutableStateOf(false) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val hasDrop = dropName.isNotBlank()

    // Pulsing animation for Pickup Pin
    val infiniteTransition = rememberInfiniteTransition(label = "mapPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Animated vehicle progress along the route
    val vehicleProgress by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vehicleProgress"
    )

    // Fallback coordinates from CityData
    val allPresetLocations = remember { CityData.supportedCities.flatMap { it.popularLocations } }
    val effectivePickupLat = pickupLat ?: allPresetLocations.find { it.name.equals(pickupName, true) }?.lat ?: 12.9344
    val effectivePickupLng = pickupLng ?: allPresetLocations.find { it.name.equals(pickupName, true) }?.lng ?: 77.6253
    val effectiveDropLat = if (hasDrop) (dropLat ?: allPresetLocations.find { it.name.equals(dropName, true) }?.lat ?: (effectivePickupLat + 0.08)) else (effectivePickupLat + 0.08)
    val effectiveDropLng = if (hasDrop) (dropLng ?: allPresetLocations.find { it.name.equals(dropName, true) }?.lng ?: (effectivePickupLng + 0.06)) else (effectivePickupLng + 0.06)

    // Fetch the exact road driving path points when drop is present
    var routePoints by remember(effectivePickupLat, effectivePickupLng, effectiveDropLat, effectiveDropLng, hasDrop) {
        mutableStateOf<List<GeoPoint>>(emptyList())
    }

    LaunchedEffect(effectivePickupLat, effectivePickupLng, effectiveDropLat, effectiveDropLng, hasDrop) {
        if (hasDrop) {
            routePoints = RouteRoutingService.getDrivingRoutePoints(
                pLat = effectivePickupLat,
                pLng = effectivePickupLng,
                dLat = effectiveDropLat,
                dLng = effectiveDropLng
            )
        } else {
            routePoints = emptyList()
        }
    }

    // Compass bearing
    val dLatDiff = effectiveDropLat - effectivePickupLat
    val dLngDiff = effectiveDropLng - effectivePickupLng
    val bearing = (Math.toDegrees(atan2(dLngDiff, dLatDiff)) + 360) % 360
    val cardinalDirection = when (bearing) {
        in 22.5..67.5 -> "North-East"
        in 67.5..112.5 -> "East"
        in 112.5..157.5 -> "South-East"
        in 157.5..202.5 -> "South"
        in 202.5..247.5 -> "South-West"
        in 247.5..292.5 -> "West"
        in 292.5..337.5 -> "North-West"
        else -> "North"
    }

    fun openGoogleMapsNavigation() {
        val destinationQuery = if (hasDrop) "$effectiveDropLat,$effectiveDropLng" else "$effectivePickupLat,$effectivePickupLng"
        val isMapsInstalled = com.example.util.IntentHelper.isPackageInstalled(context, "com.google.android.apps.maps")
        if (isMapsInstalled) {
            // Priority 1: Direct navigation intent
            val navUri = Uri.parse("google.navigation:q=$destinationQuery&mode=d")
            val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(navIntent)
                Toast.makeText(context, "Opening Google Maps app...", Toast.LENGTH_SHORT).show()
                return
            } catch (_: Exception) {}

            // Priority 2: Directions intent
            val gmmIntentUri = if (hasDrop) {
                Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$effectivePickupLat,$effectivePickupLng&destination=$effectiveDropLat,$effectiveDropLng&travelmode=driving")
            } else {
                Uri.parse("geo:$effectivePickupLat,$effectivePickupLng?q=$effectivePickupLat,$effectivePickupLng($pickupName)")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(mapIntent)
                Toast.makeText(context, "Opening Google Maps app...", Toast.LENGTH_SHORT).show()
                return
            } catch (_: Exception) {}
        }

        // Fallback geo intent
        val geoUri = if (hasDrop) {
            Uri.parse("geo:$effectiveDropLat,$effectiveDropLng?q=$effectiveDropLat,$effectiveDropLng($dropName)")
        } else {
            Uri.parse("geo:$effectivePickupLat,$effectivePickupLng?q=$effectivePickupLat,$effectivePickupLng($pickupName)")
        }
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val mapActivities = context.packageManager.queryIntentActivities(geoIntent, 0)
        if (mapActivities.isNotEmpty()) {
            try {
                context.startActivity(geoIntent)
                Toast.makeText(context, "Opening Maps app...", Toast.LENGTH_SHORT).show()
                return
            } catch (_: Exception) {}
        }

        // Web browser fallback
        val browserUri = if (hasDrop) {
            Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$effectivePickupLat,$effectivePickupLng&destination=$effectiveDropLat,$effectiveDropLng&travelmode=driving")
        } else {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=$effectivePickupLat,$effectivePickupLng")
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
        Toast.makeText(context, "Opening external map view...", Toast.LENGTH_SHORT).show()
    }

    val mapHeight = if (isMapExpanded) 300.dp else 210.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("route_map_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFE8F0FE),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC2E7FF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Map View",
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasDrop) "Live Driving Route View" else "Live Location View",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF174EA6),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Use My GPS button
                    Surface(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .clickable(onClick = onUseCurrentLocationClick)
                            .border(
                                1.dp,
                                if (isCurrentLocationActive) EmeraldSavings else Slate200,
                                RoundedCornerShape(15.dp)
                            )
                            .testTag("map_use_gps_button"),
                        color = if (isCurrentLocationActive) EmeraldSavings.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLocatingUser) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = EmeraldSavings
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "My Location",
                                    tint = if (isCurrentLocationActive) EmeraldSavings else Slate700,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCurrentLocationActive) "GPS Live" else "My GPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentLocationActive) EmeraldSavings else Slate700,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Swap Pickup & Drop button (Only if drop exists)
                    if (hasDrop) {
                        Surface(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onSwapClick)
                                .border(1.dp, Slate200, CircleShape),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap Locations",
                                    tint = Slate700,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Open in Maps Native External App Button
                    Surface(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .clickable { openGoogleMapsNavigation() }
                            .border(1.dp, Color(0xFF1A73E8), RoundedCornerShape(15.dp)),
                        color = Color(0xFF1A73E8)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in Maps App",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Maps App",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Expand / Collapse map button
                    Surface(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { isMapExpanded = !isMapExpanded }
                            .border(1.dp, Slate200, CircleShape),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMapExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                                contentDescription = "Toggle Map Size",
                                tint = Slate700,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // High-Precision Route Canvas (Instant, Native, Zero GPU Lag)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                panOffsetX = (panOffsetX + dragAmount.x).coerceIn(-140f, 140f)
                                panOffsetY = (panOffsetY + dragAmount.y).coerceIn(-90f, 90f)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Map Canvas Background (Modern urban map styling)
                    drawRect(color = Color(0xFFF1F5F9))

                    // 2. City Grid Network
                    val gridSpacing = 42f
                    var gx = (panOffsetX % gridSpacing)
                    while (gx < w) {
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1f
                        )
                        gx += gridSpacing
                    }
                    var gy = (panOffsetY % gridSpacing)
                    while (gy < h) {
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f
                        )
                        gy += gridSpacing
                    }

                    // 3. Arterial Roads Background Simulation
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, h * 0.35f + panOffsetY * 0.3f),
                        end = Offset(w, h * 0.35f + panOffsetY * 0.3f),
                        strokeWidth = 6f
                    )
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(w * 0.45f + panOffsetX * 0.3f, 0f),
                        end = Offset(w * 0.45f + panOffsetX * 0.3f, h),
                        strokeWidth = 6f
                    )

                    if (hasDrop) {
                        // 4. Coordinates to Screen Projection for Full Route
                        val pts = if (routePoints.isNotEmpty()) routePoints else listOf(
                            GeoPoint(effectivePickupLat, effectivePickupLng),
                            GeoPoint(effectiveDropLat, effectiveDropLng)
                        )

                        val minLat = pts.minOf { it.lat }
                        val maxLat = pts.maxOf { it.lat }
                        val minLng = pts.minOf { it.lng }
                        val maxLng = pts.maxOf { it.lng }

                        val latSpan = max(0.005, maxLat - minLat)
                        val lngSpan = max(0.005, maxLng - minLng)

                        val padX = w * 0.16f
                        val padY = h * 0.18f
                        val usableW = w - padX * 2
                        val usableH = h - padY * 2

                        fun project(p: GeoPoint): Offset {
                            val normX = ((p.lng - minLng) / lngSpan).toFloat()
                            val normY = (1f - ((p.lat - minLat) / latSpan).toFloat()) // Invert Y
                            return Offset(
                                x = padX + normX * usableW + panOffsetX,
                                y = padY + normY * usableH + panOffsetY
                            )
                        }

                        val screenPoints = pts.map { project(it) }

                        if (screenPoints.size >= 2) {
                            val routePath = Path()
                            routePath.moveTo(screenPoints[0].x, screenPoints[0].y)

                            for (i in 1 until screenPoints.size) {
                                val prev = screenPoints[i - 1]
                                val curr = screenPoints[i]
                                val midX = (prev.x + curr.x) / 2
                                val midY = (prev.y + curr.y) / 2
                                routePath.quadraticTo(prev.x, prev.y, midX, midY)
                            }
                            routePath.lineTo(screenPoints.last().x, screenPoints.last().y)

                            // Outer Glow
                            drawPath(
                                path = routePath,
                                color = ElectricBluePrimary.copy(alpha = 0.22f),
                                style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // Road Casing
                            drawPath(
                                path = routePath,
                                color = Color.White,
                                style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // Primary Active Navigation Line
                            drawPath(
                                path = routePath,
                                color = ElectricBluePrimary,
                                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // Traffic Congestion Zone (Middle 35% in amber/orange)
                            if (screenPoints.size >= 6) {
                                val trafficStartIdx = (screenPoints.size * 0.38f).toInt()
                                val trafficEndIdx = (screenPoints.size * 0.65f).toInt()
                                val trafficPath = Path()
                                trafficPath.moveTo(screenPoints[trafficStartIdx].x, screenPoints[trafficStartIdx].y)
                                for (j in (trafficStartIdx + 1)..trafficEndIdx) {
                                    trafficPath.lineTo(screenPoints[j].x, screenPoints[j].y)
                                }
                                drawPath(
                                    path = trafficPath,
                                    color = Color(0xFFFF9800),
                                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                                )
                            }

                            // Live Moving Vehicle Animation along the path
                            val totalSegments = screenPoints.size - 1
                            val scaledIndex = (vehicleProgress * totalSegments).toInt().coerceIn(0, totalSegments - 1)
                            val segmentFrac = (vehicleProgress * totalSegments) - scaledIndex
                            val pA = screenPoints[scaledIndex]
                            val pB = screenPoints[scaledIndex + 1]
                            val vehicleX = pA.x + (pB.x - pA.x) * segmentFrac
                            val vehicleY = pA.y + (pB.y - pA.y) * segmentFrac

                            // Vehicle aura
                            drawCircle(
                                color = ElectricBluePrimary.copy(alpha = 0.25f),
                                radius = 14f,
                                center = Offset(vehicleX, vehicleY)
                            )
                            // Vehicle body
                            drawCircle(
                                color = ElectricBluePrimary,
                                radius = 7f,
                                center = Offset(vehicleX, vehicleY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = Offset(vehicleX, vehicleY)
                            )
                        }

                        // Pickup Pin
                        val startPt = screenPoints.first()
                        drawCircle(
                            color = EmeraldSavings.copy(alpha = pulseAlpha),
                            radius = 18f * pulseScale,
                            center = startPt
                        )
                        drawCircle(
                            color = EmeraldSavings,
                            radius = 9f,
                            center = startPt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = startPt
                        )

                        // Drop Pin (Red Flag / Target Destination)
                        val endPt = screenPoints.last()
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.25f),
                            radius = 8f,
                            center = Offset(endPt.x, endPt.y + 4f)
                        )
                        drawCircle(
                            color = RoseError,
                            radius = 10f,
                            center = endPt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = endPt
                        )
                    } else {
                        // When drop is not yet entered: Center the Pickup location on the map with pulse ring!
                        val centerPt = Offset(w / 2f + panOffsetX, h / 2f + panOffsetY)

                        // Wide radar scan ripple
                        drawCircle(
                            color = EmeraldSavings.copy(alpha = pulseAlpha * 0.5f),
                            radius = 48f * pulseScale,
                            center = centerPt
                        )
                        drawCircle(
                            color = EmeraldSavings.copy(alpha = pulseAlpha),
                            radius = 24f * pulseScale,
                            center = centerPt
                        )
                        drawCircle(
                            color = EmeraldSavings,
                            radius = 11f,
                            center = centerPt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = centerPt
                        )
                    }
                }

                // Pan Reset button if user dragged the map
                if (panOffsetX != 0f || panOffsetY != 0f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            },
                        color = Color.White.copy(alpha = 0.9f),
                        shadowElevation = 3.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Center Map",
                                tint = Slate700,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Compass & Cardinal Direction Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCurrentLocationActive) Icons.Default.MyLocation else Icons.Default.Navigation,
                            contentDescription = null,
                            tint = if (isCurrentLocationActive) EmeraldSavings else Color(0xFF1A73E8),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (hasDrop) "Route $cardinalDirection" else if (isCurrentLocationActive) "GPS Location Active" else "Pickup Set",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                        )
                    }
                }

                // Bottom Floating Route Metrics Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.94f),
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Route Details
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasDrop) Icons.Default.DirectionsCar else Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = if (hasDrop) Color(0xFF1A73E8) else EmeraldSavings,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                if (hasDrop) {
                                    Text(
                                        text = "$distanceKm km • ~$durationMins mins driving",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF202124)
                                        )
                                    )
                                    Text(
                                        text = "${pickupName.take(16)} ➔ ${dropName.take(16)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            color = Slate400
                                        )
                                    )
                                } else {
                                    Text(
                                        text = if (isCurrentLocationActive) "GPS: $pickupName" else pickupName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF202124)
                                        )
                                    )
                                    Text(
                                        text = "Select drop location below to view route",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            color = Color(0xFF1A73E8)
                                        )
                                    )
                                }
                            }
                        }

                        // Toggle Driving Steps details (only when drop is set)
                        if (hasDrop) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showStepDetails = !showStepDetails }
                                    .border(1.dp, Color(0xFF1A73E8).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                color = Color(0xFFE8F0FE)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Directions,
                                        contentDescription = null,
                                        tint = Color(0xFF1A73E8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (showStepDetails) "Hide" else "Steps",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A73E8),
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expandable Step-by-Step Driving Directions
            AnimatedVisibility(visible = showStepDetails && hasDrop) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Turn-by-Turn Route Preview",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A73E8)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DirectionStepItem(
                        icon = Icons.Default.MyLocation,
                        stepNumber = "1",
                        title = "Depart from $pickupName",
                        subtitle = "Head out towards nearest arterial connector"
                    )
                    DirectionStepItem(
                        icon = Icons.Default.DirectionsCar,
                        stepNumber = "2",
                        title = "Merge onto primary city expressway",
                        subtitle = "Fastest route via real-time traffic corridor ($distanceKm km)"
                    )
                    DirectionStepItem(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        stepNumber = "3",
                        title = "Arrive at $dropName",
                        subtitle = "Destination on left/right • ~$durationMins mins total journey"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { openGoogleMapsNavigation() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Live Navigation in Maps App",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectionStepItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    stepNumber: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(22.dp),
            shape = CircleShape,
            color = Color(0xFF1A73E8).copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A73E8),
                        fontSize = 10.sp
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = Slate400
                )
            )
        }
    }
}
