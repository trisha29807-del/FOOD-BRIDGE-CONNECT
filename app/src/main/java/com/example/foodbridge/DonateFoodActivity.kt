package com.example.foodbridge

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class DonateFoodActivity : AppCompatActivity() {

    private lateinit var etFoodName: TextInputEditText
    private lateinit var etQuantity: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etExpiry: TextInputEditText
    private lateinit var spinnerFoodType: Spinner
    private lateinit var btnListFood: MaterialButton
    private lateinit var btnAddPhoto: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate_food)

        initViews()
        setupFoodTypeSpinner()
        setupExpiryPicker()
        setupPhotoButton()
        setupListFoodButton()
        setupBottomNav()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        etFoodName    = findViewById(R.id.etFoodName)
        etQuantity    = findViewById(R.id.etQuantity)
        etLocation    = findViewById(R.id.etLocation)
        etDescription = findViewById(R.id.etDescription)
        etExpiry      = findViewById(R.id.etExpiry)
        spinnerFoodType = findViewById(R.id.spinnerFoodType)
        btnListFood   = findViewById(R.id.btnListFood)
        btnAddPhoto   = findViewById(R.id.btnAddPhoto)
    }

    private fun setupFoodTypeSpinner() {
        val foodTypes = listOf("Select type", "Cooked Food", "Raw Vegetables", "Fruits", "Grains", "Dairy", "Bakery", "Beverages", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, foodTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFoodType.adapter = adapter
    }

    private fun setupExpiryPicker() {
        etExpiry.isFocusable = false
        etExpiry.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                TimePickerDialog(this, { _, hour, minute ->
                    val dateStr = String.format("%02d-%02d-%04d %02d:%02d", day, month + 1, year, hour, minute)
                    etExpiry.setText(dateStr)
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupPhotoButton() {
        btnAddPhoto.setOnClickListener {
            Toast.makeText(this, "Camera/Gallery coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListFoodButton() {
        btnListFood.setOnClickListener {
            val foodName = etFoodName.text?.toString()?.trim().orEmpty()
            val quantity = etQuantity.text?.toString()?.trim().orEmpty()
            val location = etLocation.text?.toString()?.trim().orEmpty()
            val description = etDescription.text?.toString()?.trim().orEmpty()
            val expiry = etExpiry.text?.toString()?.trim().orEmpty()
            val foodType = spinnerFoodType.selectedItem?.toString().orEmpty()

            if (foodName.isEmpty()) {
                etFoodName.error = "Food name is required"; return@setOnClickListener
            }
            if (spinnerFoodType.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select a food type", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (quantity.isEmpty()) {
                etQuantity.error = "Quantity is required"; return@setOnClickListener
            }
            if (location.isEmpty()) {
                etLocation.error = "Location is required"; return@setOnClickListener
            }

            // Save food item to SharedPreferences as JSON
            saveFoodItem(foodName, foodType, quantity, location, expiry, description)

            Toast.makeText(this, "🎉 Food listed successfully!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun saveFoodItem(
        name: String, type: String, quantity: String,
        location: String, expiry: String, description: String
    ) {
        val prefs = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val donorName = prefs.getString("user_name", "Anonymous") ?: "Anonymous"

        // Load existing items
        val existingJson = prefs.getString("food_listings", "[]") ?: "[]"
        val jsonArray = JSONArray(existingJson)

        // Create new item
        val item = JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("name", name)
            put("type", type)
            put("quantity", quantity)
            put("location", location)
            put("expiry", expiry)
            put("description", description)
            put("donor", donorName)
            put("timestamp", System.currentTimeMillis())
        }

        // Add to array and save
        jsonArray.put(item)
        prefs.edit().putString("food_listings", jsonArray.toString()).apply()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_donate

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java)); finish(); true
                }
                R.id.nav_donate -> true
                R.id.nav_browse -> {
                    startActivity(Intent(this, BrowseActivity::class.java)); finish(); true
                }
                R.id.nav_fssai -> {
                    startActivity(Intent(this, FssaiActivity::class.java)); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java)); true
                }
                else -> false
            }
        }
    }
}