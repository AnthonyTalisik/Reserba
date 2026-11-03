package com.example.reserba.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.reserba.data.MockData
import com.example.reserba.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnLogin.setOnClickListener { attemptLogin() }
        b.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
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
