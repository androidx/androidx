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
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.state.BaseRemoteState
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap

/**
 * Represents a pending matrix transformation operation that can potentially be optimized.
 *
 * Unlike [CanvasOp.SaveRestore] trees which form the main hierarchical tree, [PendingOp]s are flat,
 * intermediate representations of transforms. They are accumulated in lists during the transform
 * optimization pass, where they are fused and commuted before being converted back to
 * [CanvasOp.Transform] nodes for final flushing.
 */
internal sealed class PendingOp {
    /** Flushes this transformation to the [writer]. */
    abstract fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState)

    /** Returns true if this transformation is a no-op identity transform. */
    abstract val isIdentity: Boolean

    /** Represents a translation transformation. */
    class Translate(val dx: RemoteFloat, val dy: RemoteFloat) : PendingOp() {
        override val isIdentity: Boolean
            get() = dx.constantValueOrNull == 0f && dy.constantValueOrNull == 0f

        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) {
            writer.addMatrixTranslate(
                dx.getFloatIdForCreationState(creationState),
                dy.getFloatIdForCreationState(creationState),
            )
        }
    }

    /** Represents a scale transformation. */
    class Scale(
        val sx: RemoteFloat,
        val sy: RemoteFloat,
        val px: RemoteFloat?,
        val py: RemoteFloat?,
    ) : PendingOp() {
        override val isIdentity: Boolean
            get() = sx.constantValueOrNull == 1f && sy.constantValueOrNull == 1f

        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) {
            writer.addMatrixScale(
                sx.getFloatIdForCreationState(creationState),
                sy.getFloatIdForCreationState(creationState),
                px?.getFloatIdForCreationState(creationState) ?: Float.NaN,
                py?.getFloatIdForCreationState(creationState) ?: Float.NaN,
            )
        }
    }

    /** Represents a rotation transformation. */
    class Rotate(val angle: RemoteFloat, val px: RemoteFloat?, val py: RemoteFloat?) : PendingOp() {
        override val isIdentity: Boolean
            get() = angle.constantValueOrNull == 0f

        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) {
            writer.addMatrixRotate(
                angle.getFloatIdForCreationState(creationState),
                px?.getFloatIdForCreationState(creationState) ?: Float.NaN,
                py?.getFloatIdForCreationState(creationState) ?: Float.NaN,
            )
        }
    }

    /** Represents a skew transformation. */
    class Skew(val sx: RemoteFloat, val sy: RemoteFloat) : PendingOp() {
        override val isIdentity: Boolean
            get() = sx.constantValueOrNull == 0f && sy.constantValueOrNull == 0f

        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) {
            writer.addMatrixSkew(
                sx.getFloatIdForCreationState(creationState),
                sy.getFloatIdForCreationState(creationState),
            )
        }
    }
}

/**
 * Represents an operation in the structured canvas recording buffer.
 *
 * These operations are optimized (elided, flattened, fused) before being flushed to the actual
 * [RemoteComposeWriter] during serialization.
 */
internal sealed class CanvasOp {
    /** Flushes this operation (and its children, if any) to the [writer]. */
    abstract fun write(writer: RemoteComposeWriter, creationState: RemoteComposeCreationState)

    /** Returns true if this operation modifies the canvas transform or clip state. */
    open fun hasTransformsOrClips(): Boolean = false

    /**
     * Returns true if this operation is or contains visual drawing primitives (such as [Draw]
     * operations like `drawRect` or `drawBitmap` that render pixels on screen).
     *
     * Unlike [emitsWireCommands], which returns true for any wire instruction (including state
     * changes like [Transform] or [Clip]), [containsDrawingPrimitives] returns true exclusively
     * when visual rendering occurs. During optimization, if a [SaveRestore] block returns false for
     * [containsDrawingPrimitives], the entire save/restore scope and any state-only commands inside
     * it can be safely discarded ([ElisionMode.DISCARD]).
     */
    open fun containsDrawingPrimitives(): Boolean = false

    /**
     * Returns true if this operation is or contains a condition switch ([DrawConditionally]) or
     * target canvas switch (such as offscreen drawing in [Draw]).
     */
    open fun switchesCanvasOrHasCondition(): Boolean = false

    /**
     * Indicates whether recording this operation should immediately mark `hasDrawCalls = true` on
     * enclosing [SaveRestore] scopes without requiring tree traversals later.
     */
    open fun triggersDrawCall(): Boolean = containsDrawingPrimitives()

    /**
     * Returns true if this operation is or encloses any operation that emits wire commands
     * (`writer.xxx()`) to the [RemoteComposeWriter] (such as [Draw], [Clip], or [Transform]).
     *
     * This returns false for non-operational metadata or variable declarations ([Expression]) and
     * empty scopes. During optimization, conditional blocks ([DrawConditionally]) check
     * [emitsWireCommands] on their child span; if false, the conditional block is pruned because it
     * produces no wire output when executed.
     */
    open fun emitsWireCommands(): Boolean = false

