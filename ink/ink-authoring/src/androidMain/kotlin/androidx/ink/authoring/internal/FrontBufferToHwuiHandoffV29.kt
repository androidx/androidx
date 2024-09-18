/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import android.os.Build
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.ink.authoring.InProgressStrokeId

/**
 * A version of [FrontBufferToHwuiHandoff] that relies on temporarily translating the [SurfaceView]
 * off screen while the front buffer is cleared, with a timer that almost always results in a
 * flicker-free handoff from front buffer rendering to HWUI-based multi-buffer rendering.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class FrontBufferToHwuiHandoffV29(
    private val mainView: ViewGroup,
    private val surfaceView: SurfaceView,
    /**
     * Called to hand off a group of finished strokes from being rendered internally to being
     * rendered by a higher level in HWUI.
     *
     * @see InProgressStrokesRenderHelper.Callback.onStrokeCohortHandoffToHwui
     */
    @UiThread private val onCohortHandoff: (Map<InProgressStrokeId, FinishedStroke>) -> Unit,

    /**
     * Called after [onCohortHandoff] when it is safe for higher level code to start drawing again.
     *
     * @see InProgressStrokesRenderHelper.Callback.onStrokeCohortHandoffToHwuiComplete
     */
    @UiThread private val onCohortHandoffComplete: () -> Unit,

    /**
     * A function that allows registering a callback to run after all user inputs and developer UI
     * thread callbacks. This is overridable for unit tests, but in real code should always be
     * [android.view.View.postOnAnimation].
     */
    private val postOnAnimation: (Runnable) -> Unit = mainView::postOnAnimation,
) : FrontBufferToHwuiHandoff {

    private var afterHandoffFrameCount = 0

    override fun setup() = Unit

    override fun cleanup() = Unit

    @UiThread
    override fun requestCohortHandoff(handingOff: Map<InProgressStrokeId, FinishedStroke>) {
        onCohortHandoff(handingOff)
        hideThenWaitThenShow()
    }

    @UiThread
    private fun hideThenWaitThenShow() {
        // Translate the SurfaceView off screen so that it is no longer visible, synchronized with
        // the
        // next HWUI draw. This is done as a translation rather than changing the visibility to
        // avoid
        // SurfaceView destroying the underlying surface.
        surfaceView.translationX = mainView.resources.displayMetrics.widthPixels * 2F
        surfaceView.translationY = mainView.resources.displayMetrics.heightPixels * 2F
        mainView.invalidate()
        // This translate won't take effect right away, so wait a bit before clearing the surface
        // and
        // bringing it back on screen.
        afterHandoffFrameCount = 0
        postOnAnimation(::onAnimation)
    }

    @UiThread
    private fun onAnimation() {
        afterHandoffFrameCount++
        if (afterHandoffFrameCount < HANDOFF_COMPLETE_FRAME_COUNT) {
            postOnAnimation(::onAnimation)
        } else {
            onHandoffComplete()
        }
    }

    @UiThread
    private fun onHandoffComplete() {
        show()
        onCohortHandoffComplete()
    }

    /**
     * Move the front buffer layer back on screen with the next HWUI draw. The front buffer is being
     * cleared simultaneously with this via [onCohortHandoffComplete], but clearing the front buffer
     * is much faster than a HWUI operation so the front buffer will be clear by the time it is back
     * on screen.
     */
    @UiThread
    private fun show() {
        surfaceView.translationX = 0F
        surfaceView.translationY = 0F
        mainView.invalidate()
    }

    private companion object {
        /**
         * The number of frames (as delimited by [postOnAnimation] callbacks) to wait to know that
         * [surfaceView] has been translated off screen and it is safe to clear the front buffer
         * layer. This is an imperfect heuristic - if it's too low then the front buffer will be
         * cleared before it's fully off screen which will lead to a flicker, but if it's too high
         * then the front buffer will stay off screen longer than it needed to so the beginning of
         * the next stroke may be delayed in rendering.
         */
        const val HANDOFF_COMPLETE_FRAME_COUNT = 5
    }
}
