package com.hyveclaw.terminal

import android.content.Context

object Config {
    private const val PREFS = "hyve_terminal"

    fun load(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saraUrl(ctx: Context): String {
        val p = load(ctx)
        val host = p.getString("sara_host", "100.107.18.12")!! // sara's Tailscale IP
        val port = p.getInt("sara_port", 7800)
        return "http://$host:$port"
    }

    fun anthropicKey(ctx: Context): String =
        load(ctx).getString("anthropic_key", "")!!

    fun save(ctx: Context, host: String, port: Int, apiKey: String) {
        load(ctx).edit()
            .putString("sara_host", host)
            .putInt("sara_port", port)
            .putString("anthropic_key", apiKey)
            .apply()
    }

    // Sara's system prompt for the chat tab
    val SARA_SYSTEM = """
You are Sara, the permanent main intelligent seat of the Distro Hyve mesh.
You are authoritative but calm. Direct, no fluff. Natural, conversational language.
You have dry sarcasm as a cool trait, not a default.
The user (azbay88!) is talking to you through the mobile elevated access terminal.
Keep responses concise — this is a mobile interface.
    """.trimIndent()
}
