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
import java.text.SimpleDateFormat
import java.util.*

data class HistoryItem(
    val title: String,
    val subtitle: String,
    val type: String,
    val timestamp: Long
)

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

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
        val prefs    = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val myName   = prefs.getString("user_name", "") ?: ""
        val historyItems = mutableListOf<HistoryItem>()

        // Add donations as history
        val donationsJson = prefs.getString("food_listings", "[]") ?: "[]"
        val donationsArray = JSONArray(donationsJson)
        for (i in 0 until donationsArray.length()) {
            val obj = donationsArray.getJSONObject(i)
            if (obj.optString("donor") == myName) {
                historyItems.add(HistoryItem(
                    title     = "Donated: ${obj.getString("name")}",
                    subtitle  = "📍 ${obj.getString("location")} · 📦 ${obj.getString("quantity")}",
                    type      = "donation",
                    timestamp = obj.getLong("timestamp")
                ))
            }
        }

        // Add orders as history
        val ordersJson = prefs.getString("my_orders", "[]") ?: "[]"
        val ordersArray = JSONArray(ordersJson)
        for (i in 0 until ordersArray.length()) {
            val obj = ordersArray.getJSONObject(i)
            historyItems.add(HistoryItem(
                title     = "Ordered: ${obj.getString("name")}",
                subtitle  = "👤 From: ${obj.optString("donor", "Anonymous")}",
                type      = "order",
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            ))
        }

        // Sort by newest first
        historyItems.sortByDescending { it.timestamp }

        if (historyItems.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutEmpty.visibility  = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmpty.visibility  = View.GONE
            recyclerView.adapter    = HistoryAdapter(historyItems)
        }
    }
}

class HistoryAdapter(private val items: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji:    TextView = view.findViewById(R.id.tvHistoryEmoji)
        val tvTitle:    TextView = view.findViewById(R.id.tvHistoryTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvHistorySubtitle)
        val tvDate:     TextView = view.findViewById(R.id.tvHistoryDate)
        val tvBadge:    TextView = view.findViewById(R.id.tvHistoryBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvEmoji.text    = if (item.type == "donation") "🍱" else "🛒"
        holder.tvTitle.text    = item.title
        holder.tvSubtitle.text = item.subtitle
        holder.tvDate.text     = formatDate(item.timestamp)
        holder.tvBadge.text    = if (item.type == "donation") "Donated" else "Ordered"
        holder.tvBadge.setBackgroundResource(
            if (item.type == "donation") R.drawable.badge_bg else R.drawable.badge_orange_bg
        )
    }

    override fun getItemCount() = items.size

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}