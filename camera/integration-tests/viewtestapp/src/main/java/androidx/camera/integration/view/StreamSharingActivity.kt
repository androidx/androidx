/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.integration.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.OrientationEventListener
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Logger
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.FileUtil.canDeviceWriteToMediaStore
import androidx.camera.testing.impl.FileUtil.generateVideoFileOutputOptions
import androidx.camera.testing.impl.FileUtil.generateVideoMediaStoreOptions
import androidx.camera.testing.impl.FileUtil.writeTextToExternalFile
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.ImplementationMode
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer

private const val TAG = "StreamSharingActivity"
private const val PREFIX_INFORMATION = "test_information"
private const val PREFIX_VIDEO = "video"
private const val KEY_ORIENTATION = "device_orientation"
private const val KEY_STREAM_SHARING_STATE = "is_stream_sharing_enabled"

// Possible values for this intent key (case-insensitive): "portrait", "landscape".
private const val INTENT_SCREEN_ORIENTATION = "orientation"
private const val SCREEN_ORIENTATION_PORTRAIT = "portrait"
private const val SCREEN_ORIENTATION_LANDSCAPE = "landscape"

// Possible values for this intent key (case-insensitive): "back", "front".
private const val INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction"
private const val CAMERA_DIRECTION_BACK = "back"
private const val CAMERA_DIRECTION_FRONT = "front"

// Possible values for this intent key (case-insensitive): "compatible", "performance".
private const val INTENT_PREVIEW_VIEW_MODE = "preview_view_mode"
private const val PREVIEW_VIEW_COMPATIBLE_MODE = "compatible"
private const val PREVIEW_VIEW_PERFORMANCE_MODE = "performance"

class StreamSharingActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var exportButton: Button
    private lateinit var recordButton: Button
    private lateinit var useCases: Array<UseCase>
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var camera: Camera? = null
    private var previewViewMode: ImplementationMode = ImplementationMode.PERFORMANCE
    private var previewViewScaleType = PreviewView.ScaleType.FILL_CENTER;
    private var activeRecording: Recording? = null
    private var isUseCasesBound: Boolean = false
    private var deviceOrientation: Int = -1
    private val orientationEventListener by lazy {
        object : OrientationEventListener(applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                deviceOrientation = orientation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_sharing)

        // Apply settings from intent.
        val bundle = intent.extras
        if (bundle != null) {
            parseScreenOrientationAndSetValueIfNeed(bundle)
            parseCameraSelector(bundle)
            parsePreviewViewMode(bundle)
        }

        // Initial view objects.
        previewView = findViewById(R.id.preview_view)
        previewView.scaleType = previewViewScaleType
        previewView.implementationMode = previewViewMode
        exportButton = findViewById(R.id.export_button)
        exportButton.setOnClickListener {
            exportTestInformation()
        }
        recordButton = findViewById(R.id.record_button)
        recordButton.setOnClickListener {
            if (activeRecording == null) startRecording() else stopRecording()
        }

        startCamera()
    }

    override fun onResume() {
        super.onResume()
        orientationEventListener.enable()
    }

    override fun onPause() {
        super.onPause()
        orientationEventListener.disable()
    }

    private fun parseScreenOrientationAndSetValueIfNeed(bundle: Bundle) {
        val orientationString = bundle.getString(INTENT_SCREEN_ORIENTATION)
        if (SCREEN_ORIENTATION_PORTRAIT.equals(orientationString, true)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else if (SCREEN_ORIENTATION_LANDSCAPE.equals(orientationString, true)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private fun parseCameraSelector(bundle: Bundle) {
        val cameraDirection = bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION)
        if (CAMERA_DIRECTION_BACK.equals(cameraDirection, true)) {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        } else if (CAMERA_DIRECTION_FRONT.equals(cameraDirection, true)) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    private fun parsePreviewViewMode(bundle: Bundle) {
        val mode = bundle.getString(INTENT_PREVIEW_VIEW_MODE)
        if (PREVIEW_VIEW_COMPATIBLE_MODE.equals(mode, true)) {
            previewViewMode = ImplementationMode.COMPATIBLE
        } else if (PREVIEW_VIEW_PERFORMANCE_MODE.equals(mode, true)) {
            previewViewMode = ImplementationMode.PERFORMANCE
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
        cameraProviderFuture.addListener({
            bindUseCases(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(applicationContext))
    }

    private fun bindUseCases(cameraProvider: ProcessCameraProvider) {
        enableRecording(false)
        isUseCasesBound = false
        cameraProvider.unbindAll()
        useCases = arrayOf(
            createPreview(),
            createImageCapture(),
            createImageAnalysis(),
            createVideoCapture()
        )
        isUseCasesBound = try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, *useCases)
            enableRecording(true)
            true
        } catch (exception: Exception) {
            Logger.e(TAG, "Failed to bind use cases.", exception)
            false
        }
    }

    private fun createPreview(): Preview {
        return Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
    }

    private fun createImageCapture(): ImageCapture {
        return ImageCapture.Builder().build()
    }

    private fun createImageAnalysis(): ImageAnalysis {
        return ImageAnalysis.Builder().build()
    }

    private fun createVideoCapture(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder().build()
        return VideoCapture.Builder(recorder).setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY).build()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getVideoCapture(): VideoCapture<Recorder>? {
        return findUseCase(VideoCapture::class.java) as VideoCapture<Recorder>?
    }

    private fun <T : UseCase?> findUseCase(useCaseSubclass: Class<T>): T? {
        for (useCase in useCases) {
            if (useCaseSubclass.isInstance(useCase)) {
                return useCaseSubclass.cast(useCase)
            }
        }
        return null
    }

    @SuppressLint("RestrictedApi")
    private fun isStreamSharingEnabled(): Boolean {
        val isCombinationSupported =
            camera != null && camera!!.isUseCasesCombinationSupportedByFramework(*useCases)
        return !isCombinationSupported && isUseCasesBound
    }

    private fun enableRecording(enabled: Boolean) {
        recordButton.isEnabled = enabled
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        recordButton.text = getString(R.string.btn_video_stop_recording)
        activeRecording = getVideoCapture()!!.let {
            prepareRecording(applicationContext, it.output).withAudioEnabled().start(
                CameraXExecutors.directExecutor(),
                generateVideoRecordEventListener()
            )
        }
    }

    private fun stopRecording() {
        recordButton.text = getString(R.string.btn_video_record)
        activeRecording!!.stop()
        activeRecording = null
    }

    private fun prepareRecording(context: Context, recorder: Recorder): PendingRecording {
        val fileName = generateFileName(PREFIX_VIDEO)

        return if (canDeviceWriteToMediaStore()) {
            recorder.prepareRecording(
                context,
                generateVideoMediaStoreOptions(context.contentResolver, fileName)
            )
        } else {
            recorder.prepareRecording(context, generateVideoFileOutputOptions(fileName))
        }
    }

    private fun exportTestInformation() {
        val fileName = generateFileName(PREFIX_INFORMATION)
        val information = "$KEY_ORIENTATION:$deviceOrientation" +
            "\n" + "$KEY_STREAM_SHARING_STATE:${isStreamSharingEnabled()}"

        writeTextToExternalFile(information, fileName)
    }

    private fun generateFileName(prefix: String? = null): String {
        val timeMillis = System.currentTimeMillis()
        return if (prefix != null) "${prefix}_$timeMillis" else "$timeMillis"
    }

    private fun generateVideoRecordEventListener(): Consumer<VideoRecordEvent> {
        return Consumer<VideoRecordEvent> { event ->
            if (event is VideoRecordEvent.Finalize) {
                val uri = event.outputResults.outputUri
                when (event.error) {
                    VideoRecordEvent.Finalize.ERROR_NONE,
                    VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED,
                    VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED,
                    VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE,
                    VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> Logger.d(
                        TAG,
                        "Video saved to: $uri"
                    )

                    else -> Logger.e(
                        TAG,
                        "Failed to save video: uri $uri with code (${event.error})"
                    )
                }
            }
        }
    }
}