    /**
     * Recursively optimizes or elides operations within child scopes or spans owned by this
     * operation.
     */
    open fun optimizeChildScopes(buffer: CanvasOperationBuffer) {}

    /**
     * Evaluates and updates the elision mode or optimization state for this operation and its
     * children.
     */
    open fun evaluateElision() {}

    /**
     * Evaluates whether this operation is empty or dead and should be pruned during optimization.
     *
     * @param buffer The [CanvasOperationBuffer] running the optimization.
     * @return True if this operation should be pruned, false otherwise.
     */
    open fun shouldPrune(buffer: CanvasOperationBuffer): Boolean = false

    /**
     * Recursively flattens this operation into the target [dest] list during internal tree
     * unrolling.
     *
     * By default, operations append themselves directly to [dest]. Composite operations like
     * [SaveRestore] override this to omit discarded scopes, inline children of elided scopes, or
     * retain preserved boundaries.
     *
     * @param dest The destination list accumulating flattened [CanvasOp] nodes.
     */
    open fun flattenInto(dest: MutableList<CanvasOp>) {
        dest.add(this)
    }

    /**
     * Flattens this operation within a dependency-aware [span] during the span optimization pass.
     *
     * By default, the wrapping [spanOp] is added directly to [dest]. Composite operations like
     * [SaveRestore] override this to unroll inlined children into standalone
     * [CanvasOperationBuffer.SpanOp] wrappers, chaining their execution order and rewiring
     * dependent operations across the scope boundary.
     *
     * @param spanOp The wrapping [CanvasOperationBuffer.SpanOp] representing this operation in the
     *   span graph.
     * @param span The containing [CanvasOperationBuffer.Span] whose operations are being flattened.
     * @param dest The destination list accumulating flattened [CanvasOperationBuffer.SpanOp] nodes.
     */
    open fun flattenInto(
        spanOp: CanvasOperationBuffer.SpanOp,
        span: CanvasOperationBuffer.Span,
        dest: MutableList<CanvasOperationBuffer.SpanOp>,
    ) {
        dest.add(spanOp)
    }

    /** Represents an actual drawing or state-setting operation (e.g., drawRect). */
    class Draw(val switchesCanvas: Boolean = false, val action: (RemoteComposeWriter) -> Unit) :
        CanvasOp() {
        constructor(action: (RemoteComposeWriter) -> Unit) : this(false, action)

        override fun write(writer: RemoteComposeWriter, creationState: RemoteComposeCreationState) {
            action(writer)
        }

        // Visual drawing primitive that renders content directly to the canvas.
        override fun containsDrawingPrimitives(): Boolean = true

        override fun switchesCanvasOrHasCondition(): Boolean = switchesCanvas

        // Emits a direct drawing wire command (`writer.drawXxx()`) to the document.
        override fun emitsWireCommands(): Boolean = true

        override fun toString(): String = "Draw"
    }

    /** Represents a clipping operation. */
    class Clip(val action: (RemoteComposeWriter) -> Unit) : CanvasOp() {
        override fun write(writer: RemoteComposeWriter, creationState: RemoteComposeCreationState) {
            action(writer)
        }

        override fun hasTransformsOrClips(): Boolean = true

        // State modification only; does not draw pixels directly.
        override fun containsDrawingPrimitives(): Boolean = false

        // Emits a clipping wire command (`writer.clipXxx()`) to the document.
        override fun emitsWireCommands(): Boolean = true

        override fun toString(): String = "Clip"
    }

    /**
     * Represents a matrix transformation operation (Translate, Scale, Rotate, Skew).
     *
     * @property op The underlying [PendingOp] transformation.
     */
    class Transform(val op: PendingOp) : CanvasOp() {
        override fun write(writer: RemoteComposeWriter, creationState: RemoteComposeCreationState) {
            op.write(writer.buffer, creationState)
        }

        override fun hasTransformsOrClips(): Boolean = true

        // State modification only; does not draw pixels directly.
        override fun containsDrawingPrimitives(): Boolean = false

        // Emits a matrix transformation wire command (`writer.buffer.addXxx()`) to the document.
        override fun emitsWireCommands(): Boolean = true

        override fun toString(): String = "Transform(${op.javaClass.simpleName})"
    }

