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

object PriceAlertNotificationHelper {

    private const val CHANNEL_ID = "ride_price_drop_alerts"
    private const val CHANNEL_NAME = "Ride Price Drop Alerts"
    private const val CHANNEL_DESC = "Notifications when fares for saved routes drop below your target price"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendPriceDropNotification(
        context: Context,
        route: SavedRoute,
        lowestPrice: Int,
        providerName: String,
        serviceName: String
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_ROUTE_ID", route.id)
            putExtra("PICKUP_NAME", route.pickupName)
            putExtra("DROP_NAME", route.dropName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            route.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val savings = route.priceThreshold - lowestPrice

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📉 Price Drop: ₹$lowestPrice for ${route.title}")
            .setContentText("${route.title}: $serviceName on $providerName is now ₹$lowestPrice (₹$savings below your ₹${route.priceThreshold} alert threshold)!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Great deal on ${route.title}!\n" +
                                "Route: ${route.pickupName} ➔ ${route.dropName}\n" +
                                "Current Best Fare: ₹$lowestPrice ($serviceName on $providerName)\n" +
                                "Target Threshold: ₹${route.priceThreshold} (You save ₹$savings!)"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROMO)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                route.id.toInt().coerceAtLeast(100),
                notification
            )
        } catch (_: SecurityException) {
            // Handled safely
        }
    }

    fun sendTestPriceAlert(context: Context, route: SavedRoute) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (route.id + 999).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 Price Drop Alert Active: ${route.title}")
            .setContentText("We will notify you immediately whenever fares drop below ₹${route.priceThreshold} for ${route.title}!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Price monitoring is active for ${route.title}!\n" +
                                "Route: ${route.pickupName} ➔ ${route.dropName}\n" +
                                "Target Alert Threshold: Fares ≤ ₹${route.priceThreshold}\n" +
                                "You will receive a notification as soon as Uber, Ola, or Rapido fares drop below this price."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (route.id + 999).toInt(),
                notification
            )
        } catch (_: SecurityException) {
            // Handled safely
        }
    }
}
