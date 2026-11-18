package com.example.reserba.util

import java.security.MessageDigest

object PasswordUtils {

    /**
     * Hashes password string using SHA-256 algorithm.
     */
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies plain text input password against stored hashed password
     * (with plain text fallback for legacy accounts).
     */
    fun verify(inputPassword: String, storedHashOrPlain: String): Boolean {
        if (storedHashOrPlain == inputPassword) return true
        return hash(inputPassword) == storedHashOrPlain
    }
}
