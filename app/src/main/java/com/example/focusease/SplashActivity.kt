package com.example.focusease

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ambil komponen dari XML
        val logoImage = findViewById<View>(R.id.logoImage)
        val bottomShape = findViewById<View>(R.id.bottomShape)

        // Logo muncul dulu
        val fadeInLogo = AlphaAnimation(0f, 1f)
        fadeInLogo.duration = 1000
        logoImage.startAnimation(fadeInLogo)

        // Setelah 1,5 detik baru bagian ungu muncul
        Handler().postDelayed({
            bottomShape.visibility = View.VISIBLE
            val fadeInBottom = AlphaAnimation(0f, 1f)
            fadeInBottom.duration = 800
            bottomShape.startAnimation(fadeInBottom)
        }, 1500)

        // Setelah animasi selesai, pindah ke Sign Up
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)

    }
}
