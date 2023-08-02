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

package androidx.camera.integration.avsync.model

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.MainThread
import androidx.camera.core.CameraSelector
import androidx.camera.core.Logger
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaStoreVideoCannotWrite
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import java.io.File

private const val TAG = "CameraHelper"

class CameraHelper {

    private val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    @MainThread
    suspend fun bindCamera(context: Context, lifecycleOwner: LifecycleOwner): Boolean {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        val recorder = Recorder.Builder().build()
        videoCapture = VideoCapture.withOutput(recorder)

        return try {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, videoCapture)
            true
        } catch (exception: Exception) {
            Logger.e(TAG, "Camera binding failed", exception)
            videoCapture = null
            false
        }
    }

    /**
     * Start video recording.
     *
     * <p> For E2E test, permissions will be handled by the launch script.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(context: Context, eventListener: Consumer<VideoRecordEvent>? = null) {
        activeRecording = videoCapture!!.let {
            val listener = eventListener ?: generateVideoRecordEventListener()
            prepareRecording(context, it.output).withAudioEnabled().start(
                ContextCompat.getMainExecutor(context),
                listener
            )
        }
    }

    private fun prepareRecording(context: Context, recorder: Recorder): PendingRecording {
        return if (canDeviceWriteToMediaStore()) {
            recorder.prepareRecording(
                context,
                generateVideoMediaStoreOptions(context.contentResolver)
            )
        } else {
            recorder.prepareRecording(
                context,
                generateVideoFileOutputOptions()
            )
        }
    }

    private fun canDeviceWriteToMediaStore(): Boolean {
        return DeviceQuirks.get(MediaStoreVideoCannotWrite::class.java) == null
    }

    fun stopRecording() {
        activeRecording!!.stop()
        activeRecording = null
    }

    fun pauseRecording() {
        activeRecording!!.pause()
    }

    fun resumeRecording() {
        activeRecording!!.resume()
    }

    private fun generateVideoFileOutputOptions(): FileOutputOptions {
        val videoFileName = "${generateVideoFileName()}.mp4"
        val videoFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES
        )
        if (!videoFolder.exists() && !videoFolder.mkdirs()) {
            Logger.e(TAG, "Failed to create directory: $videoFolder")
        }
        return FileOutputOptions.Builder(File(videoFolder, videoFileName)).build()
    }

    private fun generateVideoMediaStoreOptions(
        contentResolver: ContentResolver
    ): MediaStoreOutputOptions {
        val contentValues = generateVideoContentValues(generateVideoFileName())

        return MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
    }

    private fun generateVideoFileName(): String {
        return "video_" + System.currentTimeMillis()
    }

    private fun generateVideoContentValues(fileName: String): ContentValues {
        val res = ContentValues()
        res.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        res.put(MediaStore.Video.Media.TITLE, fileName)
        res.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        res.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        res.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())

        return res
    }

    private fun generateVideoRecordEventListener(): Consumer<VideoRecordEvent> {
        return Consumer<VideoRecordEvent> { videoRecordEvent ->
            if (videoRecordEvent is VideoRecordEvent.Finalize) {
                val uri = videoRecordEvent.outputResults.outputUri
                if (videoRecordEvent.error == VideoRecordEvent.Finalize.ERROR_NONE) {
                    Logger.d(TAG, "Video saved to: $uri")
                } else {
                    val msg = "save to uri $uri with error code (${videoRecordEvent.error})"
                    Logger.e(TAG, "Failed to save video: $msg")
                }
            }
        }
    }
}
