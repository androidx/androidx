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

/**
 * Represents a connect request from a remote device.
 *
 * @property device the remote device connecting to the server
 */
class GattServerConnectRequest
internal constructor(
    private val session: GattServer.Session,
) {

    val device: BluetoothDevice
        get() = session.device

    /**
     * Accepts the connect request and handles incoming requests after that.
     *
     * Requests from the client before calling this should be saved.
     *
     * @param block a block of code that is invoked after the connection is made.
     * @see GattServerSessionScope
     */
    suspend fun accept(block: suspend GattServerSessionScope.() -> Unit) {
        return session.acceptConnection(block)
    }

    /**
     * Rejects the connect request.
     *
     * All the requests from the client will be rejected.
     */
    fun reject() {
        return session.rejectConnection()
    }
}
