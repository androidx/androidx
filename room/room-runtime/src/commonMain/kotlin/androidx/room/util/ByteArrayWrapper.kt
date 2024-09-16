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
package androidx.room.util

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmField

/** A [ByteArray] wrapper that implements equals and hashCode to be used as a Map key.typ */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class ByteArrayWrapper(@JvmField val array: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteArrayWrapper) return false
        return array contentEquals other.array
    }

    override fun hashCode(): Int = array.contentHashCode()
}
