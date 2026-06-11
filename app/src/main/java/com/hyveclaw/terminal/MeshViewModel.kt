package com.hyveclaw.terminal

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class NodeStatus(
    val def: NodeDef,
    val alive: Boolean,
    val latencyMs: Long,
    val checking: Boolean = false
)

class MeshViewModel : ViewModel() {

    val nodes = MutableLiveData<List<NodeStatus>>()

    fun defaultNodes(ctx: Context): List<NodeDef> {
        val saraUrl = Config.saraUrl(ctx)
        // door (this device) connects inward, so its url resolves via Tailscale
        return listOf(
            NodeDef("sara",   saraUrl,                          "heavy_lifter"),
            NodeDef("data",   "http://100.126.101.53:7801",     "heavy_lifter"),
            NodeDef("box",    "http://100.111.116.28:7801",     "limited"),
            NodeDef("nim",    "http://100.114.214.53:7801",     "limited"),
            NodeDef("orange", "http://100.120.205.125:7801",    "heavy_lifter"),
        )
    }

    fun refresh(ctx: Context, client: HyveClient) {
        val nodeList = defaultNodes(ctx)
        nodes.postValue(nodeList.map { NodeStatus(it, false, -1, checking = true) })
        client.pingAll(nodeList) { def, alive, latency ->
            val current = nodes.value?.toMutableList() ?: return@pingAll
            val idx = current.indexOfFirst { it.def.name == def.name }
            if (idx >= 0) current[idx] = NodeStatus(def, alive, latency, checking = false)
            nodes.postValue(current)
        }
    }
}
