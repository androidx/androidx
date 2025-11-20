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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.InputConfiguration
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraInterop
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.writeParameter
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/**
 * Interface around a [CameraDevice] with minor modifications.
 *
 * This interface has been modified to correct nullness, adjust exceptions, and to return or produce
 * wrapper interfaces instead of the native Camera2 types.
 */
internal interface CameraDeviceWrapper : UnsafeWrapper, AudioRestrictionController.Listener {
    /** @see [CameraDevice.getId] */
    val cameraId: CameraId

    /** @see CameraDevice.createCaptureRequest */
    fun createCaptureRequest(template: RequestTemplate): CaptureRequest.Builder?

    /** @see CameraDevice.createReprocessCaptureRequest */
    fun createReprocessCaptureRequest(inputResult: TotalCaptureResult): CaptureRequest.Builder?

    /** @see CameraDevice.createCaptureSession */
    fun createCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean

    /** @see CameraDevice.createReprocessableCaptureSession */
    fun createReprocessableCaptureSession(
        input: InputConfiguration,
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean

    /** @see CameraDevice.createConstrainedHighSpeedCaptureSession */
    fun createConstrainedHighSpeedCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean

    /** @see CameraDevice.createCaptureSessionByOutputConfigurations */
    @RequiresApi(24)
    fun createCaptureSessionByOutputConfigurations(
        outputConfigurations: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean

    /** @see CameraDevice.createReprocessableCaptureSessionByConfigurations */
    @RequiresApi(24)
    fun createReprocessableCaptureSessionByConfigurations(
        inputConfig: InputConfigData,
        outputs: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean

    /** @see CameraDevice.createCaptureSession */
    @RequiresApi(28) fun createCaptureSession(config: SessionConfigData): Boolean

    /** @see CameraDevice.createExtensionSession */
    @RequiresApi(31) fun createExtensionSession(config: ExtensionSessionConfigData): Boolean

    /** Invoked when the [CameraDevice] is being closed */
    fun onDeviceClosing()

    /** Invoked when the [CameraDevice] has been closed */
    fun onDeviceClosed()

    /** @see CameraDevice.getCameraAudioRestriction */
    @RequiresApi(30) fun getCameraAudioRestriction(): AudioRestrictionMode
}

internal fun CameraDevice?.closeWithTrace() {
    this?.let {
        Log.info { "Closing Camera ${it.id}" }
        Debug.instrument("CXCP#CameraDevice-${it.id}#close") {
            try {
                it.close()
            } catch (e: NullPointerException) {
                // Certain vendors add buggy modifications to CameraDevice.close() such that it can
                // throw NPEs during the call. See b/443330486.
                Log.warn(e) { "NPE encountered during CameraDevice.close()" }
            }
        }
    }
}

internal class AndroidCameraDevice(
    private val cameraMetadata: CameraMetadata,
    private val cameraDevice: CameraDevice,
    override val cameraId: CameraId,
    private val cameraErrorListener: CameraErrorListener,
    private val interopCaptureSessionListener: CameraInterop.CaptureSessionListener? = null,
    private val threads: Threads,
) : CameraDeviceWrapper {
    private val closed = atomic(false)
    private val _lastStateCallback = atomic<SessionStateCallback?>(null)

    override fun createCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        val (success, previousStateCallback) = checkAndSetStateCallback(stateCallback)
        if (!success) return false
        previousStateCallback?.onSessionDisconnectedWithTrace()
        val result =
            instrumentAndCatch("createCaptureSession") {
                // This function was deprecated in Android Q, but is required for some
                // configurations when running on older versions of the OS.
                @Suppress("deprecation")
                cameraDevice.createCaptureSession(
                    outputs,
                    AndroidCaptureSessionStateCallback(
                        this,
                        stateCallback,
                        previousStateCallback,
                        cameraErrorListener,
                        interopCaptureSessionListener,
                        threads.camera2Handler,
                    ),
                    threads.camera2Handler,
                )
            }
        if (result == null) {
            // CameraCaptureSession.StateCallback.onConfigureFailed isn't called in certain
            // situations, such as when the camera is closed, or when it encounters an error. As
            // such, we need to make sure we finalize the previous session too.
            Log.warn {
                "Failed to create capture session from $cameraDevice. Finalizing previous session"
            }
            previousStateCallback?.onSessionFinalizedWithTrace()
        }
        return result != null
    }

    @RequiresApi(31)
    override fun createExtensionSession(config: ExtensionSessionConfigData): Boolean {
        val stateCallback = config.extensionStateCallback
        checkNotNull(stateCallback) {
            "extensionStateCallback must be set to create Extension session"
        }
        checkNotNull(config.extensionMode) {
            "extensionMode must be set to create Extension session"
        }
        val (success, previousStateCallback) = checkAndSetStateCallback(stateCallback)
        if (!success) return false
        previousStateCallback?.onSessionDisconnectedWithTrace()
        val result =
            instrumentAndCatch("createExtensionSession") {
                val sessionConfig =
                    Api31Compat.newExtensionSessionConfiguration(
                        config.extensionMode,
                        config.outputConfigurations.map { it.unwrapAs(OutputConfiguration::class) },
                        config.executor,
                        AndroidExtensionSessionStateCallback(
                            this,
                            stateCallback,
                            previousStateCallback,
                            cameraErrorListener,
                            interopCaptureSessionListener,
                            config.executor,
                        ),
                    )

                if (
                    config.postviewOutputConfiguration != null &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                ) {
                    val postviewOutput =
                        config.postviewOutputConfiguration.unwrapAs(OutputConfiguration::class)
                    checkNotNull(postviewOutput) { "Failed to unwrap Postview OutputConfiguration" }
                    Api34Compat.setPostviewOutputConfiguration(sessionConfig, postviewOutput)
                }

                Api31Compat.createExtensionCaptureSession(cameraDevice, sessionConfig)
            }
        if (result == null) {
            // CameraCaptureSession.StateCallback.onConfigureFailed isn't called in certain
            // situations, such as when the camera is closed, or when it encounters an error. As
            // such, we need to make sure we finalize the previous session too.
            Log.warn {
                "Failed to create extension session from $cameraDevice. Finalizing previous session"
            }
            previousStateCallback?.onSessionFinalizedWithTrace()
        }
        return result != null
    }

    @Suppress("deprecation")
    override fun createReprocessableCaptureSession(
        input: InputConfiguration,
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        val (success, previousStateCallback) = checkAndSetStateCallback(stateCallback)
        if (!success) return false
        previousStateCallback?.onSessionDisconnectedWithTrace()
        val result =
            instrumentAndCatch("createReprocessableCaptureSession") {
                // This function was deprecated in Android Q, but is required for some
                // configurations when running on older versions of the OS.
                cameraDevice.createReprocessableCaptureSession(
                    input,
                    outputs,
                    AndroidCaptureSessionStateCallback(
                        this,
                        stateCallback,
                        previousStateCallback,
                        cameraErrorListener,
                        interopCaptureSessionListener,
                        threads.camera2Handler,
                    ),
                    threads.camera2Handler,
                )
            }
        if (result == null) {
            // CameraCaptureSession.StateCallback.onConfigureFailed isn't called in certain
            // situations, such as when the camera is closed, or when it encounters an error. As
            // such, we need to make sure we finalize the previous session too.
            Log.warn {
                "Failed to create reprocess session from $cameraDevice. Finalizing previous session"
            }
            previousStateCallback?.onSessionFinalizedWithTrace()
        }
        return result != null
    }

    @Suppress("deprecation")
    override fun createConstrainedHighSpeedCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        val (success, previousStateCallback) = checkAndSetStateCallback(stateCallback)
        if (!success) return false
        previousStateCallback?.onSessionDisconnectedWithTrace()
        val result =
            instrumentAndCatch("createConstrainedHighSpeedCaptureSession") {
                // This function was deprecated in Android Q, but is required for some
                // configurations
                // when running on older versions of the OS.
                cameraDevice.createConstrainedHighSpeedCaptureSession(
                    outputs,
                    AndroidCaptureSessionStateCallback(
                        this,
                        stateCallback,
                        previousStateCallback,
                        cameraErrorListener,
                        interopCaptureSessionListener,
                        threads.camera2Handler,
                    ),
                    threads.camera2Handler,
                )
            }
        if (result == null) {
            // CameraCaptureSession.StateCallback.onConfigureFailed isn't called in certain
            // situations, such as when the camera is closed, or when it encounters an error. As
            // such, we need to make sure we finalize the previous session too.
            Log.warn {
                "Failed to create capture session from $cameraDevice. Finalizing previous session"
            }
            previousStateCallback?.onSessionFinalizedWithTrace()
        }
        return result != null
    }

    @RequiresApi(24)
    override fun createCaptureSessionByOutputConfigurations(
        outputConfigurations: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        val (success, previousStateCallback) = checkAndSetStateCallback(stateCallback)
        if (!success) return false
        previousStateCallback?.onSessionDisconnectedWithTrace()
        val result =
            instrumentAndCatch("createCaptureSessionByOutputConfigurations") {
                // This function was deprecated in Android Q, but is required for some
                // configurations
                // when running on older versions of the OS.
                Api24Compat.createCaptureSessionByOutputConfigurations(
                    cameraDevice,
                    outputConfigurations.map { it.unwrapAs(OutputConfiguration::class) },
                    AndroidCaptureSessionStateCallback(
                        this,
                        stateCallback,
                        previousStateCallback,
                        cameraErrorListener,
                        interopCaptureSessionListener,
                        threads.camera2Handler,
                    ),
                    threads.camera2Handler,
                )
            }
        if (result == null) {
            // CameraCaptureSession.StateCallback.onConfigureFailed isn't called in certain
            // situations, such as when the camera is closed, or when it encounters an error. As
            // such, we need to make sure we finalize the previous session too.
            Log.warn {
                "Failed to create capture session from $cameraDevice. Finalizing previous session"
            }
            previousStateCallback?.onSessionFinalizedWithTrace()
        }
        return result != null
    }

    @RequiresApi(24)
    override fun createReprocessableCaptureSessionByConfigurations(
        inputConfig: InputConfigData,
        outputs: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        val (success, previousStateCallback) = checkAndSetStateCallback(stateCallback)
        if (!success) return false
        previousStateCallback?.onSessionDisconnectedWithTrace()
        val result =
            instrumentAndCatch("createReprocessableCaptureSessionByConfigurations") {
                // This function was deprecated in Android Q, but is required for some
                // configurations when running on older versions of the OS.
                Api24Compat.createReprocessableCaptureSessionByConfigurations(
                    cameraDevice,
                    InputConfiguration(inputConfig.width, inputConfig.height, inputConfig.format),
                    outputs.map { it.unwrapAs(OutputConfiguration::class) },
                    AndroidCaptureSessionStateCallback(
                        this,
                        stateCallback,
                        previousStateCallback,
                        cameraErrorListener,
                        interopCaptureSessionListener,
                        threads.camera2Handler,
                    ),
                    threads.camera2Handler,
                )
            }
        if (result == null) {
            // CameraCaptureSession.StateCallback.onConfigureFailed isn't called in certain
            // situations, such as when the camera is closed, or when it encounters an error. As
            // such, we need to make sure we finalize the previous session too.
            Log.warn {
                "Failed to create reprocess session from $cameraDevice. Finalizing previous session"
            }
            previousStateCallback?.onSessionFinalizedWithTrace()
        }
        return result != null
    }

    @RequiresApi(28)
    override fun createCaptureSession(config: SessionConfigData): Boolean {
        val (success, previousStateCallback) = checkAndSetStateCallback(config.stateCallback)
        if (!success) return false
        previousStateCallback?.onSessionDisconnectedWithTrace()
        val result =
            instrumentAndCatch("createCaptureSession") {
                val sessionConfig =
                    Api28Compat.newSessionConfiguration(
                        config.sessionType,
                        config.outputConfigurations.map { it.unwrapAs(OutputConfiguration::class) },
                        config.executor,
                        AndroidCaptureSessionStateCallback(
                            this,
                            config.stateCallback,
                            previousStateCallback,
                            cameraErrorListener,
                            interopCaptureSessionListener,
                            threads.camera2Handler,
                        ),
                    )

                if (config.inputConfiguration != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Api28Compat.setInputConfiguration(
                            sessionConfig,
                            Api31Compat.newInputConfiguration(
                                config.inputConfiguration,
                                cameraId.value,
                            ),
                        )
                    } else {
                        Api28Compat.setInputConfiguration(
                            sessionConfig,
                            InputConfiguration(
                                config.inputConfiguration.single().width,
                                config.inputConfiguration.single().height,
                                config.inputConfiguration.single().format,
                            ),
                        )
                    }
                }

                // set color space if using supported API levels and color space is provided
                val sessionColorSpace = config.sessionColorSpace
                if (Build.VERSION.SDK_INT >= 34 && sessionColorSpace != null) {
                    val colorSpaceNamed = sessionColorSpace.toColorSpaceNamed()
                    if (colorSpaceNamed != null) {
                        Api34Compat.setColorSpace(sessionConfig, colorSpaceNamed)
                    } else {
                        // case that sessionColorSpace is UNKNOWN
                        Log.warn {
                            "Provided session color space ${sessionColorSpace.colorSpaceName} is not supported"
                        }
                    }
                } else if (sessionColorSpace != null) {
                    Log.warn {
                        "Failed to set session color space to ${sessionColorSpace.colorSpaceName}, at least API level 34 is required"
                    }
                }

                val requestBuilder =
                    Debug.trace("createCaptureRequest") {
                        cameraDevice.createCaptureRequest(config.sessionTemplateId)
                    }

                // This compares and sets ONLY the session keys for this camera. Setting parameters
                // that are not listed in availableSessionKeys can cause an unusual amount of extra
                // latency.
                val sessionKeyNames = cameraMetadata.sessionKeys.map { it.name }

                // Iterate template parameters and CHECK BY NAME, as there have been cases where
                // equality checks did not pass.
                for ((key, value) in config.sessionParameters) {
                    if (key !is CaptureRequest.Key<*>) continue
                    if (sessionKeyNames.contains(key.name)) {
                        requestBuilder.writeParameter(key, value)
                    }
                }
                Api28Compat.setSessionParameters(sessionConfig, requestBuilder.build())
                Debug.trace("Api28Compat.createCaptureSession") {
                    Api28Compat.createCaptureSession(cameraDevice, sessionConfig)
                }
            }
        if (result == null) {
            // CameraCaptureSession.StateCallback.onConfigureFailed isn't called in certain
            // situations, such as when the camera is closed, or when it encounters an error. As
            // such, we need to make sure we finalize the previous session too.
            Log.warn {
                "Failed to create capture session from $cameraDevice. Finalizing previous session"
            }
            previousStateCallback?.onSessionFinalizedWithTrace()
        }
        return result != null
    }

