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

package androidx.compose.ui.window

import androidx.compose.ui.platform.PlatformOutOfFrameExecutor
import androidx.compose.ui.util.trace
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Drains out-of-frame work around Metal frame production.
 *
 * Work scheduled during a frame is drained in [onFrameEnd], after that frame has been recorded and
 * before the next frame starts. Work scheduled between frames is already out of the current frame,
 * so it should run before the next frame starts instead of waiting for that next frame to finish.
 * The async main-queue drain is a best-effort way to run such work early, while [onFrameStart]
 * provides the ordering guarantee if the display-link callback wins the race.
 */
internal class MetalOutOfFrameExecutor : PlatformOutOfFrameExecutor {
    private val queue = ArrayDeque<() -> Unit>()
    private var isFrameInProgress = false
    private var isDrainScheduled = false
    private var isDraining = false
    private var isDisposed = false

    override val hasWorkScheduled: Boolean
        get() = queue.isNotEmpty()

    override fun schedule(block: () -> Unit) {
        check(NSThread.isMainThread) {
            "MetalOutOfFrameExecutor.schedule() must be called on main thread"
        }
        if (isDisposed) {
            return
        }

        queue.addLast(block)

        if (!isFrameInProgress && !isDraining && !isDrainScheduled) {
            // When work is scheduled during frame recording, onFrameEnd() drains it before the next
            // frame starts. Outside of a frame there is no such drain point, but running the block
            // inline would make scheduling synchronous and could mutate composition state from the
            // current rendering/layout stack. Post one main-queue drain to defer the work while
            // keeping it on the main thread.
            isDrainScheduled = true
            dispatch_async(dispatch_get_main_queue()) {
                drain()
            }
        }
    }

    fun onFrameStart() {
        check(NSThread.isMainThread) {
            "MetalOutOfFrameExecutor.onFrameStart() must be called on main thread"
        }
        if (isDisposed) {
            return
        }

        // The async main-queue drain is best-effort and can lose the race to a display-link
        // callback. Drain pending work before starting frame production to preserve the contract
        // that out-of-frame work runs before the next frame starts.
        drain()

        isFrameInProgress = true
    }

    fun onFrameEnd() {
        check(NSThread.isMainThread) {
            "MetalOutOfFrameExecutor.onFrameEnd() must be called on main thread"
        }
        if (isDisposed) {
            return
        }

        isFrameInProgress = false
        drain()
    }

    fun dispose() {
        check(NSThread.isMainThread) {
            "MetalOutOfFrameExecutor.dispose() must be called on main thread"
        }
        isDisposed = true
        isFrameInProgress = false
        isDrainScheduled = false
        queue.clear()
    }

    override fun drainScheduledWorkForTest() = drain()

    private fun drain() {
        check(NSThread.isMainThread) {
            "MetalOutOfFrameExecutor.drain() must be called on main thread"
        }

        if (isDisposed || isDraining) {
            return
        }

        isDrainScheduled = false
        isDraining = true
        try {
            trace("MetalRedrawer:outOfFrameExecutor") {
                while (queue.isNotEmpty()) {
                    queue.removeLast().invoke()
                }
            }
        } finally {
            isDraining = false
        }
    }
}
