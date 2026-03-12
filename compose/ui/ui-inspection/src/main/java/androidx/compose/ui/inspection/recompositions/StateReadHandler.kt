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
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.RecomposerInfo
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.CompositionRegistrationObserver
import androidx.compose.runtime.tooling.IdentifiableRecomposeScope
import androidx.compose.runtime.tooling.ObservableComposition
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.inspection.ArtTooling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings.MethodCase

/** The elements of the result for a [StateReadHandler.getReadsAndRemove] call. */
class ObservedReadResult(val recomposition: Int, val reads: List<StateReadRecord>)

/** An extension of [RecompositionHandler] that keeps track of state reads. */
class StateReadHandler(artTooling: ArtTooling, anchorMap: AnchorMap) :
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

    // The recomposers being observed (for state reads)
    @OptIn(ExperimentalComposeRuntimeApi::class)
    @GuardedBy("lock")
    private val recomposers = hashMapOf<RecomposerInfo, CompositionObserverHandle>()

    // The compositions being observed (for state reads)
    @OptIn(ExperimentalComposeRuntimeApi::class)
    @GuardedBy("lock")
    private val compositions = hashMapOf<ObservableComposition, CompositionObserverHandle>()

    // An observer to keep track of compositions
    @OptIn(ExperimentalComposeRuntimeApi::class, ComposeToolingApi::class)
    private val observer =
        object : CompositionRegistrationObserver, CompositionObserver {

            override fun onCompositionRegistered(composition: ObservableComposition) {
                synchronized(lock) { compositions[composition] = composition.setObserver(this) }
            }

            override fun onCompositionUnregistered(composition: ObservableComposition) {
                synchronized(lock) { compositions.remove(composition)?.dispose() }
            }

            override fun onBeginComposition(composition: ObservableComposition) {}

            override fun onScopeEnter(scope: RecomposeScope) {}

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

            override fun onScopeExit(scope: RecomposeScope) {}

            override fun onEndComposition(composition: ObservableComposition) {}

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
        }

    // Apply the [StateReadSettings] and recomposition settings received from Studio.
    fun changeCollectionMode(
        includeRecomposeCounts: Boolean,
        keepRecomposeCounts: Boolean,
        settings: StateReadSettings,
    ) {
        synchronized(lock) {
            super.changeCollectionMode(includeRecomposeCounts, keepRecomposeCounts)
            val observingStateReads =
                includeRecomposeCounts &&
                    when (settings.methodCase) {
                        MethodCase.BY_ID -> settings.byId.composableToObserveList.isNotEmpty()
                        MethodCase.ALL -> true
                        else -> false
                    }
            if (observingStateReads != (observerJob != null)) {
                if (observingStateReads) {
                    startObservingStateReads()
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
                }.takeIf { it != 0 } ?: DEFAULT_MAX_STATE_READS
        }
    }

    override fun incrementRecompositionCount(anchor: Any): RecompositionDataWithStateReads? {
        synchronized(lock) {
            val data = super.incrementRecompositionCount(anchor) ?: return null
            if (isObserving(anchor)) {
                data.expectStateReads()
            }
            return data
        }
    }

    override fun dispose() {
        super.dispose()
        scope.cancel()
    }

    /**
     * Return the stats reads for a range of recompositions for a composable. There may be holes in
     * the data i.e. recompositions with no state reads.
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

    private fun isObserving(anchor: Any): Boolean =
        observerJob != null && (anchorsObserved.isEmpty() || anchorsObserved.contains(anchor))

    @OptIn(ExperimentalComposeRuntimeApi::class)
    private fun startObservingStateReads() {
        synchronized(lock) {
            if (observerJob != null) {
                // We are already observing state reads
                return
            }
        }
        observerJob =
            scope.launch {
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

    @OptIn(ExperimentalComposeRuntimeApi::class)
    private fun stopObservingStateReads() {
        synchronized(lock) {
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
}
