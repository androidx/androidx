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

package androidx.compose.remote.player.compose.embedded.doc

import androidx.collection.IntObjectMap
import androidx.collection.IntSet
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.collection.MutableLongObjectMap
import androidx.collection.emptyIntSet
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.player.compose.embedded.RcImageLoader
import androidx.compose.remote.player.compose.embedded.RcTypefaceResolver
import androidx.compose.remote.player.compose.embedded.draw.ComposeLocalPaint
import androidx.compose.remote.player.compose.embedded.draw.GraphPaintContext
import androidx.compose.remote.player.compose.embedded.draw.derefId
import androidx.compose.remote.player.compose.embedded.resources.RcTypefaceCache
import androidx.compose.remote.player.compose.embedded.resources.resolveImageAttributeDimensions
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.State
import androidx.compose.ui.text.TextMeasurer
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
    /** Reactive time fields and clock that time reads in this context resolve against. */
    internal val timeState: GraphTimeState,
    internal var componentValues: Map<Int, State<Float>> = emptyMap(),
    private val touchExpressionIds: IntSet = emptyIntSet(),
    internal var animatedFloatStates: Map<Int, State<Float>> = emptyMap(),
) : StoreBackedRemoteContext(timeState.baseClock) {

    internal val typefaceCache = RcTypefaceCache()

    internal var typefaceResolver: RcTypefaceResolver?
        get() = typefaceCache.resolver
        set(value) {
            typefaceCache.resolver = value
            (store as? AndroidRemoteContext)?.setTypefaceResolver(
                typefaceCache.coreTypefaceResolver
            )
        }

    /**
     * The context that owns the shared store. This context drops non-scalar writes (see
     * [loadBitmap], [putObject]) to keep evaluation pure, so writes that must persist, such as
     * lazily decoding a bitmap, go here instead. See [writeContext].
     */
    internal var store: RemoteContext? = null
        set(value) {
            field = value
            (value as? AndroidRemoteContext)?.setTypefaceResolver(
                typefaceCache.coreTypefaceResolver
            )
        }

    init {
        // Share the leaf store so collections/objects/paths and plain variables resolve against the
        // same (snapshot-backed) data the rest of the player uses.
        mRemoteComposeState = realState
        // The base clock goes to the super constructor, which reads the clock (for the document
        // load time) before this class's properties are initialised. Switch to the reactive clock
        // afterwards, so operations that read time through getClock() record a time read.
        setClock(timeState.reactiveClock)
    }

    private val graphPaintContext = GraphPaintContext(this)
    private val textMeasurePaints = MutableIntObjectMap<ComposeLocalPaint>()

    internal var textMeasurer: TextMeasurer?
        get() = graphPaintContext.textMeasurer
        set(value) {
            graphPaintContext.textMeasurer = value
        }

    internal fun setTextMeasurePaint(id: Int, paintState: ComposeLocalPaint) {
        textMeasurePaints[id] = paintState
    }

    /**
     * IDs (`ParticlesCreate.getId()`) of `ParticlesCreate` ops that have seeded their particles at
     * least once, so a `ParticlesLoop` only falls back to seeding a source the draw stream never
     * painted.
     */
    internal val particlesInitialized: MutableIntSet = MutableIntSet()

    /**
     * Fire state of each impulse whose window is open, keyed by the operation (identity: operations
     * don't override equals) and then by the canvas loop iteration path it runs under (0 outside
     * any loop). An entry exists only while that window is open and records that its initial pass
     * already ran: the embedded equivalent of core `ImpulseOperation.mInitialPass` being false.
     * Entries are dropped when the window closes or is pending, so the map stays bounded by the
     * impulses currently in their window.
     */
    internal val impulseWindows: MutableMap<Any, MutableLongObjectMap<ImpulseWindowState>> =
        ConcurrentHashMap()

    /** Removes the fire state of [op] under [loopScope], if any, without allocating. */
    internal fun clearImpulseWindow(op: Any, loopScope: Long) {
        val states = impulseWindows[op] ?: return
        states.remove(loopScope)
        if (states.isEmpty()) impulseWindows.remove(op)
    }

    /** Returns the fire state of [op] under [loopScope], or null if its window was not open. */
    internal fun impulseWindow(op: Any, loopScope: Long): ImpulseWindowState? =
        impulseWindows[op]?.get(loopScope)

    /** Records that the initial pass of [op] under [loopScope] ran for [startAt] at time [now]. */
    internal fun openImpulseWindow(op: Any, loopScope: Long, startAt: Float, now: Float) {
        impulseWindows
            .getOrPut(op) { MutableLongObjectMap() }
            .getOrPut(loopScope) { ImpulseWindowState() }
            .restart(startAt, now)
    }

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

    internal fun isTouchExpression(id: Int): Boolean = touchExpressionIds.contains(id)

    private fun computedValue(id: Int): Any? {
        val state = evalState.get()!!
        if (state.computing.contains(id)) return null // cycle: break rather than recurse forever

        if (state.memoPass.contains(id)) {
            return state.memoValues[id]
        }

        val op = computedOps[id] ?: return null
        if (state.depth == 0) {
            clearLastOpCount()
        }
        state.depth++
        val prevId = state.captureId
        val prevCaptured = state.captured
        val prevPaintState = graphPaintContext.paintState
        state.captureId = id
        state.captured = null
        state.computing.add(id)
        try {
            if (op is VariableSupport && op !is FloatExpression) {
                op.updateVariables(this) // reads inputs (tracked)
            }
            // Value-producing PaintOperations (e.g. ColorAttribute, TextMeasure, ImageAttribute)
            // compute and write their outputs during paint(paintContext) rather than
            // apply(remoteContext).
            if (op is PaintOperation && op !is Component) {
                if (op is ImageAttribute) {
                    resolveImageAttributeDimensions(this, writeContext, derefId(op.imageId, this))
                }
                graphPaintContext.paintState = textMeasurePaints[id] ?: ComposeLocalPaint()
                op.paint(graphPaintContext)
            } else if (op is FloatExpression) {
                loadFloat(id, op.evaluate(this))
            } else {
                op.apply(this) // writes output -> captured
            }
            val result = state.captured
            state.memoPass.add(id)
            state.memoValues[id] = result
            return result
        } finally {
            graphPaintContext.paintState = prevPaintState
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
        val animVal = animatedFloatStates[id]
        return when {
            compVal != null -> compVal.value
            realState.isFloatOverridden(id) -> super.getFloat(id)
            animVal != null -> animVal.value
            isComputed(id) -> (computedValue(id) as? Number)?.toFloat() ?: 0f
            isTimeVariable(id) -> timeState.timeFloatState(id).value
            else -> super.getFloat(id)
        }
    }

    override fun getInteger(id: Int): Int =
        when {
            realState.isIntegerOverridden(id) -> super.getInteger(id)
            isComputed(id) -> (computedValue(id) as? Number)?.toInt() ?: 0
            id == RemoteContext.ID_EPOCH_SECOND -> timeState.epochSecondState.intValue
            isTimeVariable(id) -> timeState.timeFloatState(id).value.toInt()
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

    override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
        if (evalState.get()!!.captureId == -1) {
            super.loadPathData(instanceId, winding, floatPath)
        }
    }

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

/**
 * The context to write through: the [GraphContext.store] behind a [GraphContext], otherwise this
 * context itself. Lets helpers take a single (read) context and still persist writes.
 */
internal val RemoteContext.writeContext: RemoteContext
    get() = (this as? GraphContext)?.store ?: this

/**
 * Fire state of one open impulse window (see [GraphContext.impulseWindows]): the start time its
 * initial pass ran for, and the first and latest animation times that start was seen at.
 */
internal class ImpulseWindowState {
    var startAt: Float = Float.NaN
        private set

    private var firstSeen = 0f
    private var lastSeen = 0f

    /**
     * True once [startAt] has been seen at two different animation times, i.e. it held across a
     * frame rather than following the clock.
     */
    val isStartStable: Boolean
        get() = lastSeen > firstSeen

    fun restart(startAt: Float, now: Float) {
        this.startAt = startAt
        firstSeen = now
        lastSeen = now
    }

    fun seen(now: Float) {
        if (now > lastSeen) lastSeen = now
    }
}
