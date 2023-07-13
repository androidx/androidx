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

import androidx.annotation.IntRange
import java.util.UUID

/**
 * A class to provide a way to adjust advertising preferences and advertise data packet.
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
    /**
     * Whether the advertisement will be discoverable.
     *
     * Please note that it would be ignored under API level 34 and [isConnectable] would be
     * used instead.
     */
    val isDiscoverable: Boolean = false,
    /**
     * Advertising duration in milliseconds
     *
     * It must not exceed 655350 milliseconds. A value of 0 means advertising continues
     * until it is stopped explicitly.
     */
    @IntRange(from = 0, to = 655350) val durationMillis: Int = 0,

    /**
     * A map of company identifiers to manufacturer specific data.
     * <p>
     * Please refer to the Bluetooth Assigned Numbers document provided by the <a
     * href="https://www.bluetooth.org">Bluetooth SIG</a> for the list of existing company
     * identifiers.
     */
    val manufacturerData: Map<Int, ByteArray> = emptyMap(),
    /**
     * A map of 16-bit UUIDs of the services to corresponding additional service data.
     */
    val serviceData: Map<UUID, ByteArray> = emptyMap(),
    /**
     * A list of service UUIDs to advertise.
     */
    val serviceUuids: List<UUID> = emptyList()
)
