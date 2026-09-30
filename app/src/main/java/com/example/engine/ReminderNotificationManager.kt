package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.TrackerItem

class ReminderNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "STASH Reminders & Warranties",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when warranties, subscriptions, documents, and policies are about to expire"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postTrackerNotification(tracker: TrackerItem) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TRACKER_ID", tracker.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            tracker.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val days = tracker.daysRemaining()
        val text = when {
            days == 0L -> "Expires today! Take action if needed."
            days == 1L -> "Expires tomorrow!"
            days > 1L -> "Expires in $days days (${tracker.trackerType.label})."
            else -> "Expired ${-days} days ago."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("STASH: ${tracker.title}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n${tracker.notes ?: ""}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(tracker.id.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "stash_reminders_channel"
    }
}
