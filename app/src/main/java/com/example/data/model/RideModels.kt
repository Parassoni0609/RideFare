package com.example.data.model

enum class RideProvider(
    val displayName: String,
    val packageName: String,
    val websiteUrl: String
) {
    UBER("Uber", "com.ubercab", "https://m.uber.com/looking"),
    OLA("Ola", "com.olacabs.customer", "https://book.olacabs.com"),
    RAPIDO("Rapido", "com.rapido.passenger", "https://www.rapido.bike")
}

enum class VehicleCategory(
    val title: String,
    val subtitle: String,
    val iconName: String
) {
    ALL("All Rides", "Compare everything", "all"),
    BIKE("Bike", "Fast & Solo", "bike"),
    AUTO("Auto", "Affordable 3-wheeler", "auto"),
    CAB_ECONOMY("Economy Cab", "Go & Mini", "cab"),
    CAB_PREMIUM("Prime / Sedan", "Top comfort AC", "sedan"),
    CAB_XL("SUV / XL", "6-Seater spacious", "suv")
}

enum class TrafficCondition(
    val title: String,
    val avgSpeedKmh: Float,
    val fareTimeMultiplier: Float
) {
    LIGHT("Light", 32f, 0.9f),
    NORMAL("Normal", 22f, 1.0f),
    HEAVY("Heavy", 14f, 1.25f),
    JAMMED("Gridlock", 9f, 1.5f)
}

enum class WeatherOrTimeCondition(
    val title: String,
    val surgeMultiplier: Float,
    val description: String
) {
    REGULAR("Standard", 1.0f, "Standard rates apply"),
    MORNING_PEAK("Morning Rush", 1.25f, "Office hours surge (8 - 10:30 AM)"),
    EVENING_PEAK("Evening Rush", 1.35f, "Peak evening traffic (6 - 9:30 PM)"),
    RAIN("Rain / Storm", 1.45f, "High ride demand in rain"),
    LATE_NIGHT("Late Night", 1.20f, "Night driver allowance (11 PM - 5 AM)")
}

data class FareBreakdown(
    val baseFare: Int,
    val distanceFare: Int,
    val timeFare: Int,
    val surgeAmount: Int,
    val surgeMultiplier: Float,
    val platformFee: Int,
    val gstAndTaxes: Int,
    val totalFare: Int
)

data class RideOption(
    val id: String,
    val provider: RideProvider,
    val serviceName: String,
    val category: VehicleCategory,
    val fareBreakdown: FareBreakdown,
    val totalFare: Int,
    val etaMinutes: Int,
    val tripDurationMinutes: Int,
    val isCheapestInOverall: Boolean = false,
    val isCheapestInCategory: Boolean = false,
    val isFastestPickup: Boolean = false,
    val isBestValue: Boolean = false,
    val savingsVsHighest: Int = 0,
    val deepLinkUrl: String,
    val webFallbackUrl: String,
    val capacity: String,
    val featureHighlight: String
)

data class ProviderComparison(
    val provider: RideProvider,
    val nearestEtaMinutes: Int,
    val startingFare: Int,
    val isFastest: Boolean = false,
    val isCheapest: Boolean = false
)

data class PresetLocation(
    val name: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val category: String = "general"
)

data class CityInfo(
    val id: String,
    val name: String,
    val popularLocations: List<PresetLocation>
)
