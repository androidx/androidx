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

package androidx.camera.camera2.interop

import android.graphics.ColorSpace
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.impl.createCaptureRequestOption
import androidx.camera.camera2.impl.createSessionParameterOption
import androidx.camera.camera2.impl.createStillCaptureRequestOption
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.MutableConfig
import java.util.concurrent.Executor

/**
 * Base interop interfaces, delegates, and callback wrappers for Camera2 integration.
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. If an
 * option configured via interop conflicts with options required by CameraX internally, the option
 * from Camera2Interop will override, which may result in unexpected behavior or interfere with
 * CameraX functionality.
 */

// =========================================================================================
// Base Interop Interfaces
// =========================================================================================

/** Configures Camera2 [android.hardware.camera2.params.OutputConfiguration] options. */
public interface OutputConfigurationInterop {
    /** The physical camera ID. */
    @get:RequiresApi(28) @set:RequiresApi(28) public var physicalCameraId: String?

    /** The stream use case. */
    @get:RequiresApi(33) @set:RequiresApi(33) public var streamUseCase: Long

    /** The mirror mode. */
    @get:RequiresApi(33) @set:RequiresApi(33) public var mirrorMode: Int

    /** The timestamp base. */
    @get:RequiresApi(33) @set:RequiresApi(33) public var timestampBase: Int

    /** The dynamic range profile. */
    @get:RequiresApi(33) @set:RequiresApi(33) public var dynamicRangeProfile: Long

    /** The surface group ID. */
    public var surfaceGroupId: Int

    /**
     * Adds a sensor pixel mode that this output configuration will be used in.
     *
     * @param sensorPixelMode sensor pixel mode to add
     * @see android.hardware.camera2.params.OutputConfiguration.addSensorPixelModeUsed
     */
    @RequiresApi(31) public fun addSensorPixelModeUsed(sensorPixelMode: Int)

    /**
     * The sensor pixel modes that this output configuration will be used in.
     *
     * @see addSensorPixelModeUsed
     */
    @get:RequiresApi(31) @set:RequiresApi(31) public var sensorPixelModesUsed: Set<Int>
}

/** Configures Camera2 [android.hardware.camera2.CameraCaptureSession] options. */
public interface CameraCaptureSessionInterop {
    /**
     * Sets a [CaptureRequest.Key] and value on repeating and one-shot requests.
     *
     * @param key capture request option key
     * @param value option value
     */
    public fun <V> setCaptureRequestOption(key: CaptureRequest.Key<V>, value: V)

    /** Helper target property supporting `captureRequest[key] = value` indexing operator syntax. */
    public val captureRequest: CaptureRequestOptionTarget
        get() = CaptureRequestOptionTarget(this)

    /**
     * Clears a [CaptureRequest.Key] previously set on this target.
     *
     * @param key capture request option key
     */
    public fun clearCaptureRequestOption(key: CaptureRequest.Key<*>)

    /** Clears all [CaptureRequest.Key]s previously set on this target. */
    public fun clearAllCaptureRequestOptions()

    /**
     * The repeating capture request template type (e.g.,
     * [android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW]).
     */
    public var repeatingCaptureRequestTemplate: Int

    /**
     * Sets the callback to receive repeating capture updates with an executor.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Directly invoking state-altering, lifecycle, or request-submitting methods on
     * these raw objects bypasses CameraX pipeline management and causes state desynchronization,
     * stream freezing, or crashes. Do not invoke:
     * - Session lifecycle methods: [android.hardware.camera2.CameraCaptureSession.close],
     *   [android.hardware.camera2.CameraCaptureSession.abortCaptures], or
     *   [android.hardware.camera2.CameraCaptureSession.stopRepeating]
     * - Request submission methods:
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingRequest],
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingBurst],
     *   [android.hardware.camera2.CameraCaptureSession.capture], or
     *   [android.hardware.camera2.CameraCaptureSession.captureBurst]
     * - Surface / output configuration methods:
     *   [android.hardware.camera2.CameraCaptureSession.updateOutputConfiguration],
     *   [android.hardware.camera2.CameraCaptureSession.finalizeOutputConfigurations],
     *   [android.hardware.camera2.CameraCaptureSession.prepare], or
     *   [android.hardware.camera2.CameraCaptureSession.switchToOffline]
     *
     * @param executor executor to run the callback on
     * @param callback repeating capture callback
     */
    public fun setRepeatingCaptureCallback(
        executor: Executor,
        callback: CameraCaptureSession.CaptureCallback,
    )

