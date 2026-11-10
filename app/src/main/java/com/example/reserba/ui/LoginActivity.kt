package com.example.reserba.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.reserba.data.MockData
import com.example.reserba.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvShowPassword.setOnClickListener { togglePassword() }
        b.btnLogin.setOnClickListener { attemptLogin() }
        b.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /** "Show / Hide" text next to the password field. */
    private fun togglePassword() {
        passwordVisible = !passwordVisible
        b.etPassword.transformationMethod =
            if (passwordVisible) HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
        b.etPassword.setSelection(b.etPassword.text?.length ?: 0)
        b.tvShowPassword.text = if (passwordVisible) "Hide" else "Show"
    }

    private fun attemptLogin() {
        b.tilEmail.error = null
        b.tilPassword.error = null
        b.tvError.visibility = View.GONE

        val email = b.etEmail.text.toString().trim()
        val password = b.etPassword.text.toString()

        var valid = true
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.error = "Enter a valid email address"
            valid = false
        }
        if (password.length < 6) {
            b.tilPassword.error = "Password must be at least 6 characters"
            valid = false
        }
        if (!valid) return

        val result = MockData.login(email, password)
        if (result.user == null) {
            b.tvError.text = result.error
            b.tvError.visibility = View.VISIBLE
            return
        }

        // Role-based start: MainActivity shows the requester or administrator home.
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}