package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.example.data.model.CityData
import com.example.data.model.RideOption
import com.example.data.model.RideProvider
import com.example.data.model.VehicleCategory

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
     * Launches Ola, Uber, or Rapido with pre-filled destination coordinates,
     * pickup coordinates, and location labels via deep link URL intents.
     */
    fun bookRide(
        context: Context,
        option: RideOption,
        pickup: String,
        drop: String,
        isCurrentLocation: Boolean = false,
        pickupLat: Double? = null,
        pickupLng: Double? = null,
        dropLat: Double? = null,
        dropLng: Double? = null
    ) {
        val providerName = option.provider.displayName
        val packageName = option.provider.packageName

        val pLat = pickupLat ?: 12.9344
        val pLng = pickupLng ?: 77.6253
        val dLat = dropLat ?: (pLat + 0.05)
        val dLng = dropLng ?: (pLng + 0.05)

        val encPickup = Uri.encode(pickup.ifBlank { "Current Location" })
        val encDrop = Uri.encode(drop.ifBlank { "Destination" })

        val isUber = option.provider == RideProvider.UBER
        val isOla = option.provider == RideProvider.OLA
        val isRapido = option.provider == RideProvider.RAPIDO

        // 1. UBER DEEP LINKING
        if (isUber) {
            val uberSchemeUri = if (isCurrentLocation || pickup.contains("Current Location", ignoreCase = true)) {
                "uber://?action=setPickup&pickup=my_location&dropoff[latitude]=$dLat&dropoff[longitude]=$dLng&dropoff[nickname]=$encDrop&dropoff[formatted_address]=$encDrop"
            } else {
                "uber://?action=setPickup&pickup[latitude]=$pLat&pickup[longitude]=$pLng&pickup[nickname]=$encPickup&dropoff[latitude]=$dLat&dropoff[longitude]=$dLng&dropoff[nickname]=$encDrop&dropoff[formatted_address]=$encDrop"
            }

            if (tryLaunchUri(context, uberSchemeUri, "com.ubercab") ||
                tryLaunchUri(context, uberSchemeUri, "com.ubercab.uberlite") ||
                tryLaunchUri(context, uberSchemeUri, null)
            ) {
                Toast.makeText(context, "Opening Uber with destination set...", Toast.LENGTH_SHORT).show()
                return
            }

            // Universal App Link
            val uberWebAppLink = "https://m.uber.com/ul/?action=setPickup&client_id=ridefare&pickup[latitude]=$pLat&pickup[longitude]=$pLng&pickup[formatted_address]=$encPickup&dropoff[latitude]=$dLat&dropoff[longitude]=$dLng&dropoff[formatted_address]=$encDrop"
            if (tryLaunchUri(context, uberWebAppLink, null)) {
                Toast.makeText(context, "Opening Uber...", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 2. OLA DEEP LINKING
        if (isOla) {
            val olaCategory = when (option.category) {
                VehicleCategory.BIKE -> "bike"
                VehicleCategory.AUTO -> "auto"
                VehicleCategory.CAB_PREMIUM, VehicleCategory.CAB_XL -> "prime"
                else -> "micro"
            }

            val olaUri1 = "olacabs://app/launch?lat=$pLat&lng=$pLng&drop_lat=$dLat&drop_lng=$dLng&drop_name=$encDrop&pickup_name=$encPickup&category=$olaCategory"
            val olaUri2 = "ola://ola/rides?action=request&pickup_lat=$pLat&pickup_lng=$pLng&pickup_name=$encPickup&drop_lat=$dLat&drop_lng=$dLng&drop_name=$encDrop"
            val olaUri3 = "ola://rides?drop_lat=$dLat&drop_lng=$dLng&drop_name=$encDrop"

            if (tryLaunchUri(context, olaUri1, "com.olacabs.customer") ||
                tryLaunchUri(context, olaUri2, "com.olacabs.customer") ||
                tryLaunchUri(context, olaUri3, "com.olacabs.customer") ||
                tryLaunchUri(context, olaUri1, null)
            ) {
                Toast.makeText(context, "Opening Ola with destination set...", Toast.LENGTH_SHORT).show()
                return
            }

            // Direct Package Launch fallback
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.olacabs.customer")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.putExtra("drop_lat", dLat)
                launchIntent.putExtra("drop_lng", dLng)
                launchIntent.putExtra("drop_name", drop)
                context.startActivity(launchIntent)
                Toast.makeText(context, "Opening Ola App...", Toast.LENGTH_SHORT).show()
                return
            }

            // Web Universal Fallback
            val olaWebUrl = "https://book.olacabs.com/?pickup_lat=$pLat&pickup_lng=$pLng&pickup_name=$encPickup&drop_lat=$dLat&drop_lng=$dLng&drop_name=$encDrop"
            if (tryLaunchUri(context, olaWebUrl, null)) {
                Toast.makeText(context, "Opening Ola...", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 3. RAPIDO DEEP LINKING
        if (isRapido) {
            val rapidoUri1 = "rapido://booking?pickup_lat=$pLat&pickup_lng=$pLng&drop_lat=$dLat&drop_lng=$dLng&pickup_address=$encPickup&drop_address=$encDrop"
            val rapidoUri2 = "rapido://ride?pickup=$encPickup&drop=$encDrop&pickup_lat=$pLat&pickup_lng=$pLng&drop_lat=$dLat&drop_lng=$dLng"

            if (tryLaunchUri(context, rapidoUri1, "com.rapido.passenger") ||
                tryLaunchUri(context, rapidoUri2, "com.rapido.passenger") ||
                tryLaunchUri(context, rapidoUri1, null)
            ) {
                Toast.makeText(context, "Opening Rapido with destination set...", Toast.LENGTH_SHORT).show()
                return
            }

            // Direct Package Launch fallback
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.rapido.passenger")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.putExtra("drop_lat", dLat)
                launchIntent.putExtra("drop_lng", dLng)
                launchIntent.putExtra("drop_name", drop)
                context.startActivity(launchIntent)
                Toast.makeText(context, "Opening Rapido App...", Toast.LENGTH_SHORT).show()
                return
            }

            // Web Universal Fallback
            val rapidoWebUrl = "https://www.rapido.bike/booking?pickup_lat=$pLat&pickup_lng=$pLng&drop_lat=$dLat&drop_lng=$dLng&pickup=$encPickup&drop=$encDrop"
            if (tryLaunchUri(context, rapidoWebUrl, null)) {
                Toast.makeText(context, "Opening Rapido...", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 4. General Web/Store Fallback
        val fallbackUrl = when {
            isUber -> "https://m.uber.com/ul/?dropoff[latitude]=$dLat&dropoff[longitude]=$dLng&dropoff[formatted_address]=$encDrop"
            isOla -> "https://book.olacabs.com/?drop_lat=$dLat&drop_lng=$dLng&drop_name=$encDrop"
            isRapido -> "https://www.rapido.bike/booking?drop_lat=$dLat&drop_lng=$dLng"
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
                context.startActivity(playStoreWebIntent)
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
            ETA: ~${option.etaMinutes} mins | Trip: ~${option.tripDurationMinutes} mins
            Book link: ${option.webFallbackUrl}
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
                "Cheapest Ride found on RideFare"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Found cheapest ride from $pickup to $drop!\n" +
                        "🏆 ${cheapest.serviceName} for only ₹${cheapest.totalFare} (saves ₹${cheapest.savingsVsHighest})!\n" +
                        "Book directly: ${cheapest.webFallbackUrl}"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Ride Deal"))
    }
}