    /**
     * The callback to receive repeating capture updates on a direct executor.
     *
     * **Warning:** The callback receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Directly invoking state-altering, lifecycle, or request-submitting methods on
     * these raw objects bypasses CameraX pipeline management and causes state desynchronization,
     * stream freezing, or crashes. Do not invoke:
     * - Session lifecycle methods: [android.hardware.camera2.CameraCaptureSession.close],
     *   [android.hardware.camera2.CameraCaptureSession.abortCaptures], or
     *   [android.hardware.camera2.CameraCaptureSession.stopRepeating]
     * - Request submission methods:
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingRequest],
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingBurst],
     *   [android.hardware.camera2.CameraCaptureSession.capture], or
     *   [android.hardware.camera2.CameraCaptureSession.captureBurst]
     * - Surface / output configuration methods:
     *   [android.hardware.camera2.CameraCaptureSession.updateOutputConfiguration],
     *   [android.hardware.camera2.CameraCaptureSession.finalizeOutputConfigurations],
     *   [android.hardware.camera2.CameraCaptureSession.prepare], or
     *   [android.hardware.camera2.CameraCaptureSession.switchToOffline]
     */
    public var repeatingCaptureCallback: CameraCaptureSession.CaptureCallback?
}

/**
 * Configures Camera2 options for still capture requests.
 *
 * Keys configured for still capture requests override corresponding keys set in repeating requests.
 */
public interface StillCaptureInterop {
    /**
     * Sets a [CaptureRequest.Key] and value for still capture requests.
     *
     * The capture request keys for one-shot still captures (such as
     * [androidx.camera.core.ImageCapture.takePicture]) are determined by copying all repeating
     * request keys (which may include keys added via
     * [androidx.camera.core.SessionConfig.Builder.setInterop] or
     * [androidx.camera.core.CameraControl.applyInteropAsync]) and then overriding them with the
     * still capture request key set here.
     *
     * @param key capture request option key
     * @param value option value
     */
    public fun <V> setStillCaptureRequestOption(key: CaptureRequest.Key<V>, value: V)

    /**
     * Helper target property supporting `stillCaptureRequest[key] = value` indexing operator
     * syntax.
     */
    public val stillCaptureRequest: StillCaptureRequestOptionTarget
        get() = StillCaptureRequestOptionTarget(this)

    /** The template type for still capture requests. */
    public var stillCaptureRequestTemplateType: Int

    /**
     * Sets the callback to receive still capture updates with an executor.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Directly invoking state-altering, lifecycle, or request-submitting methods on
     * these raw objects bypasses CameraX pipeline management and causes state desynchronization,
     * stream freezing, or crashes. Do not invoke:
     * - Session lifecycle methods: [android.hardware.camera2.CameraCaptureSession.close],
     *   [android.hardware.camera2.CameraCaptureSession.abortCaptures], or
     *   [android.hardware.camera2.CameraCaptureSession.stopRepeating]
     * - Request submission methods:
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingRequest],
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingBurst],
     *   [android.hardware.camera2.CameraCaptureSession.capture], or
     *   [android.hardware.camera2.CameraCaptureSession.captureBurst]
     * - Surface / output configuration methods:
     *   [android.hardware.camera2.CameraCaptureSession.updateOutputConfiguration],
     *   [android.hardware.camera2.CameraCaptureSession.finalizeOutputConfigurations],
     *   [android.hardware.camera2.CameraCaptureSession.prepare], or
     *   [android.hardware.camera2.CameraCaptureSession.switchToOffline]
     *
     * @param executor executor to run the callback on
     * @param callback still capture callback
     */
    public fun setStillCaptureCallback(
        executor: Executor,
        callback: CameraCaptureSession.CaptureCallback,
    )

