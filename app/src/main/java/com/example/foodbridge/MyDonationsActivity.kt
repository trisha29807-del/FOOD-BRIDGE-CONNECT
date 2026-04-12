package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class MyDonationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_donations)

        recyclerView  = findViewById(R.id.recyclerView)
        layoutEmpty   = findViewById(R.id.layoutEmpty)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadMyDonations()
    }

    private fun loadMyDonations() {
        val prefs    = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val myName   = prefs.getString("user_name", "") ?: ""
        val json     = prefs.getString("food_listings", "[]") ?: "[]"
        val jsonArray = JSONArray(json)

        val myItems = mutableListOf<FoodItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.optString("donor") == myName) {
                myItems.add(FoodItem(
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
        }

        if (myItems.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutEmpty.visibility  = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmpty.visibility  = View.GONE
            recyclerView.adapter    = DonationAdapter(myItems) { item ->
                // Delete donation
                deleteDonation(item.id)
            }
        }
    }

    private fun deleteDonation(id: Long) {
        val prefs     = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val json      = prefs.getString("food_listings", "[]") ?: "[]"
        val jsonArray = JSONArray(json)
        val newArray  = JSONArray()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getLong("id") != id) newArray.put(obj)
        }

        prefs.edit().putString("food_listings", newArray.toString()).apply()
        android.widget.Toast.makeText(this, "Donation removed", android.widget.Toast.LENGTH_SHORT).show()
        loadMyDonations()
    }
}

class DonationAdapter(
    private val items: List<FoodItem>,
    private val onDelete: (FoodItem) -> Unit
) : RecyclerView.Adapter<DonationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji:    TextView = view.findViewById(R.id.tvFoodEmoji)
        val tvName:     TextView = view.findViewById(R.id.tvFoodName)
        val tvType:     TextView = view.findViewById(R.id.tvFoodType)
        val tvQuantity: TextView = view.findViewById(R.id.tvFoodQuantity)
        val tvLocation: TextView = view.findViewById(R.id.tvFoodLocation)
        val tvExpiry:   TextView = view.findViewById(R.id.tvFoodExpiry)
        val tvDonor:    TextView = view.findViewById(R.id.tvFoodDonor)
        val btnDelete:  TextView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_donation_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvEmoji.text    = getFoodEmoji(item.type)
        holder.tvName.text     = item.name
        holder.tvType.text     = item.type
        holder.tvQuantity.text = "📦 ${item.quantity}"
        holder.tvLocation.text = "📍 ${item.location}"
        holder.tvExpiry.text   = if (item.expiry.isNotEmpty()) "⏰ ${item.expiry}" else ""
        holder.tvDonor.text    = "👤 ${item.donor}"
        holder.btnDelete.setOnClickListener { onDelete(item) }
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