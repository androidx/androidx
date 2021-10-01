/*
 * Copyright 2020 The Android Open Source Project
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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.compat

import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE
import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP
import android.hardware.camera2.CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
import android.hardware.camera2.CaptureResult
import android.util.Range
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.core.CameraControl
import dagger.Binds
import dagger.Module
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import javax.inject.Inject

interface EvCompCompat {
    val supported: Boolean
    val range: Range<Int>
    val step: Rational

    fun stopRunningTask()

    fun applyAsync(
        evCompIndex: Int,
        camera: UseCaseCamera
    ): Deferred<Int>

    @Module
    abstract class Bindings {
        @Binds
        abstract fun bindEvCompImpl(impl: EvCompImpl): EvCompCompat
    }
}

internal val EMPTY_RANGE = Range(0, 0)

/**
 * The implementation of the [EvCompCompat]. The [applyAsync] update the new exposure index value
 * to the camera, and wait for the exposure value of the camera reach to the new target.
 * It receives the [FrameInfo] via the [ComboRequestListener] to monitor the capture result.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class EvCompImpl @Inject constructor(
    private val cameraProperties: CameraProperties,
    private val threads: UseCaseThreads,
    private val comboRequestListener: ComboRequestListener,
) : EvCompCompat {
    override val supported: Boolean
        get() = range.upper != 0 && range.lower != 0
    override val range: Range<Int> by lazy {
        cameraProperties.metadata.getOrDefault(CONTROL_AE_COMPENSATION_RANGE, EMPTY_RANGE)
    }
    override val step: Rational
        get() = if (!supported) {
            Rational.ZERO
        } else {
            cameraProperties.metadata[CONTROL_AE_COMPENSATION_STEP]!!
        }

    private var updateSignal: CompletableDeferred<Int>? = null
    private var updateListener: Request.Listener? = null

    override fun stopRunningTask() {
        threads.sequentialScope.launch {
            stopRunningTaskInternal()
        }
    }

    override fun applyAsync(evCompIndex: Int, camera: UseCaseCamera): Deferred<Int> {
        val signal = CompletableDeferred<Int>()

        threads.sequentialScope.launch {
            stopRunningTaskInternal()
            updateSignal = signal

            camera.setParameterAsync(CONTROL_AE_EXPOSURE_COMPENSATION, evCompIndex)

            // Prepare the listener to wait for the exposure value to reach the target.
            updateListener = object : Request.Listener {
                override fun onComplete(
                    requestMetadata: RequestMetadata,
                    frameNumber: FrameNumber,
                    result: FrameInfo
                ) {
                    val state = result.metadata[CaptureResult.CONTROL_AE_STATE]
                    val evResult = result.metadata[CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION]
                    if (state != null && evResult != null) {
                        when (state) {
                            CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                            CaptureResult.CONTROL_AE_STATE_CONVERGED,
                            CaptureResult.CONTROL_AE_STATE_LOCKED ->
                                if (evResult == evCompIndex) {
                                    signal.complete(evCompIndex)
                                    comboRequestListener.removeListener(this)
                                }
                            else -> {
                            }
                        }
                    } else if (evResult != null && evResult == evCompIndex) {
                        // If AE state is null, only wait for the exposure result to the desired
                        // value.
                        signal.complete(evCompIndex)

                        // Remove the capture result listener. The updateSignal and
                        // updateListener will be cleared before the next set exposure task.
                        comboRequestListener.removeListener(this)
                    }
                }
            }
            comboRequestListener.addListener(updateListener!!, threads.sequentialExecutor)
        }

        return signal
    }

    private fun stopRunningTaskInternal() {
        updateSignal?.let {
            it.completeExceptionally(
                CameraControl.OperationCanceledException(
                    "Cancelled by another setExposureCompensationIndex()"
                )
            )
            updateSignal = null
        }

        updateListener?.let {
            comboRequestListener.removeListener(it)
            updateListener = null
        }
    }
}