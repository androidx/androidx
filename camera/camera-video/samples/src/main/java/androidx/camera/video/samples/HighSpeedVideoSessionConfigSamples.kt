/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.video.samples

import android.annotation.SuppressLint
import android.util.Range
import androidx.annotation.Sampled
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.ExperimentalHighSpeedVideo
import androidx.camera.video.HighSpeedVideoSessionConfig
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

@SuppressLint("MissingPermission")
@Suppress("unused", "UnusedVariable")
@OptIn(ExperimentalHighSpeedVideo::class, ExperimentalSessionConfig::class)
@Sampled
suspend fun slowMotionVideoSample(activity: AppCompatActivity, previewView: PreviewView) {
    // Get ProcessCameraProvider.
    val cameraProvider = ProcessCameraProvider.awaitInstance(activity)

    // Get desired CameraInfo.
    val cameraInfo = cameraProvider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

    // Get high-speed video capabilities.
    val videoCapabilities = Recorder.getHighSpeedVideoCapabilities(cameraInfo)
    if (videoCapabilities == null) {
        // High-speed video is not supported on this camera device.
        return
    }

    // Get supported Qualities.
    val supportedQualities = videoCapabilities.getSupportedQualities(DynamicRange.SDR)

    // App's preferred qualities.
    val preferredQualities = selectQualities(supportedQualities)

    // Create UseCases and HighSpeedVideoSessionConfig.Builder
    val preview = Preview.Builder().build()
    preview.surfaceProvider = previewView.surfaceProvider
    val recorder =
        Recorder.Builder()
            .setQualitySelector(QualitySelector.fromOrderedList(preferredQualities))
            .build()
    val videoCapture = VideoCapture.withOutput(recorder)
    val sessionConfigBuilder =
        HighSpeedVideoSessionConfig.Builder(videoCapture)
            .setPreview(preview)
            .setSlowMotionEnabled(true)

    // Query supported frame rate ranges.
    val supportedFrameRateRanges =
        cameraInfo.getSupportedFrameRateRanges(sessionConfigBuilder.build())

    // App's preferred frame rate.
    val preferredFrameRate = selectFrameRate(supportedFrameRateRanges)

    // Apply preferred frame rate.
    sessionConfigBuilder.setFrameRateRange(preferredFrameRate)

    // Bind to lifecycle.
    cameraProvider.bindToLifecycle(
        activity,
        CameraSelector.DEFAULT_BACK_CAMERA,
        sessionConfigBuilder.build(),
    )

    // Start recording.
    val recording =
        recorder.prepareRecording(activity, generateOutputOptions()).start(
            ContextCompat.getMainExecutor(activity)
        ) { videoRecordEvent ->
            // ...
        }
}

private fun selectQualities(qualities: List<Quality>): List<Quality> {
    throw UnsupportedOperationException("$qualities")
}

private fun selectFrameRate(frameRates: Set<Range<Int>>): Range<Int> {
    throw UnsupportedOperationException("$frameRates")
}

private fun generateOutputOptions(): MediaStoreOutputOptions {
    throw UnsupportedOperationException()
}
