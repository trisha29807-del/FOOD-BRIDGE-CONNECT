package com.example.foodbridge

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DonateFoodActivity : AppCompatActivity() {

    // Your XML has no IDs on TextInputLayouts — only on the EditTexts inside them
    private lateinit var etFoodName: TextInputEditText
    private lateinit var etQuantity: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etExpiry: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerFoodType: Spinner
    private lateinit var btnListFood: MaterialButton

    private var selectedExpiryMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate_food)

        bindViews()
        setupFoodTypeSpinner()
        setupExpiryPicker()
        setupSubmit()
        setupBottomNav()

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun bindViews() {
        etFoodName    = findViewById(R.id.etFoodName)
        etQuantity    = findViewById(R.id.etQuantity)
        etLocation    = findViewById(R.id.etLocation)
        etExpiry      = findViewById(R.id.etExpiry)
        etDescription = findViewById(R.id.etDescription)
        spinnerFoodType = findViewById(R.id.spinnerFoodType)
        btnListFood   = findViewById(R.id.btnListFood)
    }

    private fun setupFoodTypeSpinner() {
        val types = listOf(
            "Cooked Food", "Raw Vegetables", "Fruits",
            "Grains", "Dairy", "Bakery", "Beverages", "Other"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFoodType.adapter = adapter
    }

    private fun setupExpiryPicker() {
        etExpiry.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                TimePickerDialog(this, { _, hour, minute ->
                    cal.set(year, month, day, hour, minute)
                    selectedExpiryMillis = cal.timeInMillis
                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    etExpiry.setText(sdf.format(cal.time))
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupSubmit() {
        btnListFood.setOnClickListener {
            val foodName    = etFoodName.text.toString().trim()
            val quantity    = etQuantity.text.toString().trim()
            val location    = etLocation.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val foodType    = spinnerFoodType.selectedItem.toString()

            // Validation — show errors directly on EditText since no TIL ids
            var isValid = true
            if (foodName.isEmpty()) {
                etFoodName.error = "Food name is required"; isValid = false
            } else { etFoodName.error = null }

            if (quantity.isEmpty()) {
                etQuantity.error = "Quantity is required"; isValid = false
            } else { etQuantity.error = null }

            if (location.isEmpty()) {
                etLocation.error = "Location is required"; isValid = false
            } else { etLocation.error = null }

            if (selectedExpiryMillis == 0L) {
                Toast.makeText(this, "Please select expiry date", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            val uid = FirebaseHelper.currentUid ?: run {
                Toast.makeText(this, "Please sign in again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnListFood.isEnabled = false
            btnListFood.text = "Posting..."

            lifecycleScope.launch {
                val donorName = FirebaseHelper.getUserProfile(uid)
                    .getOrNull()?.get("name") as? String ?: "Anonymous"

                val listing = mapOf(
                    "donorUid"    to uid,
                    "donorName"   to donorName,
                    "foodName"    to foodName,
                    "foodType"    to foodType,
                    "quantity"    to quantity,
                    "location"    to location,
                    "description" to description,
                    "status"      to "available",
                    "imageUrl"    to "",
                    "createdAt"   to Timestamp.now(),
                    "expiryDate"  to Timestamp(Date(selectedExpiryMillis))
                )

                val result = FirebaseHelper.postFoodListing(listing)

                btnListFood.isEnabled = true
                btnListFood.text = "List Food Item"

                result.fold(
                    onSuccess = {
                        Toast.makeText(this@DonateFoodActivity,
                            "Food listed successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onFailure = {
                        Toast.makeText(this@DonateFoodActivity,
                            "Failed to post listing. Try again.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun setupBottomNav() {
        // Your XML uses bottomNav not bottomNavigationView
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home    -> { startActivity(Intent(this, MainActivity::class.java)); true }
                    R.id.nav_donate  -> true
                    R.id.nav_fssai   -> { startActivity(Intent(this, FssaiActivity::class.java)); true }
                    R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                    else -> false
                }
            }
    }
}