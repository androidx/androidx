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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.ImageCapture
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

@CameraScope
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class State3AControl @Inject constructor(
    val cameraProperties: CameraProperties,
) : UseCaseCameraControl {
    private var _useCaseCamera: UseCaseCamera? = null
    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value
            invalidate() // Always apply the settings to the camera.
        }

    private val aeModes = cameraProperties.metadata.getOrDefault(
        CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES,
        intArrayOf(CaptureRequest.CONTROL_AE_MODE_OFF)
    ).asList()
    private val awbModes = cameraProperties.metadata.getOrDefault(
        CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES,
        intArrayOf(CaptureRequest.CONTROL_AWB_MODE_OFF)
    ).asList()

    var updateSignal: Deferred<Unit>? = null
        private set
    var flashMode by updateOnPropertyChange(ImageCapture.FLASH_MODE_OFF)
    var preferredAeMode: Int? by updateOnPropertyChange(null)

    override fun reset() {
        preferredAeMode = null
        flashMode = ImageCapture.FLASH_MODE_OFF
    }

    private fun <T> updateOnPropertyChange(
        initialValue: T
    ) = object : ObservableProperty<T>(initialValue) {
        override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
            if (newValue != oldValue) {
                invalidate()
            }
        }
    }

    fun invalidate() {
        val preferAeMode = preferredAeMode ?: when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> CaptureRequest.CONTROL_AE_MODE_ON
            ImageCapture.FLASH_MODE_ON -> CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
            ImageCapture.FLASH_MODE_AUTO -> CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            // TODO(b/209383160): porting the Quirk for AEModeDisabler
            //      mAutoFlashAEModeDisabler.getCorrectedAeMode(
            //      CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            //    )
            else -> CaptureRequest.CONTROL_AE_MODE_ON
        }

        updateSignal = useCaseCamera?.requestControl?.addParametersAsync(
            values = mapOf(
                CaptureRequest.CONTROL_AE_MODE to getSupportedAeMode(preferAeMode),
                CaptureRequest.CONTROL_AWB_MODE to getSupportedAwbMode(
                    CaptureRequest.CONTROL_AWB_MODE_AUTO
                ),
            )
        ) ?: CompletableDeferred(null)
    }

    /**
     * If preferredMode not available, priority is AE_ON > AE_OFF
     */
    private fun getSupportedAeMode(preferredMode: Int) = when {
        aeModes.contains(preferredMode) -> {
            preferredMode
        }

        aeModes.contains(CaptureRequest.CONTROL_AE_MODE_ON) -> {
            CaptureRequest.CONTROL_AE_MODE_ON
        }

        else -> {
            CaptureRequest.CONTROL_AE_MODE_OFF
        }
    }

    /**
     * If preferredMode not available, priority is AWB_AUTO > AWB_OFF
     */
    private fun getSupportedAwbMode(preferredMode: Int) = when {
        awbModes.contains(preferredMode) -> {
            preferredMode
        }

        awbModes.contains(CaptureRequest.CONTROL_AWB_MODE_AUTO) -> {
            CaptureRequest.CONTROL_AWB_MODE_AUTO
        }

        else -> {
            CaptureRequest.CONTROL_AWB_MODE_OFF
        }
    }

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(state3AControl: State3AControl): UseCaseCameraControl
    }
}