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

import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES
import android.content.Context
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.ScanFilter
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothLeScanner

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RobolectricScanTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private var bluetoothLe = BluetoothLe(context)
    private companion object {
        private const val TIMEOUT_MS: Long = 2_000
    }

    @Test
    fun scanTest() = runTest {
        val scanResults = listOf(
            createScanResult("00:00:00:00:00:01"),
            createScanResult("00:00:00:00:00:02"),
            createScanResult("00:00:00:00:00:03"),
        )

        val scannerRef = AtomicReference<ShadowBluetoothLeScanner>(null)
        bluetoothLe.onStartScanListener = BluetoothLe.OnStartScanListener { scanner ->
            val shadowScanner = shadowOf(scanner)
            scannerRef.set(shadowScanner)

            // Check if the scan is started
            Assert.assertEquals(1, shadowScanner.activeScans.size)

            shadowScanner.scanCallbacks.forEach { callback ->
                scanResults.forEach { res ->
                    callback.onScanResult(CALLBACK_TYPE_ALL_MATCHES, res)
                }
            }
        }

        launch {
            bluetoothLe.scan(listOf(ScanFilter())).collectIndexed { index, value ->
                Assert.assertEquals(scanResults[index].device.address, value.deviceAddress.address)
                if (index == scanResults.size - 1) {
                    this.cancel()
                }
            }
        }.join()

        // Check if the scan is stopped
        Assert.assertEquals(0, scannerRef.get().activeScans.size)
    }

    @Suppress("DEPRECATION")
    private fun createScanResult(
        address: String,
        rssi: Int = 0,
        timestampNanos: Long = 0
    ): ScanResult {
        return ScanResult(ShadowBluetoothDevice.newInstance(address), null, rssi, timestampNanos)
    }
}
