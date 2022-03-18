/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.collection.integration

import androidx.collection.ArraySet
import java.util.function.IntFunction

/**
 * Integration (actually build) test for source compatibility for usages of ArraySet.
 */
@Suppress("unused")
fun arraySetSourceCompatibility(): Boolean {
    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    var arraySet: ArraySet<Int> = ArraySet()
    arraySet = ArraySet<Int>(5)
    @Suppress("UNUSED_VALUE")
    arraySet = ArraySet(arraySet)
    @Suppress("UNUSED_VALUE")
    arraySet = ArraySet(setOf())
    arraySet = ArraySet(arrayOf())

    arraySet.clear()
    arraySet.ensureCapacity(10)

    for (item in arraySet) {
        println(item)
    }

    @Suppress("RedundantSamConstructor", "DEPRECATION")
    return arraySet.isEmpty() && arraySet.remove(0) && arraySet.removeAll(arrayOf(1, 2)) &&
        arraySet.removeAt(0) == 0 && arraySet.contains(0) && arraySet.size == 0 &&
        arraySet.isEmpty() && arraySet.toArray() === arraySet.toArray(arrayOf<Number>()) &&
        arraySet + arrayOf(1) == arraySet - arrayOf(1) && arraySet == arrayOf(0) &&
        arraySet.toArray { value -> arrayOf(value) }.equals(
            arraySet.toArray(IntFunction { value -> arrayOf(value) })
        ) && arraySet.containsAll(listOf(1, 2))
}
