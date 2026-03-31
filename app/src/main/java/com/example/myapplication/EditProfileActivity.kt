package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etContact: TextInputEditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        initViews()
        loadCurrentData()
        setupRoleSpinner()
        setupSaveButton()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        etName     = findViewById(R.id.etName)
        etEmail    = findViewById(R.id.etEmail)
        etLocation = findViewById(R.id.etLocation)
        etContact  = findViewById(R.id.etContact)
        spinnerRole = findViewById(R.id.spinnerRole)
        btnSave    = findViewById(R.id.btnSaveProfile)
    }

    private fun loadCurrentData() {
        val prefs = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        etName.setText(prefs.getString("user_name", ""))
        etEmail.setText(prefs.getString("user_email", ""))
        etLocation.setText(prefs.getString("user_location", "Mumbai, India"))
        etContact.setText(prefs.getString("user_contact", ""))
    }

    private fun setupRoleSpinner() {
        val roles = listOf("Donor / Seller", "NGO / Orphanage", "Buyer")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        // Set current role
        val prefs = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val currentRole = prefs.getString("user_role", "Donor / Seller") ?: "Donor / Seller"
        val index = roles.indexOf(currentRole)
        if (index >= 0) spinnerRole.setSelection(index)
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            val name     = etName.text?.toString()?.trim().orEmpty()
            val email    = etEmail.text?.toString()?.trim().orEmpty()
            val location = etLocation.text?.toString()?.trim().orEmpty()
            val contact  = etContact.text?.toString()?.trim().orEmpty()
            val role     = spinnerRole.selectedItem.toString()

            // Validate
            if (name.isEmpty()) {
                etName.error = "Name is required"; return@setOnClickListener
            }
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Enter a valid email"; return@setOnClickListener
            }
            if (contact.isNotEmpty() && contact.length < 10) {
                etContact.error = "Enter a valid contact number"; return@setOnClickListener
            }

            // Save to SharedPreferences
            getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
                .edit()
                .putString("user_name", name)
                .putString("user_email", email)
                .putString("user_location", location)
                .putString("user_contact", contact)
                .putString("user_role", role)
                .apply()

            android.widget.Toast.makeText(this, "Profile updated successfully! ✅", android.widget.Toast.LENGTH_SHORT).show()

            // Go back to Profile
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}