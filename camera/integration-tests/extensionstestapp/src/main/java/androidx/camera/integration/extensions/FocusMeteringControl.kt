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

package androidx.camera.integration.extensions

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
import android.hardware.camera2.CameraMetadata.CONTROL_AF_TRIGGER_IDLE
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

val EMPTY_RECTANGLES = arrayOfNulls<MeteringRectangle>(0)

private const val TAG = "FocusMeteringControl"
private const val AUTO_FOCUS_TIMEOUT_DURATION_MS = 5000L

/**
 * A class to manage focus-metering related operations. This class will help to monitor whether the
 * state is locked or not and then make the AF-Trigger become to idle or cancel state.
 */
@RequiresApi(31)
class FocusMeteringControl(
    private val startAfTriggerImpl: (Array<MeteringRectangle?>) -> Unit,
    private val cancelAfTriggerImpl: (Int) -> Unit,
) {

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var currentAfState: Int = CaptureResult.CONTROL_AF_STATE_INACTIVE
    @Volatile private var autoFocusTimeoutHandle: ScheduledFuture<*>? = null
    @Volatile private var focusTimeoutCounter: Long = 0
    private val isAutoFocusCompleted = AtomicBoolean(true)

    private val captureCallbackExtensionMode =
        object : CameraExtensionSession.ExtensionCaptureCallback() {
            override fun onCaptureResultAvailable(
                session: CameraExtensionSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                result.get(CaptureResult.CONTROL_AF_STATE)?.let { handleCaptureResultForAf(it) }
            }
        }

    private val captureCallbackNormalMode =
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                result.get(CaptureResult.CONTROL_AF_STATE)?.let { handleCaptureResultForAf(it) }
            }
        }

    private fun handleCaptureResultForAf(afState: Int?) {
        if (isAutoFocusCompleted.get()) {
            return
        }

        var completedNow = false
        if (afState == null) {
            Log.e(TAG, "afState == null")
            // set isAutoFocusCompleted to true when camera does not support AF_AUTO.
            completedNow = isAutoFocusCompleted.compareAndSet(false, true)
        } else if (currentAfState == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN) {
            if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                Log.d(TAG, "afState == CONTROL_AF_STATE_FOCUSED_LOCKED")
                completedNow = isAutoFocusCompleted.compareAndSet(false, true)
            } else if (afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                Log.d(TAG, "afState == CONTROL_AF_STATE_NOT_FOCUSED_LOCKED")
                completedNow = isAutoFocusCompleted.compareAndSet(false, true)
            }
        }

        // Check 3A regions
        if (completedNow) {
            clearAutoFocusTimeoutHandle()
            Log.d(TAG, "cancelAfTrigger: CONTROL_AF_TRIGGER_IDLE")
            cancelAfTriggerImpl.invoke(CONTROL_AF_TRIGGER_IDLE)
        }

        if (currentAfState != afState && afState != null) {
            currentAfState = afState
        }
    }

    fun updateMeteringRectangles(meteringRectangles: Array<MeteringRectangle?>) {
        clearAutoFocusTimeoutHandle()
        isAutoFocusCompleted.set(false)
        val timeoutId: Long = ++focusTimeoutCounter
        autoFocusTimeoutHandle =
            scheduler.schedule(
                {
                    if (
                        timeoutId == focusTimeoutCounter &&
                            isAutoFocusCompleted.compareAndSet(false, true)
                    ) {
                        Log.d(TAG, "cancelAfTrigger: CONTROL_AF_TRIGGER_CANCEL")
                        cancelAfTriggerImpl.invoke(CONTROL_AF_TRIGGER_CANCEL)
                    }
                },
                AUTO_FOCUS_TIMEOUT_DURATION_MS,
                TimeUnit.MILLISECONDS,
            )
        currentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE
        startAfTriggerImpl.invoke(meteringRectangles)
    }

    fun cancelFocusAndMetering() {
        clearAutoFocusTimeoutHandle()
        isAutoFocusCompleted.set(true)
        currentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE
    }

    fun getCaptureCallback(extensionEnabled: Boolean): Any =
        if (extensionEnabled) {
            captureCallbackExtensionMode
        } else {
            captureCallbackNormalMode
        }

    private fun clearAutoFocusTimeoutHandle() {
        autoFocusTimeoutHandle?.let {
            it.cancel(/* mayInterruptIfRunning= */ true)
            autoFocusTimeoutHandle = null
        }
    }
}
