package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ── Cart item data class (replaces old FoodItem) ─────────────────────────────
data class CartItem(
    val id: String,          // Firestore document ID (String, not Long)
    val name: String,
    val type: String,
    val quantity: String,
    val location: String,
    val expiry: String,
    val description: String,
    val donor: String,
    val donorUid: String,
    val price: Double,
    val listingType: String
)

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutCheckout: LinearLayout
    private lateinit var tvItemCount: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnCheckout: MaterialButton
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        recyclerView   = findViewById(R.id.recyclerView)
        layoutEmpty    = findViewById(R.id.layoutEmpty)
        layoutCheckout = findViewById(R.id.layoutCheckout)
        tvItemCount    = findViewById(R.id.tvItemCount)
        tvTotalPrice   = findViewById(R.id.tvTotalPrice)
        btnCheckout    = findViewById(R.id.btnCheckout)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        btnCheckout.setOnClickListener { placeAllOrders() }

        loadCart()
    }

    override fun onResume() {
        super.onResume()
        loadCart()
    }

    private fun loadCart() {
        val prefs = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val json  = prefs.getString("cart_items", "[]") ?: "[]"
        val arr   = JSONArray(json)

        val items = mutableListOf<CartItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            items.add(CartItem(
                id          = obj.getString("id"),
                name        = obj.optString("name", ""),
                type        = obj.optString("type", ""),
                quantity    = obj.optString("quantity", ""),
                location    = obj.optString("location", ""),
                expiry      = obj.optString("expiry", ""),
                description = obj.optString("description", ""),
                donor       = obj.optString("donor", "Anonymous"),
                donorUid    = obj.optString("donorUid", ""),
                price       = obj.optDouble("price", 0.0),
                listingType = obj.optString("listingType", "Free Donation")
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

            // Show total price
            val total = items.sumOf { it.price }
            tvTotalPrice.text = if (total == 0.0) "Total: Free 🎁" else "Total: ₹${String.format("%.0f", total)}"

            recyclerView.adapter = CartAdapter(items) { item ->
                removeFromCart(item.id)
            }
        }
    }

    private fun removeFromCart(id: String) {
        val prefs  = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val arr    = JSONArray(prefs.getString("cart_items", "[]") ?: "[]")
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("id") != id) newArr.put(obj)
        }
        prefs.edit().putString("cart_items", newArr.toString()).apply()
        Toast.makeText(this, "Removed from cart", Toast.LENGTH_SHORT).show()
        loadCart()
    }

    private fun placeAllOrders() {
        val uid = FirebaseHelper.currentUid ?: run {
            Toast.makeText(this, "Please sign in again", Toast.LENGTH_SHORT).show()
            return
        }
        val prefs = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val arr   = JSONArray(prefs.getString("cart_items", "[]") ?: "[]")
        if (arr.length() == 0) return

        btnCheckout.isEnabled = false
        btnCheckout.text = "Placing orders..."

        CoroutineScope(Dispatchers.Main).launch {
            var successCount = 0
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val listingId = obj.getString("id")
                try {
                    // Mark listing as claimed
                    db.collection("food_listings").document(listingId)
                        .update(mapOf(
                            "status"       to "claimed",
                            "claimedByUid" to uid,
                            "claimedAt"    to Timestamp.now()
                        )).await()

                    // Save to orders collection
                    db.collection("orders").add(mapOf(
                        "listingId"      to listingId,
                        "buyerUid"       to uid,
                        "donorUid"       to obj.optString("donorUid", ""),
                        "donorName"      to obj.optString("donor", ""),
                        "foodName"       to obj.optString("name", ""),
                        "foodType"       to obj.optString("type", ""),
                        "quantity"       to obj.optString("quantity", ""),
                        "pickupLocation" to obj.optString("location", ""),
                        "price"          to obj.optDouble("price", 0.0),
                        "listingType"    to obj.optString("listingType", "Free Donation"),
                        "paymentMethod"  to "Cash on Delivery",
                        "paymentStatus"  to "pending",
                        "orderStatus"    to "confirmed",
                        "createdAt"      to Timestamp.now()
                    )).await()

                    successCount++
                } catch (e: Exception) { /* skip failed items */ }
            }

            // Clear cart
            prefs.edit().putString("cart_items", "[]").apply()
            btnCheckout.isEnabled = true
            btnCheckout.text = "Checkout"

            Toast.makeText(
                this@CartActivity,
                "🎉 $successCount order(s) placed successfully!",
                Toast.LENGTH_LONG
            ).show()

            // Go to track orders
            startActivity(Intent(this@CartActivity, TrackOrdersActivity::class.java))
            finish()
        }
    }
}

class CartAdapter(
    private val items: List<CartItem>,
    private val onRemove: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji:       TextView = view.findViewById(R.id.tvFoodEmoji)
        val tvName:        TextView = view.findViewById(R.id.tvFoodName)
        val tvType:        TextView = view.findViewById(R.id.tvFoodType)
        val tvQuantity:    TextView = view.findViewById(R.id.tvFoodQuantity)
        val tvLocation:    TextView = view.findViewById(R.id.tvFoodLocation)
        val tvDonor:       TextView = view.findViewById(R.id.tvFoodDonor)
        val tvPrice:       TextView = view.findViewById(R.id.tvCartItemPrice)
        val btnRemove:     TextView = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvEmoji.text    = getFoodEmoji(item.type)
        holder.tvName.text     = item.name
        holder.tvType.text     = item.type
        holder.tvQuantity.text = "📦 ${item.quantity}"
        holder.tvLocation.text = "📍 ${item.location}"
        holder.tvDonor.text    = "👤 ${item.donor}"
        holder.tvPrice.text    = if (item.price == 0.0) "🆓 Free" else "₹${String.format("%.0f", item.price)}"
        holder.btnRemove.setOnClickListener { onRemove(item) }
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
