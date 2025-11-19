package com.example.focusease

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.focusease.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userPrefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userPrefs = UserPreferences(this)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsernameLogin.text.toString().trim()
            val password = binding.etPasswordLogin.text.toString().trim()

            val savedUsername = sharedPreferences.getString("username", "")
            val savedPassword = sharedPreferences.getString("password", "")

            // Debugging (cek di logcat)
            println("Saved Username: $savedUsername")
            println("Saved Password: $savedPassword")
            println("Input Username: $username")
            println("Input Password: $password")

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (username == savedUsername && password == savedPassword) {
                // Login berhasil - simpan username ke SharedPreferences DAN UserPreferences
                val editor = sharedPreferences.edit()
                editor.putString("username", username)
                editor.putBoolean("logged_in", true)
                editor.apply()

                userPrefs.saveUsername(username)

                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}