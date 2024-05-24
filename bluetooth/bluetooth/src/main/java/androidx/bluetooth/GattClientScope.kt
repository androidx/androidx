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

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Scope for operations as a GATT client role.
 *
 * @see BluetoothLe.connectGatt
 */
interface GattClientScope {

    /**
     * A flow of GATT services discovered from the remote device.
     *
     * If the services of the remote device has changed, the new services will be discovered and
     * emitted automatically.
     */
    val servicesFlow: StateFlow<List<GattService>>

    /**
     * GATT services recently discovered from the remote device.
     *
     * Note that this can be changed, subscribe to [servicesFlow] to get notified of services
     * changes.
     */
    val services: List<GattService>
        get() = servicesFlow.value

    /**
     * Gets the service of the remote device by UUID.
     *
     * If multiple instances of the same service exist, the first instance of the services is
     * returned.
     */
    fun getService(uuid: UUID): GattService?

    /**
     * Reads the characteristic value from the server.
     *
     * @param characteristic a remote [GattCharacteristic] to read
     * @return the value of the characteristic
     */
    suspend fun readCharacteristic(characteristic: GattCharacteristic): Result<ByteArray>

    /**
     * Writes the characteristic value to the server.
     *
     * @param characteristic a remote [GattCharacteristic] to write
     * @param value a value to be written.
     * @return the result of the write operation
     * @throws IllegalArgumentException if the [characteristic] doesn't have the write property or
     *   the length of the [value] is greater than the maximum attribute length (512)
     */
    suspend fun writeCharacteristic(
        characteristic: GattCharacteristic,
        value: ByteArray
    ): Result<Unit>

    /** Returns a _cold_ [Flow] that contains the indicated value of the given characteristic. */
    fun subscribeToCharacteristic(characteristic: GattCharacteristic): Flow<ByteArray>
}
