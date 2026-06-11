package com.hyveclaw.terminal

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        val ctx      = requireContext()
        val prefs    = Config.load(ctx)

        val etHost   = view.findViewById<EditText>(R.id.etSaraHost)
        val etPort   = view.findViewById<EditText>(R.id.etSaraPort)
        val etKey    = view.findViewById<EditText>(R.id.etApiKey)
        val tvInfo   = view.findViewById<TextView>(R.id.tvNodeInfo)
        val btnSave  = view.findViewById<Button>(R.id.btnSaveSettings)
        val btnTest  = view.findViewById<Button>(R.id.btnTestConn)
        val tvResult = view.findViewById<TextView>(R.id.tvTestResult)

        etHost.setText(prefs.getString("sara_host", "100.107.18.12"))
        etPort.setText(prefs.getInt("sara_port", 7800).toString())
        etKey.setText(prefs.getString("anthropic_key", ""))

        tvInfo.text = buildString {
            appendLine("This device:  door (100.126.101.53)")
            appendLine("Seat type:    heavy_lifter")
            appendLine("Role:         elevated access terminal")
            append(    "Network:      tail8667d4.ts.net")
        }

        btnSave.setOnClickListener {
            Config.save(
                ctx,
                etHost.text.toString().trim(),
                etPort.text.toString().toIntOrNull() ?: 7800,
                etKey.text.toString().trim()
            )
            Toast.makeText(ctx, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        btnTest.setOnClickListener {
            tvResult.text = "Testing…"
            tvResult.setTextColor(0xFFEAB308.toInt())
            val url = "http://${etHost.text.toString().trim()}:${etPort.text.toString().trim()}"
            HyveClient(ctx).nodeStatus(url) { result ->
                requireActivity().runOnUiThread {
                    if (result != null) {
                        tvResult.text = "Sara ALIVE — ${result.optString("status","?")} | ${result.optString("seat_type","?")}"
                        tvResult.setTextColor(0xFF22C55E.toInt())
                    } else {
                        tvResult.text = "Sara UNREACHABLE — check host/port and Tailscale"
                        tvResult.setTextColor(0xFFEF4444.toInt())
                    }
                }
            }
        }
    }
}
