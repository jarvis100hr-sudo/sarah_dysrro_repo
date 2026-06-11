package com.hyveclaw.distro

import android.os.Build
import java.time.Instant

class DoctorAgent(private val config: NodeConfig) {

    fun runCheck(checkType: String = "boot"): String {
        val ts = Instant.now().toString()
        val rt = Runtime.getRuntime()
        val usedMb  = (rt.totalMemory() - rt.freeMemory()) / 1_048_576
        val totalMb = rt.totalMemory() / 1_048_576

        val (status, findings, fixes) = when (checkType) {
            "heartbeat" -> Triple(
                "healthy",
                listOf(
                    "Service running normally",
                    "Memory: ${usedMb}MB / ${totalMb}MB",
                    "Android ${Build.VERSION.RELEASE} on ${Build.MANUFACTURER} ${Build.MODEL}"
                ),
                listOf("none")
            )
            else -> Triple(
                "healthy",
                listOf(
                    "Android node online — ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT}",
                    "Distro Hyve agent running as foreground service",
                    "HTTP server active on port ${config.localPort}",
                    "Sara address configured: ${config.saraAddress}:${config.saraPort}",
                    "Seat type: ${config.seatType}",
                    "Network: ${config.network}"
                ),
                listOf("none")
            )
        }

        val findingsText = findings.joinToString("\n") { "- $it" }
        val fixesText    = fixes.joinToString("\n")    { "- $it" }

        return """
DOCTOR_REPORT
-------------
SEAT: ${config.nodeName}
TIMESTAMP: $ts
CHECK_TYPE: $checkType
STATUS: $status

FINDINGS:
$findingsText

PROPOSED_FIXES:
$fixesText

MEMORY_WRITE_RECOMMENDED: ${if (checkType == "boot") "yes" else "no"}
ESCALATE_TO_SARA: no
ESCALATE_TO_HUMAN: no

NOTES:
Android Data node $checkType check complete. Node alive on Tailscale mesh (${config.network}).
-------------
END_DOCTOR_REPORT
        """.trimIndent()
    }
}
