package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TerritoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerritoryDao {
    @Query("SELECT * FROM territories WHERE userId = :userId ORDER BY capturedAt DESC")
    fun getAllTerritories(userId: String): Flow<List<TerritoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerritory(territory: TerritoryEntity)
}
