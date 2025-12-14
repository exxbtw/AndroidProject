package com.example.myapplication.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class Sockets : AppCompatActivity() {

    private val logTag = "ZMQ_CLIENT"
    private lateinit var tvSockets: TextView
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sockets)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvSockets = findViewById(R.id.tvSockets)
        handler = Handler(Looper.getMainLooper())

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            Thread { startClient() }.start()
        }

        findViewById<Button>(R.id.back_to_main).setOnClickListener {
            finish()
        }
    }

    private fun startClient() {
        val context = ZContext()
        val socket = context.createSocket(SocketType.REQ)

        socket.connect("tcp://192.168.1.104:5566")

        val request = "Hello from Android!"
        Log.d(logTag, "Send: $request")

        socket.send(request.toByteArray(ZMQ.CHARSET), 0)

        val replyBytes = socket.recv(0)
        val reply = String(replyBytes, ZMQ.CHARSET)

        Log.d(logTag, "Received: $reply")

        handler.post {
            tvSockets.text = "Server response:\n$reply"
        }

        socket.close()
        context.close()
    }
}