/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.bluetooth.core

import android.bluetooth.le.AdvertiseSettings as FwkAdvertiseSettings

/**
 * Bluetooth LE advertising callbacks, used to deliver advertising operation status.
 *
 * @hide
 */
interface AdvertiseCallback {

    companion object {
        /**
         * The requested operation was successful.
         */
        const val ADVERTISE_SUCCESS = 0

        /**
         * Failed to start advertising as the advertise data to be broadcasted is larger than 31
         * bytes.
         */
        const val ADVERTISE_FAILED_DATA_TOO_LARGE = 1

        /**
         * Failed to start advertising because no advertising instance is available.
         */
        const val ADVERTISE_FAILED_TOO_MANY_ADVERTISERS = 2

        /**
         * Failed to start advertising as the advertising is already started.
         */
        const val ADVERTISE_FAILED_ALREADY_STARTED = 3

        /**
         * Operation failed due to an internal error.
         */
        const val ADVERTISE_FAILED_INTERNAL_ERROR = 4

        /**
         * This feature is not supported on this platform.
         */
        const val ADVERTISE_FAILED_FEATURE_UNSUPPORTED = 5
    }

    /**
     * Callback triggered in response to {@link BluetoothLeAdvertiser#startAdvertising} indicating
     * that the advertising has been started successfully.
     *
     * @param settingsInEffect The actual settings used for advertising, which may be different from
     * what has been requested.
     */
    // TODO(kihongs) Change FwkAdvertiseSettings to core.AdvertiseSettings when it is available
    fun onStartSuccess(settingsInEffect: FwkAdvertiseSettings) {}

    /**
     * Callback when advertising could not be started.
     *
     * @param errorCode Error code (see ADVERTISE_FAILED_* constants) for advertising start
     * failure.
     */
    fun onStartFailure(errorCode: Int) {}
}