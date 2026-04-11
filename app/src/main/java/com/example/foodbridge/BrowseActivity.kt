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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray


data class FoodItem(
    val id: Long,
    val name: String,
    val type: String,
    val quantity: String,
    val location: String,
    val expiry: String,
    val description: String,
    val donor: String
)

class BrowseActivity : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    private var allItems = mutableListOf<FoodItem>()
    private var filteredItems = mutableListOf<FoodItem>()
    private var selectedFilter = "All"
    private lateinit var adapter: FoodAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

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
        loadFoodItems()
    }

    override fun onResume() {
        super.onResume()
        loadFoodItems()
    }

    private fun loadFoodItems() {
        val prefs = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val json = prefs.getString("food_listings", "[]") ?: "[]"
        val jsonArray = JSONArray(json)

        allItems.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            allItems.add(FoodItem(
                id          = obj.getLong("id"),
                name        = obj.getString("name"),
                type        = obj.getString("type"),
                quantity    = obj.getString("quantity"),
                location    = obj.getString("location"),
                expiry      = obj.optString("expiry", ""),
                description = obj.optString("description", ""),
                donor       = obj.optString("donor", "Anonymous")
            ))
        }
        applyFilters()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
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
                for (i in 0 until chipGroup.childCount) {
                    (chipGroup.getChildAt(i) as Chip).isChecked = false
                }
                chip.isChecked = true
                applyFilters()
            }
            chipGroup.addView(chip)
        }
    }

    private fun applyFilters() {
        val query = etSearch.text?.toString()?.trim()?.lowercase().orEmpty()

        filteredItems = allItems.filter { item ->
            val matchesSearch = query.isEmpty() ||
                    item.name.lowercase().contains(query) ||
                    item.location.lowercase().contains(query) ||
                    item.type.lowercase().contains(query)

            val matchesFilter = when (selectedFilter) {
                "All"        -> true
                "Cooked"     -> item.type.contains("Cooked", ignoreCase = true)
                "Fruits/Veg" -> item.type.contains("Fruit", ignoreCase = true) ||
                        item.type.contains("Vegetable", ignoreCase = true)
                "Packed"     -> item.type.contains("Bakery", ignoreCase = true) ||
                        item.type.contains("Grain", ignoreCase = true) ||
                        item.type.contains("Beverage", ignoreCase = true)
                "Veg"        -> !item.type.contains("Non", ignoreCase = true)
                "Non-Veg"    -> item.type.contains("Non", ignoreCase = true)
                else         -> true
            }
            matchesSearch && matchesFilter
        }.toMutableList()

        adapter.updateItems(filteredItems)

        if (filteredItems.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
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

class FoodAdapter(private val items: MutableList<FoodItem>) :
    RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    fun updateItems(newItems: List<FoodItem>) {
        items.clear()
        items.addAll(newItems)
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvEmoji.text    = getFoodEmoji(item.type)
        holder.tvName.text     = item.name
        holder.tvType.text     = item.type
        holder.tvQuantity.text = "📦 ${item.quantity}"
        holder.tvLocation.text = "📍 ${item.location}"
        holder.tvExpiry.text   = if (item.expiry.isNotEmpty()) "⏰ Expires: ${item.expiry}" else ""
        holder.tvDonor.text    = "👤 ${item.donor}"
    }

    override fun getItemCount() = items.size

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