/*
 * Copyright 2019 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.camera2.adapter.CameraInfoAdapter.Companion.unwrapAs
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.impl.createCaptureRequestOption
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExtendableBuilder
import androidx.camera.core.impl.Config
import androidx.core.util.Consumer

/**
 * Provides utilities to configure and query Camera2 APIs.
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. If an
 * option configured via interop conflicts with options required by CameraX internally, the option
 * from Camera2Interop will override, which may result in unexpected behavior or interfere with
 * CameraX functionality.
 *
 * Use this class to:
 * - Apply Camera2 configuration to [androidx.camera.core.UseCase] builders using [forUseCase],
 *   [forImageCapture], or [forSessionConfig]
 * - Apply Camera2 configuration to active camera controls using [forCameraControl]
 * - Query Camera2 metadata using [getCameraId] or [getCameraCharacteristics]
 * - Create camera selectors or filters using [getCameraSelectorFromCameraId] or
 *   [getCameraFilterFromCameraId]
 */
public class Camera2Interop private constructor() {

    public companion object {
        /**
         * Returns the Camera2 camera ID from a [CameraInfo].
         *
         * Example:
         * ```java
         * String cameraId = Camera2Interop.getCameraId(cameraInfo);
         * ```
         *
         * @param cameraInfo target [CameraInfo]
         * @return Camera2 camera ID
         * @throws IllegalArgumentException if [cameraInfo] does not contain Camera2 information
         */
        @JvmStatic
        @OptIn(ExperimentalCamera2Interop::class)
        public fun getCameraId(cameraInfo: CameraInfo): String {
            return Camera2CameraInfo.from(cameraInfo).cameraId
        }

        /**
         * Returns the [CameraCharacteristics] from a [CameraInfo].
         *
         * Example:
         * ```java
         * CameraCharacteristics characteristics = Camera2Interop.getCameraCharacteristics(cameraInfo);
         * Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
         * ```
         *
         * @param cameraInfo target [CameraInfo]
         * @return [CameraCharacteristics] associated with [cameraInfo]
         * @throws IllegalArgumentException if [cameraInfo] does not contain Camera2 information
         */
        @JvmStatic
        public fun getCameraCharacteristics(cameraInfo: CameraInfo): CameraCharacteristics {
            val characteristics = cameraInfo.unwrapAs(CameraCharacteristics::class.java)
            requireNotNull(characteristics) {
                "Could not unwrap $cameraInfo as CameraCharacteristics!"
            }
            return characteristics
        }

        /**
         * Creates a [CameraSelector] targeting a specific Camera2 camera ID.
         *
         * Example:
         * ```java
         * CameraSelector cameraSelector = Camera2Interop.getCameraSelectorFromCameraId("0");
         * cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase);
         * ```
         *
         * @param cameraId target Camera2 camera ID
         * @return [CameraSelector] that filters for [cameraId]
         */
        @JvmStatic
        public fun getCameraSelectorFromCameraId(cameraId: String): CameraSelector {
            return CameraSelector.Builder()
                .addCameraFilter(getCameraFilterFromCameraId(cameraId))
                .build()
        }

        /**
         * Creates a [CameraFilter] that matches a specific Camera2 camera ID.
         *
         * @param cameraId target Camera2 camera ID
         * @return [CameraFilter] matching [cameraId]
         */
        @JvmStatic
        public fun getCameraFilterFromCameraId(cameraId: String): CameraFilter {
            return CameraFilter { cameraInfos ->
                val filtered = ArrayList<CameraInfo>()
                for (cameraInfo in cameraInfos) {
                    try {
                        if (getCameraId(cameraInfo) == cameraId) {
                            filtered.add(cameraInfo)
                        }
                    } catch (e: IllegalArgumentException) {
                        // Ignore non-Camera2 cameras
                    }
                }
                filtered
            }
        }

        /**
         * Creates a [UseCaseCamera2Configurator] to modify Preview, ImageAnalysis, and VideoCapture
         * builders.
         *
         * Use this with [androidx.camera.core.UseCase.InteropConfigurable.setInterop] in Java to
         * apply Camera2 options. The [UseCaseCamera2Interop] target allows configuring options such
         * as:
         * - Physical camera ID ([UseCaseCamera2Interop.setPhysicalCameraId])
         * - Stream use case ([UseCaseCamera2Interop.setStreamUseCase])
         * - Mirror mode ([UseCaseCamera2Interop.setMirrorMode])
         * - Timestamp base ([UseCaseCamera2Interop.setTimestampBase])
         * - Dynamic range profile ([UseCaseCamera2Interop.setDynamicRangeProfile])
         * - Surface group ID ([UseCaseCamera2Interop.setSurfaceGroupId])
         *
         * For [androidx.camera.core.ImageCapture.Builder], use [forImageCapture].
         *
         * @param configurator the callback applying Camera2 options to [UseCaseCamera2Interop]
         * @return the [UseCaseCamera2Configurator] to pass to
         *   [androidx.camera.core.UseCase.InteropConfigurable.setInterop]
         */
        @JvmStatic
        public fun forUseCase(
            configurator: Consumer<UseCaseCamera2Interop>
        ): UseCaseCamera2Configurator {
            return UseCaseCamera2Configurator { interop -> configurator.accept(interop) }
        }

        /**
         * Creates an [ImageCaptureCamera2Configurator] to modify ImageCapture builders.
         *
         * Use this with [androidx.camera.core.ImageCapture.Builder.setInterop] in Java to apply
         * Camera2 options. The [ImageCaptureCamera2Interop] target allows configuring options such
         * as:
         * - Physical camera ID ([ImageCaptureCamera2Interop.setPhysicalCameraId])
         * - Stream use case ([ImageCaptureCamera2Interop.setStreamUseCase])
         * - Mirror mode ([ImageCaptureCamera2Interop.setMirrorMode])
         * - Timestamp base ([ImageCaptureCamera2Interop.setTimestampBase])
         * - Dynamic range profile ([ImageCaptureCamera2Interop.setDynamicRangeProfile])
         * - Surface group ID ([ImageCaptureCamera2Interop.setSurfaceGroupId])
         * - Still capture request key-value pairs
         *   ([ImageCaptureCamera2Interop.setStillCaptureRequestOption])
         * - Still capture request template type
         *   ([ImageCaptureCamera2Interop.setStillCaptureRequestTemplateType])
         * - Still capture callbacks ([ImageCaptureCamera2Interop.setStillCaptureCallback])
         *
         * The capture request keys for one-shot still captures (such as
         * [androidx.camera.core.ImageCapture.takePicture]) are determined by copying all repeating
         * request keys (which may include keys added via
         * [androidx.camera.core.SessionConfig.Builder.setInterop] or
         * [androidx.camera.core.CameraControl.applyInteropAsync]) and then overriding them with the
         * still capture request keys configured here.
         *
         * **Warning:** Callbacks configured via interop receive raw
         * [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking
         * state-altering methods on these raw objects (such as
         * [android.hardware.camera2.CameraCaptureSession.close] or
         * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
         * management and may cause state desynchronization, stream interruption, or application
         * crashes.
         *
         * @param configurator the callback applying Camera2 options to [ImageCaptureCamera2Interop]
         * @return the [ImageCaptureCamera2Configurator] to pass to
         *   [androidx.camera.core.ImageCapture.Builder.setInterop]
         */
        @JvmStatic
        public fun forImageCapture(
            configurator: Consumer<ImageCaptureCamera2Interop>
        ): ImageCaptureCamera2Configurator {
            return ImageCaptureCamera2Configurator { interop -> configurator.accept(interop) }
        }

        /**
         * Creates a [SessionConfigCamera2Configurator] to modify a session configuration builder.
         *
         * Use this with [androidx.camera.core.SessionConfig.Builder.setInterop] in Java to apply
         * Camera2 options to a custom session configuration. The [SessionConfigCamera2Interop]
         * target allows configuring options such as:
         * - Session parameters ([SessionConfigCamera2Interop.setSessionParameter])
         * - Session type ([SessionConfigCamera2Interop.setSessionType])
         * - Color space ([SessionConfigCamera2Interop.setColorSpace])
         * - Session state callbacks ([SessionConfigCamera2Interop.setSessionStateCallback])
         * - Device state callbacks ([SessionConfigCamera2Interop.setDeviceStateCallback])
         * - Capture request key-value pairs ([SessionConfigCamera2Interop.setCaptureRequestOption])
         * - Clearing specific capture request keys
         *   ([SessionConfigCamera2Interop.clearCaptureRequestOption])
         * - Clearing all capture request keys
         *   ([SessionConfigCamera2Interop.clearAllCaptureRequestOptions])
         * - Capture request template type
         *   ([SessionConfigCamera2Interop.setRepeatingCaptureRequestTemplate])
         * - Repeating capture callbacks ([SessionConfigCamera2Interop.setRepeatingCaptureCallback])
         *
         * **Warning:** Callbacks configured via interop receive raw
         * [android.hardware.camera2.CameraDevice] and
         * [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking
         * state-altering methods on these raw objects (such as
         * [android.hardware.camera2.CameraCaptureSession.close],
         * [android.hardware.camera2.CameraCaptureSession.abortCaptures], or
         * [android.hardware.camera2.CameraDevice.close]) bypasses CameraX pipeline management and
         * may cause state desynchronization, stream interruption, or application crashes.
         *
         * @param configurator the callback applying Camera2 options to
         *   [SessionConfigCamera2Interop]
         * @return the [SessionConfigCamera2Configurator] to pass to
         *   [androidx.camera.core.SessionConfig.Builder.setInterop]
         */
        @JvmStatic
        public fun forSessionConfig(
            configurator: Consumer<SessionConfigCamera2Interop>
        ): SessionConfigCamera2Configurator {
            return SessionConfigCamera2Configurator { interop -> configurator.accept(interop) }
        }

        /**
         * Creates a [CameraControlCamera2Configurator] to modify a
         * [androidx.camera.core.CameraControl].
         *
         * Use this with [androidx.camera.core.CameraControl.applyInteropAsync] to dynamically send
         * Camera2 capture request options to an active camera. All options configured within a
         * single [configurator] are applied atomically in a single repeating capture request
         * update. Subsequent calls to [androidx.camera.core.CameraControl.applyInteropAsync] update
         * parameters incrementally without clearing previously set keys, unless explicitly cleared
         * using [CameraControlCamera2Interop.clearCaptureRequestOption] or
         * [CameraControlCamera2Interop.clearAllCaptureRequestOptions]. This overwrites options set
         * with [SessionConfigCamera2Interop] via
         * [androidx.camera.core.SessionConfig.Builder.setInterop].
         *
         * The [CameraControlCamera2Interop] target allows configuring options such as:
         * - Capture request key-value pairs ([CameraControlCamera2Interop.setCaptureRequestOption])
         * - Clearing specific capture request keys
         *   ([CameraControlCamera2Interop.clearCaptureRequestOption])
         * - Clearing all capture request keys
         *   ([CameraControlCamera2Interop.clearAllCaptureRequestOptions])
         * - Capture request template type
         *   ([CameraControlCamera2Interop.setRepeatingCaptureRequestTemplate] or
         *   [CameraControlCamera2Interop.repeatingCaptureRequestTemplate])
         * - Repeating capture callbacks ([CameraControlCamera2Interop.setRepeatingCaptureCallback]
         *   or [CameraControlCamera2Interop.repeatingCaptureCallback])
         *
         * **Warning:** Callbacks configured via interop receive raw
         * [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking
         * state-altering methods on these raw objects (such as
         * [android.hardware.camera2.CameraCaptureSession.close] or
         * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
         * management and may cause state desynchronization, stream interruption, or application
         * crashes.
         *
         * @param configurator the callback applying Camera2 options to
         *   [CameraControlCamera2Interop]
         * @return the [CameraControlCamera2Configurator] to pass to
         *   [androidx.camera.core.CameraControl.applyInteropAsync]
         */
        @JvmStatic
        public fun forCameraControl(
            configurator: Consumer<CameraControlCamera2Interop>
        ): CameraControlCamera2Configurator {
            return CameraControlCamera2Configurator { interop -> configurator.accept(interop) }
        }
    }

