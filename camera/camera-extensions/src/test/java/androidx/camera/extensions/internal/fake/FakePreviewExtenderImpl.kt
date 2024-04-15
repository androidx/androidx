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
import android.hardware.camera2.params.SessionConfiguration
import android.util.Pair
import android.util.Size
import androidx.camera.extensions.impl.CaptureStageImpl
import androidx.camera.extensions.impl.PreviewExtenderImpl
import androidx.camera.extensions.impl.PreviewExtenderImpl.ProcessorType
import androidx.camera.extensions.impl.ProcessorImpl

class FakePreviewExtenderImpl(
    val supportedSizes: List<Pair<Int, Array<Size>>>? = null,
    private val processorType: ProcessorType = ProcessorType.PROCESSOR_TYPE_NONE
) : PreviewExtenderImpl {
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

    override fun isExtensionAvailable(
        cameraId: String,
        cameraCharacteristics: CameraCharacteristics
    ): Boolean = true

    override fun init(cameraId: String, cameraCharacteristics: CameraCharacteristics) {}

    override fun getCaptureStage(): CaptureStageImpl =
        object : CaptureStageImpl {
            override fun getId(): Int = 1

            override fun getParameters(): List<Pair<CaptureRequest.Key<Any>, Any>> = emptyList()
        }

    override fun getProcessorType(): ProcessorType = processorType

    override fun getProcessor(): ProcessorImpl? = null

    override fun getSupportedResolutions(): List<Pair<Int, Array<Size>>>? = supportedSizes
}
