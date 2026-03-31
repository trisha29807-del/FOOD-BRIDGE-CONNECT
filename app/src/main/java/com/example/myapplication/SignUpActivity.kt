package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
    private lateinit var btnCreateAccount: MaterialButton
    private lateinit var tvSignIn: TextView

    private var selectedRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        initViews()
        setupRoleSelection()
        setupCreateAccountButton()
        setupSignInText()
    }

    private fun initViews() {
        cardDonor        = findViewById(R.id.cardDonor)
        cardNgo          = findViewById(R.id.cardNgo)
        cardBuyer        = findViewById(R.id.cardBuyer)
        tilFullName      = findViewById(R.id.tilFullName)
        tilEmail         = findViewById(R.id.tilEmail)
        tilPassword      = findViewById(R.id.tilPassword)
        etFullName       = findViewById(R.id.etFullName)
        etEmail          = findViewById(R.id.etEmail)
        etPassword       = findViewById(R.id.etPassword)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvSignIn         = findViewById(R.id.tvSignIn)
    }

    private fun setupRoleSelection() {
        cardDonor.setOnClickListener { selectRole("Donor / Seller", cardDonor) }
        cardNgo.setOnClickListener   { selectRole("NGO / Orphanage", cardNgo) }
        cardBuyer.setOnClickListener { selectRole("Buyer", cardBuyer) }
    }

    private fun selectRole(role: String, selectedCard: CardView) {
        selectedRole = role
        // Reset all cards to white
        listOf(cardDonor, cardNgo, cardBuyer).forEach { card ->
            card.setCardBackgroundColor(0xFFFFFFFF.toInt())
            card.cardElevation = 2f
        }
        // Highlight selected card in light green
        selectedCard.setCardBackgroundColor(0xFFE8F5E9.toInt())
        selectedCard.cardElevation = 6f
    }

    private fun setupCreateAccountButton() {
        btnCreateAccount.setOnClickListener {
            if (validateInputs()) performSignUp()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (selectedRole.isEmpty()) {
            android.widget.Toast.makeText(this, "Please select your role", android.widget.Toast.LENGTH_SHORT).show()
            isValid = false
        }

        val fullName = etFullName.text?.toString()?.trim().orEmpty()
        if (fullName.isEmpty()) {
            tilFullName.error = "Full name is required"
            isValid = false
        } else {
            tilFullName.error = null
        }

        val email = etEmail.text?.toString()?.trim().orEmpty()
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            isValid = false
        } else {
            tilEmail.error = null
        }

        val password = etPassword.text?.toString().orEmpty()
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun performSignUp() {
        // TODO: Replace with real registration (Firebase, Retrofit, etc.)
        getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("is_logged_in", true)
            .putString("user_role", selectedRole)
            .putString("user_email", etEmail.text?.toString()?.trim())
            .putString("user_name", etFullName.text?.toString()?.trim())
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setupSignInText() {
        val fullText    = "Already have an account? Sign In"
        val spannable   = SpannableString(fullText)
        val signInStart = fullText.indexOf("Sign In")
        val signInEnd   = signInStart + "Sign In".length

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.green_primary)),
            signInStart, signInEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                finish()
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.isUnderlineText = false
                ds.color = ContextCompat.getColor(this@SignUpActivity, R.color.green_primary)
            }
        }, signInStart, signInEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvSignIn.text           = spannable
        tvSignIn.movementMethod = LinkMovementMethod.getInstance()
        tvSignIn.highlightColor = android.graphics.Color.TRANSPARENT
        tvSignIn.textSize       = 15f
        tvSignIn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }
}