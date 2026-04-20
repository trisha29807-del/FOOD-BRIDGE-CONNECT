package com.example.foodbridge

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class BrowseActivity : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: BrowseAdapter
    private lateinit var tvCartCount: TextView
    private lateinit var btnCart: FrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val db = FirebaseFirestore.getInstance()
    private var allItems = mutableListOf<DocumentSnapshot>()
    private var selectedFilter = "All"

    // Updated filter list: includes Donate and Request
    private val filterLabels = listOf("All", "Veg", "Non-Veg", "Cooked", "Packed", "Fruits/Veg", "Donations", "Requests")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        etSearch     = findViewById(R.id.etSearch)
        chipGroup    = findViewById(R.id.chipGroup)
        recyclerView = findViewById(R.id.recyclerView)
        layoutEmpty  = findViewById(R.id.layoutEmpty)
        tvCartCount  = findViewById(R.id.tvCartCount)
        btnCart      = findViewById(R.id.btnCart)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        adapter = BrowseAdapter(
            mutableListOf(),
            onOrder   = { doc -> openPlaceOrder(doc) },
            onAddCart = { doc -> addToCart(doc) },
            onChat    = { doc -> openChat(doc) }
        )

        findViewById<MaterialButton>(R.id.btnDonateQuick).setOnClickListener {
            startActivity(Intent(this, DonateFoodActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnRequestFood).setOnClickListener {
            startActivity(Intent(this, RequestFoodActivity::class.java))
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        swipeRefresh.setColorSchemeColors(getColor(R.color.green_primary))
        swipeRefresh.setOnRefreshListener { loadData() }

        setupChips()
        setupSearch()
        setupBottomNav()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
        updateCartBadge()
    }

    private fun updateCartBadge() {
        val prefs = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val count = JSONArray(prefs.getString("cart_items", "[]") ?: "[]").length()
        tvCartCount.visibility = if (count > 0) View.VISIBLE else View.GONE
        tvCartCount.text = count.toString()
    }

    private fun addToCart(doc: DocumentSnapshot) {
        if (doc.reference.path.contains("food_requests")) {
            Toast.makeText(this, "Cannot add a request to cart", Toast.LENGTH_SHORT).show()
            return
        }
        val prefs = getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
        val arr   = JSONArray(prefs.getString("cart_items", "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i).getString("id") == doc.id) {
                Toast.makeText(this, "Already in cart!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val rawQty = doc.getString("quantity") ?: "1"
        val maxQty = rawQty.filter { it.isDigit() }.toIntOrNull() ?: 1

        val obj = JSONObject().apply {
            put("id",          doc.id)
            put("name",        doc.getString("foodName")  ?: "")
            put("type",        doc.getString("foodType")  ?: "")
            put("quantity",    rawQty)
            put("maxQty",      maxQty)
            put("location",    doc.getString("location")  ?: "")
            put("donor",       doc.getString("donorName") ?: "Anonymous")
            put("donorUid",    doc.getString("donorUid")  ?: "")
            put("listingId",   doc.id)
            put("description", doc.getString("description") ?: "")
            val expiry = doc.getTimestamp("expiryDate")
            put("expiry", if (expiry != null)
                SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(expiry.toDate())
            else "")
        }
        arr.put(obj)
        prefs.edit().putString("cart_items", arr.toString()).apply()
        Toast.makeText(this, "✅ Added to cart!", Toast.LENGTH_SHORT).show()
        updateCartBadge()
    }

    private fun openChat(doc: DocumentSnapshot) {
        val myUid     = FirebaseHelper.currentUid ?: return
        val isRequest = doc.reference.path.contains("food_requests")

        val otherUid  = if (isRequest) doc.getString("requesterUid") else doc.getString("donorUid")
        val otherName = if (isRequest) doc.getString("requesterName") else doc.getString("donorName") ?: "User"

        if (myUid == otherUid || otherUid == null) {
            Toast.makeText(this, "Cannot chat with yourself", Toast.LENGTH_SHORT).show()
            return
        }

        val chatId = listOf(myUid, otherUid).sorted().joinToString("_")

        FirebaseFirestore.getInstance().collection("chats").document(chatId)
            .set(
                mapOf(
                    "participants"     to listOf(myUid, otherUid),
                    "participantNames" to mapOf(otherUid to otherName),
                    "lastUpdated"      to com.google.firebase.Timestamp.now()
                ),
                SetOptions.merge()
            )

        startActivity(Intent(this, ChatActivity::class.java).apply {
            putExtra("chatId",    chatId)
            putExtra("otherName", otherName)
        })
    }

    private fun openPlaceOrder(doc: DocumentSnapshot) {
        val isRequest = doc.reference.path.contains("food_requests")
        if (isRequest) {
            // Redirect to FulfillRequestActivity instead of DonateFoodActivity directly
            startActivity(Intent(this, FulfillRequestActivity::class.java).apply {
                putExtra("requestId",       doc.id)
                putExtra("requesterName",   doc.getString("requesterName") ?: "Anonymous")
                putExtra("requesterUid",    doc.getString("requesterUid")  ?: "")
                putExtra("foodName",        doc.getString("foodName")      ?: "")
                putExtra("quantity",        doc.getString("quantity")      ?: "")
                putExtra("location",        doc.getString("location")      ?: "")
                putExtra("reason",          doc.getString("reason")        ?: "")
                putExtra("urgency",         doc.getString("urgency")       ?: "Normal")
            })
        } else {
            val rawQty = doc.getString("quantity") ?: "1"
            val maxQty = rawQty.filter { it.isDigit() }.toIntOrNull() ?: 1
            startActivity(Intent(this, PlaceOrderActivity::class.java).apply {
                putExtra("listingId", doc.id)
                putExtra("foodName",  doc.getString("foodName")  ?: "")
                putExtra("donorUid",  doc.getString("donorUid")  ?: "")
                putExtra("donorName", doc.getString("donorName") ?: "")
                putExtra("quantity",  rawQty)
                putExtra("maxQty",    maxQty)
                putExtra("location",  doc.getString("location")  ?: "")
                putExtra("foodType",  doc.getString("foodType")  ?: "")
            })
        }
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        db.collection("food_listings").whereEqualTo("status", "available").get()
            .addOnSuccessListener { listings ->
                db.collection("food_requests").whereEqualTo("status", "open").get()
                    .addOnSuccessListener { requests ->
                        swipeRefresh.isRefreshing = false
                        val now = com.google.firebase.Timestamp.now()

                        val combined = mutableListOf<DocumentSnapshot>()
                        combined.addAll(listings.documents)
                        combined.addAll(requests.documents)

                        allItems = combined.filter { doc ->
                            val expiry = doc.getTimestamp("expiryDate")
                            expiry == null || expiry.compareTo(now) > 0
                        }.sortedByDescending {
                            it.getTimestamp("createdAt") ?: it.getTimestamp("expiryDate")
                        }.toMutableList()

                        applyFilters()
                    }
            }
            .addOnFailureListener {
                swipeRefresh.isRefreshing = false
                showEmpty(true)
            }
    }

    private fun applyFilters() {
        val query = etSearch.text?.toString()?.trim()?.lowercase().orEmpty()
        val filtered = allItems.filter { doc ->
            val isRequest = doc.reference.path.contains("food_requests")
            val foodType  = doc.getString("foodType")?.lowercase() ?: ""
            val isVeg     = doc.getBoolean("isVeg")

            val matchesFilter = when (selectedFilter) {
                "All"        -> true
                "Requests"   -> isRequest
                "Donations"  -> !isRequest
                "Veg"        -> isVeg == true
                "Non-Veg"    -> isVeg == false
                "Cooked"     -> foodType.contains("cooked")
                "Packed"     -> foodType.contains("packed")
                "Fruits/Veg" -> foodType.contains("fruit") || foodType.contains("vegetable") || foodType.contains("veg")
                else         -> true
            }

            val name = doc.getString("foodName")?.lowercase() ?: ""
            val loc  = doc.getString("location")?.lowercase() ?: ""
            val matchesSearch = query.isEmpty() || name.contains(query) || loc.contains(query)

            matchesFilter && matchesSearch
        }.toMutableList()

        adapter.updateItems(filtered)
        showEmpty(filtered.isEmpty())
    }

    private fun showEmpty(empty: Boolean) {
        layoutEmpty.visibility  = if (empty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (empty) View.GONE    else View.VISIBLE
    }

    private fun setupChips() {
        chipGroup.removeAllViews()
        filterLabels.forEach { label ->
            val chip = Chip(this)
            chip.text = label
            chip.isCheckable = true
            chip.isChecked = (label == "All")
            chip.setTextColor(getColor(R.color.green_primary))
            chip.setOnClickListener {
                selectedFilter = label
                for (i in 0 until chipGroup.childCount) (chipGroup.getChildAt(i) as Chip).isChecked = false
                chip.isChecked = true
                applyFilters()
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_browse
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> { startActivity(Intent(this, MainActivity::class.java)); finish(); true }
                R.id.nav_donate  -> { startActivity(Intent(this, DonateFoodActivity::class.java)); true }
                R.id.nav_browse  -> true
                R.id.nav_fssai   -> { startActivity(Intent(this, FssaiActivity::class.java)); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                else -> false
            }
        }
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class BrowseAdapter(
    private val items: MutableList<DocumentSnapshot>,
    private val onOrder:   (DocumentSnapshot) -> Unit,
    private val onAddCart: (DocumentSnapshot) -> Unit,
    private val onChat:    (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<BrowseAdapter.ViewHolder>() {

    fun updateItems(newItems: List<DocumentSnapshot>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFoodImage:   ImageView      = view.findViewById(R.id.ivFoodImage)
        val tvExpiryWarn:  TextView       = view.findViewById(R.id.tvExpiryWarn)
        val tvFoodName:    TextView       = view.findViewById(R.id.tvFoodName)
        val tvFoodType:    TextView       = view.findViewById(R.id.tvFoodType)
        val tvPrice:       TextView       = view.findViewById(R.id.tvPrice)
        val tvListingType: TextView       = view.findViewById(R.id.tvListingType)
        val tvDonorName:   TextView       = view.findViewById(R.id.tvDonorName)
        val tvLocation:    TextView       = view.findViewById(R.id.tvLocation)
        val tvQuantity:    TextView       = view.findViewById(R.id.tvQuantity)
        val tvExpiry:      TextView       = view.findViewById(R.id.tvExpiry)
        val tvDescription: TextView       = view.findViewById(R.id.tvDescription)
        val tvVegBadge:    TextView       = view.findViewById(R.id.tvVegBadge)
        val btnClaim:      MaterialButton = view.findViewById(R.id.btnClaim)
        val btnAddToCart:  MaterialButton = view.findViewById(R.id.btnAddToCart)
        val btnChat:       MaterialButton = view.findViewById(R.id.btnChat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_food_listing, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = items[position]
        val ctx = holder.itemView.context
        val isRequest = doc.reference.path.contains("food_requests")

        holder.tvFoodName.text = doc.getString("foodName") ?: "Unknown"
        holder.tvLocation.text = doc.getString("location") ?: "No Location"

        // ── Quantity: display only, NO adder controls ──────────────────────
        holder.tvQuantity.text = doc.getString("quantity") ?: ""

        if (isRequest) {
            holder.tvDonorName.text   = "Requester: ${doc.getString("requesterName") ?: "Anonymous"}"
            holder.tvFoodType.text    = "Urgency: ${doc.getString("urgency") ?: "Normal"}"
            holder.tvListingType.text = "HELP NEEDED"
            holder.tvPrice.text       = "Request"
            holder.btnClaim.text      = "Fulfill Request"
            holder.btnAddToCart.visibility = View.GONE
            holder.ivFoodImage.visibility  = View.GONE
            holder.tvExpiry.text      = "Reason: ${doc.getString("reason") ?: "N/A"}"
            holder.tvVegBadge.visibility   = View.GONE
        } else {
            holder.tvDonorName.text   = "Donor: ${doc.getString("donorName") ?: "Anonymous"}"
            holder.tvFoodType.text    = doc.getString("foodType") ?: ""
            holder.tvListingType.text = doc.getString("listingType") ?: "Free Donation"
            holder.btnClaim.text      = "Order / Claim"
            holder.btnAddToCart.visibility = View.VISIBLE

            val price = doc.getDouble("price") ?: 0.0
            holder.tvPrice.text = if (price == 0.0) "🆓 Free" else "₹${String.format("%.0f", price)}"

            val imageUrl = doc.getString("imageUrl") ?: ""
            if (imageUrl.isNotEmpty()) {
                holder.ivFoodImage.visibility = View.VISIBLE
                Glide.with(ctx).load(imageUrl).centerCrop().into(holder.ivFoodImage)
            } else {
                holder.ivFoodImage.visibility = View.GONE
            }

            val expiry = doc.getTimestamp("expiryDate")
            holder.tvExpiry.text = if (expiry != null)
                "⏰ " + SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(expiry.toDate())
            else "N/A"
        }

        // Chat visibility
        val myUid    = FirebaseHelper.currentUid ?: ""
        val otherUid = if (isRequest) doc.getString("requesterUid") else doc.getString("donorUid")
        holder.btnChat.visibility = if (myUid == otherUid) View.GONE else View.VISIBLE

        holder.btnChat.setOnClickListener    { onChat(doc) }
        holder.btnClaim.setOnClickListener   { onOrder(doc) }
        holder.btnAddToCart.setOnClickListener { onAddCart(doc) }
    }
}