    /**
     * Represents a save/restore group (corresponding to [RemoteComposeWriter.save] and
     * [RemoteComposeWriter.restore]).
     *
     * @property parent The parent [SaveRestore] scope, or null if this is the root scope.
     * @property children The list of child operations inside this save/restore block.
     */
    class SaveRestore(
        val parent: SaveRestore? = null,
        val children: MutableList<CanvasOp> = ArrayList(),
    ) : CanvasOp() {
        /**
         * Indicates whether this scope or any of its descendants contain actual drawing calls. Used
         * during the elision pass to discard empty scopes.
         */
        var hasDrawCalls = false

        /** The elision strategy decided for this scope during the elision pass. */
        var elisionMode = ElisionMode.PRESERVE
        var spanOp: CanvasOperationBuffer.SpanOp? = null

        fun getRootSaveNode(): SaveRestore {
            var curr = this
            while (curr.parent != null) {
                curr = curr.parent!!
            }
            return curr
        }

        // Returns true if this save scope leaks state modifications into surrounding operations.
        // When a save scope preserves its save/restore boundaries (PRESERVE), its restore point
        // completely neutralizes internal transforms and clips, preventing them from leaking out.
        // Only when the save/restore boundaries are stripped (INLINE) do internal state changes
        // bubble up to affect external drawing operations.
        override fun hasTransformsOrClips(): Boolean =
            elisionMode == ElisionMode.INLINE && hasChildTransformsOrClips()

        fun hasChildTransformsOrClips(): Boolean {
            for (i in 0 until children.size) {
                if (children[i].hasTransformsOrClips()) return true
            }
            return false
        }

        override fun containsDrawingPrimitives(): Boolean {
            for (i in 0 until children.size) {
                if (children[i].containsDrawingPrimitives()) return true
            }
            return false
        }

        override fun switchesCanvasOrHasCondition(): Boolean {
            for (i in 0 until children.size) {
                if (children[i].switchesCanvasOrHasCondition()) return true
            }
            return false
        }

        // Pushing a Save scope does not draw pixels; only child leaf primitives trigger
        // markDrawCall().
        override fun triggersDrawCall(): Boolean = false

        override fun emitsWireCommands(): Boolean {
            if (elisionMode == ElisionMode.DISCARD) return false
            for (i in 0 until children.size) {
                if (children[i].emitsWireCommands()) return true
            }
            return false
        }

        override fun optimizeChildScopes(buffer: CanvasOperationBuffer) {
            children.removeAll { op ->
                op.optimizeChildScopes(buffer)
                op.shouldPrune(buffer)
            }
            hasDrawCalls = containsDrawingPrimitives()
        }

        var isTopLevel = false

        override fun evaluateElision() {
            children.evaluateElisionPass(isTopLevel) { it }
            elisionMode =
                when {
                    // Empty or non-drawing scope: completely prune wire output.
                    !hasDrawCalls -> ElisionMode.DISCARD
                    // Scope has no state changes: inline drawing calls without save/restore
                    // overhead.
                    !hasChildTransformsOrClips() -> ElisionMode.INLINE
                    // Top-level scope with no target/condition switch: inline by default (may be
                    // upgraded to PRESERVE by parent if subsequent drawing operations appear).
                    !switchesCanvasOrHasCondition() && isTopLevel -> ElisionMode.INLINE
                    // Default behavior: emit matching save and restore wire commands.
                    else -> ElisionMode.PRESERVE
                }
        }

        private fun flattenChildren() {
            val newChildren = ArrayList<CanvasOp>(children.size)
            for (i in 0 until children.size) {
                children[i].flattenInto(newChildren)
            }
            children.clear()
            children.addAll(newChildren)
        }

        override fun flattenInto(dest: MutableList<CanvasOp>) {
            flattenChildren()
            when (elisionMode) {
                ElisionMode.DISCARD -> {}
                ElisionMode.INLINE -> dest.addAll(children)
                ElisionMode.PRESERVE -> dest.add(this)
            }
        }

        override fun flattenInto(
            spanOp: CanvasOperationBuffer.SpanOp,
            span: CanvasOperationBuffer.Span,
            dest: MutableList<CanvasOperationBuffer.SpanOp>,
        ) {
            flattenChildren()
            when (elisionMode) {
                ElisionMode.DISCARD -> {
                    span.routeDependenciesAround(spanOp)
                }
                ElisionMode.INLINE -> {
                    var lastInlinedOp: CanvasOperationBuffer.SpanOp? = null
                    for (j in 0 until children.size) {
                        val newSpanOp = CanvasOperationBuffer.SpanOp(span, children[j])
                        if (j == 0) {
                            newSpanOp.deps.addAll(spanOp.deps)
                        } else {
                            newSpanOp.deps.add(lastInlinedOp!!)
                        }
                        dest.add(newSpanOp)
                        lastInlinedOp = newSpanOp
                    }
                    if (lastInlinedOp != null) {
                        span.replaceDependency(spanOp, lastInlinedOp)
                    } else {
                        span.routeDependenciesAround(spanOp)
                    }
                }
                ElisionMode.PRESERVE -> {
                    dest.add(spanOp)
                }
            }
        }

        override fun write(writer: RemoteComposeWriter, creationState: RemoteComposeCreationState) {
            when (elisionMode) {
                ElisionMode.DISCARD -> {}
                ElisionMode.INLINE -> {
                    for (i in 0 until children.size) {
                        children[i].write(writer, creationState)
                    }
                }
                ElisionMode.PRESERVE -> {
                    writer.save()
                    for (i in 0 until children.size) {
                        children[i].write(writer, creationState)
                    }
                    writer.restore()
                }
            }
        }

        override fun toString(): String = "SaveRestore(children=$children)"
    }

