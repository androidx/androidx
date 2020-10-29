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

package androidx.camera.camera2.pipe.wrapper

import android.annotation.SuppressLint
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.InputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.impl.Debug
import androidx.camera.camera2.pipe.impl.Log
import androidx.camera.camera2.pipe.impl.Timestamps
import androidx.camera.camera2.pipe.impl.Timestamps.formatMs
import androidx.camera.camera2.pipe.writeParameter
import kotlin.jvm.Throws

/** Interface around a [CameraDevice] with minor modifications.
 *
 * This interface has been modified to correct nullness, adjust exceptions, and to return or produce
 * wrapper interfaces instead of the native Camera2 types.
 */
interface CameraDeviceWrapper : UnsafeWrapper<CameraDevice> {
    /** @see [CameraDevice.getId] */
    val cameraId: CameraId

    /** @see CameraDevice.createCaptureRequest */
    @Throws(ObjectUnavailableException::class)
    fun createCaptureRequest(template: RequestTemplate): CaptureRequest.Builder

    /** @see CameraDevice.createReprocessCaptureRequest */
    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(ObjectUnavailableException::class)
    fun createReprocessCaptureRequest(inputResult: TotalCaptureResult): CaptureRequest.Builder

    /** @see CameraDevice.createCaptureSession */
    @Throws(ObjectUnavailableException::class)
    fun createCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    )

    /** @see CameraDevice.createReprocessableCaptureSession */
    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(ObjectUnavailableException::class)
    fun createReprocessableCaptureSession(
        input: InputConfiguration,
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    )

    /** @see CameraDevice.createConstrainedHighSpeedCaptureSession */
    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(ObjectUnavailableException::class)
    fun createConstrainedHighSpeedCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    )

    /** @see CameraDevice.createCaptureSessionByOutputConfigurations */
    @RequiresApi(Build.VERSION_CODES.N)
    @Throws(ObjectUnavailableException::class)
    fun createCaptureSessionByOutputConfigurations(
        outputConfigurations: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    )

    /** @see CameraDevice.createReprocessableCaptureSessionByConfigurations */
    @RequiresApi(Build.VERSION_CODES.N)
    @Throws(ObjectUnavailableException::class)
    fun createReprocessableCaptureSessionByConfigurations(
        inputConfig: InputConfigData,
        outputs: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    )

    /** @see CameraDevice.createCaptureSession */
    @RequiresApi(Build.VERSION_CODES.P)
    @Throws(ObjectUnavailableException::class)
    fun createCaptureSession(config: SessionConfigData)
}

fun CameraDeviceWrapper?.closeWithTrace() {
    this?.unwrap().closeWithTrace()
}

fun CameraDevice?.closeWithTrace() {
    this?.let {
        val start = Timestamps.now()
        Log.info { "Closing Camera ${it.id}" }
        Debug.trace("CameraDevice-${it.id}#close") {
            it.close()
        }
        val duration = Timestamps.now() - start
        Log.info { "Closed Camera ${it.id} in ${duration.formatMs()}" }
    }
}

class AndroidCameraDevice(
    private val cameraMetadata: CameraMetadata,
    private val cameraDevice: CameraDevice,
    override val cameraId: CameraId
) : CameraDeviceWrapper, UnsafeWrapper<CameraDevice> {

    override fun createCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    ) = rethrowCamera2Exceptions {

        // This function was deprecated in Android Q, but is required for some configurations when
        // running on older versions of the OS.
        @Suppress("deprecation")
        cameraDevice.createCaptureSession(
            outputs,
            AndroidCaptureSessionStateCallback(this, stateCallback),
            handler
        )
    }

    @RequiresApi(23)
    override fun createReprocessableCaptureSession(
        input: InputConfiguration,
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    ) = rethrowCamera2Exceptions {

        // This function was deprecated in Android Q, but is required for some configurations when
        // running on older versions of the OS.
        @Suppress("deprecation")
        cameraDevice.createReprocessableCaptureSession(
            input,
            outputs,
            AndroidCaptureSessionStateCallback(this, stateCallback),
            handler
        )
    }

    @RequiresApi(23)
    override fun createConstrainedHighSpeedCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    ) = rethrowCamera2Exceptions {

        // This function was deprecated in Android Q, but is required for some configurations when
        // running on older versions of the OS.
        @Suppress("deprecation")
        cameraDevice.createConstrainedHighSpeedCaptureSession(
            outputs,
            AndroidCaptureSessionStateCallback(this, stateCallback),
            handler
        )
    }

    @RequiresApi(24)
    override fun createCaptureSessionByOutputConfigurations(
        outputConfigurations: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    ) = rethrowCamera2Exceptions {

        // This function was deprecated in Android Q, but is required for some configurations when
        // running on older versions of the OS.
        @Suppress("deprecation")
        cameraDevice.createCaptureSessionByOutputConfigurations(
            outputConfigurations.map { it.unwrap() },
            AndroidCaptureSessionStateCallback(this, stateCallback),
            handler
        )
    }

    @RequiresApi(24)
    override fun createReprocessableCaptureSessionByConfigurations(
        inputConfig: InputConfigData,
        outputs: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
        handler: Handler?
    ) = rethrowCamera2Exceptions {

        // This function was deprecated in Android Q, but is required for some configurations when
        // running on older versions of the OS.
        @Suppress("deprecation")
        cameraDevice.createReprocessableCaptureSessionByConfigurations(
            InputConfiguration(inputConfig.width, inputConfig.height, inputConfig.format),
            outputs.map { it.unwrap() },
            AndroidCaptureSessionStateCallback(this, stateCallback),
            handler
        )
    }

    @RequiresApi(28)
    @SuppressLint("UnsafeNewApiCall")
    override fun createCaptureSession(config: SessionConfigData) = rethrowCamera2Exceptions {
        val sessionConfig = SessionConfiguration(
            config.sessionType,
            config.outputConfigurations.map { it.unwrap() },
            config.executor,
            AndroidCaptureSessionStateCallback(this, config.stateCallback)
        )

        if (config.inputConfiguration != null) {
            sessionConfig.inputConfiguration = InputConfiguration(
                config.inputConfiguration.width,
                config.inputConfiguration.height,
                config.inputConfiguration.format
            )
        }

        val requestBuilder = cameraDevice.createCaptureRequest(config.sessionTemplateId)

        // This compares and sets ONLY the session keys for this camera. Setting parameters that are
        // not listed in availableSessionKeys can cause an unusual amount of extra latency.
        val sessionKeyNames = cameraMetadata.sessionKeys.map { it.name }

        // Iterate template parameters and CHECK BY NAME, as there have been cases where equality
        // checks did not pass.
        for ((key, value) in config.sessionParameters) {
            if (key is CaptureRequest.Key<*> && sessionKeyNames.contains(key.name)) {
                requestBuilder.writeParameter(key, value)
            }
        }
        sessionConfig.sessionParameters = requestBuilder.build()

        cameraDevice.createCaptureSession(sessionConfig)
    }

    override fun createCaptureRequest(template: RequestTemplate): CaptureRequest.Builder =
        rethrowCamera2Exceptions {
            cameraDevice.createCaptureRequest(template.value)
        }

    @RequiresApi(23)
    @SuppressLint("UnsafeNewApiCall")
    override fun createReprocessCaptureRequest(
        inputResult: TotalCaptureResult
    ): CaptureRequest.Builder = rethrowCamera2Exceptions {
        cameraDevice.createReprocessCaptureRequest(inputResult)
    }

    override fun unwrap(): CameraDevice? {
        return cameraDevice
    }
}
