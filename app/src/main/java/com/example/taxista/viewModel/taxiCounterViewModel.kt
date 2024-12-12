package com.example.taxista.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxista.service.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

class TaxiCounterViewModel(context: Context) : ViewModel() {
    private val locationService = LocationService(context)

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation: StateFlow<GeoPoint?> = _currentLocation

    private val _routePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val routePoints: StateFlow<List<GeoPoint>> = _routePoints

    private val _distanceTraveled = MutableStateFlow(0.0)
    val distanceTraveled: StateFlow<Double> = _distanceTraveled

    private val _totalCost = MutableStateFlow(0.0)
    val totalCost: StateFlow<Double> = _totalCost

    private val baseRate = 5.0 // Base rate in DH
    private val ratePerKm = 2.5 // Rate per kilometer in DH

    init {
        startLocationTracking()
    }

    private fun startLocationTracking() {
        viewModelScope.launch {
            try {
                // Get initial location
                val initialLocation = locationService.getCurrentLocation()
                _currentLocation.value = initialLocation
                _routePoints.value = listOf(initialLocation)

                // Start location updates
                locationService.getLocationUpdates()
                    .catch { e -> 
                        e.printStackTrace()
                    }
                    .collect { newLocation ->
                        _currentLocation.value = newLocation
                        updateRoute(newLocation)
                        updateCost()
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateRoute(newLocation: GeoPoint) {
        val currentPoints = _routePoints.value.toMutableList()
        val lastPoint = currentPoints.lastOrNull()

        if (lastPoint != null) {
            // Calculate distance from last point
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                lastPoint.latitude, lastPoint.longitude,
                newLocation.latitude, newLocation.longitude,
                results
            )
            val distance = results[0]

            // Only add point if we've moved more than 5 meters
            if (distance > 5) {
                currentPoints.add(newLocation)
                _routePoints.value = currentPoints
                _distanceTraveled.value += distance
            }
        } else {
            currentPoints.add(newLocation)
            _routePoints.value = currentPoints
        }
    }

    private fun updateCost() {
        val distanceKm = _distanceTraveled.value / 1000
        val cost = baseRate + (distanceKm * ratePerKm)
        _totalCost.value = (cost * 100).roundToInt() / 100.0 // Round to 2 decimal places
    }

    fun resetCounter() {
        val currentLocation = _currentLocation.value
        _distanceTraveled.value = 0.0
        _totalCost.value = baseRate
        _routePoints.value = currentLocation?.let { listOf(it) } ?: emptyList()
    }
}