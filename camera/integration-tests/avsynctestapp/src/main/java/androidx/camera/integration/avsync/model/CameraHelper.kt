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
import android.content.Context
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
import android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE
import android.util.Log
import android.util.Range
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.FileUtil.canDeviceWriteToMediaStore
import androidx.camera.testing.impl.FileUtil.generateVideoFileOutputOptions
import androidx.camera.testing.impl.FileUtil.generateVideoMediaStoreOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner

private const val TAG = "CameraHelper"

class CameraHelper {

    private val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    @MainThread
    suspend fun bindCamera(context: Context, lifecycleOwner: LifecycleOwner): Boolean {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()

        return try {
            // Binds to lifecycle without use cases to get camera info for necessary checks.
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            videoCapture = createVideoCapture(camera.cameraInfo)

            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, videoCapture)
            true
        } catch (exception: Exception) {
            Log.e(TAG, "Camera binding failed", exception)
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

    companion object {
        private val FPS_30 = Range(30, 30)

        private fun createVideoCapture(cameraInfo: CameraInfo): VideoCapture<Recorder> {
            val recorder = Recorder.Builder().build()
            val videoCaptureBuilder = VideoCapture.Builder<Recorder>(recorder)
            if (isLegacyDevice(cameraInfo)) {
                // Set target FPS to 30 on legacy devices. Legacy devices use lower FPS to
                // workaround exposure issues, but this makes the video timestamp info become
                // fewer and causes A/V sync test to false alarm. See AeFpsRangeLegacyQuirk.
                videoCaptureBuilder.setTargetFpsRange(FPS_30)
            }

            return videoCaptureBuilder.build()
        }

        @SuppressLint("NullAnnotationGroup")
        @OptIn(ExperimentalCamera2Interop::class)
        private fun VideoCapture.Builder<Recorder>.setTargetFpsRange(
            range: Range<Int>
        ): VideoCapture.Builder<Recorder> {
            Log.d(TAG, "Set target fps to $range")

            val camera2InterOp = Camera2Interop.Extender(this)
            camera2InterOp.setCaptureRequestOption(CONTROL_AE_TARGET_FPS_RANGE, range)
            return this
        }

        @SuppressLint("NullAnnotationGroup")
        @OptIn(ExperimentalCamera2Interop::class)
        private fun isLegacyDevice(cameraInfo: CameraInfo): Boolean {
            return Camera2CameraInfo.from(cameraInfo).getCameraCharacteristic(
                INFO_SUPPORTED_HARDWARE_LEVEL
            ) == INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
        }

        private fun prepareRecording(context: Context, recorder: Recorder): PendingRecording {
            val fileName = generateVideoFileName()
            return if (canDeviceWriteToMediaStore()) {
                recorder.prepareRecording(
                    context,
                    generateVideoMediaStoreOptions(context.contentResolver, fileName)
                )
            } else {
                recorder.prepareRecording(
                    context,
                    generateVideoFileOutputOptions(fileName)
                )
            }
        }

        private fun generateVideoFileName(): String {
            return "video_" + System.currentTimeMillis()
        }

        private fun generateVideoRecordEventListener(): Consumer<VideoRecordEvent> {
            return Consumer<VideoRecordEvent> { videoRecordEvent ->
                if (videoRecordEvent is VideoRecordEvent.Finalize) {
                    val uri = videoRecordEvent.outputResults.outputUri
                    if (videoRecordEvent.error == VideoRecordEvent.Finalize.ERROR_NONE) {
                        Log.d(TAG, "Video saved to: $uri")
                    } else {
                        val msg = "save to uri $uri with error code (${videoRecordEvent.error})"
                        Log.e(TAG, "Failed to save video: $msg")
                    }
                }
            }
        }
    }
}
