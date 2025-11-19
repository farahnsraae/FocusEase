package com.example.focusease

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    // PERBAIKAN: Gunakan nama yang SAMA dengan LoginActivity & MainActivity
    private val prefs: SharedPreferences =
        context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_LOGGED_IN = "logged_in"
    }

    // Simpan username saat register/login
    fun saveUsername(username: String) {
        prefs.edit().apply {
            putString(KEY_USERNAME, username)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    // Ambil username
    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "User") ?: "User"
    }

    // Cek apakah user sudah login
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Logout - hapus semua data
    fun logout() {
        prefs.edit().apply {
            clear()
            apply()
        }
    }
}