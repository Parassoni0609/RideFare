package com.example.ridefare

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SavedRoute
import com.example.data.model.*
import com.example.util.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BookingAndNotificationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val option = FareCalculator.calculateAllRides("A", "B", 10f,
        TrafficCondition.NORMAL, WeatherOrTimeCondition.REGULAR).first()

    @Test fun `unresolved booking never launches a provider with invented coordinates`() {
        IntentHelper.bookRide(context, option, "A", "B", pickupLat = 12.9, pickupLng = 77.6)
        assertNull(shadowOf(context as android.app.Application).nextStartedActivity)
    }

    @Test fun `manual checks use distinct route notifications and restore route ids`() {
        val one = SavedRoute(id = 1, title = "One", pickupName = "A", dropName = "B", distanceKm = 10f, priceThreshold = 1)
        val two = one.copy(id = 2, title = "Two")
        assertTrue(PriceAlertNotificationHelper.sendEstimateCheck(context, one, option))
        assertTrue(PriceAlertNotificationHelper.sendEstimateCheck(context, two, option))
        val notifications = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).activeNotifications
        assertEquals(2, notifications.size)
        notifications.forEach { item ->
            val notification = item.notification
            assertTrue(notification.extras.getCharSequence("android.text").toString().contains("above"))
            val intent = shadowOf(notification.contentIntent).savedIntent
            val id = intent.getLongExtra(PriceAlertNotificationHelper.OPEN_ROUTE_ID, -1)
            assertEquals("route-$id", item.tag)
            assertEquals("ridefare://saved-route/$id", intent.data.toString())
        }
    }
}
