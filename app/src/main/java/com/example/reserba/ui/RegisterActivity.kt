package com.example.reserba.ui

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.reserba.R
import com.example.reserba.data.FirebaseRepository
import com.example.reserba.data.Organizations
import com.example.reserba.databinding.ActivityRegisterBinding
import com.example.reserba.model.Role
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupOrgDropdown()
        b.rgRole.setOnCheckedChangeListener { _, _ -> setupOrgDropdown() }

        b.btnRegister.setOnClickListener { attemptRegister() }
        b.tvBackToLogin.setOnClickListener { finish() }
    }

    private fun setupOrgDropdown() {
        val role = if (b.rgRole.checkedRadioButtonId == R.id.rbFaculty) Role.FACULTY else Role.STUDENT
        val orgList = Organizations.forRole(role)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, orgList)
        b.actvOrg.setAdapter(adapter)
        b.actvOrg.setText("", false)
    }

    private fun attemptRegister() {
        listOf(
            b.tilFirstName, b.tilMiddleName, b.tilLastName,
            b.tilIdNumber, b.tilOrg, b.tilEmail, b.tilPassword
        ).forEach { it.error = null }
        b.tvError.visibility = View.GONE

        val firstName = b.etFirstName.text.toString().trim()
        val middleName = b.etMiddleName.text.toString().trim()
        val lastName = b.etLastName.text.toString().trim()
        val id = b.etIdNumber.text.toString().trim()
        val org = b.actvOrg.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val password = b.etPassword.text.toString()
        val role = if (b.rgRole.checkedRadioButtonId == R.id.rbFaculty) Role.FACULTY else Role.STUDENT

        // BR-12 required-field validation
        var valid = true
        if (firstName.isEmpty()) { b.tilFirstName.error = "First name is required"; valid = false }
        if (lastName.isEmpty()) { b.tilLastName.error = "Last name is required"; valid = false }
        if (id.isEmpty()) { b.tilIdNumber.error = "ID number is required"; valid = false }
        if (org.isEmpty()) { b.tilOrg.error = "Please select an organization / department"; valid = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tilEmail.error = "Enter a valid email address"; valid = false }
        if (password.length < 6) { b.tilPassword.error = "Password must be at least 6 characters"; valid = false }
        if (!valid) return

        b.btnRegister.isEnabled = false
        b.btnRegister.text = "Registering..."

        lifecycleScope.launch {
            val error = FirebaseRepository.register(
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                idNumber = id,
                organization = org,
                email = email,
                password = password,
                role = role
            )
            if (error != null) {
                b.tvError.text = error
                b.tvError.visibility = View.VISIBLE
                b.btnRegister.isEnabled = true
                b.btnRegister.text = "Submit for verification"
                return@launch
            }

            toast("Registration sent. You can log in once the administrator verifies your ID.")
            finish()
        }
    }
}
