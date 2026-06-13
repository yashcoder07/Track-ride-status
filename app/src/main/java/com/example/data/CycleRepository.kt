package com.example.data

import kotlinx.coroutines.flow.Flow

class CycleRepository(
    private val bicycleDao: BicycleDao,
    private val rideDao: RideDao
) {
    val allBicycles: Flow<List<Bicycle>> = bicycleDao.getAllBicyclesFlow()
    val allRides: Flow<List<Ride>> = rideDao.getAllRidesFlow()
    val activeRides: Flow<List<Ride>> = rideDao.getActiveRidesFlow()

    suspend fun getBicycleById(id: Int): Bicycle? = bicycleDao.getBicycleById(id)
    suspend fun insertBicycle(bicycle: Bicycle) = bicycleDao.insertBicycle(bicycle)
    suspend fun updateBicycle(bicycle: Bicycle) = bicycleDao.updateBicycle(bicycle)
    suspend fun deleteBicycle(bicycle: Bicycle) = bicycleDao.deleteBicycle(bicycle)

    suspend fun getRideById(id: Int): Ride? = rideDao.getRideById(id)
    suspend fun insertRide(ride: Ride): Long = rideDao.insertRide(ride)
    suspend fun updateRide(ride: Ride) = rideDao.updateRide(ride)
    suspend fun deleteRide(ride: Ride) = rideDao.deleteRide(ride)
}
