package com.example.util

import com.example.data.model.FareBreakdown
import com.example.data.model.RideOption
import com.example.data.model.RideProvider
import com.example.data.model.TrafficCondition
import com.example.data.model.VehicleCategory
import com.example.data.model.WeatherOrTimeCondition
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.roundToInt

object FareCalculator {

    private fun encode(text: String): String {
        return try {
            URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            text.replace(" ", "%20")
        }
    }

    fun calculateAllRides(
        pickup: String,
        drop: String,
        distanceKm: Float,
        traffic: TrafficCondition,
        weather: WeatherOrTimeCondition,
        availableProviders: List<RideProvider> = listOf(RideProvider.OLA, RideProvider.UBER, RideProvider.RAPIDO)
    ): List<RideOption> {
        val dist = max(0.5f, distanceKm)
        val durationMins = max(3, (dist / traffic.avgSpeedKmh * 60).roundToInt())

        val rawOptions = mutableListOf<RideOption>()

        // 1. RAPIDO BIKE
        rawOptions.add(
            createOption(
                id = "rapido_bike",
                provider = RideProvider.RAPIDO,
                serviceName = "Rapido Bike",
                category = VehicleCategory.BIKE,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = (durationMins * 0.75f).roundToInt(), // Bikes beat traffic!
                baseFare = 20,
                baseDistance = 1.0f,
                perKmRate = 8.8f,
                perMinRate = 1.0f,
                platformFee = 3,
                providerSurgeBonus = 1.0f,
                weatherSurge = if (weather == WeatherOrTimeCondition.RAIN) 1.6f else weather.surgeMultiplier,
                etaMinutes = 2,
                capacity = "1 Person",
                featureHighlight = "Beats city traffic • Helmet provided"
            )
        )

        // 2. UBER MOTO
        rawOptions.add(
            createOption(
                id = "uber_moto",
                provider = RideProvider.UBER,
                serviceName = "Uber Moto",
                category = VehicleCategory.BIKE,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = (durationMins * 0.78f).roundToInt(),
                baseFare = 24,
                baseDistance = 1.5f,
                perKmRate = 9.5f,
                perMinRate = 1.1f,
                platformFee = 4,
                providerSurgeBonus = 1.05f,
                weatherSurge = if (weather == WeatherOrTimeCondition.RAIN) 1.55f else weather.surgeMultiplier,
                etaMinutes = 3,
                capacity = "1 Person",
                featureHighlight = "Affordable two-wheeler ride"
            )
        )

        // 3. OLA BIKE
        rawOptions.add(
            createOption(
                id = "ola_bike",
                provider = RideProvider.OLA,
                serviceName = "Ola Bike",
                category = VehicleCategory.BIKE,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = (durationMins * 0.80f).roundToInt(),
                baseFare = 23,
                baseDistance = 1.2f,
                perKmRate = 9.8f,
                perMinRate = 1.05f,
                platformFee = 4,
                providerSurgeBonus = 1.02f,
                weatherSurge = if (weather == WeatherOrTimeCondition.RAIN) 1.58f else weather.surgeMultiplier,
                etaMinutes = 4,
                capacity = "1 Person",
                featureHighlight = "Quick daily commute"
            )
        )

        // 4. RAPIDO AUTO
        rawOptions.add(
            createOption(
                id = "rapido_auto",
                provider = RideProvider.RAPIDO,
                serviceName = "Rapido Auto",
                category = VehicleCategory.AUTO,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = (durationMins * 0.92f).roundToInt(),
                baseFare = 30,
                baseDistance = 1.8f,
                perKmRate = 13.8f,
                perMinRate = 1.2f,
                platformFee = 5,
                providerSurgeBonus = 1.0f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 3,
                capacity = "3 Persons",
                featureHighlight = "Meter-free guaranteed rates"
            )
        )

        // 5. UBER AUTO
        rawOptions.add(
            createOption(
                id = "uber_auto",
                provider = RideProvider.UBER,
                serviceName = "Uber Auto",
                category = VehicleCategory.AUTO,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = (durationMins * 0.95f).roundToInt(),
                baseFare = 32,
                baseDistance = 2.0f,
                perKmRate = 14.5f,
                perMinRate = 1.25f,
                platformFee = 6,
                providerSurgeBonus = 1.04f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 4,
                capacity = "3 Persons",
                featureHighlight = "Doorstep auto pickup • Cashless"
            )
        )

        // 6. OLA AUTO
        rawOptions.add(
            createOption(
                id = "ola_auto",
                provider = RideProvider.OLA,
                serviceName = "Ola Auto",
                category = VehicleCategory.AUTO,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = (durationMins * 0.95f).roundToInt(),
                baseFare = 31,
                baseDistance = 1.9f,
                perKmRate = 14.2f,
                perMinRate = 1.22f,
                platformFee = 5,
                providerSurgeBonus = 1.02f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 3,
                capacity = "3 Persons",
                featureHighlight = "Reliable auto with verified drivers"
            )
        )

        // 7. UBER GO (ECONOMY CAB)
        rawOptions.add(
            createOption(
                id = "uber_go",
                provider = RideProvider.UBER,
                serviceName = "Uber Go",
                category = VehicleCategory.CAB_ECONOMY,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = durationMins,
                baseFare = 55,
                baseDistance = 2.0f,
                perKmRate = 14.2f,
                perMinRate = 1.6f,
                platformFee = 18,
                providerSurgeBonus = 1.03f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 3,
                capacity = "4 Persons",
                featureHighlight = "Affordable, compact AC hatchback"
            )
        )

        // 8. OLA MINI (ECONOMY CAB)
        rawOptions.add(
            createOption(
                id = "ola_mini",
                provider = RideProvider.OLA,
                serviceName = "Ola Mini",
                category = VehicleCategory.CAB_ECONOMY,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = durationMins,
                baseFare = 58,
                baseDistance = 2.0f,
                perKmRate = 14.6f,
                perMinRate = 1.65f,
                platformFee = 19,
                providerSurgeBonus = 1.05f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 4,
                capacity = "4 Persons",
                featureHighlight = "Comfortable everyday city hatchback"
            )
        )

        // 9. RAPIDO CAB ECONOMY
        rawOptions.add(
            createOption(
                id = "rapido_cab_economy",
                provider = RideProvider.RAPIDO,
                serviceName = "Rapido Cab",
                category = VehicleCategory.CAB_ECONOMY,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = durationMins,
                baseFare = 52,
                baseDistance = 2.0f,
                perKmRate = 13.9f,
                perMinRate = 1.55f,
                platformFee = 15,
                providerSurgeBonus = 1.0f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 5,
                capacity = "4 Persons",
                featureHighlight = "Lowest commission cab guarantee"
            )
        )

        // 10. UBER PREMIER (SEDAN)
        rawOptions.add(
            createOption(
                id = "uber_premier",
                provider = RideProvider.UBER,
                serviceName = "Uber Premier",
                category = VehicleCategory.CAB_PREMIUM,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = durationMins,
                baseFare = 85,
                baseDistance = 3.0f,
                perKmRate = 18.5f,
                perMinRate = 2.0f,
                platformFee = 25,
                providerSurgeBonus = 1.02f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 5,
                capacity = "4 Persons",
                featureHighlight = "Premium sedans with top rated drivers"
            )
        )

        // 11. OLA PRIME SEDAN
        rawOptions.add(
            createOption(
                id = "ola_prime_sedan",
                provider = RideProvider.OLA,
                serviceName = "Ola Prime Sedan",
                category = VehicleCategory.CAB_PREMIUM,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = durationMins,
                baseFare = 88,
                baseDistance = 3.0f,
                perKmRate = 18.8f,
                perMinRate = 2.1f,
                platformFee = 25,
                providerSurgeBonus = 1.04f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 6,
                capacity = "4 Persons",
                featureHighlight = "Extra legroom, free in-cab WiFi"
            )
        )

        // 12. UBER XL (SUV 6-SEATER)
        rawOptions.add(
            createOption(
                id = "uber_xl",
                provider = RideProvider.UBER,
                serviceName = "Uber XL",
                category = VehicleCategory.CAB_XL,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = durationMins,
                baseFare = 125,
                baseDistance = 3.0f,
                perKmRate = 23.0f,
                perMinRate = 2.4f,
                platformFee = 30,
                providerSurgeBonus = 1.05f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 7,
                capacity = "6 Persons",
                featureHighlight = "Spacious 6-seater SUV for luggage"
            )
        )

        // 13. OLA PRIME SUV
        rawOptions.add(
            createOption(
                id = "ola_prime_suv",
                provider = RideProvider.OLA,
                serviceName = "Ola Prime SUV",
                category = VehicleCategory.CAB_XL,
                pickup = pickup,
                drop = drop,
                distanceKm = dist,
                durationMinutes = durationMins,
                baseFare = 130,
                baseDistance = 3.0f,
                perKmRate = 23.5f,
                perMinRate = 2.5f,
                platformFee = 32,
                providerSurgeBonus = 1.06f,
                weatherSurge = weather.surgeMultiplier,
                etaMinutes = 8,
                capacity = "6 Persons",
                featureHighlight = "Ertiga/Innova with ample luggage room"
            )
        )

        val filtered = rawOptions.filter { it.provider in availableProviders }
        if (filtered.isEmpty()) return emptyList()

        // Calculate rankings, cheapest, fastest pickup, and best value
        val minOverallFare = filtered.minOfOrNull { it.totalFare } ?: 0
        val maxOverallFare = filtered.maxOfOrNull { it.totalFare } ?: 0
        val minOverallEta = filtered.minOfOrNull { it.etaMinutes } ?: 0

        // Find cheapest per category
        val cheapestPerCat = filtered.groupBy { it.category }
            .mapValues { entry -> entry.value.minOf { it.totalFare } }

        return filtered.map { option ->
            val isCheapestOverall = option.totalFare == minOverallFare
            val isCheapestCat = option.totalFare == cheapestPerCat[option.category]
            val isFastest = option.etaMinutes == minOverallEta
            val isBestValue = (isCheapestOverall && isFastest) ||
                    (option.totalFare <= minOverallFare * 1.18 && option.etaMinutes <= minOverallEta + 1)
            val savings = max(0, maxOverallFare - option.totalFare)

            option.copy(
                isCheapestInOverall = isCheapestOverall,
                isCheapestInCategory = isCheapestCat,
                isFastestPickup = isFastest,
                isBestValue = isBestValue,
                savingsVsHighest = savings
            )
        }
    }

