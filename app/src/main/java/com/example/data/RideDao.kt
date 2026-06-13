package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM rides ORDER BY startTime DESC")
    fun getAllRidesFlow(): Flow<List<Ride>>

    @Query("SELECT * FROM rides WHERE endTime IS NULL")
    fun getActiveRidesFlow(): Flow<List<Ride>>

    @Query("SELECT * FROM rides WHERE bikeId = :bikeId ORDER BY startTime DESC")
    fun getRidesForBikeFlow(bikeId: Int): Flow<List<Ride>>

    @Query("SELECT * FROM rides WHERE id = :id")
    suspend fun getRideById(id: Int): Ride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: Ride): Long

    @Update
    suspend fun updateRide(ride: Ride)

    @Delete
    suspend fun deleteRide(ride: Ride)
}
