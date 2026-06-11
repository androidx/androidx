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

package androidx.compose.remote.creation.compose.capture

import androidx.collection.MutableObjectIntMap
import androidx.compose.remote.creation.compose.state.BaseRemoteState
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import java.util.ArrayList
import java.util.HashMap

/**
 * Buffers drawing operations and tracks expression roots. It allows for global optimizations such
 * as common subexpression elimination and operation reordering before operations are recorded into
 * the document.
 */
internal class CanvasOperationBuffer {

    /**
     * Represents a node in the tree of operations, corresponding to a lexical scope (e.g., a branch
     * of a conditional or a loop). Spans are used to determine the ideal location to hoist common
     * subexpressions.
     */
    internal class Span(val parent: Span?, val depth: Int) {
        val operations = ArrayList<SpanOp>()
        var child: Span? = null
        var next: Span? = null

        fun record() {
            for (i in 0 until operations.size) {
                operations[i].action()
            }
        }

        fun sortAllSpans() {
            topologicalSort()
            var currentChild = child
            while (currentChild != null) {
                currentChild.sortAllSpans()
                currentChild = currentChild.next
            }
        }

        private fun topologicalSort() {
            val sortedOps = ArrayList<SpanOp>(operations.size)

            for (i in 0 until operations.size) {
                operations[i].visited = false
            }

            fun dfs(op: SpanOp) {
                if (op.visited) return
                op.visited = true
                for (i in 0 until op.deps.size) {
                    val dep = op.deps[i]
                    if (dep.idealSpan == this) {
                        dfs(dep)
                    }
                }
                sortedOps.add(op)
            }

            for (i in 0 until operations.size) {
                dfs(operations[i])
            }

            operations.clear()
            operations.addAll(sortedOps)
        }
    }

    /**
     * Represents a single operation (or a common subexpression evaluation) within a [Span]. It
     * tracks its dependencies and can be hoisted to a higher span if it is used across multiple
     * spans.
     */
    internal class SpanOp(var idealSpan: Span, val action: () -> Unit) {
        val deps = ArrayList<SpanOp>()
        var visited = false
    }

    internal var spanTreeRoot = Span(null, 0)
    internal var insertPoint = spanTreeRoot
    private val operationMap = HashMap<RemoteStateCacheKey, SpanOp>()
    private val usageMap = HashMap<RemoteStateCacheKey, ArrayList<SpanOp>>()
    private val expressionMap = HashMap<RemoteStateCacheKey, BaseRemoteState<*>>()
    internal var lastRenderingOp: SpanOp? = null

    /**
     * Records a rendering operation and ensures it is correctly ordered.
     *
     * This method creates a new [SpanOp] for the operation, adds it to the current insert point,
     * and chains it to the previous rendering operation by adding a dependency. This guarantees
     * that rendering operations preserve their relative order during execution.
     *
     * @param action The action that performs the rendering operation.
     * @return The newly created and recorded operation.
     */
    public fun recordRenderingOp(action: () -> Unit): SpanOp {
        val op = SpanOp(insertPoint, action = action)
        insertPoint.operations.add(op)
        lastRenderingOp?.let { op.deps.add(it) }
        lastRenderingOp = op
        return op
    }

    /**
     * Registers the state variables (roots) used by a rendering operation.
     *
     * This method establishes dependencies between the given operation [op] and the [states] it
     * depends on. It updates the dependency graph used for hoisting and common sub-expression
     * elimination by recording usages and adding dependencies to the operation.
     *
     * @param op The operation that uses the states.
     * @param states The state variables (roots) used by the operation.
     */
    public fun addRoots(op: SpanOp, vararg states: Any?) {
        for (state in states) {
            if (state is BaseRemoteState<*>) {
                expressionMap[state.cacheKey] = state
                usageMap.getOrPut(state.cacheKey) { ArrayList() }.add(op)
            }
        }
    }

    /**
     * Creates a new child span under the current insert point.
     *
     * This method creates a new nested scope in the execution tree, which is used for conditional
     * blocks or loops. The new span is added to the parent's list of children.
     *
     * @return The newly created child span.
     */
    public fun createChildSpan(): Span {
        val parent = insertPoint
        val childSpan = Span(parent, parent.depth + 1)
        if (parent.child == null) {
            parent.child = childSpan
        } else {
            var current = parent.child
            while (current?.next != null) {
                current = current.next
            }
            current?.next = childSpan
        }
        return childSpan
    }

