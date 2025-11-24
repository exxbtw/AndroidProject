package com.example.myapplication
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import java.io.OutputStream

class LocationActivity : AppCompatActivity() {

    val value: Int = 0
    val LOG_TAG: String = "LOCATION_ACTIVITY"
    private lateinit var bBackToMain: Button

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION= 100
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest
    private lateinit var locationCallback: com.google.android.gms.location.LocationCallback


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bBackToMain = findViewById<Button>(R.id.back_to_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tvLat = findViewById(R.id.tv_lat) as TextView
        tvLon = findViewById(R.id.tv_lon) as TextView

        locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).setMinUpdateDistanceMeters(1f).build()

        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    updateUI(location)
                }
            }
        }
    }



    override fun onResume() {
        super.onResume()
        startLocationUpdates()

        bBackToMain.setOnClickListener({
            val backToMain = Intent(this, MainActivity::class.java)
            startActivity(backToMain)
        })

    }

    private fun getCurrentLocation() {

        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Enable location in settings", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateUI(location)
                } else {
                    requestFreshLocation()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestFreshLocation() {

        val priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY

        val cancellationToken = com.google.android.gms.tasks.CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(priority, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateUI(location)
                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUI(location: Location) {
        tvLat.text = "Latitude: ${"%.6f".format(location.latitude)}"
        tvLon.text = "Longitude: ${"%.6f".format(location.longitude)}"

        val alt = if (location.hasAltitude()) location.altitude else 0.0
        findViewById<TextView>(R.id.tv_alt).text = "Altitude: ${"%.3f".format(alt)} m"

        findViewById<TextView>(R.id.tv_time).text =
            "Time: ${System.currentTimeMillis()}"

        saveToJSON(location)
    }

    private fun startLocationUpdates() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun saveToJSON(location: Location) {
        try {
            val newInfo = JSONObject().apply {
                put("lat", location.latitude)
                put("lon", location.longitude)
                put("alt", if (location.hasAltitude()) location.altitude else 0.0)
                put("time", System.currentTimeMillis())
            }

            val jsonArray = if (fileExists()) {
                JSONArray(readExistingFile())
            } else {
                JSONArray()
            }

            jsonArray.put(newInfo)
            val jsonContent = jsonArray.toString()

            saveToPublicDocuments(jsonContent)
            //Log.d(LOG_TAG, "Location saved to public Documents")
        } catch (e: Exception) {
            //Log.e(LOG_TAG, "Error writing JSON: ${e.message}")
        }
    }

    private fun saveToPublicDocuments(content: String) {
        val fileName = "location_log.json"
        val mimeType = "application/json"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MyLocationApp")
        }

        val resolver = contentResolver
        var outputStream: OutputStream? = null

        try {
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            uri?.let {
                outputStream = resolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    stream.write(content.toByteArray())
                }
                //Toast.makeText(this, "File saved to Documents", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            //Log.e(LOG_TAG, "Error saving to Documents: ${e.message}")
        } finally {
            outputStream?.close()
        }
    }

    private fun fileExists(): Boolean {
        val internalFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "location_log.json")
        return internalFile.exists()
    }

    private fun readExistingFile(): String {
        val internalFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "location_log.json")
        return if (internalFile.exists()) {
            internalFile.readText()
        } else {
            "[]"
        }
    }

    private fun requestPermissions() {
        Log.w(LOG_TAG, "requestPermissions()");
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun checkPermissions(): Boolean{
        if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED )
        {
            return true
        } else {
            return false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST_ACCESS_LOCATION)
        {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied by user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }

}

