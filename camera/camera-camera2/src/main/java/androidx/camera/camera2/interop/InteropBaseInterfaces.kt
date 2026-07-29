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

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
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
public interface OutputConfigurationInterop<T : OutputConfigurationInterop<T>> {
    /**
     * Sets the physical camera ID.
     *
     * @param cameraId physical camera ID
     * @return this [OutputConfigurationInterop] instance
     */
    @RequiresApi(28) public fun setPhysicalCameraId(cameraId: String): T

    /**
     * Sets the physical camera ID.
     *
     * @see setPhysicalCameraId
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    @get:RequiresApi(28)
    @set:RequiresApi(28)
    public var physicalCameraId: String
        get() = throw UnsupportedOperationException()
        set(value) {
            setPhysicalCameraId(value)
        }

    /**
     * Sets the stream use case.
     *
     * @param streamUseCase stream use case
     * @return this [OutputConfigurationInterop] instance
     */
    @RequiresApi(33) public fun setStreamUseCase(streamUseCase: Long): T

    /**
     * Sets the stream use case.
     *
     * @see setStreamUseCase
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    @get:RequiresApi(33)
    @set:RequiresApi(33)
    public var streamUseCase: Long
        get() = throw UnsupportedOperationException()
        set(value) {
            setStreamUseCase(value)
        }

    /**
     * Sets the mirror mode.
     *
     * @param mirrorMode mirror mode
     * @return this [OutputConfigurationInterop] instance
     */
    @RequiresApi(33) public fun setMirrorMode(mirrorMode: Int): T

    /**
     * Sets the mirror mode.
     *
     * @see setMirrorMode
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    @get:RequiresApi(33)
    @set:RequiresApi(33)
    public var mirrorMode: Int
        get() = throw UnsupportedOperationException()
        set(value) {
            setMirrorMode(value)
        }

    /**
     * Sets the timestamp base.
     *
     * @param timestampBase timestamp base
     * @return this [OutputConfigurationInterop] instance
     */
    @RequiresApi(33) public fun setTimestampBase(timestampBase: Int): T

    /**
     * Sets the timestamp base.
     *
     * @see setTimestampBase
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    @get:RequiresApi(33)
    @set:RequiresApi(33)
    public var timestampBase: Int
        get() = throw UnsupportedOperationException()
        set(value) {
            setTimestampBase(value)
        }

    /**
     * Sets the dynamic range profile.
     *
     * @param dynamicRangeProfile dynamic range profile
     * @return this [OutputConfigurationInterop] instance
     */
    @RequiresApi(33) public fun setDynamicRangeProfile(dynamicRangeProfile: Long): T

    /**
     * Sets the dynamic range profile.
     *
     * @see setDynamicRangeProfile
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    @get:RequiresApi(33)
    @set:RequiresApi(33)
    public var dynamicRangeProfile: Long
        get() = throw UnsupportedOperationException()
        set(value) {
            setDynamicRangeProfile(value)
        }

    /**
     * Sets the surface group ID.
     *
     * @param groupId surface group ID
     * @return this [OutputConfigurationInterop] instance
     */
    @RequiresApi(24) public fun setSurfaceGroupId(groupId: Int): T

    /**
     * Sets the surface group ID.
     *
     * @see setSurfaceGroupId
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    @get:RequiresApi(24)
    @set:RequiresApi(24)
    public var surfaceGroupId: Int
        get() = throw UnsupportedOperationException()
        set(value) {
            setSurfaceGroupId(value)
        }
}

/** Configures Camera2 [android.hardware.camera2.CameraCaptureSession] options. */
public interface CameraCaptureSessionInterop<T : CameraCaptureSessionInterop<T>> {
    /**
     * Sets a [CaptureRequest.Key] and value on repeating and one-shot requests.
     *
     * @param key capture request option key
     * @param value option value
     * @return this [CameraCaptureSessionInterop] instance
     */
    public fun <V> setCaptureRequestOption(key: CaptureRequest.Key<V>, value: V): T

    /** Helper target property supporting `captureRequest[key] = value` indexing operator syntax. */
    @get:JvmSynthetic
    public val captureRequest: CaptureRequestOptionTarget
        get() = CaptureRequestOptionTarget(this)

    /**
     * Clears a [CaptureRequest.Key] previously set on this target.
     *
     * @param key capture request option key
     * @return this [CameraCaptureSessionInterop] instance
     */
    public fun clearCaptureRequestOption(key: CaptureRequest.Key<*>): T

