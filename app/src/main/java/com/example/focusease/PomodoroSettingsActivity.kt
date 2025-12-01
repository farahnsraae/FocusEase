package com.example.focusease.ui.pomodoro  // UBAH INI

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.focusease.R

class PomodoroSettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnSave: CardView

    // Duration settings
    private lateinit var seekBarFocus: SeekBar
    private lateinit var tvFocusDuration: TextView
    private lateinit var seekBarShortBreak: SeekBar
    private lateinit var tvShortBreakDuration: TextView
    private lateinit var seekBarLongBreak: SeekBar
    private lateinit var tvLongBreakDuration: TextView
    private lateinit var seekBarPomodorosPerCycle: SeekBar
    private lateinit var tvPomodorosPerCycle: TextView

    // Auto-start settings
    private lateinit var switchAutoStartBreak: androidx.appcompat.widget.SwitchCompat
    private lateinit var switchAutoStartFocus: androidx.appcompat.widget.SwitchCompat

    // Notification settings
    private lateinit var switchSound: androidx.appcompat.widget.SwitchCompat
    private lateinit var switchVibration: androidx.appcompat.widget.SwitchCompat

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pomodoro_settings)

        prefs = getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE) // UBAH NAMA PREFS AGAR SAMA

        initViews()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)

        seekBarFocus = findViewById(R.id.seekBarFocus)
        tvFocusDuration = findViewById(R.id.tvFocusDuration)
        seekBarShortBreak = findViewById(R.id.seekBarShortBreak)
        tvShortBreakDuration = findViewById(R.id.tvShortBreakDuration)
        seekBarLongBreak = findViewById(R.id.seekBarLongBreak)
        tvLongBreakDuration = findViewById(R.id.tvLongBreakDuration)
        seekBarPomodorosPerCycle = findViewById(R.id.seekBarPomodorosPerCycle)
        tvPomodorosPerCycle = findViewById(R.id.tvPomodorosPerCycle)

        switchAutoStartBreak = findViewById(R.id.switchAutoStartBreak)
        switchAutoStartFocus = findViewById(R.id.switchAutoStartFocus)

        switchSound = findViewById(R.id.switchSound)
        switchVibration = findViewById(R.id.switchVibration)
    }

    private fun loadSettings() {
        // UBAH: gunakan getInt bukan getLong
        val focusDuration = prefs.getInt("focus_duration", 25)
        val shortBreak = prefs.getInt("short_break", 5)
        val longBreak = prefs.getInt("long_break", 15)
        val pomodorosPerCycle = prefs.getInt("pomodoros_per_cycle", 4)

        seekBarFocus.progress = focusDuration - 5 // Min 5
        tvFocusDuration.text = "$focusDuration Menit"

        seekBarShortBreak.progress = shortBreak - 1 // Min 1
        tvShortBreakDuration.text = "$shortBreak Menit"

        seekBarLongBreak.progress = longBreak - 5 // Min 5
        tvLongBreakDuration.text = "$longBreak Menit"

        seekBarPomodorosPerCycle.progress = pomodorosPerCycle - 2 // Min 2
        tvPomodorosPerCycle.text = "$pomodorosPerCycle Pomodoro"

        switchAutoStartBreak.isChecked = prefs.getBoolean("auto_start_break", true)
        switchAutoStartFocus.isChecked = prefs.getBoolean("auto_start_focus", false)

        switchSound.isChecked = prefs.getBoolean("notification_sound", true)
        switchVibration.isChecked = prefs.getBoolean("vibration", true)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveSettings()
        }

        seekBarFocus.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val duration = progress + 5
                tvFocusDuration.text = "$duration Menit"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarShortBreak.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val duration = progress + 1
                tvShortBreakDuration.text = "$duration Menit"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarLongBreak.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val duration = progress + 5
                tvLongBreakDuration.text = "$duration Menit"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarPomodorosPerCycle.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val count = progress + 2
                tvPomodorosPerCycle.text = "$count Pomodoro"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun saveSettings() {
        prefs.edit().apply {
            // UBAH: gunakan putInt bukan putLong
            putInt("focus_duration", seekBarFocus.progress + 5)
            putInt("short_break", seekBarShortBreak.progress + 1)
            putInt("long_break", seekBarLongBreak.progress + 5)
            putInt("pomodoros_per_cycle", seekBarPomodorosPerCycle.progress + 2)

            putBoolean("auto_start_break", switchAutoStartBreak.isChecked)
            putBoolean("auto_start_focus", switchAutoStartFocus.isChecked)

            putBoolean("notification_sound", switchSound.isChecked)
            putBoolean("vibration", switchVibration.isChecked)

            apply()
        }

        setResult(RESULT_OK)
        finish()
    }
}