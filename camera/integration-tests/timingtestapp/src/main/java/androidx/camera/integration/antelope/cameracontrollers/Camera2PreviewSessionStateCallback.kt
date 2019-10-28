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

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.view.Surface
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.integration.antelope.FocusMode
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.PrefHelper
import androidx.camera.integration.antelope.TestConfig
import androidx.camera.integration.antelope.TestType
import androidx.camera.integration.antelope.setAutoFlash
import java.util.Arrays

/**
 * Callbacks that track the state of a preview capture session.
 */
class Camera2PreviewSessionStateCallback(
    internal val activity: MainActivity,
    internal val params: CameraParams,
    internal val testConfig: TestConfig
) : CameraCaptureSession.StateCallback() {

    /**
     * Preview session is open and frames are coming through. If the test is preview only, record
     * results and close the camera, if a switch or image capture test, proceed to the next step.
     *
     */
    override fun onActive(session: CameraCaptureSession) {
        if (!params.isOpen || params.state == CameraState.IMAGE_REQUESTED) {
            return
        }

        params.timer.previewEnd = System.currentTimeMillis()
        params.isPreviewing = true

        when (testConfig.currentRunningTest) {
            TestType.PREVIEW -> {
                testConfig.testFinished = true
                closePreviewAndCamera(activity, params, testConfig)
            }

            TestType.SWITCH_CAMERA, TestType.MULTI_SWITCH -> {
                if (testConfig.switchTestCurrentCamera == testConfig.switchTestCameras.get(0)) {
                    if (testConfig.testFinished) {
                        params.timer.switchToFirstEnd = System.currentTimeMillis()
                        Thread.sleep(PrefHelper.getPreviewBuffer(activity)) // Let preview run
                        closePreviewAndCamera(activity, params, testConfig)
                    } else {
                        Thread.sleep(PrefHelper.getPreviewBuffer(activity)) // Let preview run
                        params.timer.switchToSecondStart = System.currentTimeMillis()
                        closePreviewAndCamera(activity, params, testConfig)
                    }
                } else {
                    params.timer.switchToSecondEnd = System.currentTimeMillis()
                    Thread.sleep(PrefHelper.getPreviewBuffer(activity)) // Let preview run
                    params.timer.switchToFirstStart = System.currentTimeMillis()
                    closePreviewAndCamera(activity, params, testConfig)
                }
            }

            else -> {
                initializeStillCapture(activity, params, testConfig)
            }
        }

        super.onActive(session)
    }

    /**
     * Preview session has been configured, set up preview parameters and request that the preview
     * capture begin.
     */
    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
        if (!params.isOpen) {
            return
        }

        MainActivity.logd("In onConfigured: CaptureSession configured!")

        try {
            when (testConfig.focusMode) {
                FocusMode.AUTO -> {
                    params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO)
                }
                FocusMode.CONTINUOUS -> {
                    params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                }
                FocusMode.FIXED -> {
                    params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO)
                }
            }

            // Enable flash automatically when necessary.
            setAutoFlash(params, params.captureRequestBuilder)

            params.camera2CaptureSession = cameraCaptureSession
            params.state = CameraState.PREVIEW_RUNNING

            // Request that the camera preview begins
            cameraCaptureSession.setRepeatingRequest(params.captureRequestBuilder?.build()!!,
                params.camera2CaptureSessionCallback, params.backgroundHandler)
        } catch (e: CameraAccessException) {
            MainActivity.logd("Create Capture Session error: " + params.id)
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            MainActivity.logd("createCameraPreviewSession onConfigured, IllegalStateException," +
                " aborting: " + e)
        }
    }

    /**
     * Configuration of the preview stream failed, try again.
     */
    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
        if (!params.isOpen) {
            return
        }

        MainActivity.logd("Camera preview initialization failed. Trying again")
        createCameraPreviewSession(activity, params, testConfig)
    }

    /**
     * Preview session has been closed. Record close timing and proceed to close camera.
     */
    override fun onClosed(session: CameraCaptureSession) {
        params.timer.previewCloseEnd = System.currentTimeMillis()
        params.isPreviewing = false

        /** If legacy HAL, create a dummy preview session before closing the device */
        if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.M) && (params.isLegacy)) {
            closeDeviceWithWorkaround(params)
        } else {
            params.timer.cameraCloseStart = System.currentTimeMillis()
            params.device?.close()
        }
        super.onClosed(session)
    }

    /**
     * Before closing the device, create a dummy preview session to workaround a bug where an
     * ImageReader can stay active after a device close. For example on Pixel 1 with Android 8.0.0
     * during a SWITCH_CAMERA test.
     */
    fun closeDeviceWithWorkaround(params: CameraParams) {
        val surfaceTexture = SurfaceTexture(0)
        surfaceTexture.setDefaultBufferSize(640, 480)

        val surface = Surface(surfaceTexture)
        try {
            params.device?.createCaptureSession(
                Arrays.asList(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.close()
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        session.close()
                    }
                    /** Dummy preview session created and closed, now proceed to close camera */
                    override fun onClosed(session: CameraCaptureSession) {
                        surfaceTexture.release()
                        params.timer.cameraCloseStart = System.currentTimeMillis()
                        params.device?.close()
                    }
                }, null)
        } catch (e: CameraAccessException) {
            MainActivity.logd("createCameraPreviewSession CameraAccessException: " + e.message)
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            MainActivity.logd("createCameraPreviewSession IllegalStateException: " + e.message)
            e.printStackTrace()
        }
    }
}
