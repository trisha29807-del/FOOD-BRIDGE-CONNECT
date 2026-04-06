package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSignIn: MaterialButton
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseHelper.isLoggedIn()) {
            goToMain(); return
        }

        setContentView(R.layout.activity_login)
        bindViews()
        setupSignUpLink()
        setupSignIn()
    }

    private fun bindViews() {
        tilEmail    = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail     = findViewById(R.id.etEmail)
        etPassword  = findViewById(R.id.etPassword)
        btnSignIn   = findViewById(R.id.btnSignIn)
        tvSignUp    = findViewById(R.id.tvSignUp)
    }

    private fun setupSignIn() {
        btnSignIn.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            var isValid = true
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email"; isValid = false
            } else { tilEmail.error = null }
            if (password.length < 6) {
                tilPassword.error = "Min 6 characters"; isValid = false
            } else { tilPassword.error = null }
            if (!isValid) return@setOnClickListener

            setLoading(true)
            lifecycleScope.launch {
                val result = FirebaseHelper.signIn(email, password)
                setLoading(false)
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                        goToMain()
                    },
                    onFailure = { exception ->
                        val message = when {
                            exception.message?.contains("no user record") == true -> "No account found with this email"
                            exception.message?.contains("password is invalid") == true -> "Incorrect password"
                            exception.message?.contains("network") == true -> "No internet connection"
                            else -> "Sign in failed. Please try again."
                        }
                        tilPassword.error = message
                    }
                )
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSignIn.isEnabled = !loading
        btnSignIn.text = if (loading) "Signing in..." else "Sign In"
        etEmail.isEnabled = !loading
        etPassword.isEnabled = !loading
    }

    private fun setupSignUpLink() {
        val fullText = "Don't have an account? Sign Up"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf("Sign Up")
        val end = start + "Sign Up".length
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.green_primary)),
            start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvSignUp.text = spannable
        tvSignUp.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
