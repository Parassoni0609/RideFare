package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedRoute::class, SearchHistoryItem::class],
    version = 3,
    exportSchema = true
)
abstract class RideFareDatabase : RoomDatabase() {

    abstract fun rideDao(): RideDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_routes ADD COLUMN priceAlertEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_routes ADD COLUMN priceThreshold INTEGER NOT NULL DEFAULT 250")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("pickupLat", "pickupLng", "dropLat", "dropLng").forEach {
                    db.execSQL("ALTER TABLE saved_routes ADD COLUMN $it REAL")
                }
                db.execSQL("ALTER TABLE saved_routes ADD COLUMN cityId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_routes ADD COLUMN trafficCondition TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE saved_routes ADD COLUMN weatherCondition TEXT NOT NULL DEFAULT 'REGULAR'")
            }
        }

        @Volatile
        private var INSTANCE: RideFareDatabase? = null

        fun getDatabase(context: Context): RideFareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RideFareDatabase::class.java,
                    "ride_fare_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
