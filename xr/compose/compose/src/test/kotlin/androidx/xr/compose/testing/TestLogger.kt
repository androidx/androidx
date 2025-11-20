/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.testing

import androidx.xr.compose.subspace.node.Logger
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose

/**
 * Test implementation of Logger that supports asserting logged actions.
 *
 * Logger may be called many times when manipulating nodes so it's recommended to only enable the
 * logger for small code blocks, ideally a single statement. To do this, utilize the [log] method,
 * which first clears any existing logs and enables the logger while executing a given block of
 * code. You may then assert as many conditions as you'd like before repeating. If you'd like to
 * confirm all logs have been asserted use [assertIsEmpty]. An example sequence looks like:
 * ```
 * val owner = AndroidComposeSpatialElement()
 * val logger = TestLogger().also { owner.logger = it }
 * val node = SubspaceLayoutNode()
 * logger
 *     .log { owner.root.insertAt(0, node) }
 *     .assertNodeInserted(node)
 *     .assertIsEmpty()
 *     .log { owner.root.removeAt(0, 1) }
 *     .assertNodeRemoved(node)
 *     .assertIsEmpty()
 * ```
 *
 * The `node` parameter of the assert methods may be any of the following:
 * - An instance of SubspaceLayoutNode
 * - A string matching the testTag value of a SubspaceLayoutNode
 * - An instance of SubspaceLayoutModifierNode
 * - A string matching the SubspaceLayoutModifierNode type, e.g. "OffsetNode"
 */
internal class TestLogger : Logger {
    private var enabled: Boolean = false
    private val actions = mutableListOf<Any>()

    override fun nodeInserted(node: Any, parent: Any?, atIndex: Int) {
        if (!enabled) return
        actions.add(NodeInsert(node, parent, atIndex))
    }

    override fun nodeMoved(node: Any, parent: Any?, fromIndex: Int, toIndex: Int) {
        if (!enabled) return
        actions.add(NodeMove(node, parent, fromIndex, toIndex))
    }

    override fun nodeUpdated(node: Any, parent: Any?, atIndex: Int) {
        if (!enabled) return
        actions.add(NodeUpdate(node, parent, atIndex))
    }

    override fun nodeRemoved(node: Any, parent: Any?, fromIndex: Int) {
        if (!enabled) return
        actions.add(NodeRemove(node, parent, fromIndex))
    }

    override fun measureRequested(node: Any) {
        if (!enabled) return
        actions.add(MeasureRequest(node))
    }

    override fun layoutRequested(node: Any) {
        if (!enabled) return
        actions.add(LayoutRequest(node))
    }

    override fun nodeMeasured(node: Any, constraints: VolumeConstraints, size: IntVolumeSize) {
        if (!enabled) return
        actions.add(NodeMeasure(node, constraints, size))
    }

    override fun nodePlaced(node: Any, pose: Pose) {
        if (!enabled) return
        actions.add(NodePlace(node, pose))
    }

    /**
     * Asserts that [nodeInserted] was called with the given [node] and an optional index. See
     * [TestLogger] for possible values of [node].
     */
    fun assertNodeInserted(node: Any, atIndex: Int? = null): TestLogger {
        val match =
            nextMatch<NodeInsert>("Insert action not found for $node") {
                checkNodesMatch(node, it.node)
            }
        if (atIndex != null && match.atIndex != atIndex) {
            throw AssertionError("Actual insert index is ${match.atIndex}, expected $atIndex")
        }
        return this
    }

    /**
     * Asserts that [nodeMoved] was called with the given [node] and an optional indices. See
     * [TestLogger] for possible values of [node].
     */
    fun assertNodeMoved(node: Any, fromIndex: Int? = null, toIndex: Int? = null): TestLogger {
        val match =
            nextMatch<NodeMove>("Move action not found for $node") {
                checkNodesMatch(node, it.node)
            }
        if (fromIndex != null && match.fromIndex != fromIndex) {
            throw AssertionError(
                "Actual move from index is ${match.fromIndex}, expected $fromIndex"
            )
        }
        if (toIndex != null && match.toIndex != toIndex) {
            throw AssertionError("Actual move to index is ${match.toIndex}, expected $toIndex")
        }
        return this
    }

    /**
     * Asserts that [nodeUpdated] was called with the given [node] and an optional index. See
     * [TestLogger] for possible values of [node].
     */
    fun assertNodeUpdated(node: Any, atIndex: Int? = null): TestLogger {
        val match =
            nextMatch<NodeUpdate>("Update action not found for $node") {
                checkNodesMatch(node, it.node)
            }
        if (atIndex != null && match.atIndex != atIndex) {
            throw AssertionError("Actual update index is ${match.atIndex}, expected $atIndex")
        }
        return this
    }

