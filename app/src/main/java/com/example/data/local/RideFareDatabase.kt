package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedRoute::class, SearchHistoryItem::class],
    version = 2,
    exportSchema = false
)
abstract class RideFareDatabase : RoomDatabase() {

    abstract fun rideDao(): RideDao

    companion object {
        @Volatile
        private var INSTANCE: RideFareDatabase? = null

        fun getDatabase(context: Context): RideFareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RideFareDatabase::class.java,
                    "ride_fare_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