    /**
     * Clears all [CaptureRequest.Key]s previously set on this target.
     *
     * @return this [CameraCaptureSessionInterop] instance
     */
    public fun clearAllCaptureRequestOptions(): T

    /**
     * Sets the repeating capture request template type.
     *
     * @param templateType template type (e.g.,
     *   [android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW])
     * @return this [CameraCaptureSessionInterop] instance
     */
    public fun setRepeatingCaptureRequestTemplate(templateType: Int): T

    /**
     * Sets the repeating capture request template type.
     *
     * @see setRepeatingCaptureRequestTemplate
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    public var repeatingCaptureRequestTemplate: Int
        get() = throw UnsupportedOperationException()
        set(value) {
            setRepeatingCaptureRequestTemplate(value)
        }

    /**
     * Sets the callback to receive repeating capture updates.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Calling state-altering methods on the session (such as
     * [android.hardware.camera2.CameraCaptureSession.close] or
     * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
     * management and may cause state desynchronization or application crashes.
     *
     * @param executor executor to run the callback on
     * @param callback repeating capture callback
     * @return this [CameraCaptureSessionInterop] instance
     */
    public fun setRepeatingCaptureCallback(
        executor: Executor,
        callback: CameraCaptureSession.CaptureCallback,
    ): T

    /**
     * Sets the callback to receive repeating capture updates on a direct executor.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Calling state-altering methods on the session (such as
     * [android.hardware.camera2.CameraCaptureSession.close] or
     * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
     * management and may cause state desynchronization or application crashes.
     *
     * @param callback repeating capture callback
     * @return this [CameraCaptureSessionInterop] instance
     */
    public fun setRepeatingCaptureCallback(callback: CameraCaptureSession.CaptureCallback): T =
        setRepeatingCaptureCallback({ it.run() }, callback)

    /**
     * Sets the callback to receive repeating capture updates on a direct executor.
     *
     * @see setRepeatingCaptureCallback
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    public var repeatingCaptureCallback: CameraCaptureSession.CaptureCallback
        get() = throw UnsupportedOperationException()
        set(value) {
            setRepeatingCaptureCallback(value)
        }
}

/**
 * Configures Camera2 options for still capture requests.
 *
 * Keys configured for still capture requests override corresponding keys set in repeating requests.
 */
public interface StillCaptureInterop<T : StillCaptureInterop<T>> {
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
     * @return this [StillCaptureInterop] instance
     */
    public fun <V> setStillCaptureRequestOption(key: CaptureRequest.Key<V>, value: V): T

    /**
     * Helper target property supporting `stillCaptureRequest[key] = value` indexing operator
     * syntax.
     */
    @get:JvmSynthetic
    public val stillCaptureRequest: StillCaptureRequestOptionTarget
        get() = StillCaptureRequestOptionTarget(this)

    /**
     * Sets the template type for still capture requests.
     *
     * @param templateType template type (e.g.,
     *   [android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE])
     * @return this [StillCaptureInterop] instance
     */
    public fun setStillCaptureRequestTemplateType(templateType: Int): T

    /**
     * Sets the template type for still capture requests.
     *
     * @see setStillCaptureRequestTemplateType
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    public var stillCaptureRequestTemplateType: Int
        get() = throw UnsupportedOperationException()
        set(value) {
            setStillCaptureRequestTemplateType(value)
        }

    /**
     * Sets the callback to receive still capture updates.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Calling state-altering methods on the session (such as
     * [android.hardware.camera2.CameraCaptureSession.close] or
     * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
     * management and may cause state desynchronization or application crashes.
     *
     * @param executor executor to run the callback on
     * @param callback still capture callback
     * @return this [StillCaptureInterop] instance
     */
    public fun setStillCaptureCallback(
        executor: Executor,
        callback: CameraCaptureSession.CaptureCallback,
    ): T

    /**
     * Sets the callback to receive still capture updates on a direct executor.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Calling state-altering methods on the session (such as
     * [android.hardware.camera2.CameraCaptureSession.close] or
     * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
     * management and may cause state desynchronization or application crashes.
     *
     * @param callback still capture callback
     * @return this [StillCaptureInterop] instance
     */
    public fun setStillCaptureCallback(callback: CameraCaptureSession.CaptureCallback): T =
        setStillCaptureCallback({ it.run() }, callback)

