package com.example.coursework

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi

class NotificationApplication : Application() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        val notificationChannel = NotificationChannel(
            "expiry_reminder",
            "Expiry Reminder",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.description = "For when a food expires in the next day"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
}