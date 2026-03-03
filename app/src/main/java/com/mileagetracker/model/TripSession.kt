package com.mileagetracker.model

import java.util.UUID

data class TripSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    val locationPoints: MutableList<LocationPoint> = mutableListOf(),
    var totalDistanceKm: Double = 0.0,
    var endAddress: String? = null,
    var endTimeStr: String? = null
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val speed: Float = 0f
)
