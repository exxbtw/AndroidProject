package com.example.myapplication.activities

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import android.location.LocationListener
import android.location.LocationManager
import android.content.Context

class Location2 : AppCompatActivity(), LocationListener {

    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var locationManager: LocationManager

    private lateinit var tvTime: TextView
    private var currentTime: Long = 0L

    private val handler = Handler(Looper.getMainLooper())

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var currentAlt: Double = 0.0

    private val REQUEST_LOCATION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location2)

        tvTime = findViewById(R.id.tv_time)
        tvLat = findViewById(R.id.tv_lat)
        tvLon = findViewById(R.id.tv_lon)
        tvAlt = findViewById(R.id.tv_alt)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        requestLocationPermission()

        findViewById<Button>(R.id.back_to_main).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(this)
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000L,
            2f,
            this
        )
    }

    override fun onLocationChanged(location: Location) {
        updateUI(location)

        Thread {
            sendToZmq(location.latitude, location.longitude,
                if (location.hasAltitude()) location.altitude else 0.0)
        }.start()
    }

    private fun updateUI(location: Location) {

        currentLat = location.latitude
        currentLon = location.longitude
        currentAlt = if (location.hasAltitude()) location.altitude else 0.0
        currentTime = System.currentTimeMillis()

        handler.post {
            tvLat.text = "Latitude: ${"%.6f".format(currentLat)}"
            tvLon.text = "Longitude: ${"%.6f".format(currentLon)}"
            tvAlt.text = "Altitude: ${"%.3f".format(currentAlt)} m"
            tvTime.text = "Time: $currentTime"
        }


//        saveToJSON(location)
    }

//    private fun saveToJSON(location: Location) {
//        val json = JSONObject()
//        json.put("latitude", location.latitude)
//        json.put("longitude", location.longitude)
//        json.put("altitude", if (location.hasAltitude()) location.altitude else 0.0)
//        json.put("time", System.currentTimeMillis())
//
//    }

    private fun sendToZmq(lat: Double, lon: Double, alt: Double) {

        val maxAttempts = 5
        var attempt = 0

        while (attempt < maxAttempts) {

            val context = ZContext()
            val socket = context.createSocket(SocketType.REQ)

            try {

                socket.receiveTimeOut = 3000
                socket.sendTimeOut = 3000

                socket.connect("tcp://10.36.114.129:5566")

                val json = JSONObject()
                json.put("latitude", lat)
                json.put("longitude", lon)
                json.put("altitude", alt)
                json.put("time", System.currentTimeMillis())

                socket.send(json.toString())

                val reply = socket.recvStr()

                if (reply != null) {
                    handler.post {
                        Toast.makeText(this, "Server: $reply", Toast.LENGTH_LONG).show()
                    }
                    socket.close()
                    context.close()
                    return
                }

            } catch (e: Exception) {
                attempt++
                Thread.sleep(2000)
            } finally {
                socket.close()
                context.close()
            }
        }

//        handler.post {
//            Toast.makeText(this, "Server unavailable", Toast.LENGTH_LONG).show()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
    }
}