package dev.vishnuv.camera

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProvider
import dev.vishnuv.camera.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var mainViewModel: MainViewModel

    private var recordingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val binding = setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            lifecycleOwner = this@MainActivity
            mainViewModel = this@MainActivity.mainViewModel
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        takePhotoBtn.setOnClickListener { takePhoto() }

        recordVideoBtn.setOnClickListener { if (recordingState) stopRecording() else startRecording() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                this.showToast("Camera permission is required")
                finish()
            }
        }
    }

    private fun takePhoto() {

        Log.d(TAG, "takePhoto")

        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val msg = "Photo capture succeeded"
                    Log.d(TAG, msg)
                    this@MainActivity.showToast(msg)

//                    Share
                    shareToOtherApp(photoFile)
                }
            })
    }

    private fun startRecording() {
        Log.d(TAG, "video Record")

        val videoCapture = videoCapture ?: return
        recordingState = true

        mainViewModel.setRecordingStatus(recordingState)

        val videoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".mp4"
        )

        val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()

        videoCapture.startRecording(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    //Share
                    shareToOtherApp(videoFile)
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    Log.e(TAG, "Video record failed: $message", cause)
                }


            })
    }

    private fun stopRecording() {
        videoCapture ?: return
        videoCapture?.stopRecording()
        recordingState = false
        mainViewModel.setRecordingStatus(recordingState)
    }

    private fun shareToOtherApp(file: File) {
        try {

            val fileUri = FileProvider.getUriForFile(
                this@MainActivity,
                this@MainActivity.packageName + ".provider", file
            )

            startActivity(
                Intent.createChooser(
                    this@MainActivity.shareImage(fileUri),
                    "Image to Share"
                )
            )
        } catch (e: ActivityNotFoundException) {
            this@MainActivity.showToast("No App Available for sharing")
        } catch (e: Exception) {
            Log.e(TAG, "Unable to share", e)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            videoCapture = VideoCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "Camera"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }
}