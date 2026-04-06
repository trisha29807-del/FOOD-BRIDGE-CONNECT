package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var cardDonor: CardView
    private lateinit var cardNgo: CardView
    private lateinit var cardBuyer: CardView
    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnCreate: MaterialButton
    private lateinit var tvSignIn: TextView

    private var selectedRole: String = "Donor/Seller"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        bindViews()
        setupRoleCards()
        setupCreateAccount()
        setupSignInLink()
    }

    private fun bindViews() {
        cardDonor   = findViewById(R.id.cardDonor)
        cardNgo     = findViewById(R.id.cardNgo)
        cardBuyer   = findViewById(R.id.cardBuyer)
        tilFullName = findViewById(R.id.tilFullName)
        tilEmail    = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etFullName  = findViewById(R.id.etFullName)
        etEmail     = findViewById(R.id.etEmail)
        etPassword  = findViewById(R.id.etPassword)
        btnCreate   = findViewById(R.id.btnCreateAccount)
        tvSignIn    = findViewById(R.id.tvSignIn)
    }

    private fun setupRoleCards() {
        highlightCard(cardDonor)
        cardDonor.setOnClickListener {
            selectedRole = "Donor/Seller"
            highlightCard(cardDonor); resetCard(cardNgo); resetCard(cardBuyer)
        }
        cardNgo.setOnClickListener {
            selectedRole = "NGO/Orphanage"
            highlightCard(cardNgo); resetCard(cardDonor); resetCard(cardBuyer)
        }
        cardBuyer.setOnClickListener {
            selectedRole = "Buyer"
            highlightCard(cardBuyer); resetCard(cardDonor); resetCard(cardNgo)
        }
    }

    // Uses background color only — plain CardView doesn't support stroke
    private fun highlightCard(card: CardView) {
        card.setCardBackgroundColor(
            ContextCompat.getColor(this, android.R.color.holo_green_light)
        )
    }

    private fun resetCard(card: CardView) {
        card.setCardBackgroundColor(
            ContextCompat.getColor(this, android.R.color.white)
        )
    }

    private fun setupCreateAccount() {
        btnCreate.setOnClickListener {
            val name     = etFullName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            var isValid = true
            if (name.isEmpty()) {
                tilFullName.error = "Name is required"; isValid = false
            } else { tilFullName.error = null }
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email"; isValid = false
            } else { tilEmail.error = null }
            if (password.length < 6) {
                tilPassword.error = "Min 6 characters"; isValid = false
            } else { tilPassword.error = null }
            if (!isValid) return@setOnClickListener

            setLoading(true)
            lifecycleScope.launch {
                val result = FirebaseHelper.signUp(name, email, password, selectedRole)
                setLoading(false)
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@SignUpActivity,
                            "Account created! Welcome to FoodBridge!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                        finishAffinity()
                    },
                    onFailure = { exception ->
                        val message = when {
                            exception.message?.contains("email address is already in use") == true ->
                                "This email is already registered"
                            exception.message?.contains("network") == true -> "No internet connection"
                            else -> "Sign up failed. Please try again."
                        }
                        tilEmail.error = message
                    }
                )
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnCreate.isEnabled = !loading
        btnCreate.text = if (loading) "Creating account..." else "Create Account"
    }

    private fun setupSignInLink() {
        tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}