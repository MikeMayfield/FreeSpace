package com.tmf.freespace.domainlayer.general

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import com.tmf.freespace.R

class ForegroundWorkerUtils {
    private val tag = "ForegroundWorkerUtils"

    // Notification constants
    private val notificationID = 1001 // Unique ID for the notification
    private val notificationChannelID = "FileOptimizationChannel" // Unique channel ID

    /**
     * Set the worker as a ForeGround service to allow it to run up to 6 hours in the background
     *
     * @return TRUE if processed successfully, FALSE if not
     */
    suspend fun runWorkerAsForegroundService(worker: CoroutineWorker, context: Context): Boolean {
        val foregroundInfo = createForegroundInfo(worker, context)
        try {
            worker.setForeground(foregroundInfo) // Use suspend version for CoroutineWorker
//            DLog.d(tag, "Foreground service started for worker ${worker.id}")
            return true
        } catch (e: IllegalStateException) {
            DLog.e(tag, "Error setting foreground service. Does the app have FOREGROUND_SERVICE permission? Or is it running on an older API without appropriate service type?", e)
            return false
        }
    }

    /**
     * Create the ForegroundInfo for this Worker.
     * This involves creating a notification that will be shown to the user.
     */
    @SuppressLint("InlinedApi")
    private fun createForegroundInfo(worker: CoroutineWorker, context: Context): ForegroundInfo {
        val title = context.getString(R.string.file_optimization_notification_title) // Define in strings.xml
        val cancel = context.getString(R.string.file_optimization_notification_cancel) // Define in strings.xml
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(context).createCancelPendingIntent(worker.id)

        createNotificationChannel(context) // Ensure channel is created

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
        val foregroundServiceType = FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING or FOREGROUND_SERVICE_TYPE_DATA_SYNC // Or your relevant type

        return ForegroundInfo(notificationID, notification, foregroundServiceType)
    }

    /**
     * Creates a notification channel for Android Oreo (API 26) and above.
     */
    private fun createNotificationChannel(context: Context) {
        val name = context.getString(R.string.file_optimization_channel_name) // Define in strings.xml
        val descriptionText = context.getString(R.string.file_optimization_channel_description) // Define in strings.xml
        val importance = NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound/vibration unless critical
        val channel = NotificationChannel(notificationChannelID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

}