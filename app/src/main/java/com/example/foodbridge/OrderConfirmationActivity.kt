package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class OrderConfirmationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        val foodName      = intent.getStringExtra("foodName")      ?: ""
        val donorName     = intent.getStringExtra("donorName")     ?: ""
        val address       = intent.getStringExtra("address")       ?: ""
        val contact       = intent.getStringExtra("contact")       ?: ""
        val paymentMethod = intent.getStringExtra("paymentMethod") ?: ""

        findViewById<TextView>(R.id.tvFoodName).text      = foodName
        findViewById<TextView>(R.id.tvDonorName).text     = "From: $donorName"
        findViewById<TextView>(R.id.tvAddress).text       = "📍 $address"
        findViewById<TextView>(R.id.tvContact).text       = "📞 $contact"
        findViewById<TextView>(R.id.tvPaymentMethod).text = "💳 $paymentMethod"

        // Track order button
        findViewById<MaterialButton>(R.id.btnTrackOrder).setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
            finish()
        }

        // Back to home
        findViewById<MaterialButton>(R.id.btnGoHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}
