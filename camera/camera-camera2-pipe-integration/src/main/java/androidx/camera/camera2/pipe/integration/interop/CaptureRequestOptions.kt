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
package androidx.camera.camera2.pipe.integration.interop

import android.hardware.camera2.CaptureRequest
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.integration.impl.CAPTURE_REQUEST_ID_STEM
import androidx.camera.camera2.pipe.integration.impl.createCaptureRequestOption
import androidx.camera.core.ExtendableBuilder
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.MutableConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.ReadableConfig

/**
 * A bundle of Camera2 capture request options.
 *
 * @property config The config that potentially contains Camera2 capture request options.
 * @constructor Creates a CaptureRequestOptions for reading Camera2 capture request options from the
 * given config.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@ExperimentalCamera2Interop
open class CaptureRequestOptions(private val config: Config) : ReadableConfig {

    /**
     * Returns a value for the given [CaptureRequest.Key] or null if it hasn't been set.
     *
     * @param key            The key to retrieve.
     * @param <ValueT>       The type of the value.
     * @return The stored value or null if the value does not exist in this
     * configuration.
     */
    fun <ValueT> getCaptureRequestOption(key: CaptureRequest.Key<ValueT>): ValueT? {
        // Type should have been only set via Builder#setCaptureRequestOption()
        @Suppress("UNCHECKED_CAST")
        val opt = key.createCaptureRequestOption() as Config.Option<ValueT>
        return config.retrieveOption(opt, null)
    }

    /**
     * Returns a value for the given [CaptureRequest.Key].
     *
     * @param key            The key to retrieve.
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @param <ValueT>       The type of the value.
     * @return The stored value or `valueIfMissing` if the value does not exist in this
     * configuration.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun <ValueT> getCaptureRequestOption(
        key: CaptureRequest.Key<ValueT>,
        valueIfMissing: ValueT?
    ): ValueT? {
        // Type should have been only set via Builder#setCaptureRequestOption()
        @Suppress("UNCHECKED_CAST")
        val opt = key.createCaptureRequestOption() as Config.Option<ValueT>
        return config.retrieveOption(opt, valueIfMissing)
    }

    /**
     * Returns the [Config] object associated with this [CaptureRequestOptions].
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun getConfig(): Config {
        return config
    }

    /**
     * Builder for creating [CaptureRequestOptions] instance.
     */
    class Builder : ExtendableBuilder<CaptureRequestOptions?> {
        private val mutableOptionsBundle = MutableOptionsBundle.create()

        companion object {
            /**
             * Generates a Builder from another Config object.
             *
             * @param config An immutable configuration to pre-populate this builder.
             * @return The new Builder.
             * @hide
             */
            @JvmStatic
            @RestrictTo(RestrictTo.Scope.LIBRARY)
            fun from(config: Config): Builder {
                val bundleBuilder = Builder()
                config.findOptions(CAPTURE_REQUEST_ID_STEM) {
                    // Erase the type of the option. Capture request options should only be
                    // set via Camera2Interop so that the type of the key and value should
                    // always match.
                    @Suppress("UNCHECKED_CAST")
                    val objectOpt = it as Config.Option<Any>
                    bundleBuilder.mutableConfig.insertOption(
                        objectOpt,
                        config.getOptionPriority(objectOpt),
                        config.retrieveOption(objectOpt)
                    )
                    true
                }
                return bundleBuilder
            }
        }

        /**
         * {@inheritDoc}
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
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
         * Removes a capture request option with specific [CaptureRequest.Key] setting.
         */
        fun <ValueT> clearCaptureRequestOption(
            key: CaptureRequest.Key<ValueT>
        ): Builder {
            val opt = key.createCaptureRequestOption()
            mutableOptionsBundle.removeOption(opt)
            return this
        }

        /**
         * Builds an immutable [CaptureRequestOptions] from the current state.
         *
         * @return A [CaptureRequestOptions] populated with the current state.
         */
        override fun build(): CaptureRequestOptions {
            return CaptureRequestOptions(OptionsBundle.from(mutableOptionsBundle))
        }
    }
}