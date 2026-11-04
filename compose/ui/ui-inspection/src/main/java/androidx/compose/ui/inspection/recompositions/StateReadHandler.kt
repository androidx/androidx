/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.inspection.recompositions

import androidx.annotation.GuardedBy
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.RecomposerInfo
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.CompositionRegistrationObserver
import androidx.compose.runtime.tooling.IdentifiableRecomposeScope
import androidx.compose.runtime.tooling.ObservableComposition
import androidx.compose.ui.inspection.RootsDetector
import androidx.compose.ui.inspection.inspector.InlineClassConverter
import androidx.compose.ui.inspection.inspector.RawParameter
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.findParameters
import androidx.inspection.ArtTooling
import java.util.IdentityHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings.MethodCase

private const val NUMBER_OF_CHANGE_MASKS_PER_DIRTY_VARIABLE = 10
private const val CHANGED_BIT_MASK = 0b011
private const val BITS_IN_CHANGE_MASK = 3

/**
 * The elements of the result for a [StateReadHandler.getReadsAndRemove] call.
 *
 * @param recomposition the recomposition number
 * @param reads the state reads for this recomposition
 * @param parameterChanges the parameters that changed
 */
class ObservedReadResult(
    val recomposition: Int,
    val reads: List<StateReadRecord>,
    val parameterChanges: List<RawParameter>,
)

