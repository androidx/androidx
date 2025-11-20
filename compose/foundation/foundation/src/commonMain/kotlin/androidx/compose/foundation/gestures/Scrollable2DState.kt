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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.coroutineScope

interface Scrollable2DState {
    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [Scroll2DScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this object)
     * in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere with the [scrollPriority] higher or equal to ongoing
     * scroll, ongoing scroll will be canceled.
     */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend Scroll2DScope.() -> Unit,
    )

    /**
     * Dispatch scroll delta in pixels in both coordinates avoiding all scroll related mechanisms.
     *
     * **NOTE:** unlike [scroll], dispatching any delta with this method won't trigger nested
     * scroll, won't stop ongoing scroll/drag animation and will bypass scrolling of any priority.
     * This method will also ignore `reverseDirection` and other parameters set in scrollable.
     *
     * This method is used internally for nested scrolling dispatch and other low level operations,
     * allowing implementers of [Scrollable2DState] influence the consumption as suits them.
     * Manually dispatching delta via this method will likely result in a bad user experience, you
     * must prefer [scroll] method over this one.
     *
     * @param delta amount of scroll dispatched in the nested scroll process in both coordinates
     * @return the amount of delta consumed in both coordinates
     */
    fun dispatchRawDelta(delta: Offset): Offset

    /**
     * Whether this [Scrollable2DState] is currently scrolling by gesture, fling or programmatically
     * or not.
     */
    val isScrollInProgress: Boolean

    /**
     * Whether this [Scrollable2DState] can scroll using [offset]. This means that this state has
     * enough space to apply offsets on the direction represented by [offset]. Inferring
     * directionality from [offset] can be achieved using the angle offered by atan(offset.y,
     * offset.x). Note that returning `true` here does not imply that offset *will* be consumed. The
     * Scrollable2DState may decide not to handle the incoming offset (such as if it is already
     * being scrolled separately).
     *
     * @param offset An offset in pixels representing the 2D vector to check against.
     * @return Whether this state can scroll in the direction given by [offset].
     */
    fun canScroll(offset: Offset): Boolean
}

/** Scope used for suspending scroll blocks */
interface Scroll2DScope {
    /**
     * Attempts to scroll forward by [delta] px.
     *
     * @return the amount of the requested scroll that was consumed (that is, how far it scrolled)
     */
    fun scrollBy(delta: Offset): Offset
}

/**
 * Default implementation of [Scrollable2DState] interface that contains necessary information about
 * the ongoing fling and provides smooth scrolling capabilities.
 *
 * This is the simplest way to set up a [scrollable2D] modifier. When constructing this
 * [Scrollable2DState], you must provide a [consumeScrollDelta] lambda, which will be invoked
 * whenever scroll happens (by gesture input, by smooth scrolling, by flinging or nested scroll)
 * with the delta in pixels. The amount of scrolling delta consumed must be returned from this
 * lambda to ensure proper nested scrolling behaviour.
 *
 * @param consumeScrollDelta callback invoked when drag/fling/smooth scrolling occurs. The callback
 *   receives the delta in pixels. Callers should update their state in this lambda and return the
 *   amount of delta consumed
 */
fun Scrollable2DState(consumeScrollDelta: (Offset) -> Offset): Scrollable2DState {
    return DefaultScrollable2DState(consumeScrollDelta)
}

/**
 * Create and remember the default implementation of [Scrollable2DState] interface that contains
 * necessary information about the ongoing fling and provides smooth scrolling capabilities.
 *
 * This is the simplest way to set up a [scrollable2D] modifier. When constructing this
 * [Scrollable2DState], you must provide a [consumeScrollDelta] lambda, which will be invoked
 * whenever scroll happens (by gesture input, by smooth scrolling, by flinging or nested scroll)
 * with the delta in pixels. The amount of scrolling delta consumed must be returned from this
 * lambda to ensure proper nested scrolling behaviour.
 *
 * @param consumeScrollDelta callback invoked when drag/fling/smooth scrolling occurs. The callback
 *   receives the delta in pixels. Callers should update their state in this lambda and return the
 *   amount of delta consumed
 */
@Composable
fun rememberScrollable2DState(consumeScrollDelta: (Offset) -> Offset): Scrollable2DState {
    val lambdaState = rememberUpdatedState(consumeScrollDelta)
    return remember { Scrollable2DState { lambdaState.value.invoke(it) } }
}

private class DefaultScrollable2DState(val onDelta: (Offset) -> Offset) : Scrollable2DState {

    private val scrollScope: Scroll2DScope =
        object : Scroll2DScope {
            override fun scrollBy(delta: Offset): Offset {
                if (delta.x.isNaN() || delta.y.isNaN()) return Offset.Zero
                return onDelta(delta)
            }
        }

    private val scrollMutex = MutatorMutex()

    private val isScrollingState = mutableStateOf(false)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend Scroll2DScope.() -> Unit,
    ): Unit = coroutineScope {
        scrollMutex.mutateWith(scrollScope, scrollPriority) {
            isScrollingState.value = true
            try {
                block()
            } finally {
                isScrollingState.value = false
            }
        }
    }

    override fun dispatchRawDelta(delta: Offset): Offset {
        return onDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value

    override fun canScroll(offset: Offset): Boolean = true
}