    /**
     * Processes all recorded operations, applies optimizations, and writes them to the document.
     *
     * This method performs the following steps:
     * 1. Applies Common Subexpression Elimination (CSE) and hoisting to state expressions.
     * 2. Discovers the ideal spans for rendering operations based on dependency chains.
     * 3. Records all operations into the document by executing them in the correct order.
     * 4. Resets the buffer state, clearing maps and resetting the span tree root for the next use.
     *
     * @param creationState The state used to write operations to the document and allocate variable
     *   IDs.
     */
    public fun flush(creationState: RemoteComposeCreationState) {
        commonSubExpressionElimination(creationState)
        spanTreeRoot.sortAllSpans()
        spanTreeRoot.record()

        // Reset for next flush
        spanTreeRoot = Span(null, 0)
        insertPoint = spanTreeRoot
        operationMap.clear()
        usageMap.clear()
        expressionMap.clear()
        lastRenderingOp = null
    }

    private fun commonSubExpressionElimination(creationState: RemoteComposeCreationState) {
        val counts = MutableObjectIntMap<RemoteStateCacheKey>()
        val commonOps = mutableSetOf<RemoteOperationCacheKey>()

        val visitedDuringTraversal = mutableSetOf<RemoteStateCacheKey>()

        // First, initialize counts with direct usages from usageMap
        for ((key, usages) in usageMap) {
            counts.put(key, usages.size)
            if (usages.size >= 2 && key is RemoteOperationCacheKey) {
                commonOps.add(key)
            }
        }

        // Then, traverse from all roots in usageMap
        for (key in usageMap.keys) {
            traverseCacheKey(key, counts, commonOps, visitedDuringTraversal)
        }

        if (commonOps.isEmpty()) {
            return
        }

        // Pass 1: Determine idealSpan for all commonOps
        val keyToIdealSpan = mutableMapOf<RemoteOperationCacheKey, Span>()

        // First, initialize with direct usages from usageMap
        for (key in usageMap.keys) {
            if (key is RemoteOperationCacheKey) {
                val usages = usageMap[key]
                if (!usages.isNullOrEmpty()) {
                    var idealSpan = usages[0].idealSpan
                    for (j in 1 until usages.size) {
                        idealSpan = findCommonAncestor(idealSpan, usages[j].idealSpan)
                    }
                    keyToIdealSpan[key] = idealSpan
                }
            }
        }

        // Then, propagate from parents to children (pre-order)
        val visited = mutableSetOf<RemoteOperationCacheKey>()
        for (key in usageMap.keys) {
            if (key is RemoteOperationCacheKey) {
                keyToIdealSpan[key]?.let { propagateSpan(key, it, keyToIdealSpan, visited) }
            }
        }

        // Pass 2: Create SpanOps and add to operationMap
        val spanToExpressions = mutableMapOf<Span, ArrayList<SpanOp>>()

        for (key in commonOps) {
            emitExpression(key, commonOps, keyToIdealSpan, spanToExpressions, creationState)
        }

        // Prepend collected expressions to spans
        for ((span, expressions) in spanToExpressions) {
            span.operations.addAll(0, expressions)
        }

        // Add dependencies from rendering operations to CSEs
        for ((key, usages) in usageMap) {
            if (key is RemoteOperationCacheKey) {
                key.forEachCommonDependency(commonOps) { depKey ->
                    val depOp = operationMap[depKey]
                    if (depOp != null) {
                        for (i in 0 until usages.size) {
                            usages[i].deps.add(depOp)
                        }
                    }
                }
            }
        }
    }

    /**
     * Recursively emits the expression for the given [key] and its arguments into their ideal
     * spans.
     *
     * This method ensures that common sub-expressions are emitted before the expressions that
     * depend on them, and that they are placed in the highest possible span in the tree that
     * dominates all their usages.
     *
     * @param key The cache key of the operation to emit.
     * @param commonOps The set of operation keys that have been identified as common
     *   sub-expressions.
     * @param keyToIdealSpan A map from operation key to the ideal span where it should be emitted.
     * @param spanToExpressions A mutable map where emitted operations are accumulated per span.
     * @param creationState The state used to write operations to the document and allocate variable
     *   IDs.
     */
    private fun emitExpression(
        key: RemoteOperationCacheKey,
        commonOps: Set<RemoteOperationCacheKey>,
        keyToIdealSpan: Map<RemoteOperationCacheKey, Span>,
        spanToExpressions: MutableMap<Span, ArrayList<SpanOp>>,
        creationState: RemoteComposeCreationState,
    ) {
        if (key in operationMap) return

        val idealSpan = keyToIdealSpan[key] ?: return

        val state = expressionMap[key] ?: key.state
        if (state is BaseRemoteState<*>) {
            // Recurse on children first!
            for (i in 0 until key.args.size) {
                val arg = key.args[i]
                if (arg is RemoteOperationCacheKey) {
                    arg.forEachCommonDependency(commonOps) { depKey ->
                        emitExpression(
                            depKey,
                            commonOps,
                            keyToIdealSpan,
                            spanToExpressions,
                            creationState,
                        )
                    }
                }
            }

            val op =
                SpanOp(idealSpan) {
                    creationState.getOrPutVariableId(key) { state.writeToDocument(creationState) }
                }

            // Add dependencies on other common ops
            for (i in 0 until key.args.size) {
                val arg = key.args[i]
                if (arg is RemoteOperationCacheKey) {
                    arg.forEachCommonDependency(commonOps) { depKey ->
                        operationMap[depKey]?.let { op.deps.add(it) }
                    }
                }
            }

            operationMap[key] = op
            spanToExpressions.getOrPut(idealSpan) { ArrayList() }.add(op)
        }
    }

