/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/** A shared base class for [TrackpadScrollingLogic] and [MouseWheelScrollingLogic]. */
internal abstract class NonTouchScrollingLogic(
    protected val scrollingLogic: ScrollingLogic,
    protected val onScrollStopped: suspend (velocity: Velocity) -> Unit,
    protected var density: Density,
) {
    fun updateDensity(density: Density) {
        this.density = density
    }

    internal inline val PointerEvent.isConsumed: Boolean
        get() = changes.fastAny { it.isConsumed }

    internal fun PointerEvent.consume() = changes.fastForEach { it.consume() }

    internal var isScrolling = false

    internal suspend fun userScroll(block: suspend NestedScrollScope.() -> Unit) {
        isScrolling = true
        // Run it in supervisorScope to ignore cancellations from scrolls with higher MutatePriority
        supervisorScope { scrollingLogic.scroll(MutatePriority.UserInput, block) }
        isScrolling = false
    }

    internal val velocityTracker = DifferentialVelocityTracker()

    /** Forwards the given [pointerEvent] for processing by this scroll logic. */
    abstract fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize)

    /** Begins processing of events sent to [onPointerEvent] using the given [coroutineScope]. */
    abstract fun startReceivingEvents(coroutineScope: CoroutineScope)
}

/**
 * Replacement of regular [Channel.receive] that schedules an invalidation each frame. It avoids
 * entering an idle state while waiting for [ScrollProgressTimeout]. It's important for tests that
 * attempt to trigger another scroll after a mouse wheel event.
 */
internal suspend fun <T> Channel<T>.busyReceive(): T = coroutineScope {
    val job = launch {
        while (coroutineContext.isActive) {
            withFrameNanos {}
        }
    }
    try {
        receive()
    } finally {
        job.cancel()
    }
}

internal fun <E> untilNull(builderAction: () -> E?) =
    sequence<E> {
        do {
            val element = builderAction()?.also { yield(it) }
        } while (element != null)
    }
