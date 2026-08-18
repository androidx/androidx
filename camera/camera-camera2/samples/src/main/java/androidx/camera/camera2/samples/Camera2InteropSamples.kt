/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.samples

import android.graphics.ColorSpace
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Range
import androidx.annotation.RequiresApi
import androidx.annotation.Sampled
import androidx.camera.camera2.interop.applyCamera2Interop
import androidx.camera.camera2.interop.applyCamera2InteropAsync
import androidx.camera.camera2.interop.camera2Interop
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.imageCapture
import androidx.camera.core.preview
import androidx.camera.core.sessionConfig

@RequiresApi(Build.VERSION_CODES.P)
@Sampled
fun useCaseBuilderCamera2InteropSample() {
    val preview =
        Preview.Builder()
            .camera2Interop {
                physicalCameraId = "5"
                mirrorMode = OutputConfiguration.MIRROR_MODE_H
            }
            .build()
}

@Sampled
fun imageCaptureBuilderCamera2InteropSample() {
    val imageCapture =
        ImageCapture.Builder()
            .camera2Interop {
                stillCaptureRequestTemplateType = CameraDevice.TEMPLATE_STILL_CAPTURE
                stillCaptureRequest[CaptureRequest.CONTROL_AF_MODE] =
                    CaptureRequest.CONTROL_AF_MODE_OFF
                stillCaptureCallback =
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult,
                        ) {}
                    }
                physicalCameraId = "5"
            }
            .build()
}

@Sampled
fun useCaseDslCamera2InteropSample() {
    val preview = preview {
        camera2Interop {
            physicalCameraId = "0"
            mirrorMode = OutputConfiguration.MIRROR_MODE_H
        }
    }
}

@Sampled
fun imageCaptureDslCamera2InteropSample() {
    val imageCapture = imageCapture {
        camera2Interop {
            stillCaptureRequestTemplateType = CameraDevice.TEMPLATE_PREVIEW
            stillCaptureRequest[CaptureRequest.CONTROL_AF_MODE] = CaptureRequest.CONTROL_AF_MODE_OFF
            stillCaptureCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult,
                    ) {}
                }
            physicalCameraId = "5"
        }
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Sampled
fun sessionConfigBuilderCamera2InteropSample(preview: Preview) {
    val sessionConfig =
        SessionConfig.Builder(preview)
            .camera2Interop {
                colorSpace = ColorSpace.Named.DISPLAY_P3
                sessionParameter[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE] = Range(60, 60)
                captureRequest[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE] = Range(60, 60)
                repeatingCaptureRequestTemplate = CameraDevice.TEMPLATE_PREVIEW
                repeatingCaptureCallback = object : CameraCaptureSession.CaptureCallback() {}
            }
            .build()
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Sampled
fun sessionConfigDslCamera2InteropSample(preview: Preview) {
    val sessionConfig =
        sessionConfig(listOf(preview)) {
            camera2Interop {
                colorSpace = ColorSpace.Named.DISPLAY_P3
                sessionParameter[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE] = Range(60, 60)
                captureRequest[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE] = Range(60, 60)
                repeatingCaptureRequestTemplate = CameraDevice.TEMPLATE_PREVIEW
                repeatingCaptureCallback = object : CameraCaptureSession.CaptureCallback() {}
            }
        }
}

@Sampled
fun applyCamera2InteropAsyncSample(cameraControl: CameraControl) {
    cameraControl.applyCamera2InteropAsync {
        captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
        repeatingCaptureCallback = object : CameraCaptureSession.CaptureCallback() {}
    }
}

@Sampled
suspend fun applyCamera2InteropSample(cameraControl: CameraControl) {
    cameraControl.applyCamera2Interop {
        captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
        repeatingCaptureCallback = object : CameraCaptureSession.CaptureCallback() {}
    }
}