    override fun createCaptureRequest(template: RequestTemplate): CaptureRequest.Builder? =
        instrumentAndCatch("createCaptureRequest") {
            cameraDevice.createCaptureRequest(template.value)
        }

    override fun createReprocessCaptureRequest(
        inputResult: TotalCaptureResult
    ): CaptureRequest.Builder? =
        instrumentAndCatch("createReprocessCaptureRequest") {
            cameraDevice.createReprocessCaptureRequest(inputResult)
        }

    @RequiresApi(30)
    override fun getCameraAudioRestriction(): AudioRestrictionMode =
        Debug.trace("getCameraAudioRestriction") {
            AudioRestrictionMode(Api30Compat.getCameraAudioRestriction(cameraDevice))
        }

    @RequiresApi(30)
    override fun onCameraAudioRestrictionUpdated(mode: AudioRestrictionMode) {
        Debug.trace("setCameraAudioRestriction") {
            catchAndReportCameraExceptions(cameraId, cameraErrorListener) {
                Api30Compat.setCameraAudioRestriction(cameraDevice, mode.value)
            }
        }
    }

    override fun onDeviceClosing() {
        if (closed.compareAndSet(expect = false, update = true)) {
            val lastStateCallback = _lastStateCallback.value
            lastStateCallback?.onSessionDisconnectedWithTrace()
        }
    }