    /**
     * Asserts that [nodeRemoved] was called with the given [node] and an optional index. See
     * [TestLogger] for possible values of [node].
     */
    fun assertNodeRemoved(node: Any, fromIndex: Int? = null): TestLogger {
        val match =
            nextMatch<NodeRemove>("Remove action not found for $node") {
                checkNodesMatch(node, it.node)
            }
        if (fromIndex != null && match.fromIndex != fromIndex) {
            throw AssertionError("Actual remove index is ${match.fromIndex}, expected $fromIndex")
        }
        return this
    }

    /**
     * Asserts that [measureRequested] was called with the given [node]. See [TestLogger] for
     * possible values of [node].
     */
    fun assertMeasureRequested(node: Any): TestLogger {
        val match =
            nextMatch<MeasureRequest>("Measure request action not found for $node") {
                checkNodesMatch(node, it.node)
            }
        return this
    }

    /**
     * Asserts that [layoutRequested] was called with the given [node]. See [TestLogger] for
     * possible values of [node].
     */
    fun assertLayoutRequested(node: Any): TestLogger {
        val match =
            nextMatch<LayoutRequest>("Layout request action not found for $node") {
                checkNodesMatch(node, it.node)
            }
        return this
    }

    /**
     * Asserts that [nodeMeasured] was called with the given [node] and an optional
     * constraints/size. See [TestLogger] for possible values of [node].
     */
    fun assertNodeMeasured(
        node: Any,
        constraints: VolumeConstraints? = null,
        size: IntVolumeSize? = null,
    ): TestLogger {
        val match =
            nextMatch<NodeMeasure>("Measure action not found for $node") {
                checkNodesMatch(node, it.node)
            }
        if (constraints != null && match.constraints != constraints) {
            throw AssertionError(
                "Actual measure constraints are ${match.constraints}, expected $constraints"
            )
        }
        if (size != null) {
            if (match.constraints != constraints) {
                throw AssertionError(
                    "Actual constraints are ${match.constraints}, expected $constraints"
                )
            }
            if (match.size != size) {
                throw AssertionError("Actual size is ${match.size}, expected $size")
            }
        }
        return this
    }

    /**
     * Asserts that [nodePlaced] was called with the given [node] and an optional pose. See
     * [TestLogger] for possible values of [node].
     */
    fun assertNodePlaced(node: Any, pose: Pose? = null): TestLogger {
        val match =
            nextMatch<NodePlace>("Place action not found for $node") {
                checkNodesMatch(node, it.node)
            }
        if (pose != null && Pose.distance(match.pose, pose) > 5) {
            throw AssertionError("Actual place pose is ${match.pose}, expected $pose")
        }
        return this
    }

    /**
     * Clears existing logged actions, enables the logger, run the [block] of code, and disables the
     * logger. This method should be called with a minimal amount of logic, followed by one or more
     * chained assertions. Repeat the [log] -> assertions flow as many times as necessary.
     */
    fun log(block: () -> Unit): TestLogger {
        clear()
        enabled = true
        block()
        enabled = false
        return this
    }

    /**
     * Asserts that no more actions are left after asserting expected logs or that no actions were
     * ever logged.
     */
    fun assertIsEmpty(): TestLogger {
        if (actions.size > 0) {
            throw AssertionError("Expected 0 actions remaining but found ${actions.size}\n$actions")
        }
        return this
    }

    /**
     * Clears the current state of the logger, including all previously logged actions. This is
     * called automatically by [log].
     */
    fun clear(): TestLogger {
        actions.clear()
        return this
    }

    private inline fun <reified T> nextMatch(errorMsg: String, nodeMatcher: (T) -> Boolean): T {
        var position = 0

        while (position < actions.size) {
            val currentAction = actions[position]
            if (currentAction is T && nodeMatcher(currentAction)) {
                actions.removeAt(position)
                return currentAction
            }
            ++position
        }

        throw AssertionError(errorMsg)
    }

    private fun checkNodesMatch(inputNode: Any, actionNode: Any): Boolean {
        if (inputNode is String) {
            val asString = actionNode.toString()
            if (inputNode == asString || asString.contains(".$inputNode@")) {
                return true
            }
        }

        return inputNode == actionNode
    }

    private data class NodeInsert(val node: Any, val parent: Any?, val atIndex: Int)

    private data class NodeMove(
        val node: Any,
        val parent: Any?,
        val fromIndex: Int,
        val toIndex: Int,
    )

    private data class NodeUpdate(val node: Any, val parent: Any?, val atIndex: Int)

    private data class NodeRemove(val node: Any, val parent: Any?, val fromIndex: Int)

    private data class MeasureRequest(val node: Any)

    private data class LayoutRequest(val node: Any)

    private data class NodeMeasure(
        val node: Any,
        val constraints: VolumeConstraints,
        val size: IntVolumeSize,
    )

    private data class NodePlace(val node: Any, val pose: Pose)
}
