/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.savedstate.benchmark.utils

import kotlinx.serialization.Serializable

@Serializable internal data class ListIntData(val value: List<Int>)

@Serializable internal data class ListStringData(val value: List<String>)

@Serializable
internal data class BooleanArrayData(val value: BooleanArray) {
    override fun equals(other: Any?) = value.contentEquals((other as? BooleanArrayData)?.value)

    override fun hashCode() = value.contentHashCode()
}

@Serializable
internal data class CharArrayData(val value: CharArray) {
    override fun equals(other: Any?) = value.contentEquals((other as? CharArrayData)?.value)

    override fun hashCode() = value.contentHashCode()
}

@Serializable
internal data class DoubleArrayData(val value: DoubleArray) {
    override fun equals(other: Any?) = value.contentEquals((other as? DoubleArrayData)?.value)

    override fun hashCode() = value.contentHashCode()
}

@Serializable
internal data class FloatArrayData(val value: FloatArray) {
    override fun equals(other: Any?) = value.contentEquals((other as? FloatArrayData)?.value)

    override fun hashCode() = value.contentHashCode()
}

@Serializable
internal data class IntArrayData(val value: IntArray) {
    override fun equals(other: Any?) = value.contentEquals((other as? IntArrayData)?.value)

    override fun hashCode() = value.contentHashCode()
}

@Serializable
internal data class LongArrayData(val value: LongArray) {
    override fun equals(other: Any?) = value.contentEquals((other as? LongArrayData)?.value)

    override fun hashCode() = value.contentHashCode()
}

@Serializable
internal data class StringArrayData(val value: Array<String>) {
    override fun equals(other: Any?) = value.contentDeepEquals((other as? StringArrayData)?.value)

    override fun hashCode() = value.contentDeepHashCode()
}
