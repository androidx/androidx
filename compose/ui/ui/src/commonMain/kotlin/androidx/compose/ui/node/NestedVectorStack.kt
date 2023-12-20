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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.MutableVector

internal class NestedVectorStack<T> {
    // number of vectors in the stack
    private var size = 0
    // holds the current "top" index for each vector
    private var currentIndexes = IntArray(16)
    private var vectors = arrayOfNulls<MutableVector<T>>(16)

    fun isNotEmpty(): Boolean {
        return size > 0 && currentIndexes[size - 1] >= 0
    }

    fun pop(): T {
        check(size > 0) {
            "Cannot call pop() on an empty stack. Guard with a call to isNotEmpty()"
        }
        val indexOfVector = size - 1
        val indexOfItem = currentIndexes[indexOfVector]
        val vector = vectors[indexOfVector]!!
        if (indexOfItem > 0) currentIndexes[indexOfVector]--
        else if (indexOfItem == 0) {
            vectors[indexOfVector] = null
            size--
        }
        return vector[indexOfItem]
    }

    fun push(vector: MutableVector<T>) {
        // if the vector is empty there is no reason for us to add it
        if (vector.isEmpty()) return
        val nextIndex = size
        // check to see that we have capacity to add another vector
        if (nextIndex >= currentIndexes.size) {
            currentIndexes = currentIndexes.copyOf(currentIndexes.size * 2)
            vectors = vectors.copyOf(vectors.size * 2)
        }
        currentIndexes[nextIndex] = vector.size - 1
        vectors[nextIndex] = vector
        size++
    }
}
