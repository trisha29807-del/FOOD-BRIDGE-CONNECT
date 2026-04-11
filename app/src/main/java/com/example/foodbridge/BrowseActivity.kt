package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class BrowseActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var etSearch: TextInputEditText
    private lateinit var adapter: FoodListingAdapter
    private lateinit var bottomNav: BottomNavigationView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Master list fetched from Firestore (never filtered)
    private var allListings: MutableList<DocumentSnapshot> = mutableListOf()

    private var activeFilter: String = "All"
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Views
        recyclerView   = findViewById(R.id.recyclerView)
        progressBar    = findViewById(R.id.progressBar)
        layoutEmpty    = findViewById(R.id.layoutEmpty)
        chipGroup      = findViewById(R.id.chipGroup)
        etSearch       = findViewById(R.id.etSearch)
        bottomNav      = findViewById(R.id.bottomNav)

        // RecyclerView
        adapter = FoodListingAdapter(mutableListOf()) { doc -> claimListing(doc) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Bottom nav — mark Browse as selected
        bottomNav.selectedItemId = R.id.nav_browse
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish(); true
                }
                R.id.nav_browse -> true
                R.id.nav_donate -> {
                    startActivity(Intent(this, DonateFoodActivity::class.java))
                    finish(); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish(); true
                }
                else -> false
            }
        }

        // Filter chips
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            activeFilter = when {
                checkedIds.contains(R.id.chipVeg)     -> "Veg"
                checkedIds.contains(R.id.chipNonVeg)  -> "Non-Veg"
                checkedIds.contains(R.id.chipCooked)  -> "Cooked"
                checkedIds.contains(R.id.chipPacked)  -> "Packed"
                checkedIds.contains(R.id.chipFruits)  -> "Fruits/Veg"
                else                                   -> "All"
            }
            applyFilterAndSearch()
        }

        // Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fetchListings()
    }

    // ─── Firestore ────────────────────────────────────────────────────────────

    private fun fetchListings() {
        showLoading(true)
        db.collection("food_listings")
            .whereEqualTo("status", "available")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)
                allListings = snapshot.documents.toMutableList()
                applyFilterAndSearch()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to load listings: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmpty(true)
            }
    }

    private fun claimListing(doc: DocumentSnapshot) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "Please log in to claim food", Toast.LENGTH_SHORT).show()
            return
        }

        // Prevent donors from claiming their own listing
        if (doc.getString("donorUid") == currentUser.uid) {
            Toast.makeText(this, "You cannot claim your own listing", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("food_listings").document(doc.id)
            .update(
                mapOf(
                    "status"       to "claimed",
                    "claimedByUid" to currentUser.uid,
                    "claimedAt"    to com.google.firebase.Timestamp.now()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Food claimed successfully!", Toast.LENGTH_SHORT).show()
                allListings.removeAll { it.id == doc.id }
                applyFilterAndSearch()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to claim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ─── Filter + Search ──────────────────────────────────────────────────────

    private fun applyFilterAndSearch() {
        val filtered = allListings.filter { doc ->
            val matchesType = activeFilter == "All" ||
                    doc.getString("foodType") == activeFilter

            val matchesSearch = searchQuery.isEmpty() || run {
                val q = searchQuery.lowercase()
                (doc.getString("foodName")    ?.lowercase()?.contains(q) == true) ||
                (doc.getString("location")    ?.lowercase()?.contains(q) == true) ||
                (doc.getString("description") ?.lowercase()?.contains(q) == true)
            }

            matchesType && matchesSearch
        }.toMutableList()

        adapter.submitList(filtered)
        showEmpty(filtered.isEmpty())
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility  = View.GONE
            layoutEmpty.visibility   = View.GONE
        }
    }

    private fun showEmpty(empty: Boolean) {
        layoutEmpty.visibility  = if (empty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (empty) View.GONE    else View.VISIBLE
    }
}
