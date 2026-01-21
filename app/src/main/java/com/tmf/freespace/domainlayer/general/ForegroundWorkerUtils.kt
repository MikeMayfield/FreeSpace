package com.tmf.freespace.domainlayer.general

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import com.tmf.freespace.MainActivity
import com.tmf.freespace.R

class ForegroundWorkerUtils {
//    private val tag = "ForegroundWorkerUtils"

    // Notification constants
    private val notificationID = 1001 // Unique ID for the notification
    private val subscriptionNotificationId = 1002 // Unique ID for the subscription notification
    private val notificationChannelID = "FileOptimizationChannel" // Unique channel ID

    /**
     * Create the ForegroundInfo for this Worker.
     * This involves creating a notification that will be shown to the user.
     */
    fun createForegroundInfo(worker: CoroutineWorker, context: Context): ForegroundInfo {
        val title = context.getString(R.string.file_optimization_notification_title) // Define in strings.xml
        val cancel = context.getString(R.string.file_optimization_notification_cancel) // Define in strings.xml
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(context).createCancelPendingIntent(worker.id)

        createNotificationChannel(context,
            R.string.file_optimization_channel_name,
            R.string.file_optimization_channel_description,
            NotificationManager.IMPORTANCE_LOW,
            false) // Ensure channel is created

        val notification = NotificationCompat.Builder(context, notificationChannelID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText("Making your storage bigger on the inside than the outside...")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true) // Makes the notification non-dismissible
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            // Optionally, set a category and priority
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // For Android 12 (API 31) and above, you might need to specify foregroundServiceType
        // in the notification if you also declare it in the manifest for the service.
        // However, WorkManager often handles this. If you encounter issues on API 31+,
        // you might need to look into this more.
        // The foreground service type for dataSync or similar might be appropriate if explicitly needed.
        val foregroundServiceType = when {
            // FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING is available from Android 15 (API 35)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            }
            // For older versions that still require a type (Android 12+), fallback to dataSync.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            // No type is needed for versions below Android 12.
            else -> 0
        }

        return ForegroundInfo(notificationID, notification, foregroundServiceType)
    }

    /**
     * Displays a notification to the user with the provided title and body content.
     * The notification includes an "OK" button to launch the app and a "Cancel" button to dismiss.
     */
    fun showStandardNotification(context: Context, title: String, content: String, buttonText: String = "OK") {
        createNotificationChannel(context,
            R.string.subscription_notification_channel_name,
            R.string.subscription_notification_channel_description,
            NotificationManager.IMPORTANCE_DEFAULT,
            true) // Ensure channel is created

        // Intent to launch the AppSummary screen
        val launchAppIntent = Intent(context, MainActivity::class.java).apply {
            // Use flags to bring an existing task to the foreground or start a new one
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            //NOTE: Pass extra data to navigate to a specific screen if needed. Eg. intent.putExtra("destination_route", "app_summary_route")
        }
        val okPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // The "Cancel" button will simply dismiss the notification, so no intent is needed,
        // as `setAutoCancel(true)` handles this.

        val notification = NotificationCompat.Builder(context, notificationChannelID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismisses the notification when the user taps on it or a button
            .addAction(0, buttonText, okPendingIntent) // "OK" button
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(subscriptionNotificationId, notification)
    }

    /**
     * Creates a notification channel for Android Oreo (API 26) and above.
     */
    private fun createNotificationChannel(context: Context, nameID: Int, descriptionID: Int, importance: Int, playSound: Boolean = false) {
        val name = context.getString(nameID) // Define in strings.xml
        val descriptionText = context.getString(descriptionID) // Define in strings.xml
        val importance = importance  // Use NotificationManager.IMPORTANCE_LOW to avoid sound/vibration unless critical
        val channel = NotificationChannel(notificationChannelID, name, importance).apply {
            description = descriptionText
            if (playSound) {
                val soundUri = ("android.resource://" + context.packageName + "/" + R.raw.notification).toUri()
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes) // Set the custom sound for the channel
            }
        }
        // Register the channel with the system
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}