/** An extension of [RecompositionHandler] that keeps track of state reads. */
@OptIn(
    ExperimentalComposeRuntimeApi::class,
    ComposeToolingApi::class,
    InternalComposeTracingApi::class,
)
internal class StateReadHandler(
    artTooling: ArtTooling,
    anchorMap: AnchorMap,
    private val inlineClassConverter: InlineClassConverter,
    private val rootsDetector: RootsDetector,
) :
    RecompositionHandler<RecompositionDataWithStateReads>(
        artTooling,
        anchorMap,
        { RecompositionDataWithStateReads() },
    ) {
    private val scope = CoroutineScope(Dispatchers.Main)

    // Job used for observing state reads.
    @GuardedBy("lock") private var observerJob: Job? = null

    // The anchors of all the composable where state reads are being collected for.
    @GuardedBy("lock") private val anchorsObserved = mutableSetOf<Any>()

    @GuardedBy("lock") private val cache = StateReadCache(counts)

    // The root [CompositionData] by [ObservableComposition] lazily detected.
    @GuardedBy("lock")
    private val rootByObservableComposition =
        IdentityHashMap<ObservableComposition, CompositionData>()

    // The recomposers being observed (for state reads)
    @GuardedBy("lock")
    private val recomposers = hashMapOf<RecomposerInfo, CompositionObserverHandle>()

    // The compositions being observed (for state reads)
    @GuardedBy("lock")
    private val compositions = hashMapOf<ObservableComposition, CompositionObserverHandle>()

    // The anchor of the scope last entered by the observer on the current thread.
    // A scope is entered in startRestartGroup right before the hook calls
    // incrementRecompositionCount on the same thread, unless the observer is not attached to the
    // composition being composed.
    private val enteredAnchor = ThreadLocal<Any?>()

    // The recomposing anchors and their parameterChangeMask for the current composition.
    // The insertion order is used to find the last recomposing anchor, which ObjectIntMap lacks.
    @Suppress("PrimitiveInCollection")
    @GuardedBy("lock")
    private val recompositions = LinkedHashMap<Any, Int>()

    // Use the CompositionTracer to keep track of parameter changes.
    @GuardedBy("lock") private var trackingParameterChanges = false

    // An observer to keep track of compositions
    private val observer =
        object : CompositionRegistrationObserver, CompositionObserver, CompositionTracer {
            override fun onCompositionRegistered(composition: ObservableComposition) {
                synchronized(lock) { compositions[composition] = composition.setObserver(this) }
            }

            override fun onCompositionUnregistered(composition: ObservableComposition) {
                synchronized(lock) { compositions.remove(composition)?.dispose() }
            }

            override fun onBeginComposition(composition: ObservableComposition) {}

            override fun onScopeEnter(scope: RecomposeScope) {
                (scope as? IdentifiableRecomposeScope)?.identity?.let { enteredAnchor.set(it) }
            }

            override fun onReadInScope(scope: RecomposeScope, value: Any) {
                synchronized(lock) {
                    val anchor = (scope as? IdentifiableRecomposeScope)?.identity ?: return
                    // For now: filter by the composable.
                    // Possible optimization: create only an observer for this scope.
                    if (anchorsObserved.isNotEmpty() && !anchorsObserved.contains(anchor)) {
                        return
                    }
                    cache.addStateRead(anchor, value, Exception())
                }
            }

            override fun onScopeExit(scope: RecomposeScope) {
                enteredAnchor.remove()
            }

            override fun onEndComposition(composition: ObservableComposition) {
                // The recomposition is done.
                // We can now read the parameter values for all the registered parameter changes.
                synchronized(lock) {
                    registerChangedParameterValues(composition)
                    recompositions.clear()
                }
            }

            override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {
                synchronized(lock) {
                    val anchor = (scope as? IdentifiableRecomposeScope)?.identity ?: return
                    // Again: filter by the composable.
                    if (!isObserving(anchor)) {
                        return
                    }
                    cache.addInvalidation(anchor, value)
                }
            }

            override fun onScopeDisposed(scope: RecomposeScope) {}

            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                // Detect parameter changes, but do not read the parameter values yet.
                registerParameterChanges(key, dirty1, dirty2)
            }

            override fun traceEventEnd() {}

            override fun isTraceInProgress(): Boolean = true
        }

    // Apply the [StateReadSettings] and recomposition settings received from Studio.
    fun changeCollectionMode(
        includeRecomposeCounts: Boolean,
        keepRecomposeCounts: Boolean,
        settings: StateReadSettings,
    ) {
        synchronized(lock) {
            if (!keepRecomposeCounts) {
                // The recomposition numbers are reset, so the recorded state reads and parameter
                // changes no longer match the recomposition numbers.
                cache.clear()
            }
            super.changeCollectionMode(includeRecomposeCounts, keepRecomposeCounts)
            val observingStateReads =
                includeRecomposeCounts &&
                    when (settings.methodCase) {
                        MethodCase.BY_ID -> settings.byId.composableToObserveList.isNotEmpty()
                        MethodCase.ALL -> true
                        else -> false
                    }
            val includeParameterChanges =
                observingStateReads &&
                    when (settings.methodCase) {
                        MethodCase.BY_ID -> settings.byId.includeParameterChanges
                        MethodCase.ALL -> settings.all.includeParameterChanges
                        else -> false
                    }
            if (observingStateReads != (observerJob != null)) {
                if (observingStateReads) {
                    startObservingStateReads(includeParameterChanges)
                } else {
                    stopObservingStateReads()
                }
            }
            if (!observingStateReads || settings.methodCase == MethodCase.ALL) {
                anchorsObserved.clear()
            } else {
                val anchorsToObserve =
                    settings.byId.composableToObserveList.mapNotNull { anchorMap[it] }
                if (anchorsToObserve != anchorsObserved) {
                    anchorsObserved.clear()
                    anchorsObserved.addAll(anchorsToObserve)
                    cache.removeAllExcept(anchorsObserved)
                }
            }
            cache.maxStateReads =
                when (settings.methodCase) {
                    MethodCase.ALL -> settings.all.maxStateReads
                    MethodCase.BY_ID -> settings.byId.maxStateReads
                    else -> 0
                }.takeIf { it > 0 } ?: DEFAULT_MAX_STATE_READS

            // For now: Use the same max for parameter changes:
            cache.maxRecompositionsWithParameterChanges = cache.maxStateReads
        }
    }

    override fun incrementRecompositionCount(anchor: Any): RecompositionDataWithStateReads? {
        // The scope is entered by our observer right before this hook is called, unless the
        // observer is not attached (yet) to the composition being composed.
        val entered = enteredAnchor.get() === anchor
        enteredAnchor.remove()
        synchronized(lock) {
            val data = super.incrementRecompositionCount(anchor) ?: return null
            if (isObserving(anchor)) {
                if (entered) {
                    // Let the client tell this recomposition apart from one that did not happen
                    // yet. Not when this recomposition is not observed, since its state reads are
                    // then not recorded.
                    data.markObserved()
                }
                recompositions[anchor] = 0
            }
            return data
        }
    }

    override fun dispose() {
        super.dispose()
        stopObservingStateReads()
        scope.cancel()
    }

    /**
     * Return the state reads and parameter changes for a range of recompositions for a composable.
     * Observed recompositions without state reads or parameter changes are included without any.
     * There may be holes in the data, e.g. for recompositions before observation started, or for
     * recompositions discarded from the cache.
     *
     * @param anchorHash the anchorHash of the composable
     * @param recompositionNumberStart the lower recomposition to look for
     * @param recompositionNumberEnd the upper recomposition to look for
     * @param includeExtra include extra state reads after recompositionNumberEnd if state reads are
     *   missing from the requested range
     */
    fun getReadsAndRemove(
        anchorHash: Int,
        recompositionNumberStart: Int,
        recompositionNumberEnd: Int,
        includeExtra: Boolean,
    ): List<ObservedReadResult> {
        synchronized(lock) {
            val anchor = anchorMap[anchorHash] ?: return emptyList()
            return cache.getReadsAndRemove(
                anchor,
                recompositionNumberStart,
                recompositionNumberEnd,
                includeExtra,
            )
        }
    }

    /** Return the number of state reads purged from the state read cache. */
    fun getPurgedStateReadCount(): Long = cache.purgedStateReads

    /**
     * Register the parameters that have changed.
     *
     * At this point the parameter value has not been updated in the SlotTree. Instead, we will
     * compute a compact parameter change mask and store it in the recomposition data.
     */
    private fun registerParameterChanges(key: Int, dirty1: Int, dirty2: Int) {
        synchronized(lock) {
            val changes = findChanges(dirty1, dirty2)
            if (changes == 0 || recompositions.isEmpty()) return
            val anchor = recompositions.keys.last()
            if (anchorMap.getKey(anchor) == key) {
                recompositions[anchor] = changes
            }
        }
    }

    @OptIn(UiToolingDataApi::class)
    private fun registerChangedParameterValues(composition: ObservableComposition) {
        synchronized(lock) {
            for ((anchor, changes) in recompositions) {
                if (changes == 0 || !isObserving(anchor)) {
                    // Observation may have stopped, or changed, during the composition.
                    continue
                }
                val parameters = findGroup(composition, anchor)?.findParameters()
                if (parameters.isNullOrEmpty()) {
                    continue
                }
                val parameterChanges = mutableListOf<RawParameter>()
                var bitMask = 1
                parameters.forEach { parameter ->
                    if (changes and bitMask != 0) {
                        parameterChanges.add(inlineClassConverter.toRawParameter(parameter))
                    }
                    bitMask = bitMask shl 1
                }
                if (parameterChanges.isNotEmpty()) {
                    cache.addParameterChanges(anchor, parameterChanges)
                }
            }
        }
    }

    private fun findGroup(composition: ObservableComposition, anchor: Any): CompositionGroup? {
        val root = rootByObservableComposition[composition]
        if (root != null) return root.find(anchor)
        rootsDetector.getAllCompositionRoots().forEach { foundRoot ->
            val group = foundRoot.find(anchor)
            if (group != null) {
                rootByObservableComposition[composition] = foundRoot
                return group
            }
        }
        return null
    }

    private fun findChanges(dirty1: Int, dirty2: Int): Int {
        return convertDirtyBits(dirty1) or
            (convertDirtyBits(dirty2) shl NUMBER_OF_CHANGE_MASKS_PER_DIRTY_VARIABLE)
    }

    private fun convertDirtyBits(dirty: Int): Int {
        var changeBit = 1
        var changes = 0
        var bits = dirty shr 1
        var index = 0
        while (index < NUMBER_OF_CHANGE_MASKS_PER_DIRTY_VARIABLE && bits != 0) {
            // Each parameter uses 3 (BITS_IN_CHANGE_MASK) bits.
            // Status is in the lower 2 bits of the 3-bit slot, starting at bit 1.
            // A value of 2 means the value changed.
            val status = bits and CHANGED_BIT_MASK
            if (status == 2) {
                changes = changes or changeBit
            }
            bits = bits shr BITS_IN_CHANGE_MASK
            changeBit = changeBit shl 1
            index++
        }
        return changes
    }

    private fun isObserving(anchor: Any): Boolean =
        observerJob != null && (anchorsObserved.isEmpty() || anchorsObserved.contains(anchor))

    private fun startObservingStateReads(includeParameterChanges: Boolean) {
        synchronized(lock) {
            if (includeParameterChanges) {
                Composer.setTracer(observer)
            } else if (trackingParameterChanges) {
                Composer.setTracer(null)
            }
            trackingParameterChanges = includeParameterChanges

            if (observerJob != null) {
                // We are already observing state reads
                return
            }
        }
        observerJob = scope.launch {
            Recomposer.runningRecomposers.collect { running ->
                running.forEach { info ->
                    val alreadyRegistered = synchronized(lock) { info in recomposers.keys }
                    if (!alreadyRegistered) {
                        info.observe(observer)?.let { handle ->
                            synchronized(lock) { recomposers[info] = handle }
                        }
                    }
                }
                val toRemove = synchronized(lock) { recomposers.keys.filter { it !in running } }
                toRemove.forEach { info ->
                    synchronized(lock) {
                        recomposers[info]?.dispose()
                        recomposers.remove(info)
                    }
                }
            }
        }
    }

    private fun stopObservingStateReads() {
        synchronized(lock) {
            if (trackingParameterChanges) {
                Composer.setTracer(null)
                trackingParameterChanges = false
            }
            // Just return if we are not currently observing state reads
            val job = observerJob ?: return
            job.cancel()
            observerJob = null
            compositions.values.forEach { it.dispose() }
            compositions.clear()
            recomposers.values.forEach { it.dispose() }
            recomposers.clear()
            recompositions.clear()
            cache.clear()
        }
    }
}
