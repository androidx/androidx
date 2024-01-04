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
import android.bluetooth.BluetoothGattCharacteristic as FwkCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService as FwkService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattClient
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import junit.framework.TestCase.fail
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        private val readCharUuid = UUID.fromString("00002221-0000-1000-8000-00805F9B34FB")
        private val writeCharUuid = UUID.fromString("00002222-0000-1000-8000-00805F9B34FB")
        private val notifyCharUuid = UUID.fromString("00002223-0000-1000-8000-00805F9B34FB")
        private val noPropertyCharUuid = UUID.fromString("00003333-0000-1000-8000-00805F9B34FB")

        private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private val service1 = FwkService(serviceUuid1,
            FwkService.SERVICE_TYPE_PRIMARY)
        private val service2 = FwkService(serviceUuid2,
            FwkService.SERVICE_TYPE_PRIMARY)

        private val readCharacteristic = FwkCharacteristic(readCharUuid,
            PROPERTY_READ, /*permissions=*/0)
        private val writeCharacteristic = FwkCharacteristic(writeCharUuid,
            PROPERTY_READ or PROPERTY_WRITE, /*permissions=*/0)
        private val notifyCharacteristic = FwkCharacteristic(notifyCharUuid,
            PROPERTY_READ or PROPERTY_NOTIFY, /*permissions=*/0)
        private val noPropertyCharacteristic = FwkCharacteristic(noPropertyCharUuid,
            /*properties=*/0, /*permissions=*/0)

        private val sampleServices: List<FwkService> = listOf(service1, service2)
        init {
            notifyCharacteristic.addDescriptor(
                BluetoothGattDescriptor(cccdUuid, /*permissions=*/0))

            service1.addCharacteristic(readCharacteristic)
            service1.addCharacteristic(writeCharacteristic)
            service1.addCharacteristic(notifyCharacteristic)
            service1.addCharacteristic(noPropertyCharacteristic)
        }
    }

    @Before
    fun setUp() {
        bluetoothLe = BluetoothLe(context)
        clientAdapter = StubClientFrameworkAdapter(bluetoothLe.client.fwkAdapter)
        bluetoothLe.client.fwkAdapter = clientAdapter
    }

    @Test
    fun connectGatt() = runTest {
        val device = createDevice("00:11:22:33:44:55")

        acceptConnect()

        bluetoothLe.connectGatt(device) {
            assertTrue(clientAdapter.shadowBluetoothGatt.isConnected)

            Assert.assertEquals(sampleServices.size, services.size)
            sampleServices.forEachIndexed { index, service ->
                Assert.assertEquals(service.uuid, services[index].uuid)
            }
        }

        assertTrue(clientAdapter.shadowBluetoothGatt.isClosed)
        assertFalse(clientAdapter.shadowBluetoothGatt.isConnected)
    }

    @Test
    fun connectGatt_throwException_closeGatt() = runTest {
        val device = createDevice("00:11:22:33:44:55")

        acceptConnect()

        assertFailsWith<RuntimeException> {
            bluetoothLe.connectGatt(device) {
                assertTrue(clientAdapter.shadowBluetoothGatt.isConnected)
                throw RuntimeException()
            }
        }

        assertTrue(clientAdapter.shadowBluetoothGatt.isClosed)
        assertFalse(clientAdapter.shadowBluetoothGatt.isConnected)
    }

    @Test
    fun connectFail() = runTest {
        val device = createDevice("00:11:22:33:44:55")
        rejectConnect()
        assertFailsWith<CancellationException> { bluetoothLe.connectGatt(device) { } }
    }

    @Test
    fun readCharacteristic() = runTest {
        val testValue = 48
        val device = createDevice("00:11:22:33:44:55")
        acceptConnect()

        clientAdapter.onReadCharacteristicListener =
            StubClientFrameworkAdapter.OnReadCharacteristicListener { char ->
                launch {
                    // Check if awaitClose waits for the callback is finished
                    delay(100)
                    clientAdapter.callback?.onCharacteristicRead(
                        clientAdapter.bluetoothGatt!!,
                        char,
                        testValue.toByteArray(),
                        BluetoothGatt.GATT_SUCCESS
                    )
                }
        }

        bluetoothLe.connectGatt(device) {
            Assert.assertEquals(sampleServices.size, services.size)
            Assert.assertEquals(testValue,
                readCharacteristic(
                    services[0].getCharacteristic(readCharUuid)!!
                ).getOrNull()?.toInt())
        }
        assertTrue(clientAdapter.shadowBluetoothGatt.isClosed)
        assertFalse(clientAdapter.shadowBluetoothGatt.isConnected)
    }

    @Test
    fun readCharacteristicWithoutReadProperty_returnsException() = runTest {
        acceptConnect()
        val device = createDevice("00:11:22:33:44:55")

        clientAdapter.onReadCharacteristicListener =
            StubClientFrameworkAdapter.OnReadCharacteristicListener {
                // Should not be run
                fail()
            }

        bluetoothLe.connectGatt(device) {
            Assert.assertEquals(sampleServices.size, services.size)
            assertTrue(
                readCharacteristic(
                    services[0].getCharacteristic(noPropertyCharUuid)!!
                ).exceptionOrNull()
                is IllegalArgumentException)
        }
    }

    @Test
    fun writeCharacteristic() = runTest {
        val initialValue = 48
        val valueToWrite = 96
        val device = createDevice("00:11:22:33:44:55")
        val currentValue = AtomicInteger(initialValue)

        acceptConnect()

        clientAdapter.onReadCharacteristicListener =
            StubClientFrameworkAdapter.OnReadCharacteristicListener { char ->
                launch {
                    // For the callback being invoked after waiting
                    delay(0)
                    clientAdapter.callback?.onCharacteristicRead(
                        clientAdapter.bluetoothGatt!!,
                        char,
                        currentValue.get().toByteArray(),
                        BluetoothGatt.GATT_SUCCESS
                    )
                }
            }

        clientAdapter.onWriteCharacteristicListener =
            StubClientFrameworkAdapter.OnWriteCharacteristicListener { char, value, _ ->
                launch {
                    delay(0)
                    currentValue.set(value.toInt())
                    clientAdapter.callback?.onCharacteristicWrite(
                        clientAdapter.bluetoothGatt!!, char, BluetoothGatt.GATT_SUCCESS
                    )
                }
        }

        bluetoothLe.connectGatt(device) {
            Assert.assertEquals(sampleServices.size, services.size)
            val characteristic = services[0].getCharacteristic(writeCharUuid)!!

            Assert.assertEquals(initialValue,
                readCharacteristic(characteristic).getOrNull()?.toInt())
            writeCharacteristic(characteristic,
                valueToWrite.toByteArray())
            Assert.assertEquals(valueToWrite,
                readCharacteristic(characteristic).getOrNull()?.toInt())
        }
        assertTrue(clientAdapter.shadowBluetoothGatt.isClosed)
        assertFalse(clientAdapter.shadowBluetoothGatt.isConnected)
    }

    @Test
    fun writeCharacteristicWithoutWriteProperty_returnsException() = runTest {
        acceptConnect()
        val device = createDevice("00:11:22:33:44:55")

        clientAdapter.onWriteCharacteristicListener =
            StubClientFrameworkAdapter.OnWriteCharacteristicListener { _, _, _ ->
                // Should not be run
                fail()
            }

        bluetoothLe.connectGatt(device) {
            Assert.assertEquals(sampleServices.size, services.size)
            assertTrue(
                writeCharacteristic(
                    services[0].getCharacteristic(readCharUuid)!!,
                    48.toByteArray()
                ).exceptionOrNull()
                is IllegalArgumentException)
        }
    }

    @Test
    fun subscribeToCharacteristic() = runTest {
        val initialValue = 48
        val valueToNotify = 96
        val device = createDevice("00:11:22:33:44:55")
        val currentValue = AtomicInteger(initialValue)

        acceptConnect()

        clientAdapter.onReadCharacteristicListener =
            StubClientFrameworkAdapter.OnReadCharacteristicListener { char ->
                launch {
                    // For the callback being invoked after waiting
                    delay(0)
                    clientAdapter.callback?.onCharacteristicRead(
                        clientAdapter.bluetoothGatt!!,
                        char,
                        currentValue.get().toByteArray(),
                        BluetoothGatt.GATT_SUCCESS
                    )
                }
            }

        clientAdapter.onWriteDescriptorListener =
            StubClientFrameworkAdapter.OnWriteDescriptorListener { desc, _ ->
                launch {
                    delay(100)
                    currentValue.set(valueToNotify)
                    clientAdapter.callback?.onCharacteristicChanged(
                        clientAdapter.bluetoothGatt!!,
                        desc.characteristic,
                        valueToNotify.toByteArray()
                    )
                }
        }

        bluetoothLe.connectGatt(device) {
            Assert.assertEquals(sampleServices.size, services.size)
            val characteristic = services[0].getCharacteristic(notifyCharUuid)!!

            Assert.assertEquals(initialValue,
                readCharacteristic(characteristic).getOrNull()?.toInt())
            Assert.assertEquals(
                valueToNotify,
                subscribeToCharacteristic(characteristic).first().toInt())
            Assert.assertEquals(valueToNotify,
                readCharacteristic(characteristic).getOrNull()?.toInt())
        }
        assertTrue(clientAdapter.shadowBluetoothGatt.isClosed)
        assertFalse(clientAdapter.shadowBluetoothGatt.isConnected)
    }

    @Test
    fun subscribeToCharacteristicWithoutNotifyProperty_returnsException() = runTest {
        acceptConnect()
        val device = createDevice("00:11:22:33:44:55")

        clientAdapter.onWriteDescriptorListener =
            StubClientFrameworkAdapter.OnWriteDescriptorListener { _, _ ->
                // Should not be run
                fail()
            }

        bluetoothLe.connectGatt(device) {
            Assert.assertEquals(sampleServices.size, services.size)
            subscribeToCharacteristic(
                services[0].getCharacteristic(readCharUuid)!!,
            ).collect {
                // Should not be notified
                fail()
            }
        }
    }

    @Test
    fun servicesFlow_emittedWhenServicesChange() = runTest {
        val device = createDevice("00:11:22:33:44:55")

        val newServiceUuid = UUID.randomUUID()
        val newService = FwkService(newServiceUuid, FwkService.SERVICE_TYPE_PRIMARY)
        val newServices = sampleServices + newService

        acceptConnect()

        clientAdapter.onDiscoverServicesListener =
            StubClientFrameworkAdapter.OnDiscoverServicesListener {
                if (clientAdapter.gattServices.isEmpty()) {
                    clientAdapter.gattServices = sampleServices
                }
                clientAdapter.callback?.onServicesDiscovered(
                    clientAdapter.bluetoothGatt,
                    BluetoothGatt.GATT_SUCCESS
                )
            }

        bluetoothLe.connectGatt(device) {
            launch {
                clientAdapter.gattServices = newServices
                if (Build.VERSION.SDK_INT >= 31) {
                    clientAdapter.callback?.onServiceChanged(clientAdapter.bluetoothGatt!!)
                }
            }
            val servicesEmitted = servicesFlow.take(2).toList()
            Assert.assertEquals(sampleServices.size, servicesEmitted[0].size)
            Assert.assertEquals(sampleServices.size + 1, servicesEmitted[1].size)
            Assert.assertEquals(newServiceUuid, servicesEmitted[1][sampleServices.size].uuid)
        }
    }

    private fun acceptConnect() {
        clientAdapter.onConnectListener =
            StubClientFrameworkAdapter.OnConnectListener { device, _ ->
                clientAdapter.shadowBluetoothGatt.notifyConnection(device.address)
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
        var gattServices: List<FwkService> = listOf()
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

        override fun getServices(): List<FwkService> {
            return gattServices
        }

        override fun getService(uuid: UUID): FwkService? {
            return gattServices.find { it.uuid == uuid }
        }

        override fun readCharacteristic(characteristic: FwkCharacteristic) {
            baseAdapter.readCharacteristic(characteristic)
            onReadCharacteristicListener?.onReadCharacteristic(characteristic)
        }

        override fun writeCharacteristic(
            characteristic: FwkCharacteristic,
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
            characteristic: FwkCharacteristic,
            enable: Boolean
        ) {
            baseAdapter.setCharacteristicNotification(characteristic, enable)
            onSetCharacteristicNotifiationListener
                ?.onSetCharacteristicNotification(characteristic, enable)
        }

        override fun closeGatt() {
            baseAdapter.closeGatt()
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
            fun onReadCharacteristic(characteristic: FwkCharacteristic)
        }
        fun interface OnWriteCharacteristicListener {
            fun onWriteCharacteristic(
                characteristic: FwkCharacteristic,
                value: ByteArray,
                writeType: Int
            )
        }
        fun interface OnWriteDescriptorListener {
            fun onWriteDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray)
        }
        fun interface OnSetCharacteristicNotificationListener {
            fun onSetCharacteristicNotification(
                characteristic: FwkCharacteristic,
                enable: Boolean
            )
        }
    }
}
