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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions.Builder.Companion.from
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.ExtendableBuilder
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.MutableConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OptionsBundle

internal const val CAPTURE_REQUEST_ID_STEM = "camera2.captureRequest.option."
internal val TEMPLATE_TYPE_OPTION = Config.Option.create<Int>(
    "camera2.captureRequest.templateType",
    Int::class.javaPrimitiveType!!
)
internal val DEVICE_STATE_CALLBACK_OPTION = Config.Option.create<CameraDevice.StateCallback>(
    "camera2.cameraDevice.stateCallback",
    CameraDevice.StateCallback::class.java
)
internal val SESSION_STATE_CALLBACK_OPTION =
    Config.Option.create<CameraCaptureSession.StateCallback>(
        "camera2.cameraCaptureSession.stateCallback",
        CameraCaptureSession.StateCallback::class.java
    )
internal val SESSION_CAPTURE_CALLBACK_OPTION = Config.Option.create<CaptureCallback>(
    "camera2.cameraCaptureSession.captureCallback",
    CaptureCallback::class.java
)
internal val CAPTURE_REQUEST_TAG_OPTION = Config.Option.create<Any>(
    "camera2.captureRequest.tag", Any::class.java
)
// TODO: Porting the CameraEventCallback option constant.

/**
 * Internal shared implementation details for camera 2 interop.
 *
 * @property config The config that potentially contains Camera2 options.
 * @constructor Creates a Camera2ImplConfig for reading Camera2 options from the given config.
 */
@OptIn(ExperimentalCamera2Interop::class)
class Camera2ImplConfig(config: Config) : CaptureRequestOptions(config) {

    /**
     * Returns all capture request options contained in this configuration.
     *
     * @hide
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val captureRequestOptions: CaptureRequestOptions
        get() = from(config).build()

    /**
     * Returns the CameraDevice template from the given configuration.
     *
     * See [CameraDevice] for valid template types. For example, [CameraDevice.TEMPLATE_PREVIEW].
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or `valueIfMissing` if the value does not exist in this
     * configuration.
     */
    fun getCaptureRequestTemplate(valueIfMissing: Int): Int {
        return config.retrieveOption(TEMPLATE_TYPE_OPTION, valueIfMissing)!!
    }

    /**
     * Returns the stored [CameraDevice.StateCallback].
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * Defaults to `null`.
     * @return The stored value or `valueIfMissing` if the value does not exist in this
     * configuration.
     */
    fun getDeviceStateCallback(
        valueIfMissing: CameraDevice.StateCallback? = null
    ): CameraDevice.StateCallback? {
        return config.retrieveOption(DEVICE_STATE_CALLBACK_OPTION, valueIfMissing)
    }

    /**
     * Returns the stored [CameraCaptureSession.StateCallback].
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * Defaults to `null`.
     * @return The stored value or `valueIfMissing` if the value does not exist in this
     * configuration.
     */
    fun getSessionStateCallback(
        valueIfMissing: CameraCaptureSession.StateCallback? = null
    ): CameraCaptureSession.StateCallback? {
        return config.retrieveOption(SESSION_STATE_CALLBACK_OPTION, valueIfMissing)
    }

    /**
     * Returns the stored [CameraCaptureSession.CaptureCallback].
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * Defaults to `null`.
     * @return The stored value or `valueIfMissing` if the value does not exist in this
     * configuration.
     */
    fun getSessionCaptureCallback(
        valueIfMissing: CaptureCallback? = null
    ): CaptureCallback? {
        return config.retrieveOption(SESSION_CAPTURE_CALLBACK_OPTION, valueIfMissing)
    }

    // TODO: Prepare a getter for CameraEventCallbacks

    /**
     * Returns the capture request tag.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * Defaults to `null`.
     * @return The stored value or `valueIfMissing` if the value does not exist in this
     * configuration.
     */
    fun getCaptureRequestTag(valueIfMissing: Any? = null): Any? {
        return config.retrieveOption(CAPTURE_REQUEST_TAG_OPTION, valueIfMissing)
    }

    /**
     * Builder for creating [Camera2ImplConfig] instance.
     *
     * Use [Builder] for creating [Config] which contains
     * camera2 options only. And use
     * [androidx.camera.camera2.pipe.integration.interop.Camera2Interop.Extender] to add
     * Camera2 options on existing other [ExtendableBuilder].
     */
    class Builder : ExtendableBuilder<Camera2ImplConfig?> {

        private val mutableOptionsBundle = MutableOptionsBundle.create()

        override fun getMutableConfig(): MutableConfig {
            return mutableOptionsBundle
        }

        /**
         * Inserts new capture request option with specific [CaptureRequest.Key] setting.
         */
        fun <ValueT> setCaptureRequestOption(
            key: CaptureRequest.Key<ValueT>,
            value: ValueT
        ): Builder {
            val opt = key.createCaptureRequestOption()
            mutableOptionsBundle.insertOption(opt, value)
            return this
        }

        /**
         * Inserts new capture request option with specific [CaptureRequest.Key] setting and
         * [Config.OptionPriority].
         */
        fun <ValueT> setCaptureRequestOptionWithPriority(
            key: CaptureRequest.Key<ValueT>,
            value: ValueT,
            priority: Config.OptionPriority
        ): Builder {
            val opt = key.createCaptureRequestOption()
            mutableOptionsBundle.insertOption(opt, priority, value)
            return this
        }

        /**
         * Inserts all capture request options in the map to the setting with
         * [Config.OptionPriority].
         */
        fun addAllCaptureRequestOptionsWithPriority(
            values: Map<CaptureRequest.Key<*>, Any>,
            priority: Config.OptionPriority
        ): Builder {
            values.forEach { (key, value) ->
                val opt = key.createCaptureRequestOption()
                mutableOptionsBundle.insertOption(opt, priority, value)
            }
            return this
        }

        /** Inserts options from other [Config] objects. */
        fun insertAllOptions(config: Config): Builder {
            for (option in config.listOptions()) {
                // Options/values and priority are being copied directly
                @Suppress("UNCHECKED_CAST")
                val objectOpt = option as Config.Option<Any>
                mutableOptionsBundle.insertOption(
                    objectOpt, config.getOptionPriority(option), config.retrieveOption(objectOpt)
                )
            }
            return this
        }

        /**
         * Builds an immutable [Camera2ImplConfig] from the current state.
         *
         * @return A [Camera2ImplConfig] populated with the current state.
         */
        override fun build(): Camera2ImplConfig {
            return Camera2ImplConfig(OptionsBundle.from(mutableOptionsBundle))
        }
    }

    // TODO: Prepare a setter for CameraEventCallbacks, ex: setCameraEventCallback
}

internal fun CaptureRequest.Key<*>.createCaptureRequestOption(): Config.Option<Any> {
    /**
     * Unfortunately, we can't get the Class<T> from the CaptureRequest.Key, so we're forced to
     * erase the type. This shouldn't be a problem as long as we are only using these
     * options within the Camera2ImplConfig and Camera2ImplConfig.Builder classes.
     */
    return Config.Option.create(CAPTURE_REQUEST_ID_STEM + name, Any::class.java, this)
}

/**
 * Convert the Config to the CaptureRequest key-value map.
 */
fun Config.toParameters(): Map<CaptureRequest.Key<*>, Any> {
    val parameters = mutableMapOf<CaptureRequest.Key<*>, Any>()
    for (configOption in listOptions()) {
        val requestKey = configOption.token as? CaptureRequest.Key<*> ?: continue
        val value = retrieveOption(configOption) ?: continue
        parameters[requestKey] = value
    }

    return parameters
}