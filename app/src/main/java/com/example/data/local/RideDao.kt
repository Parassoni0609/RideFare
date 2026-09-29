package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {

    @Query("SELECT * FROM saved_routes ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllSavedRoutes(): Flow<List<SavedRoute>>

    @Query("SELECT * FROM saved_routes WHERE id = :id LIMIT 1")
    suspend fun getSavedRoute(id: Long): SavedRoute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRoute(route: SavedRoute): Long

    @Query("UPDATE saved_routes SET priceAlertEnabled = :enabled, priceThreshold = :threshold WHERE id = :routeId")
    suspend fun updatePriceAlert(routeId: Long, enabled: Boolean, threshold: Int)

    @Update
    suspend fun updateSavedRoute(route: SavedRoute)

    @Delete
    suspend fun deleteSavedRoute(route: SavedRoute)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getSearchHistory(): Flow<List<SearchHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(item: SearchHistoryItem): Long

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
