/*
 * Copyright 2024 The Android Open Source Project
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
 * Set of parameters which is used for add controlee Provisioned STS individual key case.
 *
 * @property subSessionId The ID of the ranging sub-session.
 * @property subSessionKey The sub-session key info to use for the ranging. This byte array is 16 or
 *   32-byte long.
 */
public class RangingControleeParameters(
    public val subSessionId: Int,
    public val subSessionKey: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RangingControleeParameters) return false

        if (subSessionId != other.subSessionId) return false
        if (subSessionKey != null) {
            if (other.subSessionKey == null) return false
            if (!subSessionKey.contentEquals(other.subSessionKey)) return false
        } else if (other.subSessionKey != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = subSessionId
        result = 31 * result + (subSessionKey?.contentHashCode() ?: 0)
        return result
    }
}
