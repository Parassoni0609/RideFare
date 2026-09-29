package com.example.ridefare

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.RideFareDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DatabaseMigrationTest {
    @Test fun `version 2 upgrade preserves favorites targets and history`() = verifyUpgrade(2)
    @Test fun `version 1 upgrade preserves favorites and adds target defaults`() = verifyUpgrade(1)

    private fun verifyUpgrade(version: Int) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-$version.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            val alertColumns = if (version == 2) ", priceAlertEnabled INTEGER NOT NULL, priceThreshold INTEGER NOT NULL" else ""
            db.execSQL("CREATE TABLE saved_routes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, pickupName TEXT NOT NULL, dropName TEXT NOT NULL, distanceKm REAL NOT NULL, isFavorite INTEGER NOT NULL, createdAt INTEGER NOT NULL$alertColumns)")
            db.execSQL("CREATE TABLE search_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, pickupName TEXT NOT NULL, dropName TEXT NOT NULL, distanceKm REAL NOT NULL, cheapestProvider TEXT NOT NULL, cheapestService TEXT NOT NULL, cheapestFare INTEGER NOT NULL, maxFare INTEGER NOT NULL, timestamp INTEGER NOT NULL)")
            val alerts = if (version == 2) ", 1, 180" else ""
            db.execSQL("INSERT INTO saved_routes VALUES (7, 'Commute', 'A', 'B', 10.5, 1, 123$alerts)")
            db.execSQL("INSERT INTO search_history VALUES (3, 'A', 'B', 10.5, 'Uber', 'Uber Go', 150, 300, 123)")
            db.version = version
        }
        val database = Room.databaseBuilder(context, RideFareDatabase::class.java, name)
            .addMigrations(RideFareDatabase.MIGRATION_1_2, RideFareDatabase.MIGRATION_2_3).build()
        try {
            runBlocking {
                val saved = database.rideDao().getSavedRoute(7)!!
                assertEquals("Commute", saved.title)
                assertEquals(if (version == 2) 180 else 250, saved.priceThreshold)
                assertEquals(version == 2, saved.priceAlertEnabled)
                assertNull(saved.pickupLat)
                assertEquals("NORMAL", saved.trafficCondition)
            }
            database.openHelper.readableDatabase.query("SELECT cheapestFare FROM search_history WHERE id = 3").use {
                assertTrue(it.moveToFirst()); assertEquals(150, it.getInt(0))
            }
        } finally {
            database.close(); context.deleteDatabase(name)
        }
    }
}
