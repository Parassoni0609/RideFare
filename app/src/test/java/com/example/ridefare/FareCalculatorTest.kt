package com.example.ridefare

import com.example.data.model.*
import com.example.util.FareCalculator
import org.junit.Assert.*
import org.junit.Test

class FareCalculatorTest {
    private fun rides(traffic: TrafficCondition, weather: WeatherOrTimeCondition) =
        FareCalculator.calculateAllRides("Pickup", "Drop", 12f, traffic, weather)

    @Test fun `traffic and weather affect every pickup estimate`() {
        val normal = rides(TrafficCondition.NORMAL, WeatherOrTimeCondition.REGULAR)
        val heavyRain = rides(TrafficCondition.HEAVY, WeatherOrTimeCondition.RAIN).associateBy { it.id }
        val light = rides(TrafficCondition.LIGHT, WeatherOrTimeCondition.REGULAR).associateBy { it.id }
        assertEquals(13, normal.size)
        normal.forEach {
            assertEquals("${it.id} must apply both offsets", it.etaMinutes + 4, heavyRain.getValue(it.id).etaMinutes)
            assertEquals((it.etaMinutes - 1).coerceAtLeast(1), light.getValue(it.id).etaMinutes)
            assertTrue(heavyRain.getValue(it.id).totalFare > it.totalFare)
        }
    }

    @Test fun `breakdown sums to total and ranking follows estimates`() {
        val options = rides(TrafficCondition.JAMMED, WeatherOrTimeCondition.EVENING_PEAK)
        options.forEach {
            val b = it.fareBreakdown
            assertEquals(b.baseFare + b.distanceFare + b.timeFare + b.surgeAmount + b.platformFee + b.gstAndTaxes, it.totalFare)
            assertEquals(it.totalFare == options.minOf { r -> r.totalFare }, it.isCheapestInOverall)
        }
        val uber = FareCalculator.calculateAllRides("A", "B", 8f, TrafficCondition.NORMAL,
            WeatherOrTimeCondition.REGULAR, listOf(RideProvider.UBER))
        assertTrue(uber.all { it.provider == RideProvider.UBER })
    }

    @Test fun `unknown cities do not create coordinates or claim coverage`() {
        assertTrue(CityData.getCityByNameOrId("Unknown Test Town").popularLocations.isEmpty())
        assertFalse(validCoordinates(0.0, 0.0))
        assertFalse(validCoordinates(Double.NaN, 77.0))
        assertFalse(validCoordinates(91.0, 77.0))
        assertFalse(validCoordinates(null, 77.0))
        assertTrue(validCoordinates(0.0, 77.0))
    }
}
