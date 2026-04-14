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

class BrowseActivity : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
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

        adapter = BrowseAdapter(mutableListOf()) { doc -> openPlaceOrder(doc) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

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
                allListings = snapshot.documents.toMutableList()
                applyFilters()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_LONG).show()
                showEmpty(true)
            }
    }

    private fun applyFilters() {
        val query = etSearch.text?.toString()?.trim()?.lowercase().orEmpty()

        val filtered = allListings.filter { doc ->
            val type  = doc.getString("foodType") ?: ""
            val isVeg = doc.getBoolean("isVeg")   // null = old listing without this field

            val matchesFilter = when (selectedFilter) {
                "All"        -> true
                // Veg/Non-Veg use the saved isVeg boolean — accurate
                "Veg"        -> isVeg == true
                "Non-Veg"    -> isVeg == false
                // Category filters use foodType string
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
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFoodName:    TextView       = view.findViewById(R.id.tvFoodName)
        val tvFoodType:    TextView       = view.findViewById(R.id.tvFoodType)
        val tvDonorName:   TextView       = view.findViewById(R.id.tvDonorName)
        val tvLocation:    TextView       = view.findViewById(R.id.tvLocation)
        val tvQuantity:    TextView       = view.findViewById(R.id.tvQuantity)
        val tvExpiry:      TextView       = view.findViewById(R.id.tvExpiry)
        val tvDescription: TextView       = view.findViewById(R.id.tvDescription)
        val tvDistance:    TextView       = view.findViewById(R.id.tvDistance)
        val tvVegBadge:    TextView       = view.findViewById(R.id.tvVegBadge)
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

        // Veg / Non-Veg badge
        when (isVeg) {
            true  -> {
                holder.tvVegBadge.text = "🟢 Veg"
                holder.tvVegBadge.setBackgroundColor(0xFF2E7D32.toInt())
                holder.tvVegBadge.visibility = View.VISIBLE
            }
            false -> {
                holder.tvVegBadge.text = "🔴 Non-Veg"
                holder.tvVegBadge.setBackgroundColor(0xFFC62828.toInt())
                holder.tvVegBadge.visibility = View.VISIBLE
            }
            null  -> holder.tvVegBadge.visibility = View.GONE
        }

        val expiry = doc.getTimestamp("expiryDate")
        holder.tvExpiry.text = if (expiry != null) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(expiry.toDate())
        } else "N/A"

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