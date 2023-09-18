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

import android.bluetooth.BluetoothGatt.GATT_READ_NOT_PERMITTED
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGatt.GATT_WRITE_NOT_PERMITTED
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents a request to be handled as a GATT server role.
 *
 * @see BluetoothLe.GattServerConnectRequest.accept
 */
open class GattServerRequest private constructor() {
    private val handled = AtomicBoolean(false)

    internal inline fun handleRequest(block: () -> Unit) {
        if (handled.compareAndSet(false, true)) {
            block()
        } else {
            throw IllegalStateException("Request is already handled")
        }
    }

    /**
     * Represents a read characteristic request.
     *
     * @property characteristic a characteristic to read
     */
    class ReadCharacteristic internal constructor(
        private val session: GattServer.Session,
        private val requestId: Int,
        private val offset: Int,
        val characteristic: GattCharacteristic
    ) : GattServerRequest() {
        /**
         * Sends the result for the read request.
         *
         * @param value a value of the characteristic
         */
        fun sendResponse(value: ByteArray) {
            handleRequest {
                val resValue: ByteArray = if (offset == 0) value
                else if (value.size > offset) value.copyOfRange(offset, value.size - 1)
                else if (value.size == offset) byteArrayOf()
                else byteArrayOf()
                session.sendResponse(requestId, GATT_SUCCESS, offset, resValue)
            }
        }

        /**
         * Notifies the failure for the read request.
         */
        fun sendFailure() {
            handleRequest {
                session.sendResponse(requestId, GATT_READ_NOT_PERMITTED, offset, null)
            }
        }
    }

    /**
     * Represents a request to write characteristics.
     *
     * If two or more writes are requested, they are expected to be written in order.
     *
     * @property parts a list of write request parts
     */
    class WriteCharacteristics internal constructor(
        private val session: GattServer.Session,
        private val requestId: Int,
        val parts: List<Part>
    ) : GattServerRequest() {
        /**
         * Notifies the success of the write request.
         */
        fun sendResponse() {
            handleRequest {
                session.sendResponse(requestId, GATT_SUCCESS, 0, null)
            }
        }

        /**
         * Notifies the failure of the write request.
         */
        fun sendFailure() {
            handleRequest {
                session.sendResponse(requestId, GATT_WRITE_NOT_PERMITTED, 0, null)
            }
        }

        /**
         * A part of write requests.
         *
         * It represents a partial write request such that
         * [value] is to be written to a part of [characteristic] based on [offset].
         * <p>
         * For example, if the [offset] is 2, the first byte of [value] should be written to
         * the third byte of the [characteristic], and the second byte of [value] should be
         * written to the fourth byte of the [characteristic] and so on.
         *
         * @property characteristic a characteristic to write
         * @property offset an offset of the first octet to be written
         * @property value a value to be written
         */
        class Part internal constructor(
            val characteristic: GattCharacteristic,
            val offset: Int,
            val value: ByteArray
        )
    }
}
