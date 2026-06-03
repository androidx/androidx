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

package androidx.compose.ui.scene

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.platform.FrameRecomposer

/**
 * Coordinates synchronous rendering of a single [ComposeScene] driven by a [FrameRecomposer].
 *
 * It bundles the per-frame pipeline — advance the [FrameRecomposer], then run the scene's
 * [ComposeScene.measureAndLayout] and draw phases — into a single [render] call, and folds the
 * scene's layout/draw invalidations into one scheduled frame ([onSceneInvalidation]). Invalidations
 * raised while a frame is already being rendered are handled by that same frame rather than
 * scheduling a new one.
 *
 * The scope exists only to ease the migration to the split [FrameRecomposer] / [ComposeScene] APIs,
 * so each platform host doesn't reimplement the same single-scene wiring.
 *
 * @param scheduleFrame Requests the host to run a frame. Must be idempotent/coalescing:
 *  it may be called multiple times before a frame actually runs.
 *
 * TODO: Deprecate this class once platforms drive [FrameRecomposer.performFrame] and the scene
 *  phases directly (e.g. integrated with their native layout/draw scheduling).
 */
@InternalComposeUiApi
class SingleComposeSceneRenderingScope(
    private val scheduleFrame: () -> Unit,
) {
    private var isRendering = false

    private inline fun postponingSceneInvalidations(crossinline block: () -> Unit) {
        check(!isRendering)
        isRendering = true
        try {
            block()
        } finally {
            isRendering = false
        }
    }

    /**
     * Should be wired to the scene's `invalidateLayout` / `invalidateDraw` callbacks.
     * Schedules a frame when the content needs another layout/draw pass, unless the invalidation
     * was raised by a phase of the frame that is currently being rendered.
     */
    fun onSceneInvalidation() {
        if (isRendering) return
        scheduleFrame()
    }

    /**
     * Renders the current content on [canvas]: advances the host [frameRecomposer] by one frame,
     * then runs the scene's measure/layout and draw phases. [nanoTime] is the frame time used to
     * drive all animations in the content (and any other code using [withFrameNanos]).
     */
    fun ComposeScene.render(frameRecomposer: FrameRecomposer, canvas: Canvas, nanoTime: Long) {
        postponingSceneInvalidations {
            frameRecomposer.performFrame(nanoTime)
            measureAndLayout()
            draw(canvas)
        }
        if (hasInvalidations()) {
            scheduleFrame()
        }
    }
}
