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
import androidx.collection.IntObjectMap
import androidx.collection.emptyIntObjectMap
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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings.MethodCase

/** The default number of recompositions with state reads per composable */
private const val DEFAULT_MAX_RECOMPOSITIONS_WITH_STATE_READS = 20

/** The time to wait for all state reads for a composable that was recomposed for the 1st time */
val DELAY_FOR_STATE_READS = 100.milliseconds

/** Sender for state read events from ComposeLayoutInspector */
fun interface StateReadEventSender {
    fun sendEvent(anchorHash: Int, stateReadsPerRecomposition: IntObjectMap<ObservedStateReads>)
}

/** The result for a [StateReadHandler.getReads] call. */
class ObservedReadResult(
    val firstObservedRecomposition: Int,
    val recomposition: Int,
    val reads: ObservedStateReads,
) {
    companion object {
        val EMPTY_RESULT = ObservedReadResult(0, 0, ObservedStateReads())
    }
}

/** An extension of [RecompositionHandler] that keeps track of state reads. */
class StateReadHandler(
    artTooling: ArtTooling,
    anchorMap: AnchorMap,
    private val readEventSender: StateReadEventSender,
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

    // The first state read was sent as an event for these anchors.
    @GuardedBy("lock") private val firstStateReadSendForAnchor = mutableSetOf<Any>()

    // The max number of recompositions with state reads kept per composable.
    @GuardedBy("lock") private var maxRecompositions = DEFAULT_MAX_RECOMPOSITIONS_WITH_STATE_READS

    // If true, send state read events for first recomposition with state reads,
    // and whenever state reads are discarded because of maxRecompositions.
    @GuardedBy("lock") private var sendDiscardedEvent = false

    // The recomposers being observed (for state reads)
    @OptIn(ExperimentalComposeRuntimeApi::class)
    @GuardedBy("lock")
    private val recomposers = mutableMapOf<RecomposerInfo, CompositionObserverHandle>()

    // The compositions being observed (for state reads)
    @OptIn(ExperimentalComposeRuntimeApi::class)
    @GuardedBy("lock")
    private val compositions = mutableMapOf<ObservableComposition, CompositionObserverHandle>()

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
                    counts
                        .getOrPut(anchor) { RecompositionDataWithStateReads() }
                        .addStateRead(value, Exception())
                }
            }

            override fun onScopeExit(scope: RecomposeScope) {}

            override fun onEndComposition(composition: ObservableComposition) {}

            override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {
                synchronized(lock) {
                    val anchor = (scope as? IdentifiableRecomposeScope)?.identity ?: return
                    // Again: filter by the composable.
                    if (anchorsObserved.isNotEmpty() && !anchorsObserved.contains(anchor)) {
                        return
                    }
                    counts
                        .getOrPut(anchor) { RecompositionDataWithStateReads() }
                        .addInvalidation(value)
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
            if (!keepRecomposeCounts) {
                firstStateReadSendForAnchor.clear()
            }
            if (!observingStateReads) {
                anchorsObserved.clear()
                firstStateReadSendForAnchor.clear()
            } else if (
                settings.methodCase == MethodCase.BY_ID &&
                    settings.byId.composableToObserveList != anchorsObserved
            ) {
                val stopObserving = mutableSetOf<Any>()
                stopObserving.addAll(anchorsObserved)
                anchorsObserved.clear()
                anchorsObserved.addAll(
                    settings.byId.composableToObserveList.mapNotNull { anchorMap[it] }
                )
                stopObserving.removeAll(anchorsObserved)
                stopObserving.forEach { counts[it]?.clearStateReads() }
                firstStateReadSendForAnchor.removeAll(stopObserving)
            }
            this.sendDiscardedEvent =
                when (settings.methodCase) {
                    MethodCase.BY_ID ->
                        settings.byId.sendDiscardedEvents && anchorsObserved.isNotEmpty()
                    else -> false
                }
            settings.methodCase == MethodCase.BY_ID && anchorsObserved.isNotEmpty()
            this.maxRecompositions =
                when (settings.methodCase) {
                    MethodCase.ALL -> settings.all.maxRecompositions
                    MethodCase.BY_ID -> settings.byId.maxRecompositions
                    else -> 0
                }.takeIf { it != 0 } ?: DEFAULT_MAX_RECOMPOSITIONS_WITH_STATE_READS
        }
    }

    override fun dispose() {
        super.dispose()
        scope.cancel()
    }

    // Return the stats reads for a recomposition of a composable or the first recomposition
    // with state reads for this composable. If no state reads exist return EMPTY_RESULT.
    fun getReads(anchorHash: Int, recomposition: Int): ObservedReadResult {
        synchronized(lock) {
            val anchor = anchorMap[anchorHash] ?: return ObservedReadResult.EMPTY_RESULT
            val data = counts[anchor] ?: return ObservedReadResult.EMPTY_RESULT
            val actualRecomposition = maxOf(data.firstObserved, recomposition)
            val observedStateReads =
                data.getReads(actualRecomposition, sendDiscardedEvent)
                    ?: return ObservedReadResult.EMPTY_RESULT
            val reads =
                when {
                    // If this the most recent recomposition, there may still be state reads
                    // being recorded, make a copy:
                    actualRecomposition == data.count -> observedStateReads.copy()
                    // Otherwise it is safe to return the ObservedStateReads
                    else -> observedStateReads
                }
            return ObservedReadResult(data.firstObserved, actualRecomposition, reads)
        }
    }

    override fun incrementRecompositionCount(anchor: Any): RecompositionDataWithStateReads? {
        val data: RecompositionDataWithStateReads
        synchronized(lock) {
            data = super.incrementRecompositionCount(anchor) ?: return null
            if (observerJob != null) {
                val newRecomposition = data.count
                if (!sendDiscardedEvent) {
                    // Quietly discard the state reads for older recompositions:
                    data.discardExcessStateReads(maxRecompositions)
                } else if (data.recompositionsWithObservations > maxRecompositions) {
                    sendStateReads(anchor, data.count - 1)
                } else if (
                    !firstStateReadSendForAnchor.contains(anchor) &&
                        anchorsObserved.contains(anchor)
                ) {
                    sendStateReads(
                        anchor,
                        newRecomposition,
                        doWaitForStateReadsOfFirstRecomposition = true,
                    )
                }
            }
        }
        return data
    }

    /**
     * Send state reads up to [lastRecomposition] of the specified anchor and discard those state
     * reads. Delay the send slightly if [doWaitForStateReadsOfFirstRecomposition] is true.
     */
    private fun sendStateReads(
        anchor: Any,
        lastRecomposition: Int,
        doWaitForStateReadsOfFirstRecomposition: Boolean = false,
    ) {
        scope.launch {
            if (doWaitForStateReadsOfFirstRecomposition) {
                // Delay a little to receive any upcoming state reads for the current recomposition
                delay(DELAY_FOR_STATE_READS)
            }
            var reads = emptyIntObjectMap<ObservedStateReads>()
            synchronized(lock) {
                if (
                    doWaitForStateReadsOfFirstRecomposition &&
                        firstStateReadSendForAnchor.contains(anchor)
                ) {
                    // We already sent the first event
                    return@launch
                }
                val data = counts[anchor] ?: return@launch
                reads = data.getAndRemoveReads(lastRecomposition)
                if (reads.isNotEmpty()) {
                    firstStateReadSendForAnchor.add(anchor)
                }
            }
            if (reads.isNotEmpty()) {
                readEventSender.sendEvent(anchorMap[anchor], reads)
            }
        }
    }

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
            for (data in counts.values) {
                data.clearStateReads()
            }
        }
    }
}
