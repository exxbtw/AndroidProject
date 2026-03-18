package com.example.myapplication.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.telephony.*
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class TelephonyService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var zContext: ZContext
    private lateinit var socket: org.zeromq.ZMQ.Socket

    private val CHANNEL_ID = "TelephonyServiceChannel"
    private val SERVER_URL = "tcp://10.64.11.27:5566"

    private val LOG_FILE_NAME = "location_log.json"

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        zContext = ZContext()
        socket = zContext.createSocket(SocketType.REQ)
        socket.receiveTimeOut = 2000
        socket.sendTimeOut = 2000
        socket.connect(SERVER_URL)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Data Collector Active")
            .setContentText("Recording GPS + Cell Info to file...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .build()
        startForeground(1, notification)

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                2f,
                this
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        executor.execute {
            val json = JSONObject()
            json.put("latitude", location.latitude)
            json.put("longitude", location.longitude)
            json.put("altitude", location.altitude)
            json.put("time", System.currentTimeMillis())

            val cellArray = JSONArray()
            try {
                telephonyManager.allCellInfo?.firstOrNull { it.isRegistered }?.let { info ->
                    val cellObject = JSONObject()
                    when (info) {
                        is CellInfoLte -> {
                            val id = info.cellIdentity
                            val sig = info.cellSignalStrength

                            cellObject.put("type", "LTE")

                            cellObject.put("pci", id.pci)
                            cellObject.put("earfcn", id.earfcn)
                            cellObject.put("tac", id.tac)
                            cellObject.put("mcc", id.mccString?.toIntOrNull() ?: 0)
                            cellObject.put("mnc", id.mncString?.toIntOrNull() ?: 0)
                            cellObject.put("band", if (Build.VERSION.SDK_INT >= 29) id.bandwidth else 0)

                            cellObject.put("rsrp", sig.rsrp)
                            cellObject.put("rsrq", sig.rsrq)
                            cellObject.put("rssi", sig.rssi)
                            cellObject.put("rssnr", sig.rssnr)
                            cellObject.put("cqi", sig.cqi)
                            cellObject.put("asu", sig.asuLevel)
                            cellObject.put("ta", sig.timingAdvance)
                        }
                        is CellInfoGsm -> {
                            val id = info.cellIdentity
                            val sig = info.cellSignalStrength

                            cellObject.put("type", "GSM")

                            cellObject.put("lac", id.lac)
                            cellObject.put("cid", id.cid)
                            cellObject.put("arfcn", id.arfcn)
                            cellObject.put("bsic", id.bsic)
                            cellObject.put("psc", id.psc)

                            cellObject.put("mcc", id.mccString?.toIntOrNull() ?: 0)
                            cellObject.put("mnc", id.mncString?.toIntOrNull() ?: 0)

                            cellObject.put("rssi", sig.dbm)
                            cellObject.put("asu", sig.asuLevel)
                            cellObject.put("ta", sig.timingAdvance)
                        }
                        is CellInfoNr -> {
                            val id = info.cellIdentity as CellIdentityNr
                            val sig = info.cellSignalStrength as CellSignalStrengthNr

                            cellObject.put("type", "NR")

                            cellObject.put("pci", id.pci)
                            cellObject.put("tac", id.tac)
                            cellObject.put("nci", id.nci)
                            cellObject.put("nrarfcn", id.nrarfcn)

                            cellObject.put("mcc", id.mccString?.toIntOrNull() ?: 0)
                            cellObject.put("mnc", id.mncString?.toIntOrNull() ?: 0)

                            cellObject.put("ss_rsrp", sig.ssRsrp)
                            cellObject.put("ss_rsrq", sig.ssRsrq)
                            cellObject.put("ss_sinr", sig.ssSinr)
                            cellObject.put("asu", sig.asuLevel)

                        }
                    }
                    cellArray.put(cellObject)
                }
            } catch (e: SecurityException) { e.printStackTrace() }

            json.put("cell_info", cellArray)

            val dataString = json.toString()

            saveLocally(dataString)

            try {
                socket.send(dataString)
                socket.recvStr()
            } catch (e: Exception) {
            }
        }
    }

    private fun saveLocally(data: String) {
        try {
            val folder = getExternalFilesDir(null)
            val file = File(folder, LOG_FILE_NAME)

            FileOutputStream(file, true).use { stream ->
                stream.write((data + "\n").toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
        executor.shutdown()
        socket.close()
        zContext.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}