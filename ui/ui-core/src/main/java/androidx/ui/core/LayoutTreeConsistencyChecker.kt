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

package androidx.ui.core

import android.util.Log
import androidx.ui.util.fastForEach

/**
 * There are some contracts between the tree of LayoutNodes and the state of AndroidComposeView
 * which is hard to enforce but important to maintain. This method is intended to do the
 * work only during our tests and will iterate through the tree to validate the states consistency.
 */
internal class LayoutTreeConsistencyChecker(
    private val root: LayoutNode,
    private val duringMeasureLayout: () -> Boolean,
    private val relayoutNodes: DepthSortedSet,
    private val postponedMeasureRequests: List<LayoutNode>
) {
    fun assertConsistent() {
        val inconsistencyFound = !isTreeConsistent(root)
        if (inconsistencyFound) {
            logTree()
            Log.d("AndroidOwner", "List of relayoutNodes: $relayoutNodes")
            throw IllegalStateException("Inconsistency found! See the printed tree")
        }
    }

    private fun isTreeConsistent(node: LayoutNode): Boolean {
        if (!node.consistentLayoutState()) {
            return false
        }
        node.children.fastForEach {
            if (!isTreeConsistent(it)) {
                return@isTreeConsistent false
            }
        }
        return true
    }

    private fun LayoutNode.consistentLayoutState(): Boolean {
        if (this === root && needsRemeasure) {
            return relayoutNodes.contains(this)
        }
        val parent = parent
        if (parent != null && isPlaced) {
            if (needsRelayout) {
                if (needsRemeasure) {
                    // needsRelayout and needRemeasure both true are not expected
                    return false
                }
                if (parent.needsRelayout || parent.needsRemeasure || parent.isMeasuring) {
                    // relayout for the node will be done during the parent relayout which
                    // is already scheduled or will happen after the parent's measuring
                    return true
                }
                // relayout is scheduled
                return relayoutNodes.contains(this)
            }
            if (needsRemeasure) {
                if (postponedMeasureRequests.contains(this)) {
                    // this node is waiting to be measured by parent or if this will not happen
                    // `onRequestMeasure` will be called for all items in `postponedMeasureRequests`
                    return true
                }
                if (parent.isMeasuring || parent.isLayingOut) {
                    return parent.measureIteration != measureIteration
                } else {
                    if (affectsParentSize) {
                        // node and parent both not yet laid out -> parent remeasure
                        // should be scheduled
                        return parent.needsRemeasure
                    } else {
                        // node is not affecting parent size and parent relayout(or
                        // remeasure, as it includes relayout) is scheduled
                        return parent.needsRelayout || parent.needsRemeasure
                    }
                }
            }
        }
        return true
    }

    private fun nodeToString(node: LayoutNode): String {
        return with(StringBuilder()) {
            append(node)
            if (node.needsRemeasure) append("[needsRemeasure]")
            if (node.needsRelayout) append("[needsRelayout]")
            if (node.isMeasuring) append("[isMeasuring]")
            if (duringMeasureLayout()) append("[#${node.measureIteration}]")
            if (node.isLayingOut) append("[isLayingOut]")
            if (!node.isPlaced) append("[!isPlaced]")
            if (node.affectsParentSize) append("[affectsParentSize]")
            if (!node.consistentLayoutState()) {
                append("[INCONSISTENT]")
            }
            toString()
        }
    }

    /** Prints the nodes tree into the logs. */
    private fun logTree() {
        fun printSubTree(node: LayoutNode, depth: Int) {
            var childrenDepth = depth
            val nodeRepresentation = nodeToString(node)
            if (nodeRepresentation.isNotEmpty()) {
                val stringBuilder = StringBuilder()
                for (i in 0 until depth) {
                    stringBuilder.append("..")
                }
                stringBuilder.append(nodeRepresentation)
                Log.d("AndroidOwner", stringBuilder.toString())
                childrenDepth += 1
            }
            node.children.fastForEach { printSubTree(it, childrenDepth) }
        }
        Log.d("AndroidOwner", "Tree state:")
        printSubTree(root, 0)
    }
}
