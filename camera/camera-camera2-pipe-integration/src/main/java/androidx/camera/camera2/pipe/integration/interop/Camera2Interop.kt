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
package androidx.camera.camera2.pipe.integration.interop

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.integration.impl.DEVICE_STATE_CALLBACK_OPTION
import androidx.camera.camera2.pipe.integration.impl.SESSION_CAPTURE_CALLBACK_OPTION
import androidx.camera.camera2.pipe.integration.impl.SESSION_PHYSICAL_CAMERA_ID_OPTION
import androidx.camera.camera2.pipe.integration.impl.SESSION_STATE_CALLBACK_OPTION
import androidx.camera.camera2.pipe.integration.impl.STREAM_USE_CASE_OPTION
import androidx.camera.camera2.pipe.integration.impl.TEMPLATE_TYPE_OPTION
import androidx.camera.camera2.pipe.integration.impl.createCaptureRequestOption
import androidx.camera.core.ExtendableBuilder
import androidx.camera.core.impl.Config

/**
 * Utilities related to interoperability with the [android.hardware.camera2] APIs.
 *
 * @constructor Private constructor to ensure this class isn't instantiated.
 */
@ExperimentalCamera2Interop
public class Camera2Interop private constructor() {

    /**
     * Extends a [ExtendableBuilder] to add Camera2 options.
     *
     * @param T the type being built by the extendable builder.
     * @constructor Creates an Extender that can be used to add Camera2 options to another Builder.
     * @property baseBuilder The builder being extended.
     */
    public class Extender<T>(private var baseBuilder: ExtendableBuilder<T>) {

        /**
         * Sets a [CaptureRequest.Key] and Value on the configuration.
         *
         * The value will override any value set by CameraX internally with the risk of interfering
         * with some CameraX CameraControl APIs as well as 3A behavior.
         *
         * @param key The [CaptureRequest.Key] which will be set.
         * @param value The value for the key.
         * @param ValueT The type of the value.
         * @return The current Extender.
         */
        public fun <ValueT> setCaptureRequestOption(
            key: CaptureRequest.Key<ValueT>,
            value: ValueT
        ): Extender<T> {
            // Reify the type so we can obtain the class
            val opt = key.createCaptureRequestOption()
            baseBuilder.mutableConfig.insertOption(
                opt,
                Config.OptionPriority.ALWAYS_OVERRIDE,
                value
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
            baseBuilder.mutableConfig.insertOption(TEMPLATE_TYPE_OPTION, templateType)
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
            baseBuilder.mutableConfig.insertOption(STREAM_USE_CASE_OPTION, streamUseCase)
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
            baseBuilder.mutableConfig.insertOption(DEVICE_STATE_CALLBACK_OPTION, stateCallback)
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
            baseBuilder.mutableConfig.insertOption(SESSION_STATE_CALLBACK_OPTION, stateCallback)
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
            baseBuilder.mutableConfig.insertOption(SESSION_CAPTURE_CALLBACK_OPTION, captureCallback)
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
            baseBuilder.mutableConfig.insertOption(SESSION_PHYSICAL_CAMERA_ID_OPTION, cameraId)
            return this
        }
    }
}