    /**
     * Extends [baseBuilder] to add Camera2 options.
     *
     * @param T the type being built by the extendable builder.
     * @param baseBuilder The builder being extended.
     * @constructor Creates an Extender that can be used to add Camera2 options to another Builder.
     */
    @ExperimentalCamera2Interop
    public class Extender<T>(private var baseBuilder: ExtendableBuilder<T>) {

        /**
         * Sets a [CaptureRequest.Key] and Value on the configuration.
         *
         * The value will override any value set by CameraX internally with the risk of interfering
         * with some CameraX CameraControl APIs as well as 3A behavior. When applied to an
         * [androidx.camera.core.ImageCapture.Builder], options set for still capture requests
         * override corresponding keys set in repeating requests.
         *
         * @param key The [CaptureRequest.Key] which will be set.
         * @param value The value for the key.
         * @param ValueT The type of the value.
         * @return The current Extender.
         */
        public fun <ValueT> setCaptureRequestOption(
            key: CaptureRequest.Key<ValueT>,
            value: ValueT,
        ): Extender<T> {
            // Reify the type so we can obtain the class
            val opt = key.createCaptureRequestOption()
            baseBuilder.mutableConfig.insertOption(
                opt,
                Config.OptionPriority.ALWAYS_OVERRIDE,
                value,
            )
            return this
        }

