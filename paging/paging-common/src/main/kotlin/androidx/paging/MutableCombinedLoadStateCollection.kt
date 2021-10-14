/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging

import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Helper to construct [CombinedLoadStates] that accounts for previous state to set the convenience
 * properties correctly.
 *
 * This class exposes a [flow] and handles dispatches to tracked [listeners] intended for use
 * with presenter APIs, which has the nuance of filtering out the initial value and dispatching to
 * listeners immediately as they get added.
 */
internal class MutableCombinedLoadStateCollection {
    /**
     * Tracks whether this [MutableCombinedLoadStateCollection] has been updated with real state
     * or has just been instantiated with its initial values.
     */
    private var isInitialized: Boolean = false
    private val listeners = CopyOnWriteArrayList<(CombinedLoadStates) -> Unit>()

    private var refresh: LoadState = NotLoading.Incomplete
    private var prepend: LoadState = NotLoading.Incomplete
    private var append: LoadState = NotLoading.Incomplete
    var source: LoadStates = LoadStates.IDLE
        private set
    var mediator: LoadStates? = null
        private set

    private val _stateFlow = MutableStateFlow<CombinedLoadStates?>(null)
    val flow: Flow<CombinedLoadStates> = _stateFlow.filterNotNull()

    fun set(sourceLoadStates: LoadStates, remoteLoadStates: LoadStates?) {
        isInitialized = true
        source = sourceLoadStates
        mediator = remoteLoadStates
        updateHelperStatesAndDispatch()
    }

    fun set(type: LoadType, remote: Boolean, state: LoadState): Boolean {
        isInitialized = true
        val didChange = if (remote) {
            val lastMediator = mediator
            mediator = (mediator ?: LoadStates.IDLE).modifyState(type, state)
            mediator != lastMediator
        } else {
            val lastSource = source
            source = source.modifyState(type, state)
            source != lastSource
        }

        updateHelperStatesAndDispatch()
        return didChange
    }

    fun get(type: LoadType, remote: Boolean): LoadState? {
        return (if (remote) mediator else source)?.get(type)
    }

    /**
     * When a new listener is added, it will be immediately called with the current [snapshot]
     * unless no state has been set yet, and thus has no valid state to emit.
     */
    fun addListener(listener: (CombinedLoadStates) -> Unit) {
        // Note: Important to add the listener first before sending off events, in case the
        // callback triggers removal, which could lead to a leak if the listener is added
        // afterwards.
        listeners.add(listener)
        snapshot()?.also { listener(it) }
    }

    fun removeListener(listener: (CombinedLoadStates) -> Unit) {
        listeners.remove(listener)
    }

    private fun snapshot(): CombinedLoadStates? = when {
        !isInitialized -> null
        else -> CombinedLoadStates(
            refresh = refresh,
            prepend = prepend,
            append = append,
            source = source,
            mediator = mediator,
        )
    }

    private fun updateHelperStatesAndDispatch() {
        refresh = computeHelperState(
            previousState = refresh,
            sourceRefreshState = source.refresh,
            sourceState = source.refresh,
            remoteState = mediator?.refresh
        )
        prepend = computeHelperState(
            previousState = prepend,
            sourceRefreshState = source.refresh,
            sourceState = source.prepend,
            remoteState = mediator?.prepend
        )
        append = computeHelperState(
            previousState = append,
            sourceRefreshState = source.refresh,
            sourceState = source.append,
            remoteState = mediator?.append
        )

        val snapshot = snapshot()
        if (snapshot != null) {
            _stateFlow.value = snapshot
            listeners.forEach { it(snapshot) }
        }
    }

    /**
     * Computes the next value for the convenience helpers in [CombinedLoadStates], which
     * generally defers to remote state, but waits for both source and remote states to become
     * [NotLoading] before moving to that state. This provides a reasonable default for the common
     * use-case where you generally want to wait for both RemoteMediator to return and for the
     * update to get applied before signaling to UI that a network fetch has "finished".
     */
    private fun computeHelperState(
        previousState: LoadState,
        sourceRefreshState: LoadState,
        sourceState: LoadState,
        remoteState: LoadState?
    ): LoadState {
        if (remoteState == null) return sourceState

        return when (previousState) {
            is Loading -> when {
                sourceRefreshState is NotLoading && remoteState is NotLoading -> remoteState
                remoteState is Error -> remoteState
                else -> previousState
            }
            else -> remoteState
        }
    }
}
