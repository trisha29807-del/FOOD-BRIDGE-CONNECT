package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PlaceOrderActivity : AppCompatActivity() {

    private lateinit var tvFoodName: TextView
    private lateinit var tvFoodType: TextView
    private lateinit var tvDonorName: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var tvFoodLocation: TextView
    private lateinit var etDeliveryAddress: TextInputEditText
    private lateinit var etContactNumber: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var rgPayment: RadioGroup
    private lateinit var rbCod: RadioButton
    private lateinit var rbUpi: RadioButton
    private lateinit var rbCard: RadioButton
    private lateinit var layoutUpi: LinearLayout
    private lateinit var layoutCard: LinearLayout
    private lateinit var etUpiId: TextInputEditText
    private lateinit var etCardNumber: TextInputEditText
    private lateinit var etCardExpiry: TextInputEditText
    private lateinit var etCardCvv: TextInputEditText
    private lateinit var btnPlaceOrder: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private var listingId = ""
    private var foodName = ""
    private var donorUid = ""
    private var donorName = ""
    private var quantity = ""
    private var location = ""
    private var foodType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_order)

        // Get listing details passed from BrowseActivity
        listingId  = intent.getStringExtra("listingId")  ?: ""
        foodName   = intent.getStringExtra("foodName")   ?: ""
        donorUid   = intent.getStringExtra("donorUid")   ?: ""
        donorName  = intent.getStringExtra("donorName")  ?: ""
        quantity   = intent.getStringExtra("quantity")   ?: ""
        location   = intent.getStringExtra("location")   ?: ""
        foodType   = intent.getStringExtra("foodType")   ?: ""

        bindViews()
        populateFoodDetails()
        preFillUserDetails()
        setupPaymentToggle()
        setupPlaceOrder()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun bindViews() {
        tvFoodName        = findViewById(R.id.tvFoodName)
        tvFoodType        = findViewById(R.id.tvFoodType)
        tvDonorName       = findViewById(R.id.tvDonorName)
        tvQuantity        = findViewById(R.id.tvQuantity)
        tvFoodLocation    = findViewById(R.id.tvFoodLocation)
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress)
        etContactNumber   = findViewById(R.id.etContactNumber)
        etNotes           = findViewById(R.id.etNotes)
        rgPayment         = findViewById(R.id.rgPayment)
        rbCod             = findViewById(R.id.rbCod)
        rbUpi             = findViewById(R.id.rbUpi)
        rbCard            = findViewById(R.id.rbCard)
        layoutUpi         = findViewById(R.id.layoutUpi)
        layoutCard        = findViewById(R.id.layoutCard)
        etUpiId           = findViewById(R.id.etUpiId)
        etCardNumber      = findViewById(R.id.etCardNumber)
        etCardExpiry      = findViewById(R.id.etCardExpiry)
        etCardCvv         = findViewById(R.id.etCardCvv)
        btnPlaceOrder     = findViewById(R.id.btnPlaceOrder)
        progressBar       = findViewById(R.id.progressBar)
    }

    private fun populateFoodDetails() {
        tvFoodName.text     = foodName
        tvFoodType.text     = foodType
        tvDonorName.text    = "From: $donorName"
        tvQuantity.text     = "📦 $quantity"
        tvFoodLocation.text = "📍 $location"
    }

    private fun preFillUserDetails() {
        val uid = FirebaseHelper.currentUid ?: return
        lifecycleScope.launch {
            val result = FirebaseHelper.getUserProfile(uid)
            result.getOrNull()?.let { data ->
                val contact  = data["contact"] as? String ?: ""
                val userLoc  = data["location"] as? String ?: ""
                if (contact.isNotEmpty())  etContactNumber.setText(contact)
                if (userLoc.isNotEmpty())  etDeliveryAddress.setText(userLoc)
            }
        }
    }

    private fun setupPaymentToggle() {
        // Default: COD selected, UPI and Card hidden
        layoutUpi.visibility  = View.GONE
        layoutCard.visibility = View.GONE

        rgPayment.setOnCheckedChangeListener { _, checkedId ->
            layoutUpi.visibility  = if (checkedId == R.id.rbUpi)  View.VISIBLE else View.GONE
            layoutCard.visibility = if (checkedId == R.id.rbCard) View.VISIBLE else View.GONE
        }
    }

    private fun setupPlaceOrder() {
        btnPlaceOrder.setOnClickListener {
            val address = etDeliveryAddress.text.toString().trim()
            val contact = etContactNumber.text.toString().trim()
            val notes   = etNotes.text.toString().trim()

            // Validation
            if (address.isEmpty()) {
                etDeliveryAddress.error = "Delivery address is required"
                return@setOnClickListener
            }
            if (contact.isEmpty()) {
                etContactNumber.error = "Contact number is required"
                return@setOnClickListener
            }

            val paymentMethod = when (rgPayment.checkedRadioButtonId) {
                R.id.rbUpi  -> {
                    val upiId = etUpiId.text.toString().trim()
                    if (upiId.isEmpty()) {
                        etUpiId.error = "Enter UPI ID"
                        return@setOnClickListener
                    }
                    "UPI ($upiId)"
                }
                R.id.rbCard -> {
                    val cardNum = etCardNumber.text.toString().trim()
                    if (cardNum.length < 16) {
                        etCardNumber.error = "Enter valid card number"
                        return@setOnClickListener
                    }
                    "Card (ending ${cardNum.takeLast(4)})"
                }
                else -> "Cash on Delivery"
            }

            val uid = FirebaseHelper.currentUid ?: run {
                Toast.makeText(this, "Please sign in again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)

            lifecycleScope.launch {
                try {
                    // 1. Update listing status to claimed
                    db.collection("food_listings").document(listingId)
                        .update(mapOf(
                            "status"          to "claimed",
                            "claimedByUid"    to uid,
                            "claimedAt"       to Timestamp.now()
                        )).await()

                    // 2. Save full order to orders collection
                    val order = mapOf(
                        "listingId"       to listingId,
                        "buyerUid"        to uid,
                        "donorUid"        to donorUid,
                        "donorName"       to donorName,
                        "foodName"        to foodName,
                        "foodType"        to foodType,
                        "quantity"        to quantity,
                        "pickupLocation"  to location,
                        "deliveryAddress" to address,
                        "contactNumber"   to contact,
                        "notes"           to notes,
                        "paymentMethod"   to paymentMethod,
                        "paymentStatus"   to if (paymentMethod == "Cash on Delivery")
                            "pending" else "paid",
                        "orderStatus"     to "confirmed",
                        "createdAt"       to Timestamp.now()
                    )
                    db.collection("orders").add(order).await()

                    setLoading(false)

                    // 3. Go to order confirmation screen
                    val intent = Intent(this@PlaceOrderActivity, OrderConfirmationActivity::class.java)
                    intent.putExtra("foodName",       foodName)
                    intent.putExtra("donorName",      donorName)
                    intent.putExtra("address",        address)
                    intent.putExtra("contact",        contact)
                    intent.putExtra("paymentMethod",  paymentMethod)
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    setLoading(false)
                    Toast.makeText(this@PlaceOrderActivity,
                        "Order failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnPlaceOrder.isEnabled = !loading
        btnPlaceOrder.text = if (loading) "Placing order..." else "Place Order"
    }
}
