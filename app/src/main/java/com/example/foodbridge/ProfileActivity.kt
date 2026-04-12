package com.example.foodbridge

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        val prefs        = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val userName     = prefs.getString("user_name", "FoodBridge User") ?: "FoodBridge User"
        val userEmail    = prefs.getString("user_email", "user@example.com") ?: "user@example.com"
        val userRole     = prefs.getString("user_role", "Donor / Seller") ?: "Donor / Seller"
        val userLocation = prefs.getString("user_location", "Mumbai, India") ?: "Mumbai, India"

        val initials = userName.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2).joinToString("").uppercase()
        findViewById<TextView>(R.id.tvAvatar).text          = initials.ifEmpty { "FB" }
        findViewById<TextView>(R.id.tvProfileName).text     = userName
        findViewById<TextView>(R.id.tvProfileRole).text     = "$userRole · $userLocation"
        findViewById<TextView>(R.id.tvProfileEmail).text    = userEmail
        findViewById<TextView>(R.id.tvProfileRoleInfo).text = userRole

        // Edit Profile
        findViewById<MaterialButton>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // My Activity rows
        findViewById<LinearLayout>(R.id.rowMyDonations).setOnClickListener {
            startActivity(Intent(this, MyDonationsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.rowMyOrders).setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.rowHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // FSSAI
        findViewById<LinearLayout>(R.id.rowFssai).setOnClickListener {
            startActivity(Intent(this, FssaiActivity::class.java))
        }

        // Sign Out
        val btnSignOut    = findViewById<LinearLayout>(R.id.btnSignOut)
        val tvSignOutText = findViewById<TextView>(R.id.tvSignOutText)
        val tvSignOutIcon = findViewById<TextView>(R.id.tvSignOutIcon)

        btnSignOut.setOnClickListener {
            tvSignOutText.setTextColor(Color.WHITE)
            tvSignOutIcon.setTextColor(Color.WHITE)
            btnSignOut.setBackgroundColor(Color.parseColor("#C62828"))
            btnSignOut.postDelayed({
                prefs.edit().putBoolean("is_logged_in", false).apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }, 400)
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> { startActivity(Intent(this, MainActivity::class.java)); finish(); true }
                R.id.nav_donate  -> { startActivity(Intent(this, DonateFoodActivity::class.java)); true }
                R.id.nav_browse  -> { startActivity(Intent(this, BrowseActivity::class.java)); true }
                R.id.nav_fssai   -> { startActivity(Intent(this, FssaiActivity::class.java)); true }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }
}