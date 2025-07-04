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
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.CameraMetadata.Companion.defaultTorchStrengthLevel
import androidx.camera.camera2.pipe.CameraMetadata.Companion.maxTorchStrengthLevel
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsTorchStrength
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.camera2.pipe.integration.compat.Api35Compat
import androidx.camera.camera2.pipe.integration.compat.workaround.isFlashAvailable
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
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
                        androidx.camera.core.TorchState.ON -> true
                        else -> false
                    },
                cancelPreviousTask = false,
            )
        }

    override fun reset() {
        updateTorchState(TorchMode.OFF)
        stopRunningTaskInternal()
        setTorchAsync(false)
    }

    private val hasFlashUnit: Boolean = cameraProperties.isFlashAvailable()

    @VisibleForTesting internal var torchMode = TorchMode.OFF
    private val _torchState = MutableLiveData(androidx.camera.core.TorchState.OFF)
    public val torchStateLiveData: LiveData<Int>
        get() = _torchState

    private val isTorchStrengthSupported: Boolean = cameraProperties.metadata.supportsTorchStrength

    private val defaultTorchStrength: Int = cameraProperties.metadata.defaultTorchStrengthLevel

    private val maxTorchStrength: Int = cameraProperties.metadata.maxTorchStrengthLevel

    private val _torchStrength = MutableLiveData(defaultTorchStrength)

    public val torchStrengthLiveData: LiveData<Int>
        get() = _torchStrength

    private var _updateTorchStateSignal: CompletableDeferred<Unit>? = null

    private var _updateTorchStrengthSignal: CompletableDeferred<Unit>? = null

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
        ignoreFlashUnitAvailability: Boolean = false,
    ): Deferred<Unit> {
        val torchMode = if (torch) TorchMode.ON else TorchMode.OFF
        return setTorchAsync(torchMode, cancelPreviousTask, ignoreFlashUnitAvailability)
    }

    internal fun setTorchAsync(
        mode: TorchMode,
        cancelPreviousTask: Boolean = true,
        ignoreFlashUnitAvailability: Boolean = false,
    ): Deferred<Unit> {
        debug { "TorchControl#setTorchAsync: torch mode = $mode" }

        val signal = CompletableDeferred<Unit>()

        if (!ignoreFlashUnitAvailability && !hasFlashUnit) {
            return signal.createFailureResult(IllegalStateException("No flash unit"))
        }

        requestControl?.let { requestControl ->
            updateTorchState(mode)

            if (cancelPreviousTask) {
                stopTorchStateTask()
            } else {
                // Propagate the result to the previous updateSignal
                _updateTorchStateSignal?.let { previousUpdateSignal ->
                    signal.propagateTo(previousUpdateSignal)
                }
            }

            _updateTorchStateSignal = signal

            // Hold the internal AE mode to ON while the torch is turned ON. If torch is OFF, a
            // value of null will make the state3AControl calculate the correct AE mode based on
            // other settings.
            state3AControl.preferredAeMode =
                if (isFlashUnitOn(mode)) CaptureRequest.CONTROL_AE_MODE_ON else null
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
                if (isFlashUnitOn(mode)) {
                    if (mode == TorchMode.ON) {
                        // Only set the torch strength if torch is explicitly turned on as torch
                        // purpose.
                        torchStrengthLiveData.value?.let { updateTorchStrengthLevelAsync(it) }
                    } else {
                        // Use the default torch strength if torch is turned on as flash purpose.
                        updateTorchStrengthLevelAsync(defaultTorchStrength)
                    }

                    requestControl.setTorchOnAsync()
                } else requestControl.setTorchOffAsync(aeMode)
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

    public fun setTorchStrengthLevelAsync(level: Int): Deferred<Unit> {
        if (!isTorchStrengthSupported) {
            return CompletableDeferred<Unit>().apply {
                createFailureResult(
                    UnsupportedOperationException(
                        "Configuring torch strength is not supported on the device."
                    )
                )
            }
        }

        if (level < 1 || level > maxTorchStrength) {
            return CompletableDeferred<Unit>().apply {
                createFailureResult(
                    IllegalArgumentException("The given torch strength level is invalid.")
                )
            }
        }

        _torchStrength.setLiveDataValue(level)

        return if (torchStateLiveData.value == androidx.camera.core.TorchState.ON) {
            updateTorchStrengthLevelAsync(level)
        } else CompletableDeferred(Unit)
    }

    private fun updateTorchStrengthLevelAsync(level: Int): Deferred<Unit> {
        var signal = CompletableDeferred<Unit>()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                isTorchStrengthSupported
        ) {
            if (_updateTorchStrengthSignal != null) {
                stopTorchStrengthTask()
            }

            _updateTorchStrengthSignal = signal
            signal.invokeOnCompletion { _updateTorchStrengthSignal = null }

            val parameters: MutableMap<CaptureRequest.Key<*>, Any> = mutableMapOf()
            Api35Compat.setFlashStrengthLevel(parameters, level)
            requestControl?.setParametersAsync(values = parameters)?.propagateTo(signal)
                ?: run {
                    signal.createFailureResult(
                        CameraControl.OperationCanceledException("Camera is not active.")
                    )
                }
        } else {
            signal.createFailureResult(
                UnsupportedOperationException(
                    "Configuring torch strength is not supported on the device."
                )
            )
        }

        return signal
    }

    private fun stopRunningTaskInternal() {
        stopTorchStateTask()
        stopTorchStrengthTask()
    }

    private fun stopTorchStateTask() {
        _updateTorchStateSignal?.createFailureResult(
            CameraControl.OperationCanceledException("There is a new enableTorch being set")
        )
        _updateTorchStateSignal = null
    }

    private fun stopTorchStrengthTask() {
        _updateTorchStrengthSignal?.createFailureResult(
            CameraControl.OperationCanceledException("There is a new torch strength being set")
        )
        _updateTorchStrengthSignal = null
    }

    private fun CompletableDeferred<Unit>.createFailureResult(exception: Exception) = apply {
        completeExceptionally(exception)
    }

    private fun updateTorchState(mode: TorchMode) {
        torchMode = mode
        when (mode) {
            TorchMode.ON -> androidx.camera.core.TorchState.ON
            else -> androidx.camera.core.TorchState.OFF
        }.let { torchState -> _torchState.setLiveDataValue(torchState) }
    }

    private fun MutableLiveData<Int>.setLiveDataValue(value: Int) {
        if (Threads.isMainThread()) {
            this.value = value
        } else {
            this.postValue(value)
        }
    }

    private fun isFlashUnitOn(torchState: TorchMode): Boolean {
        return torchState != TorchMode.OFF
    }

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(torchControl: TorchControl): UseCaseCameraControl
    }

    @JvmInline
    internal value class TorchMode private constructor(val value: Int) {
        companion object {
            /** The torch is off. */
            val OFF: TorchMode = TorchMode(0)
            /** The torch is turned on explicitly. */
            val ON: TorchMode = TorchMode(1)
            /**
             * The torch is used as flash.
             *
             * The flash unit is turned on by the capture pipeline for flash purpose, while in this
             * case the torch feature should usually be considered off.
             */
            val USED_AS_FLASH: TorchMode = TorchMode(2)
        }
    }
}
