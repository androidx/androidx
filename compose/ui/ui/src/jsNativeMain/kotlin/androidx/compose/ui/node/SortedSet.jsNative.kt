/*
 * Copyright 2023 The Android Open Source Project
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


/**
 * Implements [SortedSet] via a min-heap (implemented via an array) and a hash-map mapping the
 * elements to their indices in the heap.
 *
 * The performance of this implementation is:
 * - [add], [remove]: O(logN), due to the heap.
 * - [first], [contains]: O(1), due to the hash map.
 */
internal actual class SortedSet<E> actual constructor(
    private val comparator: Comparator<in E>
) {

    /**
     * Compares two elements using the [comparator].
     */
    private inline operator fun E.compareTo(value: E): Int = comparator.compare(this, value)

    /**
     * The heap array.
     */
    private val itemTree = arrayListOf<E>()

    /**
     * Returns whether the index is the root of the tree.
     */
    private val Int.isRootIndex get() = this == 0

    /**
     * Returns the index of the parent node.
     */
    private val Int.parentIndex get() = (this - 1) shr 1

    /**
     * Returns the index of the left child node.
     */
    private val Int.leftChildIndex get() = (this shl 1) + 1

    /**
     * Returns the index of the right child node.
     */
    private val Int.rightChildIndex get() = (this shl 1) + 2

    /**
     * Maps each element to its index in [itemTree].
     */
    private val indexByElement = mutableMapOf<E, Int>()

    /**
     * Inserts [element], if it's not already in the set.
     *
     * @returns whether actually inserted.
     */
    actual fun add(element: E): Boolean {
        if (element in indexByElement) {
            return false
        }

        // Insert the item at the rightmost leaf
        val index = itemTree.size
        itemTree.add(element)
        indexByElement[element] = index  // This is the initial index; heapifyUp will update it

        // Fix the heap
        heapifyUp(index)

        return true
    }

    /**
     * Removes [element], if it's in the set.
     *
     * @return whether actually removed.
     */
    actual fun remove(element: E): Boolean {
        // Get the index in the tree and remove it
        val index = indexByElement.remove(element) ?: return false

        // Remove the rightmost leaf (to move it in place of the remove element)
        val rightMostLeafElement = itemTree.removeLast()

        // If the removed element is the rightmost leaf, then there's no need to move it, or to fix
        // the heap. This also takes care of the case when the set is empty after removal.
        if (index < itemTree.size) {
            itemTree[index] = rightMostLeafElement
            indexByElement[rightMostLeafElement] = index

            // Restore min-heap invariant
            if (!index.isRootIndex && (itemTree[index.parentIndex] >= itemTree[index])) {
                heapifyUp(index)
            } else {
                heapifyDown(index)
            }
        }

        return true
    }

    /**
     * Returns the smallest item in the set, according to [comparator].
     */
    actual fun first() = itemTree[0]

    /**
     * Returns whether the set is empty.
     */
    actual fun isEmpty(): Boolean = itemTree.isEmpty()

    /**
     * Returns whether the set contains the given element.
     */
    actual fun contains(element: E) = element in indexByElement

    /**
     * Bubbles up the element at the given index until the min-heap invariant is restored.
     */
    private fun heapifyUp(index: Int) {
        val element = itemTree[index]  // The element being bubbled up
        var currentIndex = index  // The index we're currently comparing to its parent

        while (!currentIndex.isRootIndex) {
            val parentIndex = currentIndex.parentIndex

            // If the order is correct, stop
            if (itemTree[parentIndex] <= element) {
                break
            }

            // Swap
            swap(currentIndex, parentIndex)

            // Continue with parent
            currentIndex = parentIndex
        }
    }

    /**
     * Sinks down the element at the given index until the min-heap invariant is restored.
     */
    private fun heapifyDown(index: Int) {
        val element = itemTree[index]  // The element being sunk down
        var currentIndex = index  // The index we're currently comparing to its children

        while (true) {
            val leftChildIndex = currentIndex.leftChildIndex
            if (leftChildIndex >= itemTree.size) {
                break
            }
            val leftChildElement = itemTree[leftChildIndex]
            val rightChildIndex = currentIndex.rightChildIndex

            val indexOfSmallerElement: Int
            val smallerElement: E
            if (rightChildIndex >= itemTree.size) {  // There's no right child
                // Look at left child
                indexOfSmallerElement = leftChildIndex
                smallerElement = leftChildElement
            } else {
                val rightChildElement = itemTree[rightChildIndex]
                // Look at the smaller child
                if (leftChildElement < rightChildElement) {
                    indexOfSmallerElement = leftChildIndex
                    smallerElement = leftChildElement
                } else {
                    indexOfSmallerElement = rightChildIndex
                    smallerElement = rightChildElement
                }
            }

            if (element <= smallerElement) {
                break
            }

            swap(currentIndex, indexOfSmallerElement)
            currentIndex = indexOfSmallerElement
        }
    }

    /**
     * Swaps the elements at the given indices in [itemTree], and updates the indices in
     * [indexByElement].
     */
    private fun swap(index1: Int, index2: Int) {
        // Get the items
        val item1 = itemTree[index1]
        val item2 = itemTree[index2]

        // Swap the items
        itemTree[index1] = item2
        itemTree[index2] = item1

        // Update the indices
        indexByElement[item1] = index2
        indexByElement[item2] = index1
    }
}