    /**
     * Sets the callback to receive still capture updates on a direct executor.
     *
     * @see setStillCaptureCallback
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    public var stillCaptureCallback: CameraCaptureSession.CaptureCallback
        get() = throw UnsupportedOperationException()
        set(value) {
            setStillCaptureCallback(value)
        }
}

/** Configures Camera2 [android.hardware.camera2.params.SessionConfiguration] options. */
public interface SessionConfigurationInterop<T : SessionConfigurationInterop<T>> {
    /**
     * Sets a session parameter used during session creation.
     *
     * @param key session parameter key
     * @param value parameter value
     * @return this [SessionConfigurationInterop] instance
     */
    @RequiresApi(28) public fun <V> setSessionParameter(key: CaptureRequest.Key<V>, value: V): T

    /**
     * Helper target property supporting `sessionParameter[key] = value` indexing operator syntax.
     */
    @get:JvmSynthetic
    @get:RequiresApi(28)
    public val sessionParameter: SessionParameterTarget
        get() = SessionParameterTarget(this)

    /**
     * Sets the operating mode session type.
     *
     * @param sessionType session type
     * @return this [SessionConfigurationInterop] instance
     */
    @RequiresApi(28) public fun setSessionType(sessionType: Int): T

    /**
     * Sets the operating mode session type.
     *
     * @see setSessionType
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    @get:RequiresApi(28)
    @set:RequiresApi(28)
    public var sessionType: Int
        get() = throw UnsupportedOperationException()
        set(value) {
            setSessionType(value)
        }

    /**
     * Sets the session color space.
     *
     * @param colorSpace color space
     * @return this [SessionConfigurationInterop] instance
     */
    @RequiresApi(34) public fun setColorSpace(colorSpace: Int): T

    /**
     * Sets the session color space.
     *
     * @see setColorSpace
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    @get:RequiresApi(34)
    @set:RequiresApi(34)
    public var colorSpace: Int
        get() = throw UnsupportedOperationException()
        set(value) {
            setColorSpace(value)
        }

    /**
     * Sets the callback to receive session state updates.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Calling state-altering methods on the session (such as
     * [android.hardware.camera2.CameraCaptureSession.close] or
     * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
     * management and may cause state desynchronization or application crashes.
     *
     * @param executor executor to run the callback on
     * @param callback session state callback
     * @return this [SessionConfigurationInterop] instance
     */
    public fun setSessionStateCallback(
        executor: Executor,
        callback: CameraCaptureSession.StateCallback,
    ): T

    /**
     * Sets the callback to receive session state updates on a direct executor.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraCaptureSession]
     * instances. Calling state-altering methods on the session (such as
     * [android.hardware.camera2.CameraCaptureSession.close] or
     * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
     * management and may cause state desynchronization or application crashes.
     *
     * @param callback session state callback
     * @return this [SessionConfigurationInterop] instance
     */
    public fun setSessionStateCallback(callback: CameraCaptureSession.StateCallback): T =
        setSessionStateCallback({ it.run() }, callback)

    /**
     * Sets the callback to receive session state updates on a direct executor.
     *
     * @see setSessionStateCallback
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    public var sessionStateCallback: CameraCaptureSession.StateCallback
        get() = throw UnsupportedOperationException()
        set(value) {
            setSessionStateCallback(value)
        }
}

/** Configures Camera2 [android.hardware.camera2.CameraDevice] options. */
public interface CameraDeviceInterop<T : CameraDeviceInterop<T>> {
    /**
     * Sets the callback to receive camera device state updates.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraDevice] instances.
     * Calling [android.hardware.camera2.CameraDevice.close] directly on the device bypasses CameraX
     * pipeline management and may cause state desynchronization or application crashes.
     *
     * @param executor executor to run the callback on
     * @param callback device state callback
     * @return this [CameraDeviceInterop] instance
     */
    public fun setDeviceStateCallback(executor: Executor, callback: CameraDevice.StateCallback): T

    /**
     * Sets the callback to receive camera device state updates on a direct executor.
     *
     * **Warning:** The [callback] receives raw [android.hardware.camera2.CameraDevice] instances.
     * Calling [android.hardware.camera2.CameraDevice.close] directly on the device bypasses CameraX
     * pipeline management and may cause state desynchronization or application crashes.
     *
     * @param callback device state callback
     * @return this [CameraDeviceInterop] instance
     */
    public fun setDeviceStateCallback(callback: CameraDevice.StateCallback): T =
        setDeviceStateCallback({ it.run() }, callback)