    /**
     * The callback to receive still capture updates on a direct executor.
     *
     * **Warning:** The callback receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Directly invoking state-altering, lifecycle, or request-submitting methods on
     * these raw objects bypasses CameraX pipeline management and causes state desynchronization,
     * stream freezing, or crashes. Do not invoke:
     * - Session lifecycle methods: [android.hardware.camera2.CameraCaptureSession.close],
     *   [android.hardware.camera2.CameraCaptureSession.abortCaptures], or
     *   [android.hardware.camera2.CameraCaptureSession.stopRepeating]
     * - Request submission methods:
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingRequest],
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingBurst],
     *   [android.hardware.camera2.CameraCaptureSession.capture], or
     *   [android.hardware.camera2.CameraCaptureSession.captureBurst]
     * - Surface / output configuration methods:
     *   [android.hardware.camera2.CameraCaptureSession.updateOutputConfiguration],
     *   [android.hardware.camera2.CameraCaptureSession.finalizeOutputConfigurations],
     *   [android.hardware.camera2.CameraCaptureSession.prepare], or
     *   [android.hardware.camera2.CameraCaptureSession.switchToOffline]
     */
    public var stillCaptureCallback: CameraCaptureSession.CaptureCallback?
}

/** Configures Camera2 [android.hardware.camera2.params.SessionConfiguration] options. */
public interface SessionConfigurationInterop {
    /**
     * Sets a session parameter used during session creation.
     *
     * @param key session parameter key
     * @param value parameter value
     */
    @RequiresApi(28) public fun <V> setSessionParameter(key: CaptureRequest.Key<V>, value: V)

    /**
     * Helper target property supporting `sessionParameter[key] = value` indexing operator syntax.
     */
    @get:RequiresApi(28)
    public val sessionParameter: SessionParameterTarget
        get() = SessionParameterTarget(this)

    /** The operating mode session type. */
    @get:RequiresApi(28) @set:RequiresApi(28) public var sessionType: Int

    /** The session color space. */
    @get:RequiresApi(34) @set:RequiresApi(34) public var colorSpace: ColorSpace.Named?

    /**
     * Sets the callback to receive session state updates with an executor.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Directly invoking state-altering, lifecycle, or request-submitting methods on
     * these raw objects bypasses CameraX pipeline management and causes state desynchronization,
     * stream freezing, or crashes. Do not invoke:
     * - Session lifecycle methods: [android.hardware.camera2.CameraCaptureSession.close],
     *   [android.hardware.camera2.CameraCaptureSession.abortCaptures], or
     *   [android.hardware.camera2.CameraCaptureSession.stopRepeating]
     * - Request submission methods:
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingRequest],
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingBurst],
     *   [android.hardware.camera2.CameraCaptureSession.capture], or
     *   [android.hardware.camera2.CameraCaptureSession.captureBurst]
     * - Surface / output configuration methods:
     *   [android.hardware.camera2.CameraCaptureSession.updateOutputConfiguration],
     *   [android.hardware.camera2.CameraCaptureSession.finalizeOutputConfigurations],
     *   [android.hardware.camera2.CameraCaptureSession.prepare], or
     *   [android.hardware.camera2.CameraCaptureSession.switchToOffline]
     *
     * @param executor executor to run the callback on
     * @param callback session state callback
     */
    public fun setSessionStateCallback(
        executor: Executor,
        callback: CameraCaptureSession.StateCallback,
    )

    /**
     * The callback to receive session state updates on a direct executor.
     *
     * **Warning:** The callback receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Directly invoking state-altering, lifecycle, or request-submitting methods on
     * these raw objects bypasses CameraX pipeline management and causes state desynchronization,
     * stream freezing, or crashes. Do not invoke:
     * - Session lifecycle methods: [android.hardware.camera2.CameraCaptureSession.close],
     *   [android.hardware.camera2.CameraCaptureSession.abortCaptures], or
     *   [android.hardware.camera2.CameraCaptureSession.stopRepeating]
     * - Request submission methods:
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingRequest],
     *   [android.hardware.camera2.CameraCaptureSession.setRepeatingBurst],
     *   [android.hardware.camera2.CameraCaptureSession.capture], or
     *   [android.hardware.camera2.CameraCaptureSession.captureBurst]
     * - Surface / output configuration methods:
     *   [android.hardware.camera2.CameraCaptureSession.updateOutputConfiguration],
     *   [android.hardware.camera2.CameraCaptureSession.finalizeOutputConfigurations],
     *   [android.hardware.camera2.CameraCaptureSession.prepare], or
     *   [android.hardware.camera2.CameraCaptureSession.switchToOffline]
     */
    public var sessionStateCallback: CameraCaptureSession.StateCallback?
}

