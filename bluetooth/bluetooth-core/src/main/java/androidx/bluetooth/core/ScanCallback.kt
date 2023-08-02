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

import android.bluetooth.le.ScanCallback as FwkScanCallback
import android.bluetooth.le.ScanResult as FwkScanResult

import androidx.annotation.IntDef

/**
 * Bluetooth LE scan callbacks. Scan results are reported using these callbacks.
 *
 * @see BluetoothLeScanner#startScan
 *
 * @hide
 */
interface ScanCallback {

    companion object {
        /**
         * Fails to start scan as BLE scan with the same settings is already started by the app.
         */
        const val SCAN_FAILED_ALREADY_STARTED =
            FwkScanCallback.SCAN_FAILED_ALREADY_STARTED

        /**
         * Fails to start scan as app cannot be registered.
         */
        const val SCAN_FAILED_APPLICATION_REGISTRATION_FAILED =
            FwkScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED

        /**
         * Fails to start scan due an internal error
         */
        const val SCAN_FAILED_INTERNAL_ERROR =
            FwkScanCallback.SCAN_FAILED_INTERNAL_ERROR

        /**
         * Fails to start power optimized scan as this feature is not supported.
         */
        const val SCAN_FAILED_FEATURE_UNSUPPORTED =
            FwkScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED

        /**
         * Fails to start scan as it is out of hardware resources.
         */
        // Added in API level 33
        // FwkScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES
        const val SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5

        /**
         * Fails to start scan as application tries to scan too frequently.
         */
        // Added in API level 33
        // FwkScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY
        const val SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6

        const val NO_ERROR = 0
    }

    // Le Roles
    /**
     * @hide
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        SCAN_FAILED_ALREADY_STARTED,
        SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,
        SCAN_FAILED_INTERNAL_ERROR,
        SCAN_FAILED_FEATURE_UNSUPPORTED,
        SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES,
        SCAN_FAILED_SCANNING_TOO_FREQUENTLY
    )
    annotation class ScanFailed

    /**
     * Callback when a BLE advertisement has been found.
     *
     * @param callbackType Determines how this callback was triggered. Could be one of {@link
     * ScanSettings#CALLBACK_TYPE_ALL_MATCHES}, {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
     * {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
     * @param result A Bluetooth LE scan result.
     */
    // TODO(ofy) Change FwkScanResult to core.ScanResult when it is available
    fun onScanResult(callbackType: Int, result: FwkScanResult) {}

    /**
     * Callback when batch results are delivered.
     *
     * @param results List of scan results that are previously scanned.
     */
    // TODO(ofy) Change FwkScanResult to core.ScanResult when it is available
    fun onBatchScanResults(results: List<FwkScanResult>) {}

    /**
     * Callback when scan could not be started.
     *
     * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
     */
    fun onScanFailed(@ScanFailed errorCode: Int) {}
}
