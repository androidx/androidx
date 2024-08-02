/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.uwb

/**
 * Represents a UWB device.
 *
 * @property address the device address (e.g., MAC address).
 */
public class UwbDevice(public val address: UwbAddress) {

    public companion object {
        /**
         * Creates a new UwbDevice for a given address.
         *
         * @throws [IllegalArgumentException] if address is invalid.
         */
        @JvmStatic
        public fun createForAddress(address: String): UwbDevice {
            return UwbDevice(UwbAddress(address))
        }

        /** Creates a new UwbDevice for a given address. */
        @JvmStatic
        public fun createForAddress(address: ByteArray): UwbDevice {
            return UwbDevice(UwbAddress(address))
        }
    }
}
