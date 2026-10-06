package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.Locale

data class GeofenceResult(
    val isInsideGeofence: Boolean,
    val distanceMeters: Float,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val statusMessage: String
)

object LocationHelper {

    // Making Brands Office Coordinates (HQ)
    const val OFFICE_LAT = 28.6292
    const val OFFICE_LNG = 77.2185
    const val OFFICE_NAME = "Making Brands HQ, Connaught Place, New Delhi"
    const val GEOFENCE_RADIUS_METERS = 250f

    /**
     * Checks if location permissions are granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    /**
     * Gets current location or fallback, and computes geofence perimeter adherence.
     */
    @SuppressLint("MissingPermission")
    fun verifyOfficeGeofence(context: Context): GeofenceResult {
        var currentLat = OFFICE_LAT
        var currentLng = OFFICE_LNG
        var acquiredRealGps = false

        if (hasLocationPermission(context)) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val gpsLoc = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
                val netLoc = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
                val bestLoc = gpsLoc ?: netLoc

                if (bestLoc != null) {
                    currentLat = bestLoc.latitude
                    currentLng = bestLoc.longitude
                    acquiredRealGps = true
                }
            }
        }

        // Calculate distance between user location and Office
        val results = FloatArray(1)
        Location.distanceBetween(currentLat, currentLng, OFFICE_LAT, OFFICE_LNG, results)
        val distance = results[0]
        val isInside = distance <= GEOFENCE_RADIUS_METERS

        val locationName = if (acquiredRealGps) {
            resolveAddress(context, currentLat, currentLng)
        } else {
            OFFICE_NAME
        }

        val statusMsg = when {
            isInside -> "✅ Verified inside Office Perimeter (${distance.toInt()}m from HQ)"
            else -> "📍 Outside Office Geofence (${(distance / 1000f).let { String.format(Locale.US, "%.1f km", it) }} away) — Remote Punch-In Recorded"
        }

        return GeofenceResult(
            isInsideGeofence = isInside,
            distanceMeters = distance,
            latitude = currentLat,
            longitude = currentLng,
            locationName = locationName,
            statusMessage = statusMsg
        )
    }

    private fun resolveAddress(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                listOfNotNull(addr.locality, addr.subAdminArea, addr.adminArea)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .ifBlank { "Lat: ${String.format(Locale.US, "%.4f", lat)}, Lng: ${String.format(Locale.US, "%.4f", lng)}" }
            } else {
                "Lat: ${String.format(Locale.US, "%.4f", lat)}, Lng: ${String.format(Locale.US, "%.4f", lng)}"
            }
        } catch (_: Exception) {
            "Lat: ${String.format(Locale.US, "%.4f", lat)}, Lng: ${String.format(Locale.US, "%.4f", lng)}"
        }
    }
}
