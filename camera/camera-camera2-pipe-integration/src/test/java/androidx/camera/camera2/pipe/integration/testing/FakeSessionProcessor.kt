/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import android.hardware.camera2.CameraDevice
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.OutputSurfaceConfiguration
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SessionProcessorSurface
import androidx.camera.core.impl.TagBundle

class FakeSessionProcessor : SessionProcessor {
    val previewOutputConfigId = 0
    val imageCaptureOutputConfigId = 1
    val imageAnalysisOutputConfigId = 2

    var lastParameters: androidx.camera.core.impl.Config? = null
    var startCapturesCount = 0

    override fun initSession(
        cameraInfo: CameraInfo,
        outputSurfaceConfiguration: OutputSurfaceConfiguration,
    ): SessionConfig {
        Log.debug { "$this#initSession" }
        val previewSurface =
            SessionProcessorSurface(
                    outputSurfaceConfiguration.previewOutputSurface.surface,
                    previewOutputConfigId
                )
                .also { it.setContainerClass(Preview::class.java) }
        val imageCaptureSurface =
            SessionProcessorSurface(
                    outputSurfaceConfiguration.imageCaptureOutputSurface.surface,
                    imageCaptureOutputConfigId
                )
                .also { it.setContainerClass(ImageCapture::class.java) }
        val imageAnalysisSurface =
            outputSurfaceConfiguration.imageAnalysisOutputSurface?.surface?.let { surface ->
                SessionProcessorSurface(surface, imageAnalysisOutputConfigId).also {
                    it.setContainerClass(ImageAnalysis::class.java)
                }
            }
        return SessionConfig.Builder()
            .apply {
                setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                addSurface(previewSurface)
                addSurface(imageCaptureSurface)
                imageAnalysisSurface?.let { addSurface(it) }
            }
            .build()
    }

    override fun deInitSession() {
        Log.debug { "$this#deInitSession" }
    }

    override fun setParameters(config: androidx.camera.core.impl.Config) {
        Log.debug { "$this#setParameters" }
        lastParameters = config
    }

    override fun onCaptureSessionStart(requestProcessor: RequestProcessor) {
        Log.debug { "$this#onCaptureSessionStart" }
    }

    override fun onCaptureSessionEnd() {
        Log.debug { "$this#onCaptureSessionEnd" }
    }

    override fun startRepeating(
        tagBundle: TagBundle,
        callback: SessionProcessor.CaptureCallback
    ): Int {
        Log.debug { "$this#startRepeating" }
        return 0
    }

    override fun stopRepeating() {
        Log.debug { "$this#stopRepeating" }
    }

    override fun startCapture(
        postviewEnabled: Boolean,
        tagBundle: TagBundle,
        callback: SessionProcessor.CaptureCallback
    ): Int {
        Log.debug { "$this#startCapture" }
        startCapturesCount++
        return 0
    }

    override fun abortCapture(captureSequenceId: Int) {
        TODO("Not yet implemented")
    }
}
