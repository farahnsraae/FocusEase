package com.example.focusease.ui.pomodoro

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.focusease.R

class PomodoroService : Service() {

    companion object {
        const val CHANNEL_ID = "PomodoroServiceChannel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
    }

    private var timeLeftInMillis: Long = 0
    private var isRunning = false
    private var mediaPlayer: MediaPlayer? = null

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRunning && timeLeftInMillis > 0) {
                timeLeftInMillis -= 1000

                val intent = Intent("POMODORO_TICK").apply {
                    putExtra("timeLeft", timeLeftInMillis)
                }
                LocalBroadcastManager.getInstance(this@PomodoroService).sendBroadcast(intent)

                updateNotification("Timer Berjalan", formatTime(timeLeftInMillis))
                saveTimerState()
                handler.postDelayed(this, 1000)
            } else if (timeLeftInMillis <= 0 && isRunning) {
                onTimerFinish()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val newTime = intent.getLongExtra("timeLeft", 25 * 60 * 1000L)
                if (newTime > 0) {
                    timeLeftInMillis = newTime
                    startTimer()
                }
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> stopTimer()
        }
        return START_STICKY
    }

    private fun startTimer() {
        if (isRunning) return

        isRunning = true
        val notification = createNotification("Timer Berjalan", formatTime(timeLeftInMillis))

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }

    private fun pauseTimer() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        saveTimerState()

        val notification = createNotification("Timer Dijeda", formatTime(timeLeftInMillis))
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopTimer() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        timeLeftInMillis = 0
        saveTimerState()
        stopForeground(true)
        stopSelf()
    }

    private fun onTimerFinish() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)

        // Play alarm sound and vibrate
        playAlarmSound()
        vibratePhone()

        val prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        val currentSessions = prefs.getInt("sessions_completed", 0)
        prefs.edit().putInt("sessions_completed", currentSessions + 1).apply()

        showCompletionNotification()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("POMODORO_FINISH"))

        val focusDuration = prefs.getInt("focus_duration", 25)
        timeLeftInMillis = focusDuration * 60 * 1000L
        saveTimerState()

        stopForeground(true)
        stopSelf()
    }

    private fun saveTimerState() {
        val prefs = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putLong("time_left", timeLeftInMillis)
            putBoolean("is_running", isRunning)
            apply()
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val notificationIntent = Intent(this, PomodoroTimerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun showCompletionNotification() {
        val intent = Intent(this, PomodoroTimerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎉 Sesi Selesai!")
            .setContentText("Selamat! Kamu telah menyelesaikan 1 sesi Pomodoro")
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi untuk Pomodoro Timer"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        isRunning = false
        releaseMediaPlayer()
    }

    private fun playAlarmSound() {
        try {
            releaseMediaPlayer()

            // OPSI 1: Pakai file di res/raw (misal: alarm_sound.mp3)
            // Uncomment baris ini kalau udah taro file audio di res/raw/
            // mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)

            // OPSI 2: Pakai default notification sound
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@PomodoroService, alarmUri)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                }

                prepare()
                start()

                setOnCompletionListener {
                    releaseMediaPlayer()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibratePhone() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Pattern: wait 0ms, vibrate 500ms, wait 200ms, vibrate 500ms
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                // Fallback for older devices
                @Suppress("DEPRECATION")
                vibrator.vibrate(1000) // Vibrate for 1 second
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}