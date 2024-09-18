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

import android.graphics.Matrix
import android.graphics.Path
import androidx.annotation.UiThread
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.geometry.MutableBox
import androidx.ink.strokes.InProgressStroke

/**
 * Manages rendering of in-progress strokes and the synchronized handoff of strokes from being in
 * progress to finished. Different implementations of this interface may make use of different
 * approaches to minimize the latency of drawn strokes appearing on screen, or different approaches
 * to synchronizing the handoff. This is an internal utility used by [InProgressStrokesManager], it
 * is not to be used by clients of the Ink Strokes API.
 *
 * Terminology:
 * - UI thread: The Android main thread, which HWUI (View) operations take place on.
 * - Render thread: The single thread, managed by an implementation of this interface, which is used
 *   for drawing operations. In some cases this may be the UI thread itself, but it usually is not.
 * - Main View: The root View of [InProgressStrokesManager] which clients add to their View
 *   hierarchy. Its parent belongs to the client, and an implementation of this interface may add
 *   children to it and manage them.
 * - Stroke cohort: A group of strokes that are in progress at the same time, which means that they
 *   need to be handed off to HWUI rendering at the same time.
 */
@OptIn(ExperimentalLatencyDataApi::class)
internal interface InProgressStrokesRenderHelper {

    /**
     * Whether stroke contents that were drawn earlier are preserved for later draws, as an
     * optimization to redraw only the modified regions of the screen.
     */
    val contentsPreservedBetweenDraws: Boolean

    /**
     * Whether [InProgressStrokesView.handoffDebounceTimeMs] is supported. If not, then handoff
     * should be initiated as soon as possible.
     */
    val supportsDebounce: Boolean

    /**
     * Whether [InProgressStrokesView.flush] is supported. If not, then that method will not be able
     * to wait for strokes to be completed.
     */
    val supportsFlush: Boolean

    /**
     * An area of the inking surface where no ink should be visible, and the contents beneath should
     * show through.
     */
    var maskPath: Path?

    /**
     * Can be used by [InProgressStrokesManager] to fail fast when not operating on the expected
     * thread.
     */
    fun assertOnRenderThread()

    /**
     * Called by [InProgressStrokesManager] when new content must be drawn. Will lead to
     * [InProgressStrokesRenderHelper.onDraw].
     */
    @UiThread fun requestDraw()

    /**
     * Allows communication between this interface and the code making use of it, which is presumed
     * to be an [InProgressStrokesManager].
     */
    interface Callback {

        /**
         * Called on the render thread to prompt [InProgressStrokesManager] to start making draw
         * calls coordinate the overall logic for making draw calls to the renderer. An
         * implementation of [InProgressStrokesRenderHelper] may save some context state before
         * calling this and reset it afterwards.
         */
        fun onDraw()

        /**
         * Called on the render thread to allow [InProgressStrokesManager] to perform some cleanup
         * logic after the draw calls are complete.
         */
        fun onDrawComplete()

        /**
         * For latency tracking. Called on the render thread to report the estimated time when
         * newly-rendered pixels will be visible to the user. This time will probably be in the near
         * future.
         *
         * @param timeNanos Estimated nanosecond presentation timestamp, in the [System.nanoTime]
         *   base.
         */
        fun reportEstimatedPixelPresentationTime(timeNanos: Long)

        /**
         * For latency tracking. Called on the render thread to set a client-chosen field in every
         * in-flight [LatencyData] instance. The [setter] will be run once for each [LatencyData]
         * that is currently active. The second argument will be the value of [System.nanoTime] when
         * this callback was invoked. This callback may be used, for example, to set fields that are
         * specific to a particular implementation of [InProgressStrokesRenderHelper].
         */
        fun setCustomLatencyDataField(setter: (LatencyData, Long) -> Unit)

        /**
         * For latency tracking. Called on the render thread to finalize all in-flight [LatencyData]
         * instances and report them to the Latency API client.
         */
        fun handOffAllLatencyData()

        /**
         * Called on the UI thread to either disallow (pause) or allow (unpause) calls to
         * [requestStrokeCohortHandoffToHwui].
         */
        @UiThread fun setPauseStrokeCohortHandoffs(paused: Boolean)

        /**
         * Called to hand off a group of finished strokes from being rendered internally to being
         * rendered by a higher level in HWUI. This must happen synchronously, in the same HWUI
         * frame. Failure to do so will result in a flicker on handoff, where the stroke is
         * temporarily not rendered. Initiated by [requestStrokeCohortHandoffToHwui].
         */
        @UiThread
        fun onStrokeCohortHandoffToHwui(strokeCohort: Map<InProgressStrokeId, FinishedStroke>)

        /**
         * Called some time after [onStrokeCohortHandoffToHwui], when it is appropriate to start
         * calling [requestDraw] again.
         */
        @UiThread fun onStrokeCohortHandoffToHwuiComplete()
    }

    /**
     * Set up rendering to a particular region that has modified geometry. Called on the render
     * thread.
     */
    fun prepareToDrawInModifiedRegion(modifiedRegionInMainView: MutableBox)

    /**
     * Draw an [InProgressStroke] in the region previously prepared with
     * [prepareToDrawInModifiedRegion]. This may be called multiple times per modified region with
     * different [InProgressStroke] objects. Called on the render thread.
     */
    fun drawInModifiedRegion(inProgressStroke: InProgressStroke, strokeToMainViewTransform: Matrix)

    /**
     * Cleans up what was initialized in [prepareToDrawInModifiedRegion]. Called on the render
     * thread.
     */
    fun afterDrawInModifiedRegion()

    /**
     * Clear all contents that have previously been rendered, if [contentsPreservedBetweenDraws] is
     * `true`. Called on the render thread.
     */
    fun clear()

    /**
     * Called by [InProgressStrokesManager] when no new content is expected and the current
     * in-progress content should be handed off to be rendered by HWUI. HWUI rendering of finished
     * strokes is not handled by this class - this will lead to
     * [Callback.onStrokeCohortHandoffToHwui], which is responsible for initiating HWUI rendering.
     * Between this and [Callback.onStrokeCohortHandoffToHwuiComplete], any calls to [requestDraw]
     * may not (and may never become) visible.
     */
    @UiThread
    fun requestStrokeCohortHandoffToHwui(handingOff: Map<InProgressStrokeId, FinishedStroke>)
}
