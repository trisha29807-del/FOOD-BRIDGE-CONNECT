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
import com.google.android.material.button.MaterialButton
import org.json.JSONArray

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutCheckout: LinearLayout
    private lateinit var tvItemCount: TextView
    private lateinit var btnCheckout: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        recyclerView    = findViewById(R.id.recyclerView)
        layoutEmpty     = findViewById(R.id.layoutEmpty)
        layoutCheckout  = findViewById(R.id.layoutCheckout)
        tvItemCount     = findViewById(R.id.tvItemCount)
        btnCheckout     = findViewById(R.id.btnCheckout)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)

        btnCheckout.setOnClickListener { placeOrder() }

        loadCart()
    }

    private fun loadCart() {
        val prefs = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val json  = prefs.getString("cart_items", "[]") ?: "[]"
        val arr   = JSONArray(json)

        val items = mutableListOf<FoodItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            items.add(FoodItem(
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

        if (items.isEmpty()) {
            recyclerView.visibility   = View.GONE
            layoutEmpty.visibility    = View.VISIBLE
            layoutCheckout.visibility = View.GONE
        } else {
            recyclerView.visibility   = View.VISIBLE
            layoutEmpty.visibility    = View.GONE
            layoutCheckout.visibility = View.VISIBLE
            tvItemCount.text          = "${items.size} item(s) in cart"
            recyclerView.adapter      = CartAdapter(items) { item ->
                removeFromCart(item.id)
            }
        }
    }

    private fun removeFromCart(id: Long) {
        val prefs    = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val json     = prefs.getString("cart_items", "[]") ?: "[]"
        val arr      = JSONArray(json)
        val newArr   = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getLong("id") != id) newArr.put(obj)
        }
        prefs.edit().putString("cart_items", newArr.toString()).apply()
        android.widget.Toast.makeText(this, "Removed from cart", android.widget.Toast.LENGTH_SHORT).show()
        loadCart()
    }

    private fun placeOrder() {
        val prefs      = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val cartJson   = prefs.getString("cart_items", "[]") ?: "[]"
        val cartArr    = JSONArray(cartJson)

        // Move cart items to my_orders
        val ordersJson = prefs.getString("my_orders", "[]") ?: "[]"
        val ordersArr  = JSONArray(ordersJson)

        for (i in 0 until cartArr.length()) {
            val obj = cartArr.getJSONObject(i)
            obj.put("timestamp", System.currentTimeMillis())
            ordersArr.put(obj)
        }

        // Also remove ordered items from food_listings
        val listingsJson = prefs.getString("food_listings", "[]") ?: "[]"
        val listingsArr  = JSONArray(listingsJson)
        val newListings  = JSONArray()
        val cartIds      = (0 until cartArr.length()).map { cartArr.getJSONObject(it).getLong("id") }

        for (i in 0 until listingsArr.length()) {
            val obj = listingsArr.getJSONObject(i)
            if (!cartIds.contains(obj.getLong("id"))) newListings.put(obj)
        }

        prefs.edit()
            .putString("my_orders", ordersArr.toString())
            .putString("cart_items", "[]")
            .putString("food_listings", newListings.toString())
            .apply()

        android.widget.Toast.makeText(this, "🎉 Order placed successfully!", android.widget.Toast.LENGTH_LONG).show()
        loadCart()
    }
}

annotation class FoodItem(
    val id: Long,
    val name: String,
    val type: String,
    val quantity: String,
    val location: String,
    val expiry: String,
    val description: String,
    val donor: String
)

class CartAdapter(
    private val items: List<FoodItem>,
    private val onRemove: (FoodItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji:    TextView = view.findViewById(R.id.tvFoodEmoji)
        val tvName:     TextView = view.findViewById(R.id.tvFoodName)
        val tvType:     TextView = view.findViewById(R.id.tvFoodType)
        val tvQuantity: TextView = view.findViewById(R.id.tvFoodQuantity)
        val tvLocation: TextView = view.findViewById(R.id.tvFoodLocation)
        val tvDonor:    TextView = view.findViewById(R.id.tvFoodDonor)
        val btnRemove:  TextView = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvEmoji.text    = getFoodEmoji(item.type)
        holder.tvName.text     = item.name
        holder.tvType.text     = item.type
        holder.tvQuantity.text = "📦 ${item.quantity}"
        holder.tvLocation.text = "📍 ${item.location}"
        holder.tvDonor.text    = "👤 ${item.donor}"
        holder.btnRemove.setOnClickListener { onRemove(item) }
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