    /**
     * Sets the callback to receive camera device state updates on a direct executor.
     *
     * @see setDeviceStateCallback
     */
    @get:JvmSynthetic
    @get:Deprecated("Write-only", level = DeprecationLevel.ERROR)
    @set:JvmSynthetic
    public var deviceStateCallback: CameraDevice.StateCallback
        get() = throw UnsupportedOperationException()
        set(value) {
            setDeviceStateCallback(value)
        }
}

// =========================================================================================
// Internal Base Interop Delegates
// =========================================================================================

/** Provides default implementation for [OutputConfigurationInterop] using a [MutableConfig]. */
internal interface OutputConfigurationInteropDelegate<T : OutputConfigurationInterop<T>> :
    OutputConfigurationInterop<T> {
    val mutableConfig: MutableConfig

    @RequiresApi(28)
    @Suppress("UNCHECKED_CAST")
    override fun setPhysicalCameraId(cameraId: String): T {
        mutableConfig.insertOption(Camera2ImplConfig.SESSION_PHYSICAL_CAMERA_ID_OPTION, cameraId)
        return this as T
    }

    @RequiresApi(33)
    @Suppress("UNCHECKED_CAST")
    override fun setStreamUseCase(streamUseCase: Long): T {
        mutableConfig.insertOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION, streamUseCase)
        return this as T
    }

    @RequiresApi(33)
    @Suppress("UNCHECKED_CAST")
    override fun setMirrorMode(mirrorMode: Int): T {
        mutableConfig.insertOption(Camera2ImplConfig.SESSION_MIRROR_MODE_OPTION, mirrorMode)
        return this as T
    }

    @RequiresApi(33)
    @Suppress("UNCHECKED_CAST")
    override fun setTimestampBase(timestampBase: Int): T {
        mutableConfig.insertOption(Camera2ImplConfig.TIMESTAMP_BASE_OPTION, timestampBase)
        return this as T
    }

    @RequiresApi(33)
    @Suppress("UNCHECKED_CAST")
    override fun setDynamicRangeProfile(dynamicRangeProfile: Long): T {
        mutableConfig.insertOption(
            Camera2ImplConfig.DYNAMIC_RANGE_PROFILE_OPTION,
            dynamicRangeProfile,
        )
        return this as T
    }

