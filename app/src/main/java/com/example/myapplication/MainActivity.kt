package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val isLoggedIn = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
            .getBoolean("is_logged_in", false)

        if (!isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        findViewById<NestedScrollView>(R.id.nestedScrollView)
            .isNestedScrollingEnabled = true

        setupButtons()
        setupBottomNav()
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.btnDonateFood).setOnClickListener {
            startActivity(Intent(this, DonateFoodActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnFindFood).setOnClickListener {
            android.widget.Toast.makeText(this, "Find Food coming soon!", android.widget.Toast.LENGTH_SHORT).show()
        }
        findViewById<CardView>(R.id.cardUploadFood).setOnClickListener {
            startActivity(Intent(this, DonateFoodActivity::class.java))
        }
        findViewById<CardView>(R.id.cardNearbyFood).setOnClickListener {
            android.widget.Toast.makeText(this, "Nearby Food coming soon!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_donate -> {
                    startActivity(Intent(this, DonateFoodActivity::class.java))
                    true
                }
                R.id.nav_browse -> {
                    android.widget.Toast.makeText(this, "Browse coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_fssai -> {
                    startActivity(Intent(this, FssaiActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}