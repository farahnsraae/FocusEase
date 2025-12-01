package com.example.focusease

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userPrefs: UserPreferences
    private lateinit var txtProfileName: TextView
    private lateinit var txtProfileEmail: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var txtCurrentLanguage: TextView  // ✅ TAMBAHAN BARU

    private val PICK_IMAGE_REQUEST = 1
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        applySavedTheme()
        applySavedLanguage()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userPrefs = UserPreferences(this)

        txtProfileName = findViewById(R.id.txtProfileName)
        txtProfileEmail = findViewById(R.id.txtProfileEmail)
        imgProfile = findViewById(R.id.imgProfile)
        txtCurrentLanguage = findViewById(R.id.txtCurrentLanguage)  // ✅ TAMBAHAN BARU

        loadUserData()
        loadProfileImage()
        updateLanguageDisplay()  // ✅ TAMBAHAN BARU

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("refresh_data", true)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        val btnEditPhoto = findViewById<ImageButton>(R.id.btnEditPhoto)
        btnEditPhoto.setOnClickListener {
            checkPermissionAndPickImage()
        }

        val btnEditProfile = findViewById<LinearLayout>(R.id.btnEditProfile)
        btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        val btnChangePassword = findViewById<LinearLayout>(R.id.btnChangePassword)
        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        val btnNotifications = findViewById<LinearLayout>(R.id.btnNotifications)
        btnNotifications.setOnClickListener {
            Toast.makeText(this, getLocalizedString("notifications"), Toast.LENGTH_SHORT).show()
        }

        val btnTheme = findViewById<LinearLayout>(R.id.btnTheme)
        btnTheme.setOnClickListener {
            showThemeDialog()
        }

        val btnLanguage = findViewById<LinearLayout>(R.id.btnLanguage)
        btnLanguage.setOnClickListener {
            showLanguageDialog()
        }

        val btnHelp = findViewById<LinearLayout>(R.id.btnHelp)
        btnHelp.setOnClickListener {
            showHelpDialog()
        }

        val btnAbout = findViewById<LinearLayout>(R.id.btnAbout)
        btnAbout.setOnClickListener {
            showAboutDialog()
        }

        val btnLogout = findViewById<LinearLayout>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
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

    // ✅ FUNGSI BARU: Update tampilan bahasa
    private fun updateLanguageDisplay() {
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val languageName = when (languageCode) {
            "id" -> "Indonesia"
            "ko" -> "한국어"
            else -> "English"
        }
        txtCurrentLanguage.text = languageName
    }

    private fun getLocalizedString(key: String): String {
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val suffix = when (languageCode) {
            "id" -> "_id"
            "ko" -> "_ko"
            else -> ""
        }

        val resourceId = resources.getIdentifier("${key}${suffix}", "string", packageName)
        return if (resourceId != 0) {
            getString(resourceId)
        } else {
            getString(resources.getIdentifier(key, "string", packageName))
        }
    }

    private fun loadUserData() {
        val username = sharedPreferences.getString("username", "User") ?: "User"
        txtProfileName.text = username
        txtProfileEmail.text = "$username@focusease.com"
    }

    private fun loadProfileImage() {
        val imageString = sharedPreferences.getString("profile_image", null)
        if (imageString != null) {
            try {
                val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imgProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        } else {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permission denied to access gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            if (imageUri != null) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(contentResolver, imageUri)
                        android.graphics.ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    }

                    val resizedBitmap = resizeBitmap(bitmap, 500)
                    imgProfile.setImageBitmap(resizedBitmap)

                    saveImageToPreferences(resizedBitmap)

                    Toast.makeText(this, "Profile photo updated successfully", Toast.LENGTH_SHORT).show()

                    setResult(Activity.RESULT_OK)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        val aspectRatio = width.toFloat() / height.toFloat()

        if (width > height) {
            if (width > maxSize) {
                width = maxSize
                height = (width / aspectRatio).toInt()
            }
        } else {
            if (height > maxSize) {
                height = maxSize
                width = (height * aspectRatio).toInt()
            }
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun saveImageToPreferences(bitmap: Bitmap) {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

            val editor = sharedPreferences.edit()
            editor.putString("profile_image", encodedImage)
            editor.apply()

            setResult(Activity.RESULT_OK)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditProfileDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getLocalizedString("edit_profile"))

        val input = android.widget.EditText(this)
        input.hint = getLocalizedString("enter_new_username")
        input.setText(sharedPreferences.getString("username", ""))

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        layout.addView(input)

        builder.setView(layout)

        builder.setPositiveButton(getLocalizedString("save")) { dialog, _ ->
            val newUsername = input.text.toString().trim()
            if (newUsername.isNotEmpty()) {
                val editor = sharedPreferences.edit()
                editor.putString("username", newUsername)
                editor.commit()

                userPrefs.saveUsername(newUsername)

                loadUserData()

                val resultIntent = Intent()
                resultIntent.putExtra("refresh_data", true)
                resultIntent.putExtra("new_username", newUsername)
                setResult(Activity.RESULT_OK, resultIntent)

                Toast.makeText(this, getLocalizedString("profile_updated"), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getLocalizedString("username_empty"), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(getLocalizedString("cancel")) { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showChangePasswordDialog() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getLocalizedString("change_password"))

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val oldPasswordInput = android.widget.EditText(this)
        oldPasswordInput.hint = getLocalizedString("old_password")
        oldPasswordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(oldPasswordInput)

        val newPasswordInput = android.widget.EditText(this)
        newPasswordInput.hint = getLocalizedString("new_password")
        newPasswordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(newPasswordInput)

        val confirmPasswordInput = android.widget.EditText(this)
        confirmPasswordInput.hint = getLocalizedString("confirm_password")
        confirmPasswordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(confirmPasswordInput)

        builder.setView(layout)

        builder.setPositiveButton(getLocalizedString("change")) { dialog, _ ->
            val oldPassword = oldPasswordInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            val savedPassword = sharedPreferences.getString("password", "")

            when {
                oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                    Toast.makeText(this, getLocalizedString("fill_all_fields"), Toast.LENGTH_SHORT).show()
                }
                oldPassword != savedPassword -> {
                    Toast.makeText(this, getLocalizedString("old_password_incorrect"), Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(this, getLocalizedString("passwords_not_match"), Toast.LENGTH_SHORT).show()
                }
                newPassword.length < 6 -> {
                    Toast.makeText(this, getLocalizedString("password_min_length"), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val editor = sharedPreferences.edit()
                    editor.putString("password", newPassword)
                    editor.commit()

                    setResult(Activity.RESULT_OK)

                    Toast.makeText(this, getLocalizedString("password_changed"), Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        builder.setNegativeButton(getLocalizedString("cancel")) { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf(
            getLocalizedString("light_theme"),
            getLocalizedString("dark_theme")
        )
        val currentTheme = sharedPreferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_NO)
        val checkedItem = when(currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 0
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getLocalizedString("select_theme"))
        builder.setSingleChoiceItems(themes, checkedItem) { dialog, which ->
            val themeMode = when(which) {
                0 -> AppCompatDelegate.MODE_NIGHT_NO
                1 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }

            val editor = sharedPreferences.edit()
            editor.putInt("theme", themeMode)
            editor.commit()

            AppCompatDelegate.setDefaultNightMode(themeMode)

            Toast.makeText(this, "${getLocalizedString("theme_changed")} ${themes[which]}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

            recreate()
        }
        builder.show()
    }

    // ✅ FUNGSI INI SUDAH DIPERBAIKI
    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Indonesia", "한국어 (Korea)")
        val languageCodes = arrayOf("en", "id", "ko")
        val currentLanguage = sharedPreferences.getString("language", "en")
        val checkedItem = languageCodes.indexOf(currentLanguage)

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getLocalizedString("select_language"))
        builder.setSingleChoiceItems(languages, checkedItem) { dialog, which ->
            val selectedLanguage = languageCodes[which]

            val editor = sharedPreferences.edit()
            editor.putString("language", selectedLanguage)
            editor.commit()

            val locale = Locale(selectedLanguage)
            Locale.setDefault(locale)
            val config = Configuration()
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            // ✅ UPDATE tampilan bahasa
            updateLanguageDisplay()

            Toast.makeText(this, "Language changed to ${languages[which]}", Toast.LENGTH_SHORT).show()

            dialog.dismiss()

            recreate()
        }
        builder.show()
    }

    private fun showHelpDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getLocalizedString("help_support_title"))

        val message = getLocalizedString("help_support_content")

        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showAboutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getLocalizedString("about_title"))

        val message = getLocalizedString("about_content")

        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getLocalizedString("logout"))
        builder.setMessage(getLocalizedString("logout_confirmation"))

        builder.setPositiveButton(getLocalizedString("yes")) { dialog, _ ->
            userPrefs.logout()

            val editor = sharedPreferences.edit()
            editor.clear()
            editor.commit()

            setResult(Activity.RESULT_OK)

            Toast.makeText(this, getLocalizedString("logged_out"), Toast.LENGTH_SHORT).show()

            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            dialog.dismiss()
        }

        builder.setNegativeButton(getLocalizedString("no")) { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // ✅ FUNGSI INI SUDAH DIPERBAIKI
    override fun onResume() {
        super.onResume()
        loadUserData()
        loadProfileImage()
        updateLanguageDisplay()  // ← TAMBAHAN BARU
    }
}