        /**
         * Sets a CameraDevice template on the given configuration.
         *
         * See [CameraDevice] for valid template types. For example,
         * [CameraDevice.TEMPLATE_PREVIEW].
         *
         * Only used by [androidx.camera.core.ImageCapture] to set the template type used. For all
         * other [androidx.camera.core.UseCase] this value is ignored.
         *
         * @param templateType The template type to set.
         * @return The current Extender.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun setCaptureRequestTemplate(templateType: Int): Extender<T> {
            baseBuilder.mutableConfig.insertOption(
                Camera2ImplConfig.TEMPLATE_TYPE_OPTION,
                templateType,
            )
            return this
        }

        /**
         * Sets a stream use case flag on the given extendable builder.
         *
         * Requires API 33 or above.
         *
         * Calling this method will set the stream useCase for the stream associated with the
         * surface whose container class is the UseCase. Valid use cases available on devices can be
         * found in
         * [android.hardware.camera2.CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES] The
         * app should make sure the input argument is in the list of supported use cases first.
         *
         * If a unsupported value is provided, [IllegalArgumentException] will be thrown.
         *
         * @param streamUseCase The stream use case to set.
         * @return The current Extender.
         * @see android.hardware.camera2.params.OutputConfiguration.setStreamUseCase to see how
         *   Camera2 framework uses this.
         */
        @RequiresApi(33)
        public fun setStreamUseCase(streamUseCase: Long): Extender<T> {
            baseBuilder.mutableConfig.insertOption(
                Camera2ImplConfig.STREAM_USE_CASE_OPTION,
                streamUseCase,
            )
            return this
        }

