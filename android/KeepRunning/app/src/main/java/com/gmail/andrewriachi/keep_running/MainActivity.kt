package com.gmail.andrewriachi.keep_running

import android.content.IntentSender
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KotlinActivity"
        private const val REQUEST_CHECK_SETTINGS = 1;
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            Log.d(TAG, "something goofy")
            locationResult ?: return
            for (location in locationResult.locations) {
                // Update UI with location data
                // TODO: play sound
                val piLat = currentPiLocation.lat
                val piLng = currentPiLocation.lng
                val piLocation = Location("me")
                piLocation.setLatitude(piLat)
                piLocation.setLongitude(piLng)
                Log.d(TAG, "Latitude: ${location.latitude}")
                Log.d(TAG, "Longitude: ${location.longitude}")
                val dist = location.distanceTo(piLocation)
                val distanceText: TextView = findViewById(R.id.distance_text)
                distanceText.text = "%d m".format(dist)
            }
        }
    }
    private val locationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private var currentPiLocation = MyLocation(38.95, -95.25)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "Test")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("piLocation")
        ref.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.getValue(MyLocation::class.java)
                Log.d(TAG, "Value is: $value")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }
    private fun createLocationRequest() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            // COMMAND the fused location client provider to give us updates by calling a callback
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this
                // can be fixed by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult()
                    exception.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error
                }
            }
        }
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }
}

