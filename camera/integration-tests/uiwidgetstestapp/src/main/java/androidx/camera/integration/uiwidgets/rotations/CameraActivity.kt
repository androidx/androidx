/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.uiwidgets.rotations

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.uiwidgets.databinding.ActivityRotationsMainBinding
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

open class CameraActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityRotationsMainBinding
    private lateinit var mCamera: Camera
    protected lateinit var mImageAnalysis: ImageAnalysis
    protected lateinit var mImageCapture: ImageCapture
    private lateinit var mAnalysisExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityRotationsMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mAnalysisExecutor = Executors.newSingleThreadExecutor()
        if (shouldRequestPermissionsAtRuntime() && !hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        } else {
            setUpCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mAnalysisExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (hasPermissions()) {
                setUpCamera()
            } else {
                Log.d(TAG, "Camera permission is required")
                finish()
            }
        }
    }

    private fun shouldRequestPermissionsAtRuntime(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun hasPermissions(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun setUpCamera() {
        val cameraProcessFuture = ProcessCameraProvider.getInstance(this)
        cameraProcessFuture.addListener(
            Runnable {
                val cameraProvider = cameraProcessFuture.get()
                if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    setUpCamera(cameraProvider)
                } else {
                    Log.d(TAG, "Skip camera setup since activity is closed")
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun setUpCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
            .apply {
                setSurfaceProvider(mBinding.previewView.getSurfaceProvider())
            }
        mImageAnalysis = ImageAnalysis.Builder()
            .build()
            .apply {
                setAnalyzer(mAnalysisExecutor, createAnalyzer())
            }
        mImageCapture = ImageCapture.Builder()
            .build()
            .also {
                it.setCallback()
            }
        mCamera = cameraProvider.bindToLifecycle(
            this,
            getCameraSelector(),
            preview,
            mImageAnalysis,
            mImageCapture
        )
    }

    private fun getCameraSelector(): CameraSelector {
        val lensFacing = intent.getIntExtra(KEY_LENS_FACING, CameraSelector.LENS_FACING_BACK)
        return CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    private fun createAnalyzer(): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            mAnalysisImageRotation = imageProxy.imageInfo.rotationDegrees
            mAnalysisRunning.release()
            Log.d(TAG, "Analyzed image rotation = $mAnalysisImageRotation, $mAnalysisRunning")
            imageProxy.close()
        }
    }

    private fun ImageCapture.setCallback() {
        mBinding.previewView.setOnClickListener {
            val imageCaptureMode =
                intent.getIntExtra(KEY_IMAGE_CAPTURE_MODE, IMAGE_CAPTURE_MODE_IN_MEMORY)
            when (imageCaptureMode) {
                IMAGE_CAPTURE_MODE_IN_MEMORY -> setInMemoryCallback()
                IMAGE_CAPTURE_MODE_FILE -> setFileCallback()
                IMAGE_CAPTURE_MODE_OUTPUT_STREAM -> setOutputStreamCallback()
                IMAGE_CAPTURE_MODE_MEDIA_STORE -> setMediaStoreCallback()
            }
        }
    }

    private fun ImageCapture.setInMemoryCallback() {
        takePicture(
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    mCaptureResult = ImageCaptureResult.InMemory(image)
                    mCaptureDone.release()
                    image.close()
                    Log.d(TAG, "InMemory image capture successful")
                }

                override fun onError(exception: ImageCaptureException) {
                    mCaptureDone.release()
                    Log.e(TAG, "InMemory image capture failed", exception)
                }
            }
        )
    }

    private fun ImageCapture.setFileCallback() {
        val imageFile = File("${cacheDir.absolutePath}/${System.currentTimeMillis()}.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
        takePicture(
            outputFileOptions,
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    mCaptureResult = ImageCaptureResult.FileOrOutputStream(imageFile)
                    mCaptureDone.release()
                    Log.d(TAG, "File image capture successful")
                }

                override fun onError(exception: ImageCaptureException) {
                    mCaptureDone.release()
                    Log.e(TAG, "File image capture failed", exception)
                }
            }
        )
    }

    private fun ImageCapture.setOutputStreamCallback() {
        val imageFile = File("${cacheDir.absolutePath}/${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(imageFile)
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()
        takePicture(
            outputFileOptions,
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    mCaptureResult = ImageCaptureResult.FileOrOutputStream(imageFile)
                    mCaptureDone.release()
                    Log.d(TAG, "OutputStream image capture successful")
                }

                override fun onError(exception: ImageCaptureException) {
                    mCaptureDone.release()
                    Log.e(TAG, "OutputStream image capture failed", exception)
                }
            }
        )
    }

    private fun ImageCapture.setMediaStoreCallback() {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        val outputFileOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()
        takePicture(
            outputFileOptions,
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    mCaptureResult =
                        ImageCaptureResult.MediaStore(contentResolver, outputFileResults.savedUri!!)
                    mCaptureDone.release()
                    Log.d(TAG, "MediaStore image capture successful")
                }

                override fun onError(exception: ImageCaptureException) {
                    mCaptureDone.release()
                    Log.e(TAG, "MediaStore image capture failed", exception)
                }
            }
        )
    }

    protected fun isImageAnalysisInitialized(): Boolean {
        return ::mImageAnalysis.isInitialized
    }

    protected fun isImageCaptureInitialized(): Boolean {
        return ::mImageCapture.isInitialized
    }

    // region For testing
    @VisibleForTesting
    val mAnalysisRunning = Semaphore(0)

    @VisibleForTesting
    var mAnalysisImageRotation = -1

    @VisibleForTesting
    val mCaptureDone = Semaphore(0)

    @VisibleForTesting
    var mCaptureResult: ImageCaptureResult? = null

    @VisibleForTesting
    fun getSensorRotationRelativeToAnalysisTargetRotation(): Int {
        val targetRotation = mImageAnalysis.targetRotation
        return mCamera.cameraInfo.getSensorRotationDegrees(targetRotation)
    }

    @VisibleForTesting
    fun getSensorRotationRelativeToCaptureTargetRotation(): Int {
        val targetRotation = mImageCapture.targetRotation
        return mCamera.cameraInfo.getSensorRotationDegrees(targetRotation)
    }

    @Suppress("RestrictedApi")
    @VisibleForTesting
    fun getCaptureResolution(): Size {
        val resolution = mImageCapture.attachedSurfaceResolution
            ?: throw IllegalStateException("ImageCapture surface resolution is null")

        val rotation = getSensorRotationRelativeToCaptureTargetRotation()
        return if (rotation == 90 || rotation == 270) {
            Size(resolution.height, resolution.width)
        } else {
            resolution
        }
    }
    // endregion

    companion object {
        const val KEY_LENS_FACING = "lens-facing"
        const val KEY_IMAGE_CAPTURE_MODE = "image-capture-mode"
        const val IMAGE_CAPTURE_MODE_IN_MEMORY = 0
        const val IMAGE_CAPTURE_MODE_FILE = 1
        const val IMAGE_CAPTURE_MODE_OUTPUT_STREAM = 2
        const val IMAGE_CAPTURE_MODE_MEDIA_STORE = 3

        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 20
        val PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
