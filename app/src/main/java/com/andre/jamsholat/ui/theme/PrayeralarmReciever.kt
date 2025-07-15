package com.andre.jamsholat

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class PrayerAlarmReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "prayer_time_channel" // Must match the channel ID in MainActivity

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.andre.jamsholat.PRAYER_ALARM") {
            val prayerName = intent.getStringExtra("prayer_name") ?: "Waktu Sholat"
            Log.d("PrayerAlarmReceiver", "Alarm diterima untuk: $prayerName")
            showNotification(context, prayerName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNotification(context: Context, prayerName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_adhan) // Create this icon in res/drawable
            .setContentTitle("Waktu Sholat!")
            .setContentText("Sudah masuk waktu sholat $prayerName. Mari tunaikan sholat.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismiss notification when tapped

        // Show the notification
        // Use a unique ID for each notification, or just a generic one for prayer times
        notificationManager.notify(prayerName.hashCode(), builder.build()) // Use hashcode of prayerName as ID for uniqueness
        Log.d("PrayerAlarmReceiver", "Notifikasi untuk $prayerName ditampilkan.")
    }
}