/** Configures Camera2 [android.hardware.camera2.CameraDevice] options. */
public interface CameraDeviceInterop {
    /**
     * Sets the callback to receive camera device state updates with an executor.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraDevice] instances.
     * Directly invoking state-altering, lifecycle, or session-creation methods on these raw objects
     * bypasses CameraX pipeline management and causes state desynchronization, stream interruption,
     * or crashes. Do not invoke:
     * - [android.hardware.camera2.CameraDevice.close]
     * - Session creation methods: [android.hardware.camera2.CameraDevice.createCaptureSession],
     *   [android.hardware.camera2.CameraDevice.createCaptureSessionByOutputConfigurations],
     *   [android.hardware.camera2.CameraDevice.createReprocessableCaptureSession], or
     *   [android.hardware.camera2.CameraDevice.createExtensionSession]
     *
     * @param executor executor to run the callback on
     * @param callback device state callback
     */
    public fun setDeviceStateCallback(executor: Executor, callback: CameraDevice.StateCallback)

    /**
     * The callback to receive camera device state updates on a direct executor.
     *
     * **Warning:** The callback receives raw [android.hardware.camera2.CameraDevice] instances.
     * Directly invoking state-altering, lifecycle, or session-creation methods on these raw objects
     * bypasses CameraX pipeline management and causes state desynchronization, stream interruption,
     * or crashes. Do not invoke:
     * - [android.hardware.camera2.CameraDevice.close]
     * - Session creation methods: [android.hardware.camera2.CameraDevice.createCaptureSession],
     *   [android.hardware.camera2.CameraDevice.createCaptureSessionByOutputConfigurations],
     *   [android.hardware.camera2.CameraDevice.createReprocessableCaptureSession], or
     *   [android.hardware.camera2.CameraDevice.createExtensionSession]
     */
    public var deviceStateCallback: CameraDevice.StateCallback?
}

// =========================================================================================
// Internal Base Interop Delegates
// =========================================================================================

/** Provides default implementation for [OutputConfigurationInterop] using a [MutableConfig]. */
internal interface OutputConfigurationInteropDelegate : OutputConfigurationInterop {
    val mutableConfig: MutableConfig

    @get:RequiresApi(28)
    @set:RequiresApi(28)
    override var physicalCameraId: String?
        get() =
            mutableConfig.retrieveOption(Camera2ImplConfig.SESSION_PHYSICAL_CAMERA_ID_OPTION, null)
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.SESSION_PHYSICAL_CAMERA_ID_OPTION, value)
        }

    @get:RequiresApi(33)
    @set:RequiresApi(33)
    override var streamUseCase: Long
        get() =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.STREAM_USE_CASE_OPTION,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
            )!!
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION, value)
        }

    @get:RequiresApi(33)
    @set:RequiresApi(33)
    override var mirrorMode: Int
        get() =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.SESSION_MIRROR_MODE_OPTION,
                OutputConfiguration.MIRROR_MODE_AUTO,
            )!!
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.SESSION_MIRROR_MODE_OPTION, value)
        }

    @get:RequiresApi(33)
    @set:RequiresApi(33)
    override var timestampBase: Int
        get() =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.TIMESTAMP_BASE_OPTION,
                OutputConfiguration.TIMESTAMP_BASE_DEFAULT,
            )!!
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.TIMESTAMP_BASE_OPTION, value)
        }

    @get:RequiresApi(33)
    @set:RequiresApi(33)
    override var dynamicRangeProfile: Long
        get() =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.DYNAMIC_RANGE_PROFILE_OPTION,
                DynamicRangeProfiles.STANDARD,
            )!!
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.DYNAMIC_RANGE_PROFILE_OPTION, value)
        }

    override var surfaceGroupId: Int
        get() =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.SURFACE_GROUP_ID_OPTION,
                OutputConfiguration.SURFACE_GROUP_ID_NONE,
            )!!
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.SURFACE_GROUP_ID_OPTION, value)
        }

    @get:RequiresApi(31)
    @set:RequiresApi(31)
    override var sensorPixelModesUsed: Set<Int>
        get() =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.SENSOR_PIXEL_MODES_USED_OPTION,
                emptySet(),
            )!!
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.SENSOR_PIXEL_MODES_USED_OPTION, value)
        }

    @RequiresApi(31)
    override fun addSensorPixelModeUsed(sensorPixelMode: Int) {
        val current =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.SENSOR_PIXEL_MODES_USED_OPTION,
                emptySet(),
            )!!
        mutableConfig.insertOption(
            Camera2ImplConfig.SENSOR_PIXEL_MODES_USED_OPTION,
            current + sensorPixelMode,
        )
    }
}

