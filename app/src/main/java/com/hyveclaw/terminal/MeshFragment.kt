package com.hyveclaw.terminal

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class MeshFragment : Fragment() {

    private val vm: MeshViewModel by activityViewModels()
    private lateinit var client: HyveClient
    private lateinit var container: LinearLayout
    private lateinit var tvRefresh: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View =
        inflater.inflate(R.layout.fragment_mesh, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        client    = HyveClient(requireContext())
        container = view.findViewById(R.id.nodeContainer)
        tvRefresh = view.findViewById(R.id.tvLastRefresh)

        view.findViewById<Button>(R.id.btnRefresh).setOnClickListener { refresh() }

        vm.nodes.observe(viewLifecycleOwner) { statuses ->
            container.removeAllViews()
            statuses.forEach { addNodeCard(it) }
        }

        refresh()
    }

    private fun refresh() {
        tvRefresh.text = "Refreshing…"
        vm.refresh(requireContext(), client)
        tvRefresh.text = "Last refresh: just now"
    }

    private fun addNodeCard(s: NodeStatus) {
        val ctx   = requireContext()
        val card  = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
        }

        // Status dot
        val dot = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(20, 20).also { it.marginEnd = 20; it.gravity = android.view.Gravity.CENTER_VERTICAL }
            background = ctx.getDrawable(
                when {
                    s.checking -> android.R.drawable.presence_away
                    s.alive    -> android.R.drawable.presence_online
                    else       -> android.R.drawable.presence_busy
                }
            )
        }

        // Info
        val info = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val name = TextView(ctx).apply {
            text      = s.def.name.uppercase()
            textSize  = 14f
            setTextColor(0xFFE8EAF0.toInt())
            typeface  = android.graphics.Typeface.MONOSPACE
        }
        val detail = TextView(ctx).apply {
            text = buildString {
                append(s.def.seatType)
                append("  •  ")
                append(s.def.url.removePrefix("http://"))
                if (!s.checking && s.alive && s.latencyMs >= 0) append("  •  ${s.latencyMs}ms")
            }
            textSize = 11f
            setTextColor(0xFF6B7280.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
        }
        info.addView(name)
        info.addView(detail)

        // Status label
        val status = TextView(ctx).apply {
            text = when {
                s.checking -> "…"
                s.alive    -> "ALIVE"
                else       -> "DOWN"
            }
            textSize = 11f
            setTextColor(
                when {
                    s.checking -> 0xFFEAB308.toInt()
                    s.alive    -> 0xFF22C55E.toInt()
                    else       -> 0xFFEF4444.toInt()
                }
            )
            typeface = android.graphics.Typeface.MONOSPACE
        }

        card.addView(dot)
        card.addView(info)
        card.addView(status)

        // Divider
        val divider = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(0xFF252830.toInt())
        }

        container.addView(card)
        container.addView(divider)
    }
}