    /**
     * Recursively calculates the ideal span for a common operation and its arguments.
     *
     * The ideal span is the highest common ancestor span among all spans where the operation or its
     * dependencies are used. This ensures that the operation is hoisted high enough in the tree to
     * be available for all its usages.
     *
     * @param key The cache key of the operation.
     * @param span The span where this operation is currently being used.
     * @param keyToIdealSpan A mutable map that accumulates the calculated ideal span for each key.
     * @param visited Tracks visited operation keys to avoid redundant propagation.
     */
    private fun propagateSpan(
        key: RemoteOperationCacheKey,
        span: Span,
        keyToIdealSpan: MutableMap<RemoteOperationCacheKey, Span>,
        visited: MutableSet<RemoteOperationCacheKey>,
    ) {
        val currentSpan = keyToIdealSpan[key]
        val newSpan = if (currentSpan == null) span else findCommonAncestor(currentSpan, span)
        val isFirstPropagation = visited.add(key)
        keyToIdealSpan[key] = newSpan

        if (!isFirstPropagation && currentSpan == newSpan) {
            // We've aready propagated this span.
            return
        }

        for (i in 0 until key.args.size) {
            val arg = key.args[i]
            if (arg is RemoteOperationCacheKey) {
                propagateSpan(arg, newSpan, keyToIdealSpan, visited)
            }
        }
    }

    /**
     * Traverses the expression graph starting from the given [key] to identify common
     * sub-expressions.
     *
     * This method counts the usages of each sub-expression and identifies those that are used at
     * least twice as common sub-expressions (added to [commonOps]).
     *
     * @param key The starting cache key for traversal.
     * @param counts A mutable map to accumulate usage counts for each state cache key.
     * @param commonOps A mutable set to accumulate identified common operation keys.
     * @param visited A mutable set to track visited keys and avoid cycles or redundant traversal.
     */
    internal fun traverseCacheKey(
        key: RemoteStateCacheKey,
        counts: MutableObjectIntMap<RemoteStateCacheKey>,
        commonOps: MutableSet<RemoteOperationCacheKey>,
        visited: MutableSet<RemoteStateCacheKey>,
    ) {
        if (key in visited) return
        visited.add(key)

        if (key is RemoteOperationCacheKey) {
            for (i in 0 until key.args.size) {
                val arg = key.args[i]
                val count = counts.getOrDefault(arg, 0) + 1
                counts.put(arg, count)

                if (count >= 2 && arg is RemoteOperationCacheKey) {
                    commonOps.add(arg)
                }

                traverseCacheKey(arg, counts, commonOps, visited)
            }
        }
    }

    private fun RemoteOperationCacheKey.forEachCommonDependency(
        commonOps: Set<RemoteOperationCacheKey>,
        action: (RemoteOperationCacheKey) -> Unit,
    ) {
        if (this in commonOps) {
            action(this)
        } else {
            for (i in 0 until args.size) {
                val arg = args[i]
                if (arg is RemoteOperationCacheKey) {
                    arg.forEachCommonDependency(commonOps, action)
                }
            }
        }
    }

    internal companion object {
        internal fun findCommonAncestor(a: Span, b: Span): Span {
            var currentA = a
            var currentB = b

            while (currentA.depth > currentB.depth) {
                currentA = currentA.parent!!
            }
            while (currentB.depth > currentA.depth) {
                currentB = currentB.parent!!
            }
            while (currentA != currentB) {
                currentA = currentA.parent!!
                currentB = currentB.parent!!
            }

            return currentA
        }
    }
}
