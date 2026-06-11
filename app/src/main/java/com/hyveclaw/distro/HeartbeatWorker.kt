package com.hyveclaw.distro

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class HeartbeatWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        return runCatching {
            HandshakeManager(NodeConfig.load(applicationContext)).sendHeartbeat()
            Result.success()
        }.getOrElse {
            Log.w("HeartbeatWorker", "Heartbeat failed: ${it.message}")
            Result.retry()
        }
    }
}