    @RequiresApi(24)
    @Suppress("UNCHECKED_CAST")
    override fun setSurfaceGroupId(groupId: Int): T {
        mutableConfig.insertOption(Camera2ImplConfig.SURFACE_GROUP_ID_OPTION, groupId)
        return this as T
    }
}

/** Provides default implementation for [CameraCaptureSessionInterop] using a [MutableConfig]. */
internal interface CameraCaptureSessionInteropDelegate<T : CameraCaptureSessionInterop<T>> :
    CameraCaptureSessionInterop<T> {
    val mutableConfig: MutableConfig

    @Suppress("UNCHECKED_CAST")
    override fun <V> setCaptureRequestOption(key: CaptureRequest.Key<V>, value: V): T {
        val opt = key.createCaptureRequestOption()
        mutableConfig.insertOption(opt, Config.OptionPriority.ALWAYS_OVERRIDE, value)
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun clearCaptureRequestOption(key: CaptureRequest.Key<*>): T {
        val opt = key.createCaptureRequestOption()
        mutableConfig.removeOption(opt)
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun clearAllCaptureRequestOptions(): T {
        val optionsToRemove =
            mutableConfig.listOptions().filter {
                it.id.startsWith(Camera2ImplConfig.CAPTURE_REQUEST_ID_STEM)
            }
        for (opt in optionsToRemove) {
            mutableConfig.removeOption(opt)
        }
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun setRepeatingCaptureRequestTemplate(templateType: Int): T {
        mutableConfig.insertOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION, templateType)
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun setRepeatingCaptureCallback(
        executor: Executor,
        callback: CameraCaptureSession.CaptureCallback,
    ): T {
        val wrappedCallback = CaptureCallbackExecutorWrapper(executor, callback)
        mutableConfig.insertOption(
            Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION,
            wrappedCallback,
        )
        return this as T
    }
}

/** Provides default implementation for [StillCaptureInterop] using a [MutableConfig]. */
internal interface StillCaptureInteropDelegate<T : StillCaptureInterop<T>> :
    StillCaptureInterop<T> {
    val mutableConfig: MutableConfig

    @Suppress("UNCHECKED_CAST")
    override fun <V> setStillCaptureRequestOption(key: CaptureRequest.Key<V>, value: V): T {
        val stillOpt = key.createStillCaptureRequestOption()
        mutableConfig.insertOption(stillOpt, Config.OptionPriority.ALWAYS_OVERRIDE, value)
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun setStillCaptureRequestTemplateType(templateType: Int): T {
        mutableConfig.insertOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION, templateType)
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun setStillCaptureCallback(
        executor: Executor,
        callback: CameraCaptureSession.CaptureCallback,
    ): T {
        val wrappedCallback = CaptureCallbackExecutorWrapper(executor, callback)
        mutableConfig.insertOption(Camera2ImplConfig.STILL_CAPTURE_CALLBACK_OPTION, wrappedCallback)
        return this as T
    }
}

/** Provides default implementation for [SessionConfigurationInterop] using a [MutableConfig]. */
internal interface SessionConfigurationInteropDelegate<T : SessionConfigurationInterop<T>> :
    SessionConfigurationInterop<T> {
    val mutableConfig: MutableConfig

    @RequiresApi(28)
    @Suppress("UNCHECKED_CAST")
    override fun <V> setSessionParameter(key: CaptureRequest.Key<V>, value: V): T {
        val opt = key.createSessionParameterOption()
        mutableConfig.insertOption(opt, Config.OptionPriority.ALWAYS_OVERRIDE, value)
        return this as T
    }

    @RequiresApi(28)
    @Suppress("UNCHECKED_CAST")
    override fun setSessionType(sessionType: Int): T {
        mutableConfig.insertOption(Camera2ImplConfig.SESSION_TYPE_OPTION, sessionType)
        return this as T
    }

    @RequiresApi(34)
    @Suppress("UNCHECKED_CAST")
    override fun setColorSpace(colorSpace: Int): T {
        mutableConfig.insertOption(Camera2ImplConfig.SESSION_COLOR_SPACE_OPTION, colorSpace)
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun setSessionStateCallback(
        executor: Executor,
        callback: CameraCaptureSession.StateCallback,
    ): T {
        val wrappedCallback = SessionStateCallbackExecutorWrapper(executor, callback)
        mutableConfig.insertOption(Camera2ImplConfig.SESSION_STATE_CALLBACK_OPTION, wrappedCallback)
        return this as T
    }
}

/** Provides default implementation for [CameraDeviceInterop] using a [MutableConfig]. */
internal interface CameraDeviceInteropDelegate<T : CameraDeviceInterop<T>> :
    CameraDeviceInterop<T> {
    val mutableConfig: MutableConfig

    @Suppress("UNCHECKED_CAST")
    override fun setDeviceStateCallback(
        executor: Executor,
        callback: CameraDevice.StateCallback,
    ): T {
        val wrappedCallback = DeviceStateCallbackExecutorWrapper(executor, callback)
        mutableConfig.insertOption(Camera2ImplConfig.DEVICE_STATE_CALLBACK_OPTION, wrappedCallback)
        return this as T
    }
}

// =========================================================================================
// Internal Callback Executor Wrappers
// =========================================================================================

/** Wraps a [CameraDevice.StateCallback] to run callbacks on a specified [Executor]. */
internal class DeviceStateCallbackExecutorWrapper(
    internal val executor: Executor,
    private val callback: CameraDevice.StateCallback,
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
    private val callback: CameraCaptureSession.StateCallback,
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
    private val callback: CameraCaptureSession.CaptureCallback,
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

    @RequiresApi(24)
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
internal constructor(private val interop: CameraCaptureSessionInterop<*>) {
    /** Sets a [CaptureRequest.Key] and value on repeating and one-shot requests. */
    public operator fun <V> set(key: CaptureRequest.Key<V>, value: V) {
        interop.setCaptureRequestOption(key, value)
    }
}

/** Target class supporting `stillCaptureRequest[key] = value` indexing operator syntax. */
public class StillCaptureRequestOptionTarget
internal constructor(private val interop: StillCaptureInterop<*>) {
    /** Sets a [CaptureRequest.Key] and value for still capture requests. */
    public operator fun <V> set(key: CaptureRequest.Key<V>, value: V) {
        interop.setStillCaptureRequestOption(key, value)
    }
}

/** Target class supporting `sessionParameter[key] = value` indexing operator syntax. */
public class SessionParameterTarget
internal constructor(private val interop: SessionConfigurationInterop<*>) {
    /** Sets a session parameter used during session creation. */
    @RequiresApi(28)
    public operator fun <V> set(key: CaptureRequest.Key<V>, value: V) {
        interop.setSessionParameter(key, value)
    }
}
