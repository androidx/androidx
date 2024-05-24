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

package androidx.bluetooth.testing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult as FwkScanResult
import android.bluetooth.le.ScanSettings as FwkScanSettings
import android.content.Context
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.ScanFilter
import androidx.bluetooth.ScanImpl
import androidx.bluetooth.ScanResult
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RobolectricScanTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

    private val bluetoothLe = BluetoothLe(RuntimeEnvironment.getApplication())

    private val scanResults =
        listOf(
            createScanResult("00:00:00:00:00:01"),
            createScanResult("00:00:00:00:00:02"),
            createScanResult("00:00:00:00:00:03")
        )

    @Before
    fun setUp() {
        bluetoothLe.scanImpl = ScanImplForTesting(scanResults)
    }

    @Test
    fun scanTest() = runTest {
        bluetoothLe.scan(listOf(ScanFilter())).collectIndexed { index, value ->
            assertEquals(scanResults[index].deviceAddress.address, value.deviceAddress.address)
        }
    }

    private fun createScanResult(
        address: String,
    ): ScanResult {
        val fwkBluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
        val timeStampNanos: Long = 1
        val rssi = 34
        val periodicAdvertisingInterval = 8

        // TODO(kihongs) Find a way to create framework ScanRecord and use in test
        val fwkScanResult =
            FwkScanResult(
                fwkBluetoothDevice,
                1,
                0,
                0,
                0,
                0,
                rssi,
                periodicAdvertisingInterval,
                null,
                timeStampNanos
            )
        return ScanResult(fwkScanResult)
    }
}

class ScanImplForTesting(val scanResults: List<ScanResult>) : ScanImpl {
    override val fwkSettings: FwkScanSettings = FwkScanSettings.Builder().build()

    override fun scan(filters: List<ScanFilter>): Flow<ScanResult> {
        return scanResults.asFlow()
    }
}
