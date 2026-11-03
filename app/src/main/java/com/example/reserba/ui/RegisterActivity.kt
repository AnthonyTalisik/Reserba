package com.example.reserba.ui

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.reserba.R
import com.example.reserba.data.MockData
import com.example.reserba.databinding.ActivityRegisterBinding
import com.example.reserba.model.Role

class RegisterActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnRegister.setOnClickListener { attemptRegister() }
        b.tvBackToLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        listOf(b.tilFullName, b.tilIdNumber, b.tilOrg, b.tilEmail, b.tilPassword).forEach { it.error = null }
        b.tvError.visibility = View.GONE

        val name = b.etFullName.text.toString().trim()
        val id = b.etIdNumber.text.toString().trim()
        val org = b.etOrg.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val password = b.etPassword.text.toString()
        val role = if (b.rgRole.checkedRadioButtonId == R.id.rbFaculty) Role.FACULTY else Role.STUDENT

        // BR-12 required-field validation
        var valid = true
        if (name.isEmpty()) { b.tilFullName.error = "Full name is required"; valid = false }
        if (id.isEmpty()) { b.tilIdNumber.error = "ID number is required"; valid = false }
        if (org.isEmpty()) { b.tilOrg.error = "Organization or department is required"; valid = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tilEmail.error = "Enter a valid email address"; valid = false }
        if (password.length < 6) { b.tilPassword.error = "Password must be at least 6 characters"; valid = false }
        if (!valid) return

        val error = MockData.register(name, id, org, email, password, role)
        if (error != null) {
            b.tvError.text = error
            b.tvError.visibility = View.VISIBLE
            return
        }

        toast("Registration sent. You can log in once the administrator verifies your ID.")
        finish()
    }
}
