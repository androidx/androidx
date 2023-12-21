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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.mutableVectorOf

/**
 * Tracks the nodes being positioned and dispatches OnPositioned callbacks when we finished
 * the measure/layout pass.
 */
internal class OnPositionedDispatcher {
    private val layoutNodes = mutableVectorOf<LayoutNode>()
    private var cachedNodes: Array<LayoutNode?>? = null

    fun isNotEmpty() = layoutNodes.isNotEmpty()

    fun onNodePositioned(node: LayoutNode) {
        layoutNodes += node
        node.needsOnPositionedDispatch = true
    }

    fun onRootNodePositioned(rootNode: LayoutNode) {
        layoutNodes.clear()
        layoutNodes += rootNode
        rootNode.needsOnPositionedDispatch = true
    }

    fun dispatch() {
        // sort layoutNodes so that the root is at the end and leaves are at the front
        layoutNodes.sortWith(DepthComparator)
        val cache: Array<LayoutNode?>
        val size = layoutNodes.size
        val cachedNodes = this.cachedNodes
        if (cachedNodes == null || cachedNodes.size < size) {
            cache = arrayOfNulls(maxOf(MinArraySize, layoutNodes.size))
        } else {
            cache = cachedNodes
        }
        this.cachedNodes = null

        // copy to cache to prevent reentrancy being a problem
        for (i in 0 until size) {
            cache[i] = layoutNodes[i]
        }
        layoutNodes.clear()
        for (i in size - 1 downTo 0) {
            val layoutNode = cache[i]!!
            if (layoutNode.needsOnPositionedDispatch) {
                dispatchHierarchy(layoutNode)
            }
        }
        this.cachedNodes = cache
    }

    private fun dispatchHierarchy(layoutNode: LayoutNode) {
        // TODO(lmr): investigate a non-recursive version of this that leverages
        //  node traversal
        layoutNode.dispatchOnPositionedCallbacks()
        layoutNode.needsOnPositionedDispatch = false

        layoutNode.forEachChild { child ->
            dispatchHierarchy(child)
        }
    }

    internal companion object {
        private const val MinArraySize = 16

        private object DepthComparator : Comparator<LayoutNode> {
            override fun compare(a: LayoutNode, b: LayoutNode): Int {
                val depthDiff = b.depth.compareTo(a.depth)
                if (depthDiff != 0) {
                    return depthDiff
                }
                return a.hashCode().compareTo(b.hashCode())
            }
        }
    }
}
