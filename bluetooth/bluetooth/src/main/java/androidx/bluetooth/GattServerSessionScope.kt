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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A scope for operations as a GATT server role.
 *
 * A scope is created for each remote device.
 *
 * Collect [requests] to respond with requests from the client.
 *
 * @see GattServerConnectRequest#accept()
 */
interface GattServerSessionScope {

    /** A client device connected to the server. */
    val device: BluetoothDevice

    /**
     * A _hot_ [Flow] of incoming requests from the client.
     *
     * A request is either [GattServerRequest.ReadCharacteristic] or
     * [GattServerRequest.WriteCharacteristics]
     */
    val requests: Flow<GattServerRequest>

    /**
     * A [StateFlow] of the set of characteristics that the client has requested to be notified of.
     *
     * The set will be updated whenever the client subscribes to or unsubscribes a characteristic.
     *
     * @see [GattServerSessionScope.notify]
     */
    val subscribedCharacteristics: StateFlow<Set<GattCharacteristic>>

    /**
     * Notifies a client of a characteristic value change.
     *
     * @param characteristic the updated characteristic
     * @param value the new value of the characteristic
     * @throws CancellationException if it failed to notify
     * @throws IllegalArgumentException if the length of the [value] is greater than the maximum
     *   attribute length (512)
     */
    suspend fun notify(characteristic: GattCharacteristic, value: ByteArray)
}
