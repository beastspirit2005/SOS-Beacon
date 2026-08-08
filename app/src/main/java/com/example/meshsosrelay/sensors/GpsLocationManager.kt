package com.example.meshsosrelay.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.meshsosrelay.permissions.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationData(
    val lat: Double,
    val lon: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis()
)

class GpsLocationManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val permissionManager = PermissionManager(context)

    // Default mock location (San Francisco / Hackathon base) if GPS not acquired yet
    private val _currentLocation = MutableStateFlow(
        LocationData(
            lat = 37.7749,
            lon = -122.4194,
            accuracy = 10.0f
        )
    )
    val currentLocation: StateFlow<LocationData> = _currentLocation.asStateFlow()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _currentLocation.value = LocationData(
                lat = location.latitude,
                lon = location.longitude,
                accuracy = location.accuracy,
                timestamp = location.time
            )
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!permissionManager.hasLocationPermission() || locationManager == null) {
            return
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    10f,
                    locationListener
                )
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnown != null) {
                    _currentLocation.value = LocationData(
                        lat = lastKnown.latitude,
                        lon = lastKnown.longitude,
                        accuracy = lastKnown.accuracy,
                        timestamp = lastKnown.time
                    )
                }
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    10f,
                    locationListener
                )
            }
        } catch (e: SecurityException) {
            // Fallback to default mock location
        }
    }

    fun stopLocationUpdates() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {
            // Ignore on cleanup
        }
    }

    fun updateMockLocation(lat: Double, lon: Double, accuracy: Float = 5.0f) {
        _currentLocation.value = LocationData(lat = lat, lon = lon, accuracy = accuracy)
    }
}
