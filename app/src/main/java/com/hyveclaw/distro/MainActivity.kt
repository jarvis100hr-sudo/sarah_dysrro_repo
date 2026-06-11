package com.hyveclaw.distro

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var config: NodeConfig
    private lateinit var tvStatus: TextView
    private lateinit var tvNodeInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        config    = NodeConfig.load(this)
        tvStatus  = findViewById(R.id.tvStatus)
        tvNodeInfo = findViewById(R.id.tvNodeInfo)

        val etSaraAddress = findViewById<EditText>(R.id.etSaraAddress)
        val etLocalPort   = findViewById<EditText>(R.id.etLocalPort)
        val btnSave       = findViewById<Button>(R.id.btnSave)
        val btnStart      = findViewById<Button>(R.id.btnStart)
        val btnStop       = findViewById<Button>(R.id.btnStop)

        refreshInfo()
        etSaraAddress.setText(config.saraAddress)
        etLocalPort.setText(config.localPort.toString())

        btnSave.setOnClickListener {
            config = config.copy(
                saraAddress = etSaraAddress.text.toString().trim(),
                localPort   = etLocalPort.text.toString().toIntOrNull() ?: 7801
            )
            NodeConfig.save(this, config)
            refreshInfo()
            tvStatus.text = "Config saved — restart service to apply"
        }

        btnStart.setOnClickListener {
            ContextCompat.startForegroundService(this, Intent(this, DistroHyveService::class.java))
            tvStatus.text = "Starting…"
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, DistroHyveService::class.java))
            tvStatus.text = "Stopped"
        }
    }

    private fun refreshInfo() {
        tvNodeInfo.text = buildString {
            appendLine("Node:    ${config.nodeName}")
            appendLine("Seat:    ${config.seatType}")
            appendLine("Sara:    ${config.saraAddress}:${config.saraPort}")
            appendLine("Port:    ${config.localPort}")
            append(    "Network: ${config.network}")
        }
        tvStatus.text = "Ready"
    }
}