    /** Represents an expression evaluation (hoisted variable assignment). */
    class Expression(val key: RemoteOperationCacheKey, val state: BaseRemoteState<*>) : CanvasOp() {
        override fun write(writer: RemoteComposeWriter, creationState: RemoteComposeCreationState) {
            creationState.getOrPutVariableId(key) { state.writeToDocument(creationState) }
        }

        // Variable/expression definition only; does not draw pixels directly.
        override fun containsDrawingPrimitives(): Boolean = false

        // Pure metadata evaluation; emits no drawing or state wire commands.
        override fun emitsWireCommands(): Boolean = false
    }

    /**
     * Represents a conditional drawing block (`drawConditionally`).
     *
     * Typically [childSpan] will be a [CanvasOperationBuffer.Span], which if hasChildCommands will
     * return false if empty.
     *
     * @property condition The condition that controls execution of the child span.
     * @property childSpan The child [CanvasOperationBuffer.Span] holding conditional operations.
     * @property action The action that writes the conditional block to the writer.
     */
    class DrawConditionally(
        val condition: RemoteBoolean,
        val childSpan: CanvasOperationBuffer.Span,
        val action: (RemoteComposeWriter, RemoteComposeCreationState) -> Unit,
    ) : CanvasOp() {
        override fun write(writer: RemoteComposeWriter, creationState: RemoteComposeCreationState) {
            action(writer, creationState)
        }

        override fun hasTransformsOrClips(): Boolean = childSpan.hasTransformsOrClips()

        // Returns true if the conditional child span contains visual drawing primitives.
        override fun containsDrawingPrimitives(): Boolean = childSpan.containsDrawingPrimitives()

        override fun switchesCanvasOrHasCondition(): Boolean = true

        // Returns true if the conditional child span emits any wire commands (`writer.xxx()`).
        override fun emitsWireCommands(): Boolean = childSpan.emitsWireCommands()

        override fun optimizeChildScopes(buffer: CanvasOperationBuffer) {
            buffer.optimizeSpan(childSpan)
        }

        // Conditional block can be pruned if its child span emits no wire commands when executed.
        override fun shouldPrune(buffer: CanvasOperationBuffer) = !childSpan.emitsWireCommands()

        override fun toString(): String = "DrawConditionally(${condition.toDebugString()})"
    }

    /** The strategy for rendering a [SaveRestore] node during flush. */
    enum class ElisionMode {
        /**
         * Keep the save/restore bounds and write [RemoteComposeWriter.save] and
         * [RemoteComposeWriter.restore].
         */
        PRESERVE,

        /**
         * Discard the save/restore bounds but write all the children. This inlines the children
         * into the parent scope.
         */
        INLINE,

        /** Discard the save/restore block and all of its children. */
        DISCARD,
    }
}

/**
 * Buffers drawing operations and tracks expression roots. It allows for global optimizations such
 * as common subexpression elimination and operation reordering before operations are recorded into
 * the document.
 */
internal inline fun <T> List<T>.evaluateElisionPass(isTopLevel: Boolean, op: (T) -> CanvasOp) {
    var pendingSave: CanvasOp.SaveRestore? = null
    for (i in 0 until size) {
        val current = op(this[i])
        if (current is CanvasOp.SaveRestore) {
            current.isTopLevel = isTopLevel
            current.evaluateElision()
            if (current.elisionMode == CanvasOp.ElisionMode.DISCARD) continue
            pendingSave?.let {
                if (it.hasChildTransformsOrClips()) it.elisionMode = CanvasOp.ElisionMode.PRESERVE
            }
            pendingSave = current
        } else if (current.containsDrawingPrimitives() || current.switchesCanvasOrHasCondition()) {
            pendingSave?.let {
                if (it.hasChildTransformsOrClips()) it.elisionMode = CanvasOp.ElisionMode.PRESERVE
            }
            pendingSave = null
        }
    }
}

internal class CanvasOperationBuffer(val enableOptimizations: Boolean = false) {

    /**
     * Represents a node in the tree of operations, corresponding to a lexical scope (e.g., a branch
     * of a conditional or a loop). Spans are used to determine the ideal location to hoist common
     * subexpressions.
     *
     * A span represents its hierarchy through two complementary structures:
     * - [operations] drives linear execution and wire serialization in strict drawing order.
     * - [child] and [next] maintain a linked scope tree for structural passes and graph rewiring.
     */
    internal class Span(val parent: Span?, val depth: Int) {
        /**
         * The sequential list of operations recorded in this scope. When a nested child span is
         * created, it is eventually wrapped inside a composite operation (such as
         * [CanvasOp.DrawConditionally]) and added directly to this list for linear wire emission.
         */
        val operations = ArrayList<SpanOp>()