/** Provides default implementation for [CameraCaptureSessionInterop] using a [MutableConfig]. */
internal interface CameraCaptureSessionInteropDelegate : CameraCaptureSessionInterop {
    val mutableConfig: MutableConfig

    override var repeatingCaptureRequestTemplate: Int
        get() =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.TEMPLATE_TYPE_OPTION,
                CameraDevice.TEMPLATE_PREVIEW,
            )!!
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION, value)
        }

    override var repeatingCaptureCallback: CameraCaptureSession.CaptureCallback?
        get() =
            (mutableConfig.retrieveOption(
                    Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION,
                    null,
                ) as? CaptureCallbackExecutorWrapper)
                ?.callback
        set(value) {
            if (value != null) {
                setRepeatingCaptureCallback({ it.run() }, value)
            } else {
                mutableConfig.removeOption(
                    Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION
                )
            }
        }

    override fun <V> setCaptureRequestOption(key: CaptureRequest.Key<V>, value: V) {
        val opt = key.createCaptureRequestOption()
        mutableConfig.insertOption(opt, Config.OptionPriority.ALWAYS_OVERRIDE, value)
    }

    override fun clearCaptureRequestOption(key: CaptureRequest.Key<*>) {
        val opt = key.createCaptureRequestOption()
        mutableConfig.removeOption(opt)
    }

    override fun clearAllCaptureRequestOptions() {
        val optionsToRemove =
            mutableConfig.listOptions().filter {
                it.id.startsWith(Camera2ImplConfig.CAPTURE_REQUEST_ID_STEM)
            }
        for (opt in optionsToRemove) {
            mutableConfig.removeOption(opt)
        }
    }

    override fun setRepeatingCaptureCallback(
        executor: Executor,
        callback: CameraCaptureSession.CaptureCallback,
    ) {
        val wrappedCallback = CaptureCallbackExecutorWrapper(executor, callback)
        mutableConfig.insertOption(
            Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION,
            wrappedCallback,
        )
    }
}

/** Provides default implementation for [StillCaptureInterop] using a [MutableConfig]. */
internal interface StillCaptureInteropDelegate : StillCaptureInterop {
    val mutableConfig: MutableConfig

    override var stillCaptureRequestTemplateType: Int
        get() =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.TEMPLATE_TYPE_OPTION,
                CameraDevice.TEMPLATE_STILL_CAPTURE,
            )!!
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION, value)
        }

    override var stillCaptureCallback: CameraCaptureSession.CaptureCallback?
        get() =
            (mutableConfig.retrieveOption(Camera2ImplConfig.STILL_CAPTURE_CALLBACK_OPTION, null)
                    as? CaptureCallbackExecutorWrapper)
                ?.callback
        set(value) {
            if (value != null) {
                setStillCaptureCallback({ it.run() }, value)
            } else {
                mutableConfig.removeOption(Camera2ImplConfig.STILL_CAPTURE_CALLBACK_OPTION)
            }
        }

    override fun <V> setStillCaptureRequestOption(key: CaptureRequest.Key<V>, value: V) {
        val stillOpt = key.createStillCaptureRequestOption()
        mutableConfig.insertOption(stillOpt, Config.OptionPriority.ALWAYS_OVERRIDE, value)
    }

    override fun setStillCaptureCallback(
        executor: Executor,
        callback: CameraCaptureSession.CaptureCallback,
    ) {
        val wrappedCallback = CaptureCallbackExecutorWrapper(executor, callback)
        mutableConfig.insertOption(Camera2ImplConfig.STILL_CAPTURE_CALLBACK_OPTION, wrappedCallback)
    }
}

