package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val polygon: List<LatLngPoint>, // converted to JSON string via TypeConverters
    val area: Double,               // captured polygon area in square meters
    val capturedAt: Long = System.currentTimeMillis()
)
