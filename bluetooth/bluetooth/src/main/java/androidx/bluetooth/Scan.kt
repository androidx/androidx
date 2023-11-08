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

import android.bluetooth.le.BluetoothLeScanner as FwkBluetoothLeScanner
import android.bluetooth.le.ScanCallback as FwkScanCallback
import android.bluetooth.le.ScanResult as FwkScanResult
import android.bluetooth.le.ScanSettings as FwkScanSettings
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ScanImpl {
    fun scan(filters: List<ScanFilter> = emptyList()): Flow<ScanResult>
}

internal fun getScanImpl(bluetoothLeScanner: FwkBluetoothLeScanner): ScanImpl {
    return ScanImplBase(bluetoothLeScanner)
}

private open class ScanImplBase(val bluetoothLeScanner: FwkBluetoothLeScanner) : ScanImpl {

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    override fun scan(filters: List<ScanFilter>): Flow<ScanResult> = callbackFlow {
        val callback = object : FwkScanCallback() {
            override fun onScanResult(callbackType: Int, result: FwkScanResult) {
                trySend(ScanResult(result))
            }

            override fun onScanFailed(errorCode: Int) {
                // TODO(b/270492198): throw precise exception
                cancel("onScanFailed() called with: errorCode = $errorCode")
            }
        }

        val fwkFilters = filters.map { it.fwkScanFilter }
        val fwkSettings = FwkScanSettings.Builder()
            .build()

        bluetoothLeScanner.startScan(fwkFilters, fwkSettings, callback)

        awaitClose {
            bluetoothLeScanner.stopScan(callback)
        }
    }
}