/** Provides default implementation for [SessionConfigurationInterop] using a [MutableConfig]. */
internal interface SessionConfigurationInteropDelegate : SessionConfigurationInterop {
    val mutableConfig: MutableConfig

    @get:RequiresApi(28)
    @set:RequiresApi(28)
    override var sessionType: Int
        get() =
            mutableConfig.retrieveOption(
                Camera2ImplConfig.SESSION_TYPE_OPTION,
                SessionConfiguration.SESSION_REGULAR,
            )!!
        set(value) {
            mutableConfig.insertOption(Camera2ImplConfig.SESSION_TYPE_OPTION, value)
        }

    @get:RequiresApi(34)
    @set:RequiresApi(34)
    override var colorSpace: ColorSpace.Named?
        get() {
            val ordinal =
                mutableConfig.retrieveOption(Camera2ImplConfig.SESSION_COLOR_SPACE_OPTION, -1) ?: -1
            return if (ordinal in ColorSpace.Named.values().indices) {
                ColorSpace.Named.values()[ordinal]
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                mutableConfig.insertOption(
                    Camera2ImplConfig.SESSION_COLOR_SPACE_OPTION,
                    value.ordinal,
                )
            } else {
                mutableConfig.removeOption(Camera2ImplConfig.SESSION_COLOR_SPACE_OPTION)
            }
        }

    override var sessionStateCallback: CameraCaptureSession.StateCallback?
        get() =
            (mutableConfig.retrieveOption(Camera2ImplConfig.SESSION_STATE_CALLBACK_OPTION, null)
                    as? SessionStateCallbackExecutorWrapper)
                ?.callback
        set(value) {
            if (value != null) {
                setSessionStateCallback({ it.run() }, value)
            } else {
                mutableConfig.removeOption(Camera2ImplConfig.SESSION_STATE_CALLBACK_OPTION)
            }
        }

    @RequiresApi(28)
    override fun <V> setSessionParameter(key: CaptureRequest.Key<V>, value: V) {
        val opt = key.createSessionParameterOption()
        mutableConfig.insertOption(opt, Config.OptionPriority.ALWAYS_OVERRIDE, value)
    }

    override fun setSessionStateCallback(
        executor: Executor,
        callback: CameraCaptureSession.StateCallback,
    ) {
        val wrappedCallback = SessionStateCallbackExecutorWrapper(executor, callback)
        mutableConfig.insertOption(Camera2ImplConfig.SESSION_STATE_CALLBACK_OPTION, wrappedCallback)
    }
}

/** Provides default implementation for [CameraDeviceInterop] using a [MutableConfig]. */
internal interface CameraDeviceInteropDelegate : CameraDeviceInterop {
    val mutableConfig: MutableConfig

    override var deviceStateCallback: CameraDevice.StateCallback?
        get() =
            (mutableConfig.retrieveOption(Camera2ImplConfig.DEVICE_STATE_CALLBACK_OPTION, null)
                    as? DeviceStateCallbackExecutorWrapper)
                ?.callback
        set(value) {
            if (value != null) {
                setDeviceStateCallback({ it.run() }, value)
            } else {
                mutableConfig.removeOption(Camera2ImplConfig.DEVICE_STATE_CALLBACK_OPTION)
            }
        }

    override fun setDeviceStateCallback(executor: Executor, callback: CameraDevice.StateCallback) {
        val wrappedCallback = DeviceStateCallbackExecutorWrapper(executor, callback)
        mutableConfig.insertOption(Camera2ImplConfig.DEVICE_STATE_CALLBACK_OPTION, wrappedCallback)
    }
}

// =========================================================================================
// Internal Callback Executor Wrappers
// =========================================================================================

/** Wraps a [CameraDevice.StateCallback] to run callbacks on a specified [Executor]. */
internal class DeviceStateCallbackExecutorWrapper(
    internal val executor: Executor,
    internal val callback: CameraDevice.StateCallback,
) : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        executor.execute { callback.onOpened(camera) }
    }

    override fun onDisconnected(camera: CameraDevice) {
        executor.execute { callback.onDisconnected(camera) }
    }

    override fun onError(camera: CameraDevice, error: Int) {
        executor.execute { callback.onError(camera, error) }
    }

    override fun onClosed(camera: CameraDevice) {
        executor.execute { callback.onClosed(camera) }
    }
}

