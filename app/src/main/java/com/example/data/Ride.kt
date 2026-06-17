package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class Ride(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bikeId: Int,
    val bikeName: String,
    val startTime: Long, // Epoch ms
    val endTime: Long? = null, // Epoch ms
    val rate: Double,
    val intervalMinutes: Int,
    val isPaused: Boolean = false,
    val pauseTime: Long? = null,
    val accumulatedTimeSeconds: Long = 0,
    val finalCost: Double? = null,
    val note: String? = null
)
