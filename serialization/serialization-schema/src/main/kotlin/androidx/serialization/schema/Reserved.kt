/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.serialization.schema

/**
 * A collection of reserved IDs or names for a message class, enum class, or service interface.
 *
 * @property ids Message field, enum value, or service action IDs to reserve.
 * @property names Message field, enum value, or service action names to reserve.
 * @property idRanges Ranges of IDs to reserve in bulk.
 * @see androidx.serialization.Reserved
 */
data class Reserved(
    val ids: Set<Int> = emptySet(),
    val names: Set<String> = emptySet(),
    val idRanges: Set<IntRange> = emptySet()
) {
    operator fun contains(id: Int): Boolean {
        return id in ids || idRanges.any { id in it }
    }

    operator fun contains(name: String): Boolean {
        return name in names
    }

    companion object {
        private val EMPTY_INSTANCE = Reserved()

        fun empty(): Reserved = EMPTY_INSTANCE
    }
}