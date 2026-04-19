package com.example.foodbridge

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!FirebaseHelper.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        ExpiryCheckWorker.schedule(this)
        setupButtons()
        setupHowItWorks()
        setupBottomNav()
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.btnDonateFood).setOnClickListener {
            startActivity(Intent(this, DonateFoodActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnFindFood).setOnClickListener {
            startActivity(Intent(this, BrowseActivity::class.java))
        }
        findViewById<android.view.View>(R.id.cardUploadFood).setOnClickListener {
            startActivity(Intent(this, DonateFoodActivity::class.java))
        }
        findViewById<android.view.View>(R.id.cardNearbyFood).setOnClickListener {
            startActivity(Intent(this, BrowseActivity::class.java))
        }

        // FAB cart → opens CartActivity
        findViewById<FloatingActionButton>(R.id.fabCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Chat button in hero section → opens ChatListActivity
        findViewById<ImageButton>(R.id.btnChat).setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }
    }

    private fun setupHowItWorks() {
        findViewById<android.view.View>(R.id.cardStep1).setOnClickListener {
            startActivity(Intent(this, DonateFoodActivity::class.java))
        }
        findViewById<android.view.View>(R.id.cardStep2).setOnClickListener {
            startActivity(Intent(this, BrowseActivity::class.java))
        }
        findViewById<android.view.View>(R.id.cardStep3).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Pickup & Delivery")
                .setMessage(
                    "Once you claim a food listing:\n\n" +
                            "1. Contact the donor via chat\n\n" +
                            "2. Coordinate a pickup time\n\n" +
                            "3. Track your orders in Profile → My Orders\n\n" +
                            "4. Mark as received once collected"
                )
                .setPositiveButton("Track My Orders") { _, _ ->
                    startActivity(Intent(this, TrackOrdersActivity::class.java))
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home    -> true
                    R.id.nav_donate  -> { startActivity(Intent(this, DonateFoodActivity::class.java)); true }
                    R.id.nav_browse  -> { startActivity(Intent(this, BrowseActivity::class.java)); true }
                    R.id.nav_chat    -> { startActivity(Intent(this, ChatListActivity::class.java)); true }
                    R.id.nav_fssai   -> { startActivity(Intent(this, FssaiActivity::class.java)); true }
                    R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                    else -> false
                }
            }
    }
}