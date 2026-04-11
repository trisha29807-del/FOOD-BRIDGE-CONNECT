package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class TrackOrdersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_orders)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar  = findViewById(R.id.progressBar)
        layoutEmpty  = findViewById(R.id.layoutEmpty)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupBottomNav()
        loadOrders()
    }

    private fun loadOrders() {
        val uid = FirebaseHelper.currentUid ?: return
        progressBar.visibility = View.VISIBLE

        // Load both claimed listings (by this user) and donated listings (by this user)
        db.collection("food_listings")
            .whereEqualTo("claimedByUid", uid)
            .orderBy("claimedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                val orders = snapshot.documents
                if (orders.isEmpty()) {
                    layoutEmpty.visibility  = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    layoutEmpty.visibility  = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = OrderAdapter(orders)
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Could not load orders", Toast.LENGTH_SHORT).show()
                layoutEmpty.visibility = View.VISIBLE
            }
    }

    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home    -> { startActivity(Intent(this, MainActivity::class.java)); true }
                    R.id.nav_donate  -> { startActivity(Intent(this, DonateFoodActivity::class.java)); true }
                    R.id.nav_browse  -> { startActivity(Intent(this, BrowseActivity::class.java)); true }
                    R.id.nav_fssai   -> { startActivity(Intent(this, FssaiActivity::class.java)); true }
                    R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                    else -> false
                }
            }
    }

    // ── Inline adapter ────────────────────────────────────────────────────────
    inner class OrderAdapter(
        private val orders: List<com.google.firebase.firestore.DocumentSnapshot>
    ) : RecyclerView.Adapter<OrderAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvFoodName:   TextView       = view.findViewById(R.id.tvFoodName)
            val tvDonorName:  TextView       = view.findViewById(R.id.tvDonorName)
            val tvLocation:   TextView       = view.findViewById(R.id.tvLocation)
            val tvStatus:     TextView       = view.findViewById(R.id.tvStatus)
            val tvClaimedAt:  TextView       = view.findViewById(R.id.tvClaimedAt)
            val btnMarkDone:  MaterialButton = view.findViewById(R.id.btnMarkDone)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_order, parent, false))

        override fun getItemCount() = orders.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val doc = orders[position]
            holder.tvFoodName.text  = doc.getString("foodName")  ?: "Unknown"
            holder.tvDonorName.text = "From: ${doc.getString("donorName") ?: "Anonymous"}"
            holder.tvLocation.text  = "📍 ${doc.getString("location") ?: "Location not set"}"

            val status = doc.getString("status") ?: "claimed"
            holder.tvStatus.text = when (status) {
                "claimed"   -> "🟡 Claimed — awaiting pickup"
                "received"  -> "🟢 Received"
                else        -> status
            }
            holder.tvStatus.setTextColor(
                if (status == "received")
                    getColor(android.R.color.holo_green_dark)
                else
                    getColor(android.R.color.holo_orange_dark)
            )

            val claimedAt = doc.getTimestamp("claimedAt")
            if (claimedAt != null) {
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                holder.tvClaimedAt.text = "Claimed on: ${sdf.format(claimedAt.toDate())}"
            }

            // Hide Mark Done if already received
            if (status == "received") {
                holder.btnMarkDone.visibility = View.GONE
            } else {
                holder.btnMarkDone.visibility = View.VISIBLE
                holder.btnMarkDone.setOnClickListener {
                    db.collection("food_listings").document(doc.id)
                        .update("status", "received")
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@TrackOrdersActivity,
                                "Marked as received!",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadOrders()
                        }
                }
            }
        }
    }
}
