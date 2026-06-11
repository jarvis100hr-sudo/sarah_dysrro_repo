package com.hyveclaw.terminal

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

class HyveClient(private val ctx: Context) {

    private val json = "application/json".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Sara dashboard ─────────────────────────────────────────────────────────

    fun chatWithSara(messages: List<Pair<String, String>>, onResult: (String) -> Unit) {
        val key = Config.anthropicKey(ctx)
        if (key.isBlank()) { onResult("ERROR: No API key configured in Settings."); return }

        val messagesArr = JSONArray().also { arr ->
            messages.forEach { (role, content) ->
                arr.put(JSONObject().put("role", role).put("content", content))
            }
        }
        val body = JSONObject()
            .put("model", "claude-sonnet-4-20250514")
            .put("max_tokens", 1024)
            .put("system", Config.SARA_SYSTEM)
            .put("messages", messagesArr)
            .toString()

        val req = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", key)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body.toRequestBody(json))
            .build()

        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onResult("ERROR: ${e.message}")
            override fun onResponse(call: Call, response: Response) {
                val text = runCatching {
                    val obj = JSONObject(response.body!!.string())
                    obj.getJSONArray("content").getJSONObject(0).getString("text")
                }.getOrElse { "ERROR: ${it.message}" }
                onResult(text)
            }
        })
    }

    // ── Node status ────────────────────────────────────────────────────────────

    fun nodeStatus(nodeUrl: String, onResult: (JSONObject?) -> Unit) {
        val req = Request.Builder().url("$nodeUrl/status").get().build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onResult(null)
            override fun onResponse(call: Call, response: Response) {
                onResult(runCatching { JSONObject(response.body!!.string()) }.getOrNull())
            }
        })
    }

    fun pingAll(nodes: List<NodeDef>, onEach: (NodeDef, Boolean, Long) -> Unit) {
        nodes.forEach { node ->
            val t0 = System.currentTimeMillis()
            val ping = JSONObject()
                .put("from", "terminal_elevated")
                .put("to", node.name)
                .put("type", "ping")
                .put("timestamp", Instant.now().toString())
                .put("seat_status", "checking")
                .toString()

            val req = Request.Builder()
                .url("${node.url}/ping")
                .post(ping.toRequestBody(json))
                .build()

            http.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) =
                    onEach(node, false, -1)
                override fun onResponse(call: Call, response: Response) =
                    onEach(node, response.isSuccessful, System.currentTimeMillis() - t0)
            })
        }
    }

    // ── Claw command ───────────────────────────────────────────────────────────

    fun sendClaw(nodeUrl: String, callType: String, task: String, onResult: (String) -> Unit) {
        val taskId = "term_${System.currentTimeMillis()}"
        val cmd = JSONObject().put("claw_command", JSONObject().apply {
            put("from",             "terminal_elevated")
            put("target_node",      nodeUrl.substringAfterLast("/"))
            put("call_type",        callType)
            put("task_id",          taskId)
            put("scope", JSONObject().apply {
                put("read_paths",         org.json.JSONArray().put("distro_hyve-agent/"))
                put("write_paths",        org.json.JSONArray())
                put("external_access",    false)
                put("financial_operations", false)
            })
            put("task",             task)
            put("timeout_seconds",  60)
            put("dismiss_on_complete", true)
        }).toString()

        val req = Request.Builder()
            .url("$nodeUrl/claw")
            .post(cmd.toRequestBody(json))
            .build()

        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onResult("ERROR: ${e.message}")
            override fun onResponse(call: Call, response: Response) {
                onResult(runCatching {
                    val obj = JSONObject(response.body!!.string())
                    obj.optJSONObject("claw_result")?.let {
                        val status = it.optString("status")
                        val output = it.optJSONObject("output")?.toString(2) ?: ""
                        "[$status] $output"
                    } ?: obj.toString(2)
                }.getOrElse { "ERROR: ${it.message}" })
            }
        })
    }

    // ── Doctor call ────────────────────────────────────────────────────────────

    fun doctorCall(nodeUrl: String, checkType: String = "boot", onResult: (String) -> Unit) {
        val body = JSONObject().put("check_type", checkType).toString()
        val req = Request.Builder()
            .url("$nodeUrl/doctor")
            .post(body.toRequestBody(json))
            .build()

        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onResult("ERROR: ${e.message}")
            override fun onResponse(call: Call, response: Response) {
                onResult(runCatching {
                    JSONObject(response.body!!.string()).optString("report", "No report returned")
                }.getOrElse { "ERROR: ${it.message}" })
            }
        })
    }
}

data class NodeDef(val name: String, val url: String, val seatType: String)