/** Wraps a [CameraCaptureSession.StateCallback] to run callbacks on a specified [Executor]. */
internal class SessionStateCallbackExecutorWrapper(
    internal val executor: Executor,
    internal val callback: CameraCaptureSession.StateCallback,
) : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        executor.execute { callback.onConfigured(session) }
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        executor.execute { callback.onConfigureFailed(session) }
    }

    override fun onReady(session: CameraCaptureSession) {
        executor.execute { callback.onReady(session) }
    }

    override fun onActive(session: CameraCaptureSession) {
        executor.execute { callback.onActive(session) }
    }

    @RequiresApi(26)
    override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
        executor.execute { callback.onCaptureQueueEmpty(session) }
    }

    override fun onClosed(session: CameraCaptureSession) {
        executor.execute { callback.onClosed(session) }
    }

    override fun onSurfacePrepared(session: CameraCaptureSession, surface: Surface) {
        executor.execute { callback.onSurfacePrepared(session, surface) }
    }
}

/** Wraps a [CameraCaptureSession.CaptureCallback] to run callbacks on a specified [Executor]. */
internal class CaptureCallbackExecutorWrapper(
    internal val executor: Executor,
    internal val callback: CameraCaptureSession.CaptureCallback,
) : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureStarted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long,
    ) {
        executor.execute { callback.onCaptureStarted(session, request, timestamp, frameNumber) }
    }

    override fun onCaptureProgressed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        partialResult: android.hardware.camera2.CaptureResult,
    ) {
        executor.execute { callback.onCaptureProgressed(session, request, partialResult) }
    }

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult,
    ) {
        executor.execute { callback.onCaptureCompleted(session, request, result) }
    }

    override fun onCaptureFailed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        failure: android.hardware.camera2.CaptureFailure,
    ) {
        executor.execute { callback.onCaptureFailed(session, request, failure) }
    }

    override fun onCaptureSequenceCompleted(
        session: CameraCaptureSession,
        sequenceId: Int,
        frameNumber: Long,
    ) {
        executor.execute { callback.onCaptureSequenceCompleted(session, sequenceId, frameNumber) }
    }

    override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
        executor.execute { callback.onCaptureSequenceAborted(session, sequenceId) }
    }

    override fun onCaptureBufferLost(
        session: CameraCaptureSession,
        request: CaptureRequest,
        surface: Surface,
        frameNumber: Long,
    ) {
        executor.execute { callback.onCaptureBufferLost(session, request, surface, frameNumber) }
    }

    @RequiresApi(34)
    override fun onReadoutStarted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long,
    ) {
        executor.execute { callback.onReadoutStarted(session, request, timestamp, frameNumber) }
    }
}

/** Target class supporting `captureRequest[key] = value` indexing operator syntax. */
public class CaptureRequestOptionTarget
internal constructor(private val interop: CameraCaptureSessionInterop) {
    /** Sets a [CaptureRequest.Key] and value on repeating and one-shot requests. */
    public operator fun <V> set(key: CaptureRequest.Key<V>, value: V) {
        interop.setCaptureRequestOption(key, value)
    }
}

/** Target class supporting `stillCaptureRequest[key] = value` indexing operator syntax. */
public class StillCaptureRequestOptionTarget
internal constructor(private val interop: StillCaptureInterop) {
    /** Sets a [CaptureRequest.Key] and value for still capture requests. */
    public operator fun <V> set(key: CaptureRequest.Key<V>, value: V) {
        interop.setStillCaptureRequestOption(key, value)
    }
}

/** Target class supporting `sessionParameter[key] = value` indexing operator syntax. */
public class SessionParameterTarget
internal constructor(private val interop: SessionConfigurationInterop) {
    /** Sets a session parameter used during session creation. */
    @RequiresApi(28)
    public operator fun <V> set(key: CaptureRequest.Key<V>, value: V) {
        interop.setSessionParameter(key, value)
    }
}
