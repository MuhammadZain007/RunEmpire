package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllActivities(userId: String): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    fun getActivityById(id: String): Flow<ActivityEntity?>

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    suspend fun getActivityByIdSync(id: String): ActivityEntity?
}
