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

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.camera2.pipe.integration.compat.workaround.AeFpsRange
import androidx.camera.camera2.pipe.integration.compat.workaround.AutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

@CameraScope
public class State3AControl
@Inject
constructor(
    public val cameraProperties: CameraProperties,
    private val aeModeDisabler: AutoFlashAEModeDisabler,
    private val aeFpsRange: AeFpsRange,
) : UseCaseCameraControl, UseCaseCamera.RunningUseCasesChangeListener {
    private var _useCaseCamera: UseCaseCamera? = null
    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value
            value?.let {
                val previousSignals =
                    synchronized(lock) {
                        updateSignal = null
                        updateSignals.toList()
                    }

                invalidate() // Always apply the settings to the camera.

                synchronized(lock) { updateSignal }
                    ?.let { newUpdateSignal ->
                        previousSignals.forEach { newUpdateSignal.propagateTo(it) }
                    } ?: run { previousSignals.forEach { it.complete(Unit) } }
            }
        }

    override fun onRunningUseCasesChanged() {
        _useCaseCamera?.runningUseCases?.run { updateTemplate() }
    }

    private val lock = Any()
    private val invalidateLock = Any()

    @GuardedBy("lock") private val updateSignals = mutableSetOf<CompletableDeferred<Unit>>()

    @GuardedBy("lock")
    public var updateSignal: Deferred<Unit>? = null
        private set

    public var flashMode: Int by updateOnPropertyChange(DEFAULT_FLASH_MODE)
    public var template: Int by updateOnPropertyChange(DEFAULT_REQUEST_TEMPLATE)
    public var tryExternalFlashAeMode: Boolean by updateOnPropertyChange(false)
    public var preferredAeMode: Int? by updateOnPropertyChange(null)
    public var preferredFocusMode: Int? by updateOnPropertyChange(null)
    public var preferredAeFpsRange: Range<Int>? by
        updateOnPropertyChange(aeFpsRange.getTargetAeFpsRange())

    override fun reset() {
        synchronized(lock) { updateSignals.toList() }.cancelAll()
        tryExternalFlashAeMode = false
        preferredAeMode = null
        preferredAeFpsRange = null
        preferredFocusMode = null
        flashMode = DEFAULT_FLASH_MODE
        template = DEFAULT_REQUEST_TEMPLATE
    }

    private fun <T> updateOnPropertyChange(initialValue: T) =
        object : ObservableProperty<T>(initialValue) {
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                synchronized(invalidateLock) { super.setValue(thisRef, property, value) }
            }

            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
                if (newValue != oldValue) {
                    invalidate()
                }
            }
        }

    private fun getFinalPreferredAeMode(): Int {
        var preferAeMode =
            preferredAeMode
                ?: when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> CaptureRequest.CONTROL_AE_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                    ImageCapture.FLASH_MODE_AUTO ->
                        aeModeDisabler.getCorrectedAeMode(
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                        )
                    else -> CaptureRequest.CONTROL_AE_MODE_ON
                }

        // Overwrite AE mode to ON_EXTERNAL_FLASH only if required and explicitly supported
        if (tryExternalFlashAeMode) {
            val isSupported = cameraProperties.metadata.isExternalFlashAeModeSupported()
            debug {
                "State3AControl.invalidate: trying external flash AE mode" +
                    ", supported = $isSupported"
            }
            if (isSupported) {
                preferAeMode = CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH
            }
        }

        debug { "State3AControl.getFinalPreferredAeMode: preferAeMode = $preferAeMode" }

        return preferAeMode
    }

    public fun invalidate() {
        // TODO(b/276779600): Refactor and move the setting of these parameter to
        //  CameraGraph.Config(requiredParameters = mapOf(....)).
        synchronized(invalidateLock) {
                val preferAeMode = getFinalPreferredAeMode()
                val preferAfMode = preferredFocusMode ?: getDefaultAfMode()

                val parameters: MutableMap<CaptureRequest.Key<*>, Any> =
                    mutableMapOf(
                        CaptureRequest.CONTROL_AE_MODE to
                            cameraProperties.metadata.getSupportedAeMode(preferAeMode),
                        CaptureRequest.CONTROL_AF_MODE to
                            cameraProperties.metadata.getSupportedAfMode(preferAfMode),
                        CaptureRequest.CONTROL_AWB_MODE to
                            cameraProperties.metadata.getSupportedAwbMode(
                                CaptureRequest.CONTROL_AWB_MODE_AUTO
                            )
                    )

                preferredAeFpsRange?.let {
                    parameters[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE] = it
                }

                useCaseCamera?.requestControl?.addParametersAsync(values = parameters)
            }
            ?.apply {
                toCompletableDeferred().also { signal ->
                    synchronized(lock) {
                        updateSignals.add(signal)
                        updateSignal = signal
                        signal.invokeOnCompletion {
                            synchronized(lock) { updateSignals.remove(signal) }
                        }
                    }
                }
            } ?: run { synchronized(lock) { updateSignal = CompletableDeferred(Unit) } }
    }

    private fun getDefaultAfMode(): Int =
        when (template) {
            CameraDevice.TEMPLATE_RECORD -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            CameraDevice.TEMPLATE_PREVIEW -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            else -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        }

    private fun Collection<UseCase>.updateTemplate() {
        SessionConfigAdapter(this).getValidSessionConfigOrNull()?.let {
            val templateType = it.repeatingCaptureConfig.templateType
            template =
                if (templateType != CaptureConfig.TEMPLATE_TYPE_NONE) {
                    templateType
                } else {
                    DEFAULT_REQUEST_TEMPLATE
                }
        }
    }

    private fun <T> Deferred<T>.toCompletableDeferred() =
        CompletableDeferred<T>().also { propagateTo(it) }

    private fun <T> Collection<CompletableDeferred<T>>.cancelAll() = forEach {
        it.completeExceptionally(CameraControl.OperationCanceledException("Camera is not active."))
    }

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(state3AControl: State3AControl): UseCaseCameraControl
    }
}