        /**
         * Sets a [CameraDevice.StateCallback].
         *
         * The caller is expected to use the [CameraDevice] instance accessed through the callback
         * methods responsibly. Generally safe usages include: (1) querying the device for its id,
         * (2) using the callbacks to determine what state the device is currently in. Generally
         * unsafe usages include: (1) creating a new [CameraCaptureSession], (2) creating a new
         * [CaptureRequest], (3) closing the device. When the caller uses the device beyond the safe
         * usage limits, the usage may still work in conjunction with CameraX, but any strong
         * guarantees provided by CameraX about the validity of the camera state become void.
         *
         * @param stateCallback The [CameraDevice.StateCallback].
         * @return The current Extender.
         */
        @SuppressLint("ExecutorRegistration")
        public fun setDeviceStateCallback(stateCallback: CameraDevice.StateCallback): Extender<T> {
            baseBuilder.mutableConfig.insertOption(
                Camera2ImplConfig.DEVICE_STATE_CALLBACK_OPTION,
                stateCallback,
            )
            return this
        }

        /**
         * Sets a [CameraCaptureSession.StateCallback].
         *
         * The caller is expected to use the [CameraCaptureSession] instance accessed through the
         * callback methods responsibly. Generally safe usages include: (1) querying the session for
         * its properties, (2) using the callbacks to determine what state the session is currently
         * in. Generally unsafe usages include: (1) submitting a new [CameraCaptureSession], (2)
         * stopping an existing [CaptureRequest], (3) closing the session, (4) attaching a new
         * [android.view.Surface] to the session. When the caller uses the session beyond the safe
         * usage limits, the usage may still work in conjunction with CameraX, but any strong
         * guarantees provided by CameraX about the validity of the camera state become void.
         *
         * @param stateCallback The [CameraCaptureSession.StateCallback].
         * @return The current Extender.
         */
        @SuppressLint("ExecutorRegistration")
        public fun setSessionStateCallback(
            stateCallback: CameraCaptureSession.StateCallback
        ): Extender<T> {
            baseBuilder.mutableConfig.insertOption(
                Camera2ImplConfig.SESSION_STATE_CALLBACK_OPTION,
                stateCallback,
            )
            return this
        }

