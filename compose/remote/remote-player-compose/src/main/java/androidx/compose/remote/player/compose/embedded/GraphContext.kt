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
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import java.util.concurrent.ConcurrentHashMap

/**
 * A pure-Compose evaluator for *computed* operations (color/text/float/int expressions, attributes,
 * lookups) built out of `derivedStateOf` — no imperative recompute pass, no dirty flags.
 *
 * Each computed id resolves to a [derivedStateOf] that runs the op's existing `updateVariables` +
 * `apply` against **this** context. We intercept the two things an op does:
 * - **reads** (`getFloat`/`getInteger`/`getColor`/`getText`) — for a computed input we recurse into
 *   *its* `State` (so chains compose and only what's read recomputes); for a leaf we read the real
 *   snapshot-backed store via `super`, which records the snapshot dependency. So Compose discovers
 *   each op's dependencies automatically — we never enumerate input ids.
 * - **writes** (`loadFloat`/`loadInteger`/`loadColor`/`loadText`) — captured as the op's result
 *   rather than mutated into the store, which makes the op a pure function for the
 *   `derivedStateOf`.
 *
 * The operations are used verbatim (no core changes). Non-scalar reads
 * (objects/bitmaps/collections/ paths) fall through to the shared store via `super`. The
 * capture/cycle bookkeeping is single-thread (Compose UI read phases); nested chain evaluation is
 * handled by save/restore.
 *
 * Conceptually this is a `RemoteReadContext` (a value view) that captures the one write an op makes
 * as its result. It still extends [AndroidRemoteContext] only because the op contract
 * (`apply`/`updateVariables`) takes the concrete `RemoteContext`; once that contract is split onto
 * the read/write interfaces (issue #12), this collapses to a lightweight `RemoteReadContext` +
 * capture sink with no platform subclass.
 */
internal class GraphContext(
    private val realState: SnapshotRemoteComposeState,
    private val computedOps: IntObjectMap<Operation>,
    private val timeMillis: State<Float>,
    clock: RemoteClock,
) : AndroidRemoteContext(clock) {

    init {
        // Share the leaf store so collections/objects/paths and plain variables resolve against the
        // same (snapshot-backed) data the rest of the player uses.
        mRemoteComposeState = realState
    }

    @Suppress("BanConcurrentHashMap") private val states = ConcurrentHashMap<Int, State<Any?>>()

    /**
     * Particle loops whose state has been seeded (keyed by op identity). Lives here because the
     * GraphContext is the per-document Compose-side runtime state, remembered across frames — the
     * particle simulation (see RcPlayerParticles) needs persistent state but runs from the draw
     * pass, which isn't a composable.
     */
    @Suppress("BanConcurrentHashMap")
    internal val particlesInitialized: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    /**
     * The active [RcImageLoader], set by [RcPlayer]. Lives here because the canvas draw path (which
     * isn't a composable, so can't read [LocalRcImageLoader]) needs it to resolve document image
     * draws through the same pluggable loader the composable Image layout uses.
     */
    internal var imageLoader: RcImageLoader? = null

    // Capture bookkeeping is per-thread: `derivedStateOf` may be evaluated on whichever thread
    // reads
    // it (UI phases are main-thread today, but snapshot reads aren't contractually single-thread).
    // Re-entrancy within a thread (chained ops) is handled by save/restore.
    private val computing = ThreadLocal.withInitial { HashSet<Int>() }
    private val captureId = ThreadLocal.withInitial { -1 }
    private val captured = ThreadLocal<Any?>()

    /** True if [id] is produced by a computed op (vs a leaf variable). */
    fun isComputed(id: Int): Boolean = computedOps.containsKey(id)

    private fun computedValue(id: Int): Any? {
        if (id in computing.get()!!) return null // cycle: break rather than recurse forever
        val state =
            states.getOrPut(id) {
                derivedStateOf {
                    val op = computedOps[id] ?: return@derivedStateOf null
                    // Fetch per-thread bookkeeping on the thread actually evaluating the block.
                    val active: HashSet<Int> = computing.get()!!
                    val prevId = captureId.get()
                    val prevCaptured = captured.get()
                    captureId.set(id)
                    captured.set(null)
                    active += id
                    try {
                        if (op is VariableSupport)
                            op.updateVariables(this) // reads inputs (tracked)
                        op.apply(this) // writes output -> captured
                        captured.get()
                    } finally {
                        captureId.set(prevId)
                        captured.set(prevCaptured)
                        active -= id
                    }
                }
            }
        return state.value
    }

    override fun getFloat(id: Int): Float =
        when {
            // Time variables come from the Compose frame-clock state (matching the resolver's time
            // special-case), not the raw store — so a time-driven op reads seconds/minutes/hours.
            id == RemoteContext.ID_TIME_IN_SEC -> timeMillis.value / 1000f
            id == RemoteContext.ID_TIME_IN_MIN -> timeMillis.value / 60000f
            id == RemoteContext.ID_TIME_IN_HR -> timeMillis.value / 3600000f
            isComputed(id) -> (computedValue(id) as? Number)?.toFloat() ?: 0f
            else -> super.getFloat(id)
        }

    override fun getInteger(id: Int): Int =
        if (isComputed(id)) (computedValue(id) as? Number)?.toInt() ?: 0 else super.getInteger(id)

    override fun getColor(id: Int): Int =
        if (isComputed(id)) (computedValue(id) as? Number)?.toInt() ?: 0 else super.getColor(id)

    override fun getText(id: Int): String? =
        if (isComputed(id)) computedValue(id) as? String else super.getText(id)

    // GraphContext is a read-only-store *evaluation* context: a computed op's apply must not mutate
    // the shared store (that would be a snapshot write during a derivedStateOf read, and would let
    // one op clobber another's value). The scalar writes capture the op's own output; every other
    // write is a no-op. This makes the model robust even for ops that write more than once or via
    // non-scalar channels (e.g. MatrixExpression does putObject + loadFloat; Path/Shader/collection
    // ops write paths/shaders/collections) — those ops aren't read through the scalar resolvers,
    // but
    // if one ever is, it degrades to a captured scalar / default instead of corrupting the store.

    override fun loadFloat(id: Int, value: Float) {
        if (id == captureId.get()) captured.set(value)
    }

    override fun loadInteger(id: Int, value: Int) {
        if (id == captureId.get()) captured.set(value)
    }

    override fun loadColor(id: Int, color: Int) {
        if (id == captureId.get()) captured.set(color)
    }

    override fun loadText(id: Int, text: String) {
        if (id == captureId.get()) captured.set(text)
    }

    // Non-scalar / multi-writes during evaluation are suppressed (never reach the real store).
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
