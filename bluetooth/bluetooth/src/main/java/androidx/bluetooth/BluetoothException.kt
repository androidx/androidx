/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.bluetooth

import android.bluetooth.BluetoothStatusCodes
import kotlin.coroutines.cancellation.CancellationException

/**
 * Exception for general Bluetooth operations
 *
 * @property errorCode the error code for indicating the reason why the exception is thrown
 */
open class BluetoothException(
    open val errorCode: Int,
    message: String? = null,
    cause: Throwable? = null
) : CancellationException(message) {
    companion object {
        /** Error code indicating that Bluetooth is not enabled. */
        const val BLUETOOTH_NOT_ENABLED = BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED

        /**
         * Error code indicating that the API call was initiated by neither the system nor the
         * active user.
         */
        const val BLUETOOTH_NOT_ALLOWED = BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED

        /** Error code indicating that the Bluetooth Device specified is not bonded. */
        const val DEVICE_NOT_BONDED = BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED

        /**
         * Error code indicating that the Bluetooth Device specified is not connected, but is
         * bonded.
         */
        const val DEVICE_NOT_CONNECTED = 4

        /** Indicates that the feature is not supported. */
        const val FEATURE_NOT_SUPPORTED = BluetoothStatusCodes.FEATURE_NOT_SUPPORTED

        /** Indicates that the feature status is not configured yet. */
        const val FEATURE_NOT_CONFIGURED = BluetoothStatusCodes.FEATURE_NOT_CONFIGURED

        /** Indicates that an unknown error has occurred. */
        const val ERROR_UNKNOWN = Int.MAX_VALUE
    }

    init {
        cause?.let { this.initCause(it) }
    }
}
