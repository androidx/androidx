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
import android.bluetooth.BluetoothDevice as FwkDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattClient
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowBluetoothGatt

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RobolectricGattClientTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private lateinit var bluetoothLe: BluetoothLe
    private lateinit var clientAdapter: StubClientFrameworkAdapter

    private companion object {
        private val serviceUuid1 = UUID.fromString("00001111-0000-1000-8000-00805F9B34FB")
        private val serviceUuid2 = UUID.fromString("00001112-0000-1000-8000-00805F9B34FB")

        private val service1 = BluetoothGattService(serviceUuid1,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
        private val service2 = BluetoothGattService(serviceUuid2,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)

        private val sampleServices: List<BluetoothGattService> = listOf(service1, service2)
    }

    @Before
    fun setUp() {
        bluetoothLe = BluetoothLe(context)
        clientAdapter = StubClientFrameworkAdapter(bluetoothLe.client.fwkAdapter)
        bluetoothLe.client.fwkAdapter = clientAdapter
    }

    @Test
    fun connectGatt() = runTest {
        acceptConnect()
        val device = createDevice("00:11:22:33:44:55")
        Assert.assertEquals(true, bluetoothLe.connectGatt(device) {
            Assert.assertEquals(sampleServices.size, getServices().size)
            sampleServices.forEachIndexed { index, service ->
                Assert.assertEquals(service.uuid, getServices()[index].uuid)
            }
            true
        }.getOrNull())
    }

    @Test
    fun connectFail() = runTest {
        val device = createDevice("00:11:22:33:44:55")
        rejectConnect()
        Assert.assertEquals(true, bluetoothLe.connectGatt(device) { true }.isFailure)
    }

    private fun acceptConnect() {
        clientAdapter.onConnectListener =
            StubClientFrameworkAdapter.OnConnectListener { device, _ ->
            shadowOf(device).simulateGattConnectionChange(
                BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_CONNECTED
            )
            true
        }

        clientAdapter.onRequestMtuListener =
            StubClientFrameworkAdapter.OnRequestMtuListener { mtu ->
            clientAdapter.callback?.onMtuChanged(clientAdapter.bluetoothGatt, mtu,
                BluetoothGatt.GATT_SUCCESS)
        }

        clientAdapter.onDiscoverServicesListener =
            StubClientFrameworkAdapter.OnDiscoverServicesListener {
            clientAdapter.gattServices = sampleServices
            clientAdapter.callback?.onServicesDiscovered(clientAdapter.bluetoothGatt,
                BluetoothGatt.GATT_SUCCESS)
        }
    }

    private fun rejectConnect() {
        clientAdapter.onConnectListener =
            StubClientFrameworkAdapter.OnConnectListener { device, _ ->
            shadowOf(device).simulateGattConnectionChange(
                BluetoothGatt.GATT_FAILURE, BluetoothGatt.STATE_DISCONNECTED
            )
            false
        }
    }

    private fun createDevice(address: String): BluetoothDevice {
       return BluetoothDevice(bluetoothAdapter!!.getRemoteDevice(address))
    }

    class StubClientFrameworkAdapter(
        private val baseAdapter: GattClient.FrameworkAdapter
    ) : GattClient.FrameworkAdapter {
        var gattServices: List<BluetoothGattService> = listOf()
        var callback: BluetoothGattCallback? = null
        override var bluetoothGatt: BluetoothGatt?
            get() = baseAdapter.bluetoothGatt
            set(value) { baseAdapter.bluetoothGatt = value }
        val shadowBluetoothGatt: ShadowBluetoothGatt
            get() = shadowOf(bluetoothGatt)

        var onConnectListener: OnConnectListener? = null
        var onRequestMtuListener: OnRequestMtuListener? = null
        var onDiscoverServicesListener: OnDiscoverServicesListener? = null
        var onReadCharacteristicListener: OnReadCharacteristicListener? = null
        var onWriteCharacteristicListener: OnWriteCharacteristicListener? = null
        var onWriteDescriptorListener: OnWriteDescriptorListener? = null
        var onSetCharacteristicNotifiationListener: OnSetCharacteristicNotificationListener? = null

        override fun connectGatt(
            context: Context,
            device: FwkDevice,
            callback: BluetoothGattCallback
        ): Boolean {
            this.callback = callback
            baseAdapter.connectGatt(context, device, callback)
            return onConnectListener?.onConnect(device, callback) ?: false
        }

        override fun requestMtu(mtu: Int) {
            baseAdapter.requestMtu(mtu)
            onRequestMtuListener?.onRequestMtu(mtu)
        }

        override fun discoverServices() {
            baseAdapter.discoverServices()
            onDiscoverServicesListener?.onDiscoverServices()
        }

        override fun getServices(): List<BluetoothGattService> {
            return gattServices
        }

        override fun getService(uuid: UUID): BluetoothGattService? {
            return gattServices.find { it.uuid == uuid }
        }

        override fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
            baseAdapter.readCharacteristic(characteristic)
            onReadCharacteristicListener?.onReadCharacteristic(characteristic)
        }

        override fun writeCharacteristic(
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            writeType: Int
        ) {
            baseAdapter.writeCharacteristic(characteristic, value, writeType)
            onWriteCharacteristicListener?.onWriteCharacteristic(characteristic, value, writeType)
        }

        override fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray) {
            baseAdapter.writeDescriptor(descriptor, value)
            onWriteDescriptorListener?.onWriteDescriptor(descriptor, value)
        }

        override fun setCharacteristicNotification(
            characteristic: BluetoothGattCharacteristic,
            enable: Boolean
        ) {
            baseAdapter.setCharacteristicNotification(characteristic, enable)
            onSetCharacteristicNotifiationListener
                ?.onSetCharacteristicNotification(characteristic, enable)
        }

        fun interface OnConnectListener {
            fun onConnect(device: FwkDevice, callback: BluetoothGattCallback): Boolean
        }
        fun interface OnRequestMtuListener {
            fun onRequestMtu(mtu: Int)
        }
        fun interface OnDiscoverServicesListener {
            fun onDiscoverServices()
        }
        fun interface OnReadCharacteristicListener {
            fun onReadCharacteristic(characteristic: BluetoothGattCharacteristic)
        }
        fun interface OnWriteCharacteristicListener {
            fun onWriteCharacteristic(
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                writeType: Int
            )
        }
        fun interface OnWriteDescriptorListener {
            fun onWriteDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray)
        }
        fun interface OnSetCharacteristicNotificationListener {
            fun onSetCharacteristicNotification(
                characteristic: BluetoothGattCharacteristic,
                enable: Boolean
            )
        }
    }
}
