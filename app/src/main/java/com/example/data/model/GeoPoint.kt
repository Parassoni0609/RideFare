package com.example.data.model

data class GeoPoint(
    val lat: Double,
    val lng: Double
)

/** Reject missing, non-finite and legacy placeholder coordinates. */
fun validCoordinates(lat: Double?, lng: Double?): Boolean =
    lat != null && lng != null && lat.isFinite() && lng.isFinite() &&
        lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)
