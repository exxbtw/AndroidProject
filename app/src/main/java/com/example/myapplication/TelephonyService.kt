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
import java.util.concurrent.Executors

class TelephonyService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var zContext: ZContext
    private lateinit var socket: org.zeromq.ZMQ.Socket

    private val CHANNEL_ID = "TelephonyServiceChannel"
    private val SERVER_URL = "tcp://10.214.164.27:5566"

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        zContext = ZContext()
        socket = zContext.createSocket(SocketType.REQ)
        socket.receiveTimeOut = 3000
        socket.sendTimeOut = 3000
        socket.connect(SERVER_URL)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telephony Service")
            .setContentText("Sending GPS + Cell Info...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
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
                            cellObject.put("type", "LTE")
                            cellObject.put("pci", info.cellIdentity.pci)
                            cellObject.put("earfcn", info.cellIdentity.earfcn)
                            cellObject.put("tac", info.cellIdentity.tac)
                            cellObject.put("rsrp", info.cellSignalStrength.rsrp)
                            cellObject.put("rsrq", info.cellSignalStrength.rsrq)
                            cellObject.put("rssi", info.cellSignalStrength.rssi)
                            cellObject.put("ta", info.cellSignalStrength.timingAdvance)
                        }
                        is CellInfoGsm -> {
                            cellObject.put("type", "GSM")
                            cellObject.put("lac", info.cellIdentity.lac)
                            cellObject.put("cid", info.cellIdentity.cid)
                            cellObject.put("dbm", info.cellSignalStrength.dbm)
                            cellObject.put("ta", info.cellSignalStrength.timingAdvance)
                        }
                        is CellInfoNr -> {
                            val id = info.cellIdentity as CellIdentityNr
                            cellObject.put("type", "NR")
                            cellObject.put("pci", id.pci)
                            cellObject.put("tac", id.tac)
                            cellObject.put("nci", id.nci)
                        }
                    }
                    cellArray.put(cellObject)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            json.put("cell_info", cellArray)

            try {
                socket.send(json.toString())
                socket.recvStr()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
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