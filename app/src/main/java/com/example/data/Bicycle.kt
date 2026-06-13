package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bicycles")
data class Bicycle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rate: Double = 100.0,
    val intervalMinutes: Int = 15,
    val status: String = "Available", // "Available" or "Renting"
    val colorHex: String = "#4CAF50"
)
