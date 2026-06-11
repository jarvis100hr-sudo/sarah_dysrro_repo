package com.hyveclaw.distro

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.time.Instant

class HyveServer(
    port: Int,
    private val config: NodeConfig,
    private val handshake: HandshakeManager,
    private val claw: ClawHandler,
    private val doctor: DoctorAgent
) : NanoHTTPD(port) {

    private val tag = "HyveServer"

    override fun serve(session: IHTTPSession): Response {
        val uri    = session.uri
        val method = session.method

        Log.d(tag, "$method $uri")

        return runCatching {
            when {
                method == Method.GET  && uri == "/status"           -> handleStatus()
                method == Method.POST && uri == "/ping"             -> handlePing(session)
                method == Method.POST && uri == "/claw"             -> handleClaw(session)
                method == Method.POST && uri == "/doctor"           -> handleDoctor(session)
                method == Method.POST && uri == "/heartbeat/ack"    -> handleHeartbeatAck()
                method == Method.POST && uri == "/handshake/confirm"-> handleConfirmAck(session)
                else -> json(404, mapOf("error" to "not found"))
            }
        }.getOrElse { e ->
            Log.e(tag, "Handler error: ${e.message}")
            json(500, mapOf("error" to (e.message ?: "internal error")))
        }
    }

    private fun handleStatus(): Response {
        return json(200, mapOf(
            "node"        to config.nodeName,
            "seat_type"   to config.seatType,
            "status"      to "alive",
            "timestamp"   to Instant.now().toString(),
            "sara"        to "${config.saraAddress}:${config.saraPort}",
            "local_port"  to config.localPort,
            "network"     to config.network
        ))
    }

    private fun handlePing(session: IHTTPSession): Response {
        val body   = readBody(session)
        val result = handshake.handlePing(body)
        return rawJson(200, result.toString())
    }

    private fun handleClaw(session: IHTTPSession): Response {
        val body   = readBody(session)
        val result = claw.handle(body)
        return rawJson(200, result.toString())
    }

    private fun handleDoctor(session: IHTTPSession): Response {
        val body      = readBody(session)
        val checkType = runCatching { JSONObject(body).optString("check_type", "boot") }.getOrDefault("boot")
        val report    = doctor.runCheck(checkType)
        return rawJson(200, JSONObject().put("report", report).toString())
    }

    private fun handleHeartbeatAck(): Response {
        Log.i(tag, "Heartbeat acknowledged by Sara")
        return json(200, mapOf("status" to "ack_received", "node" to config.nodeName))
    }

    private fun handleConfirmAck(session: IHTTPSession): Response {
        Log.i(tag, "Handshake confirm received")
        return json(200, mapOf("status" to "confirm_received"))
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun readBody(session: IHTTPSession): String {
        val len = session.headers["content-length"]?.toIntOrNull() ?: return ""
        val buf = ByteArray(len)
        session.inputStream.read(buf, 0, len)
        return String(buf)
    }

    private fun json(code: Int, map: Map<String, Any>): Response {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return rawJson(code, obj.toString())
    }

    private fun rawJson(code: Int, body: String): Response {
        val status = Response.Status.lookup(code) ?: Response.Status.OK
        return newFixedLengthResponse(status, "application/json", body)
    }
}
