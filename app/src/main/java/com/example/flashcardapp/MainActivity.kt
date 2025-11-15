package com.example.flashcardapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth

object SupabaseProvider {
    val client = createSupabaseClient(
        supabaseUrl = "https://kgakmjytcakkoihkyxep.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtnYWttanl0Y2Fra29paGt5eGVwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMwMjM2MTcsImV4cCI6MjA3ODU5OTYxN30.779zN-ZEfxZ8drAN9Cokjt0SIC4U6IM05EDN-5roXSE"
    ) {
        install(Auth)
        install(Postgrest)
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inflate bottom navigation menu programmatically
//        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
//        bottomNav.inflateMenu(R.menu.bottom_nav_menu)
//
//        bottomNav.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.navigation_home -> {
//                    // TODO: switch to home fragment
//                    true
//                }
//                R.id.navigation_cards -> {
//                    // TODO: switch to cards fragment
//                    true
//                }
//                R.id.navigation_stats -> {
//                    // TODO: switch to stats fragment
//                    true
//                }
//                R.id.navigation_profile -> {
//                    // TODO: switch to profile fragment
//                    true
//                }
//                else -> false
//            }
//        }
    }
}



//Option A (in-place): add EditTexts to deck_details.xml, update adapter to support editMode, and add the btn_edit toggle/save logic in the fragment/activity.
//Option B (dialog): implement a dialog/bottom sheet to edit a single field or item.