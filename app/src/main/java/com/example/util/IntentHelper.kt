package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.example.data.model.validCoordinates
import com.example.data.model.RideOption
import com.example.data.model.RideProvider

object IntentHelper {

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            try {
                context.packageManager.getLaunchIntentForPackage(packageName) != null
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Offers an external provider handoff, never a confirmed booking.
     * Uber receives resolved coordinates; Ola/Rapido require route entry in their app.
     */
    fun bookRide(
        context: Context,
        option: RideOption,
        pickup: String,
        drop: String,
        pickupLat: Double? = null,
        pickupLng: Double? = null,
        dropLat: Double? = null,
        dropLng: Double? = null
    ) {
        val providerName = option.provider.displayName
        val packageName = option.provider.packageName

        if (!validCoordinates(pickupLat, pickupLng) || !validCoordinates(dropLat, dropLng) ||
            pickup.isBlank() || drop.isBlank()) {
            Toast.makeText(context, "Select valid pickup and destination addresses first.", Toast.LENGTH_LONG).show()
            return
        }
        val pLat = pickupLat!!
        val pLng = pickupLng!!
        val dLat = dropLat!!
        val dLng = dropLng!!

        val encPickup = Uri.encode(pickup.ifBlank { "Current Location" })
        val encDrop = Uri.encode(drop.ifBlank { "Destination" })

        val isUber = option.provider == RideProvider.UBER
        val isOla = option.provider == RideProvider.OLA
        val isRapido = option.provider == RideProvider.RAPIDO

        // 1. UBER DEEP LINKING
        if (isUber) {
            val uberSchemeUri = "uber://?action=setPickup&pickup[latitude]=$pLat&pickup[longitude]=$pLng&pickup[nickname]=$encPickup&dropoff[latitude]=$dLat&dropoff[longitude]=$dLng&dropoff[nickname]=$encDrop&dropoff[formatted_address]=$encDrop"

            if (tryLaunchUri(context, uberSchemeUri, "com.ubercab") ||
                tryLaunchUri(context, uberSchemeUri, "com.ubercab.uberlite") ||
                tryLaunchUri(context, uberSchemeUri, null)
            ) {
                Toast.makeText(context, "Opening Uber. Confirm addresses, vehicle and price.", Toast.LENGTH_SHORT).show()
                return
            }

            // Universal App Link
            val uberWebAppLink = "https://m.uber.com/ul/?action=setPickup&pickup[latitude]=$pLat&pickup[longitude]=$pLng&pickup[formatted_address]=$encPickup&dropoff[latitude]=$dLat&dropoff[longitude]=$dLng&dropoff[formatted_address]=$encDrop"
            if (tryLaunchUri(context, uberWebAppLink, null)) {
                Toast.makeText(context, "Opening Uber...", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Ola/Rapido destination URI formats are unverified. Open their app and
        // let the user enter/confirm the route rather than claiming it was set.
        if (isOla || isRapido) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                try {
                    context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    Toast.makeText(context, "Opening $providerName. Enter your route and confirm price and vehicle in the app.", Toast.LENGTH_LONG).show()
                    return
                } catch (_: Exception) { }
            }
        }

        // 4. General Web/Store Fallback
        val fallbackUrl = when {
            isUber -> "https://m.uber.com/ul/?dropoff[latitude]=$dLat&dropoff[longitude]=$dLng&dropoff[formatted_address]=$encDrop"
            isOla -> option.provider.websiteUrl
            isRapido -> option.provider.websiteUrl
            else -> option.webFallbackUrl
        }

        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            Toast.makeText(context, "Opening in $providerName...", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            try {
                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(storeIntent)
            } catch (_: Exception) {
                val playStoreWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(playStoreWebIntent) } catch (_: Exception) {
                    Toast.makeText(context, "No app or browser is available to open this provider.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun tryLaunchUri(context: Context, uriString: String, packageName: String? = null): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                if (!packageName.isNullOrBlank()) {
                    setPackage(packageName)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            if (!packageName.isNullOrBlank()) {
                try {
                    val implicitIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(implicitIntent)
                    return true
                } catch (_: Exception) {}
            }
            false
        }
    }

    fun copyBookingDetails(context: Context, option: RideOption, pickup: String, drop: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = """
            🚖 Ride Comparison via RideFare:
            Route: $pickup ➔ $drop
            Service: ${option.serviceName} (${option.provider.displayName})
            Estimated Fare: ₹${option.totalFare}
            Estimated pickup: ~${option.etaMinutes} mins | Trip: ~${option.tripDurationMinutes} mins
            Provider link: ${option.webFallbackUrl}
            Confirm price, addresses, category and availability in the provider app.
        """.trimIndent()

        val clip = ClipData.newPlainText("Ride Booking Link", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied ride details to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareComparison(context: Context, pickup: String, drop: String, cheapest: RideOption) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                "RideFare estimated comparison"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Estimated comparison from $pickup to $drop.\n" +
                        "${cheapest.serviceName}: estimated ₹${cheapest.totalFare}. Difference vs highest estimate: ₹${cheapest.savingsVsHighest}.\n" +
                        "Confirm actual price and availability in the provider app: ${cheapest.webFallbackUrl}"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Ride Deal"))
    }
}
