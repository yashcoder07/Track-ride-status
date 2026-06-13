package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BicycleDao {
    @Query("SELECT * FROM bicycles ORDER BY name ASC")
    fun getAllBicyclesFlow(): Flow<List<Bicycle>>

    @Query("SELECT * FROM bicycles WHERE id = :id")
    suspend fun getBicycleById(id: Int): Bicycle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBicycle(bicycle: Bicycle): Long

    @Update
    suspend fun updateBicycle(bicycle: Bicycle)

    @Delete
    suspend fun deleteBicycle(bicycle: Bicycle)
}
