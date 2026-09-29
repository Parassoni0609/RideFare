package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.SavedRoute
import com.example.data.model.RideOption

/** Only explicit manual estimate checks produce notifications. No background monitoring. */
object PriceAlertNotificationHelper {
    const val OPEN_ROUTE_ID = "OPEN_ROUTE_ID"
    private const val CHANNEL_ID = "ride_estimate_checks"

    fun hasNotificationPermission(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    fun sendEstimateCheck(context: Context, route: SavedRoute, option: RideOption): Boolean {
        if (!hasNotificationPermission(context) || route.id <= 0) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Manual estimate checks",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Results of price target checks requested in Favorites"
            })
            if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = android.net.Uri.parse("ridefare://saved-route/${route.id}")
            putExtra(OPEN_ROUTE_ID, route.id)
        }
        val pending = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val meets = option.totalFare <= route.priceThreshold
        val text = "${option.serviceName}: estimated ₹${option.totalFare}, ${if (meets) "within" else "above"} " +
            "your ₹${route.priceThreshold} target. Confirm price and availability in ${option.provider.displayName}."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Estimate check: ${route.title}")
            .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true).setContentIntent(pending).build()
        return try {
            NotificationManagerCompat.from(context).notify("route-${route.id}", 1, notification)
            true
        } catch (_: SecurityException) { false }
    }
}
