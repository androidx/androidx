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

import kotlinx.coroutines.flow.Flow

/**
 * A scope for handling connect requests from remote devices.
 *
 * @property connectRequests connect requests from remote devices.
 *
 * @see BluetoothLe#openGattServer
 */
interface GattServerConnectScope {

    /**
     * A _hot_ flow of [GattServerConnectRequest].
     */
    val connectRequests: Flow<GattServerConnectRequest>

    /**
     * Updates the services of the opened GATT server.
     *
     * @param services the new services that will be notified to the clients.
     */
    fun updateServices(services: List<GattService>)
}
