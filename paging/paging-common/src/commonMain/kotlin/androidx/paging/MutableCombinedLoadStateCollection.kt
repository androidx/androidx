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
import androidx.paging.internal.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Helper to construct [CombinedLoadStates] that accounts for previous state to set the convenience
 * properties correctly.
 *
 * This class exposes a [StateFlow] and handles dispatches to tracked [listeners] intended for use
 * with presenter APIs, which has the nuance of filtering out the initial value and dispatching to
 * listeners immediately as they get added.
 */
internal class MutableCombinedLoadStateCollection {

    private val listeners = CopyOnWriteArrayList<(CombinedLoadStates) -> Unit>()
    private val _stateFlow = MutableStateFlow<CombinedLoadStates?>(null)
    public val stateFlow = _stateFlow.asStateFlow()

    fun set(sourceLoadStates: LoadStates, remoteLoadStates: LoadStates?) =
        dispatchNewState { currState ->
            computeNewState(currState, sourceLoadStates, remoteLoadStates)
        }

    fun set(type: LoadType, remote: Boolean, state: LoadState) =
        dispatchNewState { currState ->
            var source = currState?.source ?: LoadStates.IDLE
            var mediator = currState?.mediator ?: LoadStates.IDLE

            if (remote) {
                mediator = mediator.modifyState(type, state)
            } else {
                source = source.modifyState(type, state)
            }
            computeNewState(currState, source, mediator)
        }

    fun get(type: LoadType, remote: Boolean): LoadState? {
        val state = _stateFlow.value
        return (if (remote) state?.mediator else state?.source)?.get(type)
    }

    /**
     * When a new listener is added, it will be immediately called with the current
     * [CombinedLoadStates] unless no state has been set yet, and thus has no valid state to emit.
     */
    fun addListener(listener: (CombinedLoadStates) -> Unit) {
        // Note: Important to add the listener first before sending off events, in case the
        // callback triggers removal, which could lead to a leak if the listener is added
        // afterwards.
        listeners.add(listener)
        _stateFlow.value?.also { listener(it) }
    }

    fun removeListener(listener: (CombinedLoadStates) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * Computes and dispatches the new CombinedLoadStates. No-op if new value is same as
     * previous value.
     *
     * We manually de-duplicate emissions to StateFlow and to listeners even though
     * [MutableStateFlow.update] de-duplicates automatically in that duplicated values are set but
     * not sent to collectors. However it doesn't indicate whether the new value is indeed a
     * duplicate or not, so we still need to manually compare previous/updated values before
     * sending to listeners. Because of that, we manually de-dupe both stateFlow and listener
     * emissions to ensure they are in sync.
     */
    private fun dispatchNewState(
        computeNewState: (currState: CombinedLoadStates?) -> CombinedLoadStates
    ) {
        var newState: CombinedLoadStates? = null
        _stateFlow.update { currState ->
            val computed = computeNewState(currState)
            if (currState != computed) {
                newState = computed
                computed
            } else {
                // no-op, doesn't dispatch
                return
            }
        }
        newState?.apply { listeners.forEach { it(this) } }
    }

    private fun computeNewState(
        previousState: CombinedLoadStates?,
        newSource: LoadStates,
        newRemote: LoadStates?
    ): CombinedLoadStates {
        val refresh = computeHelperState(
            previousState = previousState?.refresh ?: NotLoading.Incomplete,
            sourceRefreshState = newSource.refresh,
            sourceState = newSource.refresh,
            remoteState = newRemote?.refresh

        )
        val prepend = computeHelperState(
            previousState = previousState?.prepend ?: NotLoading.Incomplete,
            sourceRefreshState = newSource.refresh,
            sourceState = newSource.prepend,
            remoteState = newRemote?.prepend
        )
        val append = computeHelperState(
            previousState = previousState?.append ?: NotLoading.Incomplete,
            sourceRefreshState = newSource.refresh,
            sourceState = newSource.append,
            remoteState = newRemote?.append
        )

        return CombinedLoadStates(
            refresh = refresh,
            prepend = prepend,
            append = append,
            source = newSource,
            mediator = newRemote,
        )
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
