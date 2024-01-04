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
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic as FwkCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService as FwkService
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_NOTIFY
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_READ
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_WRITE
import androidx.bluetooth.GattServer
import androidx.bluetooth.GattServerRequest
import androidx.bluetooth.GattService
import java.util.UUID
import junit.framework.TestCase.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowBluetoothGattServer

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RobolectricGattServerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
       private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private lateinit var bluetoothLe: BluetoothLe
    private lateinit var serverAdapter: StubServerFrameworkAdapter

    private companion object {
        private val serviceUuid1 = UUID.fromString("00001111-0000-1000-8000-00805F9B34FB")
        private val serviceUuid2 = UUID.fromString("00001112-0000-1000-8000-00805F9B34FB")

        private val readCharUuid = UUID.fromString("00002221-0000-1000-8000-00805F9B34FB")
        private val writeCharUuid = UUID.fromString("00002222-0000-1000-8000-00805F9B34FB")
        private val notifyCharUuid = UUID.fromString("00002223-0000-1000-8000-00805F9B34FB")
        private val unknownCharUuid = UUID.fromString("00002224-0000-1000-8000-00805F9B34FB")

        private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private val readCharacteristic = GattCharacteristic(readCharUuid, PROPERTY_READ)
        private val writeCharacteristic = GattCharacteristic(
            writeCharUuid, PROPERTY_READ or PROPERTY_WRITE
        )
        private val notifyCharacteristic = GattCharacteristic(
            notifyCharUuid, PROPERTY_READ or PROPERTY_NOTIFY
        )
        private val unknownCharacteristic = GattCharacteristic(unknownCharUuid, 0)

        private val service1 = GattService(
            serviceUuid1,
            listOf(readCharacteristic, writeCharacteristic, notifyCharacteristic)
        )
        private val service2 = GattService(serviceUuid2, listOf())
    }

    @Before
    fun setUp() {
        bluetoothLe = BluetoothLe(context)
        serverAdapter = StubServerFrameworkAdapter(bluetoothLe.server.fwkAdapter)
        bluetoothLe.server.fwkAdapter = serverAdapter
    }

    @Test
    fun openGattServer() = runTest {
        val device = createDevice("00:11:22:33:44:55")
        val opened = CompletableDeferred<Unit>()
        val closed = CompletableDeferred<Unit>()

        serverAdapter.onOpenGattServerListener =
            StubServerFrameworkAdapter.OnOpenGattServerListener {
                connectDevice(device) { opened.complete(Unit) }
            }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        bluetoothLe.openGattServer(listOf()) {
            connectRequests.first().accept {}
        }

        assertTrue(opened.isCompleted)
        assertTrue(closed.isCompleted)
    }

    @Test
    fun openGattServer_rejectAndAccept_throwsException() = runTest {
        val services = listOf(service1, service2)
        val device = createDevice("00:11:22:33:44:55")
        val closed = CompletableDeferred<Unit>()

        runAfterServicesAreAdded(services.size) {
            connectDevice(device) {
                serverAdapter.callback.onCharacteristicReadRequest(
                    device, /*requestId=*/1, /*offset=*/0, readCharacteristic.fwkCharacteristic)
            }
        }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services) {
                connectRequests.collect {
                    it.reject()
                    Assert.assertThrows(IllegalStateException::class.java) {
                        runBlocking {
                            it.accept {}
                        }
                    }
                    this@launch.cancel()
                }
            }
        }.join()

        assertTrue(closed.isCompleted)
        Assert.assertEquals(0, serverAdapter.shadowGattServer.responses.size)
    }

       @Test
    fun openGattServer_acceptAndReject_throwsException() = runTest {
        val services = listOf(service1, service2)
        val device = createDevice("00:11:22:33:44:55")
        val closed = CompletableDeferred<Unit>()

        runAfterServicesAreAdded(services.size) {
            connectDevice(device) {
                serverAdapter.callback.onCharacteristicReadRequest(
                    device, /*requestId=*/1, /*offset=*/0, readCharacteristic.fwkCharacteristic)
            }
        }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services) {
                connectRequests.collect {
                    it.accept {}
                    Assert.assertThrows(IllegalStateException::class.java) {
                        it.reject()
                    }
                    this@launch.cancel()
                }
            }
        }.join()

        assertTrue(closed.isCompleted)
        Assert.assertEquals(0, serverAdapter.shadowGattServer.responses.size)
    }

    @Test
    fun readCharacteristic() = runTest {
        val services = listOf(service1, service2)
        val device = createDevice("00:11:22:33:44:55")
        val closed = CompletableDeferred<Unit>()
        val valueToRead = 42

        runAfterServicesAreAdded(services.size) {
            connectDevice(device) {
                serverAdapter.callback.onCharacteristicReadRequest(
                    device, /*requestId=*/1, /*offset=*/0, readCharacteristic.fwkCharacteristic)
            }
        }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services) {
                connectRequests.collect {
                    it.accept {
                        when (val request = requests.first()) {
                            is GattServerRequest.ReadCharacteristic -> {
                                request.sendResponse(valueToRead.toByteArray())
                            }
                            else -> fail("unexpected request")
                        }
                        // Close the server
                        this@launch.cancel()
                    }
                }
            }
        }.join()

        // Ensure if the server is closed
        assertTrue(closed.isCompleted)
        Assert.assertEquals(1, serverAdapter.shadowGattServer.responses.size)
        Assert.assertEquals(valueToRead, serverAdapter.shadowGattServer.responses[0].toInt())
    }

    @Test
    fun readCharacteristic_sendFailure() = runTest {
        val services = listOf(service1, service2)
        val device = createDevice("00:11:22:33:44:55")
        val closed = CompletableDeferred<Unit>()
        val responsed = CompletableDeferred<Unit>()

        runAfterServicesAreAdded(services.size) {
            connectDevice(device) {
                serverAdapter.callback.onCharacteristicReadRequest(
                    device, /*requestId=*/1, /*offset=*/0, readCharacteristic.fwkCharacteristic)
            }
        }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }
        serverAdapter.onSendResponseListener =
            StubServerFrameworkAdapter.OnSendResponseListener { _, requestId, status, _, value ->
                Assert.assertEquals(1, requestId)
                Assert.assertNotEquals(GATT_SUCCESS, status)
                Assert.assertNull(value)
                responsed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services) {
                connectRequests.collect {
                    it.accept {
                        when (val request = requests.first()) {
                            is GattServerRequest.ReadCharacteristic -> {
                                request.sendFailure()
                            }
                            else -> fail("unexpected request")
                        }
                        // Close the server
                        this@launch.cancel()
                    }
                }
            }
        }.join()

        // Ensure if the server is closed
        assertTrue(closed.isCompleted)
        assertTrue(responsed.isCompleted)
    }

    @Test
    fun readUnknownCharacteristic_failsWithoutNotified() = runTest {
        val services = listOf(service1, service2)
        val device = createDevice("00:11:22:33:44:55")
        val closed = CompletableDeferred<Unit>()
        val valueToRead = 42

        runAfterServicesAreAdded(services.size) {
            connectDevice(device) {
                serverAdapter.callback.onCharacteristicReadRequest(
                    device, /*requestId=*/1, /*offset=*/0, unknownCharacteristic.fwkCharacteristic)
                serverAdapter.callback.onCharacteristicReadRequest(
                    device, /*requestId=*/2, /*offset=*/0, readCharacteristic.fwkCharacteristic)
            }
        }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services) {
                connectRequests.collect {
                    it.accept {
                        when (val request = requests.first()) {
                            is GattServerRequest.ReadCharacteristic -> {
                                Assert.assertEquals(readCharacteristic, request.characteristic)
                                request.sendResponse(valueToRead.toByteArray())
                            }

                            else -> fail("unexpected request")
                        }
                        // Close the server
                        this@launch.cancel()
                    }
                }
            }
        }.join()

        assertTrue(closed.isCompleted)
    }
    @Test
    fun writeCharacteristic() = runTest {
        val services = listOf(service1, service2)
        val device = createDevice("00:11:22:33:44:55")
        val closed = CompletableDeferred<Unit>()
        val valueToWrite = 42

        runAfterServicesAreAdded(services.size) {
            connectDevice(device) {
                serverAdapter.callback.onCharacteristicWriteRequest(
                    device, /*requestId=*/1, writeCharacteristic.fwkCharacteristic,
                    /*preparedWrite=*/false, /*responseNeeded=*/false,
                    /*offset=*/0, valueToWrite.toByteArray())
            }
        }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services) {
                connectRequests.collect {
                    it.accept {
                        when (val request = requests.first()) {
                            is GattServerRequest.WriteCharacteristics -> {
                                Assert.assertEquals(valueToWrite, request.parts[0].value.toInt())
                                request.sendResponse()
                            }

                            else -> fail("unexpected request")
                        }
                        // Close the server
                        this@launch.cancel()
                    }
                }
            }
        }.join()

        assertTrue(closed.isCompleted)
    }

    @Test
    fun writeCharacteristic_sendFailure() = runTest {
        val services = listOf(service1, service2)
        val device = createDevice("00:11:22:33:44:55")
        val closed = CompletableDeferred<Unit>()
        val responded = CompletableDeferred<Unit>()
        val valueToWrite = 42

        runAfterServicesAreAdded(services.size) {
            connectDevice(device) {
                serverAdapter.callback.onCharacteristicWriteRequest(
                    device, /*requestId=*/1, writeCharacteristic.fwkCharacteristic,
                    /*preparedWrite=*/false, /*responseNeeded=*/false,
                    /*offset=*/0, valueToWrite.toByteArray()
                )
            }
        }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }
        serverAdapter.onSendResponseListener =
            StubServerFrameworkAdapter.OnSendResponseListener { _, requestId, status, _, value ->
                Assert.assertEquals(1, requestId)
                Assert.assertNotEquals(GATT_SUCCESS, status)
                Assert.assertNull(value)
                responded.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services) {
                connectRequests.collect {
                    it.accept {
                        when (val request = requests.first()) {
                            is GattServerRequest.WriteCharacteristics -> {
                                Assert.assertEquals(valueToWrite, request.parts[0].value.toInt())
                                request.sendFailure()
                            }

                            else -> fail("unexpected request")
                        }
                        // Close the server
                        this@launch.cancel()
                    }
                }
            }
        }.join()

        assertTrue(closed.isCompleted)
    }

    @Test
    fun notifyCharacteristic() = runTest {
       val services = listOf(service1, service2)
        val device = createDevice("00:11:22:33:44:55")
        val notified = CompletableDeferred<Int>()
        val closed = CompletableDeferred<Unit>()
        val valueToNotify = 42

        runAfterServicesAreAdded(services.size) {
            connectDevice(device) {
                serverAdapter.callback.onCharacteristicReadRequest(
                    device, /*requestId=*/1, /*offset=*/0, readCharacteristic.fwkCharacteristic)
            }
        }
        serverAdapter.onNotifyCharacteristicChangedListener =
            StubServerFrameworkAdapter.OnNotifyCharacteristicChangedListener {
                    fwkDevice, _, _, value ->
                notified.complete(value.toInt())
                serverAdapter.callback.onNotificationSent(fwkDevice, GATT_SUCCESS)
            }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services) {
                connectRequests.collect {
                    it.accept {
                        assertTrue(notify(notifyCharacteristic, valueToNotify.toByteArray()))
                        // Close the server
                        this@launch.cancel()
                    }
                }
            }
        }.join()

        // Ensure if the server is closed
        assertTrue(closed.isCompleted)
        Assert.assertEquals(valueToNotify, notified.await())
    }

    @Test
    fun updateServices() = runTest {
        val device = createDevice("00:11:22:33:44:55")
        val opened = CompletableDeferred<Unit>()
        val closed = CompletableDeferred<Unit>()

        serverAdapter.onOpenGattServerListener =
            StubServerFrameworkAdapter.OnOpenGattServerListener {
                connectDevice(device) { opened.complete(Unit) }
            }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(listOf(service1)) {
                updateServices(listOf(service2))
                connectRequests.first().accept {}
            }
        }.join()

        assertTrue(opened.isCompleted)
        assertTrue(closed.isCompleted)
    }

    @Test
    fun writeLongCharacteristic() = runTest {
        val services = listOf(service1, service2)
        val device = createDevice("00:11:22:33:44:55")
        val closed = CompletableDeferred<Unit>()
        val values = listOf(byteArrayOf(0, 1), byteArrayOf(2, 3))

        runAfterServicesAreAdded(services.size) {
            connectDevice(device) {
                var offset = 0
                values.forEachIndexed { index, value ->
                    serverAdapter.callback.onCharacteristicWriteRequest(
                        device, /*requestId=*/index + 1, writeCharacteristic.fwkCharacteristic,
                        /*preparedWrite=*/true, /*responseNeeded=*/false,
                        offset, value
                    )
                    offset += value.size
                }
                serverAdapter.callback.onExecuteWrite(device, /*requestId=*/values.size + 1, true)
            }
        }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services) {
                connectRequests.collect {
                    it.accept {
                        when (val request = requests.first()) {
                            is GattServerRequest.WriteCharacteristics -> {
                                Assert.assertEquals(values.size, request.parts.size)
                                values.forEachIndexed { index, value ->
                                    Assert.assertEquals(value, request.parts[index].value)
                                }
                                request.sendResponse()
                            }

                            else -> fail("unexpected request")
                        }
                        // Close the server
                        this@launch.cancel()
                    }
                }
            }
        }.join()

        Assert.assertTrue(closed.isCompleted)
    }

    private fun<R> runAfterServicesAreAdded(countServices: Int, block: suspend () -> R) {
        var waitCount = countServices
        serverAdapter.onAddServiceListener = StubServerFrameworkAdapter.OnAddServiceListener {
            if (--waitCount == 0) {
                runBlocking {
                    block()
                }
            }
        }
    }

    private fun<R> connectDevice(device: FwkDevice, block: () -> R): R {
        serverAdapter.shadowGattServer.notifyConnection(device)
        return block()
    }

    private fun createDevice(address: String): FwkDevice {
        return bluetoothAdapter!!.getRemoteDevice(address)
    }

    class StubServerFrameworkAdapter(
        private val baseAdapter: GattServer.FrameworkAdapter
    ) : GattServer.FrameworkAdapter {
        val shadowGattServer: ShadowBluetoothGattServer
            get() = shadowOf(gattServer)
        val callback: BluetoothGattServerCallback
            get() = shadowGattServer.gattServerCallback
        override var gattServer: BluetoothGattServer?
            get() = baseAdapter.gattServer
            set(value) { baseAdapter.gattServer = value }

        var onOpenGattServerListener: OnOpenGattServerListener? = null
        var onCloseGattServerListener: OnCloseGattServerListener? = null
        var onAddServiceListener: OnAddServiceListener? = null
        var onNotifyCharacteristicChangedListener: OnNotifyCharacteristicChangedListener? = null
        var onSendResponseListener: OnSendResponseListener? = null

        override fun openGattServer(context: Context, callback: BluetoothGattServerCallback) {
            baseAdapter.openGattServer(context, callback)
            onOpenGattServerListener?.onOpenGattServer()
        }

        override fun closeGattServer() {
            baseAdapter.closeGattServer()
            onCloseGattServerListener?.onCloseGattServer()
        }

        override fun clearServices() {
            baseAdapter.clearServices()
        }

        override fun addService(service: FwkService) {
            baseAdapter.addService(service)
            onAddServiceListener?.onAddService(service)
        }

        override fun notifyCharacteristicChanged(
            device: FwkDevice,
            characteristic: FwkCharacteristic,
            confirm: Boolean,
            value: ByteArray
        ) {
            baseAdapter.notifyCharacteristicChanged(device, characteristic, confirm, value)
            onNotifyCharacteristicChangedListener
                ?.onNotifyCharacteristicChanged(device, characteristic, confirm, value)
        }

        override fun sendResponse(
            device: FwkDevice,
            requestId: Int,
            status: Int,
            offset: Int,
            value: ByteArray?
        ) {
            baseAdapter.sendResponse(device, requestId, status, offset, value)
            onSendResponseListener
                ?.onSendResponse(device, requestId, status, offset, value)
        }

        fun interface OnOpenGattServerListener {
            fun onOpenGattServer()
        }
        fun interface OnAddServiceListener {
            fun onAddService(service: FwkService)
        }
        fun interface OnCloseGattServerListener {
            fun onCloseGattServer()
        }
        fun interface OnSendResponseListener {
            fun onSendResponse(
                device: FwkDevice,
                requestId: Int,
                status: Int,
                offset: Int,
                value: ByteArray?
            )
        }
        fun interface OnNotifyCharacteristicChangedListener {
            fun onNotifyCharacteristicChanged(
                device: FwkDevice,
                characteristic: FwkCharacteristic,
                confirm: Boolean,
                value: ByteArray
            )
        }
    }
}
