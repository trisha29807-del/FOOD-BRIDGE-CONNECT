package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!FirebaseHelper.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setupButtons()
        setupBottomNav()
    }

    private fun setupButtons() {
        // Hero buttons
        findViewById<MaterialButton>(R.id.btnDonateFood).setOnClickListener {
            startActivity(Intent(this, DonateFoodActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnFindFood).setOnClickListener {
            // TODO: open BrowseActivity when built
            startActivity(Intent(this, BrowseActivity::class.java))
        }

        // Quick action cards
        findViewById<android.view.View>(R.id.cardUploadFood).setOnClickListener {
            startActivity(Intent(this, DonateFoodActivity::class.java))
        }

        findViewById<android.view.View>(R.id.cardNearbyFood).setOnClickListener {
            startActivity(Intent(this, BrowseActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        // Your XML uses bottomNav
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home    -> true
                    R.id.nav_donate  -> {
                        startActivity(Intent(this, DonateFoodActivity::class.java)); true
                    }
                    R.id.nav_fssai   -> {
                        startActivity(Intent(this, FssaiActivity::class.java)); true
                    }
                    R.id.nav_profile -> {
                        startActivity(Intent(this, ProfileActivity::class.java)); true
                    }
                    R.id.nav_browse -> {
                        startActivity(Intent(this, BrowseActivity::class.java)); true
                    }
                    else -> false
                }
            }
    }
}