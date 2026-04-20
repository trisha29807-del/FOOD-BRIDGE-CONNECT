package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * FulfillRequestActivity
 *
 * Shown when a user taps "Fulfill Request" on a food-request card in BrowseActivity.
 * Displays the request details and lets the donor confirm / add a note before submitting.
 *
 * Extras received (all String unless noted):
 *  - requestId       : Firestore document ID of the food_request
 *  - requesterName   : display name of the person who posted the request
 *  - requesterUid    : UID of the requester
 *  - foodName        : requested food name
 *  - quantity        : requested quantity string
 *  - location        : pick-up / delivery location
 *  - reason          : why the requester needs food
 *  - urgency         : urgency level (e.g. "High", "Normal")
 */
class FulfillRequestActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fulfill_request)

        // ── Read extras ──────────────────────────────────────────────────────
        val requestId     = intent.getStringExtra("requestId")     ?: ""
        val requesterName = intent.getStringExtra("requesterName") ?: "Anonymous"
        val requesterUid  = intent.getStringExtra("requesterUid")  ?: ""
        val foodName      = intent.getStringExtra("foodName")      ?: ""
        val quantity      = intent.getStringExtra("quantity")      ?: ""
        val location      = intent.getStringExtra("location")      ?: ""
        val reason        = intent.getStringExtra("reason")        ?: "No reason provided"
        val urgency       = intent.getStringExtra("urgency")       ?: "Normal"

        // ── Bind views ───────────────────────────────────────────────────────
        val tvRequesterName : TextView         = findViewById(R.id.tvRequesterName)
        val tvFoodName      : TextView         = findViewById(R.id.tvFulfillFoodName)
        val tvQuantity      : TextView         = findViewById(R.id.tvFulfillQuantity)
        val tvLocation      : TextView         = findViewById(R.id.tvFulfillLocation)
        val tvReason        : TextView         = findViewById(R.id.tvFulfillReason)
        val tvUrgency       : TextView         = findViewById(R.id.tvFulfillUrgency)
        val etNote          : TextInputEditText = findViewById(R.id.etFulfillNote)
        val btnConfirm      : MaterialButton   = findViewById(R.id.btnConfirmFulfill)
        val btnChat         : MaterialButton   = findViewById(R.id.btnFulfillChat)
        val btnBack         : View             = findViewById(R.id.btnFulfillBack)

        // ── Populate UI ──────────────────────────────────────────────────────
        tvRequesterName.text = requesterName
        tvFoodName.text      = foodName.ifEmpty { "Not specified" }
        tvQuantity.text      = quantity.ifEmpty { "Not specified" }
        tvLocation.text      = location.ifEmpty { "Not specified" }
        tvReason.text        = reason
        tvUrgency.text       = urgency

        // Colour-code urgency badge
        val urgencyColor = when (urgency.lowercase()) {
            "high"   -> getColor(R.color.red_error)
            "medium" -> getColor(R.color.orange_warn)
            else     -> getColor(R.color.green_primary)
        }
        tvUrgency.setTextColor(urgencyColor)

        // ── Back button ──────────────────────────────────────────────────────
        btnBack.setOnClickListener { finish() }

        // ── Chat button ──────────────────────────────────────────────────────
        btnChat.setOnClickListener {
            val myUid = FirebaseHelper.currentUid ?: run {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (myUid == requesterUid) {
                Toast.makeText(this, "Cannot chat with yourself", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val chatId = listOf(myUid, requesterUid).sorted().joinToString("_")
            db.collection("chats").document(chatId).set(
                mapOf(
                    "participants"     to listOf(myUid, requesterUid),
                    "participantNames" to mapOf(requesterUid to requesterName),
                    "lastUpdated"      to Timestamp.now()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("chatId",    chatId)
                putExtra("otherName", requesterName)
            })
        }

        // ── Confirm fulfillment ──────────────────────────────────────────────
        btnConfirm.setOnClickListener {
            val myUid  = FirebaseHelper.currentUid ?: run {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val note = etNote.text?.toString()?.trim() ?: ""

            btnConfirm.isEnabled = false
            btnConfirm.text = "Submitting…"

            // 1. Write a fulfillment record
            val fulfillmentData = hashMapOf(
                "requestId"     to requestId,
                "requesterUid"  to requesterUid,
                "donorUid"      to myUid,
                "foodName"      to foodName,
                "quantity"      to quantity,
                "location"      to location,
                "note"          to note,
                "status"        to "pending",       // requester can confirm receipt
                "createdAt"     to Timestamp.now()
            )

            db.collection("fulfillments").add(fulfillmentData)
                .addOnSuccessListener {
                    // 2. Mark the request as "being fulfilled" so it disappears from browse
                    db.collection("food_requests").document(requestId)
                        .update(
                            mapOf(
                                "status"    to "claimed",
                                "donorUid"  to myUid,
                                "claimedAt" to Timestamp.now()
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Thank you! The requester has been notified.", Toast.LENGTH_LONG).show()
                            // Optionally navigate to home or orders screen
                            startActivity(Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            })
                            finish()
                        }
                        .addOnFailureListener { e ->
                            btnConfirm.isEnabled = true
                            btnConfirm.text = "Confirm & Fulfill"
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Confirm & Fulfill"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}