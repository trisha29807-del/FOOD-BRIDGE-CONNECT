package com.example.foodbridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class FssaiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fssai)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvFssaiLink).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.fssai.gov.in")))
        }

        // XML uses bottomNav
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home    -> { startActivity(Intent(this, MainActivity::class.java)); true }
                    R.id.nav_donate  -> { startActivity(Intent(this, DonateFoodActivity::class.java)); true }
                    R.id.nav_fssai   -> true
                    R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                    else -> false
                }
            }
    }
}
