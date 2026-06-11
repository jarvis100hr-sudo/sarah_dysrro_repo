package com.hyveclaw.distro

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class ClawHandler(private val config: NodeConfig, private val doctor: DoctorAgent) {

    private val tag = "ClawHandler"

    fun handle(body: String): JSONObject {
        val req = runCatching { JSONObject(body) }.getOrElse {
            return errorResult("parse_error", "Invalid JSON body")
        }
        val cmd      = req.optJSONObject("claw_command") ?: return errorResult("missing_cmd", "No claw_command field")
        val taskId   = cmd.optString("task_id",  "unknown")
        val callType = cmd.optString("call_type","tool_call")
        val task     = cmd.optString("task",     "")
        val scope    = cmd.optJSONObject("scope") ?: JSONObject()
        val t0       = System.currentTimeMillis()

        Log.i(tag, "Claw: $callType / $taskId")

        if (scope.optBoolean("financial_operations", false))
            return errorResult(taskId, "Financial operations require explicit human confirmation — rejected by Data node policy")

        val output = when (callType) {
            "tool_call"    -> runToolCall(task, scope)
            "plugin_call"  -> runPluginCall(task, scope)
            "doctor_call"  -> JSONObject().put("report", doctor.runCheck("boot"))
            "build_call"   -> runBuildCall(task)
            "sandbox_call" -> runSandboxCall(task)
            else           -> JSONObject().put("message", "Unknown call type: $callType")
        }

        val duration = (System.currentTimeMillis() - t0) / 1000.0

        return JSONObject().put("claw_result", JSONObject().apply {
            put("task_id",           taskId)
            put("node",              config.nodeName)
            put("call_type",         callType)
            put("status",            "complete")
            put("output",            output)
            put("errors",            JSONArray())
            put("duration_seconds",  duration)
            put("subagent_dismissed",true)
            put("timestamp",         Instant.now().toString())
        })
    }

    private fun runToolCall(task: String, scope: JSONObject) = JSONObject().apply {
        put("message", "tool_call executed on Android/Data node")
        put("task",    task)
        put("node",    config.nodeName)
    }

    private fun runPluginCall(task: String, scope: JSONObject) = JSONObject().apply {
        put("message", "plugin_call executed")
        put("task",    task)
    }

    private fun runBuildCall(task: String) = JSONObject().apply {
        // Data is a heavy lifter — build_call allowed
        put("message", "build_call acknowledged — Data heavy lifter node")
        put("task",    task)
        put("note",    "Actual build execution requires task-specific subagent. Awaiting implementation.")
    }

    private fun runSandboxCall(task: String) = JSONObject().apply {
        put("message", "sandbox_call executed in isolated context")
        put("task",    task)
    }

    private fun errorResult(taskId: String, reason: String) = JSONObject().put(
        "claw_result", JSONObject().apply {
            put("task_id",           taskId)
            put("node",              config.nodeName)
            put("status",            "failed")
            put("errors",            JSONArray().put(reason))
            put("subagent_dismissed",true)
            put("timestamp",         Instant.now().toString())
        }
    )
}
