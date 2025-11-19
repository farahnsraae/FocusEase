package com.example.focusease

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Restore semua alarm yang aktif
            // Anda bisa simpan alarm ke SharedPreferences dan restore di sini
            // Untuk sekarang, user harus buka app lagi untuk re-activate alarm
        }
    }
}