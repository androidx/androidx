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

import android.bluetooth.BluetoothAdapter as FwkBluetoothAdapter
import android.bluetooth.le.ScanCallback as FwkScanCallback
import android.bluetooth.le.ScanFilter as FwkScanFilter
import android.bluetooth.le.ScanSettings as FwkScanSettings
import android.Manifest
import androidx.annotation.RequiresPermission

/**
 * This class provides methods to perform scan related operations for Bluetooth LE devices. An
 * application can scan for a particular type of Bluetooth LE devices using [FwkScanFilter]. It
 * can also request different types of callbacks for delivering the result.
 *
 * Use [FwkBluetoothAdapter.getBluetoothLeScanner] to get an instance of
 * [BluetoothLeScanner].
 *
 * @see FwkScanFilter
 *
 * @hide
 */
// TODO(ofy) Change FwkScanFilter to core.ScanFilter when it is available
// TODO(ofy) Change FwkBluetoothAdapter to core.BluetoothAdapter when it is available
class BluetoothLeScanner(fwkBluetoothAdapter: FwkBluetoothAdapter) {

    companion object {
        private const val TAG = "BluetoothLeScanner"
        private const val DBG = true
        private const val VDBG = false

        /**
         * Extra containing a list of ScanResults. It can have one or more results if there was no
         * error. In case of error, [.EXTRA_ERROR_CODE] will contain the error code and this
         * extra will not be available.
         */
        const val EXTRA_LIST_SCAN_RESULT = "android.bluetooth.le.extra.LIST_SCAN_RESULT"

        /**
         * Optional extra indicating the error code, if any. The error code will be one of the
         * SCAN_FAILED_* codes in [ScanCallback].
         */
        const val EXTRA_ERROR_CODE = "android.bluetooth.le.extra.ERROR_CODE"

        /**
         * Optional extra indicating the callback type, which will be one of
         * CALLBACK_TYPE_* constants in [FwkScanSettings].
         *
         * @see ScanCallback.onScanResult
         */
        // TODO(ofy) Change FwkScanSettings to core.ScanSettings when it is available
        const val EXTRA_CALLBACK_TYPE = "android.bluetooth.le.extra.CALLBACK_TYPE"
    }

    private val fwkBluetoothLeScanner = fwkBluetoothAdapter.bluetoothLeScanner

    /**
     * Start Bluetooth LE scan with default parameters and no filters. The scan results will be
     * delivered through `callback`. For unfiltered scans, scanning is stopped on screen
     * off to save power. Scanning is resumed when screen is turned on again. To avoid this, use
     * [.startScan] with desired [FwkScanFilter].
     *
     *
     * An app must have
     * [ACCESS_COARSE_LOCATION][android.Manifest.permission.ACCESS_COARSE_LOCATION] permission
     * in order to get results. An App targeting Android Q or later must have
     * [ACCESS_FINE_LOCATION][android.Manifest.permission.ACCESS_FINE_LOCATION] permission
     * in order to get results.
     *
     * @param callback Callback used to deliver scan results.
     * @throws IllegalArgumentException If `callback` is null.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    // TODO(ofy) Change FwkScanCallback to core.ScanCallback when it is available
    fun startScan(callback: FwkScanCallback) {
        fwkBluetoothLeScanner.startScan(callback)
    }

    // TODO(ofy) Add remainder of BluetoothLeScanner
    // ...
}
