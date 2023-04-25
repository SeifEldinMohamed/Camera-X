package com.seif.cameeraxapp

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import com.seif.cameeraxapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    // if using camera controller
    private lateinit var cameraController: LifecycleCameraController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }
    }

    private fun checkPermissions() {
        if (!hasPermission(baseContext)) {
            activityResultLauncher.launch(REQUIRED_PERMESSIONS)
        } else {
            startCamera()
        }
    }

    private fun takePhoto() {
        // create timestamp name and MediaStore entry
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        // create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        // set up image capture listenter, which is triggered after phot has been taken
        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this), // the executor in which this callback will be run
            object : ImageCapture.OnImageCapturedCallback(), ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e(TAG, "photo capture failed: ${exception.message}", exception)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "photo capture successfully: ${outputFileResults.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }


    private fun startCamera() {
        val previewView = binding.viewFinder
        cameraController = LifecycleCameraController(baseContext)
        cameraController.apply {
            bindToLifecycle(this@MainActivity)
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
        previewView.controller = cameraController
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMESSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted)
                Toast.makeText(this, "permission request denied", Toast.LENGTH_SHORT).show()
            else
                startCamera()
        }

    companion object {
        // private const val TAG = "MainActivity"
        private const val TAG = "CameraApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"
        private val REQUIRED_PERMESSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        fun hasPermission(context: Context) = REQUIRED_PERMESSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
// if using camera provider ( use imageCapture to have a reference throughout the file )
// private var imageCapture: ImageCapture? = null