        /**
         * Head of a singly-linked list of child spans spawned directly under this parent scope.
         * Used during recursive structural passes (topological sorting, optimization visits, and
         * global dependency rewiring) without needing to inspect operation wrappers in
         * [operations].
         */
        var child: Span? = null

        /** Link to the next sibling span sharing the same [parent] scope. */
        var next: Span? = null
        var optimized = false

        fun createChildSpan(): Span {
            val childSpan = Span(this, depth + 1)
            if (child == null) {
                child = childSpan
            } else {
                var current = child
                while (current?.next != null) {
                    current = current.next
                }
                current?.next = childSpan
            }
            return childSpan
        }

        fun removeChildSpan(span: Span) {
            if (child == span) {
                child = span.next
            } else {
                var current = child
                while (current != null && current.next != span) {
                    current = current.next
                }
                if (current != null) {
                    current.next = span.next
                }
            }
        }

        fun record(writer: RemoteComposeWriter, creationState: RemoteComposeCreationState) {
            for (i in 0 until operations.size) {
                operations[i].op.write(writer, creationState)
            }
        }

        fun hasTransformsOrClips(): Boolean {
            for (i in 0 until operations.size) {
                if (operations[i].op.hasTransformsOrClips()) return true
            }
            return false
        }

        fun containsDrawingPrimitives(): Boolean {
            for (i in 0 until operations.size) {
                if (operations[i].op.containsDrawingPrimitives()) return true
            }
            return false
        }

        fun switchesCanvasOrCondition(): Boolean {
            for (i in 0 until operations.size) {
                if (operations[i].op.switchesCanvasOrHasCondition()) return true
            }
            return false
        }

        fun emitsWireCommands(): Boolean {
            for (i in 0 until operations.size) {
                if (operations[i].op.emitsWireCommands()) return true
            }
            return false
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("Span(depth=").append(depth).append(", ops=[")
            for (i in 0 until operations.size) {
                if (i > 0) sb.append(", ")
                sb.append(operations[i].op.toString())
            }
            sb.append("]")
            if (child != null) {
                sb.append(", child=").append(child.toString())
            }
            if (next != null) {
                sb.append(", next=").append(next.toString())
            }
            sb.append(")")
            return sb.toString()
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

        private fun updateDependencies(oldDep: SpanOp, update: (MutableList<SpanOp>) -> Unit) {
            for (i in 0 until operations.size) {
                val deps = operations[i].deps
                if (deps.remove(oldDep)) {
                    update(deps)
                }
            }
            var currentChild = child
            while (currentChild != null) {
                currentChild.updateDependencies(oldDep, update)
                currentChild = currentChild.next
            }
        }

        internal fun replaceDependency(oldDep: SpanOp, newDep: SpanOp) {
            updateDependencies(oldDep) { it.add(newDep) }
        }

        internal fun routeDependenciesAround(oldDep: SpanOp) {
            updateDependencies(oldDep) { it.addAll(oldDep.deps) }
        }
    }

    /**
     * Represents a single operation (or a common subexpression evaluation) within a [Span]. It
     * tracks its dependencies and can be hoisted to a higher span if it is used across multiple
     * spans.
     */
    internal class SpanOp(var idealSpan: Span, val op: CanvasOp) {
        val deps = ArrayList<SpanOp>()
        var visited = false
    }

    internal var spanTreeRoot = Span(null, 0)
    internal var insertPoint = spanTreeRoot
    private val operationMap = HashMap<RemoteStateCacheKey, SpanOp>()
    private val usageMap = LinkedHashMap<RemoteStateCacheKey, ArrayList<SpanOp>>()
    private val expressionMap = LinkedHashMap<RemoteStateCacheKey, BaseRemoteState<*>>()
    internal var lastRenderingOp: SpanOp? = null

