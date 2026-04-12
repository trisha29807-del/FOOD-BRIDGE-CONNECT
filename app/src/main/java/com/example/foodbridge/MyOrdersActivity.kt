package com.example.foodbridge

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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)

        recyclerView = findViewById(R.id.recyclerView)
        layoutEmpty  = findViewById(R.id.layoutEmpty)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        loadMyOrders()
    }

    override fun onResume() {
        super.onResume()
        loadMyOrders()
    }

    private fun loadMyOrders() {
        val uid = FirebaseHelper.currentUid ?: return

        // Orders = listings claimed by this user
        db.collection("food_listings")
            .whereEqualTo("claimedByUid", uid)
            .orderBy("claimedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                if (docs.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    layoutEmpty.visibility  = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    layoutEmpty.visibility  = View.GONE
                    recyclerView.adapter = OrdersAdapter(docs) { doc ->
                        markAsReceived(doc.id)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not load orders", Toast.LENGTH_SHORT).show()
                layoutEmpty.visibility = View.VISIBLE
            }
    }

    private fun markAsReceived(docId: String) {
        db.collection("food_listings").document(docId)
            .update("status", "received")
            .addOnSuccessListener {
                Toast.makeText(this, "Marked as received!", Toast.LENGTH_SHORT).show()
                loadMyOrders()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }
}

class OrdersAdapter(
    private val items: List<DocumentSnapshot>,
    private val onMarkReceived: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji:    TextView      = view.findViewById(R.id.tvFoodEmoji)
        val tvName:     TextView      = view.findViewById(R.id.tvFoodName)
        val tvType:     TextView      = view.findViewById(R.id.tvFoodType)
        val tvQuantity: TextView      = view.findViewById(R.id.tvFoodQuantity)
        val tvLocation: TextView      = view.findViewById(R.id.tvFoodLocation)
        val tvExpiry:   TextView      = view.findViewById(R.id.tvFoodExpiry)
        val tvDonor:    TextView      = view.findViewById(R.id.tvFoodDonor)
        val tvStatus:   TextView      = view.findViewById(R.id.tvStatus)
        val btnMark:    MaterialButton= view.findViewById(R.id.btnMarkReceived)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc  = items[position]
        val type = doc.getString("foodType") ?: ""

        holder.tvEmoji.text    = getFoodEmoji(type)
        holder.tvName.text     = doc.getString("foodName")  ?: "Unknown"
        holder.tvType.text     = type
        holder.tvQuantity.text = "📦 ${doc.getString("quantity") ?: ""}"
        holder.tvLocation.text = "📍 ${doc.getString("location") ?: "Location not set"}"
        holder.tvDonor.text    = "👤 Donor: ${doc.getString("donorName") ?: "Anonymous"}"

        val expiry = doc.getTimestamp("expiryDate")
        holder.tvExpiry.text = if (expiry != null) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            "⏰ ${sdf.format(expiry.toDate())}"
        } else ""

        val status = doc.getString("status") ?: "claimed"
        holder.tvStatus.text = when (status) {
            "claimed"  -> "🟡 Pending Pickup"
            "received" -> "✅ Received"
            else       -> status
        }

        if (status == "received") {
            holder.btnMark.visibility = View.GONE
        } else {
            holder.btnMark.visibility = View.VISIBLE
            holder.btnMark.setOnClickListener { onMarkReceived(doc) }
        }
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