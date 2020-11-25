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

package androidx.camera.camera2.pipe.impl

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.FRAME_NUMBER_INVALID
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.Status3A
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel

/**
 * This class implements the 3A methods of [CameraGraphSessionImpl].
 */
class Controller3A(
    private val graphProcessor: GraphProcessor,
    private val graphState3A: GraphState3A,
    private val graphListener3A: Listener3A
) {
    // Keep track of the result associated with latest call to update3A. If update3A is called again
    // and the current result is not complete, we will cancel the current result.
    @GuardedBy("this")
    private var lastUpdate3AResult: Deferred<Result3A>? = null

    fun update3A(
        aeMode: AeMode? = null,
        afMode: AfMode? = null,
        awbMode: AwbMode? = null,
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null
    ): Deferred<Result3A> {
        // Add the listener to a global pool of 3A listeners to monitor the state change to the
        // desired one.
        val listener = createListenerFor3AParams(aeMode, afMode, awbMode)
        graphListener3A.addListener(listener)

        // Update the 3A state of the graph. This will make sure then when GraphProcessor builds
        // the next request it will apply the 3A parameters corresponding to the updated 3A state
        // to the request.
        graphState3A.update(aeMode, afMode, awbMode, aeRegions, afRegions, awbRegions)
        // Try submitting a new repeating request with the 3A parameters corresponding to the new
        // 3A state and corresponding listeners.
        graphProcessor.invalidate()

        val result = listener.getDeferredResult()
        synchronized(this) {
            lastUpdate3AResult?.cancel("A newer update3A call initiated.")
            lastUpdate3AResult = result
        }
        return result
    }

    suspend fun submit3A(
        aeMode: AeMode? = null,
        afMode: AfMode? = null,
        awbMode: AwbMode? = null,
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null
    ): Deferred<Result3A> {
        // Add the listener to a global pool of 3A listeners to monitor the state change to the
        // desired one.
        val listener = createListenerFor3AParams(aeMode, afMode, awbMode)
        graphListener3A.addListener(listener)

        val extra3AParams = mutableMapOf<CaptureRequest.Key<*>, Any>()
        aeMode?.let { extra3AParams.put(CaptureRequest.CONTROL_AE_MODE, it.value) }
        afMode?.let { extra3AParams.put(CaptureRequest.CONTROL_AF_MODE, it.value) }
        awbMode?.let { extra3AParams.put(CaptureRequest.CONTROL_AWB_MODE, it.value) }
        aeRegions?.let {
            extra3AParams.put(
                CaptureRequest.CONTROL_AE_REGIONS,
                it.toTypedArray()
            )
        }
        afRegions?.let {
            extra3AParams.put(
                CaptureRequest.CONTROL_AF_REGIONS,
                it.toTypedArray()
            )
        }
        awbRegions?.let {
            extra3AParams.put(
                CaptureRequest.CONTROL_AWB_REGIONS,
                it.toTypedArray()
            )
        }

        if (!graphProcessor.submit(extra3AParams)) {
            graphListener3A.removeListener(listener)
            return CompletableDeferred(
                Result3A(FRAME_NUMBER_INVALID, Status3A.SUBMIT_FAILED)
            )
        }
        return listener.getDeferredResult()
    }

    // We create a map for the 3A modes and the desired values and leave out the keys
    // corresponding to the metering regions. The reason being the camera framework can chose to
    // crop or modify the metering regions as per its constraints. So when we receive at least
    // one capture result corresponding to this request it is assumed that the framework has
    // applied the desired metering regions to the best of its judgement, and we don't need an
    // exact match between the metering regions sent in the capture request and the metering
    // regions received from the camera device.
    private fun createListenerFor3AParams(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?
    ): Result3AStateListenerImpl {
        val resultModesMap = mutableMapOf<CaptureResult.Key<*>, List<Any>>()
        aeMode?.let { resultModesMap.put(CaptureResult.CONTROL_AE_MODE, listOf(it.value)) }
        afMode?.let { resultModesMap.put(CaptureResult.CONTROL_AF_MODE, listOf(it.value)) }
        awbMode?.let { resultModesMap.put(CaptureResult.CONTROL_AWB_MODE, listOf(it.value)) }
        return Result3AStateListenerImpl(resultModesMap.toMap())
    }
}