/*
 * Copyright 2021 The Android Open Source Project
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

import android.hardware.camera2.CaptureRequest
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.camera2.pipe.integration.compat.workaround.isFlashAvailable
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
import androidx.camera.core.TorchState
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.utils.Threads
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

/**
 * Implementation of Torch control exposed by [CameraControlInternal].
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class TorchControl @Inject constructor(
    cameraProperties: CameraProperties,
    private val state3AControl: State3AControl,
    private val threads: UseCaseThreads,
) : UseCaseCameraControl {

    private var _useCaseCamera: UseCaseCamera? = null
    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value
            setTorchAsync(
                torch = when (torchStateLiveData.value) {
                    TorchState.ON -> true
                    else -> false
                },
                cancelPreviousTask = false,
            )
        }

    override fun reset() {
        _torchState.setLiveDataValue(false)
        threads.sequentialScope.launch {
            stopRunningTaskInternal()
        }
        setTorchAsync(false)
    }

    private val hasFlashUnit: Boolean = cameraProperties.isFlashAvailable()

    private val _torchState = MutableLiveData(TorchState.OFF)
    val torchStateLiveData: LiveData<Int>
        get() = _torchState

    private var _updateSignal: CompletableDeferred<Unit>? = null

    /**
     * Turn the torch on or off.
     *
     * @param torch Whether the torch should be on or off.
     * @param cancelPreviousTask Whether to cancel the previous task if it's running.
     * @param ignoreFlashUnitAvailability Whether to ignore the flash unit availability. When true,
     *      torch mode setting will be attempted even if a physical flash unit is not available.
     */
    fun setTorchAsync(
        torch: Boolean,
        cancelPreviousTask: Boolean = true,
        ignoreFlashUnitAvailability: Boolean = false
    ): Deferred<Unit> {
        val signal = CompletableDeferred<Unit>()

        if (!ignoreFlashUnitAvailability && !hasFlashUnit) {
            return signal.createFailureResult(IllegalStateException("No flash unit"))
        }

        useCaseCamera?.let { useCaseCamera ->

            _torchState.setLiveDataValue(torch)

            threads.sequentialScope.launch {
                if (cancelPreviousTask) {
                    stopRunningTaskInternal()
                } else {
                    // Propagate the result to the previous updateSignal
                    _updateSignal?.let { previousUpdateSignal ->
                        signal.propagateTo(previousUpdateSignal)
                    }
                }

                _updateSignal = signal

                // TODO(b/209757083), handle the failed result of the setTorchAsync().
                useCaseCamera.requestControl.setTorchAsync(torch).join()

                // Hold the internal AE mode to ON while the torch is turned ON.
                state3AControl.preferredAeMode =
                    if (torch) CaptureRequest.CONTROL_AE_MODE_ON else null

                // Always update3A again to reset the AE state in the Camera-pipe controller.
                state3AControl.invalidate()
                state3AControl.updateSignal?.propagateTo(signal) ?: run { signal.complete(Unit) }
            }
        } ?: run {
            signal.createFailureResult(
                CameraControl.OperationCanceledException("Camera is not active.")
            )
        }

        return signal
    }

    private fun stopRunningTaskInternal() {
        _updateSignal?.createFailureResult(
            CameraControl.OperationCanceledException(
                "There is a new enableTorch being set"
            )
        )
        _updateSignal = null
    }

    private fun CompletableDeferred<Unit>.createFailureResult(exception: Exception) = apply {
        completeExceptionally(exception)
    }

    private fun MutableLiveData<Int>.setLiveDataValue(enableTorch: Boolean) = when (enableTorch) {
        true -> TorchState.ON
        false -> TorchState.OFF
    }.let { torchState ->
        if (Threads.isMainThread()) {
            this.value = torchState
        } else {
            this.postValue(torchState)
        }
    }

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(torchControl: TorchControl): UseCaseCameraControl
    }
}
