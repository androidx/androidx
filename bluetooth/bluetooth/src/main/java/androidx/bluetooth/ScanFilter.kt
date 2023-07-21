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
 * Criteria for filtering result from Bluetooth LE scans. A ScanFilter allows clients to restrict
 * scan results to only those that are of interest to them.
 */
class ScanFilter(
    /* The scan filter for the remote device address. Null if filter is not set. */
    val deviceAddress: BluetoothAddress? = null,

    /* The scan filter for manufacturer id. MANUFACTURER_FILTER_NONE if filter is not set. */
    val manufacturerId: Int = MANUFACTURER_FILTER_NONE,

    /* The scan filter for manufacturer data. Null if filter is not set. */
    val manufacturerData: ByteArray? = null,

    /* The partial filter on manufacturerData. Null if filter is not set. */
    val manufacturerDataMask: ByteArray? = null,

    /* The scan filter for service data uuid. Null if filter is not set. */
    val serviceDataUuid: UUID? = null,

    /* The scan filter for service data. Null if filter is not set. */
    val serviceData: ByteArray? = null,

    /* The partial filter on service data. Null if filter is not set. */
    val serviceDataMask: ByteArray? = null,

    /* The scan filter for service uuid. Null if filter is not set. */
    val serviceUuid: UUID? = null,

    /* The partial filter on service uuid. Null if filter is not set. */
    val serviceUuidMask: UUID? = null
) {
    companion object {
        const val MANUFACTURER_FILTER_NONE: Int = -1
    }

    init {
        if (manufacturerId < 0 && manufacturerId != MANUFACTURER_FILTER_NONE) {
            throw IllegalArgumentException("invalid manufacturerId")
        }

        if (manufacturerDataMask != null) {
            if (manufacturerData == null) {
                throw IllegalArgumentException(
                    "manufacturerData is null while manufacturerDataMask is not null")
            }

            if (manufacturerData.size != manufacturerDataMask.size) {
                throw IllegalArgumentException(
                    "size mismatch for manufacturerData and manufacturerDataMask")
            }
        }

        if (serviceDataMask != null) {
            if (serviceData == null) {
                throw IllegalArgumentException(
                    "serviceData is null while serviceDataMask is not null")
            }

            if (serviceData.size != serviceDataMask.size) {
                throw IllegalArgumentException(
                    "size mismatch for service data and service data mask")
            }
        }

        if (serviceUuidMask != null && serviceUuid == null) {
            throw IllegalArgumentException("uuid is null while uuidMask is not null")
        }
    }
}