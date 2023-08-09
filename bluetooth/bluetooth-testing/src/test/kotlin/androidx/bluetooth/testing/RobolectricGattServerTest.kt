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
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattCharacteristic.Companion.PERMISSION_READ
import androidx.bluetooth.GattCharacteristic.Companion.PERMISSION_WRITE
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_NOTIFY
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_READ
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_WRITE
import androidx.bluetooth.GattServer
import androidx.bluetooth.GattServerRequest
import androidx.bluetooth.GattService
import java.nio.ByteBuffer
import java.util.UUID
import junit.framework.TestCase.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
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

        private val readCharacteristic = GattCharacteristic(
            readCharUuid,
            PROPERTY_READ, PERMISSION_READ
        )
        private val writeCharacteristic = GattCharacteristic(
            writeCharUuid,
            PROPERTY_READ or PROPERTY_WRITE, PERMISSION_READ or PERMISSION_WRITE
        )
        private val notifyCharacteristic = GattCharacteristic(
            notifyCharUuid,
            PROPERTY_READ or PROPERTY_NOTIFY, PERMISSION_READ
        )
        private val unknownCharacteristic = GattCharacteristic(unknownCharUuid, 0, 0)

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

        bluetoothLe.openGattServer(listOf()).first().accept {
        }

        Assert.assertTrue(opened.isCompleted)
        Assert.assertTrue(closed.isCompleted)
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
            bluetoothLe.openGattServer(services).collect {
                it.reject()
                Assert.assertThrows(
                    IllegalStateException::class.java
                ) {
                    runBlocking {
                        it.accept {}
                    }
                }
                this@launch.cancel()
            }
        }.join()

        Assert.assertTrue(closed.isCompleted)
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
            bluetoothLe.openGattServer(services).collect {
                it.accept {}
                Assert.assertThrows(
                    IllegalStateException::class.java
                ) {
                    runBlocking {
                        it.reject()
                    }
                }
                this@launch.cancel()
            }
        }.join()

        Assert.assertTrue(closed.isCompleted)
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
            bluetoothLe.openGattServer(services).collect {
                it.accept {
                    when (val request = requests.first()) {
                        is GattServerRequest.ReadCharacteristicRequest -> {
                            request.sendResponse(true, valueToRead.toByteArray())
                        }
                        else -> fail("unexpected request")
                    }
                    // Close the server
                    this@launch.cancel()
                }
            }
        }.join()

        // Ensure if the server is closed
        Assert.assertTrue(closed.isCompleted)
        Assert.assertEquals(1, serverAdapter.shadowGattServer.responses.size)
        Assert.assertEquals(valueToRead, serverAdapter.shadowGattServer.responses[0].toInt())
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
            bluetoothLe.openGattServer(services).collect {
                it.accept {
                    when (val request = requests.first()) {
                        is GattServerRequest.ReadCharacteristicRequest -> {
                            Assert.assertEquals(readCharacteristic, request.characteristic)
                            request.sendResponse(true, valueToRead.toByteArray())
                        }
                        else -> fail("unexpected request")
                    }
                    // Close the server
                    this@launch.cancel()
                }
            }
        }.join()

        Assert.assertTrue(closed.isCompleted)
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
            bluetoothLe.openGattServer(services).collect {
                it.accept {
                    when (val request = requests.first()) {
                        is GattServerRequest.WriteCharacteristicRequest -> {
                            Assert.assertEquals(valueToWrite, request.value?.toInt())
                            request.sendResponse(true)
                        }
                        else -> fail("unexpected request")
                    }
                    // Close the server
                    this@launch.cancel()
                }
            }
        }.join()

        Assert.assertTrue(closed.isCompleted)
        Assert.assertEquals(1, serverAdapter.shadowGattServer.responses.size)
        Assert.assertEquals(valueToWrite, serverAdapter.shadowGattServer.responses[0].toInt())
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
                    _, _, _, value ->
                notified.complete(value.toInt())
            }
        serverAdapter.onCloseGattServerListener =
            StubServerFrameworkAdapter.OnCloseGattServerListener {
                closed.complete(Unit)
            }

        launch {
            bluetoothLe.openGattServer(services).collect {
                it.accept {
                    notify(notifyCharacteristic, valueToNotify.toByteArray())
                    // Close the server
                    this@launch.cancel()
                }
            }
        }.join()

        // Ensure if the server is closed
        Assert.assertTrue(closed.isCompleted)
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
            opened.await()
            bluetoothLe.updateServices(listOf(service2))
        }
        launch {
            bluetoothLe.openGattServer(listOf(service1)).first().accept {
            }
        }.join()

        Assert.assertTrue(opened.isCompleted)
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

        override fun addService(service: BluetoothGattService) {
            baseAdapter.addService(service)
            onAddServiceListener?.onAddService(service)
        }

        override fun notifyCharacteristicChanged(
            device: FwkDevice,
            characteristic: BluetoothGattCharacteristic,
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
        }

        fun interface OnOpenGattServerListener {
            fun onOpenGattServer()
        }
        fun interface OnAddServiceListener {
            fun onAddService(service: BluetoothGattService)
        }
        fun interface OnCloseGattServerListener {
            fun onCloseGattServer()
        }
        fun interface OnNotifyCharacteristicChangedListener {
            fun onNotifyCharacteristicChanged(
                device: FwkDevice,
                characteristic: BluetoothGattCharacteristic,
                confirm: Boolean,
                value: ByteArray
            )
        }
    }
}

private fun Int.toByteArray(): ByteArray {
    return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this).array()
}

private fun ByteArray.toInt(): Int {
    return ByteBuffer.wrap(this).int
}
