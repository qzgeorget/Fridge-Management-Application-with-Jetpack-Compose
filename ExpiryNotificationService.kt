package com.example.coursework

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.random.Random

class ExpiryNotificationService(private val context: Context, private val fridgeViewModel: FridgeViewModel) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun showNotification() {
        fridgeViewModel.getItemsExpiringNextDay { items ->
            if (items.isNotEmpty()) {
                for (item in items) {
                    val notification = NotificationCompat.Builder(context, "expiry_reminder")
                        .setSmallIcon(androidx.loader.R.drawable.notification_bg)
                        .setContentTitle("Expiry Notice")
                        .setContentText("Item ${item.foodItem.name} is going to expire today.")
                        .setPriority(NotificationManager.IMPORTANCE_HIGH)
                        .build()

                    notificationManager.notify(Random.nextInt(), notification)
                }
            }
        }
    }


}