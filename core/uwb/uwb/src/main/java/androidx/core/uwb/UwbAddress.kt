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

import com.google.common.io.BaseEncoding

/**
 * Represents a UWB address.
 *
 * @property address the device address (eg, MAC address).
 */
public class UwbAddress(public val address: ByteArray) {

    /** @throws [IllegalArgumentException] if address is invalid. */
    public constructor(address: String) : this(BASE_16_SEPARATOR.decode(address))

    public companion object {
        private val BASE_16_SEPARATOR: BaseEncoding = BaseEncoding.base16().withSeparator(":", 2)
    }

    /** Checks that two UwbAddresses are equal. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UwbAddress

        if (!address.contentEquals(other.address)) return false

        return true
    }

    /** Returns the hashcode. */
    override fun hashCode(): Int {
        return address.contentHashCode()
    }

    /** Returns the string format of [UwbAddress]. */
    override fun toString(): String {
        return BASE_16_SEPARATOR.encode(address)
    }
}
