package com.example.focusease.ui.pomodoro

import android.content.*
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusease.R
import com.example.focusease.databinding.ActivityPomodoroTimerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PomodoroTimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPomodoroTimerBinding
    private var timeLeftInMillis: Long = 25 * 60 * 1000L
    private var isTimerRunning = false
    private var sessionsCompleted = 0

    private val handler = Handler(Looper.getMainLooper())
    private var isDialogShowing = false

    private lateinit var taskAdapter: TaskAdapter
    private val tasks = mutableListOf<PomodoroTask>()
    private var selectedTask: PomodoroTask? = null

    // MediaPlayer untuk alarm
    private var alarmPlayer: MediaPlayer? = null

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "POMODORO_TICK" -> {
                    val newTime = intent.getLongExtra("timeLeft", timeLeftInMillis)
                    if (newTime != timeLeftInMillis) {
                        timeLeftInMillis = newTime
                        updateTimerText()
                        updateProgress()
                    }
                }
                "POMODORO_FINISH" -> {
                    onTimerFinish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPomodoroTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        loadAllData()
        setupRecyclerView()
        setupListeners()
        updateUI()
        registerTimerReceiver()

        // Animasi masuk smooth
        binding.cardStatus.alpha = 0f
        binding.cardStatus.translationY = -50f
        binding.cardStatus.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .start()
    }

    private fun registerTimerReceiver() {
        val filter = IntentFilter().apply {
            addAction("POMODORO_TICK")
            addAction("POMODORO_FINISH")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(timerReceiver, filter)
    }

    private fun loadAllData() {
        val prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)

        val focusDuration = prefs.getInt("focus_duration", 25)
        timeLeftInMillis = prefs.getLong("time_left", focusDuration * 60 * 1000L)
        isTimerRunning = prefs.getBoolean("is_running", false)
        sessionsCompleted = prefs.getInt("sessions_completed", 0)

        val tasksJson = prefs.getString("tasks_list", "[]") ?: "[]"
        val type = object : TypeToken<List<PomodoroTask>>() {}.type
        try {
            val loadedTasks: List<PomodoroTask> = Gson().fromJson(tasksJson, type)
            tasks.clear()
            tasks.addAll(loadedTasks)
        } catch (e: Exception) {
            tasks.clear()
        }

        val selectedTaskId = prefs.getString("selected_task_id", null)
        selectedTask = tasks.find { it.id == selectedTaskId }
    }

    private fun saveAllData() {
        val prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putLong("time_left", timeLeftInMillis)
            putBoolean("is_running", isTimerRunning)
            putInt("sessions_completed", sessionsCompleted)
            putString("tasks_list", Gson().toJson(tasks))
            putString("selected_task_id", selectedTask?.id)
            apply()
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            tasks = tasks,
            onTaskClick = { task ->
                selectedTask = task
                animateTaskSelection()
                updateStatusMessage()
                saveAllData()
            },
            onTaskDelete = { task ->
                taskAdapter.removeTask(task)
                if (selectedTask?.id == task.id) {
                    selectedTask = null
                    updateStatusMessage()
                }
                saveAllData()
                Snackbar.make(binding.root, "✓ Task dihapus", Snackbar.LENGTH_SHORT).show()
            },
            onTaskCompleteToggle = { task ->
                taskAdapter.updateTask(task)
                saveAllData()
            }
        )

        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@PomodoroTimerActivity)
            adapter = taskAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, PomodoroSettingsActivity::class.java))
        }

        binding.btnPlayPause.setOnClickListener {
            animateButton(it)
            if (isTimerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        binding.btnStop.setOnClickListener {
            animateButton(it)
            resetTimer()
        }

        binding.btnAddTask.setOnClickListener {
            animateButton(it)
            showAddTaskDialog()
        }
    }

    private fun startTimer() {
        if (timeLeftInMillis <= 0) {
            val prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
            timeLeftInMillis = prefs.getInt("focus_duration", 25) * 60 * 1000L
        }

        isTimerRunning = true

        val intent = Intent(this, PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_START
            putExtra("timeLeft", timeLeftInMillis)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            isTimerRunning = false
            Snackbar.make(binding.root, "❌ Gagal memulai timer", Snackbar.LENGTH_SHORT).show()
            return
        }

        binding.btnPlayPause.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_pause)
        )

        updateStatusCard("Focus Time", "Tetap fokus! Kamu bisa!", "#7E57C2")
        saveAllData()
    }

    private fun pauseTimer() {
        isTimerRunning = false

        val intent = Intent(this, PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_PAUSE
        }
        startService(intent)

        binding.btnPlayPause.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_play)
        )

        updateStatusCard("Timer Dijeda", "Istirahat sebentar", "#FF9800")
        saveAllData()
    }

    private fun resetTimer() {
        isTimerRunning = false

        // Stop alarm jika sedang berbunyi
        stopAlarmSound()

        val intent = Intent(this, PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_STOP
        }
        startService(intent)

        val prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        timeLeftInMillis = prefs.getInt("focus_duration", 25) * 60 * 1000L

        binding.btnPlayPause.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_play)
        )

        updateStatusCard("Timer Direset", "Siap untuk sesi baru", "#4CAF50")
        updateUI()
        saveAllData()

        Snackbar.make(binding.root, "↻ Timer direset", Snackbar.LENGTH_SHORT).show()
    }

    private fun onTimerFinish() {
        isTimerRunning = false

        // Mainkan alarm sound
        playAlarmSound()

        // Load fresh sessions count
        val prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        sessionsCompleted = prefs.getInt("sessions_completed", 0)

        selectedTask?.let { task ->
            task.incrementCompleted()
            taskAdapter.updateTask(task)
        }

        binding.btnPlayPause.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_play)
        )

        updateStatusCard("Selesai! 🎉", "Tap STOP untuk matikan alarm", "#4CAF50")

        // Animasi celebration
        binding.cardStatus.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(200)
            .withEndAction {
                binding.cardStatus.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()

        // Show dialog to stop alarm
        handler.postDelayed({
            showStopAlarmDialog()
        }, 500)

        updateUI()
        saveAllData()
    }

    private fun playAlarmSound() {
        try {
            // Stop alarm sebelumnya jika ada
            stopAlarmSound()

            // Gunakan alarm ringtone default sistem
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            alarmPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                this.isLooping = true // Loop sampai di-stop
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showStopAlarmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🎉 Pomodoro Selesai!")
            .setMessage("Selamat! Kamu telah menyelesaikan 1 sesi.\n\nMau stop alarm?")
            .setPositiveButton("Stop Alarm") { dialog, _ ->
                stopAlarmSound()
                dialog.dismiss()
            }
            .setNegativeButton("Nanti Aja", null)
            .setCancelable(false)
            .show()
    }

    private fun stopAlarmSound() {
        try {
            alarmPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            alarmPlayer = null

            // Juga stop alarm dari service
            val intent = Intent(this, PomodoroService::class.java).apply {
                action = "ACTION_STOP_ALARM"
            }
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAddTaskDialog() {
        isDialogShowing = true

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)

        val etTaskName = dialogView.findViewById<TextInputEditText>(R.id.etTaskName)
        val etEstimatedPomodoros = dialogView.findViewById<TextInputEditText>(R.id.etEstimatedPomodoros)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Tambah Task Baru")
            .setView(dialogView)
            .setPositiveButton("Tambah") { dialogInterface, _ ->
                val taskName = etTaskName.text.toString().trim()
                val pomodorosText = etEstimatedPomodoros.text.toString()
                val estimatedPomodoros = pomodorosText.toIntOrNull() ?: 1

                if (taskName.isNotEmpty()) {
                    val task = PomodoroTask(
                        title = taskName,
                        estimatedPomodoros = estimatedPomodoros
                    )
                    taskAdapter.addTask(task)
                    saveAllData()

                    handler.postDelayed({
                        Snackbar.make(binding.root, "✓ Task ditambahkan", Snackbar.LENGTH_SHORT).show()
                    }, 100)
                } else {
                    handler.postDelayed({
                        Snackbar.make(binding.root, "⚠ Nama task tidak boleh kosong", Snackbar.LENGTH_SHORT).show()
                    }, 100)
                }

                isDialogShowing = false
                dialogInterface.dismiss()
            }
            .setNegativeButton("Batal") { dialogInterface, _ ->
                isDialogShowing = false
                dialogInterface.dismiss()
            }
            .setCancelable(true)
            .setOnDismissListener {
                isDialogShowing = false
            }
            .create()

        dialog.show()
    }

    private fun updateUI() {
        updateTimerText()
        updateSessionCount()
        updateStatistics()
        updateProgress()
        updatePomodoroIndicators()
        updateStatusMessage()
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        binding.tvTimerDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateSessionCount() {
        binding.tvSessionCount.text = "Sesi $sessionsCompleted/4"
    }

    private fun updateStatistics() {
        binding.tvTodayPomodoros.text = sessionsCompleted.toString()

        val focusMinutes = sessionsCompleted * 25
        binding.tvTodayFocusTime.text = "${focusMinutes}m"

        // Streak calculation (simplified)
        val prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        val streak = prefs.getInt("current_streak", 0)
        binding.tvCurrentStreak.text = "$streak hari"
    }

    private fun updateProgress() {
        val prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        val totalTime = prefs.getInt("focus_duration", 25) * 60 * 1000L
        val progress = if (totalTime > 0) {
            ((totalTime - timeLeftInMillis).toFloat() / totalTime.toFloat() * 100).toInt()
        } else {
            0
        }
        binding.progressCircular.progress = progress.coerceIn(0, 100)
    }

    private fun updatePomodoroIndicators() {
        val dots = listOf(
            binding.dotPomodoro1,
            binding.dotPomodoro2,
            binding.dotPomodoro3,
            binding.dotPomodoro4
        )

        dots.forEachIndexed { index, dot ->
            if (index < sessionsCompleted) {
                dot.setBackgroundResource(R.drawable.dot_filled)
                dot.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
            } else {
                dot.setBackgroundResource(R.drawable.dot_empty)
                dot.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }
        }
    }

    private fun updateStatusMessage() {
        when {
            selectedTask != null -> {
                binding.tvStatusMessage.text = "Task: ${selectedTask?.title}"
            }
            tasks.isEmpty() -> {
                binding.tvStatusMessage.text = "Tambah task untuk memulai"
            }
            else -> {
                binding.tvStatusMessage.text = "Pilih task dari daftar"
            }
        }
    }

    private fun updateStatusCard(title: String, message: String, colorHex: String) {
        binding.tvTimerLabel.text = title
        binding.tvStatusMessage.text = message

        try {
            binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            // Fallback to purple color
            binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#7E57C2"))
        }

        // Animasi perubahan
        binding.cardStatus.animate()
            .alpha(0.7f)
            .setDuration(150)
            .withEndAction {
                binding.cardStatus.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun animateButton(view: android.view.View) {
        view.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateTaskSelection() {
        binding.tvStatusMessage.animate()
            .alpha(0.3f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                updateStatusMessage()
                binding.tvStatusMessage.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    override fun onResume() {
        super.onResume()

        // LOAD FRESH DATA DARI SERVICE
        loadAllData()

        // FORCE RESET card background color
        try {
            binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#7E57C2"))
            binding.tvTimerLabel.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            binding.tvStatusMessage.setTextColor(android.graphics.Color.parseColor("#E8DEF8"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Update UI based on current timer state
        if (isTimerRunning) {
            binding.btnPlayPause.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.ic_pause)
            )
            binding.tvTimerLabel.text = "Focus Time"
            binding.tvStatusMessage.text = "Timer masih berjalan..."
        } else {
            binding.btnPlayPause.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.ic_play)
            )
            binding.tvTimerLabel.text = "Focus Time"
            if (selectedTask != null) {
                binding.tvStatusMessage.text = "Fokus pada: ${selectedTask?.title}"
            } else {
                binding.tvStatusMessage.text = "Fokus pada task yang dipilih"
            }
        }

        updateUI()
    }

    override fun onPause() {
        super.onPause()
        saveAllData()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop dan release alarm player
        stopAlarmSound()

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(timerReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver already unregistered
        }
        saveAllData()
    }
}