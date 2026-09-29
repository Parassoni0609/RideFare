package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import org.json.JSONObject
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

        if (option.provider == RideProvider.UBER) {
            val native = uberUri(pickup, drop, pLat, pLng, dLat, dLng, false)
            // Try each explicit package before any implicit/browser fallback.
            if (tryLaunchUri(context, native.toString(), "com.ubercab") ||
                tryLaunchUri(context, native.toString(), "com.ubercab.uberlite") ||
                tryLaunchUri(context, native.toString())) {
                Toast.makeText(context, "Confirm pickup in Uber to see the destination, then check vehicle and price.", Toast.LENGTH_LONG).show()
                return
            }
            val web = uberUri(pickup, drop, pLat, pLng, dLat, dLng, true)
            if (tryLaunchUri(context, web.toString())) return
        } else {
            // These providers do not have a configured, verified route-prefill integration.
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                try {
                    context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    Toast.makeText(context, "Enter pickup and destination in $providerName. Route transfer is not supported.", Toast.LENGTH_LONG).show()
                    return
                } catch (_: android.content.ActivityNotFoundException) {
                } catch (_: SecurityException) { }
            }
        }
        // A marketing homepage is not a booking fallback. Offer the actual app listing.
        if (tryLaunchUri(context, "market://details?id=$packageName") ||
            tryLaunchUri(context, "https://play.google.com/store/apps/details?id=$packageName")) {
            Toast.makeText(context, "Install or enable $providerName, then retry from RideFare.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "No app or browser can open $providerName. Use Copy details to keep your route.", Toast.LENGTH_LONG).show()
        }
    }

    internal fun uberUri(pickup: String, drop: String, pLat: Double, pLng: Double,
                         dLat: Double, dLng: Double, web: Boolean): Uri {
        require(validCoordinates(pLat, pLng) && validCoordinates(dLat, dLng))
        fun location(name: String, lat: Double, lng: Double) = JSONObject()
            .put("latitude", lat).put("longitude", lng)
            .put("addressLine1", name).put("addressLine2", name).toString()
        if (web) return Uri.parse("https://m.uber.com/looking").buildUpon()
            .appendQueryParameter("pickup", location(pickup, pLat, pLng))
            .appendQueryParameter("drop[0]", location(drop, dLat, dLng)).build()
        return Uri.parse("uber://riderequest").buildUpon()
            .appendQueryParameter("pickup[latitude]", pLat.toString())
            .appendQueryParameter("pickup[longitude]", pLng.toString())
            .appendQueryParameter("pickup[nickname]", pickup)
            .appendQueryParameter("pickup[formatted_address]", pickup)
            .appendQueryParameter("dropoff[latitude]", dLat.toString())
            .appendQueryParameter("dropoff[longitude]", dLng.toString())
            .appendQueryParameter("dropoff[nickname]", drop)
            .appendQueryParameter("dropoff[formatted_address]", drop).build()
    }

    private fun tryLaunchUri(context: Context, uriString: String, packageName: String? = null): Boolean {
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                packageName?.let { setPackage(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: android.content.ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
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
