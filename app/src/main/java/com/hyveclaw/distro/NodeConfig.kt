package com.hyveclaw.distro

import android.content.Context

data class NodeConfig(
    val nodeName: String,
    val saraAddress: String,
    val saraPort: Int,
    val localPort: Int,
    val seatType: String,
    val network: String
) {
    companion object {
        private const val PREFS = "distro_hyve_config"

        fun load(context: Context): NodeConfig {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return NodeConfig(
                nodeName    = p.getString("node_name",    "data")!!,
                saraAddress = p.getString("sara_address", "100.107.18.12")!!,
                saraPort    = p.getInt(   "sara_port",    7800),
                localPort   = p.getInt(   "local_port",   7801),
                seatType    = p.getString("seat_type",    "heavy_lifter")!!,
                network     = p.getString("network",      "tail8667d4.ts.net")!!
            )
        }

        fun save(context: Context, config: NodeConfig) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
                putString("node_name",    config.nodeName)
                putString("sara_address", config.saraAddress)
                putInt(   "sara_port",    config.saraPort)
                putInt(   "local_port",   config.localPort)
                putString("seat_type",    config.seatType)
                putString("network",      config.network)
                apply()
            }
        }
    }
}
