package com.example.focusease

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var userPrefs: UserPreferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tvGreeting: TextView
    private lateinit var ivMenu: ImageView

    private val profileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val refresh = result.data?.getBooleanExtra("refresh_data", false) ?: false
                if (refresh) {
                    loadUserData()
                }
            }
        }

    private val dateItemIds = listOf(
        R.id.date1, R.id.date2, R.id.date3,
        R.id.date4, R.id.date5, R.id.date6
    )

    private var selectedIndex = 3
    private val colorSelectedText = Color.WHITE
    private val colorNormalText = "#666666"
    private val colorBlackText = "#000000"

    private val dayFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
    private val dateFormat = SimpleDateFormat("d", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language BEFORE super.onCreate
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        applySavedTheme()
        applySavedLanguage()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userPrefs = UserPreferences(this)

        // DEBUG: Test simpan username manual
        val testUsername = sharedPreferences.getString("username", null)
        if (testUsername == null) {
            // Kalau username belum ada, set manual untuk test
            val editor = sharedPreferences.edit()
            editor.putString("username", "TestUser")
            editor.commit()
            android.util.Log.d("MAIN_DEBUG", "⚠️ Username NULL - Set manual TestUser")
        }

        if (!userPrefs.isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Inisialisasi views
        tvGreeting = findViewById(R.id.tvGreeting)
        ivMenu = findViewById(R.id.ivMenu)

        // TEST: Set text langsung untuk cek TextView berfungsi atau tidak
        tvGreeting.text = "TESTING - Hi, Hardcoded User"
        android.util.Log.d("MAIN_DEBUG", "TextView set to: ${tvGreeting.text}")

        // Load username dari login
        loadUserData()

        ivMenu.setOnClickListener {
            showTopMenu()
        }

        updateDateItemsView()
        setupBottomNavigation()
    }

    private fun applySavedTheme() {
        val themeMode = sharedPreferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun applySavedLanguage() {
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun showTopMenu() {
        val wrapper = ContextThemeWrapper(this, R.style.PopupMenuStyle)
        val popup = PopupMenu(wrapper, ivMenu)

        popup.menu.add("Profile")
        popup.menu.add("Help")
        popup.menu.add("Logout")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Profile" -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    profileLauncher.launch(intent)
                    true
                }
                "Help" -> {
                    Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                "Logout" -> {
                    userPrefs.logout()

                    // Clear semua data
                    val editor = sharedPreferences.edit()
                    editor.clear()
                    editor.apply()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun loadUserData() {
        // DEBUG: Cek semua data di SharedPreferences
        Log.d("MAIN_DEBUG", "=== CHECKING SHARED PREFERENCES ===")
        Log.d("MAIN_DEBUG", "All keys: ${sharedPreferences.all.keys}")

        // Ambil username dari SharedPreferences (tersimpan saat login/register)
        val username = sharedPreferences.getString("username", null)
        Log.d("MAIN_DEBUG", "Username from SharedPrefs: '$username'")

        // Ambil dari UserPrefs juga
        val userPrefUsername = userPrefs.getUsername()
        Log.d("MAIN_DEBUG", "Username from UserPrefs: '$userPrefUsername'")

        // Prioritas: SharedPreferences dulu, baru UserPrefs
        val displayName = when {
            !username.isNullOrEmpty() -> {
                Log.d("MAIN_DEBUG", "✅ Using SharedPrefs username")
                username
            }
            !userPrefUsername.isNullOrEmpty() && userPrefUsername != "User" -> {
                Log.d("MAIN_DEBUG", "✅ Using UserPrefs username")
                userPrefUsername
            }
            else -> {
                Log.d("MAIN_DEBUG", "⚠️ No username found, using default")
                "User"
            }
        }

        val finalText = "Hi, $displayName"
        tvGreeting.text = finalText
        Log.d("MAIN_DEBUG", "Final display: '$finalText'")

        // TOAST untuk memastikan fungsi ini dipanggil
        Toast.makeText(this, "Welcome : s $finalText", Toast.LENGTH_LONG).show()
    }

    private fun updateDateItemsView() {
        val calendar = Calendar.getInstance()

        dateItemIds.forEachIndexed { index, itemId ->
            try {
                val dateItemView = findViewById<View>(itemId)

                val textViews = mutableListOf<TextView>()
                collectAllTextViews(dateItemView, textViews)

                Log.d("DATE_DEBUG", "Index $index: Found ${textViews.size} TextViews")

                if (textViews.size >= 2) {
                    val tvDay = textViews[0]
                    val tvDate = textViews[1]

                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.add(Calendar.DAY_OF_YEAR, index)
                    val currentDate = calendar.time

                    val dayText = dayFormat.format(currentDate)
                    val dateText = dateFormat.format(currentDate)

                    tvDay.text = dayText
                    tvDate.text = dateText

                    Log.d("DATE_SET", "Index $index: $dayText $dateText")

                    val isSelected = index == selectedIndex

                    var rootLayout: ViewGroup? = dateItemView as? ViewGroup
                    if (rootLayout?.childCount == 1) {
                        rootLayout = rootLayout.getChildAt(0) as? ViewGroup
                    }

                    if (rootLayout != null) {
                        if (isSelected) {
                            rootLayout.setBackgroundResource(R.drawable.date_item_selected_bg)
                            tvDay.setTextColor(colorSelectedText)
                            tvDate.setTextColor(colorSelectedText)
                        } else {
                            rootLayout.setBackgroundResource(R.drawable.date_item_normal_bg)
                            tvDay.setTextColor(Color.parseColor(colorNormalText))
                            tvDate.setTextColor(Color.parseColor(colorBlackText))
                        }

                        rootLayout.setOnClickListener {
                            handleDateItemClick(index)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DATE_ERROR", "Error at index $index: ${e.message}")
            }
        }
    }

    private fun collectAllTextViews(view: View, list: MutableList<TextView>) {
        when (view) {
            is TextView -> list.add(view)
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    collectAllTextViews(view.getChildAt(i), list)
                }
            }
        }
    }

    private fun handleDateItemClick(newIndex: Int) {
        if (newIndex != selectedIndex) {
            selectedIndex = newIndex
            updateDateItemsView()
        }
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<View>(R.id.navHome)
        val navMusic = findViewById<View>(R.id.navMusic)
        val navClock = findViewById<View>(R.id.navClock)
        val navProfile = findViewById<View>(R.id.navProfile)

        navHome?.setOnClickListener {
            Log.d("NAV", "Home clicked")
        }

        navMusic?.setOnClickListener {
            val intent = Intent(this, MusicActivity::class.java)
            startActivity(intent)
            Log.d("NAV", "Music clicked")
        }

        navClock?.setOnClickListener {
            val intent = Intent(this, ClockActivity::class.java)
            startActivity(intent)
            Log.d("NAV", "Clock clicked")
        }

        navProfile?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            profileLauncher.launch(intent)
            Log.d("NAV", "Profile clicked")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MAIN_DEBUG", "onResume called, reloading username...")
        loadUserData()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("MAIN_DEBUG", "onRestart called, reloading username...")
        loadUserData()
    }
}