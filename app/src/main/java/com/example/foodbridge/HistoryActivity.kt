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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerView = findViewById(R.id.recyclerView)
        layoutEmpty  = findViewById(R.id.layoutEmpty)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        loadHistory()
    }

    private fun loadHistory() {
        val uid = FirebaseHelper.currentUid ?: return
        val allDocs = mutableListOf<Pair<DocumentSnapshot, String>>() // doc + type

        // Fetch donations (listings posted by this user)
        db.collection("food_listings")
            .whereEqualTo("donorUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { donationSnap ->
                donationSnap.documents.forEach { allDocs.add(Pair(it, "donation")) }

                // Then fetch orders (listings claimed by this user)
                db.collection("food_listings")
                    .whereEqualTo("claimedByUid", uid)
                    .orderBy("claimedAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { orderSnap ->
                        orderSnap.documents.forEach { allDocs.add(Pair(it, "order")) }

                        // Sort all combined by createdAt descending
                        allDocs.sortByDescending { (doc, _) ->
                            doc.getTimestamp("createdAt")?.seconds ?: 0L
                        }

                        if (allDocs.isEmpty()) {
                            recyclerView.visibility = View.GONE
                            layoutEmpty.visibility  = View.VISIBLE
                        } else {
                            recyclerView.visibility = View.VISIBLE
                            layoutEmpty.visibility  = View.GONE
                            recyclerView.adapter = HistoryAdapter(allDocs)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Could not load history", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not load history", Toast.LENGTH_SHORT).show()
                layoutEmpty.visibility = View.VISIBLE
            }
    }
}

class HistoryAdapter(
    private val items: List<Pair<DocumentSnapshot, String>>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji:    TextView = view.findViewById(R.id.tvHistoryEmoji)
        val tvTitle:    TextView = view.findViewById(R.id.tvHistoryTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvHistorySubtitle)
        val tvDate:     TextView = view.findViewById(R.id.tvHistoryDate)
        val tvBadge:    TextView = view.findViewById(R.id.tvHistoryBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (doc, type) = items[position]
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        if (type == "donation") {
            holder.tvEmoji.text    = "🍱"
            holder.tvTitle.text    = "Donated: ${doc.getString("foodName") ?: "Food"}"
            holder.tvSubtitle.text = "📍 ${doc.getString("location") ?: ""} · 📦 ${doc.getString("quantity") ?: ""}"
            holder.tvBadge.text    = "Donated"
            holder.tvBadge.setBackgroundResource(R.drawable.badge_bg)
        } else {
            holder.tvEmoji.text    = "🛒"
            holder.tvTitle.text    = "Claimed: ${doc.getString("foodName") ?: "Food"}"
            holder.tvSubtitle.text = "👤 From: ${doc.getString("donorName") ?: "Anonymous"}"
            holder.tvBadge.text    = "Ordered"
            holder.tvBadge.setBackgroundResource(R.drawable.badge_orange_bg)
        }

        val ts = doc.getTimestamp("createdAt")
        holder.tvDate.text = if (ts != null) sdf.format(ts.toDate()) else ""
    }
}