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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.internal.JvmDefaultWithCompatibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.coroutineScope

/**
 * An object representing something that can be scrolled. This interface is implemented by states
 * of scrollable containers such as [androidx.compose.foundation.lazy.LazyListState] or
 * [androidx.compose.foundation.ScrollState] in order to provide low-level scrolling control via
 * [scroll], as well as allowing for higher-level scrolling functions like
 * [animateScrollBy] to be implemented as extension
 * functions on [ScrollableState].
 *
 * Subclasses may also have their own methods that are specific to their interaction paradigm, such
 * as [androidx.compose.foundation.lazy.LazyListState.scrollToItem].
 *
 * @see androidx.compose.foundation.gestures.animateScrollBy
 * @see androidx.compose.foundation.gestures.scrollable
 */
@JvmDefaultWithCompatibility
interface ScrollableState {
    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [ScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this
     * object) in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere with the [scrollPriority] higher or equal to ongoing
     * scroll, ongoing scroll will be canceled.
     */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend ScrollScope.() -> Unit
    )

    /**
     * Dispatch scroll delta in pixels avoiding all scroll related mechanisms.
     *
     * **NOTE:** unlike [scroll], dispatching any delta with this method won't trigger nested
     * scroll, won't stop ongoing scroll/drag animation and will bypass scrolling of any priority.
     * This method will also ignore `reverseDirection` and other parameters set in scrollable.
     *
     * This method is used internally for nested scrolling dispatch and other low level
     * operations, allowing implementers of [ScrollableState] influence the consumption as suits
     * them. Manually dispatching delta via this method will likely result in a bad user experience,
     * you must prefer [scroll] method over this one.
     *
     * @param delta amount of scroll dispatched in the nested scroll process
     *
     * @return the amount of delta consumed
     */
    fun dispatchRawDelta(delta: Float): Float

    /**
     * Whether this [ScrollableState] is currently scrolling by gesture, fling or programmatically or
     * not.
     */
    val isScrollInProgress: Boolean

    /**
     * Whether this [ScrollableState] can scroll forward (consume a positive delta). This is
     * typically false if the scroll position is equal to its maximum value, and true otherwise.
     *
     * Note that `true` here does not imply that delta *will* be consumed - the ScrollableState may
     * decide not to handle the incoming delta (such as if it is already being scrolled separately).
     * Additionally, for backwards compatibility with previous versions of ScrollableState this
     * value defaults to `true`.
     *
     * @sample androidx.compose.foundation.samples.CanScrollSample
     */
    val canScrollForward: Boolean
        get() = true

    /**
     * Whether this [ScrollableState] can scroll backward (consume a negative delta). This is
     * typically false if the scroll position is equal to its minimum value, and true otherwise.
     *
     * Note that `true` here does not imply that delta *will* be consumed - the ScrollableState may
     * decide not to handle the incoming delta (such as if it is already being scrolled separately).
     * Additionally, for backwards compatibility with previous versions of ScrollableState this
     * value defaults to `true`.
     *
     * @sample androidx.compose.foundation.samples.CanScrollSample
     */
    val canScrollBackward: Boolean
        get() = true

    /**
     * The value of this property is true under the following scenarios, otherwise it's false.
     * - This [ScrollableState] is currently scrolling forward.
     * - This [ScrollableState] was scrolling forward in its last scroll action.
     */
    @get:Suppress("GetterSetterNames")
    val lastScrolledForward: Boolean
        get() = false

    /**
     * The value of this property is true under the following scenarios, otherwise it's false.
     * - This [ScrollableState] is currently scrolling backward.
     * - This [ScrollableState] was scrolling backward in its last scroll action.
     */
    @get:Suppress("GetterSetterNames")
    val lastScrolledBackward: Boolean
        get() = false
}

/**
 * Default implementation of [ScrollableState] interface that contains necessary information about the
 * ongoing fling and provides smooth scrolling capabilities.
 *
 * This is the simplest way to set up a [scrollable] modifier. When constructing this
 * [ScrollableState], you must provide a [consumeScrollDelta] lambda, which will be invoked whenever
 * scroll happens (by gesture input, by smooth scrolling, by flinging or nested scroll) with the
 * delta in pixels. The amount of scrolling delta consumed must be returned from this lambda to
 * ensure proper nested scrolling behaviour.
 *
 * @param consumeScrollDelta callback invoked when drag/fling/smooth scrolling occurs. The
 * callback receives the delta in pixels. Callers should update their state in this lambda and
 * return the amount of delta consumed
 */
fun ScrollableState(consumeScrollDelta: (Float) -> Float): ScrollableState {
    return DefaultScrollableState(consumeScrollDelta)
}

/**
 * Create and remember the default implementation of [ScrollableState] interface that contains
 * necessary information about the ongoing fling and provides smooth scrolling capabilities.
 *
 * This is the simplest way to set up a [scrollable] modifier. When constructing this
 * [ScrollableState], you must provide a [consumeScrollDelta] lambda, which will be invoked whenever
 * scroll happens (by gesture input, by smooth scrolling, by flinging or nested scroll) with the
 * delta in pixels. The amount of scrolling delta consumed must be returned from this lambda to
 * ensure proper nested scrolling behaviour.
 *
 * @param consumeScrollDelta callback invoked when drag/fling/smooth scrolling occurs. The
 * callback receives the delta in pixels. Callers should update their state in this lambda and
 * return the amount of delta consumed
 */
@Composable
fun rememberScrollableState(consumeScrollDelta: (Float) -> Float): ScrollableState {
    val lambdaState = rememberUpdatedState(consumeScrollDelta)
    return remember { ScrollableState { lambdaState.value.invoke(it) } }
}

/**
 * Scope used for suspending scroll blocks
 */
interface ScrollScope {
    /**
     * Attempts to scroll forward by [pixels] px.
     *
     * @return the amount of the requested scroll that was consumed (that is, how far it scrolled)
     */
    fun scrollBy(pixels: Float): Float
}

private class DefaultScrollableState(val onDelta: (Float) -> Float) : ScrollableState {

    private val scrollScope: ScrollScope = object : ScrollScope {
        override fun scrollBy(pixels: Float): Float {
            if (pixels.isNaN()) return 0f
            val delta = onDelta(pixels)
            isLastScrollForwardState.value = delta > 0
            isLastScrollBackwardState.value = delta < 0
            return delta
        }
    }

    private val scrollMutex = MutatorMutex()

    private val isScrollingState = mutableStateOf(false)
    private val isLastScrollForwardState = mutableStateOf(false)
    private val isLastScrollBackwardState = mutableStateOf(false)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
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

    override fun dispatchRawDelta(delta: Float): Float {
        return onDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value

    override val lastScrolledForward: Boolean
        get() = isLastScrollForwardState.value

    override val lastScrolledBackward: Boolean
        get() = isLastScrollBackwardState.value
}
