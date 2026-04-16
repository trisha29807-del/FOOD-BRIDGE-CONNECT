package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ImageView

class BrowseActivity : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: BrowseAdapter

    private val db = FirebaseFirestore.getInstance()
    private var allListings = mutableListOf<DocumentSnapshot>()
    private var selectedFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        etSearch     = findViewById(R.id.etSearch)
        chipGroup    = findViewById(R.id.chipGroup)
        recyclerView = findViewById(R.id.recyclerView)
        layoutEmpty  = findViewById(R.id.layoutEmpty)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        adapter = BrowseAdapter(mutableListOf()) { doc -> openPlaceOrder(doc) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Pull to refresh
        swipeRefresh.setColorSchemeColors(getColor(R.color.green_primary))
        swipeRefresh.setOnRefreshListener {
            loadListings()
        }

        setupChips()
        setupSearch()
        setupBottomNav()
        loadListings()
    }

    override fun onResume() {
        super.onResume()
        loadListings()
    }

    private fun openPlaceOrder(doc: DocumentSnapshot) {
        val intent = Intent(this, PlaceOrderActivity::class.java).apply {
            putExtra("listingId", doc.id)
            putExtra("foodName",  doc.getString("foodName")  ?: "")
            putExtra("donorUid",  doc.getString("donorUid")  ?: "")
            putExtra("donorName", doc.getString("donorName") ?: "")
            putExtra("quantity",  doc.getString("quantity")  ?: "")
            putExtra("location",  doc.getString("location")  ?: "")
            putExtra("foodType",  doc.getString("foodType")  ?: "")
        }
        startActivity(intent)
    }

    private fun loadListings() {
        db.collection("food_listings")
            .whereEqualTo("status", "available")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                swipeRefresh.isRefreshing = false
                // Filter out expired listings client-side
                val now = com.google.firebase.Timestamp.now()
                allListings = snapshot.documents.filter { doc ->
                    val expiry = doc.getTimestamp("expiryDate")
                    expiry == null || expiry.compareTo(now) > 0
                }.toMutableList()
                applyFilters()
            }
            .addOnFailureListener { e ->
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_LONG).show()
                showEmpty(true)
            }
    }

    private fun applyFilters() {
        val query = etSearch.text?.toString()?.trim()?.lowercase().orEmpty()

        val filtered = allListings.filter { doc ->
            val type  = doc.getString("foodType") ?: ""
            val isVeg = doc.getBoolean("isVeg")

            val matchesFilter = when (selectedFilter) {
                "All"        -> true
                "Veg"        -> isVeg == true
                "Non-Veg"    -> isVeg == false
                "Cooked"     -> type.contains("Cooked",    ignoreCase = true)
                "Fruits/Veg" -> type.contains("Fruit",     ignoreCase = true) ||
                        type.contains("Vegetable", ignoreCase = true) ||
                        type.contains("Raw",       ignoreCase = true)
                "Packed"     -> type.contains("Bakery",    ignoreCase = true) ||
                        type.contains("Grain",     ignoreCase = true) ||
                        type.contains("Beverage",  ignoreCase = true) ||
                        type.contains("Dairy",     ignoreCase = true)
                else         -> true
            }

            val matchesSearch = query.isEmpty() ||
                    (doc.getString("foodName") ?.lowercase()?.contains(query) == true) ||
                    (doc.getString("location") ?.lowercase()?.contains(query) == true) ||
                    (doc.getString("foodType") ?.lowercase()?.contains(query) == true) ||
                    (doc.getString("donorName")?.lowercase()?.contains(query) == true)

            matchesFilter && matchesSearch
        }.toMutableList()

        adapter.updateItems(filtered)
        showEmpty(filtered.isEmpty())
    }

    private fun showEmpty(empty: Boolean) {
        layoutEmpty.visibility  = if (empty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (empty) View.GONE    else View.VISIBLE
    }

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

class BrowseAdapter(
    private val items: MutableList<DocumentSnapshot>,
    private val onOrder: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<BrowseAdapter.ViewHolder>() {

    fun updateItems(newItems: List<DocumentSnapshot>) {
        items.clear(); items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFoodImage:   ImageView      = view.findViewById(R.id.ivFoodImage)
        val tvFoodName:    TextView       = view.findViewById(R.id.tvFoodName)
        val tvFoodType:    TextView       = view.findViewById(R.id.tvFoodType)
        val tvDonorName:   TextView       = view.findViewById(R.id.tvDonorName)
        val tvLocation:    TextView       = view.findViewById(R.id.tvLocation)
        val tvQuantity:    TextView       = view.findViewById(R.id.tvQuantity)
        val tvExpiry:      TextView       = view.findViewById(R.id.tvExpiry)
        val tvDescription: TextView       = view.findViewById(R.id.tvDescription)
        val tvDistance:    TextView       = view.findViewById(R.id.tvDistance)
        val tvVegBadge:    TextView       = view.findViewById(R.id.tvVegBadge)
        val tvExpiryWarn:  TextView       = view.findViewById(R.id.tvExpiryWarn)
        val btnClaim:      MaterialButton = view.findViewById(R.id.btnClaim)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_listing, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc  = items[position]
        val type = doc.getString("foodType") ?: ""
        val isVeg = doc.getBoolean("isVeg")

        holder.tvFoodName.text  = doc.getString("foodName")  ?: "Unknown"
        holder.tvFoodType.text  = type
        holder.tvDonorName.text = doc.getString("donorName") ?: "Anonymous"
        holder.tvLocation.text  = doc.getString("location")  ?: "Location not set"
        holder.tvQuantity.text  = doc.getString("quantity")  ?: ""
        holder.tvDistance.visibility = View.GONE

        // Load food image with Glide
        val imageUrl = doc.getString("imageUrl") ?: ""
        if (imageUrl.isNotEmpty()) {
            holder.ivFoodImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivFoodImage)
        } else {
            holder.ivFoodImage.visibility = View.GONE
        }

        // Veg/Non-Veg badge
        when (isVeg) {
            true  -> { holder.tvVegBadge.text = "🟢 Veg";     holder.tvVegBadge.setBackgroundColor(0xFF2E7D32.toInt()); holder.tvVegBadge.visibility = View.VISIBLE }
            false -> { holder.tvVegBadge.text = "🔴 Non-Veg"; holder.tvVegBadge.setBackgroundColor(0xFFC62828.toInt()); holder.tvVegBadge.visibility = View.VISIBLE }
            null  -> holder.tvVegBadge.visibility = View.GONE
        }

        // Expiry + warning
        val expiry = doc.getTimestamp("expiryDate")
        if (expiry != null) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            holder.tvExpiry.text = "⏰ ${sdf.format(expiry.toDate())}"

            // Show warning if expiring within 2 hours
            val twoHoursMs = 2 * 60 * 60 * 1000L
            val timeLeft = expiry.toDate().time - System.currentTimeMillis()
            if (timeLeft in 0..twoHoursMs) {
                holder.tvExpiryWarn.visibility = View.VISIBLE
                holder.tvExpiryWarn.text = "⚠️ Expiring soon! Claim now"
            } else {
                holder.tvExpiryWarn.visibility = View.GONE
            }
        } else {
            holder.tvExpiry.text = "N/A"
            holder.tvExpiryWarn.visibility = View.GONE
        }

        val desc = doc.getString("description")
        if (!desc.isNullOrEmpty()) {
            holder.tvDescription.text = desc
            holder.tvDescription.visibility = View.VISIBLE
        } else {
            holder.tvDescription.visibility = View.GONE
        }

        holder.btnClaim.isEnabled = true
        holder.btnClaim.text = "Order / Claim"
        holder.btnClaim.setOnClickListener { onOrder(doc) }
    }
}
