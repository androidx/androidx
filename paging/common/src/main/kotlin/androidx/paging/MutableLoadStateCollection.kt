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

import androidx.annotation.RestrictTo

/**
 * TODO: Remove this once [PageEvent.LoadStateUpdate] contained [CombinedLoadStates].
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MutableLoadStateCollection(hasRemoteState: Boolean) {
    private var source: LoadStates = LoadStates.IDLE_SOURCE
    private var mediator: LoadStates? = if (hasRemoteState) LoadStates.IDLE_MEDIATOR else null

    fun snapshot() = CombinedLoadStates(source, mediator)

    fun set(combinedLoadStates: CombinedLoadStates) {
        source = combinedLoadStates.source
        mediator = combinedLoadStates.mediator
    }

    fun set(type: LoadType, remote: Boolean, state: LoadState): Boolean {
        return if (remote) {
            val lastMediator = mediator
            mediator = (mediator ?: LoadStates.IDLE_MEDIATOR).modifyState(type, state)
            mediator != lastMediator
        } else {
            val lastSource = source
            source = source.modifyState(type, state)
            source != lastSource
        }
    }

    fun get(type: LoadType, remote: Boolean): LoadState? {
        return (if (remote) mediator else source)?.get(type)
    }

    internal inline fun forEach(op: (LoadType, Boolean, LoadState?) -> Unit) {
        source.forEach { type, state ->
            op(type, false, state)
        }
        mediator?.forEach { type, state ->
            op(type, true, state)
        }
    }

    private fun LoadStates.get(type: LoadType): LoadState = when (type) {
        LoadType.REFRESH -> refresh
        LoadType.PREPEND -> prepend
        LoadType.APPEND -> append
    }

    private fun LoadStates.modifyState(
        type: LoadType,
        state: LoadState
    ): LoadStates {
        return if (get(type) == state) {
            this
        } else {
            when (type) {
                LoadType.REFRESH -> copy(refresh = state)
                LoadType.PREPEND -> copy(prepend = state)
                LoadType.APPEND -> copy(append = state)
            }
        }
    }
}
