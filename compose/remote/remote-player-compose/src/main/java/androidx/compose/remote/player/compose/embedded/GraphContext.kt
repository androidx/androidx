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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection", "BanConcurrentHashMap")

package androidx.compose.remote.player.compose.embedded

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.runtime.IntState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import java.util.concurrent.ConcurrentHashMap

/**
 * A pure-Compose evaluator for *computed* operations (color/text/float/int expressions, attributes,
 * lookups) using per-pass DAG memoization over reactive leaf states — no imperative recompute pass,
 * no dirty flags, and no intermediate `derivedStateOf` nodes.
 *
 * Each computed id evaluates the op's existing `updateVariables` + `apply` against **this**
 * context, memoized within the active top-level evaluation pass (`withEvalPass` / top-level
 * `computedValue`). We intercept the two things an op does:
 * - **reads** (`getFloat`/`getInteger`/`getColor`/`getText`) — for a computed input we recurse into
 *   `computedValue(depId)` (memoized in $\Theta(1)$ if already visited in the current pass, so
 *   reconvergent diamond DAGs evaluate in $\Theta(V + E)$ rather than $\Theta(2^D)$); for a leaf we
 *   read the real snapshot-backed store via `super`, which records the snapshot dependency directly
 *   on the caller's active Compose observation scope.
 * - **writes** (`loadFloat`/`loadInteger`/`loadColor`/`loadText`) — captured as the op's result
 *   rather than mutated into the store, keeping evaluation pure.
 *
 * The operations are used verbatim (no core changes). Non-scalar reads
 * (objects/bitmaps/collections/ paths) fall through to the shared store via `super`. The
 * capture/cycle bookkeeping is per-thread; nested chain evaluation is handled by save/restore.
 *
 * Conceptually this is a `RemoteReadContext` (a value view) that captures the one write an op makes
 * as its result. It extends [StoreBackedRemoteContext] rather than `AndroidRemoteContext`.
 */
