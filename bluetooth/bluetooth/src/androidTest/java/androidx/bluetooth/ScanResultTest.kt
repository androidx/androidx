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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult as FwkScanResult
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Test cases for [ScanResult]
 */
@RunWith(JUnit4::class)
class ScanResultTest {
    @Rule
    @JvmField
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= 31) {
        GrantPermissionRule.grant(
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
    } else GrantPermissionRule.grant(
        android.Manifest.permission.BLUETOOTH
    )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    @Before
    fun setUp() {
        assumeNotNull(bluetoothAdapter)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun constructorWithFwkInstance() {
        val address = "00:01:02:03:04:05"
        val fwkBluetoothDevice = bluetoothAdapter!!.getRemoteDevice(address)
        val timeStampNanos: Long = 1
        val serviceUuid = UUID.randomUUID()
        val rssi = 34
        val periodicAdvertisingInterval = 8
        // Framework returns interval in units of 1.25ms.
        val expectedPeriodicAdvertisingInterval = (8 * 1.25).toLong()

        // TODO(kihongs) Find a way to create framework ScanRecord and use in test
        val fwkScanResult = FwkScanResult(
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
        val scanResult = ScanResult(fwkScanResult)

        assertThat(BluetoothDevice(fwkBluetoothDevice).name).isEqualTo(scanResult.device.name)
        assertThat(BluetoothDevice(fwkBluetoothDevice).bondState)
            .isEqualTo(scanResult.device.bondState)
        assertThat(address).isEqualTo(scanResult.deviceAddress.address)
        assertThat(BluetoothAddress.ADDRESS_TYPE_UNKNOWN)
            .isEqualTo(scanResult.deviceAddress.addressType)
        assertThat(true).isEqualTo(scanResult.isConnectable())
        assertThat(timeStampNanos).isEqualTo(scanResult.timestampNanos)
        assertThat(scanResult.getManufacturerSpecificData(1)).isNull()
        assertThat(emptyList<UUID>()).isEqualTo(scanResult.serviceUuids)
        assertThat(emptyList<UUID>()).isEqualTo(scanResult.serviceSolicitationUuids)
        assertThat(scanResult.serviceData).isEqualTo(emptyMap<ParcelUuid, ByteArray>())
        assertThat(scanResult.getServiceData(serviceUuid)).isNull()
        assertThat(rssi).isEqualTo(scanResult.rssi)
        assertThat(expectedPeriodicAdvertisingInterval)
            .isEqualTo(scanResult.periodicAdvertisingInterval)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun sameDeviceReturned() {
        val address = "00:01:02:03:04:05"
        val fwkBluetoothDevice = bluetoothAdapter!!.getRemoteDevice(address)
        val rssi = 34
        val periodicAdvertisingInterval = 6
        val timeStampNanos: Long = 1

        val fwkScanResult = FwkScanResult(
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
        val scanResult = ScanResult(fwkScanResult)
        assertThat(scanResult.device).isEqualTo(scanResult.device)
        assertThat(scanResult.deviceAddress).isEqualTo(scanResult.deviceAddress)
    }
}
