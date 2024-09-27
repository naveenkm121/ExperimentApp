package com.ecommerce.experimentapp.ui

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.ecommerce.experimentapp.databinding.ActivityCameraBinding
import com.ecommerce.experimentapp.service.CameraService

class ContactActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        openCamera()

    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data")
            // Handle the imageBitmap (e.g., save it, display it, etc.)
            Log.d("CameraActivity", "Image captured successfully")
            Log.d("CameraActivity", "Image Bitmap $imageBitmap")
        }
        stopService(Intent(this, CameraService::class.java))
        finish()  // Close the activity after the picture is taken
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }

}