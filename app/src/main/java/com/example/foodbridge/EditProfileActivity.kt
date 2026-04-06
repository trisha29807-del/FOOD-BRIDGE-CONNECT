package com.example.foodbridge

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var tvAvatarPreview: TextView
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etContact: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        bindViews()
        setupRoleSpinner()
        loadCurrentProfile()
        setupSaveButton()

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun bindViews() {
        tvAvatarPreview = findViewById(R.id.tvAvatarPreview)
        etName          = findViewById(R.id.etName)
        etEmail         = findViewById(R.id.etEmail)
        etContact       = findViewById(R.id.etContact)
        etLocation      = findViewById(R.id.etLocation)
        spinnerRole     = findViewById(R.id.spinnerRole)
        btnSave         = findViewById(R.id.btnSaveProfile)  // XML uses btnSaveProfile not btnSaveChanges
    }

    private fun setupRoleSpinner() {
        val roles = listOf("Donor/Seller", "NGO/Orphanage", "Buyer")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter
    }

    private fun loadCurrentProfile() {
        val uid = FirebaseHelper.currentUid ?: return
        lifecycleScope.launch {
            val result = FirebaseHelper.getUserProfile(uid)
            result.fold(
                onSuccess = { data ->
                    val name     = data["name"] as? String ?: ""
                    val email    = data["email"] as? String ?: ""
                    val contact  = data["contact"] as? String ?: ""
                    val location = data["location"] as? String ?: ""
                    val role     = data["role"] as? String ?: "Donor/Seller"

                    etName.setText(name)
                    etEmail.setText(email)
                    etContact.setText(contact)
                    etLocation.setText(location)

                    tvAvatarPreview.text = name.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2).joinToString("").uppercase()

                    val roles = listOf("Donor/Seller", "NGO/Orphanage", "Buyer")
                    spinnerRole.setSelection(roles.indexOf(role).coerceAtLeast(0))
                },
                onFailure = {
                    Toast.makeText(this@EditProfileActivity,
                        "Could not load profile", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            val name     = etName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val contact  = etContact.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val role     = spinnerRole.selectedItem.toString()

            var isValid = true
            if (name.isEmpty()) {
                etName.error = "Name is required"; isValid = false
            } else { etName.error = null }
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Enter a valid email"; isValid = false
            } else { etEmail.error = null }
            if (!isValid) return@setOnClickListener

            val uid = FirebaseHelper.currentUid ?: return@setOnClickListener

            btnSave.isEnabled = false
            btnSave.text = "Saving..."

            lifecycleScope.launch {
                val updates = mapOf(
                    "name"     to name,
                    "email"    to email,
                    "contact"  to contact,
                    "location" to location,
                    "role"     to role
                )
                val result = FirebaseHelper.updateUserProfile(uid, updates)
                btnSave.isEnabled = true
                btnSave.text = "Save Changes"
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@EditProfileActivity,
                            "Profile updated!", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onFailure = {
                        Toast.makeText(this@EditProfileActivity,
                            "Update failed. Try again.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
