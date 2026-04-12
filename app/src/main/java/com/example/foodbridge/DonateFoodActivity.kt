package com.example.foodbridge

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DonateFoodActivity : AppCompatActivity() {

    private lateinit var etFoodName: TextInputEditText
    private lateinit var etQuantity: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etExpiry: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerFoodType: Spinner
    private lateinit var btnListFood: MaterialButton
    private lateinit var btnAddPhoto: LinearLayout
    private lateinit var ivFoodPhoto: ImageView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedExpiryMillis: Long = 0L
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var donorLat: Double = 0.0
    private var donorLng: Double = 0.0

    // ── Camera launcher ───────────────────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            showImagePreview(selectedImageUri!!)
        }
    }

    // ── Gallery launcher ──────────────────────────────────────────────────────
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            showImagePreview(uri)
        }
    }

    // ── Camera permission ─────────────────────────────────────────────────────
    private val cameraPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    // ── Location permission ───────────────────────────────────────────────────
    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLocation()
        else Toast.makeText(this,
            "Location permission denied — coordinates won't be saved",
            Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate_food)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        bindViews()
        setupFoodTypeSpinner()
        setupExpiryPicker()
        setupPhotoButton()
        setupSubmit()
        setupBottomNav()
        requestLocationAndFetch()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun bindViews() {
        etFoodName      = findViewById(R.id.etFoodName)
        etQuantity      = findViewById(R.id.etQuantity)
        etLocation      = findViewById(R.id.etLocation)
        etExpiry        = findViewById(R.id.etExpiry)
        etDescription   = findViewById(R.id.etDescription)
        spinnerFoodType = findViewById(R.id.spinnerFoodType)
        btnListFood     = findViewById(R.id.btnListFood)
        btnAddPhoto     = findViewById(R.id.btnAddPhoto)
        ivFoodPhoto     = findViewById(R.id.ivFoodPhoto)
    }

    // ── Location ──────────────────────────────────────────────────────────────

    private fun requestLocationAndFetch() {
        val fine   = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val hasFine   = ContextCompat.checkSelfPermission(this, fine)   == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) fetchLocation()
        else locationPermLauncher.launch(arrayOf(fine, coarse))
    }

    private fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    donorLat = location.latitude
                    donorLng = location.longitude
                    // Pre-fill location field with coordinates if empty
                    if (etLocation.text.isNullOrEmpty()) {
                        etLocation.setText("${String.format("%.4f", donorLat)}, ${String.format("%.4f", donorLng)}")
                    }
                }
            }
    }

    // ── Food type spinner ─────────────────────────────────────────────────────

    private fun setupFoodTypeSpinner() {
        val types = listOf(
            "Cooked Food", "Raw Vegetables", "Fruits",
            "Grains", "Dairy", "Bakery", "Beverages", "Other"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFoodType.adapter = adapter
    }

    // ── Expiry picker ─────────────────────────────────────────────────────────

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

    // ── Photo button ──────────────────────────────────────────────────────────

    private fun setupPhotoButton() {
        btnAddPhoto.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Add food photo")
                .setItems(arrayOf("Take photo", "Choose from gallery")) { _, which ->
                    when (which) {
                        0 -> {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) openCamera()
                            else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                        }
                        1 -> galleryLauncher.launch("image/*")
                    }
                }.show()
        }
    }

    private fun openCamera() {
        val imageFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "food_${System.currentTimeMillis()}.jpg"
        )
        cameraImageUri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", imageFile
        )
        cameraLauncher.launch(cameraImageUri)
    }

    private fun showImagePreview(uri: Uri) {
        btnAddPhoto.visibility = View.GONE
        ivFoodPhoto.setImageURI(uri)
        ivFoodPhoto.visibility = View.VISIBLE
    }

    // ── Image upload ──────────────────────────────────────────────────────────

    private suspend fun uploadImage(uid: String): String {
        val uri = selectedImageUri ?: return ""
        val ref = FirebaseStorage.getInstance().reference
            .child("food_images/$uid/${System.currentTimeMillis()}.jpg")
        return try {
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) { "" }
    }

    // ── Submit listing ────────────────────────────────────────────────────────

    private fun setupSubmit() {
        btnListFood.setOnClickListener {
            val foodName    = etFoodName.text.toString().trim()
            val quantity    = etQuantity.text.toString().trim()
            val location    = etLocation.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val foodType    = spinnerFoodType.selectedItem.toString()

            var isValid = true
            if (foodName.isEmpty()) { etFoodName.error = "Food name is required"; isValid = false }
            else { etFoodName.error = null }
            if (quantity.isEmpty()) { etQuantity.error = "Quantity is required"; isValid = false }
            else { etQuantity.error = null }
            if (location.isEmpty()) { etLocation.error = "Location is required"; isValid = false }
            else { etLocation.error = null }
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

                val imageUrl = if (selectedImageUri != null) uploadImage(uid) else ""

                val listing = mapOf(
                    "donorUid"    to uid,
                    "donorName"   to donorName,
                    "foodName"    to foodName,
                    "foodType"    to foodType,
                    "quantity"    to quantity,
                    "location"    to location,
                    "description" to description,
                    "status"      to "available",
                    "imageUrl"    to imageUrl,
                    "latitude"    to donorLat,   // ← GPS coordinates saved here
                    "longitude"   to donorLng,
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
                            "Failed to post. Try again.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun setupBottomNav() {
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
