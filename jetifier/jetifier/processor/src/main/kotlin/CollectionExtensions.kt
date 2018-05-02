/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor

/**
 * Creates a cartesian product from the given lists.
 *
 * Example: ((1, 2), (a, b))
 * Result: ((1, a), (1, b), (2, a), (2,b))
 */
fun <T> List<List<T>>.cartesianProduct(): List<List<T>> {
    return cartesianProductInternal(this.size - 1, this)
}

private fun <T> cartesianProductInternal(index: Int, lists: List<List<T>>): List<MutableList<T>> {
    val result = mutableListOf<MutableList<T>>()
    if (index == -1) {
        result.add(mutableListOf())
    } else {
        for (obj in lists[index]) {
            for (set in cartesianProductInternal(index - 1, lists)) {
                set.add(obj)
                result.add(set)
            }
        }
    }
    return result
}