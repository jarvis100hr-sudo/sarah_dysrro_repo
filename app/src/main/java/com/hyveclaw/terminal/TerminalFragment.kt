package com.hyveclaw.terminal

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TerminalFragment : Fragment() {

    private lateinit var output: TextView
    private lateinit var input:  EditText
    private lateinit var scroll: ScrollView
    private lateinit var client: HyveClient

    private val history = mutableListOf<String>()
    private var histIdx = -1

    // Selected node for routing commands
    private var targetNode = "sara"
    private var targetUrl  = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View =
        inflater.inflate(R.layout.fragment_terminal, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        output = view.findViewById(R.id.tvOutput)
        input  = view.findViewById(R.id.etInput)
        scroll = view.findViewById(R.id.scrollOutput)
        client = HyveClient(requireContext())

        targetUrl = Config.saraUrl(requireContext())

        print("DISTRO HYVE — ELEVATED TERMINAL v1.0")
        print("Node: door (100.126.101.53)  |  Sara: 100.107.18.12")
        print("Type 'help' for available commands.")
        print("")

        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEND || action == EditorInfo.IME_ACTION_DONE) {
                val cmd = input.text.toString().trim()
                if (cmd.isNotEmpty()) { execute(cmd); input.setText("") }
                true
            } else false
        }

        view.findViewById<ImageButton>(R.id.btnSend).setOnClickListener {
            val cmd = input.text.toString().trim()
            if (cmd.isNotEmpty()) { execute(cmd); input.setText("") }
        }
    }

    private fun execute(raw: String) {
        history.add(0, raw); histIdx = -1
        print("> $raw")

        val parts = raw.trim().split("\\s+".toRegex())
        val cmd   = parts[0].lowercase()
        val args  = parts.drop(1)

        when (cmd) {
            "help"   -> showHelp()
            "clear"  -> { output.text = ""; return }
            "status" -> doStatus()
            "ping"   -> doPing(args)
            "node"   -> doNode(args)
            "claw"   -> doClaw(args)
            "doctor" -> doDoctor(args)
            "mesh"   -> print("Use the Mesh tab to view all node statuses.")
            "chat"   -> print("Use the Chat tab to talk to Sara.")
            else     -> print("Unknown command: $cmd  (type 'help')")
        }
    }

    private fun showHelp() {
        print("""
COMMANDS
--------
status              — ping current target node
ping <node>         — ping a specific node by name
node <name|url>     — switch target node (default: sara)
claw <type> <task>  — send a claw command to target node
                      types: tool_call plugin_call doctor_call build_call
doctor [type]       — run doctor check on target node (boot|heartbeat)
clear               — clear terminal
mesh                — go to Mesh tab
chat                — go to Chat tab

NODES
-----
sara    100.107.18.12:7800
data    100.126.101.53:7801  (this device)
box     100.111.116.28:7801
nim     100.114.214.53:7801
orange  100.120.205.125:7801
        """.trimIndent())
    }

    private fun doStatus() {
        print("Pinging $targetNode ($targetUrl)…")
        client.nodeStatus(targetUrl) { result ->
            requireActivity().runOnUiThread {
                if (result == null) print("$targetNode — UNREACHABLE")
                else print("$targetNode — ${result.optString("status","?")} | ${result.optString("seat_type","?")} | port ${result.optInt("local_port",0)}")
            }
        }
    }

    private fun doPing(args: List<String>) {
        val name = args.firstOrNull() ?: targetNode
        val url  = nodeUrl(name)
        print("Pinging $name ($url)…")
        val t0 = System.currentTimeMillis()
        client.nodeStatus(url) { result ->
            requireActivity().runOnUiThread {
                val ms = System.currentTimeMillis() - t0
                if (result == null) print("$name — TIMEOUT / UNREACHABLE")
                else print("$name — ALIVE  ${ms}ms")
            }
        }
    }

    private fun doNode(args: List<String>) {
        val target = args.firstOrNull() ?: run { print("Usage: node <name|url>"); return }
        targetNode = target
        targetUrl  = nodeUrl(target)
        print("Target → $targetNode ($targetUrl)")
    }

    private fun doClaw(args: List<String>) {
        if (args.size < 2) { print("Usage: claw <call_type> <task description>"); return }
        val callType = args[0]
        val task     = args.drop(1).joinToString(" ")
        print("Sending $callType to $targetNode…")
        client.sendClaw(targetUrl, callType, task) { result ->
            requireActivity().runOnUiThread { print(result) }
        }
    }

    private fun doDoctor(args: List<String>) {
        val checkType = args.firstOrNull() ?: "boot"
        print("Running doctor ($checkType) on $targetNode…")
        client.doctorCall(targetUrl, checkType) { result ->
            requireActivity().runOnUiThread { print(result) }
        }
    }

    private fun nodeUrl(name: String) = when (name.lowercase()) {
        "sara"   -> Config.saraUrl(requireContext())
        "data"   -> "http://100.126.101.53:7801"
        "box"    -> "http://100.111.116.28:7801"
        "nim"    -> "http://100.114.214.53:7801"
        "orange" -> "http://100.120.205.125:7801"
        else     -> if (name.startsWith("http")) name else "http://$name"
    }

    private fun print(line: String) {
        requireActivity().runOnUiThread {
            val ts  = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            val cur = output.text.toString()
            val new = if (cur.isEmpty()) "[$ts] $line" else "$cur\n[$ts] $line"
            output.text = new
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
