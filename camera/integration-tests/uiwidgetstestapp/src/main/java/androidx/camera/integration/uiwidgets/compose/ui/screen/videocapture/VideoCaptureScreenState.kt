/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.compose.ui.screen.videocapture

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val DEFAULT_LENS_FACING = CameraSelector.LENS_FACING_FRONT

class VideoCaptureScreenState(
    initialLensFacing: Int = DEFAULT_LENS_FACING
) {
    var lensFacing by mutableStateOf(initialLensFacing)
        private set

    var isCameraReady by mutableStateOf(false)
        private set

    var linearZoom by mutableStateOf(0f)
        private set

    var zoomRatio by mutableStateOf(1f)
        private set

    private var recording: Recording? = null

    var recordState by mutableStateOf(RecordState.IDLE)
        private set

    var recordingStatsMsg by mutableStateOf("")
        private set

    private val preview = Preview.Builder().build()
    private lateinit var recorder: Recorder
    private lateinit var videoCapture: VideoCapture<Recorder>

    private var camera: Camera? = null

    private val mainScope = MainScope()

    fun setSurfaceProvider(surfaceProvider: Preview.SurfaceProvider) {
        Log.d(TAG, "Setting Surface Provider")
        preview.setSurfaceProvider(surfaceProvider)
    }

    @JvmName("setLinearZoomFunction")
    fun setLinearZoom(linearZoom: Float) {
        Log.d(TAG, "Setting Linear Zoom $linearZoom")

        if (camera == null) {
            Log.d(TAG, "Camera is not ready to set Linear Zoom")
            return
        }

        val future = camera!!.cameraControl.setLinearZoom(linearZoom)
        mainScope.launch {
            try {
                future.await()
            } catch (exc: Exception) {
                // Log errors not related to CameraControl.OperationCanceledException
                if (exc !is CameraControl.OperationCanceledException) {
                    Log.w(TAG, "setLinearZoom: $linearZoom failed. ${exc.message}")
                }
            }
        }
    }

    fun toggleLensFacing() {
        Log.d(TAG, "Toggling Lens")
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun startTapToFocus(meteringPoint: MeteringPoint) {
        val action = FocusMeteringAction.Builder(meteringPoint).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Starting Camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Create a new recorder. CameraX currently does not support re-use of Recorder
            recorder =
                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector
                .Builder()
                .requireLensFacing(lensFacing)
                .build()

            // Remove observers from the old camera instance
            removeZoomStateObservers(lifecycleOwner)

            // Reset internal State of Camera
            camera = null
            isCameraReady = false

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )

                this.camera = camera
                setupZoomStateObserver(lifecycleOwner)
                isCameraReady = true
            } catch (exc: Exception) {
                Log.e(TAG, "Use Cases binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun captureVideo(context: Context) {
        Log.d(TAG, "Capture Video")

        // Disable button if CameraX is already stopping the recording
        if (recordState == RecordState.STOPPING) {
            return
        }

        // Stop current recording session
        val curRecording = recording
        if (curRecording != null) {
            Log.d(TAG, "Recording session exists. Stop recording")
            recordState = RecordState.STOPPING
            curRecording.stop()
            return
        }

        Log.d(TAG, "Start recording video")
        val mediaStoreOutputOptions = getMediaStoreOutputOptions(context)

        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .apply {
                val recordAudioPermission = PermissionChecker.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                )

                if (recordAudioPermission == PermissionChecker.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                // Update record stats
                val recordingStats = recordEvent.recordingStats
                val durationMs = TimeUnit.NANOSECONDS.toMillis(recordingStats.recordedDurationNanos)
                val sizeMb = recordingStats.numBytesRecorded / (1000f * 1000f)
                val msg = "%.2f s\n%.2f MB".format(durationMs / 1000f, sizeMb)
                recordingStatsMsg = msg

                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        recordState = RecordState.RECORDING
                    }
                    is VideoRecordEvent.Finalize -> {
                        // Once finalized, save the file if it is created
                        val cause = recordEvent.cause
                        when (val errorCode = recordEvent.error) {
                            ERROR_NONE, ERROR_SOURCE_INACTIVE -> { // Save Output
                                val uri = recordEvent.outputResults.outputUri
                                val successMsg = "Video saved at $uri. Code: $errorCode"
                                Log.d(TAG, successMsg, cause)
                                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                            }
                            else -> { // Handle Error
                                val failureMsg = "VideoCapture Error($errorCode): $cause"
                                Log.e(TAG, failureMsg, cause)
                            }
                        }

                        // Tear down recording
                        recordState = RecordState.IDLE
                        recording = null
                        recordingStatsMsg = ""
                    }
                }
            }
    }

    private fun getMediaStoreOutputOptions(context: Context): MediaStoreOutputOptions {
        val contentResolver = context.contentResolver
        val displayName = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        return MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }

    private fun setupZoomStateObserver(lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Setting up Zoom State Observer")

        if (camera == null) {
            Log.d(TAG, "Camera is not ready to set up observer")
            return
        }

        removeZoomStateObservers(lifecycleOwner)
        camera!!.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
            linearZoom = state.linearZoom
            zoomRatio = state.zoomRatio
        }
    }

    private fun removeZoomStateObservers(lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Removing Observers")

        if (camera == null) {
            Log.d(TAG, "Camera is not present to remove observers")
            return
        }

        camera!!.cameraInfo.zoomState.removeObservers(lifecycleOwner)
    }

    enum class RecordState {
        IDLE,
        RECORDING,
        STOPPING
    }

    companion object {
        private const val TAG = "VideoCaptureScreenState"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        val saver: Saver<VideoCaptureScreenState, *> = listSaver(
            save = {
                listOf(it.lensFacing)
            },
            restore = {
                VideoCaptureScreenState(
                    initialLensFacing = it[0]
                )
            }
        )
    }
}

@Composable
fun rememberVideoCaptureScreenState(
    initialLensFacing: Int = DEFAULT_LENS_FACING
): VideoCaptureScreenState {
    return rememberSaveable(
        initialLensFacing,
        saver = VideoCaptureScreenState.saver
    ) {
        VideoCaptureScreenState(
            initialLensFacing = initialLensFacing
        )
    }
}
