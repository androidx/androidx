/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.data

import android.os.Bundle
import java.util.Objects

/** Utility methods for working with Bundles. */
internal object BundlesUtil {

    /**
     * Compares two Bundles recursively and returns `true` if they are equal.
     *
     * Equality in this case means that both bundles contain the same set of keys and their
     * corresponding values are all equal (using the [Object.equals] method).
     */
    @JvmStatic
    fun equals(a: Bundle?, b: Bundle?): Boolean {
        if (a == b) {
            return true
        } else if (a == null || b == null) {
            return false
        } else if (a.size() != b.size()) {
            return false
        }
        for (key in a.keySet()) {
            val aValue = a[key]
            val bValue = b[key]
            if (aValue is Bundle && bValue is Bundle) {
                if (!equals(aValue as Bundle?, bValue as Bundle?)) {
                    return false
                }
            } else if (aValue == null) {
                if (bValue != null || !b.containsKey(key)) {
                    return false
                }
            } else if (!Objects.deepEquals(aValue, bValue)) {
                return false
            }
        }
        return true
    }

    /** Calculates a hashCode for a Bundle, examining all keys and values. */
    @JvmStatic
    fun hashCode(b: Bundle?): Int {
        if (b == null) {
            return 0
        }
        val keySet = b.keySet()
        val hashCodes = IntArray(keySet.size * 2)
        var i = 0
        for (key in keySet) {
            hashCodes[i++] = Objects.hashCode(key)
            val value = b[key]
            val valueHashCode: Int =
                if (value is Bundle) {
                    hashCode(value as Bundle?)
                } else {
                    Objects.hashCode(value)
                }
            hashCodes[i++] = valueHashCode
        }
        return hashCodes.contentHashCode()
    }
}
