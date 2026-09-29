package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_routes")
data class SavedRoute(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val pickupName: String,
    val dropName: String,
    val distanceKm: Float,
    val isFavorite: Boolean = true,
    val priceAlertEnabled: Boolean = false,
    val priceThreshold: Int = 250,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pickupName: String,
    val dropName: String,
    val distanceKm: Float,
    val cheapestProvider: String,
    val cheapestService: String,
    val cheapestFare: Int,
    val maxFare: Int,
    val timestamp: Long = System.currentTimeMillis()
)
