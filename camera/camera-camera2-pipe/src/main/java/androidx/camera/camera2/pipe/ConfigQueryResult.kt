/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo

/**
 * Represents the result of a camera configuration support query.
 *
 * This is an inline class for performance, avoiding object allocation for query results.
 *
 * @see CameraPipe.isConfigSupported
 */
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public value class ConfigQueryResult(internal val value: Int) {
    override fun toString(): String {
        return when (this) {
            SUPPORTED -> "SUPPORTED"
            UNSUPPORTED -> "UNSUPPORTED"
            else -> "UNKNOWN"
        }
    }

    public companion object {
        /**
         * It is unknown whether the camera configuration is supported. This is returned on devices
         * below API 35, or when the configuration contains parameters that the framework cannot
         * query (i.e. non-queryable settings according to
         * [android.hardware.camera2.CameraCharacteristics.INFO_SESSION_CONFIGURATION_QUERY_VERSION]
         * doc), e.g. 45 FPS, ImageFormat.RAW etc.
         *
         * Corresponds to
         * [androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.RESULT_UNDEFINED]
         */
        public val UNKNOWN: ConfigQueryResult = ConfigQueryResult(0)

        /**
         * The camera configuration is definitively supported by the device.
         *
         * Corresponds to
         * [androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.RESULT_SUPPORTED]
         */
        public val SUPPORTED: ConfigQueryResult = ConfigQueryResult(1)

        /**
         * The camera configuration is definitively not supported by the device.
         *
         * Corresponds to
         * [androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.RESULT_UNSUPPORTED]
         */
        public val UNSUPPORTED: ConfigQueryResult = ConfigQueryResult(2)
    }
}