    override fun onDeviceClosed() {
        check(closed.value)
        val lastStateCallback = _lastStateCallback.getAndSet(null)
        lastStateCallback?.onSessionFinalizedWithTrace()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraDevice::class -> cameraDevice as T
            else -> null
        }

    override fun toString(): String = "AndroidCameraDevice(camera=$cameraId)"

    /** Utility function to trace, measure, and suppress exceptions for expensive method calls. */
    @Throws(ObjectUnavailableException::class)
    private inline fun <T> instrumentAndCatch(fnName: String, crossinline block: () -> T) =
        Debug.instrument("CXCP#$fnName-${cameraId.value}") {
            catchAndReportCameraExceptions(cameraId, cameraErrorListener, block)
        }

    private fun checkAndSetStateCallback(
        stateCallback: SessionStateCallback
    ): Pair<Boolean, SessionStateCallback?> {
        if (closed.value) {
            stateCallback.onSessionFinalizedWithTrace()
            return Pair(false, null)
        }
        return Pair(true, _lastStateCallback.getAndSet(stateCallback))
    }

    private fun SessionStateCallback.onSessionDisconnectedWithTrace() {
        Debug.trace("${this@AndroidCameraDevice}#onSessionDisconnected") { onSessionDisconnected() }
    }

    private fun SessionStateCallback.onSessionFinalizedWithTrace() {
        Debug.trace("${this@AndroidCameraDevice}#onSessionFinalized") { onSessionFinalized() }
    }
}

/**
 * VirtualAndroidCameraDevice creates a simple wrapper around a [AndroidCameraDevice], augmenting it
 * by enabling it to reject further capture session/request calls when it is "disconnected'.
 */
internal class VirtualAndroidCameraDevice(internal val androidCameraDevice: AndroidCameraDevice) :
    CameraDeviceWrapper {
    private val lock = Any()

    @GuardedBy("lock") private var disconnected = false

    override val cameraId: CameraId
        get() = androidCameraDevice.cameraId

    override fun createCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ) =
        synchronized(lock) {
            if (disconnected) {
                Log.warn { "createCaptureSession failed: Virtual device disconnected" }
                stateCallback.onSessionFinalized()
                false
            } else {
                androidCameraDevice.createCaptureSession(outputs, stateCallback)
            }
        }

    override fun createReprocessableCaptureSession(
        input: InputConfiguration,
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ) =
        synchronized(lock) {
            if (disconnected) {
                Log.warn { "createReprocessableCaptureSession failed: Virtual device disconnected" }
                stateCallback.onSessionFinalized()
                false
            } else {
                androidCameraDevice.createReprocessableCaptureSession(input, outputs, stateCallback)
            }
        }

    override fun createConstrainedHighSpeedCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ) =
        synchronized(lock) {
            if (disconnected) {
                Log.warn {
                    "createConstrainedHighSpeedCaptureSession failed: Virtual device disconnected"
                }
                stateCallback.onSessionFinalized()
                false
            } else {
                androidCameraDevice.createConstrainedHighSpeedCaptureSession(outputs, stateCallback)
            }
        }

    @RequiresApi(24)
    override fun createCaptureSessionByOutputConfigurations(
        outputConfigurations: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ) =
        synchronized(lock) {
            if (disconnected) {
                Log.warn {
                    "createCaptureSessionByOutputConfigurations failed: Virtual device disconnected"
                }
                stateCallback.onSessionFinalized()
                false
            } else {
                androidCameraDevice.createCaptureSessionByOutputConfigurations(
                    outputConfigurations,
                    stateCallback,
                )
            }
        }

    @RequiresApi(24)
    override fun createReprocessableCaptureSessionByConfigurations(
        inputConfig: InputConfigData,
        outputs: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ) =
        synchronized(lock) {
            if (disconnected) {
                Log.warn {
                    "createReprocessableCaptureSessionByConfigurations failed: " +
                        "Virtual device disconnected"
                }
                stateCallback.onSessionFinalized()
                false
            } else {
                androidCameraDevice.createReprocessableCaptureSessionByConfigurations(
                    inputConfig,
                    outputs,
                    stateCallback,
                )
            }
        }

    @RequiresApi(31)
    override fun createExtensionSession(config: ExtensionSessionConfigData) =
        synchronized(lock) {
            if (disconnected) {
                Log.warn { "createExtensionSession failed: Virtual device disconnected" }
                config.extensionStateCallback!!.onSessionFinalized()
                false
            } else {
                androidCameraDevice.createExtensionSession(config)
            }
        }

    @RequiresApi(28)
    override fun createCaptureSession(config: SessionConfigData) =
        synchronized(lock) {
            if (disconnected) {
                Log.warn { "createCaptureSession failed: Virtual device disconnected" }
                config.stateCallback.onSessionFinalized()
                false
            } else {
                androidCameraDevice.createCaptureSession(config)
            }
        }

    override fun createCaptureRequest(template: RequestTemplate) =
        synchronized(lock) {
            if (disconnected) {
                Log.warn { "createCaptureRequest failed: Virtual device disconnected" }
                null
            } else {
                androidCameraDevice.createCaptureRequest(template)
            }
        }

    override fun createReprocessCaptureRequest(inputResult: TotalCaptureResult) =
        synchronized(lock) {
            if (disconnected) {
                Log.warn { "createReprocessCaptureRequest failed: Virtual device disconnected" }
                null
            } else {
                androidCameraDevice.createReprocessCaptureRequest(inputResult)
            }
        }

    override fun onDeviceClosing() = androidCameraDevice.onDeviceClosing()

    override fun onDeviceClosed() = androidCameraDevice.onDeviceClosed()

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = androidCameraDevice.unwrapAs(type)

    internal fun disconnect() = synchronized(lock) { disconnected = true }

    @RequiresApi(30)
    override fun getCameraAudioRestriction(): AudioRestrictionMode {
        return androidCameraDevice.getCameraAudioRestriction()
    }

    @RequiresApi(30)
    override fun onCameraAudioRestrictionUpdated(mode: AudioRestrictionMode) {
        androidCameraDevice.onCameraAudioRestrictionUpdated(mode)
    }
}
