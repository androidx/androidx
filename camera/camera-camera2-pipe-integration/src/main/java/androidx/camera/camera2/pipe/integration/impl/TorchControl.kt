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
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
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

/** Implementation of Torch control exposed by [CameraControlInternal]. */
@CameraScope
public class TorchControl
@Inject
constructor(
    cameraProperties: CameraProperties,
    private val state3AControl: State3AControl,
    private val threads: UseCaseThreads,
) : UseCaseCameraControl {

    private var _requestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            _requestControl = value
            setTorchAsync(
                torch =
                    when (torchStateLiveData.value) {
                        TorchState.ON -> true
                        else -> false
                    },
                cancelPreviousTask = false,
            )
        }

    override fun reset() {
        _torchState.setLiveDataValue(false)
        stopRunningTaskInternal()
        setTorchAsync(false)
    }

    private val hasFlashUnit: Boolean = cameraProperties.isFlashAvailable()

    private val _torchState = MutableLiveData(TorchState.OFF)
    public val torchStateLiveData: LiveData<Int>
        get() = _torchState

    private var _updateSignal: CompletableDeferred<Unit>? = null

    /**
     * Turn the torch on or off.
     *
     * @param torch Whether the torch should be on or off.
     * @param cancelPreviousTask Whether to cancel the previous task if it's running.
     * @param ignoreFlashUnitAvailability Whether to ignore the flash unit availability. When true,
     *   torch mode setting will be attempted even if a physical flash unit is not available.
     */
    public fun setTorchAsync(
        torch: Boolean,
        cancelPreviousTask: Boolean = true,
        ignoreFlashUnitAvailability: Boolean = false
    ): Deferred<Unit> {
        debug { "TorchControl#setTorchAsync: torch = $torch" }

        val signal = CompletableDeferred<Unit>()

        if (!ignoreFlashUnitAvailability && !hasFlashUnit) {
            return signal.createFailureResult(IllegalStateException("No flash unit"))
        }

        requestControl?.let { requestControl ->
            _torchState.setLiveDataValue(torch)

            if (cancelPreviousTask) {
                stopRunningTaskInternal()
            } else {
                // Propagate the result to the previous updateSignal
                _updateSignal?.let { previousUpdateSignal ->
                    signal.propagateTo(previousUpdateSignal)
                }
            }

            _updateSignal = signal

            // Hold the internal AE mode to ON while the torch is turned ON. If torch is OFF, a
            // value of null will make the state3AControl calculate the correct AE mode based on
            // other settings.
            state3AControl.preferredAeMode = if (torch) CaptureRequest.CONTROL_AE_MODE_ON else null
            val aeMode: AeMode =
                AeMode.fromIntOrNull(state3AControl.getFinalSupportedAeMode())
                    ?: run {
                        warn {
                            "TorchControl#setTorchAsync: Failed to convert ae mode of value" +
                                " ${state3AControl.getFinalSupportedAeMode()} with" +
                                " AeMode.fromIntOrNull, fallback to AeMode.ON"
                        }
                        AeMode.ON
                    }

            val deferred =
                if (torch) requestControl.setTorchOnAsync()
                else requestControl.setTorchOffAsync(aeMode)
            deferred.propagateTo(signal) {
                // TODO: b/209757083 - handle the failed result of the setTorchAsync().
                //   Since we are not handling the result here, signal is completed with Unit
                //   value here without exception when source deferred completes (returning Unit
                //   explicitly is redundant and thus this block looks empty)
            }
        }
            ?: run {
                signal.createFailureResult(
                    CameraControl.OperationCanceledException("Camera is not active.")
                )
            }

        return signal
    }

    private fun stopRunningTaskInternal() {
        _updateSignal?.createFailureResult(
            CameraControl.OperationCanceledException("There is a new enableTorch being set")
        )
        _updateSignal = null
    }

    private fun CompletableDeferred<Unit>.createFailureResult(exception: Exception) = apply {
        completeExceptionally(exception)
    }

    private fun MutableLiveData<Int>.setLiveDataValue(enableTorch: Boolean) =
        when (enableTorch) {
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
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(torchControl: TorchControl): UseCaseCameraControl
    }
}
