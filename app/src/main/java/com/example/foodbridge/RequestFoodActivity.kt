package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RequestFoodActivity : AppCompatActivity() {

    private lateinit var etFoodName:     TextInputEditText
    private lateinit var etQuantity:     TextInputEditText
    private lateinit var etLocation:     TextInputEditText
    private lateinit var etReason:       TextInputEditText
    private lateinit var spinnerUrgency: Spinner
    private lateinit var btnPostRequest: MaterialButton
    private lateinit var recyclerView:   RecyclerView
    private lateinit var layoutEmpty:    LinearLayout

    private val db = FirebaseFirestore.getInstance()

    // ── FIX: use a real-time listener so newly posted requests appear instantly ──
    private var requestListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_food)

        etFoodName     = findViewById(R.id.etFoodName)
        etQuantity     = findViewById(R.id.etQuantity)
        etLocation     = findViewById(R.id.etLocation)
        etReason       = findViewById(R.id.etReason)
        spinnerUrgency = findViewById(R.id.spinnerUrgency)
        btnPostRequest = findViewById(R.id.btnPostRequest)
        recyclerView   = findViewById(R.id.recyclerView)
        layoutEmpty    = findViewById(R.id.layoutEmpty)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        setupSpinner()
        setupPost()
        setupBottomNav()
    }

    override fun onStart() {
        super.onStart()
        startListeningRequests()   // real-time updates
    }

    override fun onStop() {
        super.onStop()
        requestListener?.remove()
        requestListener = null
    }

    private fun setupSpinner() {
        val urgency = listOf("Normal", "Urgent", "Very Urgent")
        spinnerUrgency.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, urgency).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
    }

    private fun setupPost() {
        btnPostRequest.setOnClickListener {
            val foodName = etFoodName.text.toString().trim()
            val quantity = etQuantity.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val reason   = etReason.text.toString().trim()
            val urgency  = spinnerUrgency.selectedItem.toString()

            if (foodName.isEmpty()) { etFoodName.error = "Required"; return@setOnClickListener }
            if (quantity.isEmpty()) { etQuantity.error = "Required"; return@setOnClickListener }
            if (location.isEmpty()) { etLocation.error = "Required"; return@setOnClickListener }

            val uid = FirebaseHelper.currentUid ?: return@setOnClickListener
            btnPostRequest.isEnabled = false
            btnPostRequest.text = "Posting..."

            lifecycleScope.launch {
                val profile = FirebaseHelper.getUserProfile(uid).getOrNull()
                val name    = profile?.get("name") as? String ?: "Anonymous"
                val role    = profile?.get("role") as? String ?: ""

                db.collection("food_requests").add(
                    mapOf(
                        "requesterUid"  to uid,
                        "requesterName" to name,
                        "requesterRole" to role,
                        "foodName"      to foodName,
                        "quantity"      to quantity,
                        "location"      to location,
                        "reason"        to reason,
                        "urgency"       to urgency,
                        "status"        to "open",
                        "createdAt"     to Timestamp.now()
                    )
                ).await()

                btnPostRequest.isEnabled = true
                btnPostRequest.text = "Post Request"
                etFoodName.setText("")
                etQuantity.setText("")
                etReason.setText("")
                // etLocation intentionally kept — user often posts from same place

                Toast.makeText(
                    this@RequestFoodActivity,
                    "✅ Request posted! Donors will be notified.",
                    Toast.LENGTH_SHORT
                ).show()
                // No need to call loadRequests() — real-time listener fires automatically
            }
        }
    }

    /**
     * Real-time listener replaces the one-shot .get() call.
     * This is why requests weren't appearing: .get() was called once and the
     * newly added document wasn't fetched again.  addSnapshotListener() fires
     * immediately with existing data AND on every subsequent change.
     */
    private fun startListeningRequests() {
        requestListener = db.collection("food_requests")
            .whereEqualTo("status", "open")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: emptyList()
                if (docs.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    layoutEmpty.visibility  = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    layoutEmpty.visibility  = View.GONE
                    if (recyclerView.adapter == null) {
                        recyclerView.layoutManager = LinearLayoutManager(this)
                    }
                    recyclerView.adapter = RequestAdapter(
                        docs,
                        onChat   = { doc -> openChat(doc) },
                        onDonate = { doc -> openDonateForRequest(doc) }
                    )
                }
            }
    }

    private fun openChat(doc: DocumentSnapshot) {
        val myUid         = FirebaseHelper.currentUid ?: return
        val requesterUid  = doc.getString("requesterUid")  ?: return
        val requesterName = doc.getString("requesterName") ?: "User"
        if (myUid == requesterUid) {
            Toast.makeText(this, "This is your own request", Toast.LENGTH_SHORT).show()
            return
        }
        val chatId = listOf(myUid, requesterUid).sorted().joinToString("_")

        // Pre-create chat document so BOTH sides can receive messages
        FirebaseFirestore.getInstance().collection("chats").document(chatId)
            .set(
                mapOf(
                    "participants"  to listOf(myUid, requesterUid),
                    "participantNames" to mapOf(requesterUid to requesterName),
                    "lastUpdated"   to Timestamp.now()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

        startActivity(Intent(this, ChatActivity::class.java).apply {
            putExtra("chatId",    chatId)
            putExtra("otherName", requesterName)
        })
    }

    private fun openDonateForRequest(doc: DocumentSnapshot) {
        startActivity(Intent(this, DonateFoodActivity::class.java).apply {
            putExtra("prefillFoodName", doc.getString("foodName") ?: "")
            putExtra("prefillQuantity", doc.getString("quantity") ?: "")
            putExtra("prefillLocation", doc.getString("location") ?: "")
            putExtra("requestId",       doc.id)
        })
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
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class RequestAdapter(
    private val items:    List<DocumentSnapshot>,
    private val onChat:   (DocumentSnapshot) -> Unit,
    private val onDonate: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<RequestAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvFoodName:  TextView       = view.findViewById(R.id.tvFoodName)
        val tvRequester: TextView       = view.findViewById(R.id.tvRequester)
        val tvQuantity:  TextView       = view.findViewById(R.id.tvQuantity)
        val tvLocation:  TextView       = view.findViewById(R.id.tvLocation)
        val tvReason:    TextView       = view.findViewById(R.id.tvReason)
        val tvUrgency:   TextView       = view.findViewById(R.id.tvUrgency)
        val btnChat:     MaterialButton = view.findViewById(R.id.btnChat)
        val btnDonate:   MaterialButton = view.findViewById(R.id.btnDonate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_food_request, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val doc = items[position]
        holder.tvFoodName.text  = doc.getString("foodName") ?: "Unknown"
        holder.tvRequester.text =
            "👤 ${doc.getString("requesterName") ?: "Anonymous"} · ${doc.getString("requesterRole") ?: ""}"
        holder.tvQuantity.text  = "📦 ${doc.getString("quantity") ?: ""}"
        holder.tvLocation.text  = "📍 ${doc.getString("location") ?: ""}"

        val reason = doc.getString("reason") ?: ""
        if (reason.isNotEmpty()) {
            holder.tvReason.text = "💬 $reason"
            holder.tvReason.visibility = View.VISIBLE
        } else {
            holder.tvReason.visibility = View.GONE
        }

        val urgency = doc.getString("urgency") ?: "Normal"
        holder.tvUrgency.text = when (urgency) {
            "Very Urgent" -> "🔴 Very Urgent"
            "Urgent"      -> "🟡 Urgent"
            else          -> "🟢 Normal"
        }

        // Hide chat/donate for own requests
        val myUid = FirebaseHelper.currentUid ?: ""
        val isOwn = doc.getString("requesterUid") == myUid
        holder.btnChat.visibility   = if (isOwn) View.GONE else View.VISIBLE
        holder.btnDonate.visibility = if (isOwn) View.GONE else View.VISIBLE

        holder.btnChat.setOnClickListener   { onChat(doc) }
        holder.btnDonate.setOnClickListener { onDonate(doc) }
    }
}