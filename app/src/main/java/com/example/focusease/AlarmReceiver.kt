package com.example.focusease

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        val alarmTime = intent.getStringExtra("ALARM_TIME") ?: "Alarm"
        val alarmName = intent.getStringExtra("ALARM_NAME") ?: "Alarm" // ✅ AMBIL NAMA ALARM

        when (intent.action) {
            "STOP_ALARM" -> {
                stopAlarm(context, alarmId)
            }
            "SNOOZE_ALARM" -> {
                snoozeAlarm(context, alarmId, alarmName) // ✅ KIRIM NAMA KE SNOOZE
            }
            else -> {
                showAlarmNotification(context, alarmId, alarmTime, alarmName) // ✅ KIRIM NAMA
            }
        }
    }

    private fun showAlarmNotification(context: Context, alarmId: Int, alarmTime: String, alarmName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Play alarm sound yang panjang
        playAlarmSound(context)

        // ✅ Vibrate yang kuat dan lama
        vibratePhone(context)

        // Stop action
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "STOP_ALARM"
            putExtra("ALARM_ID", alarmId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId * 10,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "SNOOZE_ALARM"
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_NAME", alarmName) // ✅ KIRIM NAMA ALARM
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId * 10 + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Open app action
        val openIntent = Intent(context, ClockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ Build notification dengan NAMA ALARM sebagai title
        val notification = NotificationCompat.Builder(context, "ALARM_CHANNEL")
            .setSmallIcon(R.drawable.ic_clock_active)
            .setContentTitle("⏰ $alarmName") // ✅ PAKAI NAMA ALARM
            .setContentText(alarmTime) // ✅ CUMA WAKTU AJA
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_close, "Stop", stopPendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
            .setFullScreenIntent(openPendingIntent, true)
            .build()

        notificationManager.notify(alarmId, notification)
    }

    private fun playAlarmSound(context: Context) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true // ✅ Loop terus sampai di-stop
            }

            ringtone.play()

            // ✅ Simpan ringtone instance untuk bisa di-stop nanti
            AlarmSoundManager.ringtone = ringtone
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ✅ Pattern: [delay, vibrate, sleep, vibrate, sleep, ...]
            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
            val vibrationEffect = VibrationEffect.createWaveform(pattern, 0) // 0 = repeat
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000), 0) // 0 = repeat
        }

        // ✅ Simpan vibrator untuk bisa di-stop nanti
        AlarmSoundManager.vibrator = vibrator
    }

    private fun stopAlarm(context: Context, alarmId: Int) {
        // Stop sound
        AlarmSoundManager.ringtone?.stop()
        AlarmSoundManager.ringtone = null

        // Stop vibration
        AlarmSoundManager.vibrator?.cancel()
        AlarmSoundManager.vibrator = null

        // Cancel notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)

        // ✅ MATIKAN SWITCH ALARM (broadcast ke ClockActivity)
        val turnOffIntent = Intent("com.example.focusease.TURN_OFF_ALARM")
        turnOffIntent.putExtra("ALARM_ID", alarmId)
        turnOffIntent.setPackage(context.packageName)
        context.sendBroadcast(turnOffIntent)
    }

    private fun snoozeAlarm(context: Context, alarmId: Int, alarmName: String) {
        stopAlarm(context, alarmId)

        // ✅ Snooze selama 5 menit
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TIME", "Snooze")
            putExtra("ALARM_NAME", alarmName) // ✅ KIRIM NAMA ALARM KE SNOOZE
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 menit
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            snoozeTime,
            snoozePendingIntent
        )

        android.widget.Toast.makeText(context, "Alarm snoozed for 5 minutes", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ✅ Object untuk manage sound dan vibration globally
object AlarmSoundManager {
    var ringtone: android.media.Ringtone? = null
    var vibrator: Vibrator? = null
}