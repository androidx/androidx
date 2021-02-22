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

/**
 * Helper to construct [CombinedLoadStates] that accounts for previous state to set the convenience
 * properties correctly.
 */
internal class MutableLoadStateCollection {
    private var refresh: LoadState = NotLoading.Incomplete
    private var prepend: LoadState = NotLoading.Incomplete
    private var append: LoadState = NotLoading.Incomplete
    private var source: LoadStates = LoadStates.IDLE
    private var mediator: LoadStates? = null

    fun snapshot() = CombinedLoadStates(
        refresh = refresh,
        prepend = prepend,
        append = append,
        source = source,
        mediator = mediator,
    )

    fun set(combinedLoadStates: CombinedLoadStates) {
        refresh = combinedLoadStates.refresh
        prepend = combinedLoadStates.prepend
        append = combinedLoadStates.append
        source = combinedLoadStates.source
        mediator = combinedLoadStates.mediator
    }

    fun set(sourceLoadStates: LoadStates, remoteLoadStates: LoadStates?) {
        source = sourceLoadStates
        mediator = remoteLoadStates
        updateHelperStates()
    }

    fun set(type: LoadType, remote: Boolean, state: LoadState): Boolean {
        val didChange = if (remote) {
            val lastMediator = mediator
            mediator = (mediator ?: LoadStates.IDLE).modifyState(type, state)
            mediator != lastMediator
        } else {
            val lastSource = source
            source = source.modifyState(type, state)
            source != lastSource
        }

        updateHelperStates()
        return didChange
    }

    fun get(type: LoadType, remote: Boolean): LoadState? {
        return (if (remote) mediator else source)?.get(type)
    }

    private fun updateHelperStates() {
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

    internal inline fun forEach(op: (LoadType, Boolean, LoadState) -> Unit) {
        source.forEach { type, state ->
            op(type, false, state)
        }
        mediator?.forEach { type, state ->
            op(type, true, state)
        }
    }

    internal fun terminates(loadType: LoadType): Boolean {
        return get(loadType, false)!!.endOfPaginationReached &&
            get(loadType, true)?.endOfPaginationReached != false
    }
}
