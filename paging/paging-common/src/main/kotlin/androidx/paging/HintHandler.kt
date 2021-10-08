/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Helper class to handle UI hints.
 * It processes incoming hints and keeps a min/max (prepend/append) values and provides them as a
 * flow to [PageFetcherSnapshot].
 */
internal class HintHandler {
    private val state = State()

    /**
     * Latest call to [processHint]. Note that this value might be ignored wrt prepend and append
     * hints if it is not expanding the range.
     */
    val lastAccessHint: ViewportHint.Access?
        get() = state.lastAccessHint

    /**
     * Returns a flow of hints for the given [loadType].
     */
    fun hintFor(loadType: LoadType): Flow<ViewportHint> = when (loadType) {
        PREPEND -> state.prependFlow
        APPEND -> state.appendFlow
        else -> throw IllegalArgumentException("invalid load type for hints")
    }

    /**
     * Resets the hint for the given [loadType].
     * Note that this won't update [lastAccessHint] or the other load type.
     */
    fun forceSetHint(
        loadType: LoadType,
        viewportHint: ViewportHint
    ) {
        require(
            loadType == PREPEND || loadType == APPEND
        ) {
            "invalid load type for reset: $loadType"
        }
        state.modify(
            accessHint = null
        ) { prependHint, appendHint ->
            if (loadType == PREPEND) {
                prependHint.value = viewportHint
            } else {
                appendHint.value = viewportHint
            }
        }
    }

    /**
     * Processes the hint coming from UI.
     */
    fun processHint(viewportHint: ViewportHint) {
        state.modify(viewportHint as? ViewportHint.Access) { prependHint, appendHint ->
            if (viewportHint.shouldPrioritizeOver(
                    previous = prependHint.value,
                    loadType = PREPEND
                )
            ) {
                prependHint.value = viewportHint
            }
            if (viewportHint.shouldPrioritizeOver(
                    previous = appendHint.value,
                    loadType = APPEND
                )
            ) {
                appendHint.value = viewportHint
            }
        }
    }

    private inner class State {
        private val prepend = HintFlow()
        private val append = HintFlow()
        var lastAccessHint: ViewportHint.Access? = null
            private set
        val prependFlow
            get() = prepend.flow
        val appendFlow
            get() = append.flow
        private val lock = ReentrantLock()

        /**
         * Modifies the state inside a lock where it gets access to the mutable values.
         */
        fun modify(
            accessHint: ViewportHint.Access?,
            block: (prepend: HintFlow, append: HintFlow) -> Unit
        ) {
            lock.withLock {
                if (accessHint != null) {
                    lastAccessHint = accessHint
                }
                block(prepend, append)
            }
        }
    }

    /**
     * Like a StateFlow that holds the value but does not do de-duping.
     * Note that, this class is not thread safe.
     */
    private inner class HintFlow {
        var value: ViewportHint? = null
            set(value) {
                field = value
                if (value != null) {
                    _flow.tryEmit(value)
                }
            }
        private val _flow = MutableSharedFlow<ViewportHint>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val flow: Flow<ViewportHint>
            get() = _flow
    }
}

internal fun ViewportHint.shouldPrioritizeOver(
    previous: ViewportHint?,
    loadType: LoadType
): Boolean {
    return when {
        previous == null -> true
        // Prioritize Access hints over Initialize hints
        previous is ViewportHint.Initial && this is ViewportHint.Access -> true
        this is ViewportHint.Initial && previous is ViewportHint.Access -> false
        // Prioritize hints from most recent presenter state
        // not that this it not a gt/lt check because we would like to prioritize any
        // change in available pages, not necessarily more or less as drops can have an impact.
        this.originalPageOffsetFirst != previous.originalPageOffsetFirst -> true
        this.originalPageOffsetLast != previous.originalPageOffsetLast -> true
        // Prioritize hints that would load the most items
        previous.presentedItemsBeyondAnchor(loadType) <= presentedItemsBeyondAnchor(loadType) ->
            false
        else -> true
    }
}