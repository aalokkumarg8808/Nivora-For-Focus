package com.example.util

import java.security.MessageDigest

object PasswordHasher {
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, hash: String): Boolean {
        return hash(password).equals(hash, ignoreCase = true)
    }
}
