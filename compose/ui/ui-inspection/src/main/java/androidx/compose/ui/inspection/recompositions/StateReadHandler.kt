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
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.CompositionRegistrationObserver
import androidx.compose.runtime.tooling.IdentifiableRecomposeScope
import androidx.compose.runtime.tooling.ObservableComposition
import androidx.compose.ui.inspection.inspector.InlineClassConverter
import androidx.compose.ui.inspection.inspector.RawParameter
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.findParameters
import androidx.inspection.ArtTooling
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
) :
    RecompositionHandler<RecompositionDataWithStateReads>(
        artTooling,
        anchorMap,
        { RecompositionDataWithStateReads() },
    ) {
    private val scope = CoroutineScope(Dispatchers.Main)

    // Job used for observing state reads.
    @GuardedBy("lock") private var observerJob: Job? = null

    // True while state reads are being observed. Updated under [lock].
    // A composition pass that started before observation stopped keeps calling the observer, so the
    // observer callbacks check this flag before recording anything.
    @Volatile private var observingStateReads = false

    // The anchors of all the composable where state reads are being collected for.
    // An empty set means all composables are observed.
    // This is an immutable set that is replaced under [lock], but read without holding [lock]
    // from the composition threads.
    @Volatile private var anchorsObserved: Set<Any> = emptySet()

    // The composition passes in progress on the current thread, with the innermost pass last.
    // A composition may create a sub-composition while it is being composed, which will interleave
    // the events from the 2 compositions on the same thread. Compositions from different
    // recomposers may be composed concurrently on different threads.
    // The list is only present on a thread while a composition is in progress on that thread.
    private val compositionPasses = ThreadLocal<MutableList<CompositionPass>>()

    @GuardedBy("lock") private val cache = StateReadCache(counts)

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

    // True while parameter changes are tracked with the CompositionTracer. Updated under [lock].
    // The composition passes are only needed for parameter changes, and are skipped otherwise.
    @Volatile private var trackingParameterChanges = false

    // An observer to keep track of compositions
    private val observer =
        object : CompositionRegistrationObserver, CompositionObserver, CompositionTracer {
            override fun onCompositionRegistered(composition: ObservableComposition) {
                synchronized(lock) { compositions[composition] = composition.setObserver(this) }
            }

            override fun onCompositionUnregistered(composition: ObservableComposition) {
                synchronized(lock) { compositions.remove(composition)?.dispose() }
            }

            override fun onBeginComposition(composition: ObservableComposition) {
                if (!trackingParameterChanges) {
                    return
                }
                val passes =
                    compositionPasses.get()
                        ?: mutableListOf<CompositionPass>().also { compositionPasses.set(it) }
                passes.add(CompositionPass(composition, anchorMap))
            }

            override fun onScopeEnter(scope: RecomposeScope) {
                val anchor = scope.identity
                anchor?.let { enteredAnchor.set(it) }
                // Note: A pass only exists if parameter changes were tracked when it started.
                currentCompositionPass()?.enterScope(anchor)
            }

            override fun onReadInScope(scope: RecomposeScope, value: Any) {
                val anchor = scope.identity ?: return
                synchronized(lock) {
                    if (isObserved(anchor)) {
                        cache.addStateRead(anchor, value, Exception())
                    }
                }
            }

            override fun onScopeExit(scope: RecomposeScope) {
                enteredAnchor.remove()
                currentCompositionPass()?.exitScope()
            }

            override fun onEndComposition(composition: ObservableComposition) {
                val pass = removePass(composition) ?: return
                if (!observingStateReads || pass.dirtyBits.isEmpty() || pass.hasOpenScopes) {
                    // Nothing to report, or the composition was aborted by an exception.
                    return
                }
                // The recomposition is done.
                // We can now read the parameter values for all the registered parameter changes.
                // This is done outside of [lock], since it uses reflection.
                val changes = findChangedParameterValues(pass)
                if (changes.isNotEmpty()) {
                    synchronized(lock) {
                        changes.forEach { (anchor, parameterChanges) ->
                            if (isObserved(anchor)) {
                                cache.addParameterChanges(anchor, parameterChanges)
                            }
                        }
                    }
                }
            }

            override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {
                val anchor = scope.identity ?: return
                synchronized(lock) {
                    if (isObserved(anchor)) {
                        cache.addInvalidation(anchor, value)
                    }
                }
            }

            override fun onScopeDisposed(scope: RecomposeScope) {}

            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                currentCompositionPass()?.traceEventStart(key, dirty1, dirty2)
            }

            override fun traceEventEnd() {}

            override fun isTraceInProgress(): Boolean = true

            // The anchor of this scope if it is being observed, otherwise null.
            private val RecomposeScope.identity: Any?
                get() {
                    val anchor = (this as? IdentifiableRecomposeScope)?.identity ?: return null
                    return anchor.takeIf { isObserved(it) }
                }

            private fun removePass(composition: ObservableComposition): CompositionPass? {
                val passes = compositionPasses.get() ?: return null
                // The runtime calls onEndComposition in a finally block, so the pass for this
                // composition is always at the top of the stack.
                if (passes.lastOrNull()?.composition !== composition) {
                    return null
                }
                val pass = passes.removeAt(passes.lastIndex)
                if (passes.isEmpty()) {
                    // Do not keep the list on threads that are no longer composing.
                    compositionPasses.remove()
                }
                return pass
            }
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
            var anchorsToObserve = emptySet<Any>()
            val newObservingStateReads =
                includeRecomposeCounts &&
                    when (settings.methodCase) {
                        MethodCase.BY_ID -> {
                            anchorsToObserve =
                                settings.byId.composableToObserveList.mapNotNullTo(HashSet()) {
                                    anchorMap[it]
                                }
                            anchorsToObserve.isNotEmpty()
                        }
                        MethodCase.ALL -> true
                        else -> false
                    }
            if (anchorsToObserve != anchorsObserved) {
                anchorsObserved = anchorsToObserve
                if (anchorsToObserve.isNotEmpty()) {
                    cache.removeAllExcept(anchorsToObserve)
                }
            }
            val includeParameterChanges =
                newObservingStateReads &&
                    when (settings.methodCase) {
                        MethodCase.BY_ID -> settings.byId.includeParameterChanges
                        MethodCase.ALL -> settings.all.includeParameterChanges
                        else -> false
                    }
            if (newObservingStateReads != (observerJob != null)) {
                if (newObservingStateReads) {
                    startObservingStateReads(includeParameterChanges)
                } else {
                    stopObservingStateReads()
                }
            } else if (newObservingStateReads) {
                // Parameter changes may be turned on or off while observing state reads.
                trackParameterChanges(includeParameterChanges)
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

    override fun incrementRecompositionCount(
        composer: Composer,
        anchor: Any,
    ): RecompositionDataWithStateReads? {
        if (!isObserved(anchor)) {
            return super.incrementRecompositionCount(composer, anchor)
        }
        // This is done outside of [lock], since only the current thread uses these.
        recordCompositionData(composer)
        // The scope is entered by the observer right before this hook is called, unless the
        // observer is not attached (yet) to the composition being composed.
        val entered = enteredAnchor.get() === anchor
        enteredAnchor.remove()
        synchronized(lock) {
            val data = super.incrementRecompositionCount(composer, anchor) ?: return null
            if (entered && isObserved(anchor)) {
                // Let the client tell this recomposition apart from one that did not happen yet.
                // Not when this recomposition is not observed, since its state reads are then not
                // recorded.
                data.markObserved()
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
     * Record the data of the composition being composed by [composer] in the current composition
     * pass. This is called from a hook on the composition thread, which avoids looking for the
     * composition among the composition roots of the Android Views on the main thread.
     */
    private fun recordCompositionData(composer: Composer) {
        if (!trackingParameterChanges) {
            return
        }
        val pass = currentCompositionPass() ?: return
        if (pass.compositionData == null && pass.hasOpenScopes) {
            pass.compositionData = composer.compositionData
        }
    }

    private fun currentCompositionPass(): CompositionPass? = compositionPasses.get()?.lastOrNull()

    /**
     * Return true if the composable with [anchor] is being observed.
     *
     * This is called from the composition threads without holding [lock]. Observation may stop or
     * change right after this returns true, so the observer callbacks call this again while holding
     * [lock] before recording anything.
     */
    private fun isObserved(anchor: Any): Boolean {
        if (!observingStateReads) {
            return false
        }
        val observed = anchorsObserved
        return observed.isEmpty() || anchor in observed
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
    fun getPurgedStateReadCount(): Long = synchronized(lock) { cache.purgedStateReads }

    /**
     * Find the parameter values of the changed parameters in a composition that just completed a
     * composition [pass].
     *
     * This is called on the composition thread without holding [lock].
     *
     * @return the changed parameters by anchor
     */
    @OptIn(UiToolingDataApi::class)
    private fun findChangedParameterValues(
        pass: CompositionPass
    ): List<Pair<Any, List<RawParameter>>> {
        val compositionData = pass.compositionData ?: return emptyList()
        val result = mutableListOf<Pair<Any, List<RawParameter>>>()
        for ((anchor, candidates) in pass.dirtyBits) {
            try {
                val group = compositionData.find(anchor) ?: continue
                val groupKey = group.key as? Int ?: continue
                val changes = candidates.firstOrNull { it.key == groupKey }?.changes ?: continue
                val parameterChanges = mutableListOf<RawParameter>()
                var bitMask = 1
                group.findParameters().forEach { parameter ->
                    if (changes and bitMask != 0) {
                        parameterChanges.add(inlineClassConverter.toRawParameter(parameter))
                    }
                    bitMask = bitMask shl 1
                }
                if (parameterChanges.isNotEmpty()) {
                    result.add(anchor to parameterChanges)
                }
            } catch (_: Exception) {
                // Never let the inspector throw from the composition. This may happen if the
                // composition was aborted by an exception after the anchor was recorded.
                // TODO(b/562965619): Report the exception to the client.
            }
        }
        return result
    }

    private fun startObservingStateReads(includeParameterChanges: Boolean) {
        synchronized(lock) {
            trackParameterChanges(includeParameterChanges)
            observingStateReads = true

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

    @GuardedBy("lock")
    private fun trackParameterChanges(includeParameterChanges: Boolean) {
        if (includeParameterChanges) {
            Composer.setTracer(observer)
        } else if (trackingParameterChanges) {
            Composer.setTracer(null)
        }
        trackingParameterChanges = includeParameterChanges
    }

    private fun stopObservingStateReads() {
        synchronized(lock) {
            observingStateReads = false
            trackParameterChanges(false)
            // Just return if we are not currently observing state reads
            val job = observerJob ?: return
            job.cancel()
            observerJob = null
            compositions.values.forEach { it.dispose() }
            compositions.clear()
            recomposers.values.forEach { it.dispose() }
            recomposers.clear()
            cache.clear()
        }
    }

    /**
     * Holds the changed-parameter bitmask recorded from [CompositionTracer.traceEventStart].
     *
     * @param key group key passed to [CompositionTracer.traceEventStart]
     * @param changes bitmask of the changed parameters, where bit `i` is set when parameter `i`
     *   changed
     */
    private data class DirtyBits(val key: Int, val changes: Int)

    /**
     * The data collected on the composition thread during a single composition pass of a
     * composition, i.e. between [CompositionObserver.onBeginComposition] and
     * [CompositionObserver.onEndComposition].
     *
     * Instances are confined to the thread doing the composition and do not need [lock].
     */
    private class CompositionPass(
        val composition: ObservableComposition,
        private val anchorMap: AnchorMap,
    ) {
        // The anchors of the entered scopes with the innermost scope last.
        // A null value is used for scopes that are not being observed.
        private val scopeAnchors = mutableListOf<Any?>()

        // True if [traceEventStart] was already received for the innermost scope.
        private var innermostScopeTraced = false

        /**
         * The candidate parameter changes by anchor of the composables recomposed in this pass.
         * Only candidates with parameter changes are kept. The candidate with the key of the group
         * of the anchor holds the parameter changes of the composable.
         */
        val dirtyBits = hashMapOf<Any, MutableList<DirtyBits>>()

        /**
         * The data of the composition being composed in this pass. This is used to find the
         * parameter values at the end of the pass on the composition thread.
         */
        var compositionData: CompositionData? = null

        /** True if a scope was entered and not exited, i.e. the pass was aborted. */
        val hasOpenScopes: Boolean
            get() = scopeAnchors.isNotEmpty()

        fun enterScope(anchor: Any?) {
            scopeAnchors.add(anchor)
            innermostScopeTraced = false
        }

        fun exitScope() {
            if (scopeAnchors.isNotEmpty()) {
                scopeAnchors.removeAt(scopeAnchors.lastIndex)
            }
            // The parent scope has already been traced before any child scope was entered.
            // Do not attribute trace events from non-restartable composables to the parent.
            innermostScopeTraced = true
        }

        fun traceEventStart(key: Int, dirty1: Int, dirty2: Int) {
            if (innermostScopeTraced) {
                return
            }
            val anchor = scopeAnchors.lastOrNull() ?: return
            // A non-restartable composable (e.g. called from a default parameter expression)
            // may be traced before the composable that owns the innermost scope.
            // When the key of the innermost scope is known, skip events from other composables.
            // When the key is not known yet (e.g. new composables, composables in a sub-composition
            // without layout nodes, or hidden system composables), keep all candidates with changes
            // and match the key of the group when the parameters are read at the end of the
            // composition.
            val expectedKey = anchorMap.getKey(anchor)
            if (expectedKey != 0) {
                if (expectedKey != key) {
                    return
                }
                innermostScopeTraced = true
            }
            val changes = findChanges(dirty1, dirty2)
            if (changes == 0) {
                return
            }
            val candidates = dirtyBits.getOrPut(anchor) { mutableListOf() }
            if (candidates.none { it.key == key }) {
                candidates.add(DirtyBits(key, changes))
            }
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
    }
}