        /**
         * Sets a [CameraCaptureSession.CaptureCallback].
         *
         * The caller is expected to use the [CameraCaptureSession] instance accessed through the
         * callback methods responsibly. Generally safe usages include: (1) querying the session for
         * its properties. Generally unsafe usages include: (1) submitting a new [CaptureRequest],
         * (2) stopping an existing [CaptureRequest], (3) closing the session, (4) attaching a new
         * [android.view.Surface] to the session. When the caller uses the session beyond the safe
         * usage limits, the usage may still work in conjunction with CameraX, but any strong
         * guarantees provided by CameraX about the validity of the camera state become void.
         *
         * The caller is generally free to use the [CaptureRequest] and [CaptureRequest] instances
         * accessed through the callback methods.
         *
         * @param captureCallback The [CameraCaptureSession.CaptureCallback].
         * @return The current Extender.
         */
        @SuppressLint("ExecutorRegistration")
        public fun setSessionCaptureCallback(captureCallback: CaptureCallback): Extender<T> {
            baseBuilder.mutableConfig.insertOption(
                Camera2ImplConfig.SESSION_CAPTURE_CALLBACK_OPTION,
                captureCallback,
            )
            return this
        }

        /**
         * Set the ID of the physical camera to get output from.
         *
         * In the case one logical camera is made up of multiple physical cameras, this call forces
         * the physical camera with the specified camera ID to produce image.
         *
         * The valid physical camera IDs can be queried by `CameraCharacteristics
         * .getPhysicalCameraIds` on API &gt;= 28. Passing in an invalid physical camera ID will be
         * ignored.
         *
         * On API &lt;= 27, the physical camera ID will be ignored since logical camera is not
         * supported on these API levels.
         *
         * Currently it doesn't support binding use cases with different physical camera IDs. If use
         * cases with different physical camera IDs are bound at the same time, an
         * [IllegalArgumentException] will be thrown.
         *
         * @param cameraId The desired camera ID.
         * @return The current Extender.
         */
        @RequiresApi(28)
        public fun setPhysicalCameraId(
            @Suppress("UNUSED_PARAMETER") cameraId: String
        ): Extender<T> {
            baseBuilder.mutableConfig.insertOption(
                Camera2ImplConfig.SESSION_PHYSICAL_CAMERA_ID_OPTION,
                cameraId,
            )
            return this
        }
    }
}
