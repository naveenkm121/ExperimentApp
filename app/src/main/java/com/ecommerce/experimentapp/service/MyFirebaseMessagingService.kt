package com.ecommerce.experimentapp.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ecommerce.experimentapp.R
import com.ecommerce.experimentapp.model.FCMTokenData
import com.ecommerce.experimentapp.network.CommonUtility
import com.ecommerce.experimentapp.network.RetrofitClient
import com.ecommerce.experimentapp.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "camera_service_channel"
        private const val NOTIFICATION_ID = 1234
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendFcmTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if the message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            // Assuming your custom data key to start the CameraService is "start_service"
            if (remoteMessage.data["start_service"] == "true") {
                // Show notification with action to start CameraService
               // showNotification()
               // startCameraService()
                showNotificationOpenMainActivity()
            }
        }

        // Check if the message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification() {
        // Create an Intent that will start the CameraService when the notification is clicked
        val intent = Intent(this, CameraService::class.java)

        // Create a PendingIntent that will start the service when the notification is clicked
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
            .setContentTitle("Camera Service")
            .setContentText("Tap to start Camera Service")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotificationOpenMainActivity() {
        // Create an Intent that will start the MainActivity when the notification is clicked
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the stack
            putExtra("start_service", "true") // Add this extra
        }

        // Create a PendingIntent that will start the activity when the notification is clicked
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
            .setContentTitle("Camera Service")
            .setContentText("Tap to open MainActivity")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotificationReciver() {
        Log.d("NotificationClick", "Notification clicked. Starting CameraService...11")
        // Create an Intent that will be handled by the NotificationClickReceiver
        val intent = Intent(this, NotificationClickReceiver::class.java)

        // Create a PendingIntent that will trigger the BroadcastReceiver when the notification is clicked
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Camera Service")
            .setContentText("Tap to start Camera Service")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        Log.d("NotificationClick", "Notification clicked. Starting CameraService...22")
        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Camera Service"
            val descriptionText = "Channel for Camera Service notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendFcmTokenToServer(token: String) {
        val fcmTokenData = FCMTokenData().apply {
            this.token = token
            this.userId = CommonUtility.getRandomInt(0, 100)
            this.deviceModel = Build.MODEL
            this.deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            this.osVersion = Build.VERSION.RELEASE
        }

        Log.d(TAG, "FCM Token sent sendFcmTokenToServer")
        val call = RetrofitClient.instance.sendFcmToken(fcmTokenData)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM Token sent successfully")
                    Toast.makeText(baseContext, "FCM Token sent successfully", Toast.LENGTH_LONG).show()
                } else {
                    Log.w(TAG, "Failed to send FCM Token: ${response.errorBody().toString()}")
                    Toast.makeText(baseContext, "Failed to send FCM Token: ${response.errorBody().toString()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.w(TAG, "Failed to send FCM Token", t)
            }
        })
    }

    private fun startCameraService() {
        val intent = Intent(this, CameraService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