    fun calculateProviderComparisons(rides: List<RideOption>): List<com.example.data.model.ProviderComparison> {
        if (rides.isEmpty()) return emptyList()

        val providers = listOf(RideProvider.RAPIDO, RideProvider.UBER, RideProvider.OLA)
        val byProvider = rides.groupBy { it.provider }

        val rawComparisons = providers.mapNotNull { p ->
            val providerRides = byProvider[p] ?: return@mapNotNull null
            val nearestEta = providerRides.minOf { it.etaMinutes }
            val lowestFare = providerRides.minOf { it.totalFare }
            com.example.data.model.ProviderComparison(
                provider = p,
                nearestEtaMinutes = nearestEta,
                startingFare = lowestFare,
                activeFleetCount = when (p) {
                    RideProvider.RAPIDO -> 18
                    RideProvider.UBER -> 24
                    RideProvider.OLA -> 20
                }
            )
        }

        if (rawComparisons.isEmpty()) return emptyList()

        val minEta = rawComparisons.minOf { it.nearestEtaMinutes }
        val minFare = rawComparisons.minOf { it.startingFare }

        return rawComparisons.map { comp ->
            comp.copy(
                isFastest = comp.nearestEtaMinutes == minEta,
                isCheapest = comp.startingFare == minFare
            )
        }
    }

    private fun createOption(
        id: String,
        provider: RideProvider,
        serviceName: String,
        category: VehicleCategory,
        pickup: String,
        drop: String,
        distanceKm: Float,
        durationMinutes: Int,
        baseFare: Int,
        baseDistance: Float,
        perKmRate: Float,
        perMinRate: Float,
        platformFee: Int,
        providerSurgeBonus: Float,
        weatherSurge: Float,
        etaMinutes: Int,
        traffic: TrafficCondition = TrafficCondition.NORMAL,
        weather: WeatherOrTimeCondition = WeatherOrTimeCondition.REGULAR,
        capacity: String,
        featureHighlight: String
    ): RideOption {
        val trafficEtaOffset = when (traffic) {
            TrafficCondition.LIGHT -> -1
            TrafficCondition.NORMAL -> 0
            TrafficCondition.HEAVY -> 2
            TrafficCondition.JAMMED -> 4
        }
        val weatherEtaOffset = when (weather) {
            WeatherOrTimeCondition.RAIN -> 2
            WeatherOrTimeCondition.MORNING_PEAK, WeatherOrTimeCondition.EVENING_PEAK -> 1
            else -> 0
        }
        val dynamicEta = max(1, etaMinutes + trafficEtaOffset + weatherEtaOffset)

        val chargeableDist = max(0f, distanceKm - baseDistance)
        val distFare = (chargeableDist * perKmRate).roundToInt()
        val timeFare = (durationMinutes * perMinRate).roundToInt()

        val effectiveSurgeMultiplier = weatherSurge * providerSurgeBonus
        val subtotal = baseFare + distFare + timeFare
        val surgeAmount = if (effectiveSurgeMultiplier > 1.02f) {
            ((effectiveSurgeMultiplier - 1.0f) * subtotal).roundToInt()
        } else {
            0
        }

        val taxableAmount = subtotal + surgeAmount + platformFee
        val gst = (taxableAmount * 0.05f).roundToInt() // 5% GST on transport
        val totalFare = taxableAmount + gst

        val breakdown = FareBreakdown(
            baseFare = baseFare,
            distanceFare = distFare,
            timeFare = timeFare,
            surgeAmount = surgeAmount,
            surgeMultiplier = effectiveSurgeMultiplier,
            platformFee = platformFee,
            gstAndTaxes = gst,
            totalFare = totalFare
        )

        // Construct high fidelity Deep links & web fallbacks
        val encodedPickup = encode(pickup)
        val encodedDrop = encode(drop)

        val (deepLink, webLink) = when (provider) {
            RideProvider.UBER -> {
                val dl = "uber://?action=setPickup&pickup=my_location&dropoff[formatted_address]=$encodedDrop"
                val wl = "https://m.uber.com/ul/?action=setPickup&pickup=my_location&dropoff[formatted_address]=$encodedDrop"
                dl to wl
            }
            RideProvider.OLA -> {
                val dl = "ola://ola/rides?action=request&drop_name=$encodedDrop&pickup_name=$encodedPickup"
                val wl = "https://book.olacabs.com/?pickup_name=$encodedPickup&drop_name=$encodedDrop"
                dl to wl
            }
            RideProvider.RAPIDO -> {
                val dl = "rapido://booking?pickup=$encodedPickup&drop=$encodedDrop"
                val wl = "https://www.rapido.bike"
                dl to wl
            }
        }

        return RideOption(
            id = id,
            provider = provider,
            serviceName = serviceName,
            category = category,
            fareBreakdown = breakdown,
            totalFare = totalFare,
            etaMinutes = dynamicEta,
            tripDurationMinutes = durationMinutes,
            deepLinkUrl = deepLink,
            webFallbackUrl = webLink,
            capacity = capacity,
            featureHighlight = featureHighlight
        )
    }
}
