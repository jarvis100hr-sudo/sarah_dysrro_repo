package com.hyveclaw.distro

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class HandshakeManager(private val config: NodeConfig) {

    private val tag = "HandshakeManager"

    fun handlePing(body: String): JSONObject {
        val ping = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
        val from = ping.optString("from", "unknown")
        val ts = Instant.now().toString()

        Log.i(tag, "Ping received from $from")

        val pingBack = JSONObject().apply {
            put("from",        config.nodeName)
            put("to",          from)
            put("type",        "ping_back")
            put("timestamp",   ts)
            put("seat_status", "alive")
        }

        // Send confirm asynchronously so ping_back returns immediately
        Thread {
            runCatching {
                Thread.sleep(500)
                val confirm = JSONObject().apply {
                    put("from",             config.nodeName)
                    put("to",               from)
                    put("type",             "confirm")
                    put("timestamp",        Instant.now().toString())
                    put("handshake_status", "complete")
                }
                post("http://${config.saraAddress}:${config.saraPort}/handshake/confirm", confirm.toString())
                Log.i(tag, "Confirm sent to Sara")
            }.onFailure { Log.w(tag, "Confirm failed: ${it.message}") }
        }.start()

        return pingBack
    }

    fun sendHeartbeat() {
        val payload = JSONObject().apply {
            put("from",       config.nodeName)
            put("type",       "heartbeat")
            put("timestamp",  Instant.now().toString())
            put("seat_status","alive")
            put("seat_type",  config.seatType)
        }
        runCatching {
            post("http://${config.saraAddress}:${config.saraPort}/heartbeat", payload.toString())
            Log.i(tag, "Heartbeat sent")
        }.onFailure { Log.w(tag, "Heartbeat failed: ${it.message}") }
    }

    fun post(url: String, body: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.outputStream.write(body.toByteArray())
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }
}
