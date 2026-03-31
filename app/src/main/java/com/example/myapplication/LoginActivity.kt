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
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSignIn: MaterialButton
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupSignUpText()
        setupSignInButton()
    }

    private fun initViews() {
        tilEmail    = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail     = findViewById(R.id.etEmail)
        etPassword  = findViewById(R.id.etPassword)
        btnSignIn   = findViewById(R.id.btnSignIn)
        tvSignUp    = findViewById(R.id.tvSignUp)
    }

    private fun setupSignUpText() {
        val fullText    = "Don't have an account? Sign Up"
        val spannable   = SpannableString(fullText)
        val signUpStart = fullText.indexOf("Sign Up")
        val signUpEnd   = signUpStart + "Sign Up".length

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.green_primary)),
            signUpStart, signUpEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) { navigateToSignUp() }
            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.isUnderlineText = false
                ds.color = ContextCompat.getColor(this@LoginActivity, R.color.green_primary)
            }
        }, signUpStart, signUpEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvSignUp.text           = spannable
        tvSignUp.movementMethod = LinkMovementMethod.getInstance()
        tvSignUp.highlightColor = android.graphics.Color.TRANSPARENT
        tvSignUp.textSize       = 15f
        tvSignUp.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    private fun setupSignInButton() {
        btnSignIn.setOnClickListener {
            if (validateInputs()) performSignIn()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email    = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()

        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            isValid = false
        } else {
            tilEmail.error = null
        }

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

    private fun performSignIn() {
        getSharedPreferences("foodbridge_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("is_logged_in", true)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }
}