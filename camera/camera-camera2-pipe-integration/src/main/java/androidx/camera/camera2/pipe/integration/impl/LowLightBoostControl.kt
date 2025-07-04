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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
import android.hardware.camera2.CameraMetadata.CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE
import android.hardware.camera2.CaptureResult.CONTROL_LOW_LIGHT_BOOST_STATE
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsLowLightBoost
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
import androidx.camera.core.LowLightBoostState
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.utils.Threads
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/** Implementation of LowLightBoost control exposed by [CameraControlInternal]. */
@CameraScope
public class LowLightBoostControl
@Inject
constructor(
    private val cameraMetadata: CameraMetadata?,
    private val state3AControl: State3AControl,
    private val threads: UseCaseThreads,
    private val comboRequestListener: ComboRequestListener,
) : UseCaseCameraControl {

    private var _requestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            _requestControl = value

            if (isLowLightBoostOn) {
                if (value != null) {
                    setLowLightBoostAsync(lowLightBoost = true, cancelPreviousTask = false)
                } else {
                    // Updates the state to be INACTIVE when the LLB is ON but the control becomes
                    // inactive.
                    _lowLightBoostState.setLiveDataValue(LowLightBoostState.INACTIVE)
                }
            }
        }

    override fun reset() {
        stopRunningTaskInternal()
        setLowLightBoostAsync(false)
    }

    private val isLowLightBoostSupported: Boolean = cameraMetadata?.supportsLowLightBoost == true

    private var isLowLightBoostOn = false

    private val _lowLightBoostState = MutableLiveData(LowLightBoostState.OFF)
    public val lowLightBoostStateLiveData: LiveData<Int>
        get() = _lowLightBoostState

    private val lowLightBoostStateAtomic = AtomicInteger(LowLightBoostState.OFF)

    private var _updateSignal: CompletableDeferred<Unit>? = null

    init {
        /** Sets the state update listener when low-light boost is supported */
        if (isLowLightBoostSupported) {
            object : Request.Listener {
                    override fun onTotalCaptureResult(
                        requestMetadata: RequestMetadata,
                        frameNumber: FrameNumber,
                        totalCaptureResult: FrameInfo,
                    ) {
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                                _requestControl != null
                        ) {
                            // Updates the state to the LLB state live data
                            if (isLowLightBoostOn) {
                                totalCaptureResult.metadata[CONTROL_LOW_LIGHT_BOOST_STATE]?.let {
                                    _lowLightBoostState.setLiveDataValue(
                                        when (it) {
                                            CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE ->
                                                LowLightBoostState.ACTIVE
                                            else -> LowLightBoostState.INACTIVE
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                .let { comboRequestListener.addListener(it, threads.sequentialExecutor) }
        }
    }

    private var isLowLightBoostDisabledByUseCaseSessionConfig = false

    /**
     * Turn the Low Light Boost on or off.
     *
     * @param lowLightBoost Whether the low-light boost should be on or off.
     * @param cancelPreviousTask Whether to cancel the previous task if it's running.
     */
    public fun setLowLightBoostAsync(
        lowLightBoost: Boolean,
        cancelPreviousTask: Boolean = true,
    ): Deferred<Unit> {
        debug { "LowLightBoostControl#setLowLightBoostAsync: lowLightBoost = $lowLightBoost" }

        val signal = CompletableDeferred<Unit>()

        if (!isLowLightBoostSupported) {
            return signal.createFailureResult(
                IllegalStateException("Low Light Boost is not supported!")
            )
        }

        if (isLowLightBoostDisabledByUseCaseSessionConfig) {
            _lowLightBoostState.setLiveDataValue(LowLightBoostState.OFF)
            return signal.createFailureResult(
                IllegalStateException(
                    "Low Light Boost is disabled" +
                        " when expected frame rate range exceeds 30 or HDR 10-bit is on."
                )
            )
        }

        isLowLightBoostOn = lowLightBoost

        if (!lowLightBoost) {
            _lowLightBoostState.setLiveDataValue(LowLightBoostState.OFF)
        }

        requestControl?.let {
            if (lowLightBoost) {
                _lowLightBoostState.setLiveDataValue(LowLightBoostState.INACTIVE)
            }

            if (cancelPreviousTask) {
                stopRunningTaskInternal()
            } else {
                // Propagate the result to the previous updateSignal
                _updateSignal?.let { previousUpdateSignal ->
                    signal.propagateTo(previousUpdateSignal)
                }
            }

            _updateSignal = signal

            // Hold the internal AE mode to ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY while the
            // low-light boost is turned ON. If low-light boost is OFF, a value of null will make
            // the state3AControl calculate the correct AE mode based on other settings.
            state3AControl.preferredAeMode =
                if (lowLightBoost) CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY else null

            state3AControl.updateSignal?.propagateTo(signal) ?: run { signal.complete(Unit) }

            signal.invokeOnCompletion {
                if (signal == _updateSignal) {
                    _updateSignal = null
                }
            }
        }
            ?: run {
                signal.createFailureResult(
                    CameraControl.OperationCanceledException("Camera is not active.")
                )
            }

        return signal
    }

    public fun setLowLightBoostDisabledByUseCaseSessionConfig(disabled: Boolean) {
        isLowLightBoostDisabledByUseCaseSessionConfig = disabled
    }

    public fun isLowLightBoostDisabledByUseCaseSessionConfig(): Boolean =
        isLowLightBoostDisabledByUseCaseSessionConfig

    private fun stopRunningTaskInternal() {
        _updateSignal?.createFailureResult(
            CameraControl.OperationCanceledException("There is a new enableLowLightBoost being set")
        )
        _updateSignal = null
    }

    private fun CompletableDeferred<Unit>.createFailureResult(exception: Exception) = apply {
        completeExceptionally(exception)
    }

    private fun MutableLiveData<Int>.setLiveDataValue(@LowLightBoostState.State state: Int) {
        if (lowLightBoostStateAtomic.getAndSet(state) != state) {
            if (Threads.isMainThread()) {
                this.value = state
            } else {
                this.postValue(state)
            }
        }
    }

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(
            lowLightBoostControl: LowLightBoostControl
        ): UseCaseCameraControl
    }
}
