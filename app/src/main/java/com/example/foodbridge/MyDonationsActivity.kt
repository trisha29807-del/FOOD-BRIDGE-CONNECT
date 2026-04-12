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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class MyDonationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_donations)

        recyclerView = findViewById(R.id.recyclerView)
        layoutEmpty  = findViewById(R.id.layoutEmpty)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadMyDonations()
    }

    override fun onResume() {
        super.onResume()
        loadMyDonations()
    }

    private fun loadMyDonations() {
        val uid = FirebaseHelper.currentUid ?: return

        db.collection("food_listings")
            .whereEqualTo("donorUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                if (docs.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    layoutEmpty.visibility  = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    layoutEmpty.visibility  = View.GONE
                    recyclerView.adapter = DonationAdapter(docs) { doc ->
                        confirmDelete(doc)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not load donations", Toast.LENGTH_SHORT).show()
                layoutEmpty.visibility = View.VISIBLE
            }
    }

    private fun confirmDelete(doc: DocumentSnapshot) {
        AlertDialog.Builder(this)
            .setTitle("Remove listing")
            .setMessage("Remove \"${doc.getString("foodName")}\" from available listings?")
            .setPositiveButton("Remove") { _, _ -> deleteDonation(doc.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDonation(docId: String) {
        db.collection("food_listings").document(docId)
            .update("status", "removed")
            .addOnSuccessListener {
                Toast.makeText(this, "Listing removed", Toast.LENGTH_SHORT).show()
                loadMyDonations()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to remove listing", Toast.LENGTH_SHORT).show()
            }
    }
}

class DonationAdapter(
    private val items: List<DocumentSnapshot>,
    private val onDelete: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<DonationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji:    TextView = view.findViewById(R.id.tvFoodEmoji)
        val tvName:     TextView = view.findViewById(R.id.tvFoodName)
        val tvType:     TextView = view.findViewById(R.id.tvFoodType)
        val tvQuantity: TextView = view.findViewById(R.id.tvFoodQuantity)
        val tvLocation: TextView = view.findViewById(R.id.tvFoodLocation)
        val tvExpiry:   TextView = view.findViewById(R.id.tvFoodExpiry)
        val tvDonor:    TextView = view.findViewById(R.id.tvFoodDonor)
        val tvStatus:   TextView = view.findViewById(R.id.tvStatus)
        val btnDelete:  TextView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_donation_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc  = items[position]
        val type = doc.getString("foodType") ?: ""

        holder.tvEmoji.text    = getFoodEmoji(type)
        holder.tvName.text     = doc.getString("foodName")  ?: "Unknown"
        holder.tvType.text     = type
        holder.tvQuantity.text = "📦 ${doc.getString("quantity") ?: ""}"
        holder.tvLocation.text = "📍 ${doc.getString("location") ?: "Location not set"}"
        holder.tvDonor.text    = "👤 ${doc.getString("donorName") ?: "You"}"

        val expiry = doc.getTimestamp("expiryDate")
        if (expiry != null) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            holder.tvExpiry.text = "⏰ ${sdf.format(expiry.toDate())}"
        } else {
            holder.tvExpiry.text = ""
        }

        val status = doc.getString("status") ?: "available"
        holder.tvStatus.text = when (status) {
            "available" -> "🟢 Available"
            "claimed"   -> "🟡 Claimed"
            "received"  -> "✅ Received"
            else        -> status
        }

        // Only allow delete if still available
        if (status == "available") {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDelete(doc) }
        } else {
            holder.btnDelete.visibility = View.GONE
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