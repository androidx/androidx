/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.collection.MutableObjectList

/**
 * This class tracks the mutation to the provided [objectList] through the provided methods. On
 * mutation, the [onListMutated] lambda will be invoked.
 */
internal class MutableObjectListWithMutationTracking<T>(
    val objectList: MutableObjectList<T>,
    val onListMutated: () -> Unit,
) {
    val size: Int
        inline get() = objectList.size

    fun clear() {
        objectList.clear()
        onListMutated()
    }

    fun add(index: Int, element: T) {
        objectList.add(index, element)
        onListMutated()
    }

    fun removeAt(index: Int): T {
        return objectList.removeAt(index).also { onListMutated() }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun forEach(block: (T) -> Unit) = objectList.forEach(block)

    @Suppress("NOTHING_TO_INLINE", "AsCollectionCall") // Backs LayoutNode.children.
    inline fun asList(): List<T> = objectList.asList()

    @Suppress("NOTHING_TO_INLINE") inline operator fun get(index: Int): T = objectList[index]
}
