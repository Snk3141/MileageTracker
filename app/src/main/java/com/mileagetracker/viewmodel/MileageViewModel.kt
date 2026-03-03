package com.mileagetracker.viewmodel

import android.annotation.SuppressLint
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileagetracker.model.LocationPoint
import com.mileagetracker.model.TripSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrackingState(
    val isTracking: Boolean = false,
    val currentSession: TripSession? = null,
    val sessionHistory: List<TripSession> = emptyList(),
    val lastKnownAddress: String? = null
)

class MileageViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()
    
    private var geocoder: Geocoder? = null
    
    fun setGeocoder(geocoder: Geocoder) {
        this.geocoder = geocoder
    }
    
    @SuppressLint("MissingPermission")
    fun startTracking() {
        val session = TripSession()
        _state.value = _state.value.copy(
            isTracking = true,
            currentSession = session
        )
    }
    
    @Suppress("DEPRECATION")
    fun stopTracking() {
        val currentSession = _state.value.currentSession ?: return
        
        val endTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val endTimeStr = dateFormat.format(Date(endTime))
        
        // Get end address from last location point
        var endAddress = "Unknown location"
        currentSession.locationPoints.lastOrNull()?.let { lastPoint ->
            try {
                val addresses = geocoder?.getFromLocation(
                    lastPoint.latitude, 
                    lastPoint.longitude, 
                    1
                )
                endAddress = addresses?.firstOrNull()?.let { addr ->
                    listOfNotNull(
                        addr.subThoroughfare,
                        addr.thoroughfare,
                        addr.locality,
                        addr.adminArea
                    ).joinToString(", ")
                } ?: "Unknown location"
            } catch (e: Exception) {
                endAddress = "Unknown location"
            }
        }
        
        currentSession.endTime = endTime
        currentSession.endTimeStr = endTimeStr
        currentSession.endAddress = endAddress
        
        val updatedSession = currentSession.copy()
        
        _state.value = _state.value.copy(
            isTracking = false,
            currentSession = null,
            sessionHistory = listOf(updatedSession) + _state.value.sessionHistory,
            lastKnownAddress = endAddress
        )
    }
    
    @SuppressLint("MissingPermission")
    fun addLocationPoint(latitude: Double, longitude: Double, speed: Float) {
        val currentSession = _state.value.currentSession ?: return
        
        val newPoint = LocationPoint(latitude, longitude, speed = speed)
        currentSession.locationPoints.add(newPoint)
        
        // Calculate total distance
        var totalDistance = 0.0
        val points = currentSession.locationPoints
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            totalDistance += calculateDistance(
                prev.latitude, prev.longitude,
                curr.latitude, curr.longitude
            )
        }
        
        currentSession.totalDistanceKm = totalDistance
        
        _state.value = _state.value.copy(
            currentSession = currentSession
        )
    }
    
    fun updateLastAddress(address: String?) {
        _state.value = _state.value.copy(lastKnownAddress = address)
    }
    
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0 // km
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
}
