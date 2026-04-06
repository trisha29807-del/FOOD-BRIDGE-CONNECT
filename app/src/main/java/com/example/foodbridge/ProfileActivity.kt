package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvAvatar: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileRole: TextView
    private lateinit var tvProfileEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvAvatar       = findViewById(R.id.tvAvatar)
        tvProfileName  = findViewById(R.id.tvProfileName)
        tvProfileRole  = findViewById(R.id.tvProfileRole)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)

        loadProfile()
        setupButtons()
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        val uid = FirebaseHelper.currentUid ?: return

        lifecycleScope.launch {
            val result = FirebaseHelper.getUserProfile(uid)
            result.fold(
                onSuccess = { data ->
                    val name     = data["name"] as? String ?: "User"
                    val role     = data["role"] as? String ?: ""
                    val location = data["location"] as? String ?: "Location not set"
                    val email    = data["email"] as? String ?: ""

                    tvProfileName.text  = name
                    tvProfileRole.text  = "$role · $location"
                    tvProfileEmail.text = email
                    tvAvatar.text       = name.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2).joinToString("").uppercase()
                },
                onFailure = {
                    tvProfileName.text = "Could not load profile"
                }
            )
        }
    }

    private fun setupButtons() {
        findViewById<android.view.View>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<android.view.View>(R.id.rowFssai).setOnClickListener {
            startActivity(Intent(this, FssaiActivity::class.java))
        }

        // XML uses btnSignOut not rowSignOut
        findViewById<android.view.View>(R.id.btnSignOut).setOnClickListener {
            FirebaseHelper.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupBottomNav() {
        // XML uses bottomNav not bottomNavigationView
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home    -> { startActivity(Intent(this, MainActivity::class.java)); true }
                    R.id.nav_donate  -> { startActivity(Intent(this, DonateFoodActivity::class.java)); true }
                    R.id.nav_fssai   -> { startActivity(Intent(this, FssaiActivity::class.java)); true }
                    R.id.nav_profile -> true
                    else -> false
                }
            }
    }
}
