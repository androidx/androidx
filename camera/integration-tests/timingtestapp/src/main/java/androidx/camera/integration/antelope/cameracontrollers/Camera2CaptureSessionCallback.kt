/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope.cameracontrollers

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.TestConfig

/**
 * Callbacks that track an image capture session, including progress of auto-focus and
 * auto-exposure routines.
 *
 * In general these callbacks encompass the intermediate states of the camera after a preview stream
 * is running but before an actual image capture is performed.
 */
class Camera2CaptureSessionCallback(
    internal val activity: MainActivity,
    internal var params: CameraParams,
    internal var testConfig: TestConfig
) : CameraCaptureSession.CaptureCallback() {

    override fun onCaptureSequenceCompleted(
        session: CameraCaptureSession,
        sequenceId: Int,
        frameNumber: Long
    ) {
        MainActivity.logd("Camera2CaptureSessionCallback : Capture sequence COMPLETED")
        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber)
    }

    override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
        MainActivity.logd("Camera2CaptureSessionCallback : Capture sequence ABORTED")
        super.onCaptureSequenceAborted(session, sequenceId)
    }

    override fun onCaptureFailed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        failure: CaptureFailure
    ) {
        MainActivity.logd(
            "Camera2CaptureSessionCallback : Capture sequence FAILED - " +
                failure.reason
        )

        if (!params.isOpen) {
            return
        }

        // There has been a device failure, a restart might help
        closePreviewAndCamera(activity, params, testConfig)
        camera2OpenCamera(activity, params, testConfig)
    }

    private fun process(result: CaptureResult) {
        // MainActivity.logd("Camera2CaptureSessionCallback in process")
        if (!params.isOpen) {
            return
        }

        when (params.state) {
            // Preview is running normally. Nothing to do
            CameraState.PREVIEW_RUNNING -> {
                // MainActivity.logd("Camera2CaptureSessionCallback : PREVIEW_RUNNING
            }

            // We are waiting for AF and AE to converge, check if this has happened
            CameraState.WAITING_FOCUS_LOCK -> {
                val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                MainActivity.logd(
                    "Camera2CaptureSessionCallback: STATE_WAITING_LOCK, afstate == " +
                        afState + ", frame number: " + result.frameNumber
                )

                when (afState) {
                    null -> {
                        MainActivity.logd(
                            "Camera2CaptureSessionCallback: STATE_WAITING_LOCK, " +
                                "afState == null, Calling captureStillPicture!"
                        )
                        params.state = CameraState.IMAGE_REQUESTED
                        captureStillPicture(activity, params, testConfig)
                    }

                    // Waiting for a focus lock but the AF mechanism is inactive. Hopefully after
                    // waiting a few frames it will start up
                    CaptureResult.CONTROL_AF_STATE_INACTIVE -> {
                        // CONTROL_AF_STATE_INACTIVE should be a short-lived state (2-3 frames). On
                        // some devices this can be longer or get stuck indefinitely. If AF has not
                        // started after 50 frames, just run the capture.
                        if (params.autoFocusStuckCounter++ > 50) {
                            MainActivity.logd(
                                "Camera2CaptureSessionCallback : " +
                                    "STATE_WAITING_LOCK, AF is stuck! Calling captureStillPicture!"
                            )
                            params.state = CameraState.IMAGE_REQUESTED
                            captureStillPicture(activity, params, testConfig)
                        }
                    }

                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> {
                        // AF is locked, check AE. Note CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
                        ) {
                            MainActivity.logd(
                                "Camera2CaptureSessionCallback : " +
                                    "STATE_WAITING_LOCK, AF and AE converged! " +
                                    "Calling captureStillPicture!"
                            )
                            params.state = CameraState.IMAGE_REQUESTED
                            captureStillPicture(activity, params, testConfig)
                        } else {
                            // AF is locked but not AE
                            runPrecaptureSequence(params)
                        }
                    }

                    CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                    CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED,
                    CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN -> {
                        Unit // no-op, keep waiting for lock
                    }
                }
            }

            // Waiting on AE metering
            CameraState.WAITING_EXPOSURE_LOCK -> {
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)

                // No aeState on this device, just do the capture
                if (aeState == null) {
                    MainActivity.logd(
                        "Camera2CaptureSessionCallback : STATE_WAITING_PRECAPTURE, " +
                            "aeState == null, Calling captureStillPicture!"
                    )
                    params.state = CameraState.IMAGE_REQUESTED
                    captureStillPicture(activity, params, testConfig)
                } else when (aeState) {
                    // Still metering, keep waiting
                    CaptureResult.CONTROL_AE_STATE_PRECAPTURE,
                    CaptureResult.CONTROL_AE_STATE_SEARCHING
                    -> Unit // no-op

                    // AE converged, double check AF is good
                    CaptureResult.CONTROL_AE_STATE_CONVERGED,
                    CaptureResult.CONTROL_AE_STATE_LOCKED
                    -> params.state = CameraState.WAITING_FOCUS_LOCK

                    // If we need a flash, begin the capture
                    // If AE is INACTIVE, it's in an unusual state (AF locked but AE starting up),
                    // just do the capture to avoid getting stuck.
                    CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                    CaptureResult.CONTROL_AE_STATE_INACTIVE -> {
                        MainActivity.logd(
                            "Camera2CaptureSessionCallback : " +
                                "STATE_WAITING_PRECAPTURE, aeState: " + aeState +
                                ", AE stuck or needs flash, calling captureStillPicture!"
                        )
                        params.state = CameraState.IMAGE_REQUESTED
                        captureStillPicture(activity, params, testConfig)
                    }
                }
            }

            else -> {}
        }
    }

    /** Unused but retained here as it can be useful for debugging  */
    override fun onCaptureStarted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long
    ) {
        // MainActivity.logd("Camera2CaptureSessionCallback captureCallback: Capture Started.")
        super.onCaptureStarted(session, request, timestamp, frameNumber)
    }

    /** Both onCaptureProgressed and onCaptureComplete call through to the processing function */
    override fun onCaptureProgressed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        partialResult: CaptureResult
    ) {
        // MainActivity.logd("Camera2CaptureSessionCallback captureCallback: onCaptureProgressed, " +
        // "partial result frame number: " + partialResult.frameNumber)
        process(partialResult)
    }

    /** Both onCaptureProgressed and onCaptureComplete call through to the processing function */
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        // MainActivity.logd("Camera2CaptureSessionCallback captureCallback: onCaptureCompleted." +
        // " Total result frame number: " + result.frameNumber)
        process(result)
    }
}
