package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Bicycle
import com.example.data.CycleRepository
import com.example.data.Ride
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

class CycleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CycleRepository
    private val sharedPrefs = application.getSharedPreferences("ride_tracker_prefs", android.content.Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        sharedPrefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = CycleRepository(db.bicycleDao(), db.rideDao())
    }

    val bicycles = repository.allBicycles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rides = repository.allRides.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeRides = repository.activeRides.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Ticker flow emitting the system time every second to drive real-time UI counters
    private val _ticker = MutableStateFlow(System.currentTimeMillis())
    val ticker: StateFlow<Long> = _ticker.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _ticker.value = System.currentTimeMillis()
            }
        }
    }

    // Insert pre-configured default bicycles when database is totally empty
    fun insertDefaultsIfNeeded() {
        viewModelScope.launch {
            // Give database a tiny delay so it loads the current state if existing
            delay(100)
            if (bicycles.value.isEmpty()) {
                val defaults = listOf(
                    Bicycle(name = "Mountain Rover", rate = 100.0, intervalMinutes = 15, colorHex = "#3F51B5"),
                    Bicycle(name = "City Cruiser X", rate = 80.0, intervalMinutes = 15, colorHex = "#009688"),
                    Bicycle(name = "Speedster Pro", rate = 120.0, intervalMinutes = 15, colorHex = "#FF5722")
                )
                defaults.forEach { repository.insertBicycle(it) }
            }
        }
    }

    // Operations for Bicycles
    fun addBicycle(name: String, rate: Double, intervalMinutes: Int, colorHex: String) {
        viewModelScope.launch {
            repository.insertBicycle(Bicycle(name = name, rate = rate, intervalMinutes = intervalMinutes, colorHex = colorHex))
        }
    }

    fun deleteBicycle(bicycle: Bicycle) {
        viewModelScope.launch {
            repository.deleteBicycle(bicycle)
        }
    }

    fun updateBicycle(bicycle: Bicycle) {
        viewModelScope.launch {
            repository.updateBicycle(bicycle)
        }
    }

    // Operations for Ride tracking
    fun startRide(bicycle: Bicycle) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val ride = Ride(
                bikeId = bicycle.id,
                bikeName = bicycle.name,
                startTime = now,
                rate = bicycle.rate,
                intervalMinutes = bicycle.intervalMinutes
            )
            repository.insertRide(ride)
            
            // Mark bicycle as Renting
            repository.updateBicycle(bicycle.copy(status = "Renting"))
        }
    }

    fun pauseRide(ride: Ride) {
        viewModelScope.launch {
            if (ride.isPaused) return@launch
            val now = System.currentTimeMillis()
            val addedSeconds = (now - ride.startTime) / 1000
            val newAccumulated = ride.accumulatedTimeSeconds + addedSeconds
            val updatedRide = ride.copy(
                isPaused = true,
                pauseTime = now,
                accumulatedTimeSeconds = newAccumulated
            )
            repository.updateRide(updatedRide)
        }
    }

    fun resumeRide(ride: Ride) {
        viewModelScope.launch {
            if (!ride.isPaused) return@launch
            val now = System.currentTimeMillis()
            val updatedRide = ride.copy(
                isPaused = false,
                startTime = now,
                pauseTime = null
            )
            repository.updateRide(updatedRide)
        }
    }

    fun stopRide(ride: Ride, note: String? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val seconds = if (ride.isPaused) {
                ride.accumulatedTimeSeconds
            } else {
                ride.accumulatedTimeSeconds + (now - ride.startTime) / 1000
            }
            // Finalize ride cost
            val finalCostVal = calculateBlockCost(seconds, ride.rate, ride.intervalMinutes)
            val updatedRide = ride.copy(
                endTime = now,
                finalCost = finalCostVal,
                isPaused = false,
                note = if (note.isNullOrBlank()) null else note
            )
            repository.updateRide(updatedRide)

            // Make bike available again
            val bike = repository.getBicycleById(ride.bikeId)
            if (bike != null) {
                repository.updateBicycle(bike.copy(status = "Available"))
            }
        }
    }

    fun cancelRide(ride: Ride) {
        viewModelScope.launch {
            repository.deleteRide(ride)
            // Make bike available again
            val bike = repository.getBicycleById(ride.bikeId)
            if (bike != null) {
                repository.updateBicycle(bike.copy(status = "Available"))
            }
        }
    }

    companion object {
        fun calculateBlockCost(seconds: Long, rate: Double, intervalMinutes: Int): Double {
            if (seconds <= 0) return 0.0
            val minutes = seconds / 60.0
            val blocks = ceil(minutes / intervalMinutes).toLong()
            return blocks * rate
        }

        fun calculatePreciseCost(seconds: Long, rate: Double, intervalMinutes: Int): Double {
            if (seconds <= 0) return 0.0
            val minutes = seconds / 60.0
            return minutes * (rate / intervalMinutes)
        }
    }
}

class CycleViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
