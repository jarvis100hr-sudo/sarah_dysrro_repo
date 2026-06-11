package com.hyveclaw.distro

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class DistroHyveService : Service() {

    private val tag          = "DistroHyveService"
    private val channelId    = "distro_hyve"
    private val notifId      = 1

    private lateinit var config:   NodeConfig
    private lateinit var handshake: HandshakeManager
    private lateinit var doctor:    DoctorAgent
    private lateinit var claw:      ClawHandler
    private var server: HyveServer? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        config    = NodeConfig.load(this)
        handshake = HandshakeManager(config)
        doctor    = DoctorAgent(config)
        claw      = ClawHandler(config, doctor)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notifId, buildNotif("Starting…"))
        startServer()
        scheduleHeartbeat()
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        WorkManager.getInstance(this).cancelUniqueWork("hyve_heartbeat")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── server ─────────────────────────────────────────────────────────────────

    private fun startServer() {
        try {
            server = HyveServer(config.localPort, config, handshake, claw, doctor)
            server!!.start()
            Log.i(tag, "Server started on port ${config.localPort}")
            notify("Alive — port ${config.localPort}")

            // Boot heartbeat after a brief settle delay
            Thread {
                runCatching {
                    Thread.sleep(4000)
                    handshake.sendHeartbeat()
                }
            }.start()

        } catch (e: Exception) {
            Log.e(tag, "Server start failed: ${e.message}")
            notify("Error: ${e.message}")
        }
    }

    // ── periodic heartbeat via WorkManager ─────────────────────────────────────

    private fun scheduleHeartbeat() {
        val req = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "hyve_heartbeat",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    // ── notification ───────────────────────────────────────────────────────────

    private fun createChannel() {
        val ch = NotificationChannel(channelId, "Distro Hyve", NotificationManager.IMPORTANCE_LOW)
            .apply { description = "Distro Hyve Data node agent" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Distro Hyve — Data")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    fun notify(text: String) {
        getSystemService(NotificationManager::class.java).notify(notifId, buildNotif(text))
    }
}