internal class GraphContext(
    private val realState: SnapshotRemoteComposeState,
    private val computedOps: IntObjectMap<Operation>,
    timeMillis: State<Float> = mutableFloatStateOf(0f),
    clock: RemoteClock = RemoteClock.SYSTEM,
    internal var componentValues: Map<Int, State<Float>> = emptyMap(),
) : StoreBackedRemoteContext(clock) {

    internal var typefaceResolver: TypefaceResolver? = null

    fun setTypefaceResolver(resolver: TypefaceResolver?) {
        this.typefaceResolver = resolver
    }

    private var _timeState: GraphTimeState? = null

    internal val timeState: GraphTimeState
        get() = _timeState!!

    init {
        // Share the leaf store so collections/objects/paths and plain variables resolve against the
        // same (snapshot-backed) data the rest of the player uses.
        mRemoteComposeState = realState
        _timeState = GraphTimeState(clock, timeMillis.value)
    }

    internal val startClockMillis: Long
        get() = timeState.startClockMillis

    internal val epochSecondState: IntState
        get() = timeState.epochSecondState

    internal fun updateTime(frameMillis: Float, updateContinuous: Boolean = true): Boolean =
        timeState.updateTime(frameMillis, updateContinuous)

    internal fun timeFloatState(id: Int): State<Float> = timeState.timeFloatState(id)

    override fun getClock(): RemoteClock = _timeState?.reactiveClock ?: super.getClock()

    private val graphPaintContext = GraphPaintContext(this)

    @Suppress("BanConcurrentHashMap")
    internal val particlesInitialized: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    internal var imageLoader: RcImageLoader? = null

    private class EvalPassState {
        var depth: Int = 0
        val memoPass: MutableIntSet = MutableIntSet()
        val memoValues: MutableIntObjectMap<Any?> = MutableIntObjectMap()
        val computing: MutableIntSet = MutableIntSet()
        var captureId: Int = -1
        var captured: Any? = null
    }

    // Per-thread evaluation state: UI phases are main-thread today, but snapshot reads aren't
    // contractually single-thread. Re-entrancy within a thread is handled by save/restore.
    private val evalState = ThreadLocal.withInitial { EvalPassState() }

    internal fun isComputed(id: Int): Boolean = computedOps.containsKey(id)

    private fun computedValue(id: Int): Any? {
        val state = evalState.get()!!
        if (state.computing.contains(id)) return null // cycle: break rather than recurse forever

        if (state.memoPass.contains(id)) {
            return state.memoValues[id]
        }

        val op = computedOps[id] ?: return null
        state.depth++
        val prevId = state.captureId
        val prevCaptured = state.captured
        state.captureId = id
        state.captured = null
        state.computing.add(id)
        try {
            if (op is VariableSupport) {
                op.updateVariables(this) // reads inputs (tracked)
            }
            // Value-producing PaintOperations (e.g. ColorAttribute) compute and write
            // their outputs during paint(paintContext) rather than apply(remoteContext).
            if (op is PaintOperation && op !is Component) {
                op.paint(graphPaintContext)
            } else {
                op.apply(this) // writes output -> captured
            }
            val result = state.captured
            state.memoPass.add(id)
            state.memoValues[id] = result
            return result
        } finally {
            state.captureId = prevId
            state.captured = prevCaptured
            state.computing.remove(id)
            state.depth--
            if (state.depth == 0) {
                state.memoPass.clear()
                state.memoValues.clear()
            }
        }
    }

    override fun getFloat(id: Int): Float {
        val compVal = componentValues[id]
        return when {
            compVal != null -> compVal.value
            realState.isFloatOverridden(id) -> super.getFloat(id)
            isComputed(id) -> (computedValue(id) as? Number)?.toFloat() ?: 0f
            isTimeVariable(id) -> timeFloatState(id).value
            else -> super.getFloat(id)
        }
    }

    override fun getInteger(id: Int): Int =
        when {
            realState.isIntegerOverridden(id) -> super.getInteger(id)
            isComputed(id) -> (computedValue(id) as? Number)?.toInt() ?: 0
            id == RemoteContext.ID_EPOCH_SECOND -> epochSecondState.intValue
            else -> super.getInteger(id)
        }

    override fun getColor(id: Int): Int =
        if (realState.isColorOverridden(id)) {
            super.getColor(id)
        } else if (isComputed(id)) {
            (computedValue(id) as? Number)?.toInt() ?: 0
        } else {
            super.getColor(id)
        }

    override fun getText(id: Int): String? =
        if (realState.isDataOverridden(id)) {
            super.getText(id)
        } else if (isComputed(id)) {
            computedValue(id) as? String
        } else {
            super.getText(id)
        }

    // GraphContext is a read-only-store *evaluation* context: a computed op's apply must not mutate
    // the shared store (that would be a snapshot write during a snapshot read, and would let one op
    // clobber another's value). The scalar writes capture the op's own output; every other write is
    // a no-op. This makes the model robust even for ops that write more than once or via non-scalar
    // channels (e.g. MatrixExpression does putObject + loadFloat; Path/Shader/collection ops write
    // paths/shaders/collections) — those ops aren't read through the scalar resolvers, but if one
    // ever is, it degrades to a captured scalar / default instead of corrupting the store.

    override fun loadFloat(id: Int, value: Float) {
        val state = evalState.get()!!
        if (id == state.captureId) state.captured = value
    }

    override fun loadInteger(id: Int, value: Int) {
        val state = evalState.get()!!
        if (id == state.captureId) state.captured = value
    }

    override fun loadColor(id: Int, color: Int) {
        val state = evalState.get()!!
        if (id == state.captureId) state.captured = color
    }

    override fun loadText(id: Int, text: String) {
        val state = evalState.get()!!
        if (id == state.captureId) state.captured = text
    }

    override fun putObject(id: Int, value: Any) {}

    override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {}

    override fun addCollection(id: Int, collection: ArrayAccess) {}

    override fun loadShader(id: Int, value: ShaderData) {}

    override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {}

    override fun loadBitmap(
        imageId: Int,
        encoding: Short,
        type: Short,
        width: Int,
        height: Int,
        data: ByteArray,
    ) {}

    override fun needsRepaint() {}
}
