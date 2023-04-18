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

/**
 * A single class to provide a way to adjust advertising preferences and advertise data packet.
 *
 */
class AdvertiseParams(
    /* Whether the device address will be included in the advertisement packet. */
    @Suppress("GetterSetterNames")
    @get:JvmName("shouldIncludeDeviceAddress")
    val shouldIncludeDeviceAddress: Boolean = false,
    /* Whether the device name will be included in the advertisement packet. */
    @Suppress("GetterSetterNames")
    @get:JvmName("shouldIncludeDeviceName")
    val shouldIncludeDeviceName: Boolean = false,
    /* Whether the advertisement will indicate connectable. */
    val isConnectable: Boolean = false,
    /* Whether the advertisement will be discoverable. */
    val isDiscoverable: Boolean = false,
    /* Advertising time limit in milliseconds. */
    val timeoutMillis: Int = 0,
    /**
     * A map of manufacturer specific data.
     * <p>
     * Please refer to the Bluetooth Assigned Numbers document provided by the <a
     * href="https://www.bluetooth.org">Bluetooth SIG</a> for a list of existing company
     * identifiers.
     *
     * Map<Int> Manufacturer ID assigned by Bluetooth SIG.
     * Map<ByteArray> Manufacturer specific data
     */
    val manufacturerData: Map<Int, ByteArray> = emptyMap(),
    /**
     * A map of service data to advertise data.
     *
     * UUID 16-bit UUID of the service the data is associated with
     * ByteArray serviceData Service data
     */
    val serviceData: Map<UUID, ByteArray> = emptyMap(),
    /**
     * A list of service UUID to advertise data.
     *
     * UUID A service UUID to be advertised.
     */
    val serviceUuids: List<UUID> = emptyList()
)
