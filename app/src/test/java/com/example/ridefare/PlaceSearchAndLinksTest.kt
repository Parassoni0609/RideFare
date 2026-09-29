package com.example.ridefare

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.data.repository.PlaceSearchService
import com.example.util.FareCalculator
import com.example.util.IntentHelper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlaceSearchAndLinksTest {
    @Test fun `editing pickup retains city focus and restricts search to India`() {
        val url = Uri.parse(PlaceSearchService.photonUrl("MG Road & Metro", null, null, "Bengaluru"))
        assertEquals("MG Road & Metro", url.getQueryParameter("q"))
        assertEquals("IN", url.getQueryParameter("countrycode"))
        assertTrue(url.getQueryParameter("lat")!!.toDouble() in 12.8..13.3)
        assertTrue(url.getQueryParameter("lon")!!.toDouble() in 77.5..77.8)
        val focus = PlaceSearchService.searchFocus(28.6, 77.2, "Bengaluru")!!
        assertEquals(28.6, focus.first, 0.0)
    }

    @Test fun `invalid coordinates cannot override selected city search focus`() {
        val fallback = PlaceSearchService.searchFocus(Double.NaN, 77.2, "Bengaluru")!!
        assertTrue(fallback.first.isFinite())
        assertNull(PlaceSearchService.searchFocus(null, null, "unknown"))
    }

    @Test fun `foreign and malformed results are excluded and metro remains discoverable`() {
        val response = """{"features":[
            {"geometry":{"coordinates":[77.6,12.9]},"properties":{"name":"MG Road Metro Station","countrycode":"IN","osm_value":"station","city":"Bengaluru"}},
            {"geometry":{"coordinates":[-0.1,51.5]},"properties":{"name":"MG Road","countrycode":"GB"}},
            {"geometry":{"coordinates":[77.6,999]},"properties":{"name":"Invalid","countrycode":"IN"}},
            {"geometry":{"coordinates":[77.6,12.9]},"properties":{"name":"MG Road Metro Station","countrycode":"IN","osm_value":"station","city":"Bengaluru"}}
        ]}"""
        val results = PlaceSearchService.parsePhoton(response, null, null)
        assertEquals(1, results.size)
        assertEquals("metro", results.single().category)
        assertEquals("Bengaluru", results.single().subtitle)
    }

    @Test fun `Uber native route preserves both endpoints and special address characters`() {
        val uri = IntentHelper.uberUri("A & B, Gate #2", "केंद्रीय स्टेशन", 12.9, 77.6, 13.0, 77.7, false)
        assertEquals("riderequest", uri.host)
        assertEquals("12.9", uri.getQueryParameter("pickup[latitude]"))
        assertEquals("77.7", uri.getQueryParameter("dropoff[longitude]"))
        assertEquals("A & B, Gate #2", uri.getQueryParameter("pickup[formatted_address]"))
        assertEquals("केंद्रीय स्टेशन", uri.getQueryParameter("dropoff[formatted_address]"))
    }

    @Test fun `Uber browser fallback uses encoded JSON locations with both endpoints`() {
        val uri = IntentHelper.uberUri("A & B", "C #1", 12.9, 77.6, 13.0, 77.7, true)
        assertEquals("/looking", uri.path)
        val pickup = JSONObject(uri.getQueryParameter("pickup")!!)
        val drop = JSONObject(uri.getQueryParameter("drop[0]")!!)
        assertEquals(12.9, pickup.getDouble("latitude"), 0.0)
        assertEquals("C #1", drop.getString("addressLine1"))
        assertEquals(77.7, drop.getDouble("longitude"), 0.0)
    }

    @Test fun `Uber Lite is tried before implicit links and browser fallback`() {
        val attempted = mutableListOf<Intent>()
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun startActivity(intent: Intent) {
                attempted.add(intent)
                if (intent.`package` != "com.ubercab.uberlite") throw ActivityNotFoundException()
            }
        }
        val option = FareCalculator.calculateAllRides("A", "B", 10f,
            TrafficCondition.NORMAL, WeatherOrTimeCondition.REGULAR).first { it.provider == RideProvider.UBER }
        IntentHelper.bookRide(context, option, "A", "B", 12.9, 77.6, 13.0, 77.7)
        assertEquals(listOf("com.ubercab", "com.ubercab.uberlite"), attempted.map { it.`package` })
    }
}
