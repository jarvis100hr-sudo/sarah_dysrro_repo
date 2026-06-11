package com.hyveclaw.terminal

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment

data class ChatMessage(val role: String, val content: String)

class ChatFragment : Fragment() {

    private lateinit var log:    LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input:  EditText
    private lateinit var client: HyveClient
    private val history = mutableListOf<ChatMessage>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View =
        inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        log    = view.findViewById(R.id.chatLog)
        scroll = view.findViewById(R.id.chatScroll)
        input  = view.findViewById(R.id.etChatInput)
        client = HyveClient(requireContext())

        addBubble("sara", "Connected to Sara via elevated terminal. What do you need?")

        view.findViewById<ImageButton>(R.id.btnChatSend).setOnClickListener { send() }
        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEND) { send(); true } else false
        }
    }

    private fun send() {
        val text = input.text.toString().trim()
        if (text.isBlank()) return
        input.setText("")

        addBubble("user", text)
        history.add(ChatMessage("user", text))

        // Typing indicator
        val typing = addBubble("sara", "…")

        client.chatWithSara(history.map { it.role to it.content }) { reply ->
            requireActivity().runOnUiThread {
                log.removeView(typing)
                val saraMsg = addBubble("sara", reply)
                history.add(ChatMessage("assistant", reply))
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun addBubble(role: String, text: String): View {
        val ctx     = requireContext()
        val isSara  = role == "sara"
        val wrapper = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }
        val label = TextView(ctx).apply {
            this.text = if (isSara) "SARA" else "YOU"
            textSize  = 9f
            setTextColor(if (isSara) 0xFF7C6AFF.toInt() else 0xFF3DDBD9.toInt())
            typeface  = android.graphics.Typeface.MONOSPACE
            setPadding(if (isSara) 0 else 24, 0, if (isSara) 24 else 0, 2)
        }
        val bubble = TextView(ctx).apply {
            this.text = text
            textSize  = 14f
            setTextColor(0xFFE8EAF0.toInt())
            typeface  = android.graphics.Typeface.DEFAULT
            setBackgroundColor(if (isSara) 0xFF161920.toInt() else 0xFF1A1D26.toInt())
            setPadding(16, 12, 16, 12)
        }
        wrapper.addView(label)
        wrapper.addView(bubble)
        log.addView(wrapper)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return wrapper
    }
}
