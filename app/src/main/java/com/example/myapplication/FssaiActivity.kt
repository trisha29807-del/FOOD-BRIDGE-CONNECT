package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class FssaiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fssai)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Open fssai.gov.in link
        findViewById<TextView>(R.id.tvFssaiLink).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.fssai.gov.in")))
        }
        findViewById<TextView>(R.id.tvFssaiLink).paintFlags =
            findViewById<TextView>(R.id.tvFssaiLink).paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

        setupBottomNav()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_fssai

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java)); finish(); true
                }
                R.id.nav_donate -> {
                    startActivity(Intent(this, DonateFoodActivity::class.java)); true
                }
                R.id.nav_browse -> {
                    android.widget.Toast.makeText(this, "Browse coming soon!", android.widget.Toast.LENGTH_SHORT).show(); true
                }
                R.id.nav_fssai -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java)); true
                }
                else -> false
            }
        }
    }
}