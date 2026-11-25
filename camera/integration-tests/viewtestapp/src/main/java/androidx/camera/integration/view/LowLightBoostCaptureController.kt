/*
 * Copyright 2026 The Android Open Source Project
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

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles the Capture and Record buttons of the LowLightBoostEffect test UI, so that captured
 * images and recorded videos can be checked manually, e.g. their orientation after rotating the
 * device.
 *
 * Each button is shown only when the corresponding use case is bound, and enabled only while the
 * preview is streaming and no capture or recording finalization is in progress. Images and videos
 * are saved to `Pictures/CameraX-LLB` and `Movies/CameraX-LLB`. Audio is recorded when the
 * RECORD_AUDIO permission is granted.
 *
 * All methods must be called on the main thread.
 */
@RequiresApi(Build.VERSION_CODES.R)
internal class LowLightBoostCaptureController(
    private val context: Context,
    private val captureButton: Button,
    private val recordButton: Button,
    private val onRecordingStateChanged: (isRecording: Boolean) -> Unit,
    private val showMessage: (String) -> Unit,
) {
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var isStreaming = false
    private var isCapturing = false
    private var activeRecording: Recording? = null
    private var isFinalizingRecording = false

    /** Whether a recording is in progress. */
    val isRecording: Boolean
        get() = activeRecording != null

    init {
        captureButton.setOnClickListener { takePicture() }
        recordButton.setOnClickListener { toggleRecording() }
        updateButtons()
    }

    /** Sets the currently bound use cases, or null if the use case is not bound. */
    fun setUseCases(imageCapture: ImageCapture?, videoCapture: VideoCapture<Recorder>?) {
        stopRecording()
        this.imageCapture = imageCapture
        this.videoCapture = videoCapture
        isCapturing = false
        updateButtons()
    }

    /** Sets whether the preview is streaming. */
    fun setStreaming(isStreaming: Boolean) {
        if (this.isStreaming != isStreaming) {
            this.isStreaming = isStreaming
            updateButtons()
        }
    }

    /** Stops the active recording, if any. */
    fun stopRecording() {
        val recording = activeRecording ?: return
        isFinalizingRecording = true
        recording.stop()
        updateButtons()
    }

    private fun updateButtons() {
        captureButton.visibility = if (imageCapture != null) View.VISIBLE else View.GONE
        captureButton.isEnabled = isStreaming && !isCapturing
        recordButton.visibility = if (videoCapture != null) View.VISIBLE else View.GONE
        recordButton.setText(if (isRecording) R.string.llb_stop_recording else R.string.llb_record)
        recordButton.isEnabled = isStreaming && !isFinalizingRecording
    }

    private fun takePicture() {
        val capture = imageCapture ?: return
        isCapturing = true
        updateButtons()
        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    createContentValues("image/jpeg", PICTURES_RELATIVE_PATH),
                )
                .build()
        try {
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.d(TAG, "Image saved: ${outputFileResults.savedUri}")
                        onCaptureFinished(capture, "Image saved: ${outputFileResults.savedUri}")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Image capture failed", exception)
                        onCaptureFinished(capture, "Image capture failed: ${exception.message}")
                    }
                },
            )
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failed to start image capture", e)
            onCaptureFinished(capture, "Image capture failed: ${e.message}")
        }
    }

    private fun onCaptureFinished(capture: ImageCapture, message: String) {
        if (this.imageCapture === capture) {
            isCapturing = false
            updateButtons()
        }
        showMessage(message)
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    // RECORD_AUDIO is checked before enabling audio.
    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val capture = videoCapture ?: return
        val outputOptions =
            MediaStoreOutputOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                )
                .setContentValues(createContentValues("video/mp4", MOVIES_RELATIVE_PATH))
                .build()
        try {
            val pendingRecording = capture.output.prepareRecording(context, outputOptions)
            val isAudioGranted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            if (isAudioGranted) {
                pendingRecording.withAudioEnabled()
            }
            activeRecording =
                pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        onRecordingFinalized(event)
                    }
                }
            Log.d(TAG, "Recording started: audio=$isAudioGranted")
            updateButtons()
            onRecordingStateChanged(true)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failed to start recording", e)
            showMessage("Recording failed: ${e.message}")
        }
    }

    private fun onRecordingFinalized(event: VideoRecordEvent.Finalize) {
        activeRecording = null
        isFinalizingRecording = false
        val message =
            if (event.hasError()) {
                Log.e(TAG, "Recording failed: error=${event.error}", event.cause)
                "Recording failed: error=${event.error}"
            } else {
                Log.d(TAG, "Video saved: ${event.outputResults.outputUri}")
                "Video saved: ${event.outputResults.outputUri}"
            }
        updateButtons()
        onRecordingStateChanged(false)
        showMessage(message)
    }

    private fun createContentValues(mimeType: String, relativePath: String): ContentValues {
        val name = "LLB_" + SimpleDateFormat(FILE_NAME_FORMAT, Locale.US).format(Date())
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
    }

    private companion object {
        private const val TAG = "LlbCaptureController"
        private const val FILE_NAME_FORMAT = "yyyyMMdd_HHmmss_SSS"
        private const val PICTURES_RELATIVE_PATH = "Pictures/CameraX-LLB"
        private const val MOVIES_RELATIVE_PATH = "Movies/CameraX-LLB"
    }
}
