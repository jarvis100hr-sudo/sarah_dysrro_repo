package com.hyveclaw.terminal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        supportActionBar?.hide()

        if (savedInstanceState == null) loadFragment(TerminalFragment())

        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terminal -> loadFragment(TerminalFragment())
                R.id.nav_mesh     -> loadFragment(MeshFragment())
                R.id.nav_chat     -> loadFragment(ChatFragment())
                R.id.nav_settings -> loadFragment(SettingsFragment())
            }
            true
        }
    }

    private fun loadFragment(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, f)
            .commit()
    }
}
