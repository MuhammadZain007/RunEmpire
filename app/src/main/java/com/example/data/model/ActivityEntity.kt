package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val distance: Double, // in kilometers
    val duration: Int,    // in seconds
    val calories: Double,
    val route: List<LatLngPoint>, // converted to JSON string via TypeConverters
    val createdAt: Long = System.currentTimeMillis()
)