    /**
     * Records a structured rendering operation into the current active span.
     *
     * This method creates a [SpanOp] wrapping the given [op], adds it to the current [insertPoint]
     * span, and automatically establishes a sequential dependency on the previously recorded
     * rendering operation (if any) to preserve execution order.
     *
     * @param op The structured canvas operation to record.
     * @return The created [SpanOp] representing this operation in the dependency graph.
     */
    public fun recordRenderingOp(op: CanvasOp): SpanOp {
        val spanOp = SpanOp(insertPoint, op)
        insertPoint.operations.add(spanOp)
        lastRenderingOp?.let { spanOp.deps.add(it) }
        lastRenderingOp = spanOp
        return spanOp
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

    /** Returns a string representation of the operation buffer's current span tree. */
    override fun toString(): String {
        return if (spanTreeRoot.operations.isNotEmpty() || spanTreeRoot.child != null) {
            spanTreeRoot.toString()
        } else {
            "CanvasOperationBuffer(empty)"
        }
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

        // Run optimizations on the span tree
        if (enableOptimizations) {
            optimizeSpan(spanTreeRoot)
            spanTreeRoot.sortAllSpans()
        }

        spanTreeRoot.record(creationState.document, creationState)

        // Reset for next flush
        spanTreeRoot = Span(null, 0)
        insertPoint = spanTreeRoot
        operationMap.clear()
        usageMap.clear()
        expressionMap.clear()
        lastRenderingOp = null
    }

    /**
     * Recursively applies optimizations to the span tree.
     *
     * It prunes dead conditional operations ([pruneEmptyOperations]), evaluates redundant
     * save/restore boundaries ([identifyRedundantSaveRestoreScopes]), flattens inlined scopes
     * ([flattenSpan]), and optimizes transforms in the resulting simplified tree
     * ([optimizeTransformsSpan]).
     */
    internal fun optimizeSpan(span: Span) {
        if (span.optimized) return
        span.optimized = true

        pruneEmptyOperations(span)
        identifyRedundantSaveRestoreScopes(span)
        flattenSpan(span)
        optimizeTransformsSpan(span)

        var currentChild = span.child
        while (currentChild != null) {
            optimizeSpan(currentChild)
            currentChild = currentChild.next
        }
    }

    /**
     * Removes dead or empty operations (such as conditional blocks with empty child spans) from the
     * operations list, re-routing dependency chains around removed nodes.
     */
    internal fun pruneEmptyOperations(span: Span) {
        span.operations.removeAll { spanOp ->
            val op = spanOp.op
            op.optimizeChildScopes(this)
            if (op.shouldPrune(this)) {
                spanTreeRoot.routeDependenciesAround(spanOp)
                true
            } else {
                false
            }
        }
    }

    private fun RemoteFloat?.cacheKeysMatch(other: RemoteFloat?): Boolean {
        if (this == null && other == null) return true
        if (this == null || other == null) return false
        return this.cacheKey == other.cacheKey
    }

    /**
     * Pushes a translation operation into the pending operations list, attempting to commute it
     * leftwards past other transformations (Scale, Rotate, Skew) to enable further fusing with
     * existing translations.
     *
     * As the translation commutes past other operations, its offsets are adjusted:
     * - Past [PendingOp.Scale]: Offsets are scaled by the scale factors.
     * - Past [PendingOp.Rotate]: Offsets are rotated by the rotation angle.
     * - Past [PendingOp.Skew]: Offsets are skewed by the skew factors.
     *
     * Commutation is only possible if the encountered transformation has constant values (not
     * dynamic/animated expressions), allowing the adjustment to be computed at creation time. If
     * commutation is blocked, the translation is inserted at the current position.
     */
    private fun MutableList<PendingOp>.pushTranslate(dx: RemoteFloat, dy: RemoteFloat) {
        var currDx = dx
        var currDy = dy
        var i = size - 1
        // Travel backwards through the list to find a Translate to fuse with,
        // or commute past Scale/Rotate/Skew.
        while (i >= 0) {
            val op = this[i]
            when (op) {
                is PendingOp.Translate -> {
                    // Found another Translate, fuse them by adding offsets.
                    this[i] = PendingOp.Translate(op.dx + currDx, op.dy + currDy)
                    return
                }
                is PendingOp.Scale -> {
                    // Commute past Scale: we must scale the translation offsets.
                    // This is only possible if the scale factors are constants.
                    val sxVal = op.sx.constantValueOrNull
                    val syVal = op.sy.constantValueOrNull
                    if (sxVal == null || syVal == null) {
                        break // Cannot commute past dynamic scale, stop here.
                    }
                    currDx = currDx * op.sx
                    currDy = currDy * op.sy
                    i--
                }
                is PendingOp.Rotate -> {
                    // Commute past Rotate: we must rotate the translation offsets.
                    // This is only possible if the rotation angle is constant.
                    val angleVal = op.angle.constantValueOrNull
                    if (angleVal == null) {
                        break // Cannot commute past dynamic rotation, stop here.
                    }
                    val rad = Math.toRadians(angleVal.toDouble())
                    val cos = Math.cos(rad).toFloat()
                    val sin = Math.sin(rad).toFloat()
                    val rx = currDx * cos - currDy * sin
                    val ry = currDx * sin + currDy * cos
                    currDx = rx
                    currDy = ry
                    i--
                }
                is PendingOp.Skew -> {
                    // Commute past Skew: we must skew the translation offsets.
                    // This is only possible if the skew factors are constants.
                    val sxVal = op.sx.constantValueOrNull
                    val syVal = op.sy.constantValueOrNull
                    if (sxVal == null || syVal == null) {
                        break // Cannot commute past dynamic skew, stop here.
                    }
                    val rx = currDx + currDy * sxVal
                    val ry = currDx * syVal + currDy
                    currDx = rx
                    currDy = ry
                    i--
                }
            }
        }
        // Insert the accumulated translation at the position we stopped.
        this.add(i + 1, PendingOp.Translate(currDx, currDy))
    }

    /**
     * Pushes a scale operation into the pending operations list.
     *
     * If the immediate preceding operation is also a [PendingOp.Scale] and shares the exact same
     * pivot point ([px], [py]), the two scales are fused into a single scale by multiplying their
     * scale factors. Otherwise, the scale is appended.
     */
    private fun MutableList<PendingOp>.pushScale(
        sx: RemoteFloat,
        sy: RemoteFloat,
        px: RemoteFloat?,
        py: RemoteFloat?,
    ) {
        if (isNotEmpty()) {
            val last = last()
            // If the last op is also a Scale with the same pivot, we can fuse them.
            if (
                last is PendingOp.Scale && last.px.cacheKeysMatch(px) && last.py.cacheKeysMatch(py)
            ) {
                // Fuse by multiplying the scale factors.
                this[size - 1] = PendingOp.Scale(last.sx * sx, last.sy * sy, px, py)
                return
            }
        }
        // Otherwise, append the new scale.
        add(PendingOp.Scale(sx, sy, px, py))
    }

    /**
     * Pushes a rotation operation into the pending operations list.
     *
     * If the immediate preceding operation is also a [PendingOp.Rotate] and shares the exact same
     * pivot point ([px], [py]), the two rotations are fused into a single rotation by adding their
     * angles. Otherwise, the rotation is appended.
     */
    private fun MutableList<PendingOp>.pushRotate(
        angle: RemoteFloat,
        px: RemoteFloat?,
        py: RemoteFloat?,
    ) {
        if (isNotEmpty()) {
            val last = last()
            // If the last op is also a Rotate with the same pivot, we can fuse them.
            if (
                last is PendingOp.Rotate && last.px.cacheKeysMatch(px) && last.py.cacheKeysMatch(py)
            ) {
                // Fuse by adding the rotation angles.
                this[size - 1] = PendingOp.Rotate(last.angle + angle, px, py)
                return
            }
        }
        // Otherwise, append the new rotation.
        add(PendingOp.Rotate(angle, px, py))
    }

    /**
     * Pushes a skew operation into the pending operations list.
     *
     * Skew operations in different axes cannot be mathematically combined by simply adding their
     * factors. However, consecutive skews along the exact same axis (e.g. both horizontal with
     * sy==0 or both vertical with sx==0) commute and add linearly.
     */
    private fun MutableList<PendingOp>.pushSkew(sx: RemoteFloat, sy: RemoteFloat) {
        if (isNotEmpty()) {
            val last = last()
            if (last is PendingOp.Skew) {
                if (last.sy.constantValueOrNull == 0f && sy.constantValueOrNull == 0f) {
                    this[size - 1] = PendingOp.Skew(last.sx + sx, sy)
                    return
                }
                if (last.sx.constantValueOrNull == 0f && sx.constantValueOrNull == 0f) {
                    this[size - 1] = PendingOp.Skew(sx, last.sy + sy)
                    return
                }
            }
        }
        add(PendingOp.Skew(sx, sy))
    }

    /**
     * Fuses and commutes a list of consecutive [PendingOp] transforms.
     *
     * Consecutive translations, scales, and rotations are fused. Translations are commuted left
     * past scales/rotates/skews when possible to enable further fusing.
     */
    private fun optimizeTransformList(ops: List<PendingOp>): List<PendingOp> =
        buildList(ops.size) {
            for (i in 0 until ops.size) {
                val op = ops[i]
                when (op) {
                    is PendingOp.Translate -> pushTranslate(op.dx, op.dy)
                    is PendingOp.Scale -> pushScale(op.sx, op.sy, op.px, op.py)
                    is PendingOp.Rotate -> pushRotate(op.angle, op.px, op.py)
                    is PendingOp.Skew -> pushSkew(op.sx, op.sy)
                }
            }
            for (i in size - 1 downTo 0) {
                if (this[i].isIdentity) {
                    removeAt(i)
                }
            }
        }

    /**
     * Recursively optimizes transform operations within each scope of the tree.
     *
     * Within each [CanvasOp.SaveRestore] node, consecutive [CanvasOp.Transform] nodes (separated
     * only by other transforms) are grouped and optimized via [optimizeTransformList].
     * Non-transform nodes (like drawings or clips) act as barriers.
     */
    internal fun optimizeTransforms(ops: MutableList<CanvasOp>) {
        for (i in 0 until ops.size) {
            val child = ops[i]
            if (child is CanvasOp.SaveRestore) {
                optimizeTransforms(child.children)
            }
        }

        val pendingTransforms = ArrayList<PendingOp>()
        val newOps =
            buildList(ops.size) {
                fun flushTransforms() {
                    if (pendingTransforms.isNotEmpty()) {
                        val optimized = optimizeTransformList(pendingTransforms)
                        for (j in 0 until optimized.size) {
                            add(CanvasOp.Transform(optimized[j]))
                        }
                        pendingTransforms.clear()
                    }
                }

                for (i in 0 until ops.size) {
                    val child = ops[i]
                    when (child) {
                        is CanvasOp.Transform -> {
                            pendingTransforms.add(child.op)
                        }
                        else -> {
                            flushTransforms()
                            add(child)
                        }
                    }
                }
                flushTransforms()
            }
        ops.clear()
        ops.addAll(newOps)
    }

    /**
     * Optimizes consecutive transforms inside a [Span].
     *
     * Fuses consecutive [CanvasOp.Transform] nodes in the span's operations list, ensuring that any
     * dependencies on hoisted variables are correctly propagated to the new fused operation.
     */
    private fun optimizeTransformsSpan(span: Span) {
        val pendingSpanOps = ArrayList<SpanOp>()
        val newOps =
            buildList(span.operations.size) {
                fun flushTransforms() {
                    if (pendingSpanOps.isEmpty()) return

                    val pendingTransforms = ArrayList<PendingOp>(pendingSpanOps.size)
                    for (k in 0 until pendingSpanOps.size) {
                        pendingTransforms.add((pendingSpanOps[k].op as CanvasOp.Transform).op)
                    }
                    val optimized = optimizeTransformList(pendingTransforms)

                    val fusedSet = HashSet<SpanOp>(pendingSpanOps.size)
                    for (k in 0 until pendingSpanOps.size) {
                        fusedSet.add(pendingSpanOps[k])
                    }

                    val commonDeps = ArrayList<SpanOp>()
                    val seenDeps = HashSet<SpanOp>(pendingSpanOps.size)
                    for (k in 0 until pendingSpanOps.size) {
                        val deps = pendingSpanOps[k].deps
                        for (m in 0 until deps.size) {
                            val dep = deps[m]
                            if (dep !in fusedSet && seenDeps.add(dep)) {
                                commonDeps.add(dep)
                            }
                        }
                    }

                    var lastNewOp: SpanOp? = null
                    for (j in 0 until optimized.size) {
                        val newSpanOp = SpanOp(span, CanvasOp.Transform(optimized[j]))
                        if (j == 0) {
                            newSpanOp.deps.addAll(commonDeps)
                        } else {
                            newSpanOp.deps.add(lastNewOp!!)
                        }
                        add(newSpanOp)
                        lastNewOp = newSpanOp
                    }

                    if (lastNewOp != null) {
                        // Replace dependencies pointing to ANY of the old unfused transforms in
                        // the group with the final fused transform operation. This prevents
                        // intermediate transforms from being recreated during topological
                        // sorting if an operation in the span graph directly referenced an
                        // earlier transform instead of the last one.
                        for (k in 0 until pendingSpanOps.size) {
                            span.replaceDependency(pendingSpanOps[k], lastNewOp)
                        }
                    } else {
                        // If consecutive transforms canceled each other out completely (so
                        // optimized is empty and lastNewOp == null), route dependencies around
                        // every canceled transform (processed in reverse order so dependencies
                        // chain across the group) to ensure topologicalSort() does not bring
                        // them back via dangling dependency pointers.
                        for (k in pendingSpanOps.size - 1 downTo 0) {
                            span.routeDependenciesAround(pendingSpanOps[k])
                        }
                    }
                    pendingSpanOps.clear()
                }

                for (i in 0 until span.operations.size) {
                    val child = span.operations[i]
                    when (val op = child.op) {
                        is CanvasOp.Transform -> {
                            pendingSpanOps.add(child)
                        }
                        else -> {
                            flushTransforms()
                            if (op is CanvasOp.SaveRestore) {
                                optimizeTransforms(op.children)
                            }
                            add(child)
                        }
                    }
                }
                flushTransforms()
            }
        span.operations.clear()
        span.operations.addAll(newOps)
    }

    /**
     * Identifies redundant `save`/`restore` blocks across the operations directly in a [Span] via
     * [CanvasOp.evaluateElision].
     *
     * A block is marked as [CanvasOp.ElisionMode.INLINE] if it contains drawing calls but no state
     * modifications (transforms or clips). Blocks with no drawing calls at all are marked as
     * [CanvasOp.ElisionMode.DISCARD]. All other blocks preserve their boundaries by default.
     */
    private fun identifyRedundantSaveRestoreScopes(span: Span) {
        span.operations.evaluateElisionPass(span == spanTreeRoot) { it.op }
    }

    /**
     * Flattens (inlines) elided [CanvasOp.SaveRestore] nodes that reside directly in a [Span] via
     * [CanvasOp.flattenInto].
     */
    private fun flattenSpan(span: Span) {
        val newOps = ArrayList<SpanOp>(span.operations.size)
        for (i in 0 until span.operations.size) {
            val child = span.operations[i]
            child.op.flattenInto(child, span, newOps)
        }
        span.operations.clear()
        span.operations.addAll(newOps)
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

            val op = SpanOp(idealSpan, CanvasOp.Expression(key, state))

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
