package com.example.foodbridge

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

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)

        recyclerView = findViewById(R.id.recyclerView)
        layoutEmpty  = findViewById(R.id.layoutEmpty)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadMyOrders()
    }

    private fun loadMyOrders() {
        val prefs    = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val json     = prefs.getString("my_orders", "[]") ?: "[]"
        val jsonArray = JSONArray(json)

        val orders = mutableListOf<FoodItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            orders.add(FoodItem(
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

        if (orders.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutEmpty.visibility  = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmpty.visibility  = View.GONE
            recyclerView.adapter    = OrdersAdapter(orders)
        }
    }
}

class OrdersAdapter(private val items: List<FoodItem>) :
    RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji:    TextView = view.findViewById(R.id.tvFoodEmoji)
        val tvName:     TextView = view.findViewById(R.id.tvFoodName)
        val tvType:     TextView = view.findViewById(R.id.tvFoodType)
        val tvQuantity: TextView = view.findViewById(R.id.tvFoodQuantity)
        val tvLocation: TextView = view.findViewById(R.id.tvFoodLocation)
        val tvExpiry:   TextView = view.findViewById(R.id.tvFoodExpiry)
        val tvDonor:    TextView = view.findViewById(R.id.tvFoodDonor)
        val tvStatus:   TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_card, parent, false)
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
        holder.tvDonor.text    = "👤 Donor: ${item.donor}"
        holder.tvStatus.text   = "🟡 Pending Pickup"
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