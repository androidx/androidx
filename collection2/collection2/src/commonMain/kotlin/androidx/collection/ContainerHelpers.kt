/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.collection

import kotlin.jvm.JvmField
import kotlin.native.concurrent.SharedImmutable

@JvmField
@SharedImmutable
internal val DELETED = Any()

@JvmField
@SharedImmutable
internal val EMPTY_INTS = IntArray(0)

@JvmField
@SharedImmutable
internal val EMPTY_LONGS = LongArray(0)

@JvmField
@SharedImmutable
internal val EMPTY_OBJECTS = arrayOfNulls<Any>(0)

internal fun idealIntArraySize(need: Int): Int {
    return idealByteArraySize(need * 4) / 4
}

internal fun idealLongArraySize(need: Int): Int {
    return idealByteArraySize(need * 8) / 8
}

internal fun idealByteArraySize(need: Int): Int {
    for (i in 4..31) {
        val test = (1 shl i) - 12
        if (need <= test) {
            return test
        }
    }
    return need
}

// This is Arrays.binarySearch(), but doesn't do any argument validation.
internal fun IntArray.binarySearch(size: Int, value: Int): Int {
    var lo = 0
    var hi = size - 1
    while (lo <= hi) {
        val mid = lo + hi ushr 1
        val midVal = this[mid]
        if (midVal < value) {
            lo = mid + 1
        } else if (midVal > value) {
            hi = mid - 1
        } else {
            return mid // value found
        }
    }
    return lo.inv() // value not present
}

internal fun LongArray.binarySearch(size: Int, value: Long): Int {
    var lo = 0
    var hi = size - 1
    while (lo <= hi) {
        val mid = lo + hi ushr 1
        val midVal = this[mid]
        if (midVal < value) {
            lo = mid + 1
        } else if (midVal > value) {
            hi = mid - 1
        } else {
            return mid // value found
        }
    }
    return lo.inv() // value not present
}
