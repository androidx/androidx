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

package androidx.ink.authoring

import androidx.annotation.UiThread
import androidx.ink.strokes.Stroke

/**
 * Notifies the client app when a [Stroke] (or more than one) has been completed on
 * [InProgressStrokesView].
 */
@UiThread
public interface InProgressStrokesFinishedListener {
    /**
     * Called when there are no longer any in-progress strokes in the [InProgressStrokesView] for a
     * short period. All strokes that were in progress simultaneously will be delivered in the same
     * callback, running on the UI thread.
     *
     * An implementation of this function should start rendering the given strokes in an
     * [android.view.View] by calling its [android.view.View.invalidate] function and using the new
     * [Stroke] data in the next call to [android.view.View.onDraw].
     *
     * In the same UI thread run loop as calling [android.view.View.invalidate], call
     * [InProgressStrokesView.removeFinishedStrokes] with the IDs of the strokes that are now being
     * rendered in the other [android.view.View]. If that happens in a different run loop of the UI
     * thread, there may be brief rendering errors (appearing as flickers) between
     * [InProgressStrokesView] and the other [android.view.View] - either a gap where the stroke is
     * not drawn during a frame, or a double draw where the stroke is drawn twice and translucent
     * strokes appear more opaque than they should.
     *
     * Example:
     * ```
     * public fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
     *   view.addStrokes(strokes.values)
     *   view.invalidate()
     *   inProgressStrokesView.removeFinishedStrokes(strokes.keys)
     * }
     * ```
     *
     * @param strokes The finished strokes, with map iteration order in the z-order that strokes
     *   were rendered in the [InProgressStrokesView], from back to front. This is the same order
     *   that strokes were started with [startStroke].
     */
    public fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {}
}
