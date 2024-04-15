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

package androidx.camera.extensions.internal.fake

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.util.Pair
import android.util.Size
import android.view.Surface
import androidx.camera.extensions.impl.CaptureProcessorImpl
import androidx.camera.extensions.impl.CaptureStageImpl
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl
import androidx.camera.extensions.impl.ProcessResultImpl
import java.util.concurrent.Executor

class FakeImageCaptureExtenderImpl(
    val supportedRequestKeys: List<CaptureRequest.Key<out Any>> = emptyList(),
    val supportedSizes: List<Pair<Int, Array<Size>>>? = null,
    val captureProcessorImpl: CaptureProcessorImpl? = null
) : ImageCaptureExtenderImpl {
    override fun isExtensionAvailable(
        cameraId: String,
        cameraCharacteristics: CameraCharacteristics
    ): Boolean = true

    override fun init(cameraId: String, cameraCharacteristics: CameraCharacteristics) {}

    override fun getCaptureProcessor() = captureProcessorImpl

    override fun getCaptureStages(): List<CaptureStageImpl> = emptyList()

    override fun getMaxCaptureStage() = 1

    override fun getSupportedResolutions() = supportedSizes

    override fun getEstimatedCaptureLatencyRange(size: Size?) = null

    override fun getAvailableCaptureRequestKeys(): List<CaptureRequest.Key<out Any>> {
        return supportedRequestKeys
    }

    override fun getAvailableCaptureResultKeys(): List<CaptureResult.Key<Any>> {
        return mutableListOf()
    }

    override fun getSupportedPostviewResolutions(
        captureSize: Size
    ): MutableList<Pair<Int, Array<Size>>>? = null

    override fun isCaptureProcessProgressAvailable() = false

    override fun getRealtimeCaptureLatency(): Pair<Long, Long>? = null

    override fun isPostviewAvailable() = false

    override fun onInit(
        cameraId: String,
        cameraCharacteristics: CameraCharacteristics,
        context: Context
    ) {}

    override fun onDeInit() {}

    override fun onPresetSession(): CaptureStageImpl? = null

    override fun onEnableSession(): CaptureStageImpl? = null

    override fun onDisableSession(): CaptureStageImpl? = null

    override fun onSessionType(): Int = SessionConfiguration.SESSION_REGULAR
}

class FakeCaptureProcessorImpl : CaptureProcessorImpl {
    override fun process(results: MutableMap<Int, Pair<Image, TotalCaptureResult>>) {}

    override fun onPostviewOutputSurface(surface: Surface) {}

    override fun onResolutionUpdate(size: Size, postviewSize: Size) {}

    override fun process(
        results: MutableMap<Int, Pair<Image, TotalCaptureResult>>,
        resultCallback: ProcessResultImpl,
        executor: Executor?
    ) {}

    override fun processWithPostview(
        results: MutableMap<Int, Pair<Image, TotalCaptureResult>>,
        resultCallback: ProcessResultImpl,
        executor: Executor?
    ) {}

    override fun onOutputSurface(surface: Surface, imageFormat: Int) {}

    override fun onResolutionUpdate(size: Size) {}

    override fun onImageFormatUpdate(imageFormat: Int) {}
}
