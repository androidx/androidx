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

package androidx.window.area

import androidx.window.core.ExperimentalWindowApi
import androidx.window.extensions.area.WindowAreaComponent

/**
 * Represents a window area status.
 */
@ExperimentalWindowApi
class WindowAreaStatus private constructor(private val mDescription: String) {
    override fun toString(): String {
        return mDescription
    }

    companion object {
        /**
         * Status representing that the WindowArea feature is not a supported
         * feature on the device.
         */
        @JvmField
        val UNSUPPORTED = WindowAreaStatus("UNSUPPORTED")

        /**
         * Status representing that the WindowArea feature is currently not available
         * to be enabled. This could be due to another process has enabled it, or that the
         * current device configuration doesn't allow it.
         */
        @JvmField
        val UNAVAILABLE = WindowAreaStatus("UNAVAILABLE")

        /**
         * Status representing that the WindowArea feature is available to be enabled.
         */
        @JvmField
        val AVAILABLE = WindowAreaStatus("AVAILABLE")

        @JvmStatic
        internal fun translate(status: Int): WindowAreaStatus {
            return when (status) {
                WindowAreaComponent.STATUS_AVAILABLE -> AVAILABLE
                WindowAreaComponent.STATUS_UNAVAILABLE -> UNAVAILABLE
                else -> UNSUPPORTED
            }
        }
    }
}