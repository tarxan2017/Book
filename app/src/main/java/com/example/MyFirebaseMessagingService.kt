package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Calendar

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Живая Страница"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        
        // Проверяем, не наступили ли "Тихие часы" (с 23:00 до 08:00)
        val isDndTime = isDoNotDisturbTime()
        
        showNotification(title, body, isSilent = isDndTime)
    }

    override fun onNewToken(token: String) {
        // Этот метод вызывается, когда Firebase генерирует новый токен для устройства.
        // Позже мы добавим сюда сохранение токена в базу данных (Firestore).
        android.util.Log.d("FCM", "Новый токен устройства: $token")
    }

    private fun isDoNotDisturbTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        // С 23:00 (23) до 08:00 (8) утра
        return hour >= 23 || hour < 8
    }

    private fun showNotification(title: String, body: String, isSilent: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "living_page_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Создаем канал. Если "тихий час", то важность LOW (без звука)
            val importance = if (isSilent) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, "Уведомления", importance)
            notificationManager.createNotificationChannel(channel)
            
            if (isSilent) {
                notificationBuilder.setSilent(true)
            }
        } else {
            if (isSilent) {
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_LOW)
            } else {
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            }
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
