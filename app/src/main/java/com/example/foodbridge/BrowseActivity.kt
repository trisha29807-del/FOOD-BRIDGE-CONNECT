package com.example.foodbridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.math.*

class BrowseActivity : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val db = FirebaseFirestore.getInstance()
    private var allListings = mutableListOf<DocumentSnapshot>()
    private var selectedFilter = "All"
    private var userLat = 0.0
    private var userLng = 0.0
    private var hasLocation = false
    private lateinit var adapter: FoodAdapter

    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLocationThenLoad() else loadListings()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        etSearch     = findViewById(R.id.etSearch)
        chipGroup    = findViewById(R.id.chipGroup)
        recyclerView = findViewById(R.id.recyclerView)
        layoutEmpty  = findViewById(R.id.layoutEmpty)

        adapter = FoodAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupChips()
        setupSearch()
        setupBottomNav()
        requestLocationThenLoad()
    }

    // ── Location ──────────────────────────────────────────────────────────────

    private fun requestLocationThenLoad() {
        val fine   = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val hasFine   = ContextCompat.checkSelfPermission(this, fine)   == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) fetchLocationThenLoad()
        else locationPermLauncher.launch(arrayOf(fine, coarse))
    }

    private fun fetchLocationThenLoad() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            loadListings(); return
        }
        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    userLat = location.latitude
                    userLng = location.longitude
                    hasLocation = true
                }
                loadListings()
            }
            .addOnFailureListener { loadListings() }
    }

    // ── Haversine distance (km) ───────────────────────────────────────────────

    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }

    // ── Firestore fetch ───────────────────────────────────────────────────────

    private fun loadListings() {
        db.collection("food_listings")
            .whereEqualTo("status", "available")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                allListings = snapshot.documents.toMutableList()

                // Sort by distance if we have user location
                if (hasLocation) {
                    allListings.sortBy { doc ->
                        val lat = doc.getDouble("latitude") ?: 0.0
                        val lng = doc.getDouble("longitude") ?: 0.0
                        if (lat == 0.0 && lng == 0.0) Double.MAX_VALUE
                        else distanceKm(userLat, userLng, lat, lng)
                    }
                }
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load listings", Toast.LENGTH_SHORT).show()
                showEmpty(true)
            }
    }

    // ── Filter + Search ───────────────────────────────────────────────────────

    private fun applyFilters() {
        val query = etSearch.text?.toString()?.trim()?.lowercase().orEmpty()

        val filtered = allListings.filter { doc ->
            val type = doc.getString("foodType") ?: ""
            val matchesFilter = when (selectedFilter) {
                "All"        -> true
                "Cooked"     -> type.contains("Cooked", ignoreCase = true)
                "Fruits/Veg" -> type.contains("Fruit", ignoreCase = true) ||
                        type.contains("Vegetable", ignoreCase = true)
                "Packed"     -> type.contains("Bakery", ignoreCase = true) ||
                        type.contains("Grain", ignoreCase = true) ||
                        type.contains("Beverage", ignoreCase = true)
                "Veg"        -> !type.contains("Non", ignoreCase = true)
                "Non-Veg"    -> type.contains("Non", ignoreCase = true)
                else         -> true
            }
            val matchesSearch = query.isEmpty() ||
                    (doc.getString("foodName") ?.lowercase()?.contains(query) == true) ||
                    (doc.getString("location") ?.lowercase()?.contains(query) == true) ||
                    (doc.getString("foodType") ?.lowercase()?.contains(query) == true)

            matchesFilter && matchesSearch
        }.toMutableList()

        adapter.updateItems(filtered, userLat, userLng, hasLocation)
        showEmpty(filtered.isEmpty())
    }

    private fun showEmpty(empty: Boolean) {
        layoutEmpty.visibility  = if (empty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (empty) View.GONE    else View.VISIBLE
    }

    // ── Chips ─────────────────────────────────────────────────────────────────

    private fun setupChips() {
        val filters = listOf("All", "Veg", "Non-Veg", "Cooked", "Packed", "Fruits/Veg")
        filters.forEach { label ->
            val chip = Chip(this)
            chip.text = label
            chip.isCheckable = true
            chip.isChecked = label == "All"
            chip.chipBackgroundColor = null
            chip.setTextColor(getColor(R.color.green_primary))
            chip.chipStrokeWidth = 2f
            chip.setChipStrokeColorResource(R.color.green_primary)
            chip.setOnClickListener {
                selectedFilter = label
                for (i in 0 until chipGroup.childCount)
                    (chipGroup.getChildAt(i) as Chip).isChecked = false
                chip.isChecked = true
                applyFilters()
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_browse
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> { startActivity(Intent(this, MainActivity::class.java)); finish(); true }
                R.id.nav_donate  -> { startActivity(Intent(this, DonateFoodActivity::class.java)); true }
                R.id.nav_browse  -> true
                R.id.nav_fssai   -> { startActivity(Intent(this, FssaiActivity::class.java)); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                else -> false
            }
        }
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class FoodAdapter(private val items: MutableList<DocumentSnapshot>) :
    RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    private var userLat = 0.0
    private var userLng = 0.0
    private var hasLocation = false

    fun updateItems(
        newItems: List<DocumentSnapshot>,
        lat: Double, lng: Double, hasLoc: Boolean
    ) {
        items.clear(); items.addAll(newItems)
        userLat = lat; userLng = lng; hasLocation = hasLoc
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji:    TextView = view.findViewById(R.id.tvFoodEmoji)
        val tvName:     TextView = view.findViewById(R.id.tvFoodName)
        val tvType:     TextView = view.findViewById(R.id.tvFoodType)
        val tvQuantity: TextView = view.findViewById(R.id.tvFoodQuantity)
        val tvLocation: TextView = view.findViewById(R.id.tvFoodLocation)
        val tvExpiry:   TextView = view.findViewById(R.id.tvFoodExpiry)
        val tvDonor:    TextView = view.findViewById(R.id.tvFoodDonor)
        val tvDistance: TextView = view.findViewById(R.id.tvFoodDistance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = items[position]
        val type = doc.getString("foodType") ?: ""

        holder.tvEmoji.text    = getFoodEmoji(type)
        holder.tvName.text     = doc.getString("foodName") ?: "Unknown"
        holder.tvType.text     = type
        holder.tvQuantity.text = "📦 ${doc.getString("quantity") ?: ""}"
        holder.tvLocation.text = "📍 ${doc.getString("location") ?: "Location not set"}"
        holder.tvDonor.text    = "👤 ${doc.getString("donorName") ?: "Anonymous"}"

        val expiry = doc.getTimestamp("expiryDate")
        if (expiry != null) {
            val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
            holder.tvExpiry.text = "⏰ ${sdf.format(expiry.toDate())}"
        } else {
            holder.tvExpiry.text = ""
        }

        // Show distance if we have location for both user and listing
        val listingLat = doc.getDouble("latitude") ?: 0.0
        val listingLng = doc.getDouble("longitude") ?: 0.0
        if (hasLocation && listingLat != 0.0 && listingLng != 0.0) {
            val dist = distanceKm(userLat, userLng, listingLat, listingLng)
            holder.tvDistance.text = if (dist < 1.0)
                "📌 ${(dist * 1000).toInt()} m away"
            else
                "📌 ${String.format("%.1f", dist)} km away"
            holder.tvDistance.visibility = View.VISIBLE
        } else {
            holder.tvDistance.visibility = View.GONE
        }
    }

    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }

    private fun getFoodEmoji(type: String): String = when {
        type.contains("Cooked",    ignoreCase = true) -> "🍱"
        type.contains("Fruit",     ignoreCase = true) -> "🍎"
        type.contains("Vegetable", ignoreCase = true) -> "🥦"
        type.contains("Grain",     ignoreCase = true) -> "🌾"
        type.contains("Dairy",     ignoreCase = true) -> "🥛"
        type.contains("Bakery",    ignoreCase = true) -> "🍞"
        type.contains("Beverage",  ignoreCase = true) -> "🥤"
        else                                           -> "🍽️"
